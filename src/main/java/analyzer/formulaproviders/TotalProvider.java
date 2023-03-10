package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.*;
import core.dependencies.*;
import core.formula.Formula;
import core.formula.FormulaProvider;
import core.formula.TotalFormulaProvider;

public class TotalProvider implements TotalFormulaProvider {
    public TotalProvider(ProgramAnalyzer analyzer) {
        this.phiProvider = new PhiProvider(analyzer);
        this.betaProvider = new BetaProvider(analyzer);
        this.etaProvider = new EtaProvider(analyzer);
        this.alphaProvider = new AlphaProvider(analyzer);
        this.omegaProvider = new OmegaProvider(analyzer);
        this.lineUpdateProvider = new LineUpdateProvider();
        this.assertionProvider = new AssertionProvider(analyzer);
        this.assertionCorrectnessToFrequencyProvider = new FrequencyProvider();
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

    @SuppressWarnings("unchecked")
    private <Dep extends Dependency, Result extends ComputedEvent> FormulaProvider<Dep, Result> unsafeFormulaProvider() {
        return event -> switch (event) {
                    case Alpha a -> (Formula<Dep>) alphaProvider.get(a);
                    case Assertion a -> (Formula<Dep>) assertionProvider.get(a);
                    case Beta b -> (Formula<Dep>) betaProvider.get(b);
                    case Eta e -> (Formula<Dep>) etaProvider.get(e);
                    case Omega o -> (Formula<Dep>) omegaProvider.get(o);
                    case Phi p -> (Formula<Dep>) phiProvider.get(p);
            };
    }

    /*
    I don't know why this is necessary to declare in two methods - it's annoying, perhaps investigate?
     */
    public FormulaProvider<? extends Dependency, ComputedEvent> genericFormulaProvider() {
        return event -> unsafeFormulaProvider().get(event);
    }
}
