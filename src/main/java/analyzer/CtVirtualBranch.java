package analyzer;

import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtTry;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import spoon.support.sniper.internal.SourceFragment;

import java.util.Optional;

public class CtVirtualBranch implements SourcePositionHolder {

    public final CtElement underlying;
    public final Optional<Integer> switchCase;

    public CtVirtualBranch(CtIf i) {
        this.underlying = i;
        switchCase = Optional.empty();
    }

    public CtVirtualBranch(CtSwitch<?> s, int whichCase) {
        this.underlying = s;
        switchCase = Optional.of(whichCase);
    }

    public CtVirtualBranch(CtConditional<?> cond) {
        this.underlying = cond;
        switchCase = Optional.empty();
    }

    public CtVirtualBranch(CtTry t, int whichCatch) {
        this.underlying = t;
        switchCase = Optional.of(whichCatch);
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
