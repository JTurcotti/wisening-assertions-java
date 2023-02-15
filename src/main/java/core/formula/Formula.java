package core.formula;

import core.dependencies.Dependency;

import java.util.Set;
import java.util.function.Function;

public interface Formula<Dep extends Dependency> {
    Set<Dep> getDeps();

    float compute(Function<Dep, Float> resolveDependencies);
}

