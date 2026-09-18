package org.kogu.queryengine.physical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.columnar.Vectors;

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

    @Test
    void zeroRowBatchFollowedByNonEmptyBatch() {
        RecordBatch zeroRowBatch = createBatch(new int[0]);
        RecordBatch nonEmptyBatch = createBatch(new int[]{42});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.of(zeroRowBatch),
                Optional.of(nonEmptyBatch),
                Optional.empty()
        ));

        Optional<RecordBatch> result = exec.next();
        assertTrue(result.isPresent());
        assertSame(nonEmptyBatch, result.get());
        assertEquals(2, exec.pullCount());
    }

    @Test
    void exhaustionAfterFinalBatch() {
        RecordBatch finalBatch = createBatch(new int[]{1, 2, 3});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.of(finalBatch),
                Optional.empty()
        ));

        Optional<RecordBatch> first = exec.next();
        assertTrue(first.isPresent());
        assertSame(finalBatch, first.get());

        Optional<RecordBatch> second = exec.next();
        assertTrue(second.isEmpty());
        assertEquals(2, exec.pullCount());
    }

    @Test
    void repeatedCallsAfterExhaustionDoNotCallPullAgain() {
        RecordBatch batch = createBatch(new int[]{1});
        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.of(batch),
                Optional.empty()
        ));

        assertTrue(exec.next().isPresent());
        assertTrue(exec.next().isEmpty());
        assertEquals(2, exec.pullCount());

        // Repeated calls after exhaustion do not invoke pull() again
        assertTrue(exec.next().isEmpty());
        assertTrue(exec.next().isEmpty());
        assertTrue(exec.next().isEmpty());
        assertEquals(2, exec.pullCount());
    }

    @Test
    void exhaustedImmediatelyWhenFirstPullIsEmpty() {
        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.empty()
        ));

        assertTrue(exec.next().isEmpty());
        assertEquals(1, exec.pullCount());

        // Subsequent calls remain empty and do not invoke pull() again
        assertTrue(exec.next().isEmpty());
        assertTrue(exec.next().isEmpty());
        assertEquals(1, exec.pullCount());
    }

    @Test
    void returnsBatchesInOrderUntilExhausted() {
        RecordBatch batch1 = createBatch(new int[]{1, 2});
        RecordBatch batch2 = createBatch(new int[]{3, 4, 5});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.of(batch1),
                Optional.of(batch2),
                Optional.empty()
        ));

        Optional<RecordBatch> first = exec.next();
        assertTrue(first.isPresent());
        assertSame(batch1, first.get());
        assertEquals(1, exec.pullCount());

        Optional<RecordBatch> second = exec.next();
        assertTrue(second.isPresent());
        assertSame(batch2, second.get());
        assertEquals(2, exec.pullCount());

        Optional<RecordBatch> third = exec.next();
        assertTrue(third.isEmpty());
        assertEquals(3, exec.pullCount());

        // Consecutive calls after exhaustion
        assertTrue(exec.next().isEmpty());
        assertEquals(3, exec.pullCount());
    }

    @Test
    void skipsZeroRowBatches() {
        RecordBatch zeroRowBatch1 = createBatch(new int[0]);
        RecordBatch zeroRowBatch2 = createBatch(new int[0]);
        RecordBatch validBatch1 = createBatch(new int[]{10});
        RecordBatch validBatch2 = createBatch(new int[]{20, 30});

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.of(zeroRowBatch1),
                Optional.of(validBatch1),
                Optional.of(zeroRowBatch2),
                Optional.of(validBatch2),
                Optional.empty()
        ));

        // The first next() should pull zeroRowBatch1, skip it, and return validBatch1
        Optional<RecordBatch> first = exec.next();
        assertTrue(first.isPresent());
        assertSame(validBatch1, first.get());
        assertEquals(2, exec.pullCount());

        // The second next() should pull zeroRowBatch2, skip it, and return validBatch2
        Optional<RecordBatch> second = exec.next();
        assertTrue(second.isPresent());
        assertSame(validBatch2, second.get());
        assertEquals(4, exec.pullCount());

        // The third next() pulls empty and terminates
        Optional<RecordBatch> third = exec.next();
        assertTrue(third.isEmpty());
        assertEquals(5, exec.pullCount());

        // Further calls do not pull
        assertTrue(exec.next().isEmpty());
        assertEquals(5, exec.pullCount());
    }

    @Test
    void returnsEmptyWhenOnlyZeroRowBatchesBeforeEof() {
        RecordBatch zeroRow1 = createBatch(new int[0]);
        RecordBatch zeroRow2 = createBatch(new int[0]);

        FakeBufferedExec exec = new FakeBufferedExec(TEST_SCHEMA, List.of(
                Optional.of(zeroRow1),
                Optional.of(zeroRow2),
                Optional.empty()
        ));

        Optional<RecordBatch> result = exec.next();
        assertTrue(result.isEmpty());
        assertEquals(3, exec.pullCount());

        // Subsequent call remains empty without pulling
        assertTrue(exec.next().isEmpty());
        assertEquals(3, exec.pullCount());
    }

    private static RecordBatch createBatch(int[] values) {
        return new RecordBatch(TEST_SCHEMA, new Vector[]{Vectors.intVector(values)});
    }

    private static class FakeBufferedExec extends BufferedExec {
        private final Queue<Optional<RecordBatch>> pulls;
        private int pullCount = 0;

        FakeBufferedExec(Schema schema, List<Optional<RecordBatch>> responses) {
            super(schema);
            this.pulls = new ArrayDeque<>(responses);
        }

        @Override
        protected Optional<RecordBatch> pull() {
            pullCount++;
            return pulls.isEmpty() ? Optional.empty() : pulls.poll();
        }

        int pullCount() {
            return pullCount;
        }
    }
}
