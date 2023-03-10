package analyzer;

import core.codemodel.SourcePos;
import core.codemodel.elements.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;
import util.Util;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
TODO: test closure map better - for example the touchcondition logic for calls `a.foo()` being interpreted as
 writes to `a` iff `foo()` writes to self
*/

public class ClosureMap {
    final Map<Procedure, ClosureType> data = new HashMap<>();
    final ProgramAnalyzer parentAnalyzer;

    ClosureType lookupByCall(Call c) {
        if (parentAnalyzer.procedureCalledBy(c).isEmpty()) {
            throw new IllegalStateException("The parent analyzer does not have a procedure for the call: " + c);
        }
        Procedure callProc = parentAnalyzer.procedureCalledBy(c).get();
        if (!data.containsKey(callProc)) {
            throw new IllegalStateException("Closure map incomplete - no entry for procedure: " + callProc);
        }
        return data.get(callProc);
    }

    ClosureType lookupByElement(CtElement elem) {
        Optional<Procedure> proc = parentAnalyzer.procedureIndexer.lookup(SourcePos.fromSpoon(elem.getPosition()));
        if (proc.isEmpty()) {
            throw new IllegalStateException("The provided element is not associated with a procedure: " + elem);
        }
        if (!data.containsKey(proc.get())) {
            throw new IllegalStateException("Closure map incomplete - no entry for procedure: " + proc);
        }
        return data.get(proc.get());
    }

    void computeClosures() {
        parentAnalyzer.model.getElements(new TypeFilter<>(CtMethod.class))
                .forEach(this::computeClosureForMethod);
        parentAnalyzer.model.getElements(new TypeFilter<>(CtConstructor.class))
                .forEach(this::computeClosureForConstructor);
    }

    void transitivelyClose() {
        int rounds = 0;
        boolean updated = true;
        while (updated) {
            if (rounds++ > 2 * data.size()) {
                throw new IllegalStateException("Fixpoint propagation does not seem to be converging; two passes " +
                        "over the whole data structure (one for overrides and one for calls) should have been enough");
            }

            updated = false;
            for (ClosureType ct : data.values()) {
                if (ct.performUpdate()) {
                    updated = true;
                }
            }
        }
    }

    void determineOverrides() {
        for (Map.Entry<Procedure, ClosureType> eChild : data.entrySet()) {
            for (Map.Entry<Procedure, ClosureType> eParent : data.entrySet()) {
                if (eChild.getValue().equals(eParent.getValue()) ||  //yes this is redundant...
                        // but I'm relying on the fact that it occurs so... defensive programming?
                        eChild.getValue().procedure.isOverriding(eParent.getValue().procedure)) {
                    eParent.getValue().overrides.add(eChild.getValue());
                }
            }
        }
    }

    ClosureMap(ProgramAnalyzer parentAnalyzer) {
        this.parentAnalyzer = parentAnalyzer;
    }

    void computeClosureForMethod(CtMethod<?> method) {
        computeClosureForProcedure(new CtProcedure(method, parentAnalyzer));
    }

    void computeClosureForConstructor(CtConstructor<?> constr) {
        if (!constr.isImplicit()) {
            computeClosureForProcedure(new CtProcedure(constr, parentAnalyzer));
        }
    }

    void computeClosureForProcedure(CtProcedure procedure) {
        Procedure procKey = parentAnalyzer.procedureIndexer.lookupOrCreate(procedure);

        ClosureType closure = new ClosureType(procedure);

        closure.processStmtList(procedure.body);

        for (Variable v : procedure.getParamVariables()) {
            closure.reads.remove(v);
            closure.writes.remove(v);
        }

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

        boolean satisfied(ClosureMap c) {
            if (isAlways()) {
                return true;
            }
            return condition.stream().anyMatch(call ->
                    c.parentAnalyzer.isIntrasourceCall(call) ?
                            c.lookupByCall(call).writesSelf :
                            Config.NONLOCAL_METHOD_MUTATES_SELF
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(condition);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TouchCondition other) {
                return condition.equals(other.condition);
            }
            return false;
        }
    }

    class ClosureType {

        //the following fields are generated initially for a ClosureType
        final CtProcedure procedure;
        private final Set<ClosedOver> reads = new HashSet<>();
        private final Map<ClosedOver, TouchCondition> writes = new HashMap<>();
        private final Set<Call> calls = new HashSet<>();
        private final Set<Call> selfCalls = new HashSet<>();

        //the following utility fields are populated after all
        //closure types are generated

        //the set of closuretypes that extend/override/implement this closuretype
        //(including self)
        final Set<ClosureType> overrides = new HashSet<>(Set.of(this));

        //the following fields are populated by fixpoint propagation
        //after all closure types are generated
        private boolean readsSelf = false;
        private boolean writesSelf = false;

