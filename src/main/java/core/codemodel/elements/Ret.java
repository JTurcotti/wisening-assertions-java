package core.codemodel.elements;

import core.codemodel.events.Phi;

public record Ret(Procedure procedure, int num) implements Phi.Output {
}
