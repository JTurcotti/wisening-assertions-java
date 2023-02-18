package core.formula;

import core.dependencies.Dependency;
import core.codemodel.events.Event;

public interface FormulaProvider<Dep extends Dependency, Result extends Event> {
    Formula<Dep> get(Result event);
}
