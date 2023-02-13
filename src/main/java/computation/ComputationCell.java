package computation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class ComputationCell<Dep extends Dependency, Result extends Event> implements RowProvider<Dep, Result> {

    static final float FRESH_VAL_THRESHOLD = 0.001f;
    private final ComputationNetwork parentNetwork;
    private final float defaultVal;
    private final FormulaProvider<Dep, Result> formulaProvider;


    ComputationCell(ComputationNetwork parentNetwork, float defaultVal, FormulaProvider<Dep, Result> formulaProvider) {
        this.parentNetwork = parentNetwork;
        this.defaultVal = defaultVal;
        this.formulaProvider = formulaProvider;
    }

    private class Row implements ComputationRow<Dep, Result> {
        boolean initialized = false;
        private Formula<Dep> formula;
        private float val = defaultVal;
        private final Set<ComputationRow<? super Result, ?>> dependers = new HashSet<>();
        private final Map<Dep, ComputationRow<?, ? extends Dep>> dependees = new HashMap<>();
        private final AtomicBoolean dependeesUpdated = new AtomicBoolean(false);
        private boolean dependeesUpdatedSnapshot;

        public void notifyDependeesUpdated() {
            dependeesUpdated.set(true);
        }

        void takeDependeesUpdatedSnapshot() {
            dependeesUpdatedSnapshot = dependeesUpdated.getAndSet(false);
        }

        private final AtomicBoolean formulaUpdated = new AtomicBoolean(false);
        private boolean formulaUpdatedSnapshot;

        void notifyFormulaUpdated() {
            formulaUpdated.set(true);
        }

        void takeFormulaUpdatedSnapshot() {
            formulaUpdatedSnapshot = formulaUpdated.getAndSet(false);
        }

        public void notifyNoLongerDependee(ComputationRow<? super Result, ?> formerDependerRow) {
            dependers.remove(formerDependerRow);
        }

        public float getVal() {
            return val;
        }
    }

    private Row defaultRow(Result event) {
        return new Row();
    }

    private final Map<Result, Row> store = new ConcurrentHashMap<>();

    int size() {
        return store.size();
    }

    /**
     * Get a value for an event known to be stored in this cell -
     * WARNING: use `getRow` if you're another row and want to be notified of updates
     * @param event
     * @return
     */
    public float get(Result event) {
        return store.computeIfAbsent(event, this::defaultRow).val;
    }

    /**
     * Get the row corresponding to an event
     *
     * @param event     the event a row is being requested for
     * @param requester the row requesting this event
     * @return
     */
    public Row getRow(Result event, ComputationRow<? super Result, ?> requester) {
        Row row = store.computeIfAbsent(event, this::defaultRow);
        row.dependers.add(requester);
        return row;
    }

    void performCycle() {
        store.forEach((event, row) -> {
            row.takeDependeesUpdatedSnapshot();
            row.takeFormulaUpdatedSnapshot();

            //if the formula was updated, or never set because this row isn't initialized,
            //we have to get the formula from the formula provider and fix any differences
            //in dependees
            if (row.formulaUpdatedSnapshot || !row.initialized) {
                row.formula = formulaProvider.get(event);
                Set<Dep> newDependees = row.formula.getDeps();

                //first we take care of removing all dependees of the old formula
                //that are not dependees of the new formula
                Set<Dep> removedDependees = new HashSet<>();
                row.dependees.forEach((dep, depRow) -> {
                    if (!newDependees.contains(dep)) {
                        depRow.notifyNoLongerDependee(row);
                        removedDependees.add(dep);
                    }
                });
                //can't do this during the prior iteration, so have to do it now
                removedDependees.forEach(row.dependees::remove);

                //now we take care of obtaining rows for all dependees of the new formula
                //that weren't dependees of the old formula
                newDependees.forEach(dep -> {
                    if (!row.dependees.containsKey(dep)) {
                        row.dependees.put(dep, parentNetwork.getRow(dep, row));
                    }
                });
            }
            //if this row had its formula or its dependees updated, or if it is only now being initialized,
            //recompute its value and possible notify its dependers
            if (row.formulaUpdatedSnapshot || row.dependeesUpdatedSnapshot || !row.initialized) {
                float oldVal = row.val;
                row.val = row.formula.compute(dep -> row.dependees.computeIfAbsent(dep, d -> {
                            throw new IllegalStateException("dependee " + d + " should not be missing from table");
                        }
                ).getVal());
                if (Math.abs(oldVal - row.val) >= FRESH_VAL_THRESHOLD) {
                    row.dependers.forEach(depender -> depender.notifyDependeesUpdated());
                }
            }

            row.initialized = true;
        });
    }
}
