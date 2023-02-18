package core.codemodel.events;

public record LineAssertionPair(Line line, Assertion assertion) implements Event {
}
