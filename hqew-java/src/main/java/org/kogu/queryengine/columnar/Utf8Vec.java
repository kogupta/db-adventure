package org.kogu.queryengine.columnar;

import org.kogu.queryengine.type.Type;

import java.util.BitSet;

import static org.kogu.queryengine.columnar.Utf8Vec.ConstantUtf8;
import static org.kogu.queryengine.columnar.Utf8Vec.StringVector;

sealed public interface Utf8Vec extends Vector permits StringVector, ConstantUtf8 {
    String value(int index);

    /// Gathers rows at the given indices into a new compact vector.
    /// Null validity is remapped to the output positions.
    /// Constant storage may return a constant result without expansion.
    default Utf8Vec gather(int[] rows) {
        String[] values = new String[rows.length];
        BitSet nulls = new BitSet();
        for (int k = 0; k < rows.length; k++) {
            values[k] = value(rows[k]);
            if (isNull(rows[k])) nulls.set(k);
        }
        return new StringVector(values, 0, rows.length, nulls);
    }

    @Override
    default Type type() {return Type.Scalar.UTF8;}

    record StringVector(String[] values, int offset, int length,
                        BitSet nullIndices) implements Utf8Vec {
        public StringVector {
            Vector.positiveLength(length);
            Vector.validateWindow(offset, length, values.length);
        }

        public String value(int index) {
            Vector.validateIndex(index, length);
            return values[offset + index];
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return nullIndices.get(offset + index);
        }

    }

    record ConstantUtf8(String s, int length) implements Utf8Vec {
        public ConstantUtf8 {
            Vector.positiveLength(length);
        }

        @Override
        public boolean isNull(int index) {
            Vector.validateIndex(index, length);
            return false;
        }

        public String value(int index) {
            Vector.validateIndex(index, length);
            return s;
        }

        /// Every selected row reads the same value, so the gathered result
        /// is the same constant with a new length; no expansion, no copy.
        @Override
        public Utf8Vec gather(int[] rows) {
            return new ConstantUtf8(s, rows.length);
        }
    }
}
