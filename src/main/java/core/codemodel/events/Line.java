package core.codemodel.events;

import core.codemodel.types.BlameSite;
import core.dependencies.OmegaOrLine;

import java.io.Serializable;

public record Line(int num) implements Event, OmegaOrLine, BlameSite {
}
