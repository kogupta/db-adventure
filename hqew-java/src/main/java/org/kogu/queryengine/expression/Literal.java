package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.Set;

import static org.kogu.queryengine.type.Type.Scalar.*;

// leaf
sealed public interface Literal extends Expr {
    Object value();

    Type.Scalar type();

    default ExprType outputType(Schema schema) {return ExprType.of(type());}

    default void collectReferences(Set<String> out) {}

    record Int32(int intVal) implements org.kogu.queryengine.expression.Literal {
        @Override
        public Object value() {return intVal;}

        @Override
        public Type.Scalar type() {return INT32;}
    }

    record Int64(long longVal) implements org.kogu.queryengine.expression.Literal {
        @Override
        public Object value() {return longVal;}

        @Override
        public Type.Scalar type() {return INT64;}
    }

    record Float64(double floatVal) implements org.kogu.queryengine.expression.Literal {
        @Override
        public Object value() {return floatVal;}

        @Override
        public Type.Scalar type() {return FLOAT64;}
    }

    record Str(String value) implements org.kogu.queryengine.expression.Literal {
        @Override
        public Scalar type() {return Scalar.UTF8;}
    }

    enum Bool implements org.kogu.queryengine.expression.Literal {
        True(true), False(false);

        public final boolean value;

        Bool(boolean value) {this.value = value;}

        @Override
        public Object value() {return value;}

        @Override
        public Scalar type() {return Scalar.BOOLEAN;}
    }
}
