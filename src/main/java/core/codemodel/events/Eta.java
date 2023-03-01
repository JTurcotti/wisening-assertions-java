package core.codemodel.events;

import core.codemodel.elements.PhiOutput;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Ret;
import core.dependencies.AlphaOrBetaOrEta;
import core.dependencies.BetaOrEta;

public record Eta(Procedure src, PhiOutput srcOutput, Procedure tgt, PhiOutput tgtOutput)
        implements Event, BetaOrEta, AlphaOrBetaOrEta {

    public static Eta mapTgtToProc(Eta eta, Procedure newTgt) {
        PhiOutput newTgtOutput = eta.tgtOutput instanceof Ret? new Ret(newTgt): eta.tgtOutput;
        return new Eta(eta.src, eta.srcOutput, newTgt, newTgtOutput);
    }
}
