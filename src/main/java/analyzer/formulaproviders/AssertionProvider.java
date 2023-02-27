package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Assertion;
import core.dependencies.OmegaOrLine;
import core.formula.Formula;
import core.formula.FormulaProvider;

record AssertionProvider(ProgramAnalyzer analyzer) implements FormulaProvider<OmegaOrLine, Assertion> {
    @Override
    public Formula<OmegaOrLine> get(Assertion event) {
        return null;
    }
}
