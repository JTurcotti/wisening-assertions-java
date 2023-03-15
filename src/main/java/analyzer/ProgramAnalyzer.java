package analyzer;

import analyzer.formulaproviders.TotalProvider;
import core.codemodel.Indexer;
import core.codemodel.SourcePos;
import core.codemodel.elements.*;
import core.codemodel.events.*;
import core.codemodel.types.Blame;
import core.dependencies.Dependency;
import core.formula.Formula;
import serializable.SerialFormulas;
import serializable.SerialLabels;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.visitor.Filter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProgramAnalyzer {
    final CtModel model;
    final Indexer.BySourcePos<Procedure, CtProcedure> procedureIndexer;
    public final Indexer.BySourceLine lineIndexer;
    final Indexer.BySourcePos<Call, CtVirtualCall> callIndexer;
    final Indexer.BySourcePos<Pi, CtVirtualBranch> branchIndexer;
    final Indexer.BySourcePos<Field, CtField<?>> fieldIndexer;
    final Indexer.BySourcePos<Variable, CtVariable<?>> varIndexer;
    final Indexer.BySourcePos<Assertion, CtWiseningAssert> assertionIndexer;

    //wrapped map Procedure -> FullContext
    final Typechecker typechecker = new Typechecker(this);
    final ClosureMap closures = new ClosureMap(this);

    //tracks the calls of each procedure, including through polymorphism
    final CallMap callMap = new CallMap(this);

    //I know this is bad practice... it's used only to perform optional validation.
    //i.e. if it's not present certain checks will not be performed but that's it
    public static Optional<ProgramAnalyzer> availableInstance = Optional.empty();

    public ProgramAnalyzer(String sourcePath, Optional<SerialLabels> serializedLabels) {
        availableInstance = Optional.of(this); // I know this is bad practice... see above

        Launcher launcher = new Launcher();
        launcher.addInputResource(sourcePath);
        launcher.getEnvironment().setComplianceLevel(18);
        launcher.buildModel();

        if (serializedLabels.isPresent()) {
            procedureIndexer = new Indexer.BySourcePos<>(Procedure::new,
                    serializedLabels.get().procedureLabels(), serializedLabels.get().nextProcedure());
            lineIndexer = new Indexer.BySourceLine(
                    serializedLabels.get().lineLabels(), serializedLabels.get().nextLine());
            callIndexer = new Indexer.BySourcePos<>(Call::new,
                    serializedLabels.get().callLabels(), serializedLabels.get().nextCall());
            branchIndexer = new Indexer.BySourcePos<>(Pi::new,
                    serializedLabels.get().branchLabels(), serializedLabels.get().nextBranch());
            fieldIndexer = new Indexer.BySourcePos<>(Field::new,
                    serializedLabels.get().fieldLabels(), serializedLabels.get().nextField());
            varIndexer = new Indexer.BySourcePos<>(Variable::new,
                    serializedLabels.get().varLabels(), serializedLabels.get().nextVar());
            assertionIndexer = new Indexer.BySourcePos<>(Assertion::new,
                    serializedLabels.get().assertionLabels(), serializedLabels.get().nextAssertion());
        } else {
            procedureIndexer = new Indexer.BySourcePos<>(Procedure::new);
            lineIndexer = new Indexer.BySourceLine();
            callIndexer = new Indexer.BySourcePos<>(Call::new);
            branchIndexer = new Indexer.BySourcePos<>(Pi::new);
            fieldIndexer = new Indexer.BySourcePos<>(Field::new);
            varIndexer = new Indexer.BySourcePos<>(Variable::new);
            assertionIndexer = new Indexer.BySourcePos<>(Assertion::new);
        }

        model = launcher.getModel();
        closures.computeClosures();
        closures.determineOverrides();
        closures.transitivelyClose();
        typechecker.performTypechecking();
        callMap.compute();
    }

    public boolean isIntrasourceCall(Call c) {
        return callIndexer.lookupAux(c)
                .flatMap(CtVirtualCall::getProcedureSourcePos)
                .isPresent();
    }

    public Optional<Procedure> procedureCalledBy(Call c) {
        return callIndexer.lookupAux(c)
                .flatMap(CtVirtualCall::getProcedureSourcePos)
                .flatMap(procedureIndexer::lookup);
    }

    public Optional<Procedure> procedureContainingCall(Call c) {
        return callIndexer.lookupAux(c).map(call -> call.containingProcedure);
    }

    public Collection<Call> callsToProcedure(Procedure p) {
        return callMap.callsToProcedure(p);
    }

    public Collection<Procedure> procedures() {
        return procedureIndexer.outputs();
    }

    public CtProcedure lookupProcedure(Procedure p) {
        return procedureIndexer.lookupAux(p).orElseThrow(() ->
                new IllegalArgumentException("Expected procedure to be present: " + p));
    }

    private CtProcedure lookupProcedure(int i) {
        return lookupProcedure(new Procedure(i));
    }

    public Variable lookupParamVar(Arg arg) {
        return lookupProcedure(arg.procedure()).getParamVariables().get(arg.num());
    }

    public List<Variable> lookupParamVars(Procedure p) {
        return lookupProcedure(p).getParamVariables();
    }

    public Assertion indexOfAssertion(CtAssert<?> ctAssert) {
        return assertionIndexer.lookup(SourcePos.fromSpoon(ctAssert.getPosition())).orElseThrow(
                () -> new IllegalStateException("Expected Assertion to be indexed")
        );
    }

    public Pi indexOfIfBranch(CtIf ctIf) {
        return branchIndexer.lookup(SourcePos.fromSpoon(ctIf.getPosition())).orElseThrow(
                () -> new IllegalStateException("Expected If to be indexed")
        );
    }

    public CtVirtualCall lookupCall(Call c) {
        return callIndexer.lookupAux(c).orElseThrow(() -> new IllegalArgumentException("Expected call to be indexed: " + c));
    }

    private CtVirtualCall lookupCall(int i) {
        return lookupCall(new Call(i));
    }


    /*
    Given an assertion, parse the list of mutables it targets
     */
    public Set<Mutable> parseWiseningAssertionTargets(CtAssert<?> a) {
        //by default, any variables or fields mentioned in the assertion are targeted
        Filter<CtVariableAccess<?>> filter = ignored -> true;
        if (!a.getComments().isEmpty()) {
            Set<String> commented = new HashSet<>();
            for (CtComment c : a.getComments()) {
                Arrays.stream(c.getContent().split(",")).map(String::trim).forEach(commented::add);
            }
            filter = va -> commented.contains(va.getVariable().getSimpleName());
        }
        Set<Mutable> targets = new HashSet<>();
        for (CtVariableAccess<?> v : a.getElements(filter)) {
            if (v instanceof CtFieldAccess<?> f) {
                if (!f.getVariable().isStatic() && f.getTarget() instanceof CtThisAccess<?>) {
                    //don't target static fields
                    targets.add(fieldIndexer.lookupOrCreate(f.getVariable().getDeclaration()));
                }
            } else {
                targets.add(varIndexer.lookupOrCreate(v.getVariable().getDeclaration()));
            }
        }
        return targets;
    }

    public Blame getOutputBlame(Procedure p, PhiOutput out) {
        if (!typechecker.typecheckedProcedures.containsKey(p)) {
            throw new IllegalArgumentException("Expected passed procedure to be present in typechecked table: " + p);
        }
        return typechecker.typecheckedProcedures.get(p).getResult(out);
    }

    public Blame getAssertionBlame(Assertion assertion) {
        return assertionIndexer.lookupAux(assertion).orElseThrow(() ->
                        new IllegalArgumentException("Expected assertion to be indexed by analyzer: " + assertion))
                .blame();
    }

    public Procedure procedureOfLine(Line l) {
        return lineIndexer.lookupAux(l).orElseThrow(() ->
                new IllegalArgumentException("Exepected line to be indexed by analyzer: " + l)).left();
    }

    public Procedure procedureOfAssertion(Assertion assertion) {
        return assertionIndexer.lookupAux(assertion).orElseThrow(() ->
                        new IllegalArgumentException("Expected assertion to be indexed by analyzer: " + assertion))
                .procedure();
    }

    public Map<PhiOutput, Blame> getResultBlamesForProcedure(Procedure proc) {
        if (!typechecker.typecheckedProcedures.containsKey(proc)) {
            throw new IllegalArgumentException("Expected procedure to be typechecked: " + proc);
        }
        return typechecker.typecheckedProcedures.get(proc).resultBlames();
    }

    public Collection<Line> getAllLines() {
        return lineIndexer.outputs();
    }

    public Collection<Line> getAssertionDependentLines(Assertion assertion) {
        //TODO: for now do nothing fancy here - but consider filtering for performance at some point
        return getAllLines();
    }

    public Collection<Assertion> getAllAssertions() {
        return assertionIndexer.outputs();
    }

    public boolean hasImplementation(Procedure p) {
        CtProcedure proc = lookupProcedure(p);
        return !proc.isAbstract() && !proc.isInterfaceMethod();
    }

    public List<Procedure> getOverrides(Procedure p) {
        assert closures.data.get(p).getOverrides().contains(p); //TODO: delete this
        return closures.data.get(p).getOverrides();
    }

    public CtVariable<?> lookupVar(Variable v) {
        if (varIndexer.lookupAux(v).isEmpty()) {
            throw new IllegalArgumentException("Expected variable to be indexed: " + v);
        }
        return varIndexer.lookupAux(v).get();
    }

    public CtVariable<?> lookupVar(int i) {
        return lookupVar(new Variable(i));
    }

    public CtField<?> lookupField(Field f) {
        if (fieldIndexer.lookupAux(f).isEmpty()) {
            throw new IllegalArgumentException("Expected field to be indexed: " + f);
        }
        return fieldIndexer.lookupAux(f).get();
    }

    public CtField<?> lookupField(int i) {
        return lookupField(new Field(i));
    }

    public boolean isArgVar(CtVariable<?> v) {
        return v instanceof CtParameter<?>;
    }

    public boolean isArgVar(Variable v) {
        return isArgVar(lookupVar(v));
    }

    public boolean sameParam(Variable v1, Variable v2) {
        if (lookupVar(v1) instanceof CtParameter<?> p1 && lookupVar(v2) instanceof CtParameter<?> p2) {
            if (p1.getParent() instanceof CtMethod<?> m1 && p2.getParent() instanceof CtMethod<?> m2
                    && (m1.isOverriding(m2) || m2.isOverriding(m1))
                    && p1.getParent().getParameters().indexOf(p1) == p2.getParent().getParameters().indexOf(p2)) {
                return true;
            }
        }
        return false;
    }

    public Optional<Arg> asParamOf(Variable v, Procedure p) {
        List<Variable> params = lookupParamVars(p);
        return IntStream.range(0, params.size())
                .filter(i -> sameParam(v, params.get(i)))
                .mapToObj(i -> new Arg(p, i))
                .findFirst();
    }

    public Optional<Variable> asParamOf(Arg a, Procedure p) {
        return lookupParamVars(p).stream()
                .filter(v -> sameParam(v, lookupParamVar(a)))
                .findFirst();
    }

    public int numBranches() {
        return branchIndexer.inputs().size();
    }

    public int numAssertions() {
        return assertionIndexer.inputs().size();
    }

    public SerialFormulas serializeFormulas() {
        Set<ComputedEvent> keys = new HashSet<>(getAllAssertions());
        TotalProvider concreteProvider = new TotalProvider(this);
        //close keys over dependencies according to the formula provider
        while (true) {
            Set<ComputedEvent> keyDeps = keys.stream()
                    .flatMap(event -> concreteProvider.genericFormulaProvider().get(event).getDeps().stream())
                    //restrict to computed event dependencies - e.g. ignore Pi deps
                    .flatMap(dep -> ComputedEvent.ofDependencyOpt(dep).stream())
                    .collect(Collectors.toUnmodifiableSet());
            if (keys.containsAll(keyDeps)) {
                break;
            }
            keys.addAll(keyDeps);
        }
        final HashMap<Event, Formula<? extends Dependency>> data = new HashMap<>();
        keys.forEach(event -> data.put(event, concreteProvider.genericFormulaProvider().get(ComputedEvent.ofEvent(event))));
        return new SerialFormulas(data);
    }

    public SerialLabels serializeLabels() {
        return new SerialLabels(
                procedureIndexer.getIndex(), procedureIndexer.nextIndex(),
                lineIndexer.getIndex(), lineIndexer.nextIndex(),
                callIndexer.getIndex(), callIndexer.nextIndex(),
                branchIndexer.getIndex(), branchIndexer.nextIndex(),
                fieldIndexer.getIndex(), fieldIndexer.nextIndex(),
                varIndexer.getIndex(), varIndexer.nextIndex(),
                assertionIndexer.getIndex(), assertionIndexer.nextIndex());
    }
}
