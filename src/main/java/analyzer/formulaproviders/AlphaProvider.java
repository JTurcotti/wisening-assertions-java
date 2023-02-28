package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.SymbolicConstant;
import core.codemodel.events.Alpha;
import core.dependencies.AlphaOrBeta;
import core.formula.Formula;
import core.formula.FormulaProvider;

record AlphaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<AlphaOrBeta, Alpha> {
    @Override
    public Formula<AlphaOrBeta> get(Alpha event) {
        return new SymbolicConstant<>(0);
    }
}
