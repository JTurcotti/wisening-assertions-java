package core.codemodel.elements;

import java.io.Serializable;

public record Ret(Procedure procedure) implements PhiOutput, BetaSite, Serializable {
}
