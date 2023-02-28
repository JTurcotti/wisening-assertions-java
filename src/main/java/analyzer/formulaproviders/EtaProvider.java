package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.SymbolicConj;
import analyzer.formulaproviders.arith.SymbolicConstant;
import analyzer.formulaproviders.arith.SymbolicDisj;
import analyzer.formulaproviders.arith.SymbolicParam;
import core.codemodel.elements.BetaSite;
import core.codemodel.events.Beta;
import core.codemodel.events.Eta;
import core.dependencies.BetaOrEta;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.List;
import java.util.stream.Collectors;

record EtaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<BetaOrEta, Eta> {
    @Override
    public Formula<BetaOrEta> get(Eta eta) {
        if (eta.src().equals(eta.tgt())) {
            return SymbolicConstant.one();
        }
        return new SymbolicDisj<>(analyzer
                .getOutputBlame(eta.tgt(), eta.tgtOutput())
                .getBlamedOutputs().stream()
                .map(call -> new SymbolicConj<>(List.of(
                        new SymbolicParam<BetaOrEta>(
                                new Beta(eta.tgt(), call, BetaSite.ofPhiOutput(eta.tgtOutput()))),
                        new SymbolicParam<BetaOrEta>(
                                new Eta(eta.src(), eta.srcOutput(),
                                        analyzer.procedureOfCall(call.call()).get(), call.output())))))
                .collect(Collectors.toList()));
    }
}
