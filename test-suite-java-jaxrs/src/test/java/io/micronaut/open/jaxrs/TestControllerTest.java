package io.micronaut.open.jaxrs;

import io.micronaut.context.BeanContext;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
