package analyzer;

import core.codemodel.SourcePos;
import core.codemodel.elements.*;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLoop;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.sniper.internal.SourceFragment;

import java.util.List;
import java.util.Optional;

public class CtProcedure implements SourcePositionHolder {
    public final CtType<?> declaringType;
    @Nullable
    public final CtBlock<?> body;
    public final CtElement underlying;

    final ProgramAnalyzer parentAnalyzer;

    public final List<CtParameter<?>> parameters;

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

    public boolean isMethod() {
        return (underlying instanceof CtMethod<?>);
    }

    public boolean isConstructor() {
        return (underlying instanceof CtConstructor<?>);
    }

    public boolean isLoop() {
        return (underlying instanceof CtLoop);
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

    private CtProcedure(CtBodyHolder e, List<CtParameter<?>> parameters, ProgramAnalyzer parentAnalyzer) {
        this.declaringType = e.getParent(new TypeFilter<>(CtType.class));
        this.underlying = e;
        this.parentAnalyzer = parentAnalyzer;
        this.parameters = parameters;
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
        this(method, method.getParameters(), parentAnalyzer);
    }

    public CtProcedure(CtConstructor<?> constr, ProgramAnalyzer parentAnalyzer) {
        this(constr, constr.getParameters(), parentAnalyzer);
    }

    public CtProcedure(CtLoop loop, ProgramAnalyzer parentAnalyzer) {
        this(loop, List.of(), parentAnalyzer);
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

    public List<Variable> getParamVariables() {
        //we use lookupOrCreate because this is called directly by ComputeClosureForProcedure - at which
        //point auxiliary objects are not yet created
        return parameters.stream().map(parentAnalyzer.varIndexer::lookupOrCreate).toList();
    }

    public int getNumParams() {
        return parameters.size();
    }

    //ensure variables corresponding to arguments are marked thus
    public PhiInput closedOverAsInput(ClosedOver c) {
        return switch (c) {
            case Field f -> f;
            case Self s -> s;
            case Variable v -> {
                for (int i = 0; i < parameters.size(); i++) {
                    if (parentAnalyzer.varIndexer.lookupExisting(parameters.get(i)).equals(v)) {
                        yield new Arg(parentAnalyzer.procedureIndexer.lookupExisting(this), i);
                    }
                }
                yield v;
            }
        };
    }

    public CtType<?> getDeclaringType() {
        return declaringType;
    }
}


