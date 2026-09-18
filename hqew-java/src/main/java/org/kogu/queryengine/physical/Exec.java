package org.kogu.queryengine.physical;

import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;

import java.util.Optional;

public interface Exec {
    /// Output schema. Stable across calls; knowable before the first next().
    Schema schema();

    /// Next output batch:
    ///   - `Optional.empty()`    -> exhausted. Calling again returns empty() again.
    ///   - `Optional.of(batch)`  -> batch.rowCount() >= 1. Zero-row batches never cross this method.
    Optional<RecordBatch> next();

}