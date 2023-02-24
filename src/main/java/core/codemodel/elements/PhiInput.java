package core.codemodel.elements;

import java.util.Optional;

public sealed interface PhiInput permits Arg, Field, Self, Variable {
    static Optional<Mutable> asMutable(PhiInput in) {
        return switch (in) {
            case Field f -> Optional.of(f);
            case Variable v -> Optional.of(v);
            default -> Optional.empty();
        };
    }

    static Mutable assertMutable(PhiInput in) {
        if (asMutable(in).isPresent()) {
            return asMutable(in).get();
        }
        throw new IllegalArgumentException("Expected mutable");
    }
}
