package analyzer.formulaproviders.arith;

import core.dependencies.Dependency;
import core.formula.Formula;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SymbolicConj<Dep extends Dependency>(List<Formula<Dep>> conjuncts) implements Formula<Dep> {
    @Override
    public Set<Dep> getDeps() {
        return conjuncts.stream().flatMap(c -> c.getDeps().stream()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public float compute(Function<Dep, Float> resolveDependencies) {
        return conjuncts.stream()
                .map(c -> c.compute(resolveDependencies))
                .reduce(1.0f, (f1, f2) -> f1 * f2);
    }
}
