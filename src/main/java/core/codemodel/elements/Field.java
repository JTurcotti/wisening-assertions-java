package core.codemodel.elements;

import core.codemodel.types.BlameSite;

public record Field(int num) implements BlameSite, Mutable, ClosedOver, PhiInput, PhiOutput, BetaSite {
}
