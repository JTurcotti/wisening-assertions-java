package analyzer;

import core.codemodel.SourcePos;
import core.codemodel.elements.PhiInput;
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

    public CtVirtualCall(CtLoop loop) {
        this.underlying = loop;
        this.isVirtual = true;
    }

    public CtVirtualCall(CtLoop loop, CtStatement body) {
        this.underlying = body;
        this.isVirtual = true;
    }

    public CtVirtualCall(CtContinue cont) {
        this.underlying = cont;
        this.isVirtual = true;
    }

    public CtVirtualCall(CtAbstractInvocation<?> call) {
        this.underlying = call;
        this.isVirtual = false;
    }

    public void setInputBlames(Map<PhiInput, Blame> inputBlames) {
        this.inputBlames.ifPresent(ignored -> {throw new IllegalStateException("Already set");});

        this.inputBlames = Optional.of(inputBlames);
    }
}
