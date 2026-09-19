package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.kogu.queryengine.columnar.Vector.*;

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

    record LongVector(long[] values, int offset, int length,
                      BitSet nullIndices) implements LongVec {
        public LongVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public long value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record StringVector(String[] values, int offset, int length,
                        BitSet nullIndices) implements Utf8Vec {
        public StringVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public String value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record BooleanVector(boolean[] values, int offset, int length,
                         BitSet nullIndices) implements BoolVec {
        public BooleanVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public boolean value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    sealed interface LongVec extends Vector permits LongVector, ConstantLong {
        long value(int index);

        /// Gathers rows at the given indices into a new compact vector.
        /// Null validity is remapped to the output positions.
        /// Constant storage may return a constant result without expansion.
        default LongVec gather(int[] rows) {
            long[] values = new long[rows.length];
            BitSet nulls = new BitSet();
            for (int k = 0; k < rows.length; k++) {
                values[k] = value(rows[k]);
                if (isNull(rows[k])) nulls.set(k);
            }
            return new LongVector(values, 0, rows.length, nulls);
        }

        @Override
        default Type type() {return Type.Scalar.INT64;}
    }

    sealed interface BoolVec extends Vector permits BooleanVector, ConstantBool {
        boolean value(int index);

        /// Gathers rows at the given indices into a new compact vector.
        /// Null validity is remapped to the output positions.
        /// Constant storage may return a constant result without expansion.
        default BoolVec gather(int[] rows) {
            boolean[] values = new boolean[rows.length];
            BitSet nulls = new BitSet();
            for (int k = 0; k < rows.length; k++) {
                values[k] = value(rows[k]);
                if (isNull(rows[k])) nulls.set(k);
            }
            return new BooleanVector(values, 0, rows.length, nulls);
        }

        @Override
        default Type type() {return Type.Scalar.BOOLEAN;}
    }

    sealed interface Utf8Vec extends Vector permits StringVector, ConstantUtf8 {
        String value(int index);

        /// Gathers rows at the given indices into a new compact vector.
        /// Null validity is remapped to the output positions.
        /// Constant storage may return a constant result without expansion.
        default Utf8Vec gather(int[] rows) {
            String[] values = new String[rows.length];
            BitSet nulls = new BitSet();
            for (int k = 0; k < rows.length; k++) {
                values[k] = value(rows[k]);
                if (isNull(rows[k])) nulls.set(k);
            }
            return new StringVector(values, 0, rows.length, nulls);
        }

        @Override
        default Type type() {return Type.Scalar.UTF8;}
    }

    record ConstantLong(long n, int length) implements LongVec {
        public ConstantLong {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        public long value(int index) {
            Vector.validateIndex(index, length);
            return n;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public LongVec gather(int[] rows) {
            return new ConstantLong(n, rows.length);
        }
    }

    record ConstantBool(boolean value, int length) implements BoolVec {
        public ConstantBool {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        public boolean value(int index) {
            Vector.validateIndex(index, length);
            return value;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public BoolVec gather(int[] rows) {
            return new ConstantBool(value, rows.length);
        }
    }

    record ConstantUtf8(String s, int length) implements Utf8Vec {
        public ConstantUtf8 {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        public String value(int index) {
            Vector.validateIndex(index, length);
            return s;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public Utf8Vec gather(int[] rows) {
            return new ConstantUtf8(s, rows.length);
        }
    }
}
