package core.codemodel.events;

import core.codemodel.elements.Procedure;
import core.codemodel.types.Intraflow;
import core.dependencies.PiOrPhi;

public record Phi(Procedure procedure, Input in, Output out) implements Event, PiOrPhi, Intraflow.AtomicEvent {
    public interface Input {}
    public interface Output {}
}
