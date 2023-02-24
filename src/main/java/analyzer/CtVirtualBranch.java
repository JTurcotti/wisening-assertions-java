package analyzer;

import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import spoon.support.sniper.internal.SourceFragment;

import java.util.Optional;

public class CtVirtualBranch implements SourcePositionHolder {

    public final CtElement underlying;

    public CtVirtualBranch(CtIf i) {
        this.underlying = i;
    }

    public CtVirtualBranch(CtCase<?> s) {
        this.underlying = s;
    }

    public CtVirtualBranch(CtConditional<?> cond) {
        this.underlying = cond;
    }

    public CtVirtualBranch(CtTry t) {
        this.underlying = t;
    }

    public CtVirtualBranch(CtCatch t) {
        this.underlying = t;
    }

    @Override
    public SourcePosition getPosition() {
        return underlying.getPosition();
    }

    @Override
    public SourceFragment getOriginalSourceFragment() {
        return underlying.getOriginalSourceFragment();
    }
}
