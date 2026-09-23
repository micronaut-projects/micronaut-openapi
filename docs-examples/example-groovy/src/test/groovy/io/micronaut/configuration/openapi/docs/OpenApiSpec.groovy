package io.micronaut.configuration.openapi.docs

import io.micronaut.openapi.OpenApiUtils
import io.swagger.v3.oas.models.OpenAPI

/**
 * Loads the OpenAPI document generated at compile time into the class output.
 */
class OpenApiSpec {

    static OpenAPI load(String fileName = "hello-world-0.0.yml") {
        try (InputStream stream = OpenApiSpec.getResourceAsStream("/META-INF/swagger/" + fileName)) {
            if (stream == null) {
                throw new IllegalStateException("Generated OpenAPI document not found: " + fileName)
            }
            return OpenApiUtils.yamlMapper.readValue(stream, OpenAPI)
        }
    }
}
