package io.micronaut.configuration.openapi.docs.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagsGenerationTest {

    @Test
    void tagsAreGeneratedFromTheClassName() {
        var openApi = OpenApiSpec.load("hello-world-v1.1.yml");

        var tag = openApi.getTags().get(0);
        assertEquals("user-operations", tag.getName());
        assertEquals("User main operations.", tag.getDescription());
        var paths = openApi.getPaths();
        assertEquals(List.of("user-operations"), paths.get("/read/{id}").getGet().getTags());
        assertEquals("read", paths.get("/read/{id}").getGet().getOperationId());
        assertEquals(List.of("user-operations"), paths.get("/save/{id}").getPost().getTags());
        assertEquals("save2", paths.get("/save/{id}").getPost().getOperationId());
        assertEquals(List.of("user-operations"), paths.get("/save").getPost().getTags());
        assertEquals("save", paths.get("/save").getPost().getOperationId());
    }
}
