package analyzer.formulaproviders;

import analyzer.Config;
import core.codemodel.events.Assertion;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.Set;
import java.util.function.Function;

/*
This one is a little confusing: a FrequencyProvider takens an assertion, queries that assertion
for its probability of correctness, and returns the frequency with which it should execute.
 */
public record FrequencyProvider() implements FormulaProvider<Assertion, Assertion> {
    @Override
    public Formula<Assertion> get(Assertion assertion) {
        return new Formula<>() {
            @Override
            public Set<Assertion> getDeps() {
                return Set.of(assertion);
            }

            @Override
            public float compute(Function<Assertion, Float> resolveDependencies) {
                float probCorrect = resolveDependencies.apply(assertion);
                return (Config.DESIRED_CORRECTNESS - probCorrect) / (1 - probCorrect);
            }
        };
    }
}
