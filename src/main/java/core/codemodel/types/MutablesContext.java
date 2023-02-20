package core.codemodel.types;

import core.codemodel.events.Pi;
import util.Util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MutablesContext {

    final Map<Mutable, Blame> data;

    private MutablesContext() {
        data = Map.of();
    }

    private MutablesContext(Map<Mutable, Blame> data) {
        this.data = data.keySet().stream().collect(Collectors.toUnmodifiableMap(Function.identity(), data::get));
    }

    public static MutablesContext empty() {
        return new MutablesContext();
    }

    public static MutablesContext assignment(Mutable t, Blame b) {
        return new MutablesContext(Map.of(t, b));
    }

    public MutablesContext conjunctIntraflow(IntraflowEvent flow) {
        return new MutablesContext(Util.mapImmutableMap(data, blame -> blame.conjunctIntraflow(flow)));
    }

    public MutablesContext conjunctPi(Pi pi, boolean direction) {
        return new MutablesContext(Util.mapImmutableMap(data, blame -> blame.conjunctPi(pi, direction)));
    }

    public MutablesContext disjunct(MutablesContext other) {
        return new MutablesContext(Util.mergeMaps(data, other.data, Blame::disjunct));
    }
}
