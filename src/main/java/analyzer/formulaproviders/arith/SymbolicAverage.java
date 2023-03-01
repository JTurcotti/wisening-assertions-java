package analyzer.formulaproviders.arith;

import core.dependencies.Dependency;
import core.formula.Formula;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SymbolicAverage<Dep extends Dependency>(List<Formula<Dep>> operands) implements Formula<Dep> {
    @Override
    public Set<Dep> getDeps() {
        return operands.stream().flatMap(c -> c.getDeps().stream()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public float compute(Function<Dep, Float> resolveDependencies) {
        if (operands.isEmpty()) {
            throw new IllegalArgumentException("Don't create a symbolic average of 0 operands");
        }
        return operands.stream()
                .map(o -> o.compute(resolveDependencies))
                .reduce(0.0f, Float::sum) / operands.size();
    }
}
