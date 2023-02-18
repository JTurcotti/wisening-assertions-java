package core.codemodel;

import org.apache.commons.lang3.ArrayUtils;
import spoon.reflect.cu.SourcePosition;

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
}
