package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.LineAssertionPair;
import core.dependencies.OmegaOrLine;
import core.formula.Formula;
import core.formula.FormulaProvider;

record LineUpdateProvider(ProgramAnalyzer analyzer) implements FormulaProvider<OmegaOrLine, LineAssertionPair> {
    @Override
    public Formula<OmegaOrLine> get(LineAssertionPair event) {
        return null;
    }
}
