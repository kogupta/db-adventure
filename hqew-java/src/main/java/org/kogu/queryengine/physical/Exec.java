package org.kogu.queryengine.physical;

import org.jspecify.annotations.Nullable;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;

public interface Exec {
    /// Output schema. Stable across calls; knowable before the first next().
    Schema schema();

    /// Next output batch:
    ///   - `null`  -> exhausted. Calling again returns null again.
    ///   - batch   -> batch.rowCount() >= 1. Zero-row batches never cross this method.
    @Nullable RecordBatch next();

}