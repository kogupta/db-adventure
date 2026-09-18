package org.kogu.queryengine.physical;

import org.jspecify.annotations.Nullable;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;

public abstract class BufferedExec implements Exec {
    private final Schema schema;
    private boolean exhausted;

    protected BufferedExec(Schema schema) {this.schema = schema;}

    @Override
    public Schema schema() {return schema;}

    @Override
    public final @Nullable RecordBatch next() {
        if (exhausted) return null;

        while (!exhausted) {
            RecordBatch batch = pull();
            if (batch == null) {
                exhausted = true;
                return null;
            }
            if (batch.rowCount() > 0) return batch;
        }

        return null;
    }

    protected abstract @Nullable RecordBatch pull();
}
