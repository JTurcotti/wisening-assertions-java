package analyzer.formulaproviders.arith;

import core.dependencies.Dependency;
import core.formula.Formula;

import java.util.Set;
import java.util.function.Function;

public record SymbolicConstant<Dep extends Dependency>(float val) implements Formula<Dep> {
    @Override
    public Set<Dep> getDeps() {
        return Set.of();
    }

    @Override
    public float compute(Function<Dep, Float> resolveDependencies) {
        return val;
    }

    public static <D extends Dependency> Formula<D> one() {
        return new SymbolicConstant<>(1);
    }

    public static <D extends Dependency> Formula<D> zero() {
        return new SymbolicConstant<>(0);
    }
}
