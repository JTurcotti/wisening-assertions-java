package core.codestructure.events;

import core.codestructure.types.Blame;
import core.dependencies.OmegaOrLine;

public record Line(int num) implements Event, OmegaOrLine, Blame.Site {
}
