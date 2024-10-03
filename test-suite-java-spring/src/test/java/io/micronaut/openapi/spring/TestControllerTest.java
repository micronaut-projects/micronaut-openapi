package io.micronaut.openapi.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.openapi.OpenApiUtils;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
            .uri("/swagger/demo-0.0.yml")
            .retrieve()
            .body(String.class);

        var openApi = OpenApiUtils.getYamlMapper().readValue(result, OpenAPI.class);
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

        assertEquals("userId", params.get(0).getName());
        assertEquals("path", params.get(0).getIn());
        assertTrue(params.get(0).getRequired());
        assertNotNull(params.get(0).getSchema());
        assertEquals("string", params.get(0).getSchema().getType());

        assertEquals("age", params.get(1).getName());
        assertEquals("query", params.get(1).getIn());
        assertNotNull(params.get(1).getSchema());
        assertEquals("integer", params.get(1).getSchema().getType());
        assertEquals("int32", params.get(1).getSchema().getFormat());
        assertTrue(params.get(1).getSchema().getNullable());
    }
}
