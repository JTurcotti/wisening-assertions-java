package analyzer;

import core.codemodel.Indexer;
import core.codemodel.SourcePos;
import core.codemodel.elements.Call;
import core.codemodel.elements.Field;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Variable;
import core.codemodel.events.Line;
import core.codemodel.events.Pi;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.visitor.filter.TypeFilter;
import util.Unit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProgramAnalyzer {
    final CtModel model;
    final Indexer.BySourcePos<Procedure, CtProcedure> procedureIndexer = new Indexer.BySourcePos<>(Procedure::new);
    final Indexer<Line, SourcePos, Unit> lineIndexer = new Indexer<>(Line::new);
    final Indexer.BySourcePos<Call, CtVirtualCall> callIndexer = new Indexer.BySourcePos<>(Call::new);
    final Indexer<Pi, SourcePos, Unit> piIndexer = new Indexer<>(Pi::new);
    final Indexer.BySourcePos<Field, CtField<?>> fieldIndexer = new Indexer.BySourcePos<>(Field::new);
    final Indexer.BySourcePos<Variable, CtVariable<?>> varIndexer = new Indexer.BySourcePos<>(Variable::new);

    final Map<Procedure, TypecheckedProcedure> typedProcedures = new HashMap<>();
    final ClosureMap closures = new ClosureMap(this);

    public ProgramAnalyzer(String sourcePath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(sourcePath);
        launcher.getEnvironment().setComplianceLevel(18);
        launcher.buildModel();
        model = launcher.getModel();
        model.getElements(new TypeFilter<>(CtMethod.class)).forEach(closures::computeClosureForMethod);
        int rounds = closures.transitivelyClose();
    }

    private void analyzeMethod(CtMethod<?> method) {
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
}
