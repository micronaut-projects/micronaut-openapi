package io.micronaut.configuration.openapi.docs.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenApiGroupTest {

    @Test
    fun groupV1() {
        val openApi = OpenApiSpec.load("public-api-v1-v1-v1.yml")

        assertEquals("Public api v1", openApi.info.title)
        assertEquals("v1", openApi.info.version)
        assertEquals("This is API version 1", openApi.info.description)
        val paths = openApi.paths
        assertTrue(paths.containsKey("/save"))
        assertTrue(paths.containsKey("/read/{id}"))
        assertFalse(paths.containsKey("/save/{id}"))
    }

    @Test
    fun groupV2() {
        val openApi = OpenApiSpec.load("public-api-v2-v2-v2.yml")

        assertEquals("Public api v2", openApi.info.title)
        assertEquals("v2", openApi.info.version)
        val paths = openApi.paths
        assertTrue(paths.containsKey("/save"))
        assertTrue(paths.containsKey("/save/{id}"))
        assertFalse(paths.containsKey("/read/{id}"))
    }
}
