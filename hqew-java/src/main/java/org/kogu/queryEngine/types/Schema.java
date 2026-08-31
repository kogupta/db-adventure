package org.kogu.queryEngine.types;

import java.util.*;

import static java.util.stream.Collectors.joining;

public final class Schema {
    private final Map<String, Field> fields;
    private Schema(Map<String, Field> fields) {this.fields = fields;}

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
