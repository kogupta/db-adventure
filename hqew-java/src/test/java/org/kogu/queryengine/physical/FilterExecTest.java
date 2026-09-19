package org.kogu.queryengine.physical;

import org.junit.jupiter.api.Test;
import org.kogu.queryengine.columnar.*;
import org.kogu.queryengine.expression.ColumnRef;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterExecTest {
    private static final Schema INPUT_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false),
            new Field("keep", Type.Scalar.BOOLEAN, true),
            new Field("fare", Type.Scalar.FLOAT64, true)
    ));

    private static FilterExec filter(List<RecordBatch> batches) {
        return FilterExec.create(
                new MemoryScanExec(INPUT_SCHEMA, batches),
                new ColumnRef("keep"));
    }

    private static RecordBatch batch(
            int[] ids,
            boolean[] keep,
            BitSet keepNulls,
            double[] fares,
            BitSet fareNulls) {
        return new RecordBatch(INPUT_SCHEMA, new Vector[]{
                Vectors.intVector(ids),
                Vectors.booleanVector(keep, keepNulls),
                Vectors.doubleVector(fares, fareNulls)
        });
    }

    @Test
    void keepsTrueRowsAndPreservesSchemaAndOrder() {
        RecordBatch input = batch(
                new int[]{10, 20, 30},
                new boolean[]{true, false, true},
                new BitSet(),
                new double[]{1.0, 2.0, 3.0},
                new BitSet());
        FilterExec exec = filter(List.of(input));

        var output = exec.next();
        assertNotNull(output);
        assertSame(INPUT_SCHEMA, output.schema());
        assertEquals(2, output.rowCount());
        assertEquals(10, ((IntVec) output.vectorOf("id")).value(0));
        assertEquals(30, ((IntVec) output.vectorOf("id")).value(1));
        assertNull(exec.next());
    }

    @Test
    void treatsNullPredicateAsFalseAndContinuesAfterEmptyBatch() {
        BitSet firstNull = new BitSet();
        firstNull.set(1);
        RecordBatch first = batch(
                new int[]{1, 2},
                new boolean[]{false, true},
                firstNull,
                new double[]{10.0, 20.0},
                new BitSet());
        RecordBatch second = batch(
                new int[]{3, 4},
                new boolean[]{false, true},
                new BitSet(),
                new double[]{30.0, 40.0},
                new BitSet());
        FilterExec exec = filter(List.of(first, second));

        var output = exec.next();
        assertNotNull(output);
        assertEquals(1, output.rowCount());
        assertEquals(4, ((IntVec) output.vectorOf("id")).value(0));
        assertNull(exec.next());
    }

    @Test
    void remapsNullsInPayloadVectors() {
        BitSet fareNulls = new BitSet();
        fareNulls.set(2);
        RecordBatch input = batch(
                new int[]{10, 20, 30},
                new boolean[]{true, false, true},
                new BitSet(),
                new double[]{1.0, 999.0, 999.0},
                fareNulls);
        FilterExec exec = filter(List.of(input));

        var output = exec.next();
        assertNotNull(output);
        var fares = (DoubleVec) output.vectorOf("fare");
        assertEquals(2, fares.length());
        assertFalse(fares.isNull(0));
        assertEquals(1.0, fares.value(0));
        assertTrue(fares.isNull(1));
    }

    @Test
    void rejectsNonBooleanPredicateBeforeExecution() {
        MemoryScanExec child = new MemoryScanExec(INPUT_SCHEMA, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> FilterExec.create(child, new ColumnRef("id")));
    }

    @Test
    void gathersConstantPayloadWithoutExpandingIt() {
        Schema schema = Schema.from(List.of(
                new Field("id", Type.Scalar.INT32, false),
                new Field("keep", Type.Scalar.BOOLEAN, false)));
        RecordBatch input = new RecordBatch(schema, new Vector[]{
                new IntVec.ConstantInt(7, 3),
                Vectors.booleanVector(new boolean[]{true, false, true})
        });
        FilterExec exec = FilterExec.create(
                new MemoryScanExec(schema, List.of(input)),
                new ColumnRef("keep"));

        var output = exec.next();
        assertNotNull(output);
        assertEquals(new IntVec.ConstantInt(7, 2), output.vectorOf("id"));
    }
}
