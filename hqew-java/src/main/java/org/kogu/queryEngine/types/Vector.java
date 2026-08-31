package org.kogu.queryEngine.types;

import java.util.BitSet;
import java.util.Objects;

public sealed interface Vector {
    int offset();
    int length();
    BitSet nullIndices();
    Type.Scalar type();
    default boolean isNull(int index) {
        validateIndex(index, length());
        return nullIndices().get(offset() + index);
    }

    record IntVector(int[] values, int offset, int length,
                     BitSet nullIndices) implements Vector {
        public IntVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            validateWindow(offset, length, values.length);
        }


        public int value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.INT32;}
    }

    record LongVector(long[] values, int offset, int length,
                      BitSet nullIndices) implements Vector {
        public LongVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            validateWindow(offset, length, values.length);
        }

        public long value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.INT64;}
    }


    record DoubleVector(double[] values, int offset, int length,
                        BitSet nullIndices) implements Vector {
        public DoubleVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            validateWindow(offset, length, values.length);
        }


        public double value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.FLOAT64;}
    }


    record StringVector(String[] values, int offset, int length,
                        BitSet nullIndices) implements Vector {
        public StringVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            validateWindow(offset, length, values.length);
        }


        public String value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.UTF8;}
    }


    record BooleanVector(boolean[] values, int offset, int length,
                         BitSet nullIndices) implements Vector {
        public BooleanVector {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(nullIndices, "nullIndices");
            validateWindow(offset, length, values.length);
        }


        public boolean value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.BOOLEAN;}
    }

    private static void validateWindow(int offset, int length, int arrayLength) {
        if (offset < 0 || length < 0 || offset > arrayLength || length > arrayLength - offset) {
            throw new IndexOutOfBoundsException();
        }
    }

    private static void validateIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException();
        }
    }
}
