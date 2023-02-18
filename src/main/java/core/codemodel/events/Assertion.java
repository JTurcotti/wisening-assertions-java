package core.codemodel.events;

import core.dependencies.Dependency;

public record Assertion(int num) implements Event, Dependency {
}
