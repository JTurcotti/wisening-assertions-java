package core.codemodel.types;

import core.codemodel.events.Pi;

public record SignedPi(Pi pi, boolean sign) implements IntraflowEvent.AtomicEvent {
    public SignedPi opp() {
        return new SignedPi(pi, !sign);
    }
}
