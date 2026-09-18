package org.kogu.queryEngine.exec;

import org.kogu.queryEngine.types.RecordBatch;
import org.kogu.queryEngine.types.Schema;

import java.util.Optional;

public abstract class BufferedExec implements Exec {
    private final Schema schema;
    private boolean exhausted;

    public BufferedExec(Schema schema) {this.schema = schema;}

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
