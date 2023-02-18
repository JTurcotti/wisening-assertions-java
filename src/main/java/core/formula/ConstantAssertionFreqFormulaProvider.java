package core.formula;

import core.codemodel.events.*;
import core.dependencies.*;

public record ConstantAssertionFreqFormulaProvider(float assertionFreq) implements TotalFormulaProvider {
    @Override
    public FormulaProvider<PiOrPhi, Phi> phiFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<PiOrPhi, Beta> betaFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<BetaOrEta, Eta> etaFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<AlphaOrBeta, Alpha> alphaFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<AlphaOrBetaOrEta, Omega> omegaFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<OmegaOrLine, LineAssertionPair> lineUpdateFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<OmegaOrLine, Assertion> assertionFormulaProvider() {
        return new ErrorFormulaProvider<>();
    }

    @Override
    public FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider() {
        return new ConstantFormulaProvider<>(assertionFreq);
    }
}