package org.kogu.queryengine.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class SchemaTest {

    @Test
    void findsFieldsAndProjectsInRequestedOrderWithoutDuplicates() {
        Field id = new Field("id", Type.Scalar.INT64, false);
        Field name = new Field("name", Type.Scalar.UTF8, true);
        Field active = new Field("active", Type.Scalar.BOOLEAN, false);
        Schema schema = Schema.from(List.of(id, name, active));

        Schema projection = schema.project("active", "id", "active");

        assertEquals(active, projection.field("active"));
        assertEquals(id, projection.field("id"));
        assertThrows(IllegalArgumentException.class, () -> projection.field("name"));
    }

    @Test
    void rejectsDuplicateFieldNames() {
        Field first = new Field("id", Type.Scalar.INT32, false);
        Field second = new Field("id", Type.Scalar.INT64, false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Schema.from(List.of(first, second)));

        assertEquals("Duplicate field: id", error.getMessage());
    }

    @Test
    void reportsMissingFieldName() {
        Schema schema = Schema.from(List.of(new Field("id", Type.Scalar.INT64, false)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> schema.field("missing"));

        assertEquals("Field not found: missing in schema: [id: INT64, nullable:false]", error.getMessage());
    }
}
