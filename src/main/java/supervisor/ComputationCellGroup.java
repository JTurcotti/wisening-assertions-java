package supervisor;

import core.codemodel.events.Event;
import core.dependencies.Dependency;
import core.formula.FormulaProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Stream;

import static supervisor.Config.COMPUTATION_CELL_GROUP_MAX_CELL_SIZE;


class ComputationCellGroup<Dep extends Dependency, Result extends Event, MsgT> extends Thread implements RowProvider<Dep, Result, MsgT>, Runnable {
    private final ComputationNetwork parentNetwork;
    private final Function<Result, Float> defaultValProducer;
    private final boolean rowsBeginInitialized;
    private final FormulaProvider<Dep, Result> formulaProvider;
    private final MessageProcessorProducer<Result, MsgT> messageProcessorProducer;

    ComputationCellGroup(ComputationNetwork parentNetwork,
                         Function<Result, Float> defaultValProducer,
                         boolean rowsBeginInitialized, FormulaProvider<Dep, Result> formulaProvider,
                         MessageProcessorProducer<Result, MsgT> messageProcessorProducer) {
        this.parentNetwork = parentNetwork;
        this.defaultValProducer = defaultValProducer;
        this.rowsBeginInitialized = rowsBeginInitialized;
        this.formulaProvider = formulaProvider;
        this.messageProcessorProducer = messageProcessorProducer;
    }

    private final List<ComputationCell<Dep, Result, MsgT>> cells = new LinkedList<>();

    private List<ComputationCell<Dep, Result, MsgT>> readCells() {
        cellsLock.readLock().lock();
        try {
            return new ArrayList<>(cells);
        } finally {
            cellsLock.readLock().unlock();
        }

    }

    private final Map<Result, ComputationCell<Dep, Result, MsgT>> cellTable = new ConcurrentHashMap<>();

    private final ReadWriteLock cellsLock = new ReentrantReadWriteLock();

    private ComputationCell<Dep, Result, MsgT> getCellForEvent(Result event) {
        cellsLock.readLock().lock();
        try {
            if (cellTable.containsKey(event)) {
                return cellTable.get(event);
            }
        } finally {
            cellsLock.readLock().unlock();
        }

        cellsLock.writeLock().lock();
        try {
            Optional<ComputationCell<Dep, Result, MsgT>> smallest = cells.isEmpty() ?
                    Optional.empty() :
                    Optional.of(Collections.min(cells, Comparator.comparingInt(ComputationCell::size)));
            ComputationCell<Dep, Result, MsgT> target;
            if (smallest.isPresent() && smallest.get().size() < COMPUTATION_CELL_GROUP_MAX_CELL_SIZE) {
                target = smallest.get();
            } else {
                ComputationCell<Dep, Result, MsgT> newCell =
                        new ComputationCell<>(
                                parentNetwork, defaultValProducer, rowsBeginInitialized,
                                formulaProvider, messageProcessorProducer);
                cells.add(newCell);
                if (isAlive()) {
                    newCell.start();
                }
                target = newCell;
            }
            cellTable.put(event, target);
            return target;
        } finally {
            cellsLock.writeLock().unlock();
        }
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
        readCells().forEach(cell -> cell.passMessageToAll(msg));
    }

    @Override
    public void run() {
        readCells().forEach(Thread::start);
        while (!isInterrupted()) {/*noop - intentional infinite loop */}
        readCells().forEach(Thread::interrupt);
    }

    //mostly for debugging purposes
    public void performCycle() {
        readCells().forEach(ComputationCell::performCycle);
    }

    Stream<Float> streamValues() {
        return readCells().stream().flatMap(ComputationCell::streamValues);
    }

    Stream<Map.Entry<Result, ComputationCell<Dep, Result, MsgT>.Row>> streamRows() {
        return readCells().stream().flatMap(ComputationCell::streamRows);
    }

    public long numActive() {
        return readCells().stream().mapToLong(ComputationCell::numActive).sum();
    }

    @Override
    public String toString() {
        return "ComputationCellGroup[" + streamValues().count() + " values; " + numActive() + " active]";
    }
}