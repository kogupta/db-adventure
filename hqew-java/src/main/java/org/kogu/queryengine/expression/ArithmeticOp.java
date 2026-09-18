package org.kogu.queryengine.expression;

public enum ArithmeticOp implements Expr.BinaryOp {
    Subtract("-") {
        @Override
        public int apply(int a, int b) {return a - b;}

        @Override
        public long apply(long a, long b) {return a - b;}

        @Override
        public double apply(double a, double b) {return a - b;}
    },
    Add("+") {
        @Override
        public int apply(int a, int b) {return a + b;}

        @Override
        public long apply(long a, long b) {return a + b;}

        @Override
        public double apply(double a, double b) {return a + b;}
    },
    Divide("/") {
        @Override
        public int apply(int a, int b) {return a / b;}

        @Override
        public long apply(long a, long b) {return a / b;}

        @Override
        public double apply(double a, double b) {return a / b;}
    },
    Multiply("*") {
        @Override
        public int apply(int a, int b) {return a * b;}

        @Override
        public long apply(long a, long b) {return a * b;}

        @Override
        public double apply(double a, double b) {return a * b;}
    };

    public final String token;

    ArithmeticOp(String token) {this.token = token;}

    @Override
    public String token() {return token;}

    public abstract int apply(int a, int b);
    public abstract long apply(long a, long b);
    public abstract double apply(double a, double b);
}
