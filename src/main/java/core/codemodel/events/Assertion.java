package core.codemodel.events;

import core.codemodel.elements.BetaSite;
import core.dependencies.Dependency;

public record Assertion(int num) implements Event, Dependency, BetaSite {
}
