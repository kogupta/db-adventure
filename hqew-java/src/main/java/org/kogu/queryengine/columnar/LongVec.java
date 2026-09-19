package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.kogu.queryengine.columnar.LongVec.ConstantLong;
import static org.kogu.queryengine.columnar.LongVec.LongVector;

sealed public interface LongVec extends Vector permits LongVector, ConstantLong {
    long value(int index);

    /// Gathers rows at the given indices into a new compact vector.
    /// Null validity is remapped to the output positions.
    /// Constant storage may return a constant result without expansion.
    default LongVec gather(int[] rows) {
        long[] values = new long[rows.length];
        BitSet nulls = new BitSet();
        for (int k = 0; k < rows.length; k++) {
            values[k] = value(rows[k]);
            if (isNull(rows[k])) nulls.set(k);
        }
        return new LongVector(values, 0, rows.length, nulls);
    }

    @Override
    default Type type() {return Type.Scalar.INT64;}

    record LongVector(long[] values, int offset, int length,
                      BitSet nullIndices) implements LongVec {
        public LongVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public long value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record ConstantLong(long n, int length) implements LongVec {
        public ConstantLong {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        public long value(int index) {
            Vector.validateIndex(index, length);
            return n;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public LongVec gather(int[] rows) {
            return new ConstantLong(n, rows.length);
        }
    }
}
