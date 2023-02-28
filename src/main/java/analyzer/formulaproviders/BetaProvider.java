package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.elements.PhiOutput;
import core.codemodel.events.Assertion;
import core.codemodel.events.Beta;
import core.codemodel.types.Blame;
import core.dependencies.PiOrPhi;
import core.formula.Formula;
import core.formula.FormulaProvider;

record BetaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<PiOrPhi, Beta> {
    @Override
    public Formula<PiOrPhi> get(Beta beta) {
        Blame outputBlame = switch (beta.out()) {
            case PhiOutput out ->
                    analyzer.getOutputBlame(beta.procedure(), out);
            case Assertion assertion ->
                    analyzer.getAssertionBlame(assertion);
            default ->
                throw new IllegalStateException("Unexpected beta output: " + beta.out());
        };
        return PhiProvider.formulaFromEvent(outputBlame.getAtSite(beta.in()));
    }
}
