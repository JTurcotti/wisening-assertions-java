package core.codestructure.elements;

import core.codestructure.types.Intraflow;

public record Call(int num) implements Intraflow.AtomicEvent {
}
