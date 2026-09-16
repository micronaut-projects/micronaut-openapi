package io.micronaut.configuration.openapi.docs.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TagsGenerationTest {

    @Test
    fun tagsAreGeneratedFromTheClassName() {
        val openApi = OpenApiSpec.load("hello-world-v1.1.yml")

        val tag = openApi.tags[0]
        assertEquals("user-operations", tag.name)
        assertEquals("User main operations.", tag.description)
        val paths = openApi.paths
        assertEquals(listOf("user-operations"), paths["/read/{id}"]!!.get.tags)
        assertEquals("read", paths["/read/{id}"]!!.get.operationId)
        assertEquals(listOf("user-operations"), paths["/save/{id}"]!!.post.tags)
        assertEquals("save2", paths["/save/{id}"]!!.post.operationId)
        assertEquals(listOf("user-operations"), paths["/save"]!!.post.tags)
        assertEquals("save", paths["/save"]!!.post.operationId)
    }
}
