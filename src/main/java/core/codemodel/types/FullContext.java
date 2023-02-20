package core.codemodel.types;

import core.codemodel.elements.CallArgPair;
import core.codemodel.elements.Mutable;
import core.codemodel.elements.Ret;
import core.codemodel.events.Assertion;
import core.codemodel.events.Pi;
import util.Util;

import java.util.Map;

public record FullContext(
        MutablesContext mutables,
        PcStack pcNecessary,
        IntraflowEvent pcExact,
        Map<Ret, Blame> resultBlames,
        Map<Assertion, Blame> assertionBlames,
        Map<CallArgPair, Blame> callArgBlames) {

    public FullContext() {
        this(MutablesContext.empty(), new PcStack(), IntraflowEvent.one(), Map.of(), Map.of(), Map.of());
    }

    public Blame lookupMutable(Mutable mutable) {
        if (mutables.data.containsKey(mutable)) {
            return mutables.data.get(mutable);
        }
        throw new IllegalArgumentException("Context does not contain requested mutable: " + mutable);
    }

    /*
    This might be the most subtle part of the whole type system

    Let's say a value appears to have blame B - e.g. it's a constant so B is just singleton
    blame on a line. Then the "real" blame for that line actually
    1) occurs only if control flow reaches that line (so we conjunct in pcExact)
    2) separately includes the blame for each branch in pcNecessary if either
       a) control flow reached this line or
       b) control flow did NOT reach this line because that branch was taken the wrong way
    This formula here captures that breakdown:

    B_conditioned = (pcExact /\ blame) \/ ((pcExact \/ not_taken1) /\ blame1) \/ ...
    where taken<i>, blame<i> are the entries of the pcNecessary stack
     */
    public Blame conditionBlame(Blame blame) {
        return blame.disjunct(pcNecessary.toUnconditionalBlame()).conjunctIntraflow(pcExact)
                .disjunct(pcNecessary.toConditionalBlame());
    }

    public FullContext performAssignment(Mutable mutable, Blame blame) {
        MutablesContext newMutables = mutables.disjunct(MutablesContext.assignment(mutable, blame));
        return new FullContext(newMutables, pcNecessary, pcExact, resultBlames, assertionBlames, callArgBlames);
    }

    /*
    private static List<Blame> mergeBlameLists(List<Blame> blames1, List<Blame> blames2) {
        if (blames1.isEmpty()) {
            return List.copyOf(blames2);
        }
        if (blames1.size() != blames2.size()) {
            throw new IllegalArgumentException("Blame lists disagree on nonzero sizes");
        }
        return IntStream.range(0, blames1.size())
                .mapToObj(i -> blames1.get(i).disjunct(blames2.get(i))).toList();
    }*/

    public FullContext observeReturn(Map<Ret, Blame> blames) {
        Map<Ret, Blame> newResultBlames = Util.mergeMaps(blames, resultBlames, Blame::disjunct);
        return new FullContext(mutables, pcNecessary, pcExact, newResultBlames, assertionBlames, callArgBlames);
    }

    public FullContext observeAssertion(Assertion assertion, Blame blame) {
        //TODO: condition this blame on all branches in pcNecessary being taken as they were
        if (assertionBlames.containsKey(assertion)) {
            throw new IllegalArgumentException("Provided assertion already in context");
        }
        Map<Assertion, Blame> newAssertionBlames = Util.addToImmutableMap(assertionBlames, assertion, blame);
        return new FullContext(mutables, pcNecessary, pcExact, resultBlames, newAssertionBlames, callArgBlames);
    }

    public FullContext observeCallArg(CallArgPair callArg, Blame blame) {
        //TODO: conjunct in event pcExact that flow reaches this call
        if (callArgBlames.containsKey(callArg)) {
            throw new IllegalArgumentException("Provided call arg is already in context");
        }
        Map<CallArgPair, Blame> newCallArgBlames = Util.addToImmutableMap(callArgBlames, callArg, blame);
        return new FullContext(mutables, pcNecessary, pcExact, resultBlames, assertionBlames, newCallArgBlames);
    }

    public FullContext takeBranch(Pi pi, Boolean sign, Blame blame) {
        MutablesContext newMutables = mutables.conjunctPi(pi, sign);
        PcStack newPcNecessary = pcNecessary.takeBranch(pi, sign, blame);
        IntraflowEvent newPcExact = pcExact.conjunctPi(pi, sign);
        return new FullContext(newMutables, newPcNecessary, newPcExact, resultBlames, assertionBlames, callArgBlames);
    }

    public FullContext mergeAcrossBranch(Pi pi, FullContext other) {
        MutablesContext newMutables = mutables.disjunct(other.mutables);
        PcStack newPcNecessary = Util.doubleOptionMap(
                pcNecessary.popToBranch(pi),
                other.pcNecessary.popToBranch(pi),
                Util::assertEq
        ).orElseThrow(IllegalArgumentException::new);
        IntraflowEvent newPcExact = pcExact.disjunct(other.pcExact);
        Map<Ret, Blame> newResultBlames = Util.mergeMaps(resultBlames, other.resultBlames, Blame::disjunct);
        Map<Assertion, Blame> newAssertionBlames =
                Util.mergeMaps(assertionBlames, other.assertionBlames, Blame::disjunct);
        Map<CallArgPair, Blame> newCallArgBlames =
                Util.mergeMaps(callArgBlames, other.callArgBlames, Blame::disjunct);
        return new FullContext(newMutables, newPcNecessary, newPcExact,
                newResultBlames, newAssertionBlames, newCallArgBlames);
    }
}
