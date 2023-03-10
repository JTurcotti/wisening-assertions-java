package supervisor;

import core.codemodel.events.Assertion;
import core.codemodel.events.Pi;

/*
ExecutionSupervisor provides the interface to ComputationNetworks that will be called out to from within the
text of programs
 */
public interface ExecutionSupervisor {
    boolean executeAssertion(Assertion assertion);

    void notifyAssertionPass(Assertion assertion);

    void notifyBranchTaken(Pi branch, boolean direction);
}