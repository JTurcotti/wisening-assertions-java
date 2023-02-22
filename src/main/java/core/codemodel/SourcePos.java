package core.codemodel;

import org.apache.commons.lang3.ArrayUtils;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

import java.util.Arrays;

public record SourcePos(String file, int[] position) {
    public static SourcePos fromSpoon(SourcePosition pos) {
        if (!pos.isValidPosition()) {
            throw new IllegalArgumentException("NoSourcePosition passed where not expected");
        }
        return new SourcePos(pos.getFile().getPath(), new int[] {pos.getSourceStart(), pos.getSourceEnd()});
    }

    public static SourcePos fromFieldAccess(CtFieldAccess<?> fa) {
        if (fa.getVariable().getPosition().isValidPosition()) {
            return fromSpoon(fa.getVariable().getPosition());
        }
        if (fa.getPosition().isValidPosition() && fa.getTarget().getPosition().isValidPosition()) {
            return new SourcePos(fa.getPosition().getFile().getPath(),
                    new int[] {fa.getTarget().getPosition().getSourceEnd() + 2, fa.getPosition().getSourceEnd()});
        }
        if (fa.getPosition().isValidPosition()) {
            return new SourcePos(fa.getPosition().getFile().getPath(),
                    new int[] {fa.getPosition().getSourceStart(), fa.getPosition().getSourceEnd()});
        }
        throw new IllegalStateException("Cannot determine source position for field access: " + fa);
    }

    public static SourcePos fromUnop(CtUnaryOperator<?> unop) {
        int start, end;
        switch (unop.getKind()) {
            case COMPL, NEG, NOT, POS -> {
                start = unop.getPosition().getSourceStart();
                end = unop.getPosition().getSourceStart();
            }
            case POSTDEC, POSTINC -> {
                start = unop.getPosition().getSourceEnd() - 1;
                end = unop.getPosition().getSourceEnd();
            }
            case PREDEC, PREINC -> {
                start = unop.getPosition().getSourceStart();
                end = unop.getPosition().getSourceStart() + 1;
            }
            default -> throw new IllegalArgumentException("Unrecognized unop: " + unop);
        }
        return new SourcePos(unop.getPosition().getFile().getPath(), new int[] {start, end});
    }

    public static SourcePos fromBinop(CtBinaryOperator<?> binop) {
        return new SourcePos(binop.getPosition().getFile().getPath(),
                new int[] {
                        //warning: this could contain spaced around operand, I don't see how to avoid that
                        binop.getLeftHandOperand().getPosition().getSourceEnd() + 1,
                        binop.getRightHandOperand().getPosition().getSourceStart() - 1
                });
    }

    public SourcePos mergeWith(SourcePos other) {
        if (file.equals(other.file) && position[position.length - 1] <= other.position[0]) {
            return new SourcePos(file, ArrayUtils.addAll(position, other.position));
        }
        throw new IllegalArgumentException("Provided positions overlap or are in wrong order");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SourcePos sp = (SourcePos) obj;
        return file.equals(sp.file) && Arrays.equals(position, sp.position);
    }

    @Override
    public int hashCode() {
        return file.hashCode() + Arrays.hashCode(position);
    }

    @Override
    public String toString() {
        return "Person[file=" + file + ", position=" + Arrays.toString(position) + "]";
    }
}
