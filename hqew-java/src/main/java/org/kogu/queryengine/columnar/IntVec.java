package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.kogu.queryengine.columnar.IntVec.ConstantInt;
import static org.kogu.queryengine.columnar.IntVec.IntVector;

sealed public interface IntVec extends Vector permits IntVector, ConstantInt {
    int value(int index);

    /// Gathers rows at the given indices into a new compact vector.
    /// Null validity is remapped to the output positions.
    /// Constant storage may return a constant result without expansion.
    default IntVec gather(int[] rows) {
        int[] values = new int[rows.length];
        BitSet nulls = new BitSet();
        for (int k = 0; k < rows.length; k++) {
            values[k] = value(rows[k]);
            if (isNull(rows[k])) nulls.set(k);
        }
        return new IntVector(values, 0, rows.length, nulls);
    }

    @Override
    default Type type() {return Type.Scalar.INT32;}

    record IntVector(int[] values, int offset, int length,
                     BitSet nullIndices) implements IntVec {
        public IntVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public int value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record ConstantInt(int n, int length) implements IntVec {
        public ConstantInt {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length());
            return false;
        }

        public int value(int index) {
            Vector.validateIndex(index, length);
            return n;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public IntVec gather(int[] rows) {
            return new ConstantInt(n, rows.length);
        }
    }
}
