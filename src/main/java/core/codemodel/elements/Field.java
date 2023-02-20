package core.codemodel.elements;

import analyzer.ClosureMap;
import core.codemodel.events.Phi;
import core.codemodel.types.Blame;
import core.codemodel.types.Mutable;

public record Field(int num) implements Blame.Site, Mutable, ClosureMap.ClosedOver, Phi.Input, Phi.Output {
}
