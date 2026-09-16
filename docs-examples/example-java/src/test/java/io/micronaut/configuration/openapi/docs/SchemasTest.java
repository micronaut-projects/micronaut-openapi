package io.micronaut.configuration.openapi.docs;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemasTest {

    @Test
    void schemaAnnotationCustomizesThePojo() {
        var schemas = OpenApiSpec.load().getComponents().getSchemas();

        Schema<?> pet = schemas.get("MyPet");
        assertNotNull(pet);
        assertEquals("Pet description", pet.getDescription());
        Schema<?> age = pet.getProperties().get("age");
        assertEquals("Pet age", age.getDescription());
        assertEquals(new BigDecimal("20"), age.getMaximum());
        Schema<?> name = pet.getProperties().get("name");
        assertEquals("Pet name", name.getDescription());
        assertEquals(20, name.getMaxLength());
        Schema<?> petType = schemas.get("PetType");
        assertEquals("string", petType.getType());
        assertEquals(List.of("DOG", "CAT"), petType.getEnum());
    }

    @Test
    void metaAnnotationAppliesTheSchema() {
        var operation = OpenApiSpec.load().getPaths().get("/pets").getPost();

        var schema = operation.getRequestBody().getContent().get("application/json").getSchema();
        assertEquals("#/components/schemas/MyPet", schema.get$ref());
    }

    @Test
    void genericsAreIncludedInTheSchemaName() {
        var openApi = OpenApiSpec.load();

        var response = openApi.getPaths().get("/").getPut().getResponses().get("200");
        assertEquals("#/components/schemas/Response_Pet_", response.getContent().get("application/json").getSchema().get$ref());
        Schema<?> schema = openApi.getComponents().getSchemas().get("Response_Pet_");
        assertEquals("#/components/schemas/MyPet", schema.getProperties().get("result").get$ref());
    }

    @Test
    void schemaNameCanBeChanged() {
        var openApi = OpenApiSpec.load();

        var response = openApi.getPaths().get("/named").getPut().getResponses().get("200");
        assertEquals("#/components/schemas/ResponseOfPet", response.getContent().get("application/json").getSchema().get$ref());
        assertTrue(openApi.getComponents().getSchemas().containsKey("ResponseOfPet"));
    }
}
