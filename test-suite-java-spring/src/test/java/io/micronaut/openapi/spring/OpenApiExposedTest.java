package io.micronaut.openapi.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest(useMainMethod = UseMainMethod.ALWAYS, classes = {
    WebConfig.class,
    TestConfig.class,
    Application.class,
}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OpenApiExposedTest {

    @Autowired
    RestClient restClient;

    @Test
    void testOpenApiSpecEndpoint() throws IOException {

        String openApiSpec;
        try (var is = getClass().getResourceAsStream("/META-INF/swagger/demo-0.0.yml")) {
            assertNotNull(is);
            openApiSpec = new String(is.readAllBytes());
        }
        var recievedOpenApiSpec = new AtomicReference<String>();

        assertDoesNotThrow(() -> {
            var result = restClient.get()
                .uri("/swagger/demo-0.0.yml")
                .retrieve();

            recievedOpenApiSpec.set(result.body(String.class));
        });

        assertEquals(openApiSpec, recievedOpenApiSpec.get());
    }

    @Test
    void testSwaggerUiEndpoint() throws IOException {

        String openApiSpec;
        try (var is = getClass().getResourceAsStream("/META-INF/swagger/views/swagger-ui/index.html")) {
            assertNotNull(is);
            openApiSpec = new String(is.readAllBytes());
        }
        var recievedOpenApiSpec = new AtomicReference<String>();

        assertDoesNotThrow(() -> {
            var result = restClient.get()
                .uri("/swagger-ui/index.html")
                .retrieve();

            recievedOpenApiSpec.set(result.body(String.class));
        });

        assertEquals(openApiSpec, recievedOpenApiSpec.get());
    }
}
