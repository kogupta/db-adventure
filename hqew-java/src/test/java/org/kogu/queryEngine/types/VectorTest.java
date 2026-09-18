package org.kogu.queryEngine.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectorTest {

    @Test
    void intVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.IntVector vector = Vectors.intVector(new int[]{10, 20, 30});

        assertEquals(Type.Scalar.INT32, vector.type());
        assertEquals(10, vector.value(0));
        assertEquals(30, vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.intVector(new int[]{1}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.intVector(new int[]{1}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void longVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.LongVector vector = Vectors.longVector(new long[]{10L, 20L, 30L});

        assertEquals(Type.Scalar.INT64, vector.type());
        assertEquals(10L, vector.value(0));
        assertEquals(30L, vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.longVector(new long[]{1L}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.longVector(new long[]{1L}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void doubleVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.DoubleVector vector = Vectors.doubleVector(new double[]{1.5, 2.5, 3.5});

        assertEquals(Type.Scalar.FLOAT64, vector.type());
        assertEquals(1.5, vector.value(0));
        assertEquals(3.5, vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.doubleVector(new double[]{1.0}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.doubleVector(new double[]{1.0}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void stringVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.StringVector vector = Vectors.stringVector(new String[]{"a", "b", "c"});

        assertEquals(Type.Scalar.UTF8, vector.type());
        assertEquals("a", vector.value(0));
        assertEquals("c", vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.stringVector(new String[]{"a"}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.stringVector(new String[]{"a"}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }

    @Test
    void booleanVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.BooleanVector vector = Vectors.booleanVector(new boolean[]{true, false, true});

        assertEquals(Type.Scalar.BOOLEAN, vector.type());
        assertTrue(vector.value(0));
        assertTrue(vector.value(2));
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.booleanVector(new boolean[]{true}, -1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> Vectors.booleanVector(new boolean[]{true}, 1, 1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3)));
    }
    @Test
    void intVectorReadsAWindowFromItsBackingArray() {
        Vector.IntVector vector = Vectors.intVector(new int[]{10, 20, 30, 40}, 1, 2);

        assertEquals(20, vector.value(0));
        assertEquals(30, vector.value(1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(2));
    }

    @Test
    void longVectorReadsAWindowFromItsBackingArray() {
        Vector.LongVector vector = Vectors.longVector(new long[]{10L, 20L, 30L, 40L}, 1, 2);

        assertEquals(20L, vector.value(0));
        assertEquals(30L, vector.value(1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(2));
    }

    @Test
    void doubleVectorReadsAWindowFromItsBackingArray() {
        Vector.DoubleVector vector = Vectors.doubleVector(new double[]{1.0, 2.0, 3.0, 4.0}, 1, 2);

        assertEquals(2.0, vector.value(0));
        assertEquals(3.0, vector.value(1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(2));
    }

    @Test
    void stringVectorReadsAWindowFromItsBackingArray() {
        Vector.StringVector vector = Vectors.stringVector(new String[]{"a", "b", "c", "d"}, 1, 2);

        assertEquals("b", vector.value(0));
        assertEquals("c", vector.value(1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(2));
    }

    @Test
    void booleanVectorReadsAWindowFromItsBackingArray() {
        Vector.BooleanVector vector = Vectors.booleanVector(new boolean[]{true, false, false, true}, 1, 2);

        assertFalse(vector.value(0));
        assertFalse(vector.value(1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(2));
    }
    @Test
    void intVectorReadsRelativeToWindowAndStopsAtWindowEdge() {
        Vector.IntVector vector = Vectors.intVector(new int[]{9, 10, 20, 30, 99}, 1, 3);

        assertEquals(10, vector.value(0));
        assertEquals(30, vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    @SuppressWarnings("NullAway")
    void intVectorReportsMessageWhenBackingArrayIsNull() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> Vectors.intVector(null));
        assertEquals("values", error.getMessage());
    }

    @Test
    void vectorConstructorsRejectSlicesOutsideBackingArrays() {
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class,
                        () -> Vectors.intVector(new int[]{1, 2, 3, 4}, 2, 3)),
                () -> assertThrows(IndexOutOfBoundsException.class,
                        () -> Vectors.longVector(new long[]{1L, 2L, 3L, 4L}, 2, 3)),
                () -> assertThrows(IndexOutOfBoundsException.class,
                        () -> Vectors.doubleVector(new double[]{1.0, 2.0, 3.0, 4.0}, 2, 3)),
                () -> assertThrows(IndexOutOfBoundsException.class,
                        () -> Vectors.stringVector(new String[]{"a", "b", "c", "d"}, 2, 3)),
                () -> assertThrows(IndexOutOfBoundsException.class,
                        () -> Vectors.booleanVector(new boolean[]{true, false, true, false}, 2, 3)));
    }

}
