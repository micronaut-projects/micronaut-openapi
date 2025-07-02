package io.micronaut.openapi.test.api;

import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(environments = "test")
public class MicronautOpenApiTest {

    EmbeddedServer server;
    HttpClient reactiveClient;
    BlockingHttpClient client;

    public MicronautOpenApiTest(EmbeddedServer server, @Client("/api") HttpClient reactiveClient) {
        this.server = server;
        this.reactiveClient = reactiveClient;
    }

    @BeforeEach
    void setup() {
        client = reactiveClient.toBlocking();
    }

    @Test
    void testOpenApiView() {
        var swaggerUi = client.retrieve("/swagger-ui", String.class);

        assertNotNull(swaggerUi);
        assertTrue(swaggerUi.contains("link(contextPath + \"/api/swagger-ui/res/swagger-ui.css\""));

        var openApiSpec = client.retrieve("/swagger/openapi-micronaut-1.0.0.yml", String.class);
        assertNotNull(openApiSpec);
        assertTrue(
            openApiSpec.contains(
                """
                    openapi: 3.0.1
                    info:
                      title: openapi-micronaut
                      version: 1.0.0
                    """
            )
        );
    }
}
