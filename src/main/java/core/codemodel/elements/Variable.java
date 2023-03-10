package core.codemodel.elements;

import core.codemodel.types.BlameSite;

import java.io.Serializable;

public record Variable(int num) implements BlameSite, Mutable, ClosedOver, PhiInput, PhiOutput, BetaSite, Serializable {
}
