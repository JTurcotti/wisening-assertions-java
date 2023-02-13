package computation;

interface Event {}

interface Dependency extends Event {}

interface PiOrPhi extends Dependency {}

interface BetaOrEta extends Dependency {}

interface AlphaOrBeta extends Dependency {}

interface AlphaOrBetaOrEta extends Dependency {}

interface OmegaOrLine extends Dependency {}

/**
 * This interface has no implementations. That's the point.
 */
interface None extends Dependency {}

class Pi implements Event, PiOrPhi {

}
class Phi implements Event, PiOrPhi {
}

class Beta implements Event, AlphaOrBeta, BetaOrEta, AlphaOrBetaOrEta {
}

class Alpha implements Event, AlphaOrBeta, AlphaOrBetaOrEta {
}

class Eta implements Event, BetaOrEta, AlphaOrBetaOrEta {
}

class Omega implements Event, OmegaOrLine {
}

class Line implements Event, OmegaOrLine {
}

class Assertion implements Event {
}

record LineAssertionPair(Line line, Assertion assertion) implements Event {}
