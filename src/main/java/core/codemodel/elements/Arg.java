package core.codemodel.elements;

import core.codemodel.events.Phi;

public record Arg(Procedure procedure, int num) implements Phi.Input {
}
