package org.kogu.queryEngine.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ExprTest {

    @Test
    void columnReferenceResolvesTypeAndCollectsItsName() {
        Expr.ColumnRef column = new Expr.ColumnRef("fare_amount");
        Schema schema = Schema.from(List.of(
                new Field("fare_amount", Type.Scalar.INT64, false)));
        Set<String> references = new LinkedHashSet<>();

        assertEquals(Type.Scalar.INT64, column.outputType(schema));
        column.collectReferences(references);
        assertEquals(Set.of("fare_amount"), references);
    }

    @Test
    void everyLiteralReportsItsScalarType() {
        Schema schema = Schema.from(List.of());

        assertEquals(Type.Scalar.INT32, new Expr.Literal.Int32(1).outputType(schema));
        assertEquals(Type.Scalar.INT64, new Expr.Literal.Int64(1L).outputType(schema));
        assertEquals(Type.Scalar.FLOAT64, new Expr.Literal.Float64(1.0).outputType(schema));
        assertEquals(Type.Scalar.UTF8, new Expr.Literal.Str("one").outputType(schema));
        assertEquals(Type.Scalar.BOOLEAN, Expr.Literal.Bool.True.outputType(schema));
    }

    @Test
    void comparisonOperatorsAcceptMatchingTypesAndReturnBoolean() {
        Schema schema = Schema.from(List.of());
        Expr left = new Expr.Literal.Int32(1);
        Expr right = new Expr.Literal.Int32(2);

        for (Expr.BinaryOp op : List.of(
                Expr.BinaryOp.EqualEqual,
                Expr.BinaryOp.NotEqual,
                Expr.BinaryOp.Less,
                Expr.BinaryOp.LessEqual,
                Expr.BinaryOp.Greater,
                Expr.BinaryOp.GreaterEqual)) {
            assertEquals(Type.Scalar.BOOLEAN,
                    new Expr.BinaryExpr(op, left, right).outputType(schema));
        }
    }

    @Test
    void rejectsComparisonOfDifferentTypesWithOperandDetails() {
        Schema schema = Schema.from(List.of());
        Expr.BinaryExpr expression = new Expr.BinaryExpr(
                Expr.BinaryOp.EqualEqual,
                new Expr.Literal.Int32(1),
                new Expr.Literal.Int64(1L));

        Expr.TypeMismatchException error = assertThrows(
                Expr.TypeMismatchException.class,
                () -> expression.outputType(schema));

        assertEquals("Cannot compare operands for ==: left type INT32, right type INT64",
                error.getMessage());
    }

    @Test
    void arithmeticSupportsNumericTypesAndRejectsOtherTypes() {
        Schema schema = Schema.from(List.of());
        for (Type.Scalar type : List.of(Type.Scalar.INT32, Type.Scalar.INT64, Type.Scalar.FLOAT64)) {
            Expr literal = switch (type) {
                case INT32 -> new Expr.Literal.Int32(1);
                case INT64 -> new Expr.Literal.Int64(1L);
                case FLOAT64 -> new Expr.Literal.Float64(1.0);
                default -> throw new AssertionError(type);
            };
            assertEquals(type,
                    new Expr.BinaryExpr(Expr.BinaryOp.Plus, literal, literal).outputType(schema));
        }

        Expr.TypeMismatchException error = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new Expr.BinaryExpr(
                        Expr.BinaryOp.Plus,
                        new Expr.Literal.Str("a"),
                        new Expr.Literal.Str("b")).outputType(schema));
        assertEquals("Cannot apply + to operands: left type UTF8, right type UTF8",
                error.getMessage());
    }

    @Test
    void unaryAndLogicalOperatorsRequireBooleanOperands() {
        Schema schema = Schema.from(List.of());
        Expr bool = Expr.Literal.Bool.True;

        assertEquals(Type.Scalar.BOOLEAN,
                new Expr.UnaryExpr(Expr.UnaryOp.NOT, bool).outputType(schema));
        assertEquals(Type.Scalar.BOOLEAN,
                new Expr.LogicalExpr(Expr.LogicalOp.AND, bool, bool).outputType(schema));

        Expr.TypeMismatchException unaryError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new Expr.UnaryExpr(Expr.UnaryOp.NOT, new Expr.Literal.Int32(1))
                        .outputType(schema));
        assertEquals("Cannot apply NOT: operand type INT32, expected BOOLEAN", unaryError.getMessage());

        Expr.TypeMismatchException logicalError = assertThrows(
                Expr.TypeMismatchException.class,
                () -> new Expr.LogicalExpr(
                        Expr.LogicalOp.OR, bool, new Expr.Literal.Int32(1)).outputType(schema));
        assertEquals("Cannot apply OR: left type BOOLEAN, right type INT32, expected BOOLEAN operands",
                logicalError.getMessage());
    }

    @Test
    void nestedExpressionsCollectDistinctReferences() {
        Expr expression = new Expr.LogicalExpr(
                Expr.LogicalOp.AND,
                new Expr.BinaryExpr(
                        Expr.BinaryOp.Greater,
                        new Expr.ColumnRef("fare_amount"),
                        new Expr.Literal.Int32(5)),
                new Expr.BinaryExpr(
                        Expr.BinaryOp.Less,
                        new Expr.ColumnRef("fare_amount"),
                        new Expr.ColumnRef("trip_distance")));
        Set<String> references = new LinkedHashSet<>();

        expression.collectReferences(references);

        assertEquals(List.of("fare_amount", "trip_distance"), List.copyOf(references));
    }

    @Test
    void missingColumnReportsItsName() {
        Expr.ColumnRef column = new Expr.ColumnRef("missing");
        Schema schema = Schema.from(List.of());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> column.outputType(schema));

        assertInstanceOf(IllegalArgumentException.class, error);
        assertEquals("Field not found: missing in schema: []", error.getMessage());
    }
}
