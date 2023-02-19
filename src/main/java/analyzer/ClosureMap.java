package analyzer;
import core.codemodel.elements.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtMethod;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClosureMap {
    public interface ClosedOver {}
    final Map<Procedure, ClosureType> data = new HashMap<>();
    final ProgramAnalyzer parentAnalyzer;

    ClosureMap(ProgramAnalyzer parentAnalyzer) {
        this.parentAnalyzer = parentAnalyzer;
    }

    void computeClosureForMethod(CtMethod<?> method) {
        CtProcedure procedure = new CtProcedure(method);
        Procedure procKey = parentAnalyzer.procedureIndexer.lookupOrCreate(procedure);

        ClosureType closure = new ClosureType(procedure);

        closure.processStmtList(procedure.body);

        data.put(procKey, closure);
    }

    private static class TouchCondition {
        final Set<Call> condition;

        private TouchCondition() {
            condition = Set.of();
        }

        private TouchCondition(Set<Call> condition) {
            this.condition = Set.copyOf(condition);
        }

        boolean isAlways() {
            return condition.isEmpty();
        }

        static TouchCondition always() {
            return new TouchCondition();
        }

        static TouchCondition ifCall(Call c) {
            return new TouchCondition(Set.of(c));
        }

        TouchCondition or(TouchCondition other) {
            if (isAlways() || other.isAlways()) {
                return always();
            }
            return new TouchCondition(Stream.concat(condition.stream(), other.condition.stream())
                    .collect(Collectors.toUnmodifiableSet()));
        }
    }

    private class ClosureType implements Cloneable {
        final CtProcedure procedure;
        final Set<ClosedOver> reads = new HashSet<>();
        final Map<ClosedOver, TouchCondition> writes = new HashMap<>();
        final Set<Call> calls = new HashSet<>();

        ClosureType(CtProcedure procedure) {
            this.procedure = procedure;
        }

        public ClosureType clone() {
            ClosureType other = new ClosureType(this.procedure);
            other.reads.addAll(this.reads);
            other.writes.putAll(this.writes);
            other.calls.addAll(this.calls);
            return other;
        }

        ClosureType merge(ClosureType other) {
            reads.addAll(other.reads);
            other.writes.forEach((v, tc) -> writes.merge(v, tc, TouchCondition::or));
            calls.addAll(other.calls);
            return this;
        }

        static boolean mutates(UnaryOperatorKind u) {
            return switch (u) {
                case PREDEC, PREINC, POSTDEC, POSTINC -> true;
                default -> false;
            };
        }

        ClosureType processStmt(CtStatement stmt) {
            if (stmt == null) {
                return this;
            }
            switch (stmt) {
                case CtAssert<?> a ->
                    processRead(a.getAssertExpression());
                case CtOperatorAssignment<?, ?> o -> {
                    processWrite(o.getAssigned());
                    processRead(o.getAssigned());
                    processRead(o.getAssignment());
                }
                case CtAssignment<?, ?> assignment -> {
                    processWrite(assignment.getAssigned());
                    processRead(assignment.getAssignment());
                }
                case CtIf i -> {
                    processStmt(i.getThenStatement());
                    merge(clone().processStmt(i.getElseStatement()));
                    processRead(i.getCondition());
                }
                case CtInvocation<?> i ->
                    processRead(i);
                case CtLocalVariable<?> lv -> {
                    Variable v = parentAnalyzer.varIndexer.lookupOrCreate(lv);
                    reads.remove(v);
                    writes.remove(v);
                }
                case CtCFlowBreak ignored ->
                    throw new IllegalStateException("Unexpected statement " + stmt);
                case CtSwitch<?> s -> {
                    ClosureType current = clone();
                    for (CtCase<?> c : s.getCases()) {
                        merge(current.clone().processStmtList(c));
                        processRead(c.getCaseExpression());
                    }
                    processRead(s.getSelector());
                }
                case CtSynchronized s ->
                    processStmtList(s.getBlock());
                case CtUnaryOperator<?> u ->
                    processRead(u);
                case CtTry t ->
                    //TODO: handle abnormal control flow through exceptions
                    processStmtList(t.getBody());
                case CtBlock<?> b ->
                    processStmtList(b);
                default -> {
                    System.out.println("unhandled stmt: " + stmt);
                }
            }
            return this;
        }

        ClosureType processStmtList(CtStatementList stmtList) {
            if (stmtList == null || stmtList.getStatements().isEmpty()) {
                return this;
            }
            int break_count = 0;
            if (stmtList.getLastStatement() instanceof CtCFlowBreak) {
                break_count = 1;
                if (stmtList.getLastStatement() instanceof CtReturn<?> r) {
                    processRead(r.getReturnedExpression());
                }
            }
            for (int i = stmtList.getStatements().size() - 1 - break_count; i >=0; i--) {
                processStmt(stmtList.getStatement(i));
            }
            return this;
        }

        ClosureType processRead(CtExpression<?> expr) {
            if (expr == null) {
                return this;
            }
            switch (expr) {
                case CtInvocation<?> i -> {
                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(i);
                    calls.add(c);
                    processWrite(i.getTarget(), TouchCondition.ifCall(c));
                    processRead(i.getTarget());
                    i.getArguments().forEach(this::processRead);
                }
                case CtConstructorCall<?> cc -> {
                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(cc);
                    calls.add(c);
                    cc.getArguments().forEach(this::processRead);
                }
                //these are phrased as "Access" not "Read/Write" because both can flow here and both are handled the same
                case CtFieldAccess<?> fr -> {
                    if (fr.getTarget() instanceof CtThisAccess<?>) {
                        Field f = parentAnalyzer.fieldIndexer.lookupOrCreate(fr.getVariable().getDeclaration());
                        reads.add(f);
                    } else {
                        processRead(fr.getTarget());
                    }
                }
                case CtSuperAccess<?> ignored -> {
                    reads.add(new Self());
                }
                //it's important that Field case comes before Variable!
                case CtVariableAccess<?> vr -> {
                    Variable v = parentAnalyzer.varIndexer.lookupOrCreate(vr.getVariable().getDeclaration());
                    reads.add(v);
                }
                case CtTypeAccess<?> ignored -> {/*noop - indicated a static method call */}
                case CtThisAccess<?> ignored -> {
                    reads.add(new Self());
                }
                case CtLiteral<?> ignored -> {/*noop*/}
                case CtBinaryOperator<?> b -> {
                    //TODO: think about short circuiting
                    processRead(b.getRightHandOperand());
                    processRead(b.getLeftHandOperand());
                }
                case CtUnaryOperator<?> u -> {
                    if (mutates(u.getKind())) {
                        processWrite(u.getOperand());
                    }
                    processRead(u.getOperand());
                }
                case CtLambda<?> ignored -> {
                    //TODO: handle lambdas... who would use those?
                }
                case CtConditional<?> c -> {
                    processRead(c.getThenExpression());
                    merge(clone().processRead(c.getElseExpression()));
                    processRead(c.getCondition());
                }
                case CtArrayAccess<?, ?> a -> {
                    processRead(a.getTarget());
                    processRead(a.getIndexExpression());
                }
                case CtNewArray<?> n -> {
                    n.getElements().forEach(this::processRead);
                    n.getDimensionExpressions().forEach(this::processRead);
                }
                case CtExecutableReferenceExpression<?, ?> n -> {
                    //TODO: lambdas... handle them...
                }
                default -> {
                    System.out.println("Unhandled read: " + expr);}
            }
            return this;
        }

        ClosureType processWrite(CtExpression<?> expr) {
            return processWrite(expr, TouchCondition.always());
        }

        ClosureType processWrite(CtExpression<?> expr, TouchCondition condition) {
            if (expr == null) {
                return this;
            }
            switch (expr) {
                case CtArrayAccess<?, ?> a -> {
                    processWrite(a.getTarget(), condition);
                    processRead(a.getTarget());
                    processRead(a.getIndexExpression());
                }
                //these are phrased as "Access" not "Read/Write" because both can flow here and both are handled the same
                case CtFieldAccess<?> fw -> {
                    if (fw.getTarget() instanceof CtThisAccess<?>) {
                        Field f = parentAnalyzer.fieldIndexer.lookupOrCreate(fw.getVariable().getDeclaration());
                        reads.remove(f);
                        writes.merge(f, condition, TouchCondition::or);
                    } else {
                        processWrite(fw.getTarget(), condition);
                        processRead(fw.getTarget());
                    }
                }
                case CtSuperAccess<?> vw -> {
                    writes.merge(new Self(), condition, TouchCondition::or);
                }
                //it's important that Field case comes before Variable!
                case CtVariableAccess<?> vw -> {
                    Variable v = parentAnalyzer.varIndexer.lookupOrCreate(vw.getVariable().getDeclaration());
                    reads.remove(v);
                    writes.merge(v, condition, TouchCondition::or);
                }
                case CtThisAccess<?> ignored ->
                    writes.merge(new Self(), condition, TouchCondition::or);
                case CtInvocation<?> ignored -> {
                    /*
                    this is a case like a.b().c() - which could technically behave as a write to a if b() returns
                    self, but I'm not gonna track that , I'm gonna ignore it
                     */
                }
                case CtConstructorCall<?> ignored -> {
                    //can't think of anything to track here
                }
                case CtTypeAccess<?> ignored -> {/* noop - indicated a call to a static method*/}
                case CtConditional<?> c -> {
                    processWrite(c.getThenExpression(), condition);
                    merge(clone().processWrite(c.getElseExpression(), condition));
                    processRead(c.getCondition());
                }
                default ->
                    throw new IllegalStateException("Unexpected write to expression: " + expr);
            }
            return this;
        }
    }
}
