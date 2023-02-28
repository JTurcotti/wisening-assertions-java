package analyzer;

import core.codemodel.elements.Procedure;
import core.codemodel.types.Blame;
import spoon.reflect.code.CtAssert;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.support.sniper.internal.SourceFragment;

public record CtWiseningAssert(CtAssert<?> underlying, Blame blame, Procedure procedure)
        implements SourcePositionHolder {
    @Override
    public SourcePosition getPosition() {
        return underlying.getPosition();
    }

    @Override
    public SourceFragment getOriginalSourceFragment() {
        return underlying.getOriginalSourceFragment();
    }
}
