package org.kogu.queryengine.physical;

import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.type.Schema;

import java.util.List;
import java.util.Optional;

public final class MemoryScanExec extends BufferedExec {
    private final List<RecordBatch> batches;
    private int index = 0;

    public MemoryScanExec(Schema schema) {
        this(schema, List.of());
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
