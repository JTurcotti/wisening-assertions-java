package serializable;

import core.codemodel.SourcePos;
import core.codemodel.elements.Call;
import core.codemodel.elements.Field;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Variable;
import core.codemodel.events.Assertion;
import core.codemodel.events.Line;
import core.codemodel.events.Pi;

import java.util.HashMap;

public record SerialLabels(
        HashMap<SourcePos, Procedure> procedureLabels,
        int nextProcedure,
        HashMap<SourcePos, Line> lineLabels,
        int nextLine,
        HashMap<SourcePos, Call> callLabels,
        int nextCall,
        HashMap<SourcePos, Pi> branchLabels,
        int nextBranch,
        HashMap<SourcePos, Field> fieldLabels,
        int nextField,
        HashMap<SourcePos, Variable> varLabels,
        int nextVar,
        HashMap<SourcePos, Assertion> assertionLabels,
        int nextAssertion) {
}
