package analyzer;

import core.codemodel.SourcePos;
import core.codemodel.elements.Field;
import core.codemodel.elements.Procedure;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.sniper.internal.SourceFragment;

import java.util.Optional;

public class CtProcedure implements SourcePositionHolder {
    public final CtType<?> declaringType;
    @Nullable
    public final CtBlock<?> body;
    public final CtElement underlying;

    private final ProgramAnalyzer parentAnalyzer;

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

    public boolean isConstructor() {
        return (underlying instanceof CtConstructor<?>);
    }

    public Optional<CtMethod<?>> asMethod() {
        if (underlying instanceof CtMethod<?> m) {
            return Optional.of(m);
        }
        return Optional.empty();
    }

    public boolean isOverriding(CtProcedure superProc) {
        if (asMethod().isEmpty() || superProc.asMethod().isEmpty()) {
            return false;
        }
        return asMethod().get().isOverriding(superProc.asMethod().get());
    }

    private CtProcedure(CtBodyHolder e, ProgramAnalyzer parentAnalyzer) {
        this.declaringType = e.getParent(new TypeFilter<>(CtType.class));
        this.underlying = e;
        this.parentAnalyzer = parentAnalyzer;
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

    public CtProcedure(CtMethod<?> method, ProgramAnalyzer parentAnalyzer) {
        this((CtBodyHolder) method, parentAnalyzer);
    }

    public CtProcedure(CtConstructor<?> constr, ProgramAnalyzer parentAnalyzer) {
        this((CtBodyHolder) constr, parentAnalyzer);
    }

    public CtProcedure(CtLoop loop, ProgramAnalyzer parentAnalyzer) {
        this((CtBodyHolder) loop, parentAnalyzer);
    }

    /*
    Can this procedure read and write this field directly?
    i.e. is it a field defined in a class overriden by the
    current class
     */
    public boolean canAccessField(Field f) {
        Optional<CtField<?>> fld = parentAnalyzer.fieldIndexer.lookupAux(f);
        if (fld.isEmpty()) {
            throw new IllegalStateException("Field lookup should not have failed");
        }
        return declaringType.isSubtypeOf(fld.get().getDeclaringType().getReference());
    }
}


