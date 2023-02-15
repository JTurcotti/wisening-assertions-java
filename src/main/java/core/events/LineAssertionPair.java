package core.events;

public record LineAssertionPair(Line line, Assertion assertion) implements Event {
}
