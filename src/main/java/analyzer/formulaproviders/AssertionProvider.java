package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.SymbolicConj;
import analyzer.formulaproviders.arith.SymbolicDisj;
import analyzer.formulaproviders.arith.SymbolicNot;
import analyzer.formulaproviders.arith.SymbolicParam;
import core.codemodel.events.Assertion;
import core.codemodel.events.Omega;
import core.dependencies.OmegaOrLine;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.List;
import java.util.stream.Collectors;

record AssertionProvider(ProgramAnalyzer analyzer) implements FormulaProvider<OmegaOrLine, Assertion> {
    @Override
    public Formula<OmegaOrLine> get(Assertion assertion) {
        return new SymbolicConj<>(
                analyzer.getAssertionDependentLines(assertion).stream().map(line ->
                        //for every line, it is either correct or doesn't flow to this assertion
                        new SymbolicDisj<OmegaOrLine>(List.of(
                                new SymbolicParam<>(line),
                                new SymbolicNot<>(new SymbolicParam<>(new Omega(assertion, line)))
                        ))).collect(Collectors.toList()));
    }
}
