package io.micronaut.configuration.openapi.docs;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessorsStyleTest {

    @Test
    void customAccessorsAreDetected() {
        Schema<?> person = OpenApiSpec.load().getComponents().getSchemas().get("Person");

        assertEquals(List.of("name", "debtValue", "totalGoals"), List.copyOf(person.getProperties().keySet()));
        assertEquals("string", person.getProperties().get("name").getType());
        assertEquals("integer", person.getProperties().get("debtValue").getType());
        assertEquals("integer", person.getProperties().get("totalGoals").getType());
    }
}
