package org.kogu.queryEngine.types;

import java.util.Objects;

public sealed interface Vector {
    Type.Scalar type();

    record IntVector(int[] values, int offset, int length) implements Vector {
        public IntVector {
            validateWindow(values, offset, length, values.length);
        }

        public IntVector(int[] values) {
            this(values, 0, values.length);
        }

        public int value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.INT32;}
    }

    record LongVector(long[] values, int offset, int length) implements Vector {
        public LongVector {
            validateWindow(values, offset, length, values.length);
        }

        public LongVector(long[] values) {
            this(values, 0, values.length);
        }

        public long value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.INT64;}
    }

    record DoubleVector(double[] values, int offset, int length) implements Vector {
        public DoubleVector {
            validateWindow(values, offset, length, values.length);
        }

        public DoubleVector(double[] values) {
            this(values, 0, values.length);
        }

        public double value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.FLOAT64;}
    }

    record StringVector(String[] values, int offset, int length) implements Vector {
        public StringVector {
            validateWindow(values, offset, length, values.length);
        }

        public StringVector(String[] values) {
            this(values, 0, values.length);
        }

        public String value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.UTF8;}
    }

    record BooleanVector(boolean[] values, int offset, int length) implements Vector {
        public BooleanVector {
            validateWindow(values, offset, length, values.length);
        }

        public BooleanVector(boolean[] values) {
            this(values, 0, values.length);
        }

        public boolean value(int index) {
            validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public Type.Scalar type() {return Type.Scalar.BOOLEAN;}
    }

    private static void validateWindow(Object values, int offset, int length, int arrLength) {
        Objects.requireNonNull(values, "values");
        if (offset < 0 || length < 0 || offset > arrLength || length > arrLength - offset) {
            throw new IndexOutOfBoundsException();
        }
    }

    private static void validateIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException();
        }
    }
}
