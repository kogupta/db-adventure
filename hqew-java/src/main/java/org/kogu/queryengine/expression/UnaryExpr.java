package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.Set;

public record UnaryExpr(UnaryOp op, Expr e) implements Expr {
    @Override
    public ExprType outputType(Schema schema) {
        ExprType operandType = e.outputType(schema);
        if (operandType.type() == Type.Scalar.BOOLEAN) {
            return operandType;
        }

        throw new TypeMismatchException(
                "Cannot apply %s: operand type %s, expected BOOLEAN".formatted(op, operandType.type()));
    }

    @Override
    public void collectReferences(Set<String> out) {
        e.collectReferences(out);
    }
}
