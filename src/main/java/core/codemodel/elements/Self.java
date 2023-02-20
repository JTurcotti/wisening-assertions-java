package core.codemodel.elements;

import core.codemodel.events.Phi;

public record Self() implements Phi.Input, Phi.Output, ClosedOver {
}
