package supervisor;

import core.codestructure.events.*;
import core.dependencies.None;
import core.dependencies.OmegaOrLine;
import core.formula.FormulaProvider;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static supervisor.Config.BRANCH_MONITORING_WINDOW_SIZE;

interface MessageProcessorProducer<Result extends Event, MsgT> {
    MessageProcessor<MsgT> produce(Result event);
}

interface MessageProcessor<MsgT> {
    void passMessage(MsgT msg);

    float processMessages(float oldVal);
}

class NoopMessageProcessor implements MessageProcessor<None> {
    NoopMessageProcessor(Event ignoredEvent) {}

    @Override
    public void passMessage(None msg) {/*noop*/}

    @Override
    public float processMessages(float oldVal) {
        return oldVal;
    }
}

record BranchTaken(boolean direction) {}

class BranchMessageProcessor implements MessageProcessor<BranchTaken> {
    private int currWindowSize = 0;
    private float currWindowTrueCount = 0.0f;
    private final Queue<Boolean> window = new LinkedList<>();

    BranchMessageProcessor(Pi ignoredPi) {/* noop: branch prediction behavior is same for all branches */}

    @Override
    public synchronized void passMessage(BranchTaken msg) {
        if (currWindowSize == BRANCH_MONITORING_WINDOW_SIZE) {
            if (window.remove()) {
                currWindowTrueCount--;
            }
        } else {
            currWindowSize++;
        }
        window.add(msg.direction());
        if (msg.direction()) {
            currWindowTrueCount++;
        }
    }

    @Override
    public float processMessages(float oldVal) {
        return currWindowSize > 0? currWindowTrueCount / currWindowSize: oldVal;
    }
}

record AssertionPass(Assertion assertion) {}

class AssertionPassMessageProcessor implements MessageProcessor<AssertionPass> {
    private final Queue<Assertion> assertionPasses = new ConcurrentLinkedQueue<>();
    private final FormulaProvider<OmegaOrLine, LineAssertionPair> formulaProvider;
    private final ComputationNetwork parentNetwork;
    private final Line line;

    AssertionPassMessageProcessor(FormulaProvider<OmegaOrLine, LineAssertionPair> formulaProvider,
                                  ComputationNetwork parentNetwork,
                                  Line line) {
        this.formulaProvider = formulaProvider;
        this.parentNetwork = parentNetwork;
        this.line = line;
    }

    @Override
    public void passMessage(@NotNull AssertionPass msg) {
        assertionPasses.add(msg.assertion());
    }

    @Override
    public float processMessages(float oldVal) {
        while (!assertionPasses.isEmpty()) {
            oldVal = formulaProvider
                    .get(new LineAssertionPair(line, assertionPasses.remove()))
                    .compute(parentNetwork::get);
        }
        return oldVal;
    }
}