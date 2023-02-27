package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Assertion;
import core.formula.Formula;
import core.formula.FormulaProvider;

record FrequencyProvider(ProgramAnalyzer analyzer) implements FormulaProvider<Assertion, Assertion> {
    @Override
    public Formula<Assertion> get(Assertion event) {
        return null;
    }
}
