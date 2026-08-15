package io.andygrove.kquery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaPortTest {
    @Test
    void exposesProjectName() {
        assertEquals("KQuery Java", JavaPort.projectName());
    }
}
