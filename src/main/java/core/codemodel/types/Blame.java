package core.codemodel.types;

import core.codemodel.events.Phi;
import core.codemodel.events.Pi;
import util.Util;

import java.util.Map;

public class Blame {
    public interface Site {}
    private final Map<Site, IntraflowEvent> data;

    private Blame() {
        data = Map.of();
    }

    private Blame(Map<Site, IntraflowEvent> data) {
        this.data = Util.copyImmutableMap(data);
    }

    public static Blame zero() {
        return new Blame();
    }

    public static Blame oneSite(Site site) {
        return new Blame(Map.of(site, IntraflowEvent.one()));
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
}
