package org.kogu.queryEngine.types;

public record Field(String name, Type type, boolean nullable){
    public Field(String name, Type type) {
        this(name, type, true);
    }
}
