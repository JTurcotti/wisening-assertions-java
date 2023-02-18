package core.codemodel.types;

import util.Util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MutablesContext {
    public interface Mutable {}

    private final Map<Mutable, Blame> data;

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

    public MutablesContext conjunctIntraflow(Intraflow flow) {
        return new MutablesContext(Util.mapImmutableMap(data, blame -> blame.conjunctIntraflow(flow)));
    }
}
