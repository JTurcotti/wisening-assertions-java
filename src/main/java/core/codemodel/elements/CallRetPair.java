package core.codemodel.elements;

import core.codemodel.types.Blame;

public record CallRetPair(Call call, Ret ret) implements Blame.Site {
}
