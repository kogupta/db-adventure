package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.Set;

import static org.kogu.queryengine.type.Type.Scalar.*;

public record BinaryExpr(Expr left, BinaryOp op, Expr right) implements Expr {
    @Override
    public ExprType outputType(Schema schema) {
        ExprType leftType = left.outputType(schema);
        ExprType rightType = right.outputType(schema);
        boolean typeEquals = leftType.type().equals(rightType.type());
        return switch (op) {
            case ComparisonOp _ -> {
                if (typeEquals) {
                    boolean isNullable = leftType.nullable() || rightType.nullable();
                    yield new ExprType(Type.Scalar.BOOLEAN, isNullable);
                }

                throw new TypeMismatchException(
                        "Cannot compare operands for %s: left type %s, right type %s".formatted(
                                op.token(), leftType.type(), rightType.type())
                );
            }
            case ArithmeticOp _ -> {
                if (typeEquals) {
                    switch (leftType.type()) {
                        case INT32, INT64, FLOAT64 -> {
                            yield new ExprType(leftType.type(), leftType.nullable() || rightType.nullable());
                        }
                        case BOOLEAN, UTF8 -> {}
                    }
                }
                throw new TypeMismatchException(
                        "Cannot apply %s to operands: left type %s, right type %s".formatted(
                                op.token(), leftType.type(), rightType.type())
                );
            }
        };
    }

    @Override
    public void collectReferences(Set<String> out) {
        left.collectReferences(out);
        right.collectReferences(out);
    }
}
