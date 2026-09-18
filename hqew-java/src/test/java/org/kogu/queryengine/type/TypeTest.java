package org.kogu.queryengine.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class TypeTest {

    @Test
    void exposesTheSupportedScalarTypes() {
        assertArrayEquals(
                new Type.Scalar[]{
                        Type.Scalar.BOOLEAN,
                        Type.Scalar.INT32,
                        Type.Scalar.INT64,
                        Type.Scalar.FLOAT64,
                        Type.Scalar.UTF8},
                Type.Scalar.values());
    }
}
