package analyzer;

import core.codemodel.Indexer;
import core.codemodel.elements.Call;
import core.codemodel.elements.Field;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Variable;
import core.codemodel.events.Pi;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtVariable;

import java.util.*;

public class ProgramAnalyzer {
    final CtModel model;
    final Indexer.BySourcePos<Procedure, CtProcedure> procedureIndexer = new Indexer.BySourcePos<>(Procedure::new);
    final Indexer.BySourceLine lineIndexer = new Indexer.BySourceLine();
    final Indexer.BySourcePos<Call, CtVirtualCall> callIndexer = new Indexer.BySourcePos<>(Call::new);
    final Indexer.BySourcePos<Pi, CtVirtualBranch> branchIndexer = new Indexer.BySourcePos<>(Pi::new);
    final Indexer.BySourcePos<Field, CtField<?>> fieldIndexer = new Indexer.BySourcePos<>(Field::new);
    final Indexer.BySourcePos<Variable, CtVariable<?>> varIndexer = new Indexer.BySourcePos<>(Variable::new);

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
}
