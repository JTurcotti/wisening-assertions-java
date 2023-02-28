package core.codemodel.types;

import core.codemodel.elements.*;

public interface BlameSite {
    static BlameSite ofClosedOver(ClosedOver closedOver) {
        return switch (closedOver) {
            case Field f -> f;
            case Variable v -> v;
            case Self s -> s;
        };
    }
}
