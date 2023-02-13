package computation;

import org.jetbrains.annotations.NotNull;

import static computation.Config.*;

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
}

public class ComputationNetwork {

    final RowProvider<None, Pi, BranchTaken> piComputationCells;
    final RowProvider<PiOrPhi, Phi, None> phiComputationCells;
    final RowProvider<PiOrPhi, Beta, None> betaComputationCells;
    final RowProvider<BetaOrEta, Eta, None> etaComputationCells;
    final RowProvider<AlphaOrBeta, Alpha, None> alphaComputationCells;
    final RowProvider<AlphaOrBetaOrEta, Omega, None> omegaComputationCells;
    final RowProvider<None, Line, AssertionPass> lineComputationCells;
    final RowProvider<OmegaOrLine, Assertion, None> assertionComputationCells;


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
            default -> throw new IllegalStateException("Unexpected Event value: " + event);
        };
    }

    <Result extends Event> ComputationRow<?, Result, ?> getRow(Result event, ComputationRow<? super Result, ?, ?>requester) {
        return getCellGroup(event).getRow(event, requester);
    }

    public float get(Event event) {
        return getCellGroup(event).get(event);
    }
}
