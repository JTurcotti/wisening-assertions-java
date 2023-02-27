package analyzer.formulaproviders.arith;

import core.dependencies.Dependency;
import core.formula.Formula;

import java.util.Set;
import java.util.function.Function;

public record SymbolicParam<Dep extends Dependency>(Dep dep) implements Formula<Dep> {
    @Override
    public Set<Dep> getDeps() {
        return Set.of(dep);
    }

    @Override
    public float compute(Function<Dep, Float> resolveDependencies) {
        return resolveDependencies.apply(dep);
    }
}
