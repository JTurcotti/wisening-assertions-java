package core.codemodel.types;

import analyzer.CtProcedure;
import analyzer.ProgramAnalyzer;
import core.codemodel.elements.*;

public interface BlameSite {
    static BlameSite ofClosedOver(ClosedOver closedOver) {
        return switch (closedOver) {
            case Field f -> f;
            case Variable v -> v;
            case Self s -> s;
        };
    }

    static BlameSite ofPhiInput(PhiInput input, ProgramAnalyzer analyzer) {
        return switch (input) {
            case Arg a -> {
                CtProcedure proc = analyzer.lookupProcedure(a.procedure());
                if (!proc.isMethod() && !proc.isConstructor()) {
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
    }
}
