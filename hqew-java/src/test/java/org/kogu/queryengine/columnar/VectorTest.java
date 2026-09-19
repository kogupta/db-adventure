package org.kogu.queryengine.columnar;

import org.junit.jupiter.api.Test;
import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class VectorTest {

    @Test
    void intVectorReadsAWindowFromItsBackingArray() {
        IntVec.IntVector vector = Vectors.intVector(new int[]{9, 10, 20, 30, 99}, 1, 3);

        assertEquals(3, vector.length());
        assertEquals(Type.Scalar.INT32, vector.type());
        assertEquals(10, vector.value(0));
        assertEquals(20, vector.value(1));
        assertEquals(30, vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    void intVectorReadsRelativeToWindowAndStopsAtWindowEdge() {
        IntVec.IntVector vector = Vectors.intVector(new int[]{9, 10, 20, 30, 99}, 1, 3);

        assertEquals(10, vector.value(0));
        assertEquals(30, vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    void intVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        IntVec.IntVector vector = Vectors.intVector(new int[]{1, 2, 3, 4, 5}, 0, 5);
        assertEquals(1, vector.value(0));
        assertEquals(5, vector.value(4));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(5));
    }

    @Test
    void longVectorReadsAWindowFromItsBackingArray() {
        Vector.LongVector vector = Vectors.longVector(new long[]{9L, 10L, 20L, 30L, 99L}, 1, 3);

        assertEquals(3, vector.length());
        assertEquals(Type.Scalar.INT64, vector.type());
        assertEquals(10L, vector.value(0));
        assertEquals(20L, vector.value(1));
        assertEquals(30L, vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    void longVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.LongVector vector = Vectors.longVector(new long[]{1L, 2L, 3L, 4L, 5L}, 0, 5);
        assertEquals(1L, vector.value(0));
        assertEquals(5L, vector.value(4));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(5));
    }

    @Test
    void doubleVectorReadsAWindowFromItsBackingArray() {
        DoubleVec.DoubleVector vector = Vectors.doubleVector(new double[]{9.0, 10.0, 20.0, 30.0, 99.0}, 1, 3);

        assertEquals(3, vector.length());
        assertEquals(Type.Scalar.FLOAT64, vector.type());
        assertEquals(10.0, vector.value(0));
        assertEquals(20.0, vector.value(1));
        assertEquals(30.0, vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    void doubleVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        DoubleVec.DoubleVector vector = Vectors.doubleVector(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, 0, 5);
        assertEquals(1.0, vector.value(0));
        assertEquals(5.0, vector.value(4));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(5));
    }

    @Test
    void booleanVectorReadsAWindowFromItsBackingArray() {
        Vector.BooleanVector vector = Vectors.booleanVector(new boolean[]{true, false, true, false, true}, 1, 3);

        assertEquals(3, vector.length());
        assertEquals(Type.Scalar.BOOLEAN, vector.type());
        assertFalse(vector.value(0));
        assertTrue(vector.value(1));
        assertFalse(vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    void booleanVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.BooleanVector vector = Vectors.booleanVector(new boolean[]{true, false, true, false, true}, 0, 5);
        assertTrue(vector.value(0));
        assertTrue(vector.value(4));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(5));
    }

    @Test
    void stringVectorReadsAWindowFromItsBackingArray() {
        Vector.StringVector vector = Vectors.stringVector(new String[]{"a", "b", "c", "d", "e"}, 1, 3);

        assertEquals(3, vector.length());
        assertEquals(Type.Scalar.UTF8, vector.type());
        assertEquals("b", vector.value(0));
        assertEquals("c", vector.value(1));
        assertEquals("d", vector.value(2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(3));
    }

    @Test
    void stringVectorReadsValuesAndRejectsInvalidWindowsAndIndexes() {
        Vector.StringVector vector = Vectors.stringVector(new String[]{"a", "b", "c", "d", "e"}, 0, 5);
        assertEquals("a", vector.value(0));
        assertEquals("e", vector.value(4));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.value(5));
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

    @Test
    void constantVectorsGatherWithoutExpansion() {
        assertAll(
                () -> {
                    IntVec gathered = new IntVec.ConstantInt(7, 100).gather(new int[]{0, 5, 99});
                    assertEquals(new IntVec.ConstantInt(7, 3), gathered);
                },
                () -> {
                    Vector.LongVec gathered = new Vector.ConstantLong(7L, 100).gather(new int[]{0, 5, 99});
                    assertEquals(new Vector.ConstantLong(7L, 3), gathered);
                },
                () -> {
                    DoubleVec gathered = new DoubleVec.ConstantDouble(7.0, 100).gather(new int[]{0, 5, 99});
                    assertEquals(new DoubleVec.ConstantDouble(7.0, 3), gathered);
                },
                () -> {
                    Vector.BoolVec gathered = new Vector.ConstantBool(true, 100).gather(new int[]{0, 5, 99});
                    assertEquals(new Vector.ConstantBool(true, 3), gathered);
                },
                () -> {
                    Vector.Utf8Vec gathered = new Vector.ConstantUtf8("seven", 100).gather(new int[]{0, 5, 99});
                    assertEquals(new Vector.ConstantUtf8("seven", 3), gathered);
                });
    }

    @Test
    void windowedVectorsGatherCopySelectedRowsAndRemapNulls() {
        int[] values = {10, 20, 30, 40, 50};
        BitSet nulls = new BitSet();
        nulls.set(1); // value 20 is null
        nulls.set(3); // value 40 is null
        IntVec vector = Vectors.intVector(values, nulls);

        IntVec gathered = vector.gather(new int[]{0, 1, 4});

        assertAll(
                () -> assertEquals(3, gathered.length()),
                () -> assertFalse(gathered.isNull(0)),
                () -> assertEquals(10, gathered.value(0)),
                () -> assertTrue(gathered.isNull(1)),
                () -> assertFalse(gathered.isNull(2)),
                () -> assertEquals(50, gathered.value(2)));
    }
}
