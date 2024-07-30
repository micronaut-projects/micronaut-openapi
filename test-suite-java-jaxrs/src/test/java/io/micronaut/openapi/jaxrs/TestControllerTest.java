package io.micronaut.openapi.jaxrs;

import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class TestControllerTest {

    @Test
    void jaxrsOpenApiPathTest(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        String responseYaml = assertDoesNotThrow(() -> client.retrieve("/swagger/demo-0.0.yml", String.class));
        assertNotNull(responseYaml);
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load(responseYaml);
        assertNotNull(obj);
        assertInstanceOf(Map.class, obj.get("paths"));
        Map<String, Object> paths = (Map<String, Object>) obj.get("paths");
        assertTrue(paths.containsKey("/user/{userId}"));
    }
}
