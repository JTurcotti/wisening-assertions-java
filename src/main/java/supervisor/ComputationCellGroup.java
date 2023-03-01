package supervisor;

import core.codemodel.events.Event;
import core.dependencies.Dependency;
import core.formula.FormulaProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static supervisor.Config.COMPUTATION_CELL_GROUP_MAX_CELL_SIZE;


class ComputationCellGroup<Dep extends Dependency, Result extends Event, MsgT> extends Thread implements RowProvider<Dep, Result, MsgT>, Runnable {
    private final ComputationNetwork parentNetwork;
    private final float defaultVal;
    private final boolean rowsBeginInitialized;
    private final FormulaProvider<Dep, Result> formulaProvider;
    private final MessageProcessorProducer<Result, MsgT> messageProcessorProducer;

    ComputationCellGroup(ComputationNetwork parentNetwork,
                         float defaultVal,
                         boolean rowsBeginInitialized, FormulaProvider<Dep, Result> formulaProvider,
                         MessageProcessorProducer<Result, MsgT> messageProcessorProducer) {
        this.parentNetwork = parentNetwork;
        this.defaultVal = defaultVal;
        this.rowsBeginInitialized = rowsBeginInitialized;
        this.formulaProvider = formulaProvider;
        this.messageProcessorProducer = messageProcessorProducer;
    }

    private final List<ComputationCell<Dep, Result, MsgT>> cells = new ArrayList<>();

    private final Map<Result, ComputationCell<Dep, Result, MsgT>> cellTable = new ConcurrentHashMap<>();

    private ComputationCell<Dep, Result, MsgT> getCellForEvent(Result event) {
        if (cellTable.containsKey(event)) {
            return cellTable.get(event);
        }

        //guaranteed to be performed atomically by the ConcurrentHashMap implementation
        Optional<ComputationCell<Dep, Result, MsgT>> smallest = cells.isEmpty()?
                Optional.empty():
                Optional.of(Collections.min(cells, Comparator.comparingInt(ComputationCell::size)));
        ComputationCell<Dep, Result, MsgT> target;
        if (smallest.isPresent() && smallest.get().size() < COMPUTATION_CELL_GROUP_MAX_CELL_SIZE) {
            target = smallest.get();
        } else {
            ComputationCell<Dep, Result, MsgT> newCell =
                    new ComputationCell<>(
                            parentNetwork, defaultVal, rowsBeginInitialized,
                            formulaProvider, messageProcessorProducer);
            cells.add(newCell);
            if (isAlive()) {
                newCell.start();
            }
            target = newCell;
        }
        cellTable.put(event, target);
        return target;
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

    @Override
    public void passMessageToAll(MsgT msg) {
        cells.forEach(cell -> cell.passMessageToAll(msg));
    }

    @Override
    public void run() {
        cells.forEach(Thread::start);
        while (!isInterrupted()) {/*noop - intentional infinite loop */}
        cells.forEach(Thread::interrupt);
    }

    //mostly for debugging purposes
    public void performCycle() {
        new ArrayList<>(cells).forEach(ComputationCell::performCycle);
    }

    @Override
    public String toString() {
        String repr = "ComputationCellGroup[";
        for (ComputationCell<?, ?, ?> cell : cells) {
            repr += cell.toString() + ", ";
        }
        return repr + "]";
    }
}