package core.codestructure.events;

import core.codestructure.elements.Procedure;
import core.codestructure.types.Intraflow;
import core.dependencies.PiOrPhi;

public record Phi(Procedure procedure, int argNum, int retNum) implements Event, PiOrPhi, Intraflow.AtomicEvent {
}