        private Set<Field> fieldReads = new HashSet<>();
        private Set<Field> fieldWrites = new HashSet<>();



        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        private Set<Field> getFieldReads() {
            return Stream.concat(fieldReads.stream(),
                    reads.stream().flatMap(c -> {
                        if (c instanceof Field f) {
                            return Stream.of(f);
                        }
                        return Stream.empty();
                    })).collect(Collectors.toUnmodifiableSet());
        }

        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        boolean readsSelf() {
            return readsSelf ||
                    reads.contains(new Self()) ||
                    !getFieldReads().isEmpty();
        }

        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        private Set<Field> getFieldWrites() {
            return Stream.concat(fieldWrites.stream(),
                    writes.keySet().stream().flatMap(c -> {
                        if (c instanceof Field f && (writes.get(c).satisfied(ClosureMap.this))) {
                            return Stream.of(f);
                        }
                        return Stream.empty();
                    })).collect(Collectors.toUnmodifiableSet());
        }

        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        boolean writesSelf() {
            return writesSelf ||
                    (writes.containsKey(new Self()) && writes.get(new Self()).satisfied(ClosureMap.this)) ||
                    !getFieldWrites().isEmpty();
        }

        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        private Set<Variable> getVariableReads() {
            return reads.stream().map(ClosedOver::asVariable).flatMap(Optional::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }

        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        private Set<Variable> getVariableWrites() {
            return writes.keySet().stream().flatMap(c ->
                    c instanceof Variable v && writes.get(v).satisfied(ClosureMap.this)?
                            Stream.of(v): Stream.empty())
                    .collect(Collectors.toUnmodifiableSet());
        }

        Set<PhiInput> getInputs() {
            return Stream.concat(
                            Stream.concat(getFieldWrites().stream(), getVariableWrites().stream()),
                            Stream.concat(getFieldReads().stream(), getVariableReads().stream()))
                    .collect(Collectors.toUnmodifiableSet());
        }

        //WARNING: not guaranteed to return correct value until determineOverrides and transitivelyClose are called
        Set<PhiOutput> getOutputs() {
            return Stream.concat(getFieldWrites().stream(), getVariableWrites().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        private boolean performUpdate() {
            boolean updated = false;
            boolean newWritesSelf = overrides.stream().anyMatch(ClosureType::writesSelf);
            if (writesSelf != newWritesSelf) {
                updated = true;
                writesSelf = newWritesSelf;
            }
            boolean newReadsSelf = overrides.stream().anyMatch(ClosureType::readsSelf);
            if (readsSelf != newReadsSelf) {
                updated = true;
                readsSelf = newReadsSelf;
            }
            Set<Field> newFieldReads = Stream.concat(
                    overrides.stream().map(ClosureType::getFieldReads)
                            .flatMap(Set::stream)
                            //this is here to ensure that superclasses don't think they can access
                            //fields of subclasses
                            .filter(procedure::canAccessField),
                    selfCalls.stream().flatMap(call ->
                        lookupByCall(call).getFieldReads().stream()
                    )).collect(Collectors.toUnmodifiableSet());
            if (!fieldReads.equals(newFieldReads)) {
                updated = true;
                fieldReads = newFieldReads;
            }
            Set<Field> newFieldWrites = Stream.concat(
                    overrides.stream().map(ClosureType::getFieldWrites)
                            .flatMap(Set::stream)
                            //this is here to ensure that superclasses don't think they can access
                            //fields of subclasses
                            .filter(procedure::canAccessField),
                    selfCalls.stream().flatMap(call ->
                            lookupByCall(call).getFieldWrites().stream()
                    )).collect(Collectors.toUnmodifiableSet());
            if (!fieldWrites.equals(newFieldWrites)) {
                updated = true;
                fieldWrites = newFieldWrites;
            }
            return updated;
        }

        ClosureType(CtProcedure procedure) {
            this.procedure = procedure;
        }

        ClosureType copy() {
            ClosureType other = new ClosureType(this.procedure);
            other.reads.addAll(this.reads);
            other.writes.putAll(this.writes);
            other.calls.addAll(this.calls);
            return other;
        }

        ClosureType merge(ClosureType other) {
            reads.addAll(other.reads);

            //add all writes that potentially occur only in one branch to the read set
            //this will overapproximate because some of these assymetries only occur
            //conditionally, but overapproximating reads is not a big deal - overapproximating
            //writes leads to false implicit flows
            reads.addAll(Stream.concat(
                    writes.keySet().stream(),
                            other.writes.keySet().stream()).distinct()
                    .filter(c -> writes.containsKey(c) != other.writes.containsKey(c) ||
                            (writes.containsKey(c) && other.writes.containsKey(c) &&
                                    !writes.get(c).equals(other.writes.get(c)))
                    )
                    .toList());

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

        private ClosureType processStmt(CtStatement stmt) {
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
                    merge(copy().processStmt(i.getElseStatement()));
                    processRead(i.getCondition());
                }
                case CtInvocation<?> i ->
                    processRead(i);
                case CtLocalVariable<?> lv -> {
                    Variable v = parentAnalyzer.varIndexer.lookupOrCreate(lv);
                    reads.remove(v);
                    writes.remove(v);
                    processRead(lv.getAssignment());
                }
                case CtCFlowBreak ignored ->
                    throw new IllegalStateException("Unexpected statement " + stmt);
                case CtSwitch<?> s -> {
                    ClosureType current = copy();
                    for (CtCase<?> c : s.getCases()) {
                        merge(current.copy().processStmtList(c));
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
                case CtWhile w -> {
                    CtProcedure fixpoint = new CtProcedure(w, parentAnalyzer);
                    Procedure proc = parentAnalyzer.procedureIndexer.lookupOrCreate(fixpoint);

                    UnaryOperator<ClosureType> processing = c ->
                            c.processStmt(w.getBody()).processRead(w.getLoopingExpression());

                    merge(processing.apply(copy()));

                    data.put(proc, processing.apply(new ClosureType(fixpoint)));

                    Call call = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(w, procedure));
                    calls.add(call);
                    selfCalls.add(call);
                }
                case CtDo d -> {
                    CtProcedure fixpoint = new CtProcedure(d, parentAnalyzer);
                    Procedure proc = parentAnalyzer.procedureIndexer.lookupOrCreate(fixpoint);

                    UnaryOperator<ClosureType> processing = c ->
                            c.processRead(d.getLoopingExpression()).processStmt(d.getBody());

                    merge(processing.apply(copy()));

                    data.put(proc, processing.apply(new ClosureType(fixpoint)));

                    Call call = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(d, procedure));
                    calls.add(call);
                    selfCalls.add(call);
                }
                case CtFor f -> {
                    CtProcedure fixpoint = new CtProcedure(f, parentAnalyzer);
                    Procedure proc = parentAnalyzer.procedureIndexer.lookupOrCreate(fixpoint);

                    UnaryOperator<ClosureType> processing = c -> {
                        c.processRead(f.getExpression());
                        Util.forEachRev(f.getForUpdate(), c::processStmt);
                        return c.processStmt(f.getBody());
                    };

                    data.put(proc, processing.apply(new ClosureType(fixpoint)));

                    merge(processing.apply(copy()));

                    Util.forEachRev(f.getForInit(), this::processStmt);

                    Call call = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(f, procedure));
                    calls.add(call);
                    selfCalls.add(call);
                }
                case CtForEach f -> {
                    CtProcedure fixpoint = new CtProcedure(f, parentAnalyzer);
                    Procedure proc = parentAnalyzer.procedureIndexer.lookupOrCreate(fixpoint);

                    data.put(proc, new ClosureType(fixpoint).processStmt(f.getBody()));

                    merge(copy().processStmt(f.getBody()).processStmt(f.getVariable()).processRead(f.getExpression()));

                    Call call = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(f, procedure));
                    calls.add(call);
                    selfCalls.add(call);
                }
                default ->
                    throw new IllegalStateException("Unexpected statement to process: " + stmt);
            }
            return this;
        }

