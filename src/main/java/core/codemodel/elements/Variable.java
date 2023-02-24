package core.codemodel.elements;

import core.codemodel.types.BlameSite;

public record Variable(int num) implements BlameSite, Mutable, ClosedOver, PhiInput, PhiOutput {
}
