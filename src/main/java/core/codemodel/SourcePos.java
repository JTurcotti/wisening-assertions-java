package core.codemodel;

import org.apache.commons.lang3.ArrayUtils;
import spoon.reflect.cu.SourcePosition;

import java.util.Arrays;

public record SourcePos(String file, int[] position) {
    public static SourcePos fromSpoon(SourcePosition pos) {
        return new SourcePos(pos.getFile().getPath(), new int[] {pos.getSourceStart(), pos.getSourceEnd()});
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
