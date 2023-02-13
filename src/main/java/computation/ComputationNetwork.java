package computation;

interface ComputationRow<Dep extends Dependency, Result extends Event> {
    float getVal();
    void notifyDependeesUpdated();
    void notifyNoLongerDependee(ComputationRow<? super Result, ?> formerDependerRow);
}

interface RowProvider<Dep extends Dependency, Result extends Event> {
    float get(Result event);
    ComputationRow<Dep, Result> getRow(Result event, ComputationRow<? super Result, ?> requester);
}

public class ComputationNetwork {

    final ComputationCellGroup<None, Pi> piComputationCells;
    final ComputationCellGroup<PiOrPhi, Phi> phiComputationCells;
    final ComputationCellGroup<PiOrPhi, Beta> betaComputationCells;
    final ComputationCellGroup<BetaOrEta, Eta> etaComputationCells;
    final ComputationCellGroup<AlphaOrBeta, Alpha> alphaComputationCells;
    final ComputationCellGroup<AlphaOrBetaOrEta, Omega> omegaComputationCells;


    public ComputationNetwork(TotalFormulaProvider formulaProvider) {
        piComputationCells = new ComputationCellGroup<>(this, Pi.defaultValue, formulaProvider.piFormulaProvider());
        phiComputationCells = new ComputationCellGroup<>(this, Phi.defaultValue, formulaProvider.phiFormulaProvider());
        betaComputationCells = new ComputationCellGroup<>(this, Beta.defaultValue, formulaProvider.betaFormulaProvider());
        etaComputationCells = new ComputationCellGroup<>(this, Eta.defaultValue, formulaProvider.etaFormulaProvider());
        alphaComputationCells = new ComputationCellGroup<>(this, Alpha.defaultValue, formulaProvider.alphaFormulaProvider());
        omegaComputationCells = new ComputationCellGroup<>(this, Omega.defaultValue, formulaProvider.omegaFormulaProvider());
    }


    @SuppressWarnings("unchecked")
    private <Result extends Event> ComputationCellGroup<?, Result> getCellGroup(Result event) {
        return switch (event) {
            case Pi ignored -> ((ComputationCellGroup<?, Result>) piComputationCells);
            case Phi ignored -> ((ComputationCellGroup<?, Result>) phiComputationCells);
            case Beta ignored -> ((ComputationCellGroup<?, Result>) betaComputationCells);
            case Eta ignored -> ((ComputationCellGroup<?, Result>) etaComputationCells);
            case Alpha ignored -> ((ComputationCellGroup<?, Result>) alphaComputationCells);
            case Omega ignored -> ((ComputationCellGroup<?, Result>) omegaComputationCells);
            default -> throw new IllegalStateException("Unexpected Event value: " + event);
        };
    }

    <Result extends Event> ComputationRow<?, Result> getRow(Result event, ComputationRow<? super Result, ?>requester) {
        return getCellGroup(event).getRow(event, requester);
    }

    public float get(Event event) {
        return getCellGroup(event).get(event);
    }
}
