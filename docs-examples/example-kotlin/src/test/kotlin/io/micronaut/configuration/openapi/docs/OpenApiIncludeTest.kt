package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OpenApiIncludeTest {

    @Test
    fun compiledControllersAreIncluded() {
        val paths = OpenApiSpec.load().paths

        val login = paths["/login"]!!.post
        assertEquals(listOf("Security"), login.tags)
        assertNotNull(paths["/logout"])
        val env = paths["/env"]!!.get
        assertEquals(listOf("Management"), env.tags)
        assertEquals(listOf("ADMIN"), env.security[0]["BEARER"])
    }
}
