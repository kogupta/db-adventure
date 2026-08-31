package org.kogu.queryEngine.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectorTest {

    @Test
    void intVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.IntVector vector = new Vector.IntVector(new int[]{10, 20, 30});

        assertEquals(Type.Scalar.INT32, vector.type());
        assertEquals(10, vector.value(0));
        assertEquals(30, vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.IntVector(new int[]{1}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.IntVector(new int[]{1}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void longVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.LongVector vector = new Vector.LongVector(new long[]{10L, 20L, 30L});

        assertEquals(Type.Scalar.INT64, vector.type());
        assertEquals(10L, vector.value(0));
        assertEquals(30L, vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.LongVector(new long[]{1L}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.LongVector(new long[]{1L}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void doubleVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.DoubleVector vector = new Vector.DoubleVector(new double[]{1.5, 2.5, 3.5});

        assertEquals(Type.Scalar.FLOAT64, vector.type());
        assertEquals(1.5, vector.value(0));
        assertEquals(3.5, vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.DoubleVector(new double[]{1.0}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.DoubleVector(new double[]{1.0}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void stringVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.StringVector vector = new Vector.StringVector(new String[]{"a", "b", "c"});

        assertEquals(Type.Scalar.UTF8, vector.type());
        assertEquals("a", vector.value(0));
        assertEquals("c", vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.StringVector(new String[]{"a"}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.StringVector(new String[]{"a"}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void booleanVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.BooleanVector vector = new Vector.BooleanVector(new boolean[]{true, false, true});

        assertEquals(Type.Scalar.BOOLEAN, vector.type());
        assertTrue(vector.value(0));
        assertTrue(vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.BooleanVector(new boolean[]{true}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> new Vector.BooleanVector(new boolean[]{true}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }
}
