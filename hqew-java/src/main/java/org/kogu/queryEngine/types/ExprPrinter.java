package org.kogu.queryEngine.types;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.kogu.queryEngine.types.Expr.*;
import org.kogu.queryEngine.types.Type.Scalar;

import static org.kogu.queryEngine.types.ComparisionOps.GT;
import static org.kogu.queryEngine.types.Exprs.*;

public final class ExprPrinter {
    private ExprPrinter() {}

    /// Renders an expression tree in ASCII box-drawing format without type resolution.
    public static String printTree(Expr expr) {
        return printTree(expr, null);
    }

    /// Renders an expression tree in ASCII box-drawing format, resolving output types
    /// against the provided schema if non-null.
    public static String printTree(Expr expr, @Nullable Schema schema) {
        Objects.requireNonNull(expr, "expr");
        StringBuilder sb = new StringBuilder();
        renderTree(expr, schema, "", "", sb);
        return sb.toString().stripTrailing();
    }

    /// Renders an expression in infix / SQL-like string representation.
    public static String toInfix(Expr expr) {
        Objects.requireNonNull(expr, "expr");
        return switch (expr) {
            case ColumnRef(var name) -> name;
            case Literal.Int32(var val) -> String.valueOf(val);
            case Literal.Int64(var val) -> val + "L";
            case Literal.Float64(var val) -> String.valueOf(val);
            case Literal.Str(var val) -> "'" + val + "'";
            case Literal.Bool b -> Boolean.toString(b.value);
            case BinaryExpr(var left, var op, var right) ->
                    "(" + toInfix(left) + " " + op.token() + " " + toInfix(right) + ")";
            case UnaryExpr(var op, var e) ->
                    "(" + op.name() + " " + toInfix(e) + ")";
            case LogicalExpr(var left, var op, var right) ->
                    "(" + toInfix(left) + " " + op.name() + " " + toInfix(right) + ")";
        };
    }

    private static void renderTree(Expr expr, @Nullable Schema schema, String prefix,
                                   String childPrefix, StringBuilder sb) {
        sb.append(prefix).append(formatNode(expr, schema)).append(System.lineSeparator());

        List<Expr> children = getChildren(expr);
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = (i == children.size() - 1);
            String nextPrefix = childPrefix + (isLast ? "└── " : "├── ");
            String nextChildPrefix = childPrefix + (isLast ? "    " : "│   ");
            renderTree(children.get(i), schema, nextPrefix, nextChildPrefix, sb);
        }
    }

    private static String formatNode(Expr expr, @Nullable Schema schema) {
        String base = switch (expr) {
            case ColumnRef(var name) -> "ColumnRef(" + name + ")";
            case Literal.Int32(var val) -> "Literal(" + val + ")";
            case Literal.Int64(var val) -> "Literal(" + val + "L)";
            case Literal.Float64(var val) -> "Literal(" + val + ")";
            case Literal.Str(var val) -> "Literal('" + val + "')";
            case Literal.Bool b -> "Literal(" + b.value + ")";
            case BinaryExpr(_, var op, _) -> "BinaryExpr(" + op.token() + ")";
            case UnaryExpr(var op, _) -> "UnaryExpr(" + op.name() + ")";
            case LogicalExpr(_, var op, _) -> "LogicalExpr(" + op.name() + ")";
        };

        if (schema != null) {
            try {
                Type.Scalar type = expr.outputType(schema);
                return base + " -> " + type;
            } catch (Exception e) {
                return base + " -> [type error: " + e.getMessage() + "]";
            }
        }
        return base;
    }

    private static List<Expr> getChildren(Expr expr) {
        return switch (expr) {
            case ColumnRef _, Literal _ -> List.of();
            case BinaryExpr(var left, _, var right) -> List.of(left, right);
            case UnaryExpr(_, var e) -> List.of(e);
            case LogicalExpr(var left, _, var right) -> List.of(left, right);
        };
    }

    static void main() {
        Expr e = logical(
                binary(col("trip_distance"), GT, ofInt(5)),
                LogicalOp.AND,
                binary(col("fare_amount"), GT, ofInt(20)));

        System.out.println(printTree(e));

        BitSet distNulls = new BitSet();
        distNulls.set(2);
        distNulls.set(7);

        BitSet fareNulls = new BitSet();
        fareNulls.set(5);

        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Scalar.INT32),
                new Field("fare_amount", Scalar.INT32)
        ));

        RecordBatch batch = new RecordBatch(schema, new Vector[]{
                Vectors.intVector(new int[]{3, 8, 999, 5, 12, 1, 6, 999}, distNulls),
                Vectors.intVector(new int[]{15, 25, 30, 5, 50, 999, 40, 20}, fareNulls)
        });

        ExprTracer.TraceReport report = ExprTracer.trace(e, batch);
        System.out.println(report);
    }
}
