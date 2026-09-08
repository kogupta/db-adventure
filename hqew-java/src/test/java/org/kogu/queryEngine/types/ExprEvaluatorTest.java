package org.kogu.queryEngine.types;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kogu.queryEngine.types.Expr.*;
import org.kogu.queryEngine.types.Type.Scalar;

import static org.junit.jupiter.api.Assertions.*;

class ExprEvaluatorTest {

    @Test
    @DisplayName("Invariant 1: Every expression shape returns length() == batch.rowCount()")
    void everyExpressionShapeReturnsLengthMatchingBatchRowCount() {
        int rowCount = 8;
        Schema schema = Schema.from(List.of(
                new Field("c_int", Scalar.INT32, true),
                new Field("c_long", Scalar.INT64, true),
                new Field("c_double", Scalar.FLOAT64, true),
                new Field("c_utf8", Scalar.UTF8, true),
                new Field("c_bool", Scalar.BOOLEAN, true)
        ));

        RecordBatch batch = new RecordBatch(schema, new Vector[]{
                Vectors.intVector(new int[]{1, 2, 3, 4, 5, 6, 7, 8}),
                Vectors.longVector(new long[]{10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L}),
                Vectors.doubleVector(new double[]{1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8}),
                Vectors.stringVector(new String[]{"a", "b", "c", "d", "e", "f", "g", "h"}),
                Vectors.booleanVector(new boolean[]{true, false, true, false, true, false, true, false})
        });

        assertEquals(rowCount, batch.rowCount());

        List<Expr> expressions = List.of(
                // 1. ColumnRef
                new ColumnRef("c_int"),
                new ColumnRef("c_long"),
                new ColumnRef("c_double"),
                new ColumnRef("c_utf8"),
                new ColumnRef("c_bool"),

                // 2. Literals
                new Literal.Int32(42),
                new Literal.Int64(42L),
                new Literal.Float64(42.0),
                new Literal.Str("constant"),
                Literal.Bool.True,
                Literal.Bool.False,

                // 3. BinaryExpr - Arithmetic
                new BinaryExpr(new ColumnRef("c_int"), ArithmeticOps.Add, new Literal.Int32(1)),
                new BinaryExpr(new ColumnRef("c_long"), ArithmeticOps.Subtract, new Literal.Int64(2L)),
                new BinaryExpr(new ColumnRef("c_double"), ArithmeticOps.Multiply, new Literal.Float64(1.5)),
                new BinaryExpr(new ColumnRef("c_int"), ArithmeticOps.Divide, new Literal.Int32(2)),

                // 4. BinaryExpr - Comparison
                new BinaryExpr(new ColumnRef("c_int"), ComparisionOps.EqEq, new Literal.Int32(3)),
                new BinaryExpr(new ColumnRef("c_long"), ComparisionOps.NEq, new Literal.Int64(20L)),
                new BinaryExpr(new ColumnRef("c_double"), ComparisionOps.LT, new Literal.Float64(5.0)),
                new BinaryExpr(new ColumnRef("c_utf8"), ComparisionOps.LTEq, new Literal.Str("c")),
                new BinaryExpr(new ColumnRef("c_int"), ComparisionOps.GT, new Literal.Int32(5)),
                new BinaryExpr(new ColumnRef("c_int"), ComparisionOps.GTEq, new Literal.Int32(0)),
                new BinaryExpr(new ColumnRef("c_bool"), ComparisionOps.EqEq, Literal.Bool.False),

                // 5. UnaryExpr - NOT
                new UnaryExpr(UnaryOp.NOT, new ColumnRef("c_bool")),
                new UnaryExpr(UnaryOp.NOT, Literal.Bool.True),

                // 6. LogicalExpr - AND / OR
                new LogicalExpr(new ColumnRef("c_bool"), LogicalOp.AND, Literal.Bool.True),
                new LogicalExpr(new ColumnRef("c_bool"), LogicalOp.OR, Literal.Bool.False)
        );

        for (Expr expr : expressions) {
            Vector result = ExprEvaluator.eval(expr, batch);
            assertEquals(batch.rowCount(), result.length(),
                    () -> "Expression " + expr + " returned vector of length " + result.length()
                            + " instead of " + batch.rowCount());
        }
    }

