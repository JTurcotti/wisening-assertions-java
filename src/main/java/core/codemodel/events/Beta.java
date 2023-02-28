package core.codemodel.events;

import core.codemodel.elements.BetaSite;
import core.codemodel.elements.Procedure;
import core.codemodel.types.BlameSite;
import core.dependencies.AlphaOrBeta;
import core.dependencies.AlphaOrBetaOrEta;
import core.dependencies.BetaOrEta;

public record Beta(Procedure procedure, BlameSite in, BetaSite out) implements Event, AlphaOrBeta, BetaOrEta, AlphaOrBetaOrEta {

}
