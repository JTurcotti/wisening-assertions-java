package core.codemodel.types;

import core.codemodel.events.Phi;
import core.codemodel.events.Pi;
import util.Util;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class representing intraprocedural flows - none of these methods should mutate
 */
public record IntraflowEvent(Set<Set<AtomicEvent>> dnf) {
    public interface AtomicEvent {
        static <T> T process(AtomicEvent ae, Function<SignedPi, T> piProcess, Function<Phi, T> phiProcess) {
            return switch (ae) {
                case SignedPi sp -> piProcess.apply(sp);
                case Phi phi -> phiProcess.apply(phi);
                default -> throw new IllegalArgumentException("Unexpected atomic event: " + ae);
            };
        }
    }

    private IntraflowEvent() {
        this(Set.of());
    }

    public IntraflowEvent(Set<Set<AtomicEvent>> dnf) {
        this.dnf = Set.copyOf(dnf);
    }

    public static IntraflowEvent one() {
        return new IntraflowEvent(Set.of(Set.of()));
    }

    public boolean isOne() {
        return dnf.size() == 1 && Util.choose(dnf).isEmpty();
    }

    public static IntraflowEvent zero() {
        return new IntraflowEvent();
    }

    public boolean isZero() {
        return dnf.isEmpty();
    }

    public IntraflowEvent conjunctPhi(Phi phi) {
        return new IntraflowEvent(dnf.stream()
                .map(conj -> Stream.concat(conj.stream(), Stream.of(phi))
                        .collect(Collectors.toUnmodifiableSet()))
                .collect(Collectors.toUnmodifiableSet()));
    }

    public IntraflowEvent conjunctPi(Pi pi, boolean sign) {
        return new IntraflowEvent(dnf.stream()
                .filter(conj -> conj.stream().noneMatch((new SignedPi(pi, !sign))::equals))
                .map(conj -> Stream.concat(conj.stream(), Stream.of(new SignedPi(pi, sign)))
                        .collect(Collectors.toUnmodifiableSet()))
                .collect(Collectors.toUnmodifiableSet()));
    }

    private IntraflowEvent conjunctAe(AtomicEvent ae) {
        return switch (ae) {
            case Phi phi -> conjunctPhi(phi);
            case SignedPi sp -> conjunctPi(sp.pi(), sp.sign());
            default -> throw new IllegalArgumentException("Unrecognized Atomic Event " + ae);
        };
    }

    private IntraflowEvent conjunctConj(Set<AtomicEvent> conj) {
        if (conj.isEmpty()) {
            return new IntraflowEvent(dnf);
        } else {
            AtomicEvent ae = Util.choose(conj);
            return conjunctAe(ae).conjunctConj(conj.stream().filter(s -> !s.equals(ae))
                    .collect(Collectors.toUnmodifiableSet()));
        }
    }

    private Optional<Set<AtomicEvent>> conjPairReduction(Set<AtomicEvent> left, Set<AtomicEvent> right) {
        if (left.containsAll(right)) {
            return Optional.of(right);
        }
        if (right.containsAll(left)) {
            return Optional.of(left);
        }
        Optional<AtomicEvent> left_unique_opt = left.stream().filter(ae -> !right.contains(ae)).collect(Util.asSingleton());
        Optional<AtomicEvent> right_unique_opt = right.stream().filter(ae -> !left.contains(ae)).collect(Util.asSingleton());
        if (left_unique_opt.isPresent() && right_unique_opt.isPresent() &&
                left_unique_opt.get() instanceof SignedPi spl &&
                right_unique_opt.get() instanceof SignedPi spr &&
                spl.pi().equals(spr.pi()) &&
                spl.sign() == !spr.sign()) {
            return Optional.of(left.stream().filter(ae -> !ae.equals(spl)).collect(Collectors.toUnmodifiableSet()));
        }
        return Optional.empty();
    }

    private IntraflowEvent simplify() {
        boolean updated;
        Set<Set<AtomicEvent>> dnf = this.dnf;
        outer: do {
            updated = false;
            for (Set<AtomicEvent> fst : dnf) {
                for (Set<AtomicEvent> snd : dnf) {
                    if (!fst.equals(snd)) {
                        Optional<Set<AtomicEvent>> red = conjPairReduction(fst, snd);
                        if (red.isPresent()) {
                            dnf = Stream.concat(
                                            dnf.stream().filter(conj -> !conj.equals(fst) && !conj.equals(snd)),
                                            Stream.of(red.get()))
                                    .collect(Collectors.toUnmodifiableSet());
                            updated = true;
                            continue outer;
                        }
                    }
                }
            }
        } while (updated);
        return new IntraflowEvent(dnf);
    }

    public IntraflowEvent disjunct(IntraflowEvent other) {
        return new IntraflowEvent(Stream.concat(dnf.stream(), other.dnf.stream())
                .collect(Collectors.toUnmodifiableSet()))
                .simplify();
    }

    public IntraflowEvent conjunct(IntraflowEvent other) {
        return dnf.stream().map(other::conjunctConj).reduce(zero(), IntraflowEvent::disjunct).simplify();
    }

    public IntraflowEvent substPi(SignedPi subst) {
        return new IntraflowEvent(dnf.stream().flatMap(conj ->
                conj.stream().anyMatch(ae -> ae instanceof SignedPi sp && sp.equals(subst.opp())) ?
                        Stream.empty() :
                        Stream.of(conj.stream().filter(ae -> !(ae instanceof SignedPi sp && sp.equals(subst))).collect(Collectors.toUnmodifiableSet()))
        ).collect(Collectors.toUnmodifiableSet())).simplify();
    }
}
