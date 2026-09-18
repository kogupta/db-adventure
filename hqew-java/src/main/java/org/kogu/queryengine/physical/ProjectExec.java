package org.kogu.queryengine.physical;

import org.jspecify.annotations.Nullable;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.expression.ExprEvaluator;
import org.kogu.queryengine.expression.ExprType;
import org.kogu.queryengine.type.Field;
import org.kogu.queryengine.type.Schema;

import java.util.ArrayList;
import java.util.List;

public final class ProjectExec extends BufferedExec {
    private final Exec child;
    private final List<Projection> projections;

    public ProjectExec(Exec child, List<Projection> projections) {
        Schema schema = deriveSchema(child, projections);
        this.child = child;
        this.projections = List.copyOf(projections);
        super(schema);
    }

    private static Schema deriveSchema(Exec child, List<Projection> projections) {
        if (projections.isEmpty()) {
            throw new IllegalArgumentException("projection requires at least one expression");
        }

        List<Field> fields = new ArrayList<>(projections.size());
        for (Projection projection : projections) {
            ExprType result = projection.expression().outputType(child.schema());
            Field field = new Field(projection.name(), result.type(), result.nullable());
            fields.add(field);
        }
        return Schema.from(fields);
    }

    @Override
    protected @Nullable RecordBatch pull() {
        var batch = child.next();
        if (batch == null) return null;

        Vector[] output = new Vector[projections.size()];
        for (int i = 0; i < projections.size(); i++) {
            output[i] = ExprEvaluator.eval(projections.get(i).expression(), batch);
        }

        return new RecordBatch(schema(), output);
    }
}
