package io.micronaut.configuration.openapi.docs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiIncludeTest {

    @Test
    void compiledControllersAreIncluded() {
        var paths = OpenApiSpec.load().getPaths();

        var login = paths.get("/login").getPost();
        assertEquals(List.of("Security"), login.getTags());
        assertNotNull(paths.get("/logout"));
        var env = paths.get("/env").getGet();
        assertEquals(List.of("Management"), env.getTags());
        assertEquals(List.of("ADMIN"), env.getSecurity().get(0).get("BEARER"));
    }
}
