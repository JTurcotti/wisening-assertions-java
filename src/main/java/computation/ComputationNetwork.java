package computation;

import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Consumer;

import static computation.Config.*;

interface ExecutionSupervisor {
    boolean executeAssertion(Assertion assertion);

    void notifyAssertionPass(Assertion assertion);

    void notifyBranchTaken(Pi branch, boolean direction);
}

interface ComputationRow<Dep extends Dependency, Result extends Event, MsgT> {
    float getVal();
    void notifyDependeesUpdated();
    void notifyNoLongerDependee(ComputationRow<? super Result, ?, ?> formerDependerRow);
    void passMessage(MsgT msg);
}

interface RowProvider<Dep extends Dependency, Result extends Event, MsgT> {
    float get(Result event);
    ComputationRow<Dep, Result, MsgT> getRow(Result event, ComputationRow<? super Result, ?, ?> requester);
    void passMessage(Result event, MsgT msg);
    void passMessageToAll(MsgT msg);
}

public class ComputationNetwork extends Thread implements ExecutionSupervisor {

    private final ComputationCellGroup<None, Pi, BranchTaken> piComputationCells;
    private final ComputationCellGroup<PiOrPhi, Phi, None> phiComputationCells;
    private final ComputationCellGroup<PiOrPhi, Beta, None> betaComputationCells;
    private final ComputationCellGroup<BetaOrEta, Eta, None> etaComputationCells;
    private final ComputationCellGroup<AlphaOrBeta, Alpha, None> alphaComputationCells;
    private final ComputationCellGroup<AlphaOrBetaOrEta, Omega, None> omegaComputationCells;
    private final ComputationCellGroup<None, Line, AssertionPass> lineComputationCells;
    private final ComputationCellGroup<OmegaOrLine, Assertion, None> assertionComputationCells;

    private void forEach(Consumer<ComputationCellGroup> action) {
        action.accept(piComputationCells);
        action.accept(phiComputationCells);
        action.accept(betaComputationCells);
        action.accept(etaComputationCells);
        action.accept(alphaComputationCells);
        action.accept(omegaComputationCells);
        action.accept(lineComputationCells);
        action.accept(assertionComputationCells);
    }

    private final FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider;

    private final Random randomness = new Random();


    public ComputationNetwork(@NotNull TotalFormulaProvider formulaProvider) {
        piComputationCells =
                new ComputationCellGroup<>(this, PI_COLD_VALUE,
                        new ErrorFormulaProvider<>(), BranchMessageProcessor::new);
        phiComputationCells =
                new ComputationCellGroup<>(this, PHI_COLD_VALUE,
                        formulaProvider.phiFormulaProvider(), NoopMessageProcessor::new);
        betaComputationCells =
                new ComputationCellGroup<>(this, BETA_COLD_VALUE,
                        formulaProvider.betaFormulaProvider(), NoopMessageProcessor::new);
        etaComputationCells =
                new ComputationCellGroup<>(this, ETA_COLD_VALUE,
                        formulaProvider.etaFormulaProvider(), NoopMessageProcessor::new);
        alphaComputationCells =
                new ComputationCellGroup<>(this, ALPHA_COLD_VALUE,
                        formulaProvider.alphaFormulaProvider(), NoopMessageProcessor::new);
        omegaComputationCells =
                new ComputationCellGroup<>(this, OMEGA_COLD_VALUE,
                        formulaProvider.omegaFormulaProvider(), NoopMessageProcessor::new);
        lineComputationCells =
                new ComputationCellGroup<>(this, LINE_CORRECTNESS_COLD_VALUE,
                        new ErrorFormulaProvider<>(),
                        line -> new AssertionPassMessageProcessor(
                                formulaProvider.lineUpdateFormulaProvider(), this, line));
        assertionComputationCells =
                new ComputationCellGroup<>(this, ASSERTION_CORRECTNESS_COLD_VALUE,
                        formulaProvider.assertionFormulaProvider(), NoopMessageProcessor::new);
        assertionCorrectnessToFrequencyProvider = formulaProvider.assertionCorrectnessToFrequencyProvider();
    }


    @SuppressWarnings("unchecked")
    private <Result extends Event> RowProvider<?, Result, ?> getCellGroup(Result event) {
        return switch (event) {
            case Pi ignored -> ((RowProvider<?, Result, ?>) piComputationCells);
            case Phi ignored -> ((RowProvider<?, Result, ?>) phiComputationCells);
            case Beta ignored -> ((RowProvider<?, Result, ?>) betaComputationCells);
            case Eta ignored -> ((RowProvider<?, Result, ?>) etaComputationCells);
            case Alpha ignored -> ((RowProvider<?, Result, ?>) alphaComputationCells);
            case Omega ignored -> ((RowProvider<?, Result, ?>) omegaComputationCells);
            case Line ignored -> ((RowProvider<?, Result, ?>) lineComputationCells);
            case Assertion ignored -> ((RowProvider<?, Result, ?>) assertionComputationCells);
            default -> throw new IllegalStateException("Unexpected Event value: " + event);
        };
    }

    <Result extends Event> ComputationRow<?, Result, ?> getRow(Result event, ComputationRow<? super Result, ?, ?>requester) {
        return getCellGroup(event).getRow(event, requester);
    }

    public float get(Event event) {
        return getCellGroup(event).get(event);
    }

    @Override
    public boolean executeAssertion(Assertion assertion) {
        float assertion_freq = assertionCorrectnessToFrequencyProvider.get(assertion).compute(this::get);
        return randomness.nextFloat() <= assertion_freq;
    }

    @Override
    public void notifyAssertionPass(Assertion assertion) {
        lineComputationCells.passMessageToAll(new AssertionPass(assertion));
    }

    @Override
    public void notifyBranchTaken(Pi branch, boolean direction) {
        piComputationCells.passMessage(branch, new BranchTaken(direction));
    }

    @Override
    public void run() {
        forEach(Thread::start);
        while (!isInterrupted()) {}
        forEach(Thread::interrupt);
    }
}
