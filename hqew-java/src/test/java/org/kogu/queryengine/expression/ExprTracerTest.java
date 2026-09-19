package org.kogu.queryengine.expression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kogu.queryengine.columnar.BoolVec;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.columnar.Vectors;
import org.kogu.queryengine.expression.Expr.LogicalOp;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type.Scalar;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExprTracerTest {

    @Test
    @DisplayName("Traces post-order evaluation flow over a batch with nulls and constants")
    void tracesPostOrderEvaluationFlow() {
        // Prepare 8-row batch: trip_distance (nulls at 2, 7), fare_amount (null at 5)
        int[] distances = {3, 8, 999, 5, 12, 1, 6, 999};
        BitSet distNulls = new BitSet();
        distNulls.set(2);
        distNulls.set(7);

        int[] fares = {15, 25, 30, 5, 50, 999, 40, 20};
        BitSet fareNulls = new BitSet();
        fareNulls.set(5);

        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Scalar.INT32, true),
                new Field("fare_amount", Scalar.INT32, true)
        ));

        RecordBatch batch = new RecordBatch(schema, new Vector[]{
                Vectors.intVector(distances, distNulls),
                Vectors.intVector(fares, fareNulls)
        });

        // Expression: (trip_distance > 5) AND (fare_amount > 20)
        Expr expr = new LogicalExpr(
                new BinaryExpr(new ColumnRef("trip_distance"), ComparisonOp.GT, new Literal.Int32(5)),
                LogicalOp.AND,
                new BinaryExpr(new ColumnRef("fare_amount"), ComparisonOp.GT, new Literal.Int32(20))
        );

        ExprTracer.TraceReport report = ExprTracer.trace(expr, batch);

        assertNotNull(report);
        assertEquals(7, report.steps().size(), "7 post-order execution steps expected");

        // Step 1: ColumnRef("trip_distance")
        ExprTracer.TraceStep step1 = report.steps().get(0);
        assertEquals(1, step1.stepNumber());
        assertInstanceOf(ColumnRef.class, step1.expr());
        assertTrue(step1.inputStepNumbers().isEmpty());
        assertTrue(step1.resultVector().isNull(2));
        assertTrue(step1.resultVector().isNull(7));

        // Step 2: Literal.Int32(5)
        ExprTracer.TraceStep step2 = report.steps().get(1);
        assertEquals(2, step2.stepNumber());
        assertInstanceOf(Literal.Int32.class, step2.expr());

        // Step 3: trip_distance > 5 (GT)
        ExprTracer.TraceStep step3 = report.steps().get(2);
        assertEquals(3, step3.stepNumber());
        assertEquals(List.of(1, 2), step3.inputStepNumbers());
        assertInstanceOf(BoolVec.class, step3.resultVector());
        assertTrue(step3.resultVector().isNull(2));
        assertTrue(step3.resultVector().isNull(7));

        // Step 4: ColumnRef("fare_amount")
        ExprTracer.TraceStep step4 = report.steps().get(3);
        assertEquals(4, step4.stepNumber());
        assertTrue(step4.resultVector().isNull(5));

        // Step 5: Literal.Int32(20)
        ExprTracer.TraceStep step5 = report.steps().get(4);
        assertEquals(5, step5.stepNumber());

        // Step 6: fare_amount > 20 (GT)
        ExprTracer.TraceStep step6 = report.steps().get(5);
        assertEquals(6, step6.stepNumber());
        assertEquals(List.of(4, 5), step6.inputStepNumbers());
        assertTrue(step6.resultVector().isNull(5));

        // Step 7: (trip_distance > 5) AND (fare_amount > 20)
        ExprTracer.TraceStep step7 = report.steps().get(6);
        assertEquals(7, step7.stepNumber());
        assertEquals(List.of(3, 6), step7.inputStepNumbers());
        // Nulls in logical AND:
        // row 0: false AND false -> false, valid (3VL false dominates)
        // row 1: true AND true -> true, valid
        // row 2: null AND true -> null, invalid
        // row 3: false AND false -> false, valid
        // row 4: true AND true -> true, valid
        // row 5: false AND null -> false, valid (3VL false dominates!)
        // row 6: true AND true -> true, valid
        // row 7: null AND false -> false, valid (3VL false dominates!)
        BoolVec finalVec = (BoolVec) report.finalResult();
        assertEquals(8, finalVec.length());
        assertFalse(finalVec.isNull(0));
        assertFalse(finalVec.value(0));
        assertFalse(finalVec.isNull(1));
        assertTrue(finalVec.value(1));
        assertTrue(finalVec.isNull(2), "Row 2: null AND true must be null");
        assertFalse(finalVec.isNull(5), "Row 5: false AND null must be false (valid) via 3VL");
        assertFalse(finalVec.value(5));
        assertFalse(finalVec.isNull(7), "Row 7: null AND false must be false (valid) via 3VL");
        assertFalse(finalVec.value(7));

        // Formatted trace report output is readable and contains expected sections
        String output = report.toString();
        assertTrue(output.contains("EVALUATION EXECUTION TRACE"));
        assertTrue(output.contains("Step 1: EVAL ColumnRef(\"trip_distance\")"));
        assertTrue(output.contains("Step 7: EVAL LogicalExpr AND"));
        assertTrue(output.contains("Batch: 8 rows"));
    }
}
