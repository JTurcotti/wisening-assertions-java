package core.codemodel.elements;


public interface BetaSite {
    static BetaSite ofPhiOutput(PhiOutput phiOutput) {
        return switch (phiOutput) {
            case Field f -> f;
            case Variable v -> v;
            case Ret r -> r;
            case Self s -> s;
        };
    }
}
