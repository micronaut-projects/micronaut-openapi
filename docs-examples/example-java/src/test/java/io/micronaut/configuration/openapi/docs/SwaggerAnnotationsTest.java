package io.micronaut.configuration.openapi.docs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwaggerAnnotationsTest {

    @Test
    void swaggerAnnotationsTakePrecedence() {
        var operation = OpenApiSpec.load().getPaths().get("/greetings/{name}").getGet();

        assertEquals(List.of("greeting"), operation.getTags());
        assertEquals("Greets a person", operation.getSummary());
        assertEquals("A friendly greeting is returned", operation.getDescription());
        assertEquals("greetings", operation.getOperationId());
        var parameter = operation.getParameters().get(0);
        assertEquals("name", parameter.getName());
        assertEquals("path", parameter.getIn());
        assertEquals("The name of the person", parameter.getDescription());
        assertTrue(parameter.getRequired());
        assertEquals(1, parameter.getSchema().getMinLength());
        assertEquals("string", parameter.getSchema().getType());
        var responses = operation.getResponses();
        assertEquals("string", responses.get("200").getContent().get("text/plain").getSchema().getType());
        assertEquals("Invalid Name Supplied", responses.get("400").getDescription());
        assertEquals("Person not found", responses.get("404").getDescription());
    }
}
