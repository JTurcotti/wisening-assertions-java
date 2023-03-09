package analyzer;

import core.codemodel.elements.Call;
import core.codemodel.elements.Procedure;
import util.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallMap {
    final Map<Procedure, Collection<Call>> callsToProcedure = new HashMap<>();
    final Map<Call, Procedure> procedureContainingCall = new HashMap<>();

    final ProgramAnalyzer analyzer;

    CallMap(ProgramAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    private void addCallToMap(Procedure called, Call call) {
        if (callsToProcedure.containsKey(called)) {
            callsToProcedure.put(called, Util.addToStream(callsToProcedure.get(called).stream(), call).toList());
        } else {
            callsToProcedure.put(called, List.of(call));
        }
    }

    void compute() {
        analyzer.callIndexer.outputs().forEach(call ->
                analyzer.procedureCalledBy(call).ifPresent(procedure ->
                        analyzer.getOverrides(procedure).forEach(override ->
                                addCallToMap(override, call))));
        analyzer.closures.data.forEach((proc, ct) -> ct.getCalls().forEach(call -> procedureContainingCall.put(call, proc)));
    }

    Collection<Call> callsToProcedure(Procedure p) {
        if (!callsToProcedure.containsKey(p)) {
            throw new IllegalStateException("Expected procedure to be indexed: " + p);
        }
        return callsToProcedure.get(p);
    }

    Procedure getProcedureContainingCall(Call c) {
        if (!procedureContainingCall.containsKey(c)) {
            throw new IllegalStateException("Expected call to be indexed: " + c);
        }
        return procedureContainingCall.get(c);
    }
}
