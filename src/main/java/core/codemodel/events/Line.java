package core.codemodel.events;

import core.codemodel.types.BlameSite;
import core.dependencies.OmegaOrLine;

public record Line(int num) implements Event, OmegaOrLine, BlameSite {
}
