package supervisor;

import core.codemodel.events.Event;
import core.dependencies.Dependency;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import static supervisor.Config.COMPUTATION_CELL_FRESH_VAL_TRESHOLD;

class ComputationCell<Dep extends Dependency, Result extends Event, MsgT> extends Thread implements RowProvider<Dep, Result, MsgT> {

    private final ComputationNetwork parentNetwork;
    private final Function<Result, Float> defaultValProducer;
    private final boolean rowsBeginInitialized;
    private final FormulaProvider<Dep, Result> formulaProvider;
    private final MessageProcessorProducer<Result, MsgT> messageProcessorProducer;

    ComputationCell(ComputationNetwork parentNetwork,
                    Function<Result, Float> defaultValProducer,
                    boolean rowsBeginInitialized,
                    FormulaProvider<Dep, Result> formulaProvider,
                    MessageProcessorProducer<Result, MsgT> messageProcessorProducer) {
        this.parentNetwork = parentNetwork;
        this.defaultValProducer = defaultValProducer;
        this.rowsBeginInitialized = rowsBeginInitialized;
        this.formulaProvider = formulaProvider;
        this.messageProcessorProducer = messageProcessorProducer;
    }

    class Row implements ComputationRow<Dep, Result, MsgT> {
        boolean initialized = rowsBeginInitialized;

        //TODO: concurrent updates to this formula would cause problems, ensure they don't occur
        //not a problem cause we're not gonna be updating formulas
        private Formula<Dep> formula;
        private volatile float val;
        private final Set<ComputationRow<? super Result, ?, ?>> dependers = ConcurrentHashMap.newKeySet();
        private final Map<Dep, ComputationRow<?, ? extends Dep, ?>> dependees = new HashMap<>();
        private final AtomicBoolean dependeesUpdated = new AtomicBoolean(false);
        private boolean dependeesUpdatedSnapshot;
        private final MessageProcessor<MsgT> messageProcessor;

        private Row(Result event) {
            val = defaultValProducer.apply(event);
            messageProcessor = messageProcessorProducer.produce(event);
        }

        @Override
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

        @Override
        public void notifyNoLongerDependee(ComputationRow<? super Result, ?, ?> formerDependerRow) {
            dependers.remove(formerDependerRow);
        }

        @Override
        public void passMessage(MsgT msg) {
            messageProcessor.passMessage(msg);
        }

        @Override
        public float getVal() {
            return val;
        }
    }

    private final Map<Result, Row> store = new ConcurrentHashMap<>();

    int size() {
        return store.size();
    }

    private Row getOrCreateRow(Result event) {
        return store.computeIfAbsent(event, Row::new);
    }

    /**
     * Get a value for an event known to be stored in this cell -
     * WARNING: use `getRow` if you're another row and want to be notified of updates
     * @param event
     * @return
     */
    @Override
    public float get(Result event) {
        return getOrCreateRow(event).getVal();
    }

    @Override
    public void passMessage(Result event, MsgT msg) {
        getOrCreateRow(event).passMessage(msg);
    }

    @Override
    public void passMessageToAll(MsgT msg) {
        store.forEach(((ignoredResult, row) -> row.passMessage(msg)));
    }

    /**
     * Get the row corresponding to an event
     *
     * @param event     the event a row is being requested for
     * @param requester the row requesting this event
     * @return
     */
    @Override
    public Row getRow(Result event, ComputationRow<? super Result, ?, ?> requester) {
        Row row = getOrCreateRow(event);
        row.dependers.add(requester);
        return row;
    }

    Stream<Float> streamValues() {
        return store.values().stream().map(Row::getVal);
    }

    Stream<Map.Entry<Result, Row>> streamRows() {
        return store.entrySet().stream();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            performCycle();
        }
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

            float oldVal = row.val;

            //process any messages that might be waiting on this row
            row.val = row.messageProcessor.processMessages(oldVal);

            //if this row had its formula or its dependees updated, or if it is only now being initialized,
            //recompute its value with the contained formula
            if (row.formulaUpdatedSnapshot ||
                    row.dependeesUpdatedSnapshot ||
                    !row.initialized) {
                row.val = row.formula.compute(dep -> row.dependees.computeIfAbsent(dep, d -> {
                            throw new IllegalStateException("dependee " + d + " should not be missing from table");
                        }
                ).getVal());
            }

            //if value was updated significantly, notify dependers
            if (Math.abs(oldVal - row.val) >= COMPUTATION_CELL_FRESH_VAL_TRESHOLD) {
                row.dependers.forEach(ComputationRow::notifyDependeesUpdated);
            }

            row.initialized = true;
        });
    }

    public long numActive() {
        return store.values().stream().filter(row -> row.dependeesUpdated.get() || !row.initialized || row.formulaUpdated.get()).count();
    }

    @Override
    public String toString() {
        return store.size() + " rows (" + numActive() + " active)";
    }
}
