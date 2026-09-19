package org.kogu.queryengine.physical;

import org.jspecify.annotations.Nullable;
import org.kogu.queryengine.columnar.BoolVec;
import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;
import org.kogu.queryengine.expression.Expr;
import org.kogu.queryengine.expression.ExprEvaluator;
import org.kogu.queryengine.expression.ExprType;
import org.kogu.queryengine.type.Schema;
import org.kogu.queryengine.type.Type;

public final class FilterExec extends BufferedExec {
    private final Exec child;
    private final Expr predicate;

    private FilterExec(Exec child, Expr predicate) {
        super(child.schema());
        this.child = child;
        this.predicate = predicate;
    }

    public static FilterExec create(Exec child, Expr predicate) {
        ExprType exprType = predicate.outputType(child.schema());
        if (exprType.type() != Type.Scalar.BOOLEAN) {
            throw new IllegalArgumentException("Filter predicate must be of type boolean");
        }

        return new FilterExec(child, predicate);
    }

    @Override
    protected @Nullable RecordBatch pull() {
        while (true) {
            var batch = child.next();
            if (batch == null) return null;

            BoolVec mask = (BoolVec) ExprEvaluator.eval(predicate, batch);
            Schema schema = schema();
            int length = mask.length();

            // pass 1: count survivors; null predicate slots are not selected
            int rowCount = 0;
            for (int i = 0; i < length; i++) {
                if (mask.isNotNull(i) && mask.value(i)) rowCount++;
            }

            if (rowCount == 0) continue; // nothing survived this batch; try the next one

            // survivor indices
            int[] rows = new int[rowCount];
            for (int i = 0, out = 0; i < length; i++) {
                if (mask.isNotNull(i) && mask.value(i)) rows[out++] = i;
            }

            // pass 2: gather each column through the element-type abstraction
            Vector[] output = new Vector[schema.fieldCount()];
            for (int col = 0; col < schema.fieldCount(); col++) {
                String columnName = schema.fieldAtIndex(col).name();
                Vector in = batch.vectorOf(columnName);
                output[col] = Vector.gather(in, rows);
            }

            return new RecordBatch(schema, output);
        }
    }
}
