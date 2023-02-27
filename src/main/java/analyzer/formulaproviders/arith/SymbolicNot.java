package analyzer.formulaproviders.arith;

import core.dependencies.Dependency;
import core.formula.Formula;

import java.util.Set;
import java.util.function.Function;

public record SymbolicNot<Dep extends Dependency>(Formula<Dep> operand) implements Formula<Dep> {
    @Override
    public Set<Dep> getDeps() {
        return operand.getDeps();
    }

    @Override
    public float compute(Function<Dep, Float> resolveDependencies) {
        return 1.0f - operand.compute(resolveDependencies);
    }

    public static <D extends Dependency> Formula<D> symbolicSign(Formula<D> operand, boolean sign) {
        if (sign) {
            return operand;
        } else {
            return new SymbolicNot<>(operand);
        }
    }
}
