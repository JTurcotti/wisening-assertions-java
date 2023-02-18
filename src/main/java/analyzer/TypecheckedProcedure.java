package analyzer;

import core.codemodel.elements.CallArgPair;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Ret;
import core.codemodel.events.Assertion;
import core.codemodel.types.Blame;

import java.util.HashMap;
import java.util.Map;

public class TypecheckedProcedure {

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
