package supervisor;

import core.codestructure.events.Assertion;
import core.codestructure.events.Pi;

public interface ExecutionSupervisor {
    boolean executeAssertion(Assertion assertion);

    void notifyAssertionPass(Assertion assertion);

    void notifyBranchTaken(Pi branch, boolean direction);
}