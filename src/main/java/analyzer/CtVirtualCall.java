package analyzer;

import core.codemodel.SourcePos;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtLoop;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import spoon.support.sniper.internal.SourceFragment;

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

    public CtVirtualCall(CtLoop loop) {
        this.underlying = loop;
        this.isVirtual = true;
    }

    public CtVirtualCall(CtAbstractInvocation<?> call) {
        this.underlying = call;
        this.isVirtual = false;
    }
}
