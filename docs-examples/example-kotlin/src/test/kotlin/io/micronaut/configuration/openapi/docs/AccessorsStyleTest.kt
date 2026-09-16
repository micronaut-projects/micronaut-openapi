package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccessorsStyleTest {

    @Test
    fun customAccessorsAreDetected() {
        val person = OpenApiSpec.load().components.schemas["Person"]!!

        assertEquals(listOf("name", "debtValue", "totalGoals"), person.properties.keys.toList())
        assertEquals("string", person.properties["name"]!!.type)
        assertEquals("integer", person.properties["debtValue"]!!.type)
        assertEquals("integer", person.properties["totalGoals"]!!.type)
    }
}
