package core.formula;

import core.codemodel.events.Event;
import core.dependencies.Dependency;

import java.util.Set;
import java.util.function.Function;

public record ErrorFormulaProvider<Dep extends Dependency, Result extends Event>(String msg) implements FormulaProvider<Dep, Result> {
    @Override
    public Formula<Dep> get(Result event) {
        return new Formula<>() {
            @Override
            public Set<Dep> getDeps() {
                throw new IllegalStateException("This formula (" + this + ") should never be called: " + msg);
            }

            @Override
            public float compute(Function<Dep, Float> resolveDependencies) {
                throw new IllegalStateException("This formula (" + this + ") should never be called: " + msg);
            }
        };
    }
}
