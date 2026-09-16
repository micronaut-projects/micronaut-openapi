package io.micronaut.configuration.openapi.docs;

import io.micronaut.openapi.OpenApiUtils;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Loads the OpenAPI document generated at compile time into the class output.
 */
final class OpenApiSpec {

    private OpenApiSpec() {
    }

    static OpenAPI load() {
        return load("hello-world-0.0.yml");
    }

    static OpenAPI load(String fileName) {
        try (InputStream stream = OpenApiSpec.class.getResourceAsStream("/META-INF/swagger/" + fileName)) {
            if (stream == null) {
                throw new IllegalStateException("Generated OpenAPI document not found: " + fileName);
            }
            return OpenApiUtils.getYamlMapper().readValue(stream, OpenAPI.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
