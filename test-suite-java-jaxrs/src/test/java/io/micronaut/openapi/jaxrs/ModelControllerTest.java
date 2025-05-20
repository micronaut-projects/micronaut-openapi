package io.micronaut.openapi.jaxrs;

import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class ModelControllerTest {

    /**
     * Two objects which generate the same schema-name, but are in different packages, should
     * each be output exactly one time in the generated yaml's components/schema. The first object
     * will have the expected schema-name, the second will have a `_1` appended to it.
     */
    @Test
    void duplicateSchemasOnlyAppearOnce(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        String responseYaml = assertDoesNotThrow(() -> client.retrieve("/swagger/demo-0.0.yml", String.class));
        assertNotNull(responseYaml);
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load(responseYaml);
        assertNotNull(obj);
        assertInstanceOf(Map.class, obj.get("components"));
        Map<String, Object> components = (Map<String, Object>) obj.get("components");
        assertEquals(1, components.size());
        assertInstanceOf(Map.class, components.get("schemas"));
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertEquals(2, schemas.size());

        assertTrue(schemas.containsKey("Response"));
        assertTrue(schemas.containsKey("Response_1"));
    }
}
