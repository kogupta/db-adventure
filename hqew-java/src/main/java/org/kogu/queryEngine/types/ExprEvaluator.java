package org.kogu.queryEngine.types;

import org.kogu.queryEngine.types.Expr.Literal;

import java.util.BitSet;
import java.util.Objects;

import static org.kogu.queryEngine.types.Vector.*;

final class ExprEvaluator {
    private ExprEvaluator() {}

    public static Vector eval(Expr expr, RecordBatch batch) {
        return switch (expr) {
            case Expr.ColumnRef(var name) -> batch.vectorOf(name);
            case Literal.Int32(var n) -> new ConstantInt(n, batch.rowCount());
            case Literal.Int64(var n) -> new ConstantLong(n, batch.rowCount());
            case Literal.Float64(var d) -> new ConstantDouble(d, batch.rowCount());
            case Literal.Str(var s) -> new ConstantUtf8(s, batch.rowCount());
            case Literal.Bool bool -> new ConstantBool(bool.value, batch.rowCount());
            case Expr.BinaryExpr(var left, var op, var right) -> {
                Vector a = eval(left, batch);
                Vector b = eval(right, batch);
                switch (op) {
                    case Expr.ArithmeticOps o -> numeric(a, o, b);
                    case Expr.ComparisionOps o -> compare(a, o, b);
                }
            }
            case Expr.UnaryExpr(var op, Expr e) -> evalUnary(batch, op, e);
            case Expr.LogicalExpr(var left, var op, var right) -> {
                Vector a = eval(left, batch);
                Vector b = eval(right, batch);
                if (!(a instanceof BooleanVector l) || !(b instanceof BooleanVector r))
                    throw new Expr.TypeMismatchException("Logical requires boolean operands");

                yield evalLogical(batch, op, l, r);
            }
        };
    }

    private static BooleanVector evalLogical(RecordBatch batch, Expr.LogicalOp op, BooleanVector left,
                                             BooleanVector right) {
        boolean[] result = new boolean[batch.rowCount()];
        BitSet nulls = new BitSet(batch.rowCount());

        for (int i = 0; i < batch.rowCount(); i++) {
            if (left.isNotNull(i) && right.isNotNull(i))
                result[i] = switch (op) {
                    case AND -> left.value(i) && right.value(i);
                    case OR -> left.value(i) || right.value(i);
                };
            else if (left.isNotNull(i)) {
                evalBoolsNulls(op, left, i, result, nulls);
            } else if (right.isNotNull(i)) {
                // right is non null
                evalBoolsNulls(op, right, i, result, nulls);
            } else {
                nulls.set(i);
            }
        }

        return new BooleanVector(result, nulls);
    }

    private static void evalBoolsNulls(Expr.LogicalOp op, BooleanVector vector, int index, boolean[] result,
                                       BitSet nulls) {
        if (vector.value(index) && op == Expr.LogicalOp.OR)
            result[index] = true;
        else if (!vector.value(index) && op == Expr.LogicalOp.AND)
            result[index] = false;
        else
            nulls.set(index);
    }

    private static BooleanVector evalUnary(RecordBatch batch, Expr.UnaryOp op, Expr e) {
        Vector v = eval(e, batch);

        if (!(v instanceof BooleanVector bs) || op != Expr.UnaryOp.NOT) {
            throw new IllegalArgumentException("Unsupported unary operation");
        }

        boolean[] result = new boolean[batch.rowCount()];
        for (int i = 0; i < batch.rowCount(); i++) {
            if (!bs.isNull(i))
                result[i] = !bs.value(i);
        }
        BitSet nulls = bs.nullIndices().get(bs.offset(), bs.offset() + bs.length());
        return new BooleanVector(result, nulls);
    }

    static Vector numeric(Vector left, Expr.ArithmeticOps op, Vector right) {
        // TODO: allocate result vector
        //      iterate over both vectors using indices and apply op
        //      iterate by indices, should check `isNull`
    }

    static Vector compare(Vector left, Expr.ComparisionOps op, Vector right) {
        // TODO: allocate result vector
        //      iterate over both vectors using indices and apply op
        //      iterate by indices, should check `isNull`
    }

}
