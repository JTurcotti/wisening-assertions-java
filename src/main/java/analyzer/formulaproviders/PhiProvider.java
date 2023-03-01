package analyzer.formulaproviders;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.*;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Self;
import core.codemodel.events.Phi;
import core.codemodel.types.IntraflowEvent;
import core.dependencies.PiOrPhi;
import core.formula.Formula;
import core.formula.FormulaProvider;

import java.util.List;
import java.util.stream.Collectors;

record PhiProvider(ProgramAnalyzer analyzer) implements FormulaProvider<PiOrPhi, Phi> {
    static Formula<PiOrPhi> formulaFromEvent(IntraflowEvent event) {
        return new SymbolicDisj<>(event.dnf().stream().map(conj ->
                new SymbolicConj<>(conj.stream().map(ae ->
                        IntraflowEvent.AtomicEvent.<Formula<PiOrPhi>>process(ae,
                                pi -> SymbolicNot.symbolicSign(new SymbolicParam<>(pi.pi()), pi.sign()),
                                phi -> new SymbolicParam<>(phi)
                        )).collect(Collectors.toList()))).collect(Collectors.toList()));
    }

    Formula<PiOrPhi> formulaFromPhi(Phi phi) {
        if (!analyzer.hasImplementation(phi.procedure())) {
            //no flow here
            return SymbolicConstant.zero();
        }
        return formulaFromEvent(analyzer
                .getOutputBlame(phi.procedure(), phi.out())
                .getAtInputSite(analyzer, phi.in()));
    }

    @Override
    public Formula<PiOrPhi> get(Phi phi) {
        List<Procedure> overrides = analyzer.getOverrides(phi.procedure());

        if (overrides.size() == 1) {
            //only one possible dispatch
            return formulaFromPhi(phi);
        }

        if (phi.in().equals(new Self())) {
            //if multiple possible dispatches, Self influences which one is taken so definitely influences all results
            return SymbolicConstant.one();
        }

        //TODO: weight by dispatch frequency instead of just maxing, or alternatively consider a simple average
        return new SymbolicAverage<>(overrides.stream().map(p -> formulaFromPhi(Phi.mapToProc(phi, p))).toList());
    }
}
