package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Eta;
import core.dependencies.BetaOrEta;
import core.formula.Formula;
import core.formula.FormulaProvider;

record EtaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<BetaOrEta, Eta> {
    @Override
    public Formula<BetaOrEta> get(Eta event) {
        return null;
    }
}
