package core.codemodel.events;

import core.dependencies.Dependency;

import java.util.Optional;

/*
ComputedEvents are events that have formulas to computed them - see analyzer.formulaproviders
 */
public sealed interface ComputedEvent extends Event permits Alpha, Assertion, Beta, Eta, Omega, Phi {
    static ComputedEvent ofEvent(Event e) {
        if (e instanceof ComputedEvent ce) {
            return ce;
        }
        throw new IllegalArgumentException("Event " + e + " cannot be converted to computed event");
    }

    static Optional<ComputedEvent> ofDependencyOpt(Dependency d) {
        if (d instanceof ComputedEvent ce) {
            return Optional.of(ce);
        }
        return Optional.empty();
    }
}
