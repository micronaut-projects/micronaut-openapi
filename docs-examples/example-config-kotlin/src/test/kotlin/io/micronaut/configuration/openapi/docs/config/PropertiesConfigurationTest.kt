package io.micronaut.configuration.openapi.docs.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PropertiesConfigurationTest {

    @Test
    fun placeholdersAreExpanded() {
        val info = OpenApiSpec.load("hello-world-v1.1.yml").info

        assertEquals("Hello World", info.title)
        assertEquals("A nice API", info.description)
        assertEquals("v1.1", info.version)
        assertEquals("Fred", info.contact.name)
        assertEquals("Apache 2.0", info.license.name)
    }
}
