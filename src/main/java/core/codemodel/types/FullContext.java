package core.codemodel.types;

import core.codemodel.elements.*;
import core.codemodel.events.Pi;
import util.Pair;
import util.Util;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record FullContext(
        MutablesContext mutables,
        PcStack pcNecessary,
        IntraflowEvent pcExact,
        Map<PhiOutput, Blame> resultBlames
) {

    /*
    Generate an empty FullContext corresponding to the beginning of a program
     */
    public static FullContext empty() {
        return new FullContext(MutablesContext.empty(), new PcStack(), IntraflowEvent.one(), Map.of());
    }

    public static FullContext atEntry(Set<PhiInput> entrySet) {
        return new FullContext(
                MutablesContext.atEntry(entrySet.stream().map(PhiInput::assertMutable)
                        .collect(Collectors.toUnmodifiableSet())),
                new PcStack(), IntraflowEvent.one(), Map.of());
    }

    public Blame lookupMutable(Mutable mutable) {
        if (mutables.data.containsKey(mutable)) {
            return mutables.data.get(mutable);
        }
        throw new IllegalArgumentException("Context does not contain requested mutable: " + mutable);
    }

    public Blame lookupPhiOutput(PhiOutput output) {
        return switch (output) {
            case Field f -> lookupMutable(f);
            case Variable v -> lookupMutable(v);
            default -> throw new IllegalArgumentException("Requested PhiOutput cannot be looked up because it is not a mutable: " + output);
        };
    }

    public Map<PhiOutput, Blame> lookupPhiOutputs(Set<PhiOutput> outputs) {
        return outputs.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), this::lookupPhiOutput));
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
        //TODO: compress blames before assignment (merge lines with same event)
        Blame conditionedBlame = conditionBlame(blame);
        MutablesContext newMutables = mutables.disjunct(MutablesContext.assignment(mutable, conditionedBlame));
        return new FullContext(newMutables, pcNecessary, pcExact, resultBlames);
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

    /*
    Observe a return in the current context, zeroing out the possiblity of control flow passing
    this point and adding results to the resutls blame. Explicitly returned results can be passed
    in the `blames` parameter, and the `closedOver` parameter should list all variables and fields
    that the typechecking procedure is a closure over so that they can be added as results as well
     */
    public FullContext observeReturn(Map<PhiOutput, Blame> blames, Set<PhiOutput> closedOver) {
        assertReachable();

        blames = Util.mergeDisjointMaps(blames, lookupPhiOutputs(closedOver));
        blames = Util.mapImmutableMap(blames, this::conditionBlame);
        Map<PhiOutput, Blame> newResultBlames = Util.mergeMaps(blames, resultBlames, Blame::disjunct);
        return new FullContext(MutablesContext.empty(), pcNecessary, IntraflowEvent.zero(), newResultBlames);
    }

    //observe return if the current context is reachable
    public FullContext observeReturnIfReachable(Set<PhiOutput> closedOver) {
        if (!pcExact.isZero()) {
            return observeReturn(Map.of(), closedOver);
        }
        return this;
    }

    public Blame conditionAssertionBlame(Blame blame) {
        blame = conditionBlame(blame);

        for (Pair<SignedPi, Blame> pcEntry : pcNecessary.stack()) {
            //condition the blame on all branches in pcNecessary being taken as they are
            blame = blame.substPi(pcEntry.left());
        }

        return blame;
    }

    public Blame conditionCallInput(Blame blame) {
        //conjunct in the event that control flow reaches this point
        return conditionBlame(blame).conjunctIntraflow(pcExact);
    }

    public FullContext takeBranch(Pi pi, Boolean sign, Blame blame) {
        MutablesContext newMutables = mutables.conjunctPi(pi, sign);
        PcStack newPcNecessary = pcNecessary.takeBranch(pi, sign, blame);
        IntraflowEvent newPcExact = pcExact.conjunctPi(pi, sign);
        return new FullContext(newMutables, newPcNecessary, newPcExact, resultBlames);
    }

    public FullContext mergeAcrossBranch(Pi pi, FullContext other) {
        MutablesContext newMutables = mutables.disjunct(other.mutables);
        PcStack newPcNecessary = Util.doubleOptionMap(
                pcNecessary.popToBranch(pi),
                other.pcNecessary.popToBranch(pi),
                Util::assertEq
        ).orElseThrow(IllegalArgumentException::new);
        IntraflowEvent newPcExact = pcExact.disjunct(other.pcExact);
        Map<PhiOutput, Blame> newResultBlames = Util.mergeMaps(resultBlames, other.resultBlames, Blame::disjunct);
        return new FullContext(newMutables, newPcNecessary, newPcExact, newResultBlames);
    }

    public void assertReachable() {
        if (pcExact.isZero()) {
            throw new IllegalStateException("This point is unreachable and should not be");
        }
    }

    /*
    remove blame for passed mutables - used at assertion sites
     */
    public FullContext zeroMutables(Set<Mutable> toZero) {
        MutablesContext newMutables = mutables.zero(toZero);
        return new FullContext(newMutables, pcNecessary, pcExact, resultBlames);
    }

    public Blame getResult(PhiOutput out) {
        if (!resultBlames.containsKey(out)) {
            if (out instanceof Self) {
                //if an output of self is requested but not present, disjunct the blame for all fields
                return resultBlames.keySet().stream()
                        .filter(Field.class::isInstance)
                        .map(resultBlames::get)
                        .reduce(Blame.zero(), Blame::disjunct);
            }
            throw new IllegalStateException("Expected result to be present: " + out);
        }
        return resultBlames.get(out);
    }

    /*
    For any mutables that are definitely blamed by the passed blame, zero them out
    */
    public FullContext observeAssertion(Blame assertionBlame) {
        MutablesContext newMutables = mutables.observeAssertion(assertionBlame);
        return new FullContext(newMutables, pcNecessary, pcExact, resultBlames);
    }
}
