package supervisor;

import core.codemodel.events.Assertion;
import core.codemodel.events.Pi;

public interface ExecutionSupervisor {
    boolean executeAssertion(Assertion assertion);

    void notifyAssertionPass(Assertion assertion);

    void notifyBranchTaken(Pi branch, boolean direction);
}