package core.codemodel.events;

import analyzer.ProgramAnalyzer;
import core.codemodel.elements.*;
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
