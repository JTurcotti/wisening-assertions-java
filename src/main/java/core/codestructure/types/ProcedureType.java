package core.codestructure.types;

import core.codestructure.elements.CallArgPair;
import core.codestructure.elements.Procedure;
import core.codestructure.elements.Ret;
import core.codestructure.events.Assertion;

import java.util.HashMap;
import java.util.Map;

public class ProcedureType {

    public int numArgs;

    /**
     * Not always a literal name: for example function bodies
     */
    public String name;
    public Procedure procedure;

    public final Map<Assertion, Blame> assertionBlames = new HashMap<>();
    public final Map<CallArgPair, Blame> callArgBlames = new HashMap<>();
    public final Map<Ret, Blame> retBlames = new HashMap<>();
}
