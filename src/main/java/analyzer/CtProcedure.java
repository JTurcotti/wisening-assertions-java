package analyzer;

import core.codemodel.SourcePos;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.sniper.internal.SourceFragment;

public class CtProcedure implements SourcePositionHolder {
    public final CtType<?> declaringType;
    @Nullable
    public final CtBlock<?> body;
    public final CtElement underlying;

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

    public boolean isInterfaceMethod() {
        return declaringType.isInterface();
    }

    public boolean isAbstract() {
        return (underlying instanceof CtMethod<?> m && m.isAbstract());
    }

    private CtProcedure(CtBodyHolder e) {
        this.declaringType = e.getParent(new TypeFilter<>(CtType.class));
        this.underlying = e;
        if (e.getBody() instanceof CtBlock<?> block) {
            this.body = block;
        } else {
            if (isInterfaceMethod() || isAbstract()) {
                this.body = null;
            } else {
                throw new IllegalArgumentException("Passed object " + e + " did not contain a CtBlock body");
            }
        }
    }

    public CtProcedure(CtMethod<?> method) {
        this((CtBodyHolder) method);
    }

    public CtProcedure(CtFor forLoop) {
        this((CtBodyHolder) forLoop);

    }

    public CtProcedure(CtForEach forEachLoop) {
        this((CtBodyHolder) forEachLoop);

    }

    public CtProcedure(CtWhile whileLoop) {
        this((CtBodyHolder) whileLoop);

    }
}


