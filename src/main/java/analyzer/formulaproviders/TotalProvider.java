package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.*;
import core.dependencies.*;
import core.formula.FormulaProvider;
import core.formula.TotalFormulaProvider;

public class TotalProvider implements TotalFormulaProvider {
    public TotalProvider(ProgramAnalyzer analyzer) {
        this.phiProvider = new PhiProvider(analyzer);
        this.betaProvider = new BetaProvider(analyzer);
        this.etaProvider = new EtaProvider(analyzer);
        this.alphaProvider = new AlphaProvider(analyzer);
        this.omegaProvider = new OmegaProvider(analyzer);
        this.lineUpdateProvider = new LineUpdateProvider(analyzer);
        this.assertionProvider = new AssertionProvider(analyzer);
        this.assertionCorrectnessToFrequencyProvider = new FrequencyProvider(analyzer);
    }
    private final FormulaProvider<PiOrPhi, Phi> phiProvider;
    private final FormulaProvider<PiOrPhi, Beta> betaProvider;
    private final FormulaProvider<BetaOrEta, Eta> etaProvider;
    private final FormulaProvider<AlphaOrBeta, Alpha> alphaProvider;
    private final FormulaProvider<AlphaOrBetaOrEta, Omega> omegaProvider;
    private final FormulaProvider<OmegaOrLine, LineAssertionPair> lineUpdateProvider;

    private final FormulaProvider<OmegaOrLine, Assertion> assertionProvider;

    private final FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider;

    @Override
    public FormulaProvider<PiOrPhi, Phi> phiFormulaProvider() {
        return phiProvider;
    }

    @Override
    public FormulaProvider<PiOrPhi, Beta> betaFormulaProvider() {
        return betaProvider;
    }

    @Override
    public FormulaProvider<BetaOrEta, Eta> etaFormulaProvider() {
        return etaProvider;
    }

    @Override
    public FormulaProvider<AlphaOrBeta, Alpha> alphaFormulaProvider() {
        return alphaProvider;
    }

    @Override
    public FormulaProvider<AlphaOrBetaOrEta, Omega> omegaFormulaProvider() {
        return omegaProvider;
    }

    @Override
    public FormulaProvider<OmegaOrLine, LineAssertionPair> lineUpdateFormulaProvider() {
        return lineUpdateProvider;
    }

    @Override
    public FormulaProvider<OmegaOrLine, Assertion> assertionFormulaProvider() {
        return assertionProvider;
    }

    @Override
    public FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider() {
        return assertionCorrectnessToFrequencyProvider;
    }
}
