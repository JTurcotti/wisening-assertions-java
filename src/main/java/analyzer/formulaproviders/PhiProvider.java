package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.SymbolicConj;
import analyzer.formulaproviders.arith.SymbolicDisj;
import analyzer.formulaproviders.arith.SymbolicNot;
import analyzer.formulaproviders.arith.SymbolicParam;
import core.codemodel.events.Phi;
import core.codemodel.types.IntraflowEvent;
import core.dependencies.PiOrPhi;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.stream.Collectors;

record PhiProvider(ProgramAnalyzer analyzer) implements FormulaProvider<PiOrPhi, Phi> {
    public static Formula<PiOrPhi> formulaFromEvent(IntraflowEvent event) {
        return new SymbolicDisj<>(event.dnf().stream().map(conj ->
                new SymbolicConj<>(conj.stream().map(ae ->
                        IntraflowEvent.AtomicEvent.<Formula<PiOrPhi>>process(ae,
                                pi -> SymbolicNot.symbolicSign(new SymbolicParam<>(pi.pi()), pi.sign()),
                                phi -> new SymbolicParam<>(phi)
                        )).collect(Collectors.toList()))).collect(Collectors.toList()));
    }

    @Override
    public Formula<PiOrPhi> get(Phi phi) {
        return formulaFromEvent(analyzer
                .getOutputBlame(phi.procedure(), phi.out())
                .getAtInputSite(analyzer, phi.in()));
    }
}
