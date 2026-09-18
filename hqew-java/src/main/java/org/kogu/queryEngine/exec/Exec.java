package org.kogu.queryEngine.exec;

import org.kogu.queryEngine.types.RecordBatch;
import org.kogu.queryEngine.types.Schema;
import org.kogu.queryEngine.utils.Helpers;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public interface Exec {
    /// Output schema. Stable across calls; knowable before the first next().
    Schema schema();

    /// Next output batch:
    ///   - `Optional.empty()`    -> exhausted. Calling again returns empty() again.
    ///   - `Optional.of(batch)`  -> batch.rowCount() >= 1. Zero-row batches never cross this method.
    Optional<RecordBatch> next();

    final class MemoryScanExec extends BufferedExec {
        private final List<RecordBatch> batches;
        private int index = 0;

        public MemoryScanExec(Schema schema) {
            super(schema);
            this.batches = List.of();
        }

        public MemoryScanExec(Schema schema, List<RecordBatch> batches) {
            super(schema);
            this.batches = batches;
        }

        @Override
        protected Optional<RecordBatch> pull() {
            return index < batches.size() ?
                    Optional.of(batches.get(index++)) :
                    Optional.empty();
        }
    }
}