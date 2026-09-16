package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SwaggerAnnotationsTest {

    @Test
    fun swaggerAnnotationsTakePrecedence() {
        val operation = OpenApiSpec.load().paths["/greetings/{name}"]!!.get

        assertEquals(listOf("greeting"), operation.tags)
        assertEquals("Greets a person", operation.summary)
        assertEquals("A friendly greeting is returned", operation.description)
        assertEquals("greetings", operation.operationId)
        val parameter = operation.parameters[0]
        assertEquals("name", parameter.name)
        assertEquals("path", parameter.`in`)
        assertEquals("The name of the person", parameter.description)
        assertTrue(parameter.required)
        assertEquals(1, parameter.schema.minLength)
        assertEquals("string", parameter.schema.type)
        val responses = operation.responses
        assertEquals("string", responses["200"]!!.content["text/plain"]!!.schema.type)
        assertEquals("Invalid Name Supplied", responses["400"]!!.description)
        assertEquals("Person not found", responses["404"]!!.description)
    }
}
