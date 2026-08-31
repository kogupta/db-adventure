package org.kogu.queryEngine.types;

import java.util.BitSet;
import java.util.Objects;

public final class Vectors {
    private Vectors() {}

    public static Vector.IntVector intVector(int[] values) {
        Objects.requireNonNull(values, "values");
        return new Vector.IntVector(values, 0, values.length, new BitSet());
    }

    public static Vector.IntVector intVector(int[] values, int offset, int length) {
        return new Vector.IntVector(values, offset, length, new BitSet());
    }

    public static Vector.IntVector intVector(int[] values, BitSet nullIndices) {
        Objects.requireNonNull(values, "values");
        return new Vector.IntVector(values, 0, values.length, nullIndices);
    }

    public static Vector.LongVector longVector(long[] values) {
        Objects.requireNonNull(values, "values");
        return new Vector.LongVector(values, 0, values.length, new BitSet());
    }

    public static Vector.LongVector longVector(long[] values, int offset, int length) {
        return new Vector.LongVector(values, offset, length, new BitSet());
    }

    public static Vector.LongVector longVector(long[] values, BitSet nullIndices) {
        Objects.requireNonNull(values, "values");
        return new Vector.LongVector(values, 0, values.length, nullIndices);
    }

    public static Vector.DoubleVector doubleVector(double[] values) {
        Objects.requireNonNull(values, "values");
        return new Vector.DoubleVector(values, 0, values.length, new BitSet());
    }

    public static Vector.DoubleVector doubleVector(double[] values, int offset, int length) {
        return new Vector.DoubleVector(values, offset, length, new BitSet());
    }

    public static Vector.DoubleVector doubleVector(double[] values, BitSet nullIndices) {
        Objects.requireNonNull(values, "values");
        return new Vector.DoubleVector(values, 0, values.length, nullIndices);
    }

    public static Vector.StringVector stringVector(String[] values) {
        Objects.requireNonNull(values, "values");
        return new Vector.StringVector(values, 0, values.length, new BitSet());
    }

    public static Vector.StringVector stringVector(String[] values, int offset, int length) {
        return new Vector.StringVector(values, offset, length, new BitSet());
    }

    public static Vector.StringVector stringVector(String[] values, BitSet nullIndices) {
        Objects.requireNonNull(values, "values");
        return new Vector.StringVector(values, 0, values.length, nullIndices);
    }

    public static Vector.BooleanVector booleanVector(boolean[] values) {
        Objects.requireNonNull(values, "values");
        return new Vector.BooleanVector(values, 0, values.length, new BitSet());
    }

    public static Vector.BooleanVector booleanVector(boolean[] values, int offset, int length) {
        return new Vector.BooleanVector(values, offset, length, new BitSet());
    }

    public static Vector.BooleanVector booleanVector(boolean[] values, BitSet nullIndices) {
        Objects.requireNonNull(values, "values");
        return new Vector.BooleanVector(values, 0, values.length, nullIndices);
    }
}
