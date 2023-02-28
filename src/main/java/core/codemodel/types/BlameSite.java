package core.codemodel.types;

import core.codemodel.elements.ClosedOver;
import core.codemodel.elements.Field;
import core.codemodel.elements.Self;
import core.codemodel.elements.Variable;

public interface BlameSite {
    static BlameSite ofClosedOver(ClosedOver closedOver) {
        return switch (closedOver) {
            case Field f -> f;
            case Variable v -> v;
            case Self s -> s;
        };
    }
}
