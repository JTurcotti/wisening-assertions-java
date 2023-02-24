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
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
            //TODO: appropriately wrap loops
            Set<PhiInput> params = closure.procedure.parameters.stream()
                    .map(parentAnalyzer.varIndexer::lookupOrCreate)
                    .collect(Collectors.toUnmodifiableSet());
            return typecheckStmtList(FullContext.atEntry(
                    Util.mergeSets(closure.getInputs(), params)
            ), closure.procedure.body);
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

        private UnaryOperator<FullContext> typecheckStmtList(CtStatementList stmts) {
            return c -> typecheckStmtList(c, stmts);
        }

        private FullContext typecheckCond(FullContext ctxt,
                                          CtExpression<?> guardExpr,
                                          Blame extraGuardBlame,
                                          CtVirtualBranch branch,
                                          UnaryOperator<FullContext> trueBranch,
                                          UnaryOperator<FullContext> falseBrance) {
            Pair<FullContext, Blame> guard = typecheckExpression(ctxt, guardExpr);
            Blame guardBlame = guard.right().disjunct(extraGuardBlame);
            Pi pi = parentAnalyzer.branchIndexer.lookupOrCreate(branch);
            return trueBranch.apply(guard.left().takeBranch(pi, true, guardBlame))
                    .mergeAcrossBranch(pi,
                            falseBrance.apply(guard.left().takeBranch(pi, false, guardBlame)));
        }

        private UnaryOperator<FullContext> typecheckCond(CtExpression<?> guardExpr,
                                                         Blame extraGuardBlame,
                                                         CtVirtualBranch branch,
                                                         UnaryOperator<FullContext> trueBranch,
                                                         UnaryOperator<FullContext> falseBranch) {
            return c -> typecheckCond(c, guardExpr, extraGuardBlame, branch, trueBranch, falseBranch);
        }

        private Pair<FullContext, Blame> typecheckInvocation(
                FullContext ctxt,
                //receiver = null if self is an input or input, receiver != null if fields are input or output
                CtExpression<?> receiver,
                Blame callBlame, //the syntactic blame for the call itself
                Call call, Procedure proc,
                List<CtExpression<?>> args,
                Set<PhiInput> inputs, Set<PhiOutput> outputs) {
            //TODO: don't pull argument to for each loop from context, but refresh to original

            boolean touchesMutables =
                    inputs.stream().anyMatch(Mutable.class::isInstance) ||
                            outputs.stream().anyMatch(Mutable.class::isInstance);
            boolean touchesSelf =
                    inputs.stream().anyMatch(Self.class::isInstance) ||
                    outputs.stream().anyMatch(Self.class::isInstance);
            if (touchesSelf && touchesMutables) {
                throw new IllegalArgumentException("Invocations that touch self should not be closed over fields or vars");
            }

            Optional<Blame> receiverBlame = Optional.empty();
            if (receiver != null) {
                Pair<FullContext, Blame> checkReceiver = typecheckExpression(ctxt, receiver);
                ctxt = checkReceiver.left();
                receiverBlame = Optional.of(checkReceiver.right());
            }
            final Optional<Blame> finalReceiverBlame = receiverBlame;


            Pair<FullContext, List<Blame>> checkArgs = typecheckExprListBlameDisjoint(ctxt, args);
            ctxt = checkArgs.left();
            List<Blame> argBlames = checkArgs.right();

            final FullContext ctxtAtInput = ctxt;

            List<PhiInput> inputList = inputs.stream().toList();
            List<Blame> inputBlames = inputList.stream().map(input ->
                    switch (input) {
                        case Arg a -> argBlames.get(a.num());
                        case Field f -> ctxtAtInput.lookupMutable(f);
                        case Self s -> finalReceiverBlame.get();
                        case Variable v -> ctxtAtInput.lookupMutable(v);
                    }).toList();
            Function<PhiOutput, Blame> blameGenerator = output ->
                    Blame.conjunctListWithPhi(inputBlames, i ->
                            new Phi(proc, inputList.get(i), output))
                            .disjunct(Blame.oneSite(new CallOutput(call, output)))
                            .disjunct(callBlame);
            Blame retBlame = Blame.zero();
            for (PhiOutput output : outputs) {
               switch (output) {
                   case Field f ->
                           ctxt = ctxt.performAssignment(f, blameGenerator.apply(f));
                   case Ret r ->
                       retBlame = blameGenerator.apply(r);
                   case Self s ->
                       ctxt = typecheckAssignmentToExpression(ctxt, receiver, blameGenerator.apply(s));
                   case Variable v ->
                       ctxt = ctxt.performAssignment(v, blameGenerator.apply(v));
               }
            }
            return new Pair<>(ctxt, retBlame);
        }

        private UnaryOperator<FullContext> typecheckStmt(CtStatement stmt) {
            return ctxt -> typecheckStmt(ctxt, stmt);
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
                    return typecheckCond(ctxt, i.getCondition(), Blame.zero(), new CtVirtualBranch(i),
                            typecheckStmt(i.getThenStatement()), typecheckStmt(i.getElseStatement()));
                }
                case CtSwitch<?> s -> {
                    Pair<FullContext, Blame> selector = typecheckExpression(ctxt, s.getSelector());
                    ctxt = selector.left();
                    Blame selectorBlame = selector.right();

                    //if there's a default case, use it as the final fallback, otherwise no fallback
                    UnaryOperator<FullContext> fallback = s.getCases().stream()
                            .filter(c -> c.getCaseExpression() == null)
                            .map(c -> typecheckCond(null, selectorBlame, new CtVirtualBranch(c),
                                    typecheckStmtList(c), UnaryOperator.identity()))
                            .findFirst()
                            .orElse(UnaryOperator.identity());

                    for (CtCase<?> c : Util.reversed(s.getCases())) {
                        if (c.getCaseExpression() != null) {
                            fallback = typecheckCond(c.getCaseExpression(), selectorBlame, new CtVirtualBranch(c),
                                    typecheckStmtList(c), fallback);
                        }
                    }

                    return fallback.apply(ctxt);
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
                case CtLoop loop -> {
                    if (loop instanceof CtForEach fe) {
                        //if a for each loop, initialize the iterator
                        Variable v = parentAnalyzer.varIndexer.lookupOrCreate(fe.getVariable());
                        Pair<FullContext, Blame> checkCollection = typecheckExpression(ctxt, fe.getExpression());
                        ctxt = checkCollection.left().performAssignment(v, checkCollection.right());
                    }

                    if (loop instanceof CtFor f) {
                        //if a for loop, typecheck the init statements
                        for (CtStatement s : f.getForInit()) {
                            ctxt = typecheckStmt(ctxt, s);
                        }
                    }

                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(loop));
                    Procedure p = parentAnalyzer.procedureOfCall(c).orElseThrow(() ->
                            new IllegalStateException("Expected procedure lookup to succeed"));
                    ClosureMap.ClosureType loopClosure = parentAnalyzer.closures.lookupByElement(loop);
                    return typecheckInvocation(ctxt, null, Blame.zero(), c, p, List.of(),
                            loopClosure.getInputs(), loopClosure.getOutputs()).left();
                }
                case CtTry t -> {
                    //TODO: deal with exception handling - or at least some minimal switch-like processing
                    //return typecheckStmtList(ctxt, t.getBody());
                }
                case CtReturn<?> r -> {
                    Map<PhiOutput, Blame> results = Map.of();
                    if (r.getReturnedExpression() != null) {
                        Pair<FullContext, Blame> retBlame = typecheckExpression(ctxt, r.getReturnedExpression());
                        ctxt = retBlame.left();
                        results = Map.of(new Ret(procedure), retBlame.right());
                    }
                    return ctxt.observeReturn(Util.mergeDisjointMaps(results, ctxt.lookupPhiOutputs(closure.getOutputs())));
                }
                case CtBreak b -> {
                    if (b.getParent(parent -> parent instanceof CtLoop || parent instanceof CtSwitch<?>) instanceof CtLoop) {
                        return ctxt.observeReturn(ctxt.lookupPhiOutputs(closure.getOutputs()));
                    }
                    //ignore breaks in switch statements
                    //TODO: make switch statement handling break-sensitive
                    return ctxt;
                }
                case CtContinue c -> {
                    Pair<FullContext, Blame> checkContinue = typecheckInvocation(
                            ctxt, null, Blame.zero(),
                            parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(c)),
                            procedure,
                            List.of(),
                            closure.getInputs(), closure.getOutputs()
                    );
                    return checkContinue.left().observeReturn(ctxt.lookupPhiOutputs(closure.getOutputs()));
                }
                case CtUnaryOperator<?> u -> {
                    return typecheckExpression(ctxt, u).left();
                }
                case CtThrow t -> {
                    //TODO: handle this
                    System.out.println("Unhandled: " + stmt);
                }
                case CtBlock<?> b -> {
                    return typecheckStmtList(ctxt, b);
                }
                case null -> {
                    return ctxt;
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

        /*
        This should be used when we don't want to do anything with an expression except assign an
        opaque blame to its entire source position
         */
        private Pair<FullContext, Blame> typecheckOpaque(FullContext ctxt, CtExpression<?> expr) {
            return new Pair<>(ctxt, expr.isImplicit()? Blame.zero():
                    Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateEntire(expr)));
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
                case CtSuperAccess<?> sa -> {
                    return typecheckOpaque(ctxt, sa).mapRight(Blame.oneSite(new Self())::disjunct);
                }
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
                    Blame callBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateInv(inv));

                    if (!parentAnalyzer.isIntrasourceCall(c)) {
                        //call to a library method - no Phi's introduced just treated like an arithmetic assignment
                        Pair<FullContext, Blame> invResult = typecheckExprList(inv.getArguments())
                                .apply(typecheckExpression(ctxt, inv.getTarget()))
                                .mapRight(callBlame::disjunct);
                        return new Pair<>(
                                Config.NONLOCAL_METHOD_MUTATES_SELF ?
                                        typecheckAssignmentToExpression(invResult.left(), inv.getTarget(), invResult.right()) :
                                        invResult.left(),
                                invResult.right());
                    }

                    Procedure p = parentAnalyzer.procedureOfCall(c).orElseThrow(
                            () -> new IllegalStateException("Call made that could not be tied to a procedure"));

                    Set<PhiInput> argSet = IntStream.range(0, inv.getArguments().size())
                            .mapToObj(i -> new Arg(p, i))
                            .collect(Collectors.toUnmodifiableSet());
                    Set<PhiOutput> retSet = Set.of(new Ret(p));

                    if (!inv.getExecutable().isStatic()) {
                        ClosureMap.ClosureType closure = parentAnalyzer.closures.lookupByCall(c);

                        if (inv.getTarget() instanceof CtThisAccess<?> || inv.getTarget() instanceof CtSuperAccess<?>) {
                            argSet = Stream.concat(argSet.stream(), closure.getInputs().stream())
                                    .collect(Collectors.toUnmodifiableSet());
                            retSet = Stream.concat(retSet.stream(), closure.getOutputs().stream()
                            ).collect(Collectors.toUnmodifiableSet());
                        } else {
                            if (closure.readsSelf()) {
                                argSet = Util.addToStream(argSet.stream(), new Self()).collect(Collectors.toUnmodifiableSet());
                            }
                            if (closure.writesSelf()) {
                                retSet = Util.addToStream(retSet.stream(), new Self()).collect(Collectors.toUnmodifiableSet());
                            }
                        }
                    }

                    return typecheckInvocation(ctxt, inv.getTarget(), callBlame, c, p, inv.getArguments(), argSet, retSet);
                }
                case CtLiteral<?> lit -> {
                    return typecheckOpaque(ctxt, lit);
                }
                case CtUnaryOperator<?> unop -> {
                    ///TODO: handle ++ as a write
                    Blame unopBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateUnop(unop));
                    return typecheckExpression(ctxt, unop.getOperand()).mapRight(unopBlame::disjunct);
                }
                case CtBinaryOperator<?> binop -> {
                    Blame binopBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateBinop(binop));
                    return typecheckTwoExprs(ctxt, binop.getLeftHandOperand(), binop.getRightHandOperand())
                            .mapRight(binopBlame::disjunct);
                }
                case CtTypeAccess<?> t -> {
                    return typecheckOpaque(ctxt, t);
                }
                case CtConditional<?> c -> {
                    Pair<FullContext, Blame> guard = typecheckExpression(ctxt, c.getCondition());
                    ctxt = guard.left();
                    Blame guardBlame = guard.right();
                    Pi pi = parentAnalyzer.branchIndexer.lookupOrCreate(new CtVirtualBranch(c));
                    Pair<FullContext, Blame> thenExpr = typecheckExpression(ctxt.takeBranch(pi, true, guardBlame),
                            c.getThenExpression());
                    Pair<FullContext, Blame> elseExpr = typecheckExpression(ctxt.takeBranch(pi, false, guardBlame),
                            c.getElseExpression());
                    return new Pair<>(
                            thenExpr.left().mergeAcrossBranch(pi, elseExpr.left()),
                            thenExpr.right().disjunct(elseExpr.right())
                    );
                }
                case CtConstructorCall<?> constr -> {
                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(constr));
                    Blame constrBlame = Blame.oneSite(parentAnalyzer.lineIndexer.lookupOrCreateConstr(constr));
                    if (!parentAnalyzer.isIntrasourceCall(c)) {
                        return typecheckExprList(ctxt, constr.getArguments()).mapRight(constrBlame::disjunct);
                    }
                    Procedure p = parentAnalyzer.procedureOfCall(c).orElseThrow(() ->
                            new IllegalStateException("Expected procedure lookup to succeed"));

                    Set<PhiInput> argSet = IntStream.range(0, constr.getArguments().size())
                            .mapToObj(i -> new Arg(p, i))
                            .collect(Collectors.toUnmodifiableSet());
                    Set<PhiOutput> retSet = Set.of(new Ret(p));

                    return typecheckInvocation(ctxt, null, constrBlame, c, p, constr.getArguments(), argSet, retSet);
                }
                case CtLambda<?> l -> {
                    return typecheckOpaque(ctxt, l);
                }
                case CtExecutableReferenceExpression<?, ?> ere -> {
                    //TODO: consider handling this better
                    return typecheckOpaque(ctxt, ere);
                }
                case CtThisAccess<?> t -> {
                    return typecheckOpaque(ctxt, t).mapRight(Blame.oneSite(new Self())::disjunct);
                }
                case null -> {
                    return new Pair<>(ctxt, Blame.zero());
                }
                default -> throw new IllegalArgumentException("Unexpected expression: " + expr);
            }
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
