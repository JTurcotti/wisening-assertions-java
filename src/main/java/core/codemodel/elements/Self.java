package core.codemodel.elements;

import core.codemodel.types.BlameSite;

import java.io.Serializable;

public record Self() implements PhiInput, PhiOutput, ClosedOver, BlameSite, BetaSite, Serializable {
}
