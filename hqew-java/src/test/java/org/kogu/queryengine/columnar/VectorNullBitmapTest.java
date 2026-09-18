package org.kogu.queryengine.columnar;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

class VectorNullBitmapTest {

    @Test
    void readsOnlyNonNullValuesForEveryVectorType() {
        BitSet nullIndices = new BitSet();
        nullIndices.set(0);
        nullIndices.set(2);

        Vector.IntVector ints = new Vector.IntVector(new int[]{10, 20, 30}, 0, 3, nullIndices);
        Vector.LongVector longs = new Vector.LongVector(new long[]{10L, 20L, 30L}, 0, 3, nullIndices);
        Vector.DoubleVector doubles = new Vector.DoubleVector(new double[]{1.0, 2.0, 3.0}, 0, 3, nullIndices);
        Vector.StringVector strings = new Vector.StringVector(new String[]{"a", "b", "c"}, 0, 3, nullIndices);
        Vector.BooleanVector booleans = new Vector.BooleanVector(new boolean[]{true, false, true}, 0, 3, nullIndices);

        assertAll(
                () -> assertEquals(List.of(20), readNonNull(ints, ints::value)),
                () -> assertEquals(List.of(20L), readNonNull(longs, longs::value)),
                () -> assertEquals(List.of(2.0), readNonNull(doubles, doubles::value)),
                () -> assertEquals(List.of("b"), readNonNull(strings, strings::value)),
                () -> assertEquals(List.of(false), readNonNull(booleans, booleans::value)));
    }

    @Test
    void reportsNullsAtFirstMiddleAndLastPositions() {
        BitSet nullIndices = new BitSet();
        nullIndices.set(0);
        nullIndices.set(1);
        nullIndices.set(2);
        Vector.IntVector vector = new Vector.IntVector(new int[]{10, 20, 30}, 0, 3, nullIndices);

        assertTrue(vector.isNull(0));
        assertTrue(vector.isNull(1));
        assertTrue(vector.isNull(2));
        assertEquals(List.of(), readNonNull(vector, vector::value));
    }

    @Test
    void usesBackingArrayIndexesForNullsInsideAWindow() {
        BitSet nullIndices = new BitSet();
        nullIndices.set(3);
        Vector.IntVector vector = new Vector.IntVector(
                new int[]{9, 10, 20, 30, 99}, 1, 3, nullIndices);

        assertFalse(vector.isNull(0));
        assertFalse(vector.isNull(1));
        assertTrue(vector.isNull(2));
        assertEquals(List.of(10, 20), readNonNull(vector, vector::value));
    }

    @Test
    void rejectsNegativeAndWindowEdgeNullChecks() {
        Vector.IntVector vector = Vectors.intVector(new int[]{10, 20, 30});

        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.isNull(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.isNull(3)));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullBitmapAtConstruction() {
        assertThrows(
                NullPointerException.class,
                () -> new Vector.IntVector(new int[]{10}, 0, 1, null));
    }


    @Test
    void nullSlotValuesMayDifferButGatedReadsRemainIdentical() {
        BitSet nullIndices = new BitSet();
        nullIndices.set(1);
        Vector.IntVector first = new Vector.IntVector(
                new int[]{10, -999, 30}, 0, 3, nullIndices);
        Vector.IntVector second = new Vector.IntVector(
                new int[]{10, 12345, 30}, 0, 3, (BitSet) nullIndices.clone());

        assertTrue(first.isNull(1));
        assertTrue(second.isNull(1));
        assertNotEquals(first.value(1), second.value(1));
        assertEquals(readNonNull(first, first::value), readNonNull(second, second::value));
        assertEquals(List.of(10, 30), readNonNull(first, first::value));
    }
    private static <T> List<T> readNonNull(Vector vector, IntFunction<T> valueReader) {
        var values = new java.util.ArrayList<T>();
        for (int index = 0; index < vector.length(); index++) {
            if (!vector.isNull(index)) {
                values.add(valueReader.apply(index));
            }
        }
        return List.copyOf(values);
    }
}
