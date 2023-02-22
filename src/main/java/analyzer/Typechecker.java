package analyzer;

import core.codemodel.elements.*;
import core.codemodel.events.Phi;
import core.codemodel.events.Pi;
import core.codemodel.types.Blame;
import core.codemodel.types.FullContext;
import spoon.reflect.code.*;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtVariableReference;
import util.Pair;
import util.Util;

import java.util.*;
import java.util.function.Function;
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
                        typecheckedProcedures.put(procedure, new PerProcedure(procedure, closure).typecheck())
        );
    }

    private class PerProcedure {
        private final Procedure procedure;
        private final ClosureMap.ClosureType closure;

        private PerProcedure(Procedure procedure, ClosureMap.ClosureType closure) {
            this.procedure = procedure;
            this.closure = closure;
        }

        private FullContext typecheck() {
            Set<Mutable> readSet = Util.mergeSets(closure.getFieldReads(), closure.getVariableReads());
            return typecheckStmtList(FullContext.atEntry(readSet), closure.procedure.body);
        }

        private FullContext typecheckStmtList(FullContext ctxt, CtStatementList stmts) {
            if (stmts == null) {
                return ctxt;
            }
            for (CtStatement stmt : stmts) {
                ctxt.assertReachable();
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
                    Pair<FullContext, Blame> guard = typecheckExpression(ctxt, i.getCondition());
                    ctxt = guard.left();
                    Blame guardBlame = guard.right();
                    Pi pi = parentAnalyzer.branchIndexer.lookupOrCreate(new CtVirtualBranch(i));
                    return typecheckStmtList(
                            ctxt.takeBranch(pi, true, guardBlame),
                            i.getThenStatement())
                            .mergeAcrossBranch(pi,
                                    typecheckStmtList(
                                            ctxt.takeBranch(pi, false, guardBlame),
                                            i.getElseStatement()));
                }
                case CtSwitch<?> s -> {
                    //TODO: handle this
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
                    return typecheckExpression(ctxt, i).left();
                }
                case CtWhile w -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                case CtDo d -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                case CtFor f -> {
                    System.out.println("Unhandled: " + stmt);
                    //TODO: handle this
                    for (CtStatement init: f.getForInit()) {
                        ctxt = typecheckStmt(ctxt, init);
                    }
                    return ctxt;
                }
                case CtForEach f -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                case CtTry t -> {
                    //TODO: deal with exception handling
                    //return typecheckStmtList(ctxt, t.getBody());
                }
                case CtReturn<?> r -> {
                    if (r.getReturnedExpression() == null) {
                        return ctxt.observeReturn(Map.of());
                    }
                    Pair<FullContext, Blame> retBlame = typecheckExpression(ctxt, r.getReturnedExpression());
                    return ctxt.observeReturn(Map.of(new Ret(procedure, 0), retBlame.right()));
                }
                case CtBreak b -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                case CtContinue c -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                case CtUnaryOperator<?> u -> {
                    return typecheckExpression(ctxt, u).left();
                }
                case CtThrow t -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                default -> throw new IllegalArgumentException("Unexpected statement: " + stmt);
            }
            return ctxt;
        }

        private UnaryOperator<Pair<FullContext, Blame>> typecheckExpression(CtExpression<?> expr) {
            return input -> typecheckExpression(input.left(), expr).mapRight(input.right()::disjunct);
        }

        private UnaryOperator<Pair<FullContext, Blame>> typecheckExprList(List<CtExpression<?>> exprs) {
            return exprs.stream().map(this::typecheckExpression)
                    .reduce(UnaryOperator.identity(), Util::unaryAndThen);
        }

        private Pair<FullContext, Blame> wrapWithNoBlame(FullContext ctxt) {
            return new Pair<>(ctxt, Blame.zero());
        }

        private Pair<FullContext, Blame> typecheckTwoExprs(FullContext ctxt, CtExpression<?> expr1, CtExpression<?> expr2) {
            return typecheckExpression(expr2)
                    .apply(typecheckExpression(expr1)
                            .apply(wrapWithNoBlame(ctxt)));
        }

        private Pair<FullContext, Blame> typecheckExprList(FullContext ctxt, List<CtExpression<?>> exprs) {
            return typecheckExprList(exprs).apply(wrapWithNoBlame(ctxt));
        }

        private UnaryOperator<Pair<FullContext, List<Blame>>> typecheckExprBlameDisjoint(CtExpression<?> expr) {
            return input -> typecheckExpression(input.left(), expr).mapRight(blame ->
                    Util.addToStream(input.right().stream(), blame).toList());
        }

        private UnaryOperator<Pair<FullContext, List<Blame>>>
        typecheckExprListBlameDisjoint(List<CtExpression<?>> exprs) {
            return exprs.stream().map(this::typecheckExprBlameDisjoint)
                    .reduce(UnaryOperator.identity(), Util::unaryAndThen);
        }

        private Pair<FullContext, List<Blame>>
        typecheckExprListBlameDisjoint(FullContext ctxt, List<CtExpression<?>> exprs) {
            return typecheckExprListBlameDisjoint(exprs).apply(new Pair<>(ctxt, List.of()));
        }

        private Pair<FullContext, Blame> typecheckExpression(FullContext ctxt, CtExpression<?> expr) {
            switch (expr) {
                case CtArrayAccess<?, ?> a -> {
                    return typecheckTwoExprs(ctxt, a.getTarget(), a.getIndexExpression());
                }
                case CtNewArray<?> na -> {
                    return typecheckExprList(na.getElements())
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
                case CtSuperAccess<?> sa -> throw new IllegalArgumentException("Unexpected: " + sa);
                case CtVariableAccess<?> va -> {
                    Blame vaBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(va));
                    Variable v = parentAnalyzer.varIndexer.lookupOrCreate(va.getVariable().getDeclaration());
                    return new Pair<>(ctxt, ctxt.lookupMutable(v).disjunct(vaBlame));
                }
                case CtInvocation<?> inv -> {
                    if (inv.isImplicit()) {
                        return new Pair<>(ctxt, Blame.zero());
                    }
                    //TODO: also consider possible overrides

                    //TODO: handle callarg tracking

                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(inv));

                    if (!parentAnalyzer.isIntrasourceCall(c)) {
                        //call to a library method - no Phi's introduced just treated like an arithmetic assignment
                        Pair<FullContext, Blame> invResult = typecheckExprList(inv.getArguments())
                                .apply(typecheckExpression(ctxt, inv.getTarget()));
                        return new Pair<>(
                                Config.NONLOCAL_METHOD_MUTATES_SELF ?
                                        typecheckAssignmentToExpression(invResult.left(), inv.getTarget(), invResult.right()) :
                                        invResult.left(),
                                invResult.right());
                    }

                    Procedure p = parentAnalyzer.procedureOfCall(c).orElseThrow(
                            () -> new IllegalStateException("Call made that could not be tied to a procedure"));
                    Pair<FullContext, List<Blame>> args = typecheckExprListBlameDisjoint(ctxt, inv.getArguments());
                    ctxt = args.left();
                    List<Blame> argBlames = args.right();
                    Blame retBlame = Blame.oneSite(new CallRetPair(c, new Ret(p, 0)));

                    if (inv.getExecutable().isStatic()) {
                        Pair<FullContext, Blame> receiver = typecheckExpression(ctxt, inv.getTarget());
                        //only passed args can be accessed by method call so no need to consider side effects
                        return receiver.mapRight(Blame.conjunctListWithPhi(argBlames, i ->
                                        new Phi(p, new Arg(p, i), new Ret(p, 0)))
                                .disjunct(retBlame)::disjunct);
                    }

                    if (inv.getTarget() instanceof CtThisAccess<?> || inv.getTarget() instanceof CtSuperAccess<?>) {
                        List<Field> reads = parentAnalyzer.closures.lookupByCall(c).getFieldReads().stream().toList();
                        Set<Field> writes = parentAnalyzer.closures.lookupByCall(c).getFieldWrites();

                        List<Blame> fieldBlames = reads.stream().map(ctxt::lookupMutable).toList();

                        Function<Phi.Output, Blame> blameGenerator = output ->
                                Blame.conjunctListWithPhi(fieldBlames, i ->
                                                new Phi(p, reads.get(i), output))
                                        .disjunct(Blame.conjunctListWithPhi(argBlames, i ->
                                                new Phi(p, new Arg(p, i), output)))
                                        .disjunct(retBlame);

                        for (Field write : writes) {
                            ctxt = typecheckAssignmentToField(ctxt,
                                    parentAnalyzer.fieldIndexer.lookupAux(write).orElseThrow(
                                            () -> new IllegalStateException("Expected field lookup to succeed: " + write)
                                    ).getReference(),
                                    blameGenerator.apply(write));
                        }

                        return new Pair<>(ctxt, blameGenerator.apply(new Ret(p, 0)));
                    }

                    Pair<FullContext, Blame> receiver = typecheckExpression(ctxt, inv.getTarget());
                    ctxt = receiver.left();

                    Function<Phi.Output, Blame> blameGenerator = output -> {
                        Blame reciverBlame = parentAnalyzer.closures.lookupByCall(c).readsSelf() ?
                                receiver.right().conjunctPhi(new Phi(p, new Self(), output)) : Blame.zero();
                        return Blame.conjunctListWithPhi(argBlames, i -> new Phi(p, new Arg(p, i), output))
                                .disjunct(reciverBlame)
                                .disjunct(retBlame);
                    };

                    return new Pair<>(
                            typecheckAssignmentToExpression(ctxt, inv.getTarget(), blameGenerator.apply(new Self())),
                            blameGenerator.apply(new Ret(p, 0)));
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
                    if (t.isImplicit()) {
                        return new Pair<>(ctxt, Blame.zero());
                    }
                    return new Pair<>(ctxt, Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(t)));
                }
                case CtConditional<?> c -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + expr);
                }
                case CtConstructorCall<?> constr -> {
                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(constr));
                    Blame constrBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateConstr(constr));
                    if (!parentAnalyzer.isIntrasourceCall(c)) {
                        return typecheckExprList(ctxt, constr.getArguments()).mapRight(constrBlame::disjunct);
                    }
                    Procedure p = parentAnalyzer.procedureOfCall(c).orElseThrow(() ->
                            new IllegalStateException("Expected procedure lookup to succeed"));
                    return typecheckExprListBlameDisjoint(ctxt, constr.getArguments())
                            .mapRight(argBlames ->
                                    Blame.conjunctListWithPhi(argBlames, i -> new Phi(p, new Arg(p, i), new Ret(p, 0)))
                                            .disjunct(Blame.oneSite(new CallRetPair(c, new Ret(p, 0))))
                                            .disjunct(constrBlame));
                }
                case CtLambda<?> l -> {
                    return new Pair<>(ctxt, Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(l)));
                }
                case CtThisAccess<?> ignored -> {
                    return new Pair<>(ctxt, Blame.oneSite(new Self()));
                }
                case null -> {
                    return new Pair<>(ctxt, Blame.zero());
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
                    //noop, but maybe reconsider?
                }
                case CtVariableAccess<?> va -> {
                    return typecheckAssignmentToVariable(ctxt, va.getVariable(), assignment);
                }
                case CtConstructorCall<?> c -> {
                    //noop: it's a newly initialized object so there are no side effects
                }
                case CtTypeAccess<?> t -> {
                    //noop: it's a static method call
                }
                case CtThisAccess<?> ignored -> {
                    //noop - writes to fields of `this` already taken care of higher in stack
                }
                case CtInvocation<?> inv -> {
                    //for example a.foo().bar() is treated as a write to `a`
                    typecheckAssignmentToExpression(ctxt, inv.getTarget(), assignment);
                }
                case null -> {
                }
                default -> throw new IllegalArgumentException("Unexpected assignment to expression: " + assigned);
            }
            return ctxt;
        }

        @Override
        public String toString() {
            return "PerProcedure[" +
                    "procedure=" + procedure + ", " +
                    "closure=" + closure + ']';
        }

    }
}
