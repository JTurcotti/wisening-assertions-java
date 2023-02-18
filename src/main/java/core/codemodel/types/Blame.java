package core.codemodel.types;

import core.codemodel.events.Phi;
import core.codemodel.events.Pi;
import util.Util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Blame {
    public interface Site {}
    private final Map<Site, Intraflow> data;

    private Blame() {
        data = Map.of();
    }

    private Blame(Map<Site, Intraflow> data) {
        this.data = Util.copyImmutableMap(data);
    }

    public static Blame zero() {
        return new Blame();
    }

    public static Blame oneSite(Site site) {
        return new Blame(Map.of(site, Intraflow.one()));
    }

    public Blame conjunctPhi(Phi phi) {
        return new Blame(Util.mapImmutableMap(data, flow -> flow.conjunctPhi(phi)));
    }

    public Blame conjunctPi(Pi pi, boolean sign) {
        return new Blame(Util.mapImmutableMap(data, flow -> flow.conjunctPi(pi, sign)));
    }

    public Blame conjunctIntraflow(Intraflow flow) {
        return new Blame(Util.mapImmutableMap(data, flow::conjunct));
    }

    public Blame disjunct(Blame other) {
        return new Blame(Stream.concat(data.keySet().stream(), other.data.keySet().stream())
                .distinct()
                .collect(Collectors.toUnmodifiableMap(Function.identity(),
                        key -> {
                            if (data.containsKey(key)) {
                                if (other.data.containsKey(key)) {
                                    return data.get(key).disjunct(other.data.get(key));
                                } else {
                                    return data.get(key);
                                }
                            } else {
                                return other.data.get(key);
                            }
                        }
                )));
    }
}
