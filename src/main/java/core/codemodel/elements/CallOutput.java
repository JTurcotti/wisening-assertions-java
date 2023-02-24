package core.codemodel.elements;

import core.codemodel.types.BlameSite;

public record CallOutput(Call call, PhiOutput output) implements BlameSite {
}
