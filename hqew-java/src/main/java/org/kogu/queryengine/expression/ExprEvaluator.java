package org.kogu.queryengine.expression;

import org.kogu.queryengine.expression.Expr.Literal;
import org.kogu.queryengine.expression.Expr.LogicalOp;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.columnar.Vectors;

import java.util.BitSet;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static org.kogu.queryengine.columnar.Vector.*;

public final class ExprEvaluator {
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
                yield switch (op) {
                    case ArithmeticOp o -> numeric(a, o, b);
                    case ComparisonOp o -> compare(a, o, b);
                };
            }
            case Expr.UnaryExpr(var op, Expr e) -> evalUnary(batch, op, e);
            case Expr.LogicalExpr(var left, var op, var right) -> {
                Vector a = eval(left, batch);
                Vector b = eval(right, batch);
                if (!(a instanceof BoolVec l) || !(b instanceof BoolVec r))
                    throw new Expr.TypeMismatchException("Logical requires boolean operands");

                yield evalLogical(batch, op, l, r);
            }
        };
    }

    private static BooleanVector evalLogical(RecordBatch batch, LogicalOp op, BoolVec left,
                                             BoolVec right) {
        boolean[] result = new boolean[batch.rowCount()];
        BitSet nulls = new BitSet(batch.rowCount());

        for (int i = 0; i < batch.rowCount(); i++) {
            if (left.isNotNull(i) && right.isNotNull(i))
                result[i] = switch (op) {
                    case AND -> left.value(i) && right.value(i);
                    case OR -> left.value(i) || right.value(i);
                };
            else if (left.isNotNull(i)) {
                evalBoolNulls(op, left, i, result, nulls);
            } else if (right.isNotNull(i)) {
                evalBoolNulls(op, right, i, result, nulls);
            } else {
                nulls.set(i);
            }
        }

        return Vectors.booleanVector(result, nulls);
    }

    private static void evalBoolNulls(LogicalOp op, BoolVec vector, int index,
                                      boolean[] result, BitSet nulls) {
        if (vector.value(index) && op == LogicalOp.OR)
            result[index] = true;
        else if (!vector.value(index) && op == LogicalOp.AND)
            result[index] = false;
        else
            nulls.set(index);
    }

    private static BooleanVector evalUnary(RecordBatch batch, Expr.UnaryOp op, Expr e) {
        Vector v = eval(e, batch);

        if (!(v instanceof BoolVec bs) || op != Expr.UnaryOp.NOT) {
            throw new IllegalArgumentException("Unsupported unary operation");
        }

        boolean[] result = new boolean[batch.rowCount()];
        BitSet nulls = new BitSet(batch.rowCount());
        for (int i = 0; i < batch.rowCount(); i++) {
            if (bs.isNotNull(i))
                result[i] = !bs.value(i);
            else
                nulls.set(i);
        }

        return Vectors.booleanVector(result, nulls);
    }

    static Vector numeric(Vector left, ArithmeticOp op, Vector right) {
        validateSameLength(left, right);
        return switch (left) {
            case IntVec l when right instanceof IntVec r -> numericInts(l, op, r);
            case LongVec l when right instanceof LongVec r -> numericLongs(l, op, r);
            case DoubleVec l when right instanceof DoubleVec r -> numericDoubles(l, op, r);
            default -> throw new Expr.TypeMismatchException(
                    "Cannot apply %s to operands: left type %s, right type %s"
                            .formatted(op.token(), left.type(), right.type()));
        };
    }

    static Vector compare(Vector left, ComparisonOp op, Vector right) {
        validateSameLength(left, right);
        return switch (left) {
            case IntVec l when right instanceof IntVec r -> compareInts(l, op, r);
            case LongVec l when right instanceof LongVec r -> compareLongs(l, op, r);
            case DoubleVec l when right instanceof DoubleVec r -> compareDoubles(l, op, r);
            case BoolVec l when right instanceof BoolVec r -> compareBools(l, op, r);
            case Utf8Vec l when right instanceof Utf8Vec r -> compareUtf8(l, op, r);
            default -> throw new Expr.TypeMismatchException(
                    "Cannot compare operands for %s: left type %s, right type %s"
                            .formatted(op.token(), left.type(), right.type()));
        };
    }

    /// ----- nearly identical methods ----
    private static IntVector numericInts(IntVec left, ArithmeticOp op, IntVec right) {
        int[] result = new int[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.apply(left.value(i), right.value(i)),
                left.length());
        return Vectors.intVector(result, nulls);
    }

    private static LongVector numericLongs(LongVec left, ArithmeticOp op, LongVec right) {
        long[] result = new long[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.apply(left.value(i), right.value(i)),
                left.length());
        return Vectors.longVector(result, nulls);
    }

    private static DoubleVector numericDoubles(DoubleVec left, ArithmeticOp op, DoubleVec right) {
        double[] result = new double[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.apply(left.value(i), right.value(i)),
                left.length());
        return Vectors.doubleVector(result, nulls);
    }

    /// Runs writeSlot at every slot where bothNonNull holds. Sets the null bit at the rest.
    /// Slots left unwritten keep the array default, which is unobservable behind the null bit.
    private static BitSet perSlot(IntPredicate bothNonNull, IntConsumer writeSlot, int length) {
        BitSet nulls = new BitSet(length);
        for (int i = 0; i < length; i++) {
            if (bothNonNull.test(i))
                writeSlot.accept(i);
            else
                nulls.set(i);
        }
        return nulls;
    }

    /// One kernel per element type. int[]/long[]/double[] share no supertype, and a common
    /// signature would box every slot, so the repetition stays.
    private static BooleanVector compareInts(IntVec left, ComparisonOp op, IntVec right) {
        boolean[] result = new boolean[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.compare(left.value(i), right.value(i)),
                left.length());
        return Vectors.booleanVector(result, nulls);
    }

    private static BooleanVector compareLongs(LongVec left, ComparisonOp op, LongVec right) {
        boolean[] result = new boolean[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.compare(left.value(i), right.value(i)),
                left.length());
        return Vectors.booleanVector(result, nulls);
    }

    private static BooleanVector compareDoubles(DoubleVec left, ComparisonOp op, DoubleVec right) {
        boolean[] result = new boolean[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.compare(left.value(i), right.value(i)),
                left.length());
        return Vectors.booleanVector(result, nulls);
    }

    private static BooleanVector compareBools(BoolVec left, ComparisonOp op, BoolVec right) {
        boolean[] result = new boolean[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.compare(left.value(i), right.value(i)),
                left.length());
        return Vectors.booleanVector(result, nulls);
    }

    private static BooleanVector compareUtf8(Utf8Vec left, ComparisonOp op, Utf8Vec right) {
        boolean[] result = new boolean[left.length()];
        BitSet nulls = perSlot(
                i -> left.isNotNull(i) && right.isNotNull(i),
                i -> result[i] = op.compare(left.value(i), right.value(i)),
                left.length());
        return Vectors.booleanVector(result, nulls);
    }

    private static void validateSameLength(Vector left, Vector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException("Vector lengths do not match");
    }

}
