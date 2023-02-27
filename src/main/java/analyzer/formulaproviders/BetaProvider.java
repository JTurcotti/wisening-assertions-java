package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Beta;
import core.dependencies.PiOrPhi;
import core.formula.Formula;
import core.formula.FormulaProvider;

record BetaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<PiOrPhi, Beta> {
    @Override
    public Formula<PiOrPhi> get(Beta event) {
        return null;
    }
}
