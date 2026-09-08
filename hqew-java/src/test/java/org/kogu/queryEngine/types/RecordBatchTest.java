package org.kogu.queryEngine.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecordBatchTest {

    @Test
    void rejectsWrongVectorCount() {
        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Type.Scalar.INT32, false),
                new Field("fare_amount", Type.Scalar.INT32, false)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new RecordBatch(schema, new Vector[]{
                        Vectors.intVector(new int[]{6})}));

        assertEquals("vector count does not match schema field count", error.getMessage());
    }

    @Test
    void rejectsVectorTypeMismatch() {
        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Type.Scalar.INT64, false)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new RecordBatch(schema, new Vector[]{
                        Vectors.intVector(new int[]{6})}));

        assertEquals("vector type does not match schema field at index 0", error.getMessage());
    }

    @Test
    void rejectsUnequalVectorLengths() {
        Schema schema = Schema.from(List.of(
                new Field("trip_distance", Type.Scalar.INT32, false),
                new Field("fare_amount", Type.Scalar.INT32, false)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new RecordBatch(schema, new Vector[]{
                        Vectors.intVector(new int[]{6, 7}),
                        Vectors.intVector(new int[]{10})}));

        assertEquals("vector lengths do not match at index 1", error.getMessage());
    }

    @Test
    void happyPathFindsVectorBySchemaFieldName() {
        Schema schema = tripsSchema();
        Vector.IntVector fareAmount = Vectors.intVector(new int[]{10, 20, 30});
        RecordBatch batch = new RecordBatch(schema, new Vector[]{
                Vectors.intVector(new int[]{1, 2, 1}),
                Vectors.intVector(new int[]{6, 7, 8}), fareAmount});

        assertEquals(3, batch.rowCount());
        assertSame(fareAmount, batch.vectorOf("fare_amount"));
    }

    @Test
    void batchBoundariesDoNotChangeRowsReadInOrder() {
        List<Integer> expected = List.of(6, 7, 8, 9, 10, 11);
        List<RecordBatch> oneBatch = batches(6, 1);
        List<RecordBatch> twoBatches = batches(3, 2);
        List<RecordBatch> sixBatches = batches(1, 6);

        assertEquals(expected, readTripDistances(oneBatch));
        assertEquals(expected, readTripDistances(twoBatches));
        assertEquals(expected, readTripDistances(sixBatches));
        assertEquals(readTripDistances(oneBatch), readTripDistances(twoBatches));
        assertEquals(readTripDistances(oneBatch), readTripDistances(sixBatches));
    }

    private static Schema tripsSchema() {
        return Schema.from(List.of(
                new Field("passenger_count", Type.Scalar.INT32, false),
                new Field("trip_distance", Type.Scalar.INT32, false),
                new Field("fare_amount", Type.Scalar.INT32, false)));
    }

    private static List<RecordBatch> batches(int rowsPerBatch, int batchCount) {
        int[] passengers = {1, 2, 1, 3, 1, 2};
        int[] distances = {6, 7, 8, 9, 10, 11};
        int[] fares = {10, 20, 30, 40, 50, 60};
        List<RecordBatch> batches = new ArrayList<>();
        for (int batch = 0; batch < batchCount; batch++) {
            int offset = batch * rowsPerBatch;
            batches.add(new RecordBatch(tripsSchema(), new Vector[]{
                    Vectors.intVector(passengers, offset, rowsPerBatch),
                    Vectors.intVector(distances, offset, rowsPerBatch),
                    Vectors.intVector(fares, offset, rowsPerBatch)}));
        }
        return batches;
    }

    private static List<Integer> readTripDistances(List<RecordBatch> batches) {
        List<Integer> rows = new ArrayList<>();
        for (RecordBatch batch : batches) {
            Vector.IntVector distances = (Vector.IntVector) batch.vectorOf("trip_distance");
            for (int row = 0; row < batch.rowCount(); row++) {
                rows.add(distances.value(row));
            }
        }
        return rows;
    }
}
