package core.codemodel.elements;

import core.codemodel.types.BlameSite;

public record CallRetPair(Call call, Ret ret) implements BlameSite {
}
