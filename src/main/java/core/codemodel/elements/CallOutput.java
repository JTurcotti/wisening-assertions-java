package core.codemodel.elements;

import core.codemodel.types.BlameSite;

import java.io.Serializable;

public record CallOutput(Call call, PhiOutput output) implements BlameSite, Serializable {
}
