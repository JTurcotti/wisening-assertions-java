package core.codemodel.elements;

import core.codemodel.types.BlameSite;

public sealed interface Mutable permits Field, Variable {
    static BlameSite asBlameSite(Mutable m) {
        return switch (m) {
            case Field f -> f;
            case Variable v -> v;
        };
    }
}
