package core.codemodel.types;

import core.codemodel.elements.Mutable;
import core.codemodel.events.Phi;
import core.codemodel.events.Pi;
import util.Util;

import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class Blame {
    private final Map<BlameSite, IntraflowEvent> data;

    private Blame() {
        data = Map.of();
    }

    private Blame(Map<BlameSite, IntraflowEvent> data) {
        this.data = Util.copyImmutableMap(data);
    }

    public static Blame zero() {
        return new Blame();
    }

    public static Blame oneSite(BlameSite blameSite) {
        return new Blame(Map.of(blameSite, IntraflowEvent.one()));
    }
    public static Blame oneMutable(Mutable mutable) {
        return oneSite(Mutable.asBlameSite(mutable));
    }

    public Blame conjunctPhi(Phi phi) {
        return new Blame(Util.mapImmutableMap(data, flow -> flow.conjunctPhi(phi)));
    }

    public Blame conjunctPi(Pi pi, boolean sign) {
        return new Blame(Util.mapImmutableMap(data, flow -> flow.conjunctPi(pi, sign)));
    }

    public Blame conjunctIntraflow(IntraflowEvent flow) {
        return new Blame(Util.mapImmutableMap(data, flow::conjunct));
    }

    public Blame disjunct(Blame other) {
        return new Blame(Util.mergeMaps(data, other.data, IntraflowEvent::disjunct));
    }

    public static Blame conjunctListWithPhi(List<Blame> blames, IntFunction<Phi> phiGenerator) {
        return IntStream.range(0, blames.size())
                .mapToObj(i -> blames.get(i).conjunctPhi(phiGenerator.apply(i)))
                .reduce(Blame.zero(), Blame::disjunct);
    }
}
