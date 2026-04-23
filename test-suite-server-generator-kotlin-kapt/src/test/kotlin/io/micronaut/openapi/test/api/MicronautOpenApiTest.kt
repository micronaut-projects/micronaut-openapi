package io.micronaut.openapi.test.api

import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.openapi.OpenApiUtils
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.platform.commons.util.CollectionUtils
import java.lang.IllegalStateException
import kotlin.String

@MicronautTest(environments = ["test"])
class MicronautOpenApiTest(
    var server: EmbeddedServer,
    @Client("/api")
    var reactiveClient: HttpClient,
) {

    lateinit var client: BlockingHttpClient

    @BeforeEach
    fun setup() {
        client = reactiveClient.toBlocking()
    }

    @Test
    fun testOpenApiView() {
        val swaggerUi: String = client.retrieve("/swagger-ui", String::class.java)

        assertNotNull(swaggerUi)
        assertTrue(swaggerUi.contains("link(contextPath + \"/api/swagger-ui/res/swagger-ui.css\""))

        val openApiSpec: String = client.retrieve("/swagger/openapi-micronaut-1.0.0.yml", String::class.java)
        assertNotNull(openApiSpec)
        assertTrue(
            openApiSpec.contains(
                """
                openapi: 3.0.1
                info:
                  title: openapi-micronaut
                  version: 1.0.0
                """.trimIndent()
            )
        )

        val openApi = OpenApiUtils.getYamlMapper().readValue(openApiSpec, OpenAPI::class.java)

        assertNotNull(openApi)
        val schema = openApi.components?.schemas?.get("TypeDto") ?: throw IllegalStateException("Schema TypeDto not found")
        assertEquals("string", schema.type)
        assertFalse(schema.enum.isNullOrEmpty())
        assertEquals(2, schema.enum.size)
        assertTrue(schema.enum.contains("EPISODE"))
        assertTrue(schema.enum.contains("SCHEDULED_LIVESTREAM"))
    }
}
