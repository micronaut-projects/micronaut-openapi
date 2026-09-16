package io.micronaut.configuration.openapi.docs.versions

import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionedControllerTest {

    @Test
    fun version1() {
        val openApi = OpenApiSpec.load("service-1.0.0-1.yml")

        val common = openApi.paths["/versioned/common"]!!.post
        assertEquals("common", common.operationId)
        assertVersionParameter(common.parameters[0])
        val hello = openApi.paths["/versioned/hello"]!!.get
        assertEquals("helloV1", hello.operationId)
        assertVersionParameter(hello.parameters[0])
        assertFalse(openApi.paths["/versioned/hello"]!!.readOperationsMap().containsKey(PathItem.HttpMethod.POST))
    }

    @Test
    fun version2() {
        val openApi = OpenApiSpec.load("service-1.0.0-2.yml")

        val common = openApi.paths["/versioned/common"]!!.post
        assertEquals("common", common.operationId)
        val hello = openApi.paths["/versioned/hello"]!!.post
        assertEquals("helloV2", hello.operationId)
        assertVersionParameter(hello.parameters[0])
        val body = hello.requestBody.content["application/json"]!!.schema
        val userDtoRef = body.properties["userDto"]!!.`$ref`
        assertTrue(userDtoRef.endsWith("UserDto"))
        val userDto = openApi.components.schemas[userDtoRef.removePrefix("#/components/schemas/")]!!
        // Kotlin non-null properties (`age: Int`) are required as well
        assertTrue(userDto.required.contains("address"))
        assertEquals(listOf("name", "age", "secondName", "address"), userDto.properties.keys.toList())
        assertFalse(openApi.paths["/versioned/hello"]!!.readOperationsMap().containsKey(PathItem.HttpMethod.GET))
    }

    private fun assertVersionParameter(parameter: Parameter) {
        assertEquals("version", parameter.name)
        assertEquals("query", parameter.`in`)
        assertEquals("API version", parameter.description)
        assertEquals("string", parameter.schema.type)
    }
}
