package io.micronaut.openapi.spring

import tools.jackson.core.JacksonException
import io.micronaut.openapi.OpenApiUtils
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.parameters.Parameter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestClient

@ActiveProfiles("test")
@SpringBootTest(
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    classes = [
        WebConfig::class,
        TestConfig::class,
        Application::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
)
class TestControllerTest {

    @Autowired
    lateinit var restClient: RestClient

    @Disabled
    @Test
    @Throws(JacksonException::class)
    fun springOpenApiPathTest() {
        val result = restClient.get()
            .uri("/swagger/" + TestConfig.APP_NAME + '-' + TestConfig.APP_VERSION + ".yml")
            .retrieve()
            .body(String::class.java)

        val openApi = OpenApiUtils.getYamlMapper().readValue(result, OpenAPI::class.java)
        assertNotNull(openApi.info)
        assertEquals(TestConfig.APP_VERSION, openApi.info.version)
        assertNotNull(openApi.paths)

        val userSchema = openApi.components.schemas["User"]!!
        assertNotNull(userSchema)

        val createPostOp = openApi.paths["/create"]!!.post

        assertEquals("Create post op summary.", createPostOp.summary)
        assertEquals("Create post op summary. Operation post description.", createPostOp.description)
        assertNotNull(createPostOp.requestBody)

        var mediaType = createPostOp.requestBody.content["application/json"]
        assertNotNull(mediaType)
        assertNotNull(mediaType!!.schema)
        assertEquals("#/components/schemas/User", mediaType.schema.`$ref`)
        assertNotNull(createPostOp.responses)
        assertNotNull(createPostOp.responses["200"])
        assertEquals("created post user", createPostOp.responses["200"]!!.description)

        val createPatchOp = openApi.paths["/create"]!!.patch

        assertEquals("Create patch op summary.", createPatchOp.summary)
        assertEquals("Create patch op summary. Operation patch description.", createPatchOp.description)

        mediaType = createPatchOp.requestBody.content["application/json"]
        assertNotNull(mediaType)
        assertNotNull(mediaType!!.schema)
        assertEquals("#/components/schemas/User", mediaType.schema.`$ref`)
        assertNotNull(createPatchOp.responses)
        assertNotNull(createPatchOp.responses["202"])
        assertEquals("createPatch 202 response", createPatchOp.responses["202"]!!.description)

        val userIdOp = openApi.paths["/{userId}"]!!.get

        assertNotNull(userIdOp)

        val params = userIdOp.parameters
        assertNotNull(params)
        assertEquals(2, params.size)

        val userIdParam = getParamByName("userId", params)
        assertNotNull(userIdParam)
        assertEquals("userId", userIdParam!!.name)
        assertEquals("path", userIdParam.getIn())
        assertTrue(userIdParam.required)
        assertNotNull(userIdParam.schema)
        assertEquals("string", userIdParam.schema.type)

        val ageParam = getParamByName("age", params)
        assertNotNull(ageParam)
        assertEquals("age", ageParam!!.name)
        assertEquals("query", ageParam.getIn())
        assertNotNull(ageParam.schema)
        assertNull(ageParam.required)
        assertEquals("integer", ageParam.schema.type)
        assertEquals("int32", ageParam.schema.format)
        assertEquals(123, ageParam.schema.default)
    }

    private fun getParamByName(name: String, params: List<Parameter?>): Parameter? =
        params.firstOrNull { name == it!!.name }
}
