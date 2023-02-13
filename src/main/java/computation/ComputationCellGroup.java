package computation;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static computation.Config.COMPUTATION_CELL_GROUP_MAX_CELL_SIZE;


class ComputationCellGroup<Dep extends Dependency, Result extends Event, MsgT> implements RowProvider<Dep, Result, MsgT> {
    private final ComputationNetwork parentNetwork;
    private final float defaultVal;
    private final FormulaProvider<Dep, Result> formulaProvider;
    private final MessageProcessorProducer<Result, MsgT> messageProcessorProducer;

    ComputationCellGroup(ComputationNetwork parentNetwork,
                         float defaultVal,
                         FormulaProvider<Dep, Result> formulaProvider,
                         MessageProcessorProducer<Result, MsgT> messageProcessorProducer) {
        this.parentNetwork = parentNetwork;
        this.defaultVal = defaultVal;
        this.formulaProvider = formulaProvider;
        this.messageProcessorProducer = messageProcessorProducer;
    }

    private final List<ComputationCell<Dep, Result, MsgT>> cells = new ArrayList<>();

    private final Map<Result, ComputationCell<Dep, Result, MsgT>> cellTable = new ConcurrentHashMap<>();

    private ComputationCell<Dep, Result, MsgT> getCellForEvent(Result event) {
        return cellTable.computeIfAbsent(event, e -> {
            ComputationCell<Dep, Result, MsgT> smallest = Collections.min(cells, Comparator.comparingInt(ComputationCell::size));
            ComputationCell<Dep, Result, MsgT> target;
            if (smallest.size() < COMPUTATION_CELL_GROUP_MAX_CELL_SIZE) {
                target = smallest;
            } else {
                ComputationCell<Dep, Result, MsgT> newCell =
                        new ComputationCell<>(parentNetwork, defaultVal, formulaProvider, messageProcessorProducer);
                cells.add(newCell);
                target = newCell;
            }
            cellTable.put(e, target);
            return target;
        });
    }

    @Override
    public ComputationRow<Dep, Result, MsgT> getRow(Result event, ComputationRow<? super Result, ?, ?> requester) {
        return getCellForEvent(event).getRow(event, requester);
    }

    @Override
    public float get(Result event) {
        return getCellForEvent(event).get(event);
    }

    @Override
    public void passMessage(Result event, MsgT msg) {
        getCellForEvent(event).passMessage(event, msg);
    }
}