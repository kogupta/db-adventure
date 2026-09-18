package org.kogu.queryengine.type;

import java.util.*;

import static java.util.stream.Collectors.joining;

public final class Schema {
    final Map<String, Field> fields;
    final List<Field> fieldsList;   // int -> field map
    private final Map<Field, Integer> fieldIndex;

    private Schema(Map<String, Field> fields) {
        this.fields = fields;
        this.fieldsList = new ArrayList<>(fields.values());

        this.fieldIndex = new HashMap<>();
        for (int i = 0; i < fieldsList.size(); i++) {
            Field f = fieldsList.get(i);
            fieldIndex.put(f, i);
        }
    }

    public Field field(String name) {
        Field f = fields.get(name);
        if (f == null) {
            String s = asString();
            throw new IllegalArgumentException("Field not found: " + name + " in schema: " + s);
        }

        return f;
    }

    public Schema project(String... names) {
        // dedup names
        var cols = new LinkedHashSet<String>();
        var fs = new LinkedHashMap<String, Field>();
        for (String name : names) {
            if (cols.contains(name)) continue;

            Field f = field(name);
            fs.put(name, f);
            cols.add(name);
        }

        return new Schema(fs);
    }

    public int indexOfField(String name) {
        Integer n = fieldIndex.get(field(name));
        return Objects.requireNonNull(n);
    }

    public int fieldCount() {return fieldsList.size();}


    public Field fieldAtIndex(int index) {
        Objects.checkIndex(index, fieldsList.size());
        return fieldsList.get(index);
    }

    private String asString() {
        return fields.values().stream()
                .map(f -> f.name() + ": " + f.type() + ", nullable:" + f.nullable())
                .collect(joining(",", "[", "]"));
    }

    public static Schema from(List<Field> fields) {
        var uniques = new LinkedHashMap<String, Field>();
        for (Field f : fields) {
            Field prev = uniques.put(f.name(), f);
            if (prev != null) {
                throw new IllegalArgumentException("Duplicate field: " + f.name());
            }
        }

        return new Schema(uniques);
    }
}
