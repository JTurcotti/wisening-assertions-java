package supervisor;

import core.events.*;

public interface ExecutionSupervisor {
    boolean executeAssertion(Assertion assertion);

    void notifyAssertionPass(Assertion assertion);

    void notifyBranchTaken(Pi branch, boolean direction);
}