package io.micronaut.configuration.openapi.docs.versions;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionedControllerTest {

    @Test
    void version1() {
        var openApi = OpenApiSpec.load("service-1.0.0-1.yml");

        var common = openApi.getPaths().get("/versioned/common").getPost();
        assertEquals("common", common.getOperationId());
        assertVersionParameter(common.getParameters().get(0));
        var hello = openApi.getPaths().get("/versioned/hello").getGet();
        assertEquals("helloV1", hello.getOperationId());
        assertVersionParameter(hello.getParameters().get(0));
        assertFalse(openApi.getPaths().get("/versioned/hello").readOperationsMap().containsKey(io.swagger.v3.oas.models.PathItem.HttpMethod.POST));
    }

    @Test
    void version2() {
        var openApi = OpenApiSpec.load("service-1.0.0-2.yml");

        var common = openApi.getPaths().get("/versioned/common").getPost();
        assertEquals("common", common.getOperationId());
        var hello = openApi.getPaths().get("/versioned/hello").getPost();
        assertEquals("helloV2", hello.getOperationId());
        assertVersionParameter(hello.getParameters().get(0));
        Schema<?> body = hello.getRequestBody().getContent().get("application/json").getSchema();
        String userDtoRef = body.getProperties().get("userDto").get$ref();
        assertTrue(userDtoRef.endsWith("UserDto"));
        Schema<?> userDto = openApi.getComponents().getSchemas().get(userDtoRef.substring("#/components/schemas/".length()));
        assertEquals(List.of("address"), userDto.getRequired());
        assertEquals(List.of("name", "age", "secondName", "address"), List.copyOf(userDto.getProperties().keySet()));
        assertFalse(openApi.getPaths().get("/versioned/hello").readOperationsMap().containsKey(io.swagger.v3.oas.models.PathItem.HttpMethod.GET));
    }

    private static void assertVersionParameter(io.swagger.v3.oas.models.parameters.Parameter parameter) {
        assertEquals("version", parameter.getName());
        assertEquals("query", parameter.getIn());
        assertEquals("API version", parameter.getDescription());
        assertEquals("string", parameter.getSchema().getType());
    }
}
