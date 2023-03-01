package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.*;
import core.codemodel.elements.BetaSite;
import core.codemodel.elements.Procedure;
import core.codemodel.events.Beta;
import core.codemodel.events.Eta;
import core.dependencies.BetaOrEta;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.List;
import java.util.stream.Collectors;

record EtaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<BetaOrEta, Eta> {
    Formula<BetaOrEta> formulaFromEta(Eta eta) {
        if (eta.src().equals(eta.tgt())) {
            return SymbolicConstant.one();
        }
        if (!analyzer.hasImplementation(eta.tgt())) {
            //abstract methods and interface methods don't make calls
            return SymbolicConstant.zero();
        }
        return new SymbolicDisj<>(analyzer
                .getOutputBlame(eta.tgt(), eta.tgtOutput())
                .getBlamedOutputs().stream()
                .map(call -> new SymbolicConj<BetaOrEta>(List.of(
                        new SymbolicParam<>(
                                new Beta(eta.tgt(), call, BetaSite.ofPhiOutput(eta.tgtOutput()))),
                        new SymbolicParam<>(
                                new Eta(eta.src(), eta.srcOutput(),
                                        analyzer.procedureOfCall(call.call()).get(), call.output())))))
                .collect(Collectors.toList()));
    }

    @Override
    public Formula<BetaOrEta> get(Eta eta) {
        List<Procedure> tgtOverrides = analyzer.getOverrides(eta.tgt());

        if (tgtOverrides.size() == 1) {
            //only one possible dispatch
            return formulaFromEta(eta);
        }
        //TODO: weight by dispatch frequency instead of just maxing, or alternatively consider a simple average
        return new SymbolicAverage<>(tgtOverrides.stream().map(p -> formulaFromEta(Eta.mapTgtToProc(eta, p))).toList());
    }
}
