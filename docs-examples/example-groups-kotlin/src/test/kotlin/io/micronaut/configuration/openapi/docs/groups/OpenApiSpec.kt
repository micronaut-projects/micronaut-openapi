package io.micronaut.configuration.openapi.docs.groups

import io.micronaut.openapi.OpenApiUtils
import io.swagger.v3.oas.models.OpenAPI

/**
 * Loads the OpenAPI document generated at compile time into the class output.
 */
object OpenApiSpec {

    fun load(fileName: String): OpenAPI {
        val stream = OpenApiSpec::class.java.getResourceAsStream("/META-INF/swagger/$fileName")
            ?: throw IllegalStateException("Generated OpenAPI document not found: $fileName")
        return stream.use { OpenApiUtils.getYamlMapper().readValue(it, OpenAPI::class.java) }
    }
}
