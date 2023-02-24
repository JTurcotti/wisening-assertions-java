package core.codemodel.types;

import core.codemodel.events.Pi;
import util.Pair;
import util.Util;

import java.util.List;
import java.util.Optional;

record PcStack(List<Pair<SignedPi, Blame>> stack) {
    PcStack() {
        this(List.of());
    }

    PcStack takeBranch(Pi pi, boolean sign, Blame blame) {
        return new PcStack(
                Util.addToStreamHead(stack.stream(), new Pair<>(new SignedPi(pi, sign), blame)).toList());
    }

    Optional<PcStack> popToBranch(Pi pi) {
        for (int i = 0; i < stack.size(); i++) {
            if (stack.get(i).left().pi().equals(pi)) {
                if (i == stack.size() - 1) {
                    return Optional.of(new PcStack());
                }
                return Optional.of(new PcStack(List.copyOf(stack.subList(i + 1, stack.size()))));
            }
        }
        return Optional.empty();
    }

    Blame toUnconditionalBlame() {
        return stack.stream()
                .map(Pair::right)
                .reduce(Blame::disjunct)
                .orElseGet(Blame::zero);
    }

    Blame toConditionalBlame() {
        return stack.stream()
                .map(p -> p.right().conjunctPi(p.left().pi(), !p.left().sign()))
                .reduce(Blame::disjunct)
                .orElseGet(Blame::zero);
    }
}
