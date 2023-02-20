package core.codemodel.types;

import core.codemodel.events.Pi;

record SignedPi(Pi pi, boolean sign) implements IntraflowEvent.AtomicEvent {
}