    @Test
    @DisplayName("Invariant 2: trip_distance > 5 over the 8-row batch — rows 2 and 7 invalid (junk invariant)")
    void tripDistanceGreaterThanFiveOverEightRowBatchLeavesRowsTwoAndSevenInvalid() {
        // Valid rows: 0 (3 > 5 -> false), 1 (8 > 5 -> true), 3 (5 > 5 -> false),
        //             4 (12 > 5 -> true), 5 (1 > 5 -> false), 6 (6 > 5 -> true)
        // Rows 2 and 7 are null. We construct two batches with differing junk values:
        // batch 1 has junk 999 (999 > 5 is true)
        // batch 2 has junk -1  (-1 > 5 is false)
        int[] distances1 = {3, 8, 999, 5, 12, 1, 6, 999};
        int[] distances2 = {3, 8,  -1, 5, 12, 1, 6,  -1};

        BitSet nulls1 = new BitSet();
        nulls1.set(2);
        nulls1.set(7);

        BitSet nulls2 = (BitSet) nulls1.clone();

        Schema schema = Schema.from(List.of(new Field("trip_distance", Scalar.INT32, true)));
        RecordBatch batch1 = new RecordBatch(schema, new Vector[]{Vectors.intVector(distances1, nulls1)});
        RecordBatch batch2 = new RecordBatch(schema, new Vector[]{Vectors.intVector(distances2, nulls2)});

        assertEquals(8, batch1.rowCount());
        assertEquals(8, batch2.rowCount());

        Expr predicate = new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5));
        Vector.BoolVec result1 = (Vector.BoolVec) ExprEvaluator.eval(predicate, batch1);
        Vector.BoolVec result2 = (Vector.BoolVec) ExprEvaluator.eval(predicate, batch2);

        assertEquals(8, result1.length());
        assertEquals(8, result2.length());

        // In both results, rows 2 and 7 must be invalid (null)
        assertTrue(result1.isNull(2), "Row 2 must be invalid in result1");
        assertTrue(result1.isNull(7), "Row 7 must be invalid in result1");
        assertTrue(result2.isNull(2), "Row 2 must be invalid in result2");
        assertTrue(result2.isNull(7), "Row 7 must be invalid in result2");

        // Gated / valid reads remain identical across both batches regardless of junk value
        for (int i = 0; i < 8; i++) {
            assertEquals(result1.isNull(i), result2.isNull(i), "Nullness must match at row " + i);
            if (!result1.isNull(i)) {
                assertEquals(result1.value(i), result2.value(i),
                        "Valid row " + i + " must produce identical result regardless of null-slot junk");
            }
        }

        // Verify exact expected booleans at valid rows
        assertFalse(result1.value(0), "Row 0: 3 > 5 must evaluate to false");
        assertTrue(result1.value(1), "Row 1: 8 > 5 must evaluate to true");
        assertFalse(result1.value(3), "Row 3: 5 > 5 must evaluate to false");
        assertTrue(result1.value(4), "Row 4: 12 > 5 must evaluate to true");
        assertFalse(result1.value(5), "Row 5: 1 > 5 must evaluate to false");
        assertTrue(result1.value(6), "Row 6: 6 > 5 must evaluate to true");

        // Gated non-null values match expected list: [false, true, false, true, false, true]
        assertEquals(List.of(false, true, false, true, false, true), readNonNullBools(result1));
        assertEquals(readNonNullBools(result1), readNonNullBools(result2));
    }

    @Test
    @DisplayName("Invariant 3: false AND null is false and valid. true OR null is true and valid")
    void shortCircuitThreeValuedLogicEvaluatesDominantValuesAsValid() {
        // We construct a batch covering 3-valued logic truth table with nulls:
        // row 0: false AND null -> false, valid
        // row 1: true OR null   -> true, valid
        // row 2: null AND false -> false, valid
        // row 3: null OR true   -> true, valid
        // row 4: true AND null  -> null, invalid
        // row 5: false OR null  -> null, invalid
        // row 6: null AND null  -> null, invalid
        // row 7: null OR null   -> null, invalid

        boolean[] leftValues =  {false, true,  false, false, true,  false, false, false};
        boolean[] rightValues = {false, false, false, true,  false, false, false, false};

        BitSet leftNulls = new BitSet();
        leftNulls.set(2); // null in left
        leftNulls.set(3); // null in left
        leftNulls.set(6); // null in left
        leftNulls.set(7); // null in left

        BitSet rightNulls = new BitSet();
        rightNulls.set(0); // null in right
        rightNulls.set(1); // null in right
        rightNulls.set(4); // null in right
        rightNulls.set(5); // null in right
        rightNulls.set(6); // null in right
        rightNulls.set(7); // null in right

        Schema schema = Schema.from(List.of(
                new Field("a", Scalar.BOOLEAN, true),
                new Field("b", Scalar.BOOLEAN, true)
        ));

        RecordBatch batch = new RecordBatch(schema, new Vector[]{
                Vectors.booleanVector(leftValues, leftNulls),
                Vectors.booleanVector(rightValues, rightNulls)
        });

        Expr andExpr = new LogicalExpr(new ColumnRef("a"), LogicalOp.AND, new ColumnRef("b"));
        Expr orExpr = new LogicalExpr(new ColumnRef("a"), LogicalOp.OR, new ColumnRef("b"));

        Vector.BoolVec andResult = (Vector.BoolVec) ExprEvaluator.eval(andExpr, batch);
        Vector.BoolVec orResult = (Vector.BoolVec) ExprEvaluator.eval(orExpr, batch);

        // Core requirement 1: false AND null is false and valid
        assertFalse(andResult.isNull(0), "false AND null must be valid");
        assertFalse(andResult.value(0), "false AND null must evaluate to false");

        // Symmetric check: null AND false is false and valid
        assertFalse(andResult.isNull(2), "null AND false must be valid");
        assertFalse(andResult.value(2), "null AND false must evaluate to false");

        // Core requirement 2: true OR null is true and valid
        assertFalse(orResult.isNull(1), "true OR null must be valid");
        assertTrue(orResult.value(1), "true OR null must evaluate to true");

        // Symmetric check: null OR true is true and valid
        assertFalse(orResult.isNull(3), "null OR true must be valid");
        assertTrue(orResult.value(3), "null OR true must evaluate to true");

        // Non-dominant operations retain null (invalid)
        assertTrue(andResult.isNull(1), "true AND null must be invalid (null)");
        assertTrue(orResult.isNull(0), "false OR null must be invalid (null)");
        assertTrue(andResult.isNull(4), "true AND null must be invalid (null)");
        assertTrue(orResult.isNull(5), "false OR null must be invalid (null)");
        assertTrue(andResult.isNull(6), "null AND null must be invalid (null)");
        assertTrue(orResult.isNull(7), "null OR null must be invalid (null)");
    }

    @Test
    @DisplayName("Invariant 4: One 8-row batch against batches of 3, 3, 2 — concatenate, compare slot for slot")
    void boundaryInvisibilityOneBatchAgainstSlicesOfThreeThreeTwo() {
        int[] distances = {4, 10, 999, 7, 2, 8, 12, 1};
        BitSet distNulls = new BitSet();
        distNulls.set(2); // row 2 is null in distances

        int[] fares = {15, 25, 30, 5, 50, 999, 40, 20};
        BitSet fareNulls = new BitSet();
        fareNulls.set(5); // row 5 is null in fares

        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Scalar.INT32, true),
                new Field("fare_amount", Scalar.INT32, true)
        ));

        // 1 single 8-row batch
        RecordBatch singleBatch = new RecordBatch(schema, new Vector[]{
                new Vector.IntVector(distances, 0, 8, distNulls),
                new Vector.IntVector(fares, 0, 8, fareNulls)
        });

        // 3 partitioned batches of sizes 3, 3, 2 windowed over the same backing data
        RecordBatch batch1 = new RecordBatch(schema, new Vector[]{
                new Vector.IntVector(distances, 0, 3, distNulls),
                new Vector.IntVector(fares, 0, 3, fareNulls)
        });
        RecordBatch batch2 = new RecordBatch(schema, new Vector[]{
                new Vector.IntVector(distances, 3, 3, distNulls),
                new Vector.IntVector(fares, 3, 3, fareNulls)
        });
        RecordBatch batch3 = new RecordBatch(schema, new Vector[]{
                new Vector.IntVector(distances, 6, 2, distNulls),
                new Vector.IntVector(fares, 6, 2, fareNulls)
        });

        List<RecordBatch> pieces = List.of(batch1, batch2, batch3);

        List<Expr> expressionsToTest = List.of(
                // Arithmetic
                new BinaryExpr(new ColumnRef("fare_amount"), ArithmeticOps.Add, new ColumnRef("trip_distance")),
                // Comparison
                new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5)),
                // Compound: (trip_distance > 5) AND (fare_amount > 20)
                new LogicalExpr(
                        new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5)),
                        LogicalOp.AND,
                        new BinaryExpr(new ColumnRef("fare_amount"), ComparisionOps.GT, new Literal.Int32(20))
                )
        );

        for (Expr expr : expressionsToTest) {
            Vector singleResult = ExprEvaluator.eval(expr, singleBatch);
            List<Vector> pieceResults = pieces.stream()
                    .map(b -> ExprEvaluator.eval(expr, b))
                    .toList();

            assertVectorsEqualSlotForSlot(singleResult, pieceResults);
        }
    }

    @Test
    @DisplayName("Invariant 5: Evaluator rejects mixed types with TypeMismatchException")
    void rejectsMixedTypeEvaluationWithDetails() {
        Schema schema = Schema.from(List.of(
                new Field("c_int", Scalar.INT32, true),
                new Field("c_long", Scalar.INT64, true)
        ));
        RecordBatch batch = new RecordBatch(schema, new Vector[]{
                Vectors.intVector(new int[]{1, 2}),
                Vectors.longVector(new long[]{10L, 20L})
        });

        Expr mixedArithmetic = new BinaryExpr(
                new ColumnRef("c_int"), ArithmeticOps.Add, new ColumnRef("c_long"));

        Expr.TypeMismatchException arithmeticError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> ExprEvaluator.eval(mixedArithmetic, batch));
        assertEquals("Cannot apply + to operands: left type INT32, right type INT64",
                arithmeticError.getMessage());

        Expr mixedComparison = new BinaryExpr(
                new ColumnRef("c_int"), ComparisionOps.EqEq, new ColumnRef("c_long"));

        Expr.TypeMismatchException comparisonError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> ExprEvaluator.eval(mixedComparison, batch));
        assertEquals("Cannot compare operands for ==: left type INT32, right type INT64",
                comparisonError.getMessage());
    }

    private static List<Boolean> readNonNullBools(Vector.BoolVec vec) {
        var list = new ArrayList<Boolean>();
        for (int i = 0; i < vec.length(); i++) {
            if (vec.isNotNull(i)) {
                list.add(vec.value(i));
            }
        }
        return List.copyOf(list);
    }

    private static void assertVectorsEqualSlotForSlot(Vector expected, List<Vector> pieces) {
        int totalLength = pieces.stream().mapToInt(Vector::length).sum();
        assertEquals(expected.length(), totalLength, "Total length across pieces must equal single batch length");

        int globalRow = 0;
        for (Vector piece : pieces) {
            for (int localRow = 0; localRow < piece.length(); localRow++) {
                assertEquals(expected.isNull(globalRow), piece.isNull(localRow),
                        "Nullness mismatch at global row " + globalRow);

                if (!expected.isNull(globalRow)) {
                    switch (expected) {
                        case Vector.IntVec exp when piece instanceof Vector.IntVec act ->
                                assertEquals(exp.value(globalRow), act.value(localRow),
                                        "Int value mismatch at global row " + globalRow);
                        case Vector.LongVec exp when piece instanceof Vector.LongVec act ->
                                assertEquals(exp.value(globalRow), act.value(localRow),
                                        "Long value mismatch at global row " + globalRow);
                        case Vector.DoubleVec exp when piece instanceof Vector.DoubleVec act ->
                                assertEquals(exp.value(globalRow), act.value(localRow), 1e-9,
                                        "Double value mismatch at global row " + globalRow);
                        case Vector.BoolVec exp when piece instanceof Vector.BoolVec act ->
                                assertEquals(exp.value(globalRow), act.value(localRow),
                                        "Boolean value mismatch at global row " + globalRow);
                        case Vector.Utf8Vec exp when piece instanceof Vector.Utf8Vec act ->
                                assertEquals(exp.value(globalRow), act.value(localRow),
                                        "String value mismatch at global row " + globalRow);
                        default -> fail("Unexpected vector type: " + expected.getClass());
                    }
                }
                globalRow++;
            }
        }
    }
}
