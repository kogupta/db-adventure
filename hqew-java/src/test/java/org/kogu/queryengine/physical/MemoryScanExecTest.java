package org.kogu.queryengine.physical;

import org.junit.jupiter.api.Test;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.columnar.Vectors;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryScanExecTest {

    private static final Schema TEST_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false)
    ));

    private static final Schema MULTI_COLUMN_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false),
            new Field("name", Type.Scalar.UTF8, false)
    ));

    // -------------------------------------------------------------------------
    // 1. Zero input batches
    // -------------------------------------------------------------------------

    @Test
    void zeroInputBatchesWithDefaultConstructorExhaustsImmediately() {
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA);

        assertSame(TEST_SCHEMA, exec.schema());
        assertTrue(exec.next().isEmpty());
    }

    @Test
    void zeroInputBatchesWithEmptyListExhaustsImmediately() {
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of());

        assertSame(TEST_SCHEMA, exec.schema());
        assertTrue(exec.next().isEmpty());
    }

    // -------------------------------------------------------------------------
    // 2. One input batch
    // -------------------------------------------------------------------------

    @Test
    void oneInputBatchReturnsBatchThenExhausts() {
        RecordBatch batch = createBatch(TEST_SCHEMA, new int[]{1, 2, 3});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch));

        Optional<RecordBatch> first = exec.next();
        assertTrue(first.isPresent());
        assertSame(batch, first.get());
        assertEquals(3, first.get().rowCount());

        Optional<RecordBatch> second = exec.next();
        assertTrue(second.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 3. Many input batches
    // -------------------------------------------------------------------------

    @Test
    void manyInputBatchesReturnedInStrictOrder() {
        RecordBatch batch1 = createBatch(TEST_SCHEMA, new int[]{10, 20});
        RecordBatch batch2 = createBatch(TEST_SCHEMA, new int[]{30, 40, 50});
        RecordBatch batch3 = createBatch(TEST_SCHEMA, new int[]{60});

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch1, batch2, batch3));

        Optional<RecordBatch> out1 = exec.next();
        assertTrue(out1.isPresent());
        assertSame(batch1, out1.get());
        assertEquals(2, out1.get().rowCount());

        Optional<RecordBatch> out2 = exec.next();
        assertTrue(out2.isPresent());
        assertSame(batch2, out2.get());
        assertEquals(3, out2.get().rowCount());

        Optional<RecordBatch> out3 = exec.next();
        assertTrue(out3.isPresent());
        assertSame(batch3, out3.get());
        assertEquals(1, out3.get().rowCount());

        Optional<RecordBatch> out4 = exec.next();
        assertTrue(out4.isEmpty());
    }

    @Test
    void manyInputBatchesMultiColumnPreservesVectorsAndOrder() {
        RecordBatch batch1 = createMultiColumnBatch(
                MULTI_COLUMN_SCHEMA,
                new int[]{1, 2},
                new String[]{"alice", "bob"}
        );
        RecordBatch batch2 = createMultiColumnBatch(
                MULTI_COLUMN_SCHEMA,
                new int[]{3},
                new String[]{"carol"}
        );

        MemoryScanExec exec = new MemoryScanExec(MULTI_COLUMN_SCHEMA, List.of(batch1, batch2));

        Optional<RecordBatch> out1 = exec.next();
        assertTrue(out1.isPresent());
        assertSame(batch1, out1.get());

        Optional<RecordBatch> out2 = exec.next();
        assertTrue(out2.isPresent());
        assertSame(batch2, out2.get());

        assertTrue(exec.next().isEmpty());
    }

    // -------------------------------------------------------------------------
    // 4. Stable schema
    // -------------------------------------------------------------------------

    @Test
    void schemaIsAvailableBeforeFirstNextCall() {
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA);
        assertSame(TEST_SCHEMA, exec.schema());

        RecordBatch batch = createBatch(TEST_SCHEMA, new int[]{42});
        MemoryScanExec execWithBatches = new MemoryScanExec(TEST_SCHEMA, List.of(batch));
        assertSame(TEST_SCHEMA, execWithBatches.schema());
    }

    @Test
    void schemaRemainsStableAcrossEntireExecutionLifecycle() {
        RecordBatch batch1 = createBatch(TEST_SCHEMA, new int[]{1});
        RecordBatch batch2 = createBatch(TEST_SCHEMA, new int[]{2, 3});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch1, batch2));

        // Schema before first next()
        assertSame(TEST_SCHEMA, exec.schema());

        // Schema after pulling first batch
        Optional<RecordBatch> out1 = exec.next();
        assertTrue(out1.isPresent());
        assertSame(TEST_SCHEMA, exec.schema());
        assertSame(TEST_SCHEMA, out1.get().schema());

        // Schema after pulling second batch
        Optional<RecordBatch> out2 = exec.next();
        assertTrue(out2.isPresent());
        assertSame(TEST_SCHEMA, exec.schema());
        assertSame(TEST_SCHEMA, out2.get().schema());

        // Schema after exhaustion
        Optional<RecordBatch> out3 = exec.next();
        assertTrue(out3.isEmpty());
        assertSame(TEST_SCHEMA, exec.schema());

        // Schema on repeated calls after exhaustion
        assertSame(TEST_SCHEMA, exec.schema());
    }

    // -------------------------------------------------------------------------
    // 5. Permanent exhaustion
    // -------------------------------------------------------------------------

    @Test
    void permanentExhaustionWhenZeroBatches() {
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of());

        assertTrue(exec.next().isEmpty());

        // Repeated subsequent calls stay permanently exhausted
        for (int i = 0; i < 5; i++) {
            assertTrue(exec.next().isEmpty(), "Call " + i + " after exhaustion must return empty");
        }
    }

    @Test
    void permanentExhaustionAfterSingleBatch() {
        RecordBatch batch = createBatch(TEST_SCHEMA, new int[]{100});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch));

        assertTrue(exec.next().isPresent());
        assertTrue(exec.next().isEmpty());

        // Repeated subsequent calls stay permanently exhausted
        for (int i = 0; i < 5; i++) {
            assertTrue(exec.next().isEmpty(), "Call " + i + " after exhaustion must return empty");
        }
    }

    @Test
    void permanentExhaustionAfterManyBatches() {
        RecordBatch b1 = createBatch(TEST_SCHEMA, new int[]{1});
        RecordBatch b2 = createBatch(TEST_SCHEMA, new int[]{2});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(b1, b2));

        assertTrue(exec.next().isPresent());
        assertTrue(exec.next().isPresent());
        assertTrue(exec.next().isEmpty());

        // Repeated subsequent calls stay permanently exhausted
        for (int i = 0; i < 5; i++) {
            assertTrue(exec.next().isEmpty(), "Call " + i + " after exhaustion must return empty");
        }
    }

    // -------------------------------------------------------------------------
    // 6. No zero-row batches crossing next()
    // -------------------------------------------------------------------------

    @Test
    void skipsInterleavedZeroRowBatches() {
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow3 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch valid1 = createBatch(TEST_SCHEMA, new int[]{10, 20});
        RecordBatch valid2 = createBatch(TEST_SCHEMA, new int[]{30});

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(
                zeroRow1,
                valid1,
                zeroRow2,
                zeroRow3,
                valid2,
                createBatch(TEST_SCHEMA, new int[0])
        ));

        // First non-empty batch returned; zeroRow1 was skipped
        Optional<RecordBatch> out1 = exec.next();
        assertTrue(out1.isPresent());
        assertSame(valid1, out1.get());
        assertTrue(out1.get().rowCount() > 0);

        // Second non-empty batch returned; zeroRow2 and zeroRow3 were skipped
        Optional<RecordBatch> out2 = exec.next();
        assertTrue(out2.isPresent());
        assertSame(valid2, out2.get());
        assertTrue(out2.get().rowCount() > 0);

        // Trailing zero-row batch is skipped; reaches EOF
        Optional<RecordBatch> out3 = exec.next();
        assertTrue(out3.isEmpty());

        // Remains exhausted
        assertTrue(exec.next().isEmpty());
    }

    @Test
    void returnsEmptyWhenOnlyZeroRowBatchesProvided() {
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow3 = createBatch(TEST_SCHEMA, new int[0]);

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(zeroRow1, zeroRow2, zeroRow3));

        // All zero-row batches skipped; immediately exhausted
        Optional<RecordBatch> out = exec.next();
        assertTrue(out.isEmpty());

        // Permanent exhaustion holds
        for (int i = 0; i < 5; i++) {
            assertTrue(exec.next().isEmpty());
        }
    }

    @Test
    void skipsLeadingZeroRowBatches() {
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch valid = createBatch(TEST_SCHEMA, new int[]{42});

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(zeroRow1, zeroRow2, valid));

        Optional<RecordBatch> out1 = exec.next();
        assertTrue(out1.isPresent());
        assertSame(valid, out1.get());

        assertTrue(exec.next().isEmpty());
    }

    @Test
    void skipsTrailingZeroRowBatches() {
        RecordBatch valid = createBatch(TEST_SCHEMA, new int[]{42});
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(valid, zeroRow1, zeroRow2));

        Optional<RecordBatch> out1 = exec.next();
        assertTrue(out1.isPresent());
        assertSame(valid, out1.get());

        assertTrue(exec.next().isEmpty());
        assertTrue(exec.next().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RecordBatch createBatch(Schema schema, int[] values) {
        return new RecordBatch(schema, new Vector[]{Vectors.intVector(values)});
    }

    private static RecordBatch createMultiColumnBatch(Schema schema, int[] ids, String[] names) {
        return new RecordBatch(schema, new Vector[]{
                Vectors.intVector(ids),
                Vectors.stringVector(names)
        });
    }
}
