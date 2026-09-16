package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class OpenApiExcludeTest {

    @Test
    fun excludedControllersAreNotDocumented() {
        val paths = OpenApiSpec.load().paths

        assertFalse(paths.containsKey("/old"))
        assertFalse(paths.containsKey("/internal"))
    }
}
