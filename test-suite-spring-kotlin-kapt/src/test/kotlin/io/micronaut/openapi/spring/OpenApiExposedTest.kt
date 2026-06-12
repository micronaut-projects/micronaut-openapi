package io.micronaut.openapi.spring

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestClient
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

@ActiveProfiles("test")
@SpringBootTest(
    useMainMethod = UseMainMethod.ALWAYS,
    classes = [
        WebConfig::class, TestConfig::class, Application::class
    ], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class OpenApiExposedTest {

    @Autowired
    lateinit var restClient: RestClient

    @Disabled
    @Test
    @Throws(IOException::class)
    fun testOpenApiSpecEndpoint() {
        val openApiSpec: String
        javaClass.getResourceAsStream("/META-INF/swagger/" + TestConfig.APP_NAME + '-' + TestConfig.APP_VERSION + ".yml")
            .use {
                assertNotNull(it)
                openApiSpec = String(it!!.readAllBytes())
            }
        val recievedOpenApiSpec = AtomicReference<String>()

        assertDoesNotThrow {
            val result = restClient.get()
                .uri("/swagger/" + TestConfig.APP_NAME + '-' + TestConfig.APP_VERSION + ".yml")
                .retrieve()
            recievedOpenApiSpec.set(result.body(String::class.java))
        }

        assertEquals(openApiSpec, recievedOpenApiSpec.get())
    }

    @Test
    @Throws(IOException::class)
    fun testOpenApiSpecUsesApplicationYml() {
        javaClass.getResourceAsStream("/META-INF/swagger/" + TestConfig.APP_NAME + '-' + TestConfig.APP_VERSION + ".yml")
            .use {
                assertNotNull(it)
                val openApiSpec = String(it!!.readAllBytes())
                assertTrue(openApiSpec.contains("display_name:"))
                assertFalse(openApiSpec.contains("displayName:"))
            }
    }

    @Test
    @Throws(IOException::class)
    fun testSwaggerUiEndpoint() {
        val openApiSpec: String
        javaClass.getResourceAsStream("/META-INF/swagger/views/swagger-ui/index.html")
            .use {
                assertNotNull(it)
                openApiSpec = String(it!!.readAllBytes())
            }
        val recievedOpenApiSpec = AtomicReference<String>()

        assertDoesNotThrow {
            val result = restClient.get()
                .uri("/swagger-ui/index.html")
                .retrieve()
            recievedOpenApiSpec.set(result.body(String::class.java))
        }

        assertEquals(openApiSpec, recievedOpenApiSpec.get())
    }
}
