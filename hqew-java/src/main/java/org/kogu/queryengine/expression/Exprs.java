package org.kogu.queryengine.expression;

final class Exprs {
    private Exprs() {}

    public static Literal.Int32 ofInt(int value) {
        return new Literal.Int32(value);
    }

    public static ColumnRef col(String name) {
        return new ColumnRef(name);
    }

    public static BinaryExpr binary(Expr left, Expr.BinaryOp op, Expr right) {
        return new BinaryExpr(left, op, right);
    }

    public static LogicalExpr logical(Expr left, Expr.LogicalOp op, Expr right) {
        return new LogicalExpr(left, op, right);
    }
}
