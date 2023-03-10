package analyzer.formulaproviders;

import analyzer.formulaproviders.arith.SymbolicDisj;
import analyzer.formulaproviders.arith.SymbolicParam;
import core.codemodel.events.LineAssertionPair;
import core.codemodel.events.Omega;
import core.dependencies.OmegaOrLine;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.List;


/*
Given a line and an assertion that passed, compute the new correctness of that line, using the appropriate omega
and the old correctness of the line
 */
public record LineUpdateProvider() implements FormulaProvider<OmegaOrLine, LineAssertionPair> {
    @Override
    public Formula<OmegaOrLine> get(LineAssertionPair pair) {
        return new SymbolicDisj<>(List.of(
                new SymbolicParam<>(pair.line()),
                new SymbolicParam<>(new Omega(pair.assertion(), pair.line()))
        ));
    }
}
