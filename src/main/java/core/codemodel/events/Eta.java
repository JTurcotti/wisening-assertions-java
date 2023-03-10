package core.codemodel.events;

import core.codemodel.elements.PhiOutput;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Ret;
import core.dependencies.AlphaOrBetaOrEta;
import core.dependencies.BetaOrEta;

public record Eta(Procedure src, PhiOutput srcOutput, Procedure tgt, PhiOutput tgtOutput)
        implements Event, BetaOrEta, AlphaOrBetaOrEta, ComputedEvent {

    public static Eta mapTgtToProc(Eta eta, Procedure newTgt) {
        return new Eta(eta.src, eta.srcOutput, newTgt, PhiOutput.mapToProc(eta.tgtOutput, newTgt));
    }
}
