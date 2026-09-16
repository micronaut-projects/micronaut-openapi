package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenApiDecoratorTest {

    @Test
    fun operationIdsArePrefixedAndSuffixed() {
        val paths = OpenApiSpec.load().paths

        assertEquals("cats-save-suffix", paths["/cats"]!!.post.operationId)
        assertEquals("cats-get-suffix", paths["/cats/{id}"]!!.get.operationId)
        assertEquals("dogs-save", paths["/dogs"]!!.post.operationId)
        assertEquals("dogs-get", paths["/dogs/{id}"]!!.get.operationId)
    }
}
