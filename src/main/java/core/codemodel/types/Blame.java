package core.codemodel.types;

import analyzer.CtProcedure;
import analyzer.ProgramAnalyzer;
import core.codemodel.elements.*;
import core.codemodel.events.Phi;
import core.codemodel.events.Pi;
import util.Util;

import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    public Blame substPi(SignedPi subst) {
        return new Blame(Util.mapImmutableMap(data, flow -> flow.substPi(subst)));
    }

    public List<CallOutput> getBlamedOutputs() {
        return data.keySet().stream().flatMap(site ->
                        site instanceof CallOutput c? Stream.of(c): Stream.empty())
                .collect(Collectors.toList());
    }

    public List<ClosedOver> getBlamedClosedOver() {
        return data.keySet().stream().flatMap(site ->
                switch (site) {
                    case Field f -> Stream.of(f);
                    case Self s -> Stream.of(s);
                    case Variable v -> Stream.of(v);
                    default -> Stream.empty();
                }).collect(Collectors.toList());
    }

    public boolean blamesSelf() {
        return data.containsKey(new Self());
    }

    public boolean blamesSite(BlameSite site) {
        return data.containsKey(site);
    }

    public IntraflowEvent getAtSite(BlameSite site) {
        if (!data.containsKey(site)) {
            throw new IllegalStateException("Expected site to be present in blame: " + site);
        }
        return data.get(site);
    }

    public IntraflowEvent getAtInputSite(ProgramAnalyzer analyzer, PhiInput in) {
        BlameSite site = switch (in) {
            case Arg a -> {
                CtProcedure proc = analyzer.lookupProcedure(a.procedure());
                if (!proc.isMethod()) {
                    throw new IllegalStateException("Cannot lookup arg of a non-method");
                }
                if (a.num() >= proc.getNumParams()) {
                    throw new IllegalStateException("Method does not have an arg number " + a.num());
                }
                yield proc.getParamVariables().get(a.num());
            }
            case Field f -> f;
            case Self s -> s;
            case Variable v -> v;
        };
        return getAtSite(site);
    }
}
