package io.micronaut.configuration.openapi.docs.groups;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiGroupTest {

    @Test
    void groupV1() {
        var openApi = OpenApiSpec.load("public-api-v1-v1-v1.yml");

        assertEquals("Public api v1", openApi.getInfo().getTitle());
        assertEquals("v1", openApi.getInfo().getVersion());
        assertEquals("This is API version 1", openApi.getInfo().getDescription());
        var paths = openApi.getPaths();
        assertTrue(paths.containsKey("/save"));
        assertTrue(paths.containsKey("/read/{id}"));
        assertFalse(paths.containsKey("/save/{id}"));
    }

    @Test
    void groupV2() {
        var openApi = OpenApiSpec.load("public-api-v2-v2-v2.yml");

        assertEquals("Public api v2", openApi.getInfo().getTitle());
        assertEquals("v2", openApi.getInfo().getVersion());
        var paths = openApi.getPaths();
        assertTrue(paths.containsKey("/save"));
        assertTrue(paths.containsKey("/save/{id}"));
        assertFalse(paths.containsKey("/read/{id}"));
    }
}
