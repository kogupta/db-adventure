package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Type;

public record ExprType(Type type, boolean nullable) {
    public static ExprType of(Field f) {
        return new ExprType(f.type(), f.nullable());
    }

    public static ExprType of(Type.Scalar scalar) {
        return new ExprType(scalar, false);
    }
}
