package core.codemodel.elements;

import core.codemodel.events.Phi;
import core.codemodel.types.BlameSite;

public record Self() implements Phi.Input, Phi.Output, ClosedOver, BlameSite {
}
