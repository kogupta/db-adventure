package org.kogu.queryEngine.types;

import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.kogu.queryEngine.types.Expr.*;
import org.kogu.queryEngine.types.Type.Scalar;

import static org.junit.jupiter.api.Assertions.*;

class ExprTest {

    @Test
    void columnReferenceResolvesTypeAndCollectsItsName() {
        ColumnRef column = new ColumnRef("fare_amount");
        Schema schema = Schema.from(List.of(
                new Field("fare_amount", Scalar.INT64, false)));
        Set<String> references = new LinkedHashSet<>();

        assertEquals(Scalar.INT64, column.outputType(schema));
        column.collectReferences(references);
        assertEquals(Set.of("fare_amount"), references);
    }

    @Test
    void everyLiteralReportsItsScalarType() {
        Schema schema = Schema.from(List.of());

        assertEquals(Scalar.INT32, new Literal.Int32(1).outputType(schema));
        assertEquals(Scalar.INT64, new Literal.Int64(1L).outputType(schema));
        assertEquals(Scalar.FLOAT64, new Literal.Float64(1.0).outputType(schema));
        assertEquals(Scalar.UTF8, new Literal.Str("one").outputType(schema));
        assertEquals(Scalar.BOOLEAN, Literal.Bool.True.outputType(schema));
    }

    @Test
    void comparisonOperatorsAcceptMatchingTypesAndReturnBoolean() {
        Schema schema = Schema.from(List.of());
        Expr left = new Literal.Int32(1);
        Expr right = new Literal.Int32(2);

        for (Expr.BinaryOp op : List.of(
                ComparisionOps.EqEq,
                ComparisionOps.NEq,
                ComparisionOps.LT,
                ComparisionOps.LTEq,
                ComparisionOps.GT,
                ComparisionOps.GTEq)) {
            assertEquals(Scalar.BOOLEAN,
                    new BinaryExpr(left, op, right).outputType(schema));
        }
    }

    @Test
    void rejectsComparisonOfDifferentTypesWithOperandDetails() {
        Schema schema = Schema.from(List.of());
        BinaryExpr expression = new BinaryExpr(
                new Literal.Int32(1), ComparisionOps.EqEq,
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
            assertEquals(type,
                    new BinaryExpr(literal, ArithmeticOps.Add, literal).outputType(schema));
        }

        Expr.TypeMismatchException error = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new BinaryExpr(
                        new Literal.Str("a"), ArithmeticOps.Add,
                        new Literal.Str("b")).outputType(schema));
        assertEquals("Cannot apply + to operands: left type UTF8, right type UTF8",
                error.getMessage());
    }

    @Test
    void unaryAndLogicalOperatorsRequireBooleanOperands() {
        Schema schema = Schema.from(List.of());
        Expr bool = Literal.Bool.True;

        assertEquals(Scalar.BOOLEAN,
                new Expr.UnaryExpr(UnaryOp.NOT, bool).outputType(schema));
        assertEquals(Scalar.BOOLEAN,
                new Expr.LogicalExpr(bool, LogicalOp.AND, bool).outputType(schema));

        Expr.TypeMismatchException unaryError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new Expr.UnaryExpr(UnaryOp.NOT, new Literal.Int32(1))
                        .outputType(schema));
        assertEquals("Cannot apply NOT: operand type INT32, expected BOOLEAN", unaryError.getMessage());

        Expr.TypeMismatchException logicalError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new Expr.LogicalExpr(
                        bool, LogicalOp.OR, new Literal.Int32(1)).outputType(schema));
        assertEquals("Cannot apply OR: left type BOOLEAN, right type INT32, expected BOOLEAN operands",
                logicalError.getMessage());
    }

    @Test
    void nestedExpressionsCollectDistinctReferences() {
        Expr expression = new Expr.LogicalExpr(
                new BinaryExpr(
                        new ColumnRef("fare_amount"), ComparisionOps.GT,
                        new Literal.Int32(5)), LogicalOp.AND,
                new BinaryExpr(
                        new ColumnRef("fare_amount"), ComparisionOps.LT,
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

}
