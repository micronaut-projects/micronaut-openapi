package io.micronaut.configuration.openapi.docs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiDecoratorTest {

    @Test
    void operationIdsArePrefixedAndSuffixed() {
        var paths = OpenApiSpec.load().getPaths();

        assertEquals("cats-save-suffix", paths.get("/cats").getPost().getOperationId());
        assertEquals("cats-get-suffix", paths.get("/cats/{id}").getGet().getOperationId());
        assertEquals("dogs-save", paths.get("/dogs").getPost().getOperationId());
        assertEquals("dogs-get", paths.get("/dogs/{id}").getGet().getOperationId());
    }
}
