package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenApiExtraSchemaTest {

    @Test
    fun extraSchemasAreAdded() {
        val schemas = OpenApiSpec.load().components.schemas

        assertTrue(schemas.containsKey("UnusedSchema"))
        assertTrue(schemas.containsKey("UnusedModel1"))
        // KSP cannot enumerate the classes of a source package, so `packages = [...]` only applies to compiled classes
        assertFalse(schemas.containsKey("ExtraModel"))
        assertFalse(schemas.containsKey("ExcludedModel"))
        assertFalse(schemas.containsKey("ExcludedByPackage"))
    }
}
