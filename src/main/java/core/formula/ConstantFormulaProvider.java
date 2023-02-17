package core.formula;

import core.codestructure.events.Event;
import core.dependencies.Dependency;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public record ConstantFormulaProvider<Dep extends Dependency, Result extends Event>(
        float constantVal) implements FormulaProvider<Dep, Result> {
    @Override
    public Formula<Dep> get(Result ignoredEvent) {
        return new Formula<>() {
            @Override
            public Set<Dep> getDeps() {
                return new HashSet<>();
            }

            @Override
            public float compute(Function<Dep, Float> ignoredResolveDependencies) {
                return constantVal;
            }
        };
    }
}
