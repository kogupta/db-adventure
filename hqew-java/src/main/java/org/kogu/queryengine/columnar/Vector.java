package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

public sealed interface Vector permits
        BoolVec, DoubleVec, IntVec, LongVec, Utf8Vec {
    int length();

    Type type();

    boolean isNull(int index);

    default boolean isNotNull(int index) {return !isNull(index);}

    /// Gathers rows at the given indices into a new compact vector of the
    /// same element type. Constant storage expands through value()/isNull();
    /// null validity is remapped to the output positions.
    static Vector gather(Vector in, int[] rows) {
        return switch (in) {
            case IntVec v -> v.gather(rows);
            case LongVec v -> v.gather(rows);
            case DoubleVec v -> v.gather(rows);
            case BoolVec v -> v.gather(rows);
            case Utf8Vec v -> v.gather(rows);
        };
    }

    static void validateWindow(int offset, int length, int arrayLength) {
        if (offset < 0 || length < 0 || offset > arrayLength || length > arrayLength - offset) {
            throw new IndexOutOfBoundsException();
        }
    }

    static void validateIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException();
        }
    }

    static void positiveLength(int n) {
        if (n < 0) throw new IllegalArgumentException("Length must be >= 0");
    }
}
