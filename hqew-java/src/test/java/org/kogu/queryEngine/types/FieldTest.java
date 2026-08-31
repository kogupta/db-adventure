package org.kogu.queryEngine.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldTest {

    @Test
    void exposesNameTypeAndNullability() {
        Field nullableName = new Field("name", Type.Scalar.UTF8, true);
        Field requiredId = new Field("id", Type.Scalar.INT64, false);

        assertEquals("name", nullableName.name());
        assertEquals(Type.Scalar.UTF8, nullableName.type());
        assertTrue(nullableName.nullable());
        assertEquals("id", requiredId.name());
        assertEquals(Type.Scalar.INT64, requiredId.type());
        assertFalse(requiredId.nullable());
    }

    @Test
    void comparesByComponentValues() {
        assertEquals(
                new Field("id", Type.Scalar.INT64, false),
                new Field("id", Type.Scalar.INT64, false));
    }
}
