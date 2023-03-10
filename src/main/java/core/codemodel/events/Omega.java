package core.codemodel.events;

import core.dependencies.OmegaOrLine;

public record Omega(Assertion assertion, Line line) implements Event, OmegaOrLine, ComputedEvent {
}
