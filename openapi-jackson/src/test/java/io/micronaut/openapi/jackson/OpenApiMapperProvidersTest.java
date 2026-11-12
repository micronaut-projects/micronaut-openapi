package io.micronaut.openapi.jackson;

import io.micronaut.json.tree.JsonNode;
import io.micronaut.openapi.mapper.OpenApiMapperProvider;
import io.micronaut.openapi.mapper.OpenApiMapperProviders;
import io.micronaut.openapi.mapper.OpenApiMapperType;
import io.micronaut.openapi.mapper.OpenApiVersion;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiMapperProvidersTest {

    private final ClassLoader classLoader = getClass().getClassLoader();

    @Test
    void selectsJacksonByDefaultAndByName() {
        assertInstanceOf(JacksonOpenApiMapperProvider.class, OpenApiMapperProviders.select(null, classLoader));
        assertInstanceOf(JacksonOpenApiMapperProvider.class, OpenApiMapperProviders.select(" JACKSON ", classLoader));
    }

    @Test
    void failsForUnknownProvider() {
        var e = assertThrows(IllegalStateException.class, () -> OpenApiMapperProviders.select("serde", classLoader));
        assertTrue(e.getMessage().contains(OpenApiMapperProviders.MAPPER_PROPERTY), e.getMessage());
        assertTrue(e.getMessage().contains("Available: jackson, test"), e.getMessage());
    }

    @Test
    void writesSpecCompliantYaml() throws Exception {
        OpenApiMapperProvider provider = OpenApiMapperProviders.select(null, classLoader);
        var info = new Info().title("t").version("1");
        info.addExtension("x-a", 1);
        var openApi = new OpenAPI().info(info);
        openApi.schemaRequirement("k", new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("k"));

        String yaml = new String(provider.getMapper(OpenApiMapperType.YAML, OpenApiVersion.of(false)).writeValueAsBytes(openApi), StandardCharsets.UTF_8);

        assertTrue(yaml.contains("x-a: 1"), yaml);
        assertTrue(yaml.contains("type: apiKey"), yaml);
        assertTrue(yaml.contains("in: header"), yaml);
    }

    @Test
    void convertsThroughMicronautTrees() throws Exception {
        var mapper = OpenApiMapperProviders.select(null, classLoader).getMapper(OpenApiMapperType.CONVERT, OpenApiVersion.of(false));
        JsonNode tree = JsonNode.from(Map.of("type", "apiKey", "in", "header", "name", "k"));

        SecurityScheme scheme = mapper.readValueFromTree(tree, SecurityScheme.class);

        assertEquals(SecurityScheme.Type.APIKEY, scheme.getType());
        assertEquals("k", scheme.getName());
        assertEquals("apiKey", mapper.writeValueToTree(scheme).get("type").getStringValue());
    }

    @Test
    void treeRoundTripKeepsExplicitNulls() throws Exception {
        var mapper = OpenApiMapperProviders.select(null, classLoader).getMapper(OpenApiMapperType.JSON, OpenApiVersion.of(false));
        var id = new IntegerSchema();
        id.setExample(null);
        id.setExampleSetFlag(true);
        var parent = new ObjectSchema().addProperty("id", id);

        Schema<?> copy = mapper.readValueFromTree(mapper.writeValueToTree(parent), Schema.class);

        assertTrue(((Schema<?>) copy.getProperties().get("id")).getExampleSetFlag());
        assertTrue(mapper.writeValueAsString(copy).contains("\"example\":null"));
    }

    @Test
    void resolvesNamingStrategies() {
        var provider = OpenApiMapperProviders.select(null, classLoader);
        assertEquals("my_property", provider.findNamingStrategy("SNAKE_CASE").orElseThrow().apply("myProperty"));
        assertEquals("my-property", provider.findNamingStrategyClass("tools.jackson.databind.PropertyNamingStrategies$KebabCaseStrategy", classLoader)
            .orElseThrow().apply("myProperty"));
        assertTrue(provider.findNamingStrategy("NOPE").isEmpty());
    }
}
