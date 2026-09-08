package org.kogu.queryEngine.types;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kogu.queryEngine.types.Expr.*;
import org.kogu.queryEngine.types.Type.Scalar;

import static org.junit.jupiter.api.Assertions.*;

class ExprPrinterTest {

    @Test
    @DisplayName("Renders infix representations accurately across all AST forms")
    void rendersInfixRepresentations() {
        assertEquals("trip_distance", ExprPrinter.toInfix(new ColumnRef("trip_distance")));
        assertEquals("42", ExprPrinter.toInfix(new Literal.Int32(42)));
        assertEquals("100L", ExprPrinter.toInfix(new Literal.Int64(100L)));
        assertEquals("3.14", ExprPrinter.toInfix(new Literal.Float64(3.14)));
        assertEquals("'hello'", ExprPrinter.toInfix(new Literal.Str("hello")));
        assertEquals("true", ExprPrinter.toInfix(Literal.Bool.True));
        assertEquals("false", ExprPrinter.toInfix(Literal.Bool.False));

        Expr arithmetic = new BinaryExpr(new ColumnRef("a"), ArithmeticOps.Add, new Literal.Int32(1));
        assertEquals("(a + 1)", ExprPrinter.toInfix(arithmetic));

        Expr comparison = new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5));
        assertEquals("(trip_distance > 5)", ExprPrinter.toInfix(comparison));

        Expr unary = new UnaryExpr(UnaryOp.NOT, new ColumnRef("c_bool"));
        assertEquals("(NOT c_bool)", ExprPrinter.toInfix(unary));

        Expr logical = new LogicalExpr(
                new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5)),
                LogicalOp.AND,
                new BinaryExpr(new ColumnRef("fare_amount"), ComparisionOps.GT, new Literal.Int32(20))
        );
        assertEquals("((trip_distance > 5) AND (fare_amount > 20))", ExprPrinter.toInfix(logical));
    }

    @Test
    @DisplayName("Renders ASCII box-drawing tree without schema")
    void rendersAsciiTreeWithoutSchema() {
        Expr expr = new LogicalExpr(
                new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5)),
                LogicalOp.AND,
                new BinaryExpr(new ColumnRef("fare_amount"), ComparisionOps.GT, new Literal.Int32(20))
        );

        String tree = ExprPrinter.printTree(expr);
        String expected =
                """
                LogicalExpr(AND)
                ├── BinaryExpr(>)
                │   ├── ColumnRef(trip_distance)
                │   └── Literal(5)
                └── BinaryExpr(>)
                    ├── ColumnRef(fare_amount)
                    └── Literal(20)"""
                .stripTrailing();

        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Renders ASCII box-drawing tree with schema-resolved types")
    void rendersAsciiTreeWithSchema() {
        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Scalar.INT32, true),
                new Field("fare_amount", Scalar.INT32, true)
        ));

        Expr expr = new LogicalExpr(
                new BinaryExpr(new ColumnRef("trip_distance"), ComparisionOps.GT, new Literal.Int32(5)),
                LogicalOp.AND,
                new BinaryExpr(new ColumnRef("fare_amount"), ComparisionOps.GT, new Literal.Int32(20))
        );

        String tree = ExprPrinter.printTree(expr, schema);
        String expected =
                """
                LogicalExpr(AND) -> BOOLEAN
                ├── BinaryExpr(>) -> BOOLEAN
                │   ├── ColumnRef(trip_distance) -> INT32
                │   └── Literal(5) -> INT32
                └── BinaryExpr(>) -> BOOLEAN
                    ├── ColumnRef(fare_amount) -> INT32
                    └── Literal(20) -> INT32"""
                .stripTrailing();

        assertEquals(expected, tree);
    }
}
