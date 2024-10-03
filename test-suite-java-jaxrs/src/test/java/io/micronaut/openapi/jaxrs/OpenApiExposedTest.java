package io.micronaut.openapi.jaxrs;

import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MicronautTest
class OpenApiExposedTest {

    @Test
    void openApi(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        assertDoesNotThrow(() -> client.exchange("/swagger/demo-0.0.yml"));
    }
}
