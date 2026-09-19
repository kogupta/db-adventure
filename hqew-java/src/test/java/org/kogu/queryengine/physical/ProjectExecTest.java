package org.kogu.queryengine.physical;

import org.junit.jupiter.api.Test;
import org.kogu.queryengine.columnar.*;
import org.kogu.queryengine.expression.ArithmeticOp;
import org.kogu.queryengine.expression.BinaryExpr;
import org.kogu.queryengine.expression.ColumnRef;
import org.kogu.queryengine.expression.Literal;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectExecTest {
    private static final Schema INPUT_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false),
            new Field("fare", Type.Scalar.FLOAT64, true)
    ));

    private static RecordBatch batch(int[] ids, double[] fares) {
        return new RecordBatch(INPUT_SCHEMA, new Vector[]{
                Vectors.intVector(ids),
                Vectors.doubleVector(fares)
        });
    }

    @Test
    void derivesSchemaAndProjectsColumnsAndExpressions() {
        RecordBatch input = batch(new int[]{1, 2, 1}, new double[]{10.0, 20.0, 30.0});
        ProjectExec exec = new ProjectExec(
                new MemoryScanExec(INPUT_SCHEMA, List.of(input)),
                List.of(
                        Projection.identity("id"),
                        Projection.alias("fare", "fare_copy"),
                        new Projection(
                                "adjusted_fare",
                                new BinaryExpr(
                                        new ColumnRef("fare"),
                                        ArithmeticOp.Multiply,
                                        new Literal.Float64(1.1)))
                ));

        assertEquals(3, exec.schema().fieldCount());
        assertEquals(new Field("id", Type.Scalar.INT32, false), exec.schema().fieldAtIndex(0));
        assertEquals(new Field("fare_copy", Type.Scalar.FLOAT64, true), exec.schema().fieldAtIndex(1));
        assertEquals(new Field("adjusted_fare", Type.Scalar.FLOAT64, true), exec.schema().fieldAtIndex(2));

        var output = exec.next();
        assertNotNull(output);
        assertEquals(3, output.rowCount());
        assertSame(input.vectorOf("id"), output.vectorOf("id"));
        assertSame(input.vectorOf("fare"), output.vectorOf("fare_copy"));

        DoubleVec adjusted = (DoubleVec) output.vectorOf("adjusted_fare");
        assertEquals(11.0, adjusted.value(0));
        assertEquals(22.0, adjusted.value(1));
        assertEquals(33.0, adjusted.value(2));
        assertFalse(adjusted.isNull(0));
        assertFalse(adjusted.isNull(1));
        assertFalse(adjusted.isNull(2));
        assertNull(exec.next());
    }

    @Test
    void derivedExpressionPreservesNulls() {
        BitSet nulls = new BitSet();
        nulls.set(1);
        RecordBatch input = new RecordBatch(INPUT_SCHEMA, new Vector[]{
                Vectors.intVector(new int[]{1, 2, 3}),
                Vectors.doubleVector(new double[]{10.0, 999.0, 30.0}, nulls)
        });

        ProjectExec exec = new ProjectExec(
                new MemoryScanExec(INPUT_SCHEMA, List.of(input)),
                List.of(new Projection(
                        "adjusted_fare",
                        new BinaryExpr(
                                new ColumnRef("fare"),
                                ArithmeticOp.Multiply,
                                new Literal.Float64(1.1)))));

        var output = exec.next();
        assertNotNull(output);
        DoubleVec adjusted = (DoubleVec) output.vectorOf("adjusted_fare");
        assertEquals(11.0, adjusted.value(0));
        assertTrue(adjusted.isNull(1));
        assertEquals(33.0, adjusted.value(2));
    }

    @Test
    void preservesInputBatchBoundariesAndOrder() {
        RecordBatch first = batch(new int[]{1, 2}, new double[]{10.0, 20.0});
        RecordBatch second = batch(new int[]{3}, new double[]{30.0});
        ProjectExec exec = new ProjectExec(
                new MemoryScanExec(INPUT_SCHEMA, List.of(first, second)),
                List.of(Projection.identity("id")));

        var firstOutput = exec.next();
        var secondOutput = exec.next();
        assertNotNull(firstOutput);
        assertNotNull(secondOutput);
        assertEquals(2, firstOutput.rowCount());
        assertEquals(1, secondOutput.rowCount());
        assertEquals(1, ((IntVec) firstOutput.vectorOf("id")).value(0));
        assertEquals(3, ((IntVec) secondOutput.vectorOf("id")).value(0));
        assertNull(exec.next());
    }

    @Test
    void rejectsEmptyAndDuplicateProjections() {
        MemoryScanExec child = new MemoryScanExec(INPUT_SCHEMA, List.of());
        assertThrows(IllegalArgumentException.class, () -> new ProjectExec(child, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProjectExec(
                child,
                List.of(Projection.identity("id"), Projection.alias("fare", "id"))));
    }
}
