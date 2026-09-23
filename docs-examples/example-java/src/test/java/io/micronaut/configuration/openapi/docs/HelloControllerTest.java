package io.micronaut.configuration.openapi.docs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelloControllerTest {

    @Test
    void openApiDefinition() {
        var openApi = OpenApiSpec.load();

        var info = openApi.getInfo();
        assertEquals("Hello World", info.getTitle());
        assertEquals("My API", info.getDescription());
        assertEquals("0.0", info.getVersion());
        assertEquals("Fred", info.getContact().getName());
        assertEquals("https://gigantic-server.com", info.getContact().getUrl());
        assertEquals("Fred@gigagantic-server.com", info.getContact().getEmail());
        assertEquals("Apache 2.0", info.getLicense().getName());
        assertEquals("https://foo.bar", info.getLicense().getUrl());
    }

    @Test
    void javadocFillsTheDescriptions() {
        var operation = OpenApiSpec.load().getPaths().get("/hello/{name}").getGet();

        assertTrue(operation.getOperationId().startsWith("index"));
        var parameter = operation.getParameters().get(0);
        assertEquals("name", parameter.getName());
        assertEquals("path", parameter.getIn());
        assertEquals("The person's name", parameter.getDescription());
        assertTrue(parameter.getRequired());
        assertEquals("string", parameter.getSchema().getType());
        var response = operation.getResponses().get("200");
        assertEquals("The greeting", response.getDescription());
        assertEquals("string", response.getContent().get("text/plain").getSchema().getType());
    }
}
