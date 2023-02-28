package core.codemodel.events;

import core.codemodel.elements.PhiOutput;
import core.codemodel.elements.Procedure;
import core.dependencies.AlphaOrBetaOrEta;
import core.dependencies.BetaOrEta;

public record Eta(Procedure src, PhiOutput srcOutput, Procedure tgt, PhiOutput tgtOutput)
        implements Event, BetaOrEta, AlphaOrBetaOrEta {
}
