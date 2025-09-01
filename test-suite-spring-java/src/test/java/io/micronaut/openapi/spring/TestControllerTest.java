package io.micronaut.openapi.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.openapi.OpenApiUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.List;

import static io.micronaut.openapi.spring.TestConfig.APP_NAME;
import static io.micronaut.openapi.spring.TestConfig.APP_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, classes = {
    WebConfig.class,
    TestConfig.class,
    Application.class,
}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TestControllerTest {

    @Autowired
    RestClient restClient;

    @Test
    void springOpenApiPathTest() throws JsonProcessingException {
        var result = restClient.get()
            .uri("/swagger/" + APP_NAME + '-' + APP_VERSION + ".yml")
            .retrieve()
            .body(String.class);

        var openApi = OpenApiUtils.getYamlMapper().readValue(result, OpenAPI.class);
        assertNotNull(openApi.getInfo());
        assertEquals(APP_VERSION, openApi.getInfo().getVersion());
        assertNotNull(openApi.getPaths());

        var userSchema = openApi.getComponents().getSchemas().get("User");
        assertNotNull(userSchema);

        var createPostOp = openApi.getPaths().get("/create").getPost();

        assertEquals("Create post op summary.", createPostOp.getSummary());
        assertEquals("Create post op summary. Operation post description.", createPostOp.getDescription());
        assertNotNull(createPostOp.getRequestBody());

        var mediaType = createPostOp.getRequestBody().getContent().get("application/json");
        assertNotNull(mediaType);
        assertNotNull(mediaType.getSchema());
        assertEquals("#/components/schemas/User", mediaType.getSchema().get$ref());
        assertNotNull(createPostOp.getResponses());
        assertNotNull(createPostOp.getResponses().get("200"));
        assertEquals("created post user", createPostOp.getResponses().get("200").getDescription());

        var createPatchOp = openApi.getPaths().get("/create").getPatch();

        assertEquals("Create patch op summary.", createPatchOp.getSummary());
        assertEquals("Create patch op summary. Operation patch description.", createPatchOp.getDescription());

        mediaType = createPatchOp.getRequestBody().getContent().get("application/json");
        assertNotNull(mediaType);
        assertNotNull(mediaType.getSchema());
        assertEquals("#/components/schemas/User", mediaType.getSchema().get$ref());
        assertNotNull(createPatchOp.getResponses());
        assertNotNull(createPatchOp.getResponses().get("202"));
        assertEquals("createPatch 202 response", createPatchOp.getResponses().get("202").getDescription());

        var userIdOp = openApi.getPaths().get("/{userId}").getGet();

        assertNotNull(userIdOp);

        var params = userIdOp.getParameters();
        assertNotNull(params);
        assertEquals(2, params.size());

        var userIdParam = getParamByName("userId", params);
        assertNotNull(userIdParam);
        assertEquals("userId", userIdParam.getName());
        assertEquals("path", userIdParam.getIn());
        assertTrue(userIdParam.getRequired());
        assertNotNull(userIdParam.getSchema());
        assertEquals("string", userIdParam.getSchema().getType());

        var ageParam = getParamByName("age", params);
        assertNotNull(ageParam);
        assertEquals("age", ageParam.getName());
        assertEquals("query", ageParam.getIn());
        assertNotNull(ageParam.getSchema());
        assertNull(ageParam.getRequired());
        assertEquals("integer", ageParam.getSchema().getType());
        assertEquals("int32", ageParam.getSchema().getFormat());
        assertEquals(123, ageParam.getSchema().getDefault());
        assertTrue(ageParam.getSchema().getNullable());
    }

    private Parameter getParamByName(String name, List<Parameter> params) {
        return params.stream()
            .filter(p -> name.equals(p.getName()))
            .findFirst()
            .orElse(null);
    }
}
