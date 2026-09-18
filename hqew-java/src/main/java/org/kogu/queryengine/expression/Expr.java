package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Schema;

import java.util.Set;

public sealed interface Expr permits ColumnRef, BinaryExpr, Literal, LogicalExpr, UnaryExpr {
    /// The type this expression produces once column names are looked up in the schema.
    /// Throws if a name is missing or a type combination is illegal.
    ExprType outputType(Schema schema);

    /// Adds every column name this expression reads into out.
    void collectReferences(Set<String> out);

    enum UnaryOp {
        NOT
    }

    sealed interface BinaryOp permits ArithmeticOp, ComparisonOp {
        String token();
    }

    enum LogicalOp {AND, OR}

    final class TypeMismatchException extends IllegalArgumentException {
        TypeMismatchException(String message) {
            super(message);
        }
    }
}
