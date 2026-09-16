package io.micronaut.configuration.openapi.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SchemasTest {

    @Test
    fun schemaAnnotationCustomizesThePojo() {
        val schemas = OpenApiSpec.load().components.schemas

        val pet = schemas["MyPet"]
        assertNotNull(pet)
        assertEquals("Pet description", pet!!.description)
        val age = pet.properties["age"]!!
        assertEquals("Pet age", age.description)
        assertEquals(BigDecimal("20"), age.maximum)
        val name = pet.properties["name"]!!
        assertEquals("Pet name", name.description)
        assertEquals(20, name.maxLength)
        val petType = schemas["PetType"]!!
        assertEquals("string", petType.type)
        assertEquals(listOf("DOG", "CAT"), petType.enum)
    }

    @Test
    fun metaAnnotationAppliesTheSchema() {
        val operation = OpenApiSpec.load().paths["/pets"]!!.post

        val schema = operation.requestBody.content["application/json"]!!.schema
        assertEquals("#/components/schemas/MyPet", schema.`$ref`)
    }

    @Test
    fun genericsAreIncludedInTheSchemaName() {
        val openApi = OpenApiSpec.load()

        val response = openApi.paths["/"]!!.put.responses["200"]!!
        assertEquals("#/components/schemas/Response_Pet_", response.content["application/json"]!!.schema.`$ref`)
        val schema = openApi.components.schemas["Response_Pet_"]!!
        assertEquals("#/components/schemas/MyPet", schema.properties["result"]!!.`$ref`)
    }

    @Test
    fun schemaNameCanBeChanged() {
        val openApi = OpenApiSpec.load()

        val response = openApi.paths["/named"]!!.put.responses["200"]!!
        assertEquals("#/components/schemas/ResponseOfPet", response.content["application/json"]!!.schema.`$ref`)
        assertTrue(openApi.components.schemas.containsKey("ResponseOfPet"))
    }
}
