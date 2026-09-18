package org.kogu.queryengine.physical;

import org.kogu.queryengine.expression.ColumnRef;
import org.kogu.queryengine.expression.Expr;

public record Projection(String name, Expr expression) {
    public static Projection identity(String name) {
        return new Projection(name, new ColumnRef(name));
    }

    public static Projection alias(String oldName, String alias) {
        return new Projection(alias, new ColumnRef(oldName));
    }
}
