package analyzer;

import core.codemodel.elements.Field;
import core.codemodel.elements.Mutable;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Variable;
import core.codemodel.types.Blame;
import core.codemodel.types.FullContext;
import spoon.reflect.code.*;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtVariableReference;
import util.Pair;
import util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
        Set<Mutable> readSet = Util.mergeSets(closure.getFieldReads(), closure.getVariableReads());
        return typecheckStmtList(FullContext.atEntry(readSet), closure.procedure.body);
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
        switch (stmt) {
            case CtAssert<?> a -> {
                //TODO: handle assertions
            }
            case CtOperatorAssignment<?, ?> op -> {
                Pair<FullContext, Blame> lhs = typecheckExpression(ctxt, op.getAssigned());
                Pair<FullContext, Blame> rhs = typecheckExpression(lhs.left(), op.getAssignment());
                //both rhs and lhs get blame in an operator assignment like x += 2
                Blame blame = lhs.right().disjunct(rhs.right());
                return typecheckAssignmentToExpression(rhs.left(), op.getAssigned(), blame);

            }
            case CtAssignment<?, ?> a -> {
                Pair<FullContext, Blame> rhs = typecheckExpression(ctxt, a.getAssignment());
                return typecheckAssignmentToExpression(rhs.left(), a.getAssigned(), rhs.right());
            }
            case CtIf i -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtSwitch<?> s -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtLocalVariable<?> l -> {
                if (l.getAssignment() == null) {
                    //empty local var declarations should be noops... I think...
                    return ctxt;
                }
                Pair<FullContext, Blame> rhs = typecheckExpression(ctxt, l.getAssignment());
                return typecheckAssignmentToVariable(rhs.left(), l.getReference(), rhs.right());
            }
            case CtInvocation<?> i -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtWhile w -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtDo d -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtFor f -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtForEach f -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtTry t -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtReturn<?> r -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtBreak b -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtContinue c -> {
                System.out.println("Unhandled: " + stmt);
            }
            case CtUnaryOperator<?> u -> {
                return typecheckExpression(ctxt, u).left();
            }
            case CtThrow t -> {
                System.out.println("Unhandled: " + stmt);
            }
            default -> throw new IllegalArgumentException("Unexpected statement: " + stmt);
        }
        return ctxt;
    }

    private UnaryOperator<Pair<FullContext, Blame>> typecheckExprCumulative(CtExpression<?> expr) {
        return input -> typecheckExpression(input.left(), expr).mapRight(input.right()::disjunct);
    }

    private UnaryOperator<Pair<FullContext, Blame>> typecheckExprListCumulative(List<CtExpression<?>> exprs) {
        return exprs.stream().map(this::typecheckExprCumulative)
                .reduce(UnaryOperator.identity(), Util::unaryAndThen);
    }

    private static Pair<FullContext, Blame> wrapWithNoBlame(FullContext ctxt) {
        return new Pair<>(ctxt, Blame.zero());
    }

    private Pair<FullContext, Blame> typecheckTwoExprs(FullContext ctxt, CtExpression<?> expr1, CtExpression<?> expr2) {
        return typecheckExprCumulative(expr2)
                .apply(typecheckExprCumulative(expr1)
                        .apply(wrapWithNoBlame(ctxt)));
    }

    private Pair<FullContext, Blame> typecheckExprList(FullContext ctxt, List<CtExpression<?>> exprs) {
        return typecheckExprListCumulative(exprs).apply(wrapWithNoBlame(ctxt));
    }

    private Pair<FullContext, Blame> typecheckExpression(FullContext ctxt, CtExpression<?> expr) {
        switch (expr) {
            case CtArrayAccess<?, ?> a -> {
                return typecheckTwoExprs(ctxt, a.getTarget(), a.getIndexExpression());
            }
            case CtNewArray<?> na -> {
                return typecheckExprListCumulative(na.getElements())
                        .apply(typecheckExprList(ctxt,
                                na.getDimensionExpressions().stream().map(e -> (CtExpression<?>) e)
                                        .collect(Collectors.toList())));
            }
            case CtFieldAccess<?> fa -> {
                Blame faBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateField(fa));
                if (fa.getTarget() instanceof CtThisAccess<?>) {
                    Field f = parentAnalyzer.fieldIndexer.lookupOrCreate(fa.getVariable().getDeclaration());
                    return new Pair<>(ctxt, ctxt.lookupMutable(f).disjunct(faBlame));
                }
                if (fa.getVariable().isStatic() && !fa.getTarget().getPosition().isValidPosition()) {
                    //static field access without a valid position for the target: we assume it's
                    //a static import and don't create blame for the receiver
                    return new Pair<>(ctxt, faBlame);
                }
                return typecheckExpression(ctxt, fa.getTarget()).mapRight(faBlame::disjunct);
            }
            case CtSuperAccess<?> sa ->
                    System.out.println("Unhandled: " + expr);
            case CtVariableAccess<?> va -> {
                Blame vaBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(va));
                Variable v = parentAnalyzer.varIndexer.lookupOrCreate(va.getVariable().getDeclaration());
                return new Pair<>(ctxt, ctxt.lookupMutable(v).disjunct(vaBlame));
            }
            case CtInvocation<?> inv -> {
                System.out.println("Unhandled: " + expr);
            }
            case CtLiteral<?> lit -> {
                return new Pair<>(ctxt, Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(lit)));
            }
            case CtUnaryOperator<?> unop -> {
                Blame unopBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateUnop(unop));
                return typecheckExpression(ctxt, unop.getOperand()).mapRight(unopBlame::disjunct);
            }
            case CtBinaryOperator<?> binop -> {
                Blame binopBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateBinop(binop));
                return typecheckTwoExprs(ctxt, binop.getLeftHandOperand(), binop.getRightHandOperand())
                        .mapRight(binopBlame::disjunct);
            }
            case CtTypeAccess<?> t -> {
                return new Pair<>(ctxt, Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(t)));
            }
            case CtConditional<?> c -> {
                System.out.println("Unhandled: " + expr);
            }
            case CtConstructorCall<?> c -> {
                System.out.println("Unhandled: " + expr);
            }
            default -> throw new IllegalArgumentException("Unexpected expression: " + expr);
        }
        return new Pair<>(ctxt, Blame.zero());
    }

    private FullContext typecheckAssignmentToVariable(
            FullContext ctxt, CtVariableReference<?> vr, Blame assignment) {
        Variable v = parentAnalyzer.varIndexer.lookupOrCreate(vr.getDeclaration());
        return ctxt.performAssignment(v, assignment);
    }

    private FullContext typecheckAssignmentToField(
            FullContext ctxt, CtFieldReference<?> fr, Blame assignment) {
        Field f = parentAnalyzer.fieldIndexer.lookupOrCreate(fr.getDeclaration());
        return ctxt.performAssignment(f, assignment);
    }

    private FullContext typecheckAssignmentToExpression(FullContext ctxt, CtExpression<?> assigned, Blame assignment) {
        switch (assigned) {
            case CtArrayAccess<?, ?> a -> {
                Pair<FullContext, Blame> index = typecheckExpression(ctxt, a.getIndexExpression());
                return typecheckAssignmentToExpression(
                        index.left(), a.getTarget(), assignment.disjunct(index.right()));
            }
            case CtFieldAccess<?> fa -> {
                if (fa.getTarget() instanceof CtThisAccess<?>) {
                    return typecheckAssignmentToField(ctxt, fa.getVariable(), assignment);
                }
                Blame faBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateField(fa));
                return typecheckAssignmentToExpression(ctxt, fa.getTarget(), assignment.disjunct(faBlame));
            }
            case CtSuperAccess<?> sa -> {
                    System.out.println("Unhandled: " + assigned);
            }
            case CtVariableAccess<?> va -> {
                return typecheckAssignmentToVariable(ctxt, va.getVariable(), assignment);
            }
            default -> throw new IllegalArgumentException("Unexpected assignment to expression: " + assigned);
        }
        return ctxt;
    }
}
