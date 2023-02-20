package core.codemodel.elements;

import analyzer.ClosureMap;
import core.codemodel.types.Blame;
import core.codemodel.types.Mutable;

public record Variable(int num) implements Blame.Site, Mutable, ClosureMap.ClosedOver {
}
