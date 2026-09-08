package org.kogu.queryEngine.types;

import java.util.Objects;

public enum ComparisionOps implements Expr.BinaryOp {
    EqEq("==") {
        @Override public boolean compare(int left, int right) {return left == right;}

        @Override public boolean compare(long left, long right) {return left == right;}

        @Override public boolean compare(double left, double right) {return left == right;}

        @Override public boolean compare(boolean left, boolean right) {return left == right;}

        @Override public boolean compare(String left, String right) {return Objects.equals(left, right);}
    },
    NEq("!=") {
        @Override public boolean compare(int left, int right) {return left != right;}

        @Override public boolean compare(long left, long right) {return left != right;}

        @Override public boolean compare(double left, double right) {return left != right;}

        @Override public boolean compare(boolean left, boolean right) {return left != right;}

        @Override public boolean compare(String left, String right) {return !Objects.equals(left, right);}
    },
    LT("<") {
        @Override public boolean compare(int left, int right) {return left < right;}

        @Override public boolean compare(long left, long right) {return left < right;}

        @Override public boolean compare(double left, double right) {return left < right;}

        @Override
        public boolean compare(boolean left, boolean right) {
            throw new UnsupportedOperationException("compare(boolean, boolean) not supported for " + this);
        }

        @Override public boolean compare(String left, String right) {return left.compareTo(right) < 0;}
    },
    LTEq("<=") {
        @Override public boolean compare(int left, int right) {return left <= right;}

        @Override public boolean compare(long left, long right) {return left <= right;}

        @Override public boolean compare(double left, double right) {return left <= right;}

        @Override
        public boolean compare(boolean left, boolean right) {
            throw new UnsupportedOperationException("compare(boolean, boolean) not supported for " + this);
        }

        @Override public boolean compare(String left, String right) {return left.compareTo(right) <= 0;}
    },
    GT(">") {
        @Override public boolean compare(int left, int right) {return left > right;}

        @Override public boolean compare(long left, long right) {return left > right;}

        @Override public boolean compare(double left, double right) {return left > right;}

        @Override
        public boolean compare(boolean left, boolean right) {
            throw new UnsupportedOperationException("compare(boolean, boolean) not supported for " + this);
        }

        @Override public boolean compare(String left, String right) {return left.compareTo(right) > 0;}
    },
    GTEq(">=") {
        @Override public boolean compare(int left, int right) {return left >= right;}

        @Override public boolean compare(long left, long right) {return left >= right;}

        @Override public boolean compare(double left, double right) {return left >= right;}

        @Override
        public boolean compare(boolean left, boolean right) {
            throw new UnsupportedOperationException("compare(boolean, boolean) not supported for " + this);
        }

        @Override public boolean compare(String left, String right) {return left.compareTo(right) >= 0;}
    };

    public final String token;

    ComparisionOps(String token) {this.token = token;}

    @Override
    public String token() {return token;}

    public abstract boolean compare(int left, int right);

    public abstract boolean compare(long left, long right);

    public abstract boolean compare(double left, double right);

    public abstract boolean compare(boolean left, boolean right);

    public abstract boolean compare(String left, String right);
}
