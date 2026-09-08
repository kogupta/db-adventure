package org.kogu.queryEngine.types;

import java.util.Set;

sealed interface Expr {
    /// The type this expression produces once column names are looked up in schema.
    /// Throws if a name is missing or a type combination is illegal.
    Type.Scalar outputType(Schema schema);

    /// Adds every column name this expression reads into out.
    void collectReferences(Set<String> out);

    // leaf
    record ColumnRef(String name) implements Expr {
        @Override
        public Type.Scalar outputType(Schema schema) {
            Field f = schema.field(name);
            return (Type.Scalar) f.type();
        }

        @Override
        public void collectReferences(Set<String> out) {out.add(name);}
    }

    // leaf
    sealed interface Literal extends Expr {
        Object value();
        Type.Scalar type();
        default Type.Scalar outputType(Schema schema) {return type();}
        default void collectReferences(Set<String> out) {}

        record Int32(int intVal) implements Literal {
            @Override
            public Object value() {return intVal;}

            @Override
            public Type.Scalar type() {return Type.Scalar.INT32;}
        }

        record Int64(long longVal) implements Literal {
            @Override
            public Object value() {return longVal;}

            @Override
            public Type.Scalar type() {return Type.Scalar.INT64;}
        }

        record Float64(double floatVal) implements Literal {
            @Override
            public Object value() {return floatVal;}

            @Override
            public Type.Scalar type() {return Type.Scalar.FLOAT64;}
        }

        record Str(String value) implements Literal {
            @Override
            public Type.Scalar type() {return Type.Scalar.UTF8;}
        }

        enum Bool implements Literal {
            True(true), False(false);

            public final boolean value;

            Bool(boolean value) {this.value = value;}

            @Override
            public Object value() {return value;}

            @Override
            public Type.Scalar type() {return Type.Scalar.BOOLEAN;}
        }
    }

    record BinaryExpr(Expr left, BinaryOp op, Expr right) implements Expr {
        @Override
        public Type.Scalar outputType(Schema schema) {
            Type.Scalar leftType = left.outputType(schema);
            Type.Scalar rightType = right.outputType(schema);
            return switch (op) {
                case ComparisionOps _ -> {
                    if (leftType != rightType)
                        throw new TypeMismatchException(
                            "Cannot compare operands for %s: left type %s, right type %s"
                                    .formatted(op.token(), leftType, rightType));
                    yield Type.Scalar.BOOLEAN;
                }
                case ArithmeticOps _ -> {
                    if (leftType == rightType) {
                        switch (leftType) {
                            case INT32, INT64, FLOAT64 -> {yield leftType;}
                            case BOOLEAN, UTF8 -> {}
                        }
                    }
                    throw new TypeMismatchException(
                            "Cannot apply %s to operands: left type %s, right type %s"
                                    .formatted(op.token(), leftType, rightType));
                }
            };
        }

        @Override
        public void collectReferences(Set<String> out) {
            left.collectReferences(out);
            right.collectReferences(out);
        }
    }

    record UnaryExpr(UnaryOp op, Expr e) implements Expr {
        @Override
        public Type.Scalar outputType(Schema schema) {
            Type.Scalar operandType = e.outputType(schema);
            if (operandType == Type.Scalar.BOOLEAN) {
                return Type.Scalar.BOOLEAN;
            }

            throw new TypeMismatchException(
                    "Cannot apply %s: operand type %s, expected BOOLEAN"
                            .formatted(op, operandType));
        }

        @Override
        public void collectReferences(Set<String> out) {
            e.collectReferences(out);
        }
    }

    record LogicalExpr(Expr left, LogicalOp op, Expr right) implements Expr {
        @Override
        public Type.Scalar outputType(Schema schema) {
            Type.Scalar leftType = left.outputType(schema);
            Type.Scalar rightType = right.outputType(schema);
            if (leftType == Type.Scalar.BOOLEAN && rightType == Type.Scalar.BOOLEAN) {
                return Type.Scalar.BOOLEAN;
            }

            throw new TypeMismatchException(
                    "Cannot apply %s: left type %s, right type %s, expected BOOLEAN operands"
                            .formatted(op, leftType, rightType));
        }

        @Override
        public void collectReferences(Set<String> out) {
            left.collectReferences(out);
            right.collectReferences(out);
        }
    }

    enum UnaryOp {NOT}

    sealed interface BinaryOp {
        String token();
    }

    enum ArithmeticOps implements BinaryOp {
        Subtract("-"),
        Add("+"),
        Divide("/"),
        Multiply("*");

        public final String token;
        ArithmeticOps(String token) {this.token = token;}
        @Override public String token() {return token;}
    }

    enum ComparisionOps implements BinaryOp {
        EqEq("=="),
        NEq("!="),
        LT("<"),
        LTEq("<="),
        GT(">"),
        GTEq(">=");

        public final String token;
        ComparisionOps(String token) {this.token = token;}
        @Override public String token() {return token;}
    }

    enum LogicalOp {AND, OR}

    final class TypeMismatchException extends IllegalArgumentException {
        TypeMismatchException(String message) {
            super(message);
        }
    }
}
