package core.codemodel.events;

import core.dependencies.PiOrPhi;

public record Pi(int num) implements Event, PiOrPhi {
}
