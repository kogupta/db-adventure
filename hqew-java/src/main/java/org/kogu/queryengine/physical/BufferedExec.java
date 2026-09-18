package org.kogu.queryengine.physical;

import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;

import java.util.Optional;

public abstract class BufferedExec implements Exec {
    private final Schema schema;
    private boolean exhausted;

    protected BufferedExec(Schema schema) {this.schema = schema;}

    @Override
    public Schema schema() {return schema;}

    @Override
    public final Optional<RecordBatch> next() {
        if (exhausted) return Optional.empty();

        while (!exhausted) {
            Optional<RecordBatch> batch = pull();
            if (batch.isEmpty()) exhausted = true;
            else if (batch.get().rowCount() > 0)
                return batch;
        }

        return Optional.empty();
    }

    protected abstract Optional<RecordBatch> pull();
}
