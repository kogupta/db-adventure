package org.kogu.queryengine.physical;

import org.jspecify.annotations.Nullable;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;

import java.util.List;

public final class MemoryScanExec extends BufferedExec {
    private final List<RecordBatch> batches;
    private int index = 0;

    public MemoryScanExec(Schema schema, List<RecordBatch> batches) {
        super(schema);
        this.batches = batches;
    }

    @Override
    protected @Nullable RecordBatch pull() {
        return index < batches.size() ?
                batches.get(index++) :
                null;
    }

    public static MemoryScanExec of(Schema schema) {
        return new MemoryScanExec(schema, List.of());
    }
}
