package org.kogu.queryEngine.types;

public sealed interface Type {
    enum Scalar implements Type {
        BOOLEAN, INT32, INT64, FLOAT64, UTF8;
    }
}

