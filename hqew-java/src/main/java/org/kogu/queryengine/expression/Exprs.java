package org.kogu.queryengine.expression;

final class Exprs {
    private Exprs() {}

    public static Expr.Literal.Int32 ofInt(int value) {
        return new Expr.Literal.Int32(value);
    }

    public static Expr.ColumnRef col(String name) {
        return new Expr.ColumnRef(name);
    }

    public static Expr.BinaryExpr binary(Expr left, Expr.BinaryOp op, Expr right) {
        return new Expr.BinaryExpr(left, op, right);
    }

    public static Expr.LogicalExpr logical(Expr left, Expr.LogicalOp op, Expr right) {
        return new Expr.LogicalExpr(left, op, right);
    }
}
