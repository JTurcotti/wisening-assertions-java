package core.codemodel.events;

import core.codemodel.elements.*;
import core.codemodel.types.IntraflowEvent;
import core.dependencies.PiOrPhi;

public record Phi(Procedure procedure, PhiInput in, PhiOutput out) implements Event, PiOrPhi, IntraflowEvent.AtomicEvent {
    public static Phi mapToProc(Phi phi, Procedure newProc) {
        PhiInput newInput = phi.in instanceof Arg a? new Arg(newProc, a.num()): phi.in;
        PhiOutput newOutput = phi.out instanceof Ret? new Ret(newProc): phi.out;
        return new Phi(newProc, newInput, newOutput);
    }

}
