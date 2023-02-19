package analyzer;

import core.codemodel.SourcePos;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtLoop;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import spoon.support.sniper.internal.SourceFragment;

import java.util.Optional;

public class CtVirtualCall implements SourcePositionHolder {
    public final CtElement underlying;
    public final boolean isVirtual;

    @Override
    public SourcePosition getPosition() {
        return underlying.getPosition();
    }

    @Override
    public SourceFragment getOriginalSourceFragment() {
        return underlying.getOriginalSourceFragment();
    }

    public SourcePos getSourcePos() {
        return SourcePos.fromSpoon(underlying.getPosition());
    }

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
            default ->
                    throw new IllegalStateException("Unrecognized vitual call: " + this);
        }
    }

    public CtVirtualCall(CtLoop loop) {
        this.underlying = loop;
        this.isVirtual = true;
    }

    public CtVirtualCall(CtAbstractInvocation<?> call) {
        this.underlying = call;
        this.isVirtual = false;
    }
}
