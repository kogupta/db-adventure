package org.kogu.queryengine.type;

public sealed interface Type {
    enum Scalar implements Type {
        BOOLEAN, INT32, INT64, FLOAT64, UTF8;
    }
}

