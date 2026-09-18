package org.kogu.queryengine.physical;

import org.junit.jupiter.api.Test;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.columnar.Vectors;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryScanExecTest {

    private static final Schema TEST_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false)
    ));

    private static final Schema MULTI_COLUMN_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false),
            new Field("name", Type.Scalar.UTF8, false)
    ));

    @Test
    void zeroInputBatchesWithDefaultConstructorExhaustsImmediately() {
        MemoryScanExec exec = MemoryScanExec.of(TEST_SCHEMA);

        assertSame(TEST_SCHEMA, exec.schema());
        assertNull(exec.next());
    }

    @Test
    void zeroInputBatchesWithEmptyListExhaustsImmediately() {
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of());

        assertSame(TEST_SCHEMA, exec.schema());
        assertNull(exec.next());
    }

    @Test
    void oneInputBatchReturnsBatchThenExhausts() {
        RecordBatch batch = createBatch(TEST_SCHEMA, new int[]{1, 2, 3});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch));

        var first = exec.next();
        assertNotNull(first);
        assertSame(batch, first);
        assertEquals(3, first.rowCount());
        assertNull(exec.next());
    }

    @Test
    void manyInputBatchesReturnedInStrictOrder() {
        RecordBatch batch1 = createBatch(TEST_SCHEMA, new int[]{10, 20});
        RecordBatch batch2 = createBatch(TEST_SCHEMA, new int[]{30, 40, 50});
        RecordBatch batch3 = createBatch(TEST_SCHEMA, new int[]{60});

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch1, batch2, batch3));

        var out1 = exec.next();
        assertNotNull(out1);
        assertSame(batch1, out1);
        assertEquals(2, out1.rowCount());

        var out2 = exec.next();
        assertNotNull(out2);
        assertSame(batch2, out2);
        assertEquals(3, out2.rowCount());

        var out3 = exec.next();
        assertNotNull(out3);
        assertSame(batch3, out3);
        assertEquals(1, out3.rowCount());

        assertNull(exec.next());
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

        assertSame(batch1, exec.next());
        assertSame(batch2, exec.next());
        assertNull(exec.next());
    }

    @Test
    void schemaIsAvailableBeforeFirstNextCall() {
        MemoryScanExec exec = MemoryScanExec.of(TEST_SCHEMA);
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

        assertSame(TEST_SCHEMA, exec.schema());

        var out1 = exec.next();
        assertNotNull(out1);
        assertSame(TEST_SCHEMA, exec.schema());
        assertSame(TEST_SCHEMA, out1.schema());

        var out2 = exec.next();
        assertNotNull(out2);
        assertSame(TEST_SCHEMA, exec.schema());
        assertSame(TEST_SCHEMA, out2.schema());

        assertNull(exec.next());
        assertSame(TEST_SCHEMA, exec.schema());
        assertSame(TEST_SCHEMA, exec.schema());
    }

    @Test
    void permanentExhaustionWhenZeroBatches() {
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of());

        assertNull(exec.next());
        for (int i = 0; i < 5; i++) {
            assertNull(exec.next(), "Call " + i + " after exhaustion must return null");
        }
    }

    @Test
    void permanentExhaustionAfterSingleBatch() {
        RecordBatch batch = createBatch(TEST_SCHEMA, new int[]{100});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(batch));

        assertNotNull(exec.next());
        assertNull(exec.next());
        for (int i = 0; i < 5; i++) {
            assertNull(exec.next(), "Call " + i + " after exhaustion must return null");
        }
    }

    @Test
    void permanentExhaustionAfterManyBatches() {
        RecordBatch b1 = createBatch(TEST_SCHEMA, new int[]{1});
        RecordBatch b2 = createBatch(TEST_SCHEMA, new int[]{2});
        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(b1, b2));

        assertNotNull(exec.next());
        assertNotNull(exec.next());
        assertNull(exec.next());
        for (int i = 0; i < 5; i++) {
            assertNull(exec.next(), "Call " + i + " after exhaustion must return null");
        }
    }

    @Test
    void skipsInterleavedZeroRowBatches() {
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow3 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch valid1 = createBatch(TEST_SCHEMA, new int[]{10, 20});
        RecordBatch valid2 = createBatch(TEST_SCHEMA, new int[]{30});

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(
                zeroRow1, valid1, zeroRow2, zeroRow3, valid2,
                createBatch(TEST_SCHEMA, new int[0])));

        var out1 = exec.next();
        assertNotNull(out1);
        assertSame(valid1, out1);
        assertTrue(out1.rowCount() > 0);

        var out2 = exec.next();
        assertNotNull(out2);
        assertSame(valid2, out2);
        assertTrue(out2.rowCount() > 0);

        assertNull(exec.next());
        assertNull(exec.next());
    }

    @Test
    void returnsEmptyWhenOnlyZeroRowBatchesProvided() {
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow3 = createBatch(TEST_SCHEMA, new int[0]);

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(zeroRow1, zeroRow2, zeroRow3));

        assertNull(exec.next());
        for (int i = 0; i < 5; i++) {
            assertNull(exec.next());
        }
    }

    @Test
    void skipsLeadingZeroRowBatches() {
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch valid = createBatch(TEST_SCHEMA, new int[]{42});

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(zeroRow1, zeroRow2, valid));

        assertSame(valid, exec.next());
        assertNull(exec.next());
    }

    @Test
    void skipsTrailingZeroRowBatches() {
        RecordBatch valid = createBatch(TEST_SCHEMA, new int[]{42});
        RecordBatch zeroRow1 = createBatch(TEST_SCHEMA, new int[0]);
        RecordBatch zeroRow2 = createBatch(TEST_SCHEMA, new int[0]);

        MemoryScanExec exec = new MemoryScanExec(TEST_SCHEMA, List.of(valid, zeroRow1, zeroRow2));

        assertSame(valid, exec.next());
        assertNull(exec.next());
        assertNull(exec.next());
    }

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
