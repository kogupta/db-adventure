package org.kogu.queryengine.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kogu.queryengine.columnar.*;
import org.kogu.queryengine.expression.Expr.LogicalOp;
import org.kogu.queryengine.expression.Expr.UnaryOp;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type.Scalar;

import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExprTest {

    @Test
    void columnReferenceResolvesTypeAndCollectsItsName() {
        ColumnRef column = new ColumnRef("fare_amount");
        Schema schema = Schema.from(List.of(
                new Field("fare_amount", Scalar.INT64, false)));
        Set<String> references = new LinkedHashSet<>();

        assertEquals(Scalar.INT64, column.outputType(schema).type());
        column.collectReferences(references);
        assertEquals(Set.of("fare_amount"), references);
    }

    @Test
    void everyLiteralReportsItsScalarType() {
        Schema schema = Schema.from(List.of());

        assertEquals(Scalar.INT32, new Literal.Int32(1).outputType(schema).type());
        assertEquals(Scalar.INT64, new Literal.Int64(1L).outputType(schema).type());
        assertEquals(Scalar.FLOAT64, new Literal.Float64(1.0).outputType(schema).type());
        assertEquals(Scalar.UTF8, new Literal.Str("one").outputType(schema).type());
        assertEquals(Scalar.BOOLEAN, Literal.Bool.True.outputType(schema).type());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expressionTypeCases")
    void derivesExpressionTypeAndNullability(
            String name, Schema schema, Expr expression, ExprType expected) {
        assertEquals(expected, expression.outputType(schema), name);
    }

    private static Stream<Arguments> expressionTypeCases() {
        Schema schema = Schema.from(List.of(
                new Field("nullable_int", Scalar.INT32, true),
                new Field("required_int", Scalar.INT32, false),
                new Field("nullable_bool", Scalar.BOOLEAN, true),
                new Field("required_bool", Scalar.BOOLEAN, false)));

        Expr nullableInt = new ColumnRef("nullable_int");
        Expr requiredInt = new ColumnRef("required_int");
        Expr nullableBool = new ColumnRef("nullable_bool");
        Expr requiredBool = new ColumnRef("required_bool");
        Expr one = new Literal.Int32(1);

        return Stream.of(
                Arguments.of("nullable column", schema, nullableInt,
                        new ExprType(Scalar.INT32, true)),
                Arguments.of("required column", schema, requiredInt,
                        new ExprType(Scalar.INT32, false)),
                Arguments.of("literal", schema, one,
                        new ExprType(Scalar.INT32, false)),
                Arguments.of("arithmetic with nullable operand", schema,
                        new BinaryExpr(nullableInt, ArithmeticOp.Add, one),
                        new ExprType(Scalar.INT32, true)),
                Arguments.of("arithmetic with required operands", schema,
                        new BinaryExpr(requiredInt, ArithmeticOp.Add, one),
                        new ExprType(Scalar.INT32, false)),
                Arguments.of("comparison with nullable operand", schema,
                        new BinaryExpr(nullableInt, ComparisonOp.GT, one),
                        new ExprType(Scalar.BOOLEAN, true)),
                Arguments.of("NOT preserves nullability", schema,
                        new UnaryExpr(UnaryOp.NOT, nullableBool),
                        new ExprType(Scalar.BOOLEAN, true)),
                Arguments.of("AND propagates nullability", schema,
                        new LogicalExpr(nullableBool, LogicalOp.AND, requiredBool),
                        new ExprType(Scalar.BOOLEAN, true)),
                Arguments.of("OR over required operands remains required", schema,
                        new LogicalExpr(requiredBool, LogicalOp.OR, Literal.Bool.False),
                        new ExprType(Scalar.BOOLEAN, false)));
    }

    @Test
    void comparisonOperatorsAcceptMatchingTypesAndReturnBoolean() {
        Schema schema = Schema.from(List.of());
        Expr left = new Literal.Int32(1);
        Expr right = new Literal.Int32(2);

        for (Expr.BinaryOp op : List.of(
                ComparisonOp.EqEq,
                ComparisonOp.NEq,
                ComparisonOp.LT,
                ComparisonOp.LTEq,
                ComparisonOp.GT,
                ComparisonOp.GTEq)) {
            assertEquals(Scalar.BOOLEAN, new BinaryExpr(left, op, right).outputType(schema).type());
        }
    }

    @Test
    void rejectsComparisonOfDifferentTypesWithOperandDetails() {
        Schema schema = Schema.from(List.of());
        BinaryExpr expression = new BinaryExpr(
                new Literal.Int32(1), ComparisonOp.EqEq,
                new Literal.Int64(1L));

        Expr.TypeMismatchException error = assertThrows(
                Expr.TypeMismatchException.class,
                () -> expression.outputType(schema));

        assertEquals("Cannot compare operands for ==: left type INT32, right type INT64",
                error.getMessage());
    }

    @Test
    void arithmeticSupportsNumericTypesAndRejectsOtherTypes() {
        Schema schema = Schema.from(List.of());
        for (Scalar type : List.of(Scalar.INT32, Scalar.INT64, Scalar.FLOAT64)) {
            Expr literal = switch (type) {
                case INT32 -> new Literal.Int32(1);
                case INT64 -> new Literal.Int64(1L);
                case FLOAT64 -> new Literal.Float64(1.0);
                default -> throw new AssertionError(type);
            };
            assertEquals(type, new BinaryExpr(literal, ArithmeticOp.Add, literal).outputType(schema).type());
        }

        Expr.TypeMismatchException error = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new BinaryExpr(
                        new Literal.Str("a"), ArithmeticOp.Add,
                        new Literal.Str("b")).outputType(schema));
        assertEquals("Cannot apply + to operands: left type UTF8, right type UTF8",
                error.getMessage());
    }

    @Test
    void unaryAndLogicalOperatorsRequireBooleanOperands() {
        Schema schema = Schema.from(List.of());
        Expr bool = Literal.Bool.True;

        assertEquals(Scalar.BOOLEAN, new UnaryExpr(UnaryOp.NOT, bool).outputType(schema).type());
        assertEquals(Scalar.BOOLEAN, new LogicalExpr(bool, LogicalOp.AND, bool).outputType(schema).type());

        Expr.TypeMismatchException unaryError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new UnaryExpr(UnaryOp.NOT, new Literal.Int32(1))
                        .outputType(schema));
        assertEquals("Cannot apply NOT: operand type INT32, expected BOOLEAN", unaryError.getMessage());

        Expr.TypeMismatchException logicalError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new LogicalExpr(
                        bool, LogicalOp.OR, new Literal.Int32(1)).outputType(schema));
        assertEquals("Cannot apply OR: left type BOOLEAN, right type INT32, expected BOOLEAN operands",
                logicalError.getMessage());
    }

    @Test
    void nestedExpressionsCollectDistinctReferences() {
        Expr expression = new LogicalExpr(
                new BinaryExpr(
                        new ColumnRef("fare_amount"), ComparisonOp.GT,
                        new Literal.Int32(5)), LogicalOp.AND,
                new BinaryExpr(
                        new ColumnRef("fare_amount"), ComparisonOp.LT,
                        new ColumnRef("trip_distance")));
        Set<String> references = new LinkedHashSet<>();

        expression.collectReferences(references);

        assertEquals(List.of("fare_amount", "trip_distance"), List.copyOf(references));
    }

    @Test
    void missingColumnReportsItsName() {
        ColumnRef column = new ColumnRef("missing");
        Schema schema = Schema.from(List.of());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> column.outputType(schema));

        assertInstanceOf(IllegalArgumentException.class, error);
        assertEquals("Field not found: missing in schema: []", error.getMessage());
    }

    @Test
    void evaluatesArithmeticOperationsForIntLongDouble() {
        BitSet leftNulls = new BitSet();
        leftNulls.set(0); // index 0 is null in left
        BitSet rightNulls = new BitSet();
        rightNulls.set(1); // index 1 is null in right

        // INT32
        IntVec intLeft = Vectors.intVector(new int[]{0, 10, 20, 30}, leftNulls);
        IntVec intRight = Vectors.intVector(new int[]{5, 0, 4, 3}, rightNulls);
        Schema intSchema = Schema.from(List.of(
                new Field("a", Scalar.INT32, true),
                new Field("b", Scalar.INT32, true)));
        RecordBatch intBatch = new RecordBatch(intSchema, new Vector[]{intLeft, intRight});

        IntVec.IntVector intAdd = (IntVec.IntVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Add, new ColumnRef("b")), intBatch);
        assertTrue(intAdd.isNull(0));
        assertTrue(intAdd.isNull(1));
        assertFalse(intAdd.isNull(2));
        assertEquals(24, intAdd.value(2));
        assertEquals(33, intAdd.value(3));

        IntVec.IntVector intSub = (IntVec.IntVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Subtract, new ColumnRef("b")), intBatch);
        assertEquals(16, intSub.value(2));
        assertEquals(27, intSub.value(3));

        IntVec.IntVector intMul = (IntVec.IntVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Multiply, new ColumnRef("b")), intBatch);
        assertEquals(80, intMul.value(2));
        assertEquals(90, intMul.value(3));

        IntVec.IntVector intDiv = (IntVec.IntVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Divide, new ColumnRef("b")), intBatch);
        assertEquals(5, intDiv.value(2));
        assertEquals(10, intDiv.value(3));

        // INT64
        Vector.LongVec longLeft = Vectors.longVector(new long[]{0L, 100L, 200L, 300L}, leftNulls);
        Vector.LongVec longRight = Vectors.longVector(new long[]{5L, 0L, 4L, 3L}, rightNulls);
        Schema longSchema = Schema.from(List.of(
                new Field("a", Scalar.INT64, true),
                new Field("b", Scalar.INT64, true)));
        RecordBatch longBatch = new RecordBatch(longSchema, new Vector[]{longLeft, longRight});

        Vector.LongVector longAdd = (Vector.LongVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Add, new ColumnRef("b")), longBatch);
        assertTrue(longAdd.isNull(0));
        assertTrue(longAdd.isNull(1));
        assertEquals(204L, longAdd.value(2));
        assertEquals(303L, longAdd.value(3));

        Vector.LongVector longSub = (Vector.LongVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Subtract, new ColumnRef("b")), longBatch);
        assertEquals(196L, longSub.value(2));

        Vector.LongVector longMul = (Vector.LongVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Multiply, new ColumnRef("b")), longBatch);
        assertEquals(800L, longMul.value(2));

        Vector.LongVector longDiv = (Vector.LongVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Divide, new ColumnRef("b")), longBatch);
        assertEquals(50L, longDiv.value(2));

        // FLOAT64
        DoubleVec doubleLeft = Vectors.doubleVector(new double[]{0.0, 10.0, 20.0, 30.0}, leftNulls);
        DoubleVec doubleRight = Vectors.doubleVector(new double[]{5.0, 0.0, 4.0, 3.0}, rightNulls);
        Schema doubleSchema = Schema.from(List.of(
                new Field("a", Scalar.FLOAT64, true),
                new Field("b", Scalar.FLOAT64, true)));
        RecordBatch doubleBatch = new RecordBatch(doubleSchema, new Vector[]{doubleLeft, doubleRight});

        DoubleVec.DoubleVector doubleAdd = (DoubleVec.DoubleVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Add, new ColumnRef("b")), doubleBatch);
        assertTrue(doubleAdd.isNull(0));
        assertTrue(doubleAdd.isNull(1));
        assertEquals(24.0, doubleAdd.value(2));
        assertEquals(33.0, doubleAdd.value(3));

        DoubleVec.DoubleVector doubleSub = (DoubleVec.DoubleVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Subtract, new ColumnRef("b")), doubleBatch);
        assertEquals(16.0, doubleSub.value(2));

        DoubleVec.DoubleVector doubleMul = (DoubleVec.DoubleVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Multiply, new ColumnRef("b")), doubleBatch);
        assertEquals(80.0, doubleMul.value(2));

        DoubleVec.DoubleVector doubleDiv = (DoubleVec.DoubleVector) ExprEvaluator.eval(
                new BinaryExpr(new ColumnRef("a"), ArithmeticOp.Divide, new ColumnRef("b")), doubleBatch);
        assertEquals(5.0, doubleDiv.value(2));
    }

}
