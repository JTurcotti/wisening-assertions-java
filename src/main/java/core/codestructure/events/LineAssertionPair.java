package core.codestructure.events;

public record LineAssertionPair(Line line, Assertion assertion) implements Event {
}
