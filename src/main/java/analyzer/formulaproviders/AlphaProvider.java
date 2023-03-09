package analyzer.formulaproviders;

import analyzer.CtVirtualCall;
import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.arith.*;
import core.codemodel.elements.CallInput;
import core.codemodel.elements.PhiInput;
import core.codemodel.elements.Procedure;
import core.codemodel.events.Alpha;
import core.codemodel.events.Beta;
import core.codemodel.types.Blame;
import core.codemodel.types.BlameSite;
import core.dependencies.AlphaOrBeta;
import core.formula.Formula;
import core.formula.FormulaProvider;
import util.Util;

import java.util.List;
import java.util.stream.Collectors;

record AlphaProvider(ProgramAnalyzer analyzer) implements FormulaProvider<AlphaOrBeta, Alpha> {
    @Override
    public Formula<AlphaOrBeta> get(Alpha alpha) {
        //average over all calls that could occur to this procedure
        //TODO: weighted average by profiled call frequency
        return new SymbolicAverage<>(analyzer.callsToProcedure(alpha.procedure()).stream().map(c -> {
            CtVirtualCall call = analyzer.lookupCall(c);
            if (!call.inputBlamesSet()) {
                //this is only because some calls aren't typechecked yet so this lookup will fail
                //TODO: remove this once try statements and possibly other missing cases are handled
                return SymbolicConstant.<AlphaOrBeta>zero();
            }
            Procedure callProcedure = call.containingProcedure;
            CallInput callInput = new CallInput(c, alpha.input());
            Blame callBlame = call.getInputBlame(alpha.input(), analyzer);
            //identify all PhiInputs in the call's procedure that it blames and add a recurrent
            //alpha conjuncted with a beta for the flow to the list of alpha cases
            List<Formula<AlphaOrBeta>> alphaCases =
                    callBlame.getBlamedInputs(analyzer, alpha.procedure()).stream()
                            .map(input -> new SymbolicConj<AlphaOrBeta>(List.of(
                                    new SymbolicParam<>(
                                            new Beta(callProcedure,
                                                    BlameSite.ofPhiInput(input, analyzer),
                                                    callInput)),
                                    new SymbolicParam<>(
                                            new Alpha(alpha.line(), callProcedure,
                                                    PhiInput.balanceArgsAndVars(input, callProcedure, analyzer))
                                    ))
                            )).collect(Collectors.toList());
            if (callBlame.blamesSite(alpha.line())) {
                //if this call directly blames the targetted line, add that beta
                alphaCases = Util.addToStream(alphaCases.stream(),
                        new SymbolicParam<>(new Beta(callProcedure, alpha.line(), callInput))
                ).collect(Collectors.toList());
            }
            return new SymbolicDisj<>(alphaCases);
        }).collect(Collectors.toList()));
    }
}

//alpha disabled: (zero'd)
//Supervisor[10 assertions: {2.4414062E-4, 0.125, 2.2831597E-24, 0.010850694, 0.9986104, 0.015455961, 0.4993052, 9.765625E-4, 1.1920929E-7, 0.1248263, }
// line coverages: [0.0: 1650 | 0.1: 81 | 0.2: 46 | 0.3: 23 | 0.4: 44 | 0.5: 19 | 0.6: 8 | 0.7: 12 | 0.8: 25 | 0.9: 111 | ]
// line correctnesses: [0.0: 0 | 0.1: 0 | 0.2: 0 | 0.3: 0 | 0.4: 1915 | 0.5: 1915 | 0.6: 0 | 0.7: 0 | 0.8: 0 | 0.9: 96 | ]


//alpha enabled:
//Supervisor[10 assertions: {5.344878E-6, 0.019227445, 3.4435618E-25, 0.010850694, 0.99377006, 0.007562376, 0.49688503, 4.461556E-4, 2.0551997E-8, 0.12422126, },
// line coverages: [0.0: 1610 | 0.1: 79 | 0.2: 80 | 0.3: 29 | 0.4: 43 | 0.5: 19 | 0.6: 8 | 0.7: 6 | 0.8: 31 | 0.9: 111 | ]
// line correctnesses: [0.0: 0 | 0.1: 0 | 0.2: 0 | 0.3: 0 | 0.4: 1915 | 0.5: 1915 | 0.6: 0 | 0.7: 0 | 0.8: 0 | 0.9: 96 | ]

