package serializable;

import analyzer.formulaproviders.FrequencyProvider;
import analyzer.formulaproviders.LineUpdateProvider;
import core.codemodel.events.*;
import core.dependencies.*;
import core.formula.Formula;
import core.formula.FormulaProvider;
import core.formula.TotalFormulaProvider;

import java.io.Serializable;
import java.util.HashMap;

public record SerialFormulas(HashMap<Event, Formula<? extends Dependency>> data) implements Serializable, TotalFormulaProvider {

    @SuppressWarnings("unchecked")
    private <Dep extends Dependency, Result extends ComputedEvent> FormulaProvider<Dep, Result> getProvider() {
        return event -> {
            if (!data.containsKey(event)) {
               throw new IllegalStateException("The event " + event + " was requested but not present");
            }
            return (Formula<Dep>) data.get(event);
        };
    }

    @Override
    public FormulaProvider<PiOrPhi, Phi> phiFormulaProvider() {
        return getProvider();
    }

    @Override
    public FormulaProvider<PiOrPhi, Beta> betaFormulaProvider() {
        return getProvider();
    }

    @Override
    public FormulaProvider<BetaOrEta, Eta> etaFormulaProvider() {
        return getProvider();
    }

    @Override
    public FormulaProvider<AlphaOrBeta, Alpha> alphaFormulaProvider() {
        return getProvider();
    }

    @Override
    public FormulaProvider<AlphaOrBetaOrEta, Omega> omegaFormulaProvider() {
        return getProvider();
    }

    @Override
    public FormulaProvider<OmegaOrLine, LineAssertionPair> lineUpdateFormulaProvider() {
        return new LineUpdateProvider();
    }

    @Override
    public FormulaProvider<OmegaOrLine, Assertion> assertionFormulaProvider() {
        return getProvider();
    }

    @Override
    public FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider() {
        return new FrequencyProvider();
    }
}
