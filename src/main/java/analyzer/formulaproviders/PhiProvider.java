package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.*;
import core.codemodel.events.Phi;
import core.codemodel.types.IntraflowEvent;
import core.dependencies.PiOrPhi;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.stream.Collectors;

record PhiProvider(ProgramAnalyzer analyzer) implements FormulaProvider<PiOrPhi, Phi> {
    public Formula<PiOrPhi> fromEvent(IntraflowEvent event) {
        return new SymbolicDisj<>(event.dnf().stream().map(conj ->
                new SymbolicConj<>(conj.stream().map(ae ->
                        IntraflowEvent.AtomicEvent.<Formula<PiOrPhi>>process(ae,
                                pi -> SymbolicNot.symbolicSign(new SymbolicParam<>(pi.pi()), pi.sign()),
                                phi -> new SymbolicParam<>(phi)
                        )).collect(Collectors.toList()))).collect(Collectors.toList()));
    }

    @Override
    public Formula<PiOrPhi> get(Phi event) {
        return fromEvent(analyzer.getOutputBlame(event.procedure(), event.out()).getAtSite(analyzer, event.in()));
    }
}
