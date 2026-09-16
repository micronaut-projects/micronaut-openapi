package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HelloControllerTest {

    @Test
    fun openApiDefinition() {
        val openApi = OpenApiSpec.load()

        val info = openApi.info
        assertEquals("Hello World", info.title)
        assertEquals("My API", info.description)
        assertEquals("0.0", info.version)
        assertEquals("Fred", info.contact.name)
        assertEquals("https://gigantic-server.com", info.contact.url)
        assertEquals("Fred@gigagantic-server.com", info.contact.email)
        assertEquals("Apache 2.0", info.license.name)
        assertEquals("https://foo.bar", info.license.url)
    }

    @Test
    fun kdocFillsTheDescriptions() {
        val operation = OpenApiSpec.load().paths["/hello/{name}"]!!.get

        assertTrue(operation.operationId.startsWith("index"))
        val parameter = operation.parameters[0]
        assertEquals("name", parameter.name)
        assertEquals("path", parameter.`in`)
        assertEquals("The person's name", parameter.description)
        assertTrue(parameter.required)
        assertEquals("string", parameter.schema.type)
        val response = operation.responses["200"]!!
        assertEquals("The greeting", response.description)
        assertEquals("string", response.content["text/plain"]!!.schema.type)
    }
}
