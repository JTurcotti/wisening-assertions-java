package core.codestructure.events;

import core.codestructure.elements.Procedure;
import core.dependencies.AlphaOrBeta;
import core.dependencies.AlphaOrBetaOrEta;

public record Alpha(Line line, Procedure procedure, int num) implements Event, AlphaOrBeta, AlphaOrBetaOrEta {
}
