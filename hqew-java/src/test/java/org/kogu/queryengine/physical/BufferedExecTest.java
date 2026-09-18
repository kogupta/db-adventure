package org.kogu.queryengine.physical;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.columnar.Vectors;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class BufferedExecTest {

    private static final Schema TEST_SCHEMA = Schema.from(List.of(
            new Field("id", Type.Scalar.INT32, false)
    ));

    @Test
    void schemaIsAvailableBeforeFirstNextCall() {
        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of());
        assertSame(TEST_SCHEMA, exec.schema());
        assertEquals(0, exec.pullCount());
    }

    private static List<@Nullable RecordBatch> responses(@Nullable RecordBatch... responses) {
        return Arrays.asList(responses);
    }

    @Test
    void zeroRowBatchFollowedByNonEmptyBatch() {
        RecordBatch zeroRowBatch = createBatch(new int[0]);
        RecordBatch nonEmptyBatch = createBatch(new int[]{42});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA,
                responses(zeroRowBatch, nonEmptyBatch, null));

        var result = exec.next();
        assertNotNull(result);
        assertSame(nonEmptyBatch, result);
        assertEquals(2, exec.pullCount());
    }

    @Test
    void exhaustionAfterFinalBatch() {
        RecordBatch finalBatch = createBatch(new int[]{1, 2, 3});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA,
                responses(finalBatch, null));

        var first = exec.next();
        assertNotNull(first);
        assertSame(finalBatch, first);

        assertNull(exec.next());
        assertEquals(2, exec.pullCount());
    }

    @Test
    void repeatedCallsAfterExhaustionDoNotCallPullAgain() {
        RecordBatch batch = createBatch(new int[]{1});
        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA,
                responses(batch, null));

        assertNotNull(exec.next());
        assertNull(exec.next());
        assertEquals(2, exec.pullCount());

        assertNull(exec.next());
        assertNull(exec.next());
        assertNull(exec.next());
        assertEquals(2, exec.pullCount());
    }

    @Test
    void exhaustedImmediatelyWhenFirstPullIsEmpty() {
        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, responses((RecordBatch) null));

        assertNull(exec.next());
        assertEquals(1, exec.pullCount());

        assertNull(exec.next());
        assertNull(exec.next());
        assertEquals(1, exec.pullCount());
    }

    @Test
    void returnsBatchesInOrderUntilExhausted() {
        RecordBatch batch1 = createBatch(new int[]{1, 2});
        RecordBatch batch2 = createBatch(new int[]{3, 4, 5});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA,
                responses(batch1, batch2, null));

        assertSame(batch1, exec.next());
        assertEquals(1, exec.pullCount());
        assertSame(batch2, exec.next());
        assertEquals(2, exec.pullCount());
        assertNull(exec.next());
        assertEquals(3, exec.pullCount());

        assertNull(exec.next());
        assertEquals(3, exec.pullCount());
    }

    @Test
    void skipsZeroRowBatches() {
        RecordBatch zeroRowBatch1 = createBatch(new int[0]);
        RecordBatch zeroRowBatch2 = createBatch(new int[0]);
        RecordBatch validBatch1 = createBatch(new int[]{10});
        RecordBatch validBatch2 = createBatch(new int[]{20, 30});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA,
                responses(zeroRowBatch1, validBatch1, zeroRowBatch2, validBatch2, null));

        assertSame(validBatch1, exec.next());
        assertEquals(2, exec.pullCount());
        assertSame(validBatch2, exec.next());
        assertEquals(4, exec.pullCount());
        assertNull(exec.next());
        assertEquals(5, exec.pullCount());

        assertNull(exec.next());
        assertEquals(5, exec.pullCount());
    }

    private static RecordBatch createBatch(int[] values) {
        return new RecordBatch(TEST_SCHEMA, new Vector[]{Vectors.intVector(values)});
    }

    @Test
    void returnsEmptyWhenOnlyZeroRowBatchesBeforeEof() {
        RecordBatch zeroRow1 = createBatch(new int[0]);
        RecordBatch zeroRow2 = createBatch(new int[0]);

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA,
                responses(zeroRow1, zeroRow2, null));

        assertNull(exec.next());
        assertEquals(3, exec.pullCount());

        assertNull(exec.next());
        assertEquals(3, exec.pullCount());
    }

    private static class FakeBufferedExec extends BufferedExec {
        private final Queue<@Nullable RecordBatch> pulls;
        private int pullCount = 0;

        FakeBufferedExec(Schema schema, List<@Nullable RecordBatch> responses) {
            super(schema);
            this.pulls = new ArrayDeque<>(responses);
        }

        @Override
        protected @Nullable RecordBatch pull() {
            pullCount++;
            return pulls.isEmpty() ? null : pulls.poll();
        }

        int pullCount() {
            return pullCount;
        }
    }
}
