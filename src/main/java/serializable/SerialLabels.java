package serializable;

import core.codemodel.SourcePos;
import core.codemodel.events.Assertion;
import core.codemodel.events.Pi;

import java.util.HashMap;

public record SerialLabels(
        HashMap<SourcePos, Assertion> assertionLabels,
        int nextAssertionNum,
        HashMap<SourcePos, Pi> branchLabels,
        int nextBranchNum) {
}
