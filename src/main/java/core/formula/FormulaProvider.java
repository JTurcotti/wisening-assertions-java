package core.formula;

import core.codemodel.events.Event;
import core.dependencies.Dependency;

public interface FormulaProvider<Dep extends Dependency, Result extends Event> {
    Formula<Dep> get(Result event);
}
