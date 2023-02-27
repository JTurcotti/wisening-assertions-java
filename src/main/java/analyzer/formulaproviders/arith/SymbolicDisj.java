package analyzer.formulaproviders.arith;

import core.dependencies.Dependency;
import core.formula.Formula;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SymbolicDisj<Dep extends Dependency>(List<Formula<Dep>> disjuncts) implements Formula<Dep> {
    @Override
    public Set<Dep> getDeps() {
        return disjuncts.stream().flatMap(c -> c.getDeps().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public float compute(Function<Dep, Float> resolveDependencies) {
        return 1.0f - disjuncts.stream()
                .map(c -> 1.0f - c.compute(resolveDependencies))
                .reduce(1.0f, (f1, f2) -> f1 * f2);
    }
}
