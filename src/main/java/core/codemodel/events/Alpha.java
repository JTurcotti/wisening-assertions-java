package core.codemodel.events;

import core.codemodel.elements.PhiInput;
import core.codemodel.elements.Procedure;
import core.dependencies.AlphaOrBeta;
import core.dependencies.AlphaOrBetaOrEta;

public record Alpha(Line line, Procedure procedure, PhiInput input) implements Event, AlphaOrBeta, AlphaOrBetaOrEta {
}
