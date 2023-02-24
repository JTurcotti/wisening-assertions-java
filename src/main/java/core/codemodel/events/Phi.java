package core.codemodel.events;

import core.codemodel.elements.PhiInput;
import core.codemodel.elements.PhiOutput;
import core.codemodel.elements.Procedure;
import core.codemodel.types.IntraflowEvent;
import core.dependencies.PiOrPhi;

public record Phi(Procedure procedure, PhiInput in, PhiOutput out) implements Event, PiOrPhi, IntraflowEvent.AtomicEvent { }
