package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Omega;
import core.dependencies.AlphaOrBetaOrEta;
import core.formula.Formula;
import core.formula.FormulaProvider;

record OmegaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<AlphaOrBetaOrEta, Omega> {
    @Override
    public Formula<AlphaOrBetaOrEta> get(Omega event) {
        return null;
    }
}
