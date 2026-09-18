package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.Set;

public record LogicalExpr(Expr left, LogicalOp op, Expr right) implements Expr {
    @Override
    public ExprType outputType(Schema schema) {
        ExprType leftType = left.outputType(schema);
        ExprType rightType = right.outputType(schema);

        if (leftType.type() == Type.Scalar.BOOLEAN && rightType.type() == Type.Scalar.BOOLEAN) {
            return new ExprType(Type.Scalar.BOOLEAN, leftType.nullable() || rightType.nullable());
        }

        throw new TypeMismatchException(
                "Cannot apply %s: left type %s, right type %s, expected BOOLEAN operands".formatted(
                        op, leftType.type(), rightType.type())
        );
    }

    @Override
    public void collectReferences(Set<String> out) {
        left.collectReferences(out);
        right.collectReferences(out);
    }
}
