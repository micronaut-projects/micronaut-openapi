package io.micronaut.configuration.openapi.docs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiExtraSchemaTest {

    @Test
    void extraSchemasAreAdded() {
        var schemas = OpenApiSpec.load().getComponents().getSchemas();

        assertTrue(schemas.containsKey("UnusedSchema"));
        assertTrue(schemas.containsKey("UnusedModel1"));
        assertTrue(schemas.containsKey("ExtraModel"));
        assertFalse(schemas.containsKey("ExcludedModel"));
        assertFalse(schemas.containsKey("ExcludedByPackage"));
    }
}
