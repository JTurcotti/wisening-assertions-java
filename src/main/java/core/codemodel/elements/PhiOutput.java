package core.codemodel.elements;

import java.util.Optional;

public sealed interface PhiOutput permits Field, Ret, Self, Variable {
    static Optional<Mutable> asMutable(PhiOutput out) {
        return switch (out) {
            case Field f -> Optional.of(f);
            case Variable v -> Optional.of(v);
            default -> Optional.empty();
        };
    }

    //sometimes we want to consider an equivalent
    static PhiOutput mapToProc(PhiOutput out, Procedure p) {
        return out instanceof Ret? new Ret(p): out;
    }
}
