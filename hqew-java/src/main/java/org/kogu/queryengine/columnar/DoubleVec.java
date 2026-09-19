package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.kogu.queryengine.columnar.DoubleVec.ConstantDouble;
import static org.kogu.queryengine.columnar.DoubleVec.DoubleVector;

/// -----------------------------------------------------------
// level 2: element type. This is what kernels take.
sealed public interface DoubleVec extends Vector permits DoubleVector, ConstantDouble {
    double value(int index);

    /// Gathers rows at the given indices into a new compact vector.
    /// Null validity is remapped to the output positions.
    /// Constant storage may return a constant result without expansion.
    default DoubleVec gather(int[] rows) {
        double[] values = new double[rows.length];
        BitSet nulls = new BitSet();
        for (int k = 0; k < rows.length; k++) {
            values[k] = value(rows[k]);
            if (isNull(rows[k])) nulls.set(k);
        }
        return new DoubleVector(values, 0, rows.length, nulls);
    }

    @Override
    default Type type() {return Type.Scalar.FLOAT64;}

    /// -----------------------------------------------------------
    // level 3: storage. Your existing record, two words changed.
    record DoubleVector(double[] values, int offset, int length,
                        BitSet nullIndices) implements DoubleVec {
        public DoubleVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        @Override
        public double value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }
    }

    record ConstantDouble(double n, int length) implements DoubleVec {
        public ConstantDouble {
            Vector.positiveLength(length);
        }

        @Override
        public double value(int index) {
            Vector.validateIndex(index, length);
            return n;
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public DoubleVec gather(int[] rows) {
            return new ConstantDouble(n, rows.length);
        }
    }
}