        private ClosureType processStmtList(CtStatementList stmtList) {
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

        private ClosureType processRead(CtExpression<?> expr) {
            if (expr == null || expr.isImplicit()) {
                return this;
            }
            switch (expr) {
                case CtInvocation<?> i -> {
                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(i, procedure));
                    calls.add(c);
                    if ((i.getTarget() instanceof CtThisAccess<?> ||
                            i.getTarget() instanceof CtSuperAccess<?> ||
                            i.getTarget() == null) &&
                            parentAnalyzer.isIntrasourceCall(c)) {
                        selfCalls.add(c);
                    }
                    processWrite(i.getTarget(), TouchCondition.ifCall(c));
                    processRead(i.getTarget());
                    i.getArguments().forEach(this::processRead);
                }
                case CtConstructorCall<?> cc -> {
                    Call c = parentAnalyzer.callIndexer.lookupOrCreate(new CtVirtualCall(cc, procedure));
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
                    merge(copy().processRead(c.getElseExpression()));
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
                default ->
                    throw new IllegalStateException("Unexpected read to expression: " + expr);
            }
            return this;
        }

        private ClosureType processWrite(CtExpression<?> expr) {
            return processWrite(expr, TouchCondition.always());
        }

        private ClosureType processWrite(CtExpression<?> expr, TouchCondition condition) {
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
                case CtInvocation<?> inv -> {
                    //for example a.foo().bar() is treated as a write to `a`
                    processWrite(inv.getTarget());
                }
                case CtConstructorCall<?> ignored -> {
                    //can't think of anything to track here
                }
                case CtTypeAccess<?> ignored -> {/* noop - indicated a call to a static method*/}
                case CtConditional<?> c -> {
                    processWrite(c.getThenExpression(), condition);
                    merge(copy().processWrite(c.getElseExpression(), condition));
                    processRead(c.getCondition());
                }
                default ->
                    throw new IllegalStateException("Unexpected write to expression: " + expr);
            }
            return this;
        }

        List<Procedure> getOverrides() {
            return overrides.stream().map(ct -> parentAnalyzer.procedureIndexer.lookupOrCreate(ct.procedure)).toList();
        }

        Collection<Call> getCalls() {
            return calls;
        }
    }
}
