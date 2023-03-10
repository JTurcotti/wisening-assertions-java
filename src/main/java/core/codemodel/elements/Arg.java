package core.codemodel.elements;

import java.io.Serializable;

public record Arg(Procedure procedure, int num) implements PhiInput, Serializable {
}
