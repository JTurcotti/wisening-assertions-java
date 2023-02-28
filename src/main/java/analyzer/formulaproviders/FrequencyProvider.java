package analyzer.formulaproviders;

import analyzer.Config;
import analyzer.ProgramAnalyzer;
import core.codemodel.events.Assertion;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.Set;
import java.util.function.Function;

record FrequencyProvider(ProgramAnalyzer analyzer) implements FormulaProvider<Assertion, Assertion> {
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
