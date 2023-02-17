package core.codestructure.types;

import core.codestructure.events.Phi;
import core.codestructure.events.Pi;
import util.Util;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

record SignedPi(Pi pi, boolean sign) implements Intraflow.AtomicEvent {}

/**
 * Class representing intraprocedural flows - none of these methods should mutate
 */
public class Intraflow {
    public interface AtomicEvent {}

    //should be an unmodifiable set
    private final Set<Set<AtomicEvent>> dnf;

    private Intraflow() {
        dnf = Set.of();
    }

    private Intraflow(Set<Set<AtomicEvent>> dnf) {
        this.dnf = Set.copyOf(dnf);
    }

    public static Intraflow one() {
        return new Intraflow(Set.of(Set.of()));
    }

    public boolean isOne() {
        return dnf.size() == 1 && Util.choose(dnf).size() == 0;
    }

    public static Intraflow zero() {
        return new Intraflow();
    }

    public boolean isZero() {
        return dnf.size() == 0;
    }

    public Intraflow conjunctPhi(Phi phi) {
        return new Intraflow(dnf.stream()
                .map(conj -> Stream.concat(conj.stream(), Stream.of(phi))
                        .collect(Collectors.toUnmodifiableSet()))
                .collect(Collectors.toUnmodifiableSet()));
    }

    public Intraflow conjunctPi(Pi pi, boolean sign) {
        return new Intraflow(dnf.stream()
                .filter(conj -> conj.stream().noneMatch((new SignedPi(pi, !sign))::equals))
                .map(conj -> Stream.concat(conj.stream(), Stream.of(new SignedPi(pi, sign)))
                        .collect(Collectors.toUnmodifiableSet()))
                .collect(Collectors.toUnmodifiableSet()));
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

    private Intraflow simplify() {
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
        return new Intraflow(dnf);
    }

    public Intraflow disjunct(Intraflow other) {
        return new Intraflow(Stream.concat(dnf.stream(), other.dnf.stream())
                .collect(Collectors.toUnmodifiableSet()))
                .simplify();
    }
}
