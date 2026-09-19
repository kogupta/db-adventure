package org.kogu.queryengine.columnar;

import java.util.BitSet;

public final class Vectors {
    private Vectors() {}

    public static IntVec.IntVector intVector(int[] values) {
        return new IntVec.IntVector(values, 0, values.length, new BitSet());
    }

    public static IntVec.IntVector intVector(int[] values, int offset, int length) {
        return new IntVec.IntVector(values, offset, length, new BitSet());
    }

    public static IntVec.IntVector intVector(int[] values, BitSet nullIndices) {
        return new IntVec.IntVector(values, 0, values.length, nullIndices);
    }

    public static Vector.LongVector longVector(long[] values) {
        return new Vector.LongVector(values, 0, values.length, new BitSet());
    }

    public static Vector.LongVector longVector(long[] values, int offset, int length) {
        return new Vector.LongVector(values, offset, length, new BitSet());
    }

    public static Vector.LongVector longVector(long[] values, BitSet nullIndices) {
        return new Vector.LongVector(values, 0, values.length, nullIndices);
    }

    public static DoubleVec.DoubleVector doubleVector(double[] values) {
        return new DoubleVec.DoubleVector(values, 0, values.length, new BitSet());
    }

    public static DoubleVec.DoubleVector doubleVector(double[] values, int offset, int length) {
        return new DoubleVec.DoubleVector(values, offset, length, new BitSet());
    }

    public static DoubleVec.DoubleVector doubleVector(double[] values, BitSet nullIndices) {
        return new DoubleVec.DoubleVector(values, 0, values.length, nullIndices);
    }

    public static Vector.StringVector stringVector(String[] values, BitSet nullIndices) {
        return new Vector.StringVector(values, 0, values.length, nullIndices);
    }

    public static Vector.StringVector stringVector(String[] values) {
        return new Vector.StringVector(values, 0, values.length, new BitSet());
    }

    public static Vector.StringVector stringVector(String[] values, int offset, int length) {
        return new Vector.StringVector(values, offset, length, new BitSet());
    }

    public static Vector.BooleanVector booleanVector(boolean[] values) {
        return new Vector.BooleanVector(values, 0, values.length, new BitSet());
    }

    public static Vector.BooleanVector booleanVector(boolean[] values, int offset, int length) {
        return new Vector.BooleanVector(values, offset, length, new BitSet());
    }

    public static Vector.BooleanVector booleanVector(boolean[] values, BitSet nullIndices) {
        return new Vector.BooleanVector(values, 0, values.length, nullIndices);
    }
}
