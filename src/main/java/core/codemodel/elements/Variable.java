package core.codemodel.elements;

import analyzer.ClosureMap;
import core.codemodel.types.Blame;
import core.codemodel.types.MutablesContext;

public record Variable(int num) implements Blame.Site, MutablesContext.Mutable, ClosureMap.ClosedOver {
}
