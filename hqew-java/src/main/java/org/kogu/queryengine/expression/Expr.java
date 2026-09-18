package org.kogu.queryengine.expression;

import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.Set;

import static org.kogu.queryengine.type.Type.Scalar.*;

public sealed interface Expr {
    record ExprType(Type type, boolean nullable) {
        public static ExprType of(Field f) {
            return new ExprType(f.type(), f.nullable());
        }

        public static ExprType of(Type.Scalar scalar) {
            return new ExprType(scalar, false);
        }
    }

    /// The type this expression produces once column names are looked up in schema.
    /// Throws if a name is missing or a type combination is illegal.
    ExprType outputType(Schema schema);

    /// Adds every column name this expression reads into out.
    void collectReferences(Set<String> out);

    // leaf
    record ColumnRef(String name) implements Expr {
        @Override
        public ExprType outputType(Schema schema) {
            Field f = schema.field(name);
            return ExprType.of(f);
        }

        @Override
        public void collectReferences(Set<String> out) {out.add(name);}
    }

    // leaf
    sealed interface Literal extends Expr {
        Object value();
        Type.Scalar type();
        default ExprType outputType(Schema schema) {return ExprType.of(type());}
        default void collectReferences(Set<String> out) {}

        record Int32(int intVal) implements Literal {
            @Override
            public Object value() {return intVal;}

            @Override
            public Type.Scalar type() {return INT32;}
        }

        record Int64(long longVal) implements Literal {
            @Override
            public Object value() {return longVal;}

            @Override
            public Type.Scalar type() {return INT64;}
        }

        record Float64(double floatVal) implements Literal {
            @Override
            public Object value() {return floatVal;}

            @Override
            public Type.Scalar type() {return FLOAT64;}
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
        public ExprType outputType(Schema schema) {
            ExprType leftType = left.outputType(schema);
            ExprType rightType = right.outputType(schema);
            return switch (op) {
                case ComparisonOp _ -> {
                    if (leftType.type.equals(rightType.type)) {
                        boolean isNullable = leftType.nullable || rightType.nullable;
                        yield new ExprType(Scalar.BOOLEAN, isNullable);
                    }

                    throw new TypeMismatchException(
                            "Cannot compare operands for %s: left type %s, right type %s".formatted(
                                op.token(), leftType.type, rightType.type)
                    );
                }
                case ArithmeticOp _ -> {
                    if (leftType.type.equals(rightType.type)) {
                        switch (leftType.type) {
                            case INT32, INT64, FLOAT64 -> {
                                yield new ExprType(leftType.type, leftType.nullable || rightType.nullable);
                            }
                            case BOOLEAN, UTF8 -> {}
                        }
                    }
                    throw new TypeMismatchException(
                            "Cannot apply %s to operands: left type %s, right type %s".formatted(
                                    op.token(), leftType.type, rightType.type)
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

    record UnaryExpr(UnaryOp op, Expr e) implements Expr {
        @Override
        public ExprType outputType(Schema schema) {
            ExprType operandType = e.outputType(schema);
            if (operandType.type == Type.Scalar.BOOLEAN) {
                return operandType;
            }

            throw new TypeMismatchException(
                    "Cannot apply %s: operand type %s, expected BOOLEAN".formatted(op, operandType.type));
        }

        @Override
        public void collectReferences(Set<String> out) {
            e.collectReferences(out);
        }
    }

    record LogicalExpr(Expr left, LogicalOp op, Expr right) implements Expr {
        @Override
        public ExprType outputType(Schema schema) {
            ExprType leftType = left.outputType(schema);
            ExprType rightType = right.outputType(schema);
            if (leftType.type == Type.Scalar.BOOLEAN && rightType.type == Type.Scalar.BOOLEAN) {
                boolean isNullable = leftType.nullable || rightType.nullable;
                return new ExprType(Type.Scalar.BOOLEAN, isNullable);
            }

            throw new TypeMismatchException(
                    "Cannot apply %s: left type %s, right type %s, expected BOOLEAN operands".formatted(
                            op, leftType.type, rightType.type)
            );
        }

        @Override
        public void collectReferences(Set<String> out) {
            left.collectReferences(out);
            right.collectReferences(out);
        }
    }

    enum UnaryOp {NOT}

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
