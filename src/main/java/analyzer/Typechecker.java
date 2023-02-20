package analyzer;

import core.codemodel.elements.Procedure;
import core.codemodel.types.FullContext;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;

import java.util.HashMap;
import java.util.Map;

public class Typechecker {
    private final ProgramAnalyzer parentAnalyzer;

    final Map<Procedure, FullContext> typecheckedProcedures = new HashMap<>();

    Typechecker(ProgramAnalyzer parentAnalyzer) {
        this.parentAnalyzer = parentAnalyzer;
    }

    void performTypechecking() {
        this.parentAnalyzer.closures.data.forEach(
                (procedure, closure) ->
                    typecheckedProcedures.put(procedure, typecheckClosure(closure))
        );
    }

    private FullContext typecheckClosure(ClosureMap.ClosureType closure) {
        return typecheckStmtList(new FullContext(), closure.procedure.body);
    }

    private FullContext typecheckStmtList(FullContext ctxt, CtStatementList stmts) {
        if (stmts == null) {
            return ctxt;
        }
        for (CtStatement stmt : stmts) {
            ctxt = typecheckStmt(ctxt, stmt);
        }
        return ctxt;
    }

    private FullContext typecheckStmt(FullContext ctxt, CtStatement stmt) {
        return ctxt;
    }
}
