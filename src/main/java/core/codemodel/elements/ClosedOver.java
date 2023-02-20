package core.codemodel.elements;

import java.util.Optional;

public sealed interface ClosedOver permits Field, Variable, Self {
    static Optional<Field> asField(ClosedOver c) {
        return c instanceof Field f? Optional.of(f): Optional.empty();
    }

    static Optional<Variable> asVariable(ClosedOver c) {
        return c instanceof Variable v? Optional.of(v): Optional.empty();
    }

    static Optional<Self> asSelf(ClosedOver c) {
        return c instanceof Self s? Optional.of(s): Optional.empty();
    }
}
