package core.formula;

import core.dependencies.Dependency;
import core.codestructure.events.Event;

import java.util.Set;
import java.util.function.Function;

public class ErrorFormulaProvider<Dep extends Dependency, Result extends Event> implements FormulaProvider<Dep, Result> {
    @Override
    public Formula<Dep> get(Result event) {
        return new Formula<>() {
            @Override
            public Set<Dep> getDeps() {
                throw new IllegalStateException("This formula (" + this + ") should never be called");
            }

            @Override
            public float compute(Function<Dep, Float> resolveDependencies) {
                throw new IllegalStateException("This formula (" + this + ") should never be called");
            }
        };
    }
}
