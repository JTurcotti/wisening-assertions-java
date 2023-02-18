package core.codemodel.events;

import core.codemodel.types.Blame;
import core.dependencies.OmegaOrLine;

public record Line(int num) implements Event, OmegaOrLine, Blame.Site {
}
