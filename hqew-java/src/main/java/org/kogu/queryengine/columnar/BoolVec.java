package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.kogu.queryengine.columnar.BoolVec.BooleanVector;
import static org.kogu.queryengine.columnar.BoolVec.ConstantBool;

sealed public interface BoolVec extends Vector permits BooleanVector, ConstantBool {
    boolean value(int index);

    /// Gathers rows at the given indices into a new compact vector.
    /// Null validity is remapped to the output positions.
    /// Constant storage may return a constant result without expansion.
    default BoolVec gather(int[] rows) {
        boolean[] values = new boolean[rows.length];
        BitSet nulls = new BitSet();
        for (int k = 0; k < rows.length; k++) {
            values[k] = value(rows[k]);
            if (isNull(rows[k])) nulls.set(k);
        }
        return new BooleanVector(values, 0, rows.length, nulls);
    }

    @Override
    default Type type() {return Type.Scalar.BOOLEAN;}

    record BooleanVector(boolean[] values, int offset, int length,
                         BitSet nullIndices) implements BoolVec {
        public BooleanVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public boolean value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record ConstantBool(boolean value, int length) implements BoolVec {
        public ConstantBool {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        public boolean value(int index) {
            Vector.validateIndex(index, length);
            return value;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public BoolVec gather(int[] rows) {
            return new ConstantBool(value, rows.length);
        }
    }
}
