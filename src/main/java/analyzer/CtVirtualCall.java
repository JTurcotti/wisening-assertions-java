package analyzer;

import core.codemodel.SourcePos;
import core.codemodel.elements.Field;
import core.codemodel.elements.PhiInput;
import core.codemodel.elements.Procedure;
import core.codemodel.elements.Self;
import core.codemodel.types.Blame;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import spoon.support.sniper.internal.SourceFragment;

import java.util.Map;
import java.util.Optional;

public class CtVirtualCall implements SourcePositionHolder {
    public final CtElement underlying;
    public final boolean isVirtual;

    public Optional<Map<PhiInput, Blame>> inputBlames = Optional.empty();

    public final Procedure containingProcedure;

    @Override
    public SourcePosition getPosition() {
        return underlying.getPosition();
    }

    @Override
    public SourceFragment getOriginalSourceFragment() {
        return underlying.getOriginalSourceFragment();
    }

    /*
    return the source position of this call
     */
    public SourcePos getSourcePos() {
        return SourcePos.fromSpoon(underlying.getPosition());
    }

    /*
    return the source position of the procedure called by this call
     */
    public Optional<SourcePos> getProcedureSourcePos() {
        switch (underlying) {
            case CtLoop ignored -> {
                return Optional.of(getSourcePos());
            }
            case CtAbstractInvocation<?> i -> {
                SourcePosition sp = i.getExecutable().getExecutableDeclaration().getPosition();
                if (sp.isValidPosition()) {
                    return Optional.of(SourcePos.fromSpoon(sp));
                }
                return Optional.empty();
            }
            default -> {
                //continue statements and the implicit recursive call at the end of a loop yield
                //values of underlying that correspond to elements of the loop. Lookup the original loop here
                return Optional.of(SourcePos.fromSpoon(underlying.getParent(CtLoop.class).getPosition()));
            }
        }
    }

    //can create a CtVirtual call from a containing `Procedure` or `CtProcedure` reference
    private CtVirtualCall(CtElement underlying, boolean isVirtual, Procedure containingProcedure) {
        this.underlying = underlying;
        this.isVirtual = isVirtual;
        this.containingProcedure = containingProcedure;
    }

    private CtVirtualCall(CtElement underlying, boolean isVirtual, CtProcedure containingProcedure) {
        this.underlying = underlying;
        this.isVirtual = isVirtual;
        this.containingProcedure =
                containingProcedure.parentAnalyzer.procedureIndexer.lookupExisting(containingProcedure);
    }

    public CtVirtualCall(CtLoop loop, Procedure containingProcedure) {
        this(loop, true, containingProcedure);
    }

    public CtVirtualCall(CtLoop loop, CtProcedure containingProcedure) {
        this(loop, true, containingProcedure);
    }

    public CtVirtualCall(CtLoop loop, CtStatement body, Procedure containingProcedure) {
        this(body, true, containingProcedure);
    }

    public CtVirtualCall(CtLoop loop, CtStatement body, CtProcedure containingProcedure) {
        this(body, true, containingProcedure);
    }

    public CtVirtualCall(CtContinue cont, Procedure containingProcedure) {
        this(cont, true, containingProcedure);
    }

    public CtVirtualCall(CtContinue cont, CtProcedure containingProcedure) {
        this(cont, true, containingProcedure);
    }

    public CtVirtualCall(CtAbstractInvocation<?> call, Procedure containingProcedure) {
        this(call, false, containingProcedure);
    }

    public CtVirtualCall(CtAbstractInvocation<?> call, CtProcedure containingProcedure) {
        this(call, false, containingProcedure);
    }

    public void setInputBlames(Map<PhiInput, Blame> inputBlames) {
        this.inputBlames.ifPresent(ignored -> {throw new IllegalStateException("Already set");});

        this.inputBlames = Optional.of(inputBlames);
    }

    public Procedure procedureCalled(ProgramAnalyzer analyzer) {
        return analyzer.procedureCalledBy(analyzer.callIndexer.lookupExisting(this)).orElseThrow(() ->
                new IllegalStateException("Expected procedure called to be indexed")
        );
    }

    public Blame getInputBlame(PhiInput in, ProgramAnalyzer analyzer) {
        if (inputBlames.isEmpty()) {
            throw new IllegalStateException("Input blames should be set at this point");
        }
        PhiInput balancedIn = PhiInput.balanceArgsAndVars(in, procedureCalled(analyzer), analyzer);
        if (!inputBlames.get().containsKey(balancedIn)) {
            //handle some weird cases where to obvious lookup failed but the input
            //could be present in the blames under a different guise
            if (balancedIn instanceof Field) {
                //TODO: confirm the defaulting to zero is needed
                return inputBlames.get().getOrDefault(new Self(), Blame.zero());
            }
            throw new IllegalStateException("Expected input to be present in input blames: " + in);
        }
        return inputBlames.get().get(balancedIn);
    }

    public boolean inputBlamesSet() {
        return inputBlames.isPresent();
    }
}
