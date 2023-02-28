package analyzer.formulaproviders;

import analyzer.CtProcedure;
import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.SymbolicConj;
import analyzer.formulaproviders.arith.SymbolicDisj;
import analyzer.formulaproviders.arith.SymbolicParam;
import core.codemodel.elements.*;
import core.codemodel.events.Alpha;
import core.codemodel.events.Beta;
import core.codemodel.events.Eta;
import core.codemodel.events.Omega;
import core.codemodel.types.Blame;
import core.codemodel.types.BlameSite;
import core.dependencies.AlphaOrBetaOrEta;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

record OmegaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<AlphaOrBetaOrEta, Omega> {
    @Override
    public Formula<AlphaOrBetaOrEta> get(Omega omega) {
        Blame assertionBlame =  analyzer.getAssertionBlame(omega.assertion());
        Procedure lineProcedure = analyzer.procedureOfLine(omega.line());
        Procedure assertionProcedure = analyzer.procedureOfAssertion(omega.assertion());
        CtProcedure assertionCtProcedure = analyzer.lookupProcedure(assertionProcedure);

        List<Formula<AlphaOrBetaOrEta>> cases = new LinkedList<>();

        if (assertionBlame.blamesSite(omega.line())) {
            if (!lineProcedure.equals(assertionProcedure)) {
                throw new IllegalStateException("Unexpected");
            }
            cases.add(new SymbolicParam<>(new Beta(lineProcedure, omega.line(), omega.assertion())));
        }

        for (CallOutput c : assertionBlame.getBlamedOutputs()) {
            for (Map.Entry<PhiOutput, Blame> result :
                    analyzer.getResultBlamesForProcedure(lineProcedure).entrySet()) {
                if (result.getValue().blamesSite(omega.line())) {
                    cases.add(new SymbolicConj<>(List.of(
                            new SymbolicParam<>(
                                    new Beta(lineProcedure, omega.line(), BetaSite.ofPhiOutput(result.getKey()))),
                            new SymbolicParam<>(
                                    new Eta(lineProcedure, result.getKey(), analyzer.procedureOfCall(c.call()).get(), c.output())),
                            new SymbolicParam<>(
                                    new Beta(assertionProcedure, c, omega.assertion()))
                    )));
                }
            }
        }

        for (ClosedOver closedOver : assertionBlame.getBlamedClosedOver()) {
            cases.add(new SymbolicConj<>(List.of(
                    new SymbolicParam<>(
                            new Alpha(omega.line(),
                                    assertionProcedure,
                                    assertionCtProcedure.closedOverAsInput(closedOver))),
                    new SymbolicParam<>(
                            new Beta(assertionProcedure,
                                    BlameSite.ofClosedOver(closedOver),
                                    omega.assertion())))));
        }

        return new SymbolicDisj<>(cases);
    }
}
