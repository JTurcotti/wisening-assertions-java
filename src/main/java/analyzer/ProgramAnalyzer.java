package analyzer;

import core.codemodel.Indexer;
import core.codemodel.elements.*;
import core.codemodel.events.Assertion;
import core.codemodel.events.Line;
import core.codemodel.events.Pi;
import core.codemodel.types.Blame;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.visitor.Filter;

import java.util.*;

public class ProgramAnalyzer {
    final CtModel model;
    final Indexer.BySourcePos<Procedure, CtProcedure> procedureIndexer = new Indexer.BySourcePos<>(Procedure::new);
    final Indexer.BySourceLine lineIndexer = new Indexer.BySourceLine();
    final Indexer.BySourcePos<Call, CtVirtualCall> callIndexer = new Indexer.BySourcePos<>(Call::new);
    final Indexer.BySourcePos<Pi, CtVirtualBranch> branchIndexer = new Indexer.BySourcePos<>(Pi::new);
    final Indexer.BySourcePos<Field, CtField<?>> fieldIndexer = new Indexer.BySourcePos<>(Field::new);
    final Indexer.BySourcePos<Variable, CtVariable<?>> varIndexer = new Indexer.BySourcePos<>(Variable::new);
    final Indexer.BySourcePos<Assertion, CtWiseningAssert> assertionIndexer = new Indexer.BySourcePos<>(Assertion::new);

    //wrapped map Procedure -> FullContext
    final Typechecker typechecker = new Typechecker(this);
    final ClosureMap closures = new ClosureMap(this);

    public ProgramAnalyzer(String sourcePath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(sourcePath);
        launcher.getEnvironment().setComplianceLevel(18);
        launcher.buildModel();
        model = launcher.getModel();
        closures.computeClosures();
        closures.determineOverrides();
        closures.transitivelyClose();
        typechecker.performTypechecking();
    }

    public boolean isIntrasourceCall(Call c) {
        return callIndexer.lookupAux(c)
                .flatMap(CtVirtualCall::getProcedureSourcePos)
                .isPresent();
    }

    public Optional<Procedure> procedureOfCall(Call c) {
        return callIndexer.lookupAux(c)
                .flatMap(CtVirtualCall::getProcedureSourcePos)
                .flatMap(procedureIndexer::lookup);
    }

    public Collection<Procedure> procedures() {
        return procedureIndexer.outputs();
    }

    public CtProcedure lookupProcedure(Procedure p) {
        return procedureIndexer.lookupAux(p).orElseThrow(() ->
                new IllegalArgumentException("Expected procedure to be present: " + p));
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
}
