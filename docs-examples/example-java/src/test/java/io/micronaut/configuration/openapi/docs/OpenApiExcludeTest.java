package io.micronaut.configuration.openapi.docs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenApiExcludeTest {

    @Test
    void excludedControllersAreNotDocumented() {
        var paths = OpenApiSpec.load().getPaths();

        assertFalse(paths.containsKey("/old"));
        assertFalse(paths.containsKey("/internal"));
    }
}
