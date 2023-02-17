package core.codestructure.elements;

import core.codestructure.types.Blame;

public record CallRetPair(Call call, Ret ret) implements Blame.Site {
}
