package core.codemodel.events;

import core.codemodel.elements.PhiInput;
import core.codemodel.elements.PhiOutput;
import core.codemodel.elements.Procedure;
import core.codemodel.types.IntraflowEvent;
import core.dependencies.PiOrPhi;

public record Phi(Procedure procedure, PhiInput in, PhiOutput out)
        implements Event, PiOrPhi, IntraflowEvent.AtomicEvent, ComputedEvent {
    public Phi(Procedure procedure, PhiInput in, PhiOutput out) {
        //ProgramAnalyzer.availableInstance.ifPresent(analyzer -> PhiInput.validateInProc(in, procedure, analyzer));
        this.procedure = procedure;
        this.in = in;
        this.out = out;
    }
    public static Phi mapToProc(Phi phi, Procedure newProc) {
        return new Phi(newProc, PhiInput.mapToProc(phi.in, newProc), PhiOutput.mapToProc(phi.out, newProc));
    }

}
