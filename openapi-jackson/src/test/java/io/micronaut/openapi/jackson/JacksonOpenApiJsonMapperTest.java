package io.micronaut.openapi.jackson;

import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.JsonStreamConfig;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.mapper.OpenApiMapperType;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonOpenApiJsonMapperTest {

    private final JacksonOpenApiJsonMapper json = new JacksonOpenApiJsonMapper(OpenApiUtils.JSON_MAPPER);
    private final JacksonOpenApiJsonMapper yaml = new JacksonOpenApiJsonMapper(OpenApiUtils.YAML_MAPPER);

    @Test
    void exposesObjectMapperAndStreamConfig() {
        assertSame(OpenApiUtils.JSON_MAPPER, json.getObjectMapper());
        assertSame(JsonStreamConfig.DEFAULT, json.getStreamConfig());
    }

    @Test
    void readsValuesFromStringsBytesAndStreams() throws IOException {
        String doc = "{\"title\":\"t\",\"version\":\"1\",\"x-a\":1}";
        byte[] bytes = doc.getBytes(StandardCharsets.UTF_8);

        assertEquals("t", json.readValue(doc, Argument.of(Info.class)).getTitle());
        assertEquals("t", json.readValue(bytes, Argument.of(Info.class)).getTitle());
        assertEquals(Map.of("x-a", 1), json.readValue(new ByteArrayInputStream(bytes), Argument.of(Info.class)).getExtensions());
    }

    @Test
    void readsTreesFromStringsBytesAndStreams() throws IOException {
        String doc = "{\"s\":\"x\",\"b\":true,\"n\":null,\"i\":1,\"d\":1.5,\"l\":[1,\"a\"]}";
        byte[] bytes = doc.getBytes(StandardCharsets.UTF_8);

        for (JsonNode tree : List.of(
            json.readValue(doc, Argument.of(JsonNode.class)),
            json.readValue(bytes, Argument.of(JsonNode.class)),
            json.readValue(new ByteArrayInputStream(bytes), Argument.of(JsonNode.class)))) {
            assertEquals("x", tree.get("s").getStringValue());
            assertTrue(tree.get("b").getBooleanValue());
            assertTrue(tree.get("n").isNull());
            assertEquals(1, tree.get("i").getIntValue());
            assertEquals(1.5, tree.get("d").getDoubleValue());
            assertEquals(2, tree.get("l").size());
        }
        assertTrue(json.readValue("null", Argument.of(JsonNode.class)).isNull());
    }

    @Test
    void readsYaml() throws IOException {
        Info info = yaml.readValue("title: t\nversion: '1'\nx-a: 1\n", Argument.of(Info.class));

        assertEquals("1", info.getVersion());
        assertEquals(Map.of("x-a", 1), info.getExtensions());
    }

    @Test
    void wrapsParseErrorsAsIoExceptions() {
        byte[] invalid = "{not json".getBytes(StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> json.readValue("{not json", Argument.of(Info.class)));
        assertThrows(IOException.class, () -> json.readValue(invalid, Argument.of(Info.class)));
        assertThrows(IOException.class, () -> json.readValue(new ByteArrayInputStream(invalid), Argument.of(Info.class)));
        assertThrows(IOException.class, () -> json.readValue("{not json", Argument.of(JsonNode.class)));
        assertThrows(IOException.class, () -> json.readValueFromTree(JsonNode.createStringNode("x"), Argument.of(Integer.class)));
        assertThrows(IOException.class, () -> json.updateValue(new Info(), JsonNode.createObjectNode(Map.of("title", JsonNode.createObjectNode(Map.of("a", JsonNode.createNumberNodeImpl(1)))))));
    }

    @Test
    void writesValuesAndTrees() throws IOException {
        var info = new Info().title("t").version("1");
        info.addExtension("x-a", 1);

        String expected = "{\"title\":\"t\",\"version\":\"1\",\"x-a\":1}";
        assertEquals(expected, json.writeValueAsString(info));
        assertEquals(expected, new String(json.writeValueAsBytes(info), StandardCharsets.UTF_8));
        assertEquals(expected, new String(json.writeValueAsBytes(Argument.of(Info.class), info), StandardCharsets.UTF_8));
        var out = new ByteArrayOutputStream();
        json.writeValue(out, info);
        assertEquals(expected, out.toString(StandardCharsets.UTF_8));
        out.reset();
        json.writeValue(out, Argument.of(Info.class), info);
        assertEquals(expected, out.toString(StandardCharsets.UTF_8));

        JsonNode tree = json.writeValueToTree(Argument.of(Info.class), info);
        assertEquals("t", tree.get("title").getStringValue());
        assertSame(tree, json.writeValueToTree(tree));
        assertEquals(expected, json.writeValueAsString(tree));
        assertTrue(json.writeValueToTree(null).isNull());
        assertEquals("title: t\nversion: \"1\"\nx-a: 1\n", yaml.writeValueAsString(info));
    }

    @Test
    void convertsAllScalarTypesBetweenTrees() throws IOException {
        Map<String, JsonNode> values = new LinkedHashMap<>();
        values.put("int", JsonNode.createNumberNodeImpl(1));
        values.put("long", JsonNode.createNumberNodeImpl(2L));
        values.put("short", JsonNode.createNumberNodeImpl((short) 3));
        values.put("byte", JsonNode.createNumberNodeImpl((byte) 4));
        values.put("double", JsonNode.createNumberNodeImpl(5.5d));
        values.put("float", JsonNode.createNumberNodeImpl(6.5f));
        values.put("bigInteger", JsonNode.createNumberNodeImpl(BigInteger.TEN));
        values.put("bigDecimal", JsonNode.createNumberNodeImpl(new BigDecimal("1.50")));
        values.put("other", JsonNode.createNumberNodeImpl(new AtomicInteger(7)));
        values.put("bool", JsonNode.createBooleanNode(false));
        values.put("null", JsonNode.nullNode());
        values.put("array", JsonNode.createArrayNode(List.of(JsonNode.createStringNode("a"))));
        JsonNode tree = JsonNode.createObjectNode(values);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = json.readValueFromTree(tree, Argument.of(Map.class));

        assertEquals(1, map.get("int"));
        assertEquals(new BigDecimal("1.50"), new BigDecimal(map.get("bigDecimal").toString()));
        assertEquals(7, ((Number) map.get("other")).intValue());
        assertEquals(false, map.get("bool"));
        assertTrue(map.containsKey("null"));
        assertNull(map.get("null"));
        assertEquals(List.of("a"), map.get("array"));
        assertSame(tree, json.readValueFromTree(tree, Argument.of(JsonNode.class)));
        assertEquals("AQI=", json.writeValueToTree(new byte[] {1, 2}).getStringValue());
    }

    @Test
    void updatesValuesInPlaceOrReplacesThem() throws IOException {
        var stringSchema = new StringSchema();
        JsonNode description = JsonNode.createObjectNode(Map.of("description", JsonNode.createStringNode("d")));

        // StringSchema has a bean deserializer, which merges into the existing instance
        assertSame(stringSchema, json.updateValue(stringSchema, description));
        assertEquals("d", stringSchema.getDescription());

        // Schema has a custom deserializer that cannot merge, so a new instance is returned
        var schema = new Schema<>().title("kept?");
        Schema<?> updated = json.updateValue(schema, description);
        assertNotSame(schema, updated);
        assertEquals("d", updated.getDescription());

        var info = new Info();
        json.updateValueFromTree(info, JsonNode.createObjectNode(Map.of("title", JsonNode.createStringNode("t"))));
        assertEquals("t", info.getTitle());
    }

    @Test
    void providerReturnsAllMapperVariants() throws IOException {
        var provider = new JacksonOpenApiMapperProvider();
        for (OpenApiMapperType type : OpenApiMapperType.values()) {
            for (boolean openapi31 : new boolean[] {false, true}) {
                assertInstanceOf(JacksonOpenApiJsonMapper.class, provider.getMapper(type, openapi31));
            }
        }
        JsonMapper convert31 = provider.getMapper(OpenApiMapperType.CONVERT, true);
        assertSame(OpenApiUtils.CONVERT_JSON_MAPPER_31, ((JacksonOpenApiJsonMapper) convert31).getObjectMapper());
        assertSame(OpenApiUtils.YAML_MAPPER_31, ((JacksonOpenApiJsonMapper) provider.getMapper(OpenApiMapperType.YAML, true)).getObjectMapper());
    }
}
