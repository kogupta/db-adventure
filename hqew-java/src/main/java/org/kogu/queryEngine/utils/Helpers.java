package org.kogu.queryEngine.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Helpers {
    private Helpers() {}

    public static <T> Iterator<T> alwaysEmpty() {
        return new AlwaysEmpty<>();
    }

    public static final class AlwaysEmpty<T> implements Iterator<T> {
        @Override
        public boolean hasNext() {return false;}

        @Override
        public T next() {throw new NoSuchElementException();}
    }
}
