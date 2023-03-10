package core.formula;

import core.dependencies.Dependency;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;

public interface Formula<Dep extends Dependency> extends Serializable {
    Set<Dep> getDeps();

    float compute(Function<Dep, Float> resolveDependencies);
}

