package computation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

interface Formula<Dep extends Dependency> {
    Set<Dep> getDeps();

    float compute(Function<Dep, Float> resolveDependencies);
}

interface FormulaProvider<Dep extends Dependency, Result extends Event> {
    Formula<Dep> get(Result event);
}

class ConstantFormulaProvider<Result extends Event> implements FormulaProvider<None, Result> {
    private final float constantVal;
    ConstantFormulaProvider(float constantVal) {
        this.constantVal = constantVal;
    }

    @Override
    public Formula<None> get(Result ignoredEvent) {
        return new Formula<>() {
            @Override
            public Set<None> getDeps() {
                return new HashSet<>();
            }

            @Override
            public float compute(Function<None, Float> ignoredResolveDependencies) {
                return constantVal;
            }
        };
    }
}

class ErrorFormulaProvider<Result extends Event> implements FormulaProvider<None, Result> {
        @Override
    public Formula<None> get(Result event) {
        return new Formula<>() {
            @Override
            public Set<None> getDeps() {
                throw new IllegalStateException("This formula (" + this + ") should never be called");
            }

            @Override
            public float compute(Function<None, Float> resolveDependencies) {
                throw new IllegalStateException("This formula (" + this + ") should never be called");
            }
        };
    }
}

interface TotalFormulaProvider {
    FormulaProvider<PiOrPhi, Phi> phiFormulaProvider();
    FormulaProvider<PiOrPhi, Beta> betaFormulaProvider();
    FormulaProvider<BetaOrEta, Eta> etaFormulaProvider();
    FormulaProvider<AlphaOrBeta, Alpha> alphaFormulaProvider();
    FormulaProvider<AlphaOrBetaOrEta, Omega> omegaFormulaProvider();

    /**
     * This formula dictates how to compute a new value for a line correctness given that a certain
     * assertion passed
     * @return
     */
    FormulaProvider<OmegaOrLine, LineAssertionPair> lineUpdateFormulaProvider();
    FormulaProvider<OmegaOrLine, Assertion> assertionFormulaProvider();

    FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider();

}