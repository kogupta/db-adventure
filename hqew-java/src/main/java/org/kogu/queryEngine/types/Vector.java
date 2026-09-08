package org.kogu.queryEngine.types;

import java.util.BitSet;
import java.util.Objects;

import static org.kogu.queryEngine.types.Vector.*;

public sealed interface Vector permits
        BoolVec, DoubleVec, IntVec, LongVec, Utf8Vec {
    int offset();

    int length();

    Type.Scalar type();

    boolean isNull(int index);

    default boolean isNotNull(int index) {return !isNull(index);}

    /// -----------------------------------------------------------
    // level 2: element type. This is what kernels take.
    sealed interface DoubleVec extends Vector permits DoubleVector, ConstantDouble {
        double value(int index);

        @Override
        default Type.Scalar type() {return Type.Scalar.FLOAT64;}
    }

    sealed interface IntVec extends Vector permits IntVector, ConstantInt {
        int value(int index);

        @Override
        default Type.Scalar type() {return Type.Scalar.INT32;}
    }

    sealed interface LongVec extends Vector permits LongVector, ConstantLong {
        long value(int index);

        @Override
        default Type.Scalar type() {return Type.Scalar.INT64;}
    }

    sealed interface BoolVec extends Vector permits BooleanVector, ConstantBool {
        boolean value(int index);

        @Override
        default Type.Scalar type() {return Type.Scalar.BOOLEAN;}
    }

    sealed interface Utf8Vec extends Vector permits StringVector, ConstantUtf8 {
        String value(int index);

        @Override
        default Type.Scalar type() {return Type.Scalar.UTF8;}
    }

    /// -----------------------------------------------------------
    // level 3: storage. Your existing record, two words changed.
    record DoubleVector(double[] values, int offset, int length,
                        BitSet nullIndices) implements DoubleVec {
        public DoubleVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            positiveLength(length);
            validateWindow(offset, length, values.length);
        }

        @Override
        public double value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return nullIndices.get(offset + index);
        }
    }

    record ConstantDouble(double n, int length) implements DoubleVec {
        public ConstantDouble {
            positiveLength(length);
        }

        @Override
        public double value(int index) {
            validateIndex(index, length);
            return n;
        }

        @Override
        public int offset() {return 0;}

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return false;
        }
    }

    record IntVector(int[] values, int offset, int length,
                     BitSet nullIndices) implements IntVec {
        public IntVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            positiveLength(length);
            validateWindow(offset, length, values.length);
        }

        public int value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record LongVector(long[] values, int offset, int length,
                      BitSet nullIndices) implements LongVec {
        public LongVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            positiveLength(length);
            validateWindow(offset, length, values.length);
        }

        public long value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record StringVector(String[] values, int offset, int length,
                        BitSet nullIndices) implements Utf8Vec {
        public StringVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            positiveLength(length);
            validateWindow(offset, length, values.length);
        }

        public String value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record BooleanVector(boolean[] values, int offset, int length,
                         BitSet nullIndices) implements BoolVec {
        public BooleanVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            positiveLength(length);
            validateWindow(offset, length, values.length);
        }

        public BooleanVector(boolean[] values, BitSet nullIndices) {
            this(values, 0, values.length, nullIndices);
        }

        public boolean value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record ConstantInt(int n, int length) implements IntVec {
        public ConstantInt {
            positiveLength(length);
        }

        @Override
        public int offset() {return 0;}

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length());
            return false;
        }

        public int value(int index) {
            validateIndex(index, length);
            return n;
        }
    }

    record ConstantLong(long n, int length) implements LongVec {
        public ConstantLong {
            positiveLength(length);
        }

        @Override
        public int offset() {return 0;}

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return false;
        }

        public long value(int index) {
            validateIndex(index, length);
            return n;
        }
    }

    record ConstantBool(boolean value, int length) implements BoolVec {
        public ConstantBool {
            positiveLength(length);
        }

        @Override
        public int offset() {return 0;}

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return false;
        }

        public boolean value(int index) {
            validateIndex(index, length);
            return value;
        }
    }

    record ConstantUtf8(String s, int length) implements Utf8Vec {
        public ConstantUtf8 {
            positiveLength(length);
        }

        @Override
        public int offset() {return 0;}

        @Override
        public boolean isNull(int index) {
            validateIndex(index, length);
            return false;
        }

        public String value(int index) {
            validateIndex(index, length);
            return s;
        }
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
