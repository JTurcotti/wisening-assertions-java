package computation;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


class ComputationCellGroup<Dep extends Dependency, Result extends Event> implements RowProvider<Dep, Result> {
    static final int MAX_CELL_SIZE = 1000;
    private final ComputationNetwork parentNetwork;
    private final float defaultVal;
    private final FormulaProvider<Dep, Result> formulaProvider;

    ComputationCellGroup(ComputationNetwork parentNetwork, float defaultVal, FormulaProvider<Dep, Result> formulaProvider) {
        this.parentNetwork = parentNetwork;
        this.defaultVal = defaultVal;
        this.formulaProvider = formulaProvider;
    }

    private final List<ComputationCell<Dep, Result>> cells = new ArrayList<>();

    private final Map<Result, ComputationCell<Dep, Result>> cellTable = new ConcurrentHashMap<>();

    private ComputationCell<Dep, Result> getCellForEvent(Result event) {
        return cellTable.computeIfAbsent(event, e -> {
            ComputationCell<Dep, Result> smallest = Collections.min(cells, Comparator.comparingInt(ComputationCell::size));
            ComputationCell<Dep, Result> target;
            if (smallest.size() < MAX_CELL_SIZE) {
                target = smallest;
            } else {
                ComputationCell<Dep, Result> newCell = new ComputationCell<>(parentNetwork, defaultVal, formulaProvider);
                cells.add(newCell);
                target = newCell;
            }
            cellTable.put(e, target);
            return target;
        });
    }

    public ComputationRow<Dep, Result> getRow(Result event, ComputationRow<? super Result, ?> requester) {
        return getCellForEvent(event).getRow(event, requester);
    }

    public float get(Result event) {
        return getCellForEvent(event).get(event);
    }
}