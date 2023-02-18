package core.codemodel.elements;

import analyzer.ClosureMap;
import core.codemodel.events.Phi;

public record Self() implements Phi.Input, Phi.Output, ClosureMap.ClosedOver {
}
