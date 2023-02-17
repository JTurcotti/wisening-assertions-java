package core.formula;

import core.codestructure.events.*;
import core.dependencies.*;

public interface TotalFormulaProvider {
    FormulaProvider<PiOrPhi, Phi> phiFormulaProvider();

    FormulaProvider<PiOrPhi, Beta> betaFormulaProvider();

    FormulaProvider<BetaOrEta, Eta> etaFormulaProvider();

    FormulaProvider<AlphaOrBeta, Alpha> alphaFormulaProvider();

    FormulaProvider<AlphaOrBetaOrEta, Omega> omegaFormulaProvider();

    /**
     * This formula dictates how to compute a new value for a line correctness given that a certain
     * assertion passed
     *
     * @return
     */
    FormulaProvider<OmegaOrLine, LineAssertionPair> lineUpdateFormulaProvider();

    FormulaProvider<OmegaOrLine, Assertion> assertionFormulaProvider();

    FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider();
}
