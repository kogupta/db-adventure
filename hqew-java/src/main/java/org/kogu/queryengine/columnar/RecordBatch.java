package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Schema;

import java.util.List;

public final class RecordBatch {
    private final Schema schema;
    private final Vector[] vectors;
    private final int rowCount;

    public RecordBatch(Schema schema, Vector[] vectors) {
        if (vectors.length != schema.fieldCount()) {
            throw new IllegalArgumentException("vector count does not match schema field count");
        }

        for (int i = 0; i < vectors.length; i++) {
            if (!vectors[i].type().equals(schema.fieldAtIndex(i).type())) {
                throw new IllegalArgumentException(
                        "vector type does not match schema field at index " + i);
            }
        }

        for (int i = 1; i < vectors.length; i++) {
            if (vectors[i].length() != vectors[0].length()) {
                throw new IllegalArgumentException(
                        "vector lengths do not match at index " + i);
            }
        }

        this.schema = schema;
        this.vectors = vectors;
        this.rowCount = vectors.length == 0 ? 0 : vectors[0].length();
    }

    public Schema schema() {return schema;}

    public List<Vector> vectors() {return List.of(vectors);}

    public int rowCount() {return rowCount;}

    public Vector vectorOf(String name) {
        return vectors[schema.indexOfField(name)];
    }
}
