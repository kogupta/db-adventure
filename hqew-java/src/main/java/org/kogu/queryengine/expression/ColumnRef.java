package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;

import java.util.Set;

// leaf
public record ColumnRef(String name) implements Expr {
    @Override
    public ExprType outputType(Schema schema) {
        Field f = schema.field(name);
        return ExprType.of(f);
    }

    @Override
    public void collectReferences(Set<String> out) {
        out.add(name);
    }
}
