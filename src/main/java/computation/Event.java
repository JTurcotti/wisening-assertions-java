package computation;

interface Event {}

interface Dependency extends Event {}

interface PiOrPhi extends Dependency {}

interface BetaOrEta extends Dependency {}

interface AlphaOrBeta extends Dependency {}

interface AlphaOrBetaOrEta extends Dependency {}

interface None extends Dependency {}

class Pi implements Event, PiOrPhi {
    static final float defaultValue = 0.5f;
}
class Phi implements Event, PiOrPhi {
    static final float defaultValue = 0.0f;
}

class Beta implements Event, AlphaOrBeta, BetaOrEta, AlphaOrBetaOrEta {
    static final float defaultValue = 0.0f;
}

class Alpha implements Event, AlphaOrBeta, AlphaOrBetaOrEta {
    static final float defaultValue = 0.0f;
}

class Eta implements Event, BetaOrEta, AlphaOrBetaOrEta {
    static final float defaultValue = 0.0f;
}

class Omega implements Event {
    static final float defaultValue = 0.0f;
}
