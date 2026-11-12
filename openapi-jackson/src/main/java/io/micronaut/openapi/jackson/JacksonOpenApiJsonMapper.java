/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.jackson;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.JsonStreamConfig;
import io.micronaut.json.tree.JsonNode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link JsonMapper} backed by a Jackson {@link ObjectMapper}.
 *
 * <p>Trees are exchanged as Micronaut {@link JsonNode} instances, converted through plain Java maps, lists and
 * scalars. Jackson exceptions are rethrown as {@link IOException}, matching the {@link JsonMapper} contract.</p>
 *
 * @since 8.0.0
 */
public final class JacksonOpenApiJsonMapper implements JsonMapper {

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper The Jackson mapper to delegate to
     */
    public JacksonOpenApiJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @return The underlying Jackson mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public <T> @Nullable T readValueFromTree(JsonNode tree, Argument<T> type) throws IOException {
        try {
            if (isTree(type)) {
                return type.getType().cast(tree);
            }
            // read the tree directly: converting a Map would serialize it first and drop explicit nulls
            return objectMapper.readerFor(javaType(type)).readValue(toJacksonTree(tree));
        } catch (JacksonException | IllegalArgumentException e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> @Nullable T readValue(InputStream inputStream, Argument<T> type) throws IOException {
        try {
            if (isTree(type)) {
                return type.getType().cast(fromJacksonTree(objectMapper.readTree(inputStream)));
            }
            return objectMapper.readValue(inputStream, javaType(type));
        } catch (JacksonException e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> @Nullable T readValue(byte[] byteArray, Argument<T> type) throws IOException {
        try {
            if (isTree(type)) {
                return type.getType().cast(fromJacksonTree(objectMapper.readTree(byteArray)));
            }
            return objectMapper.readValue(byteArray, javaType(type));
        } catch (JacksonException e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> @Nullable T readValue(String string, Argument<T> type) throws IOException {
        try {
            if (isTree(type)) {
                return type.getType().cast(fromJacksonTree(objectMapper.readTree(string)));
            }
            return objectMapper.readValue(string, javaType(type));
        } catch (JacksonException e) {
            throw wrap(e);
        }
    }

    @Override
    public JsonNode writeValueToTree(@Nullable Object value) throws IOException {
        if (value instanceof JsonNode node) {
            return node;
        }
        try {
            return fromJacksonTree(objectMapper.valueToTree(value));
        } catch (JacksonException | IllegalArgumentException e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> JsonNode writeValueToTree(Argument<T> type, @Nullable T value) throws IOException {
        return writeValueToTree(value);
    }

    @Override
    public void writeValue(OutputStream outputStream, @Nullable Object object) throws IOException {
        try {
            objectMapper.writeValue(outputStream, unwrapTree(object));
        } catch (JacksonException e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> void writeValue(OutputStream outputStream, Argument<T> type, @Nullable T object) throws IOException {
        writeValue(outputStream, object);
    }

    @Override
    public byte[] writeValueAsBytes(@Nullable Object object) throws IOException {
        try {
            return objectMapper.writeValueAsBytes(unwrapTree(object));
        } catch (JacksonException e) {
            throw wrap(e);
        }
    }

    @Override
    public <T> byte[] writeValueAsBytes(Argument<T> type, @Nullable T object) throws IOException {
        return writeValueAsBytes(object);
    }

    @Override
    public String writeValueAsString(@Nullable Object object) throws IOException {
        try {
            return objectMapper.writeValueAsString(unwrapTree(object));
        } catch (JacksonException e) {
            throw wrap(e);
        }
    }

    @Override
    public void updateValueFromTree(Object value, JsonNode tree) throws IOException {
        updateValue(value, tree);
    }

    /**
     * Updates a value from a tree with {@link ObjectMapper#readerForUpdating(Object)}.
     *
     * @param value The value to update
     * @param tree The tree
     * @param <T> The value type
     * @return The result, which is a new instance if the type's deserializer cannot merge into an existing one
     * @throws IOException if the tree cannot be read
     */
    public <T> T updateValue(T value, JsonNode tree) throws IOException {
        try {
            return objectMapper.readerForUpdating(value).readValue(toJacksonTree(tree));
        } catch (JacksonException | IllegalArgumentException e) {
            throw wrap(e);
        }
    }

    @Override
    public JsonStreamConfig getStreamConfig() {
        return JsonStreamConfig.DEFAULT;
    }

    private JavaType javaType(Argument<?> type) {
        return objectMapper.getTypeFactory().constructType(type.asType());
    }

    private static boolean isTree(Argument<?> type) {
        return JsonNode.class.isAssignableFrom(type.getType());
    }

    private @Nullable Object unwrapTree(@Nullable Object object) {
        return object instanceof JsonNode node ? toJacksonTree(node) : object;
    }

    /**
     * Converts a Micronaut tree into a Jackson tree, keeping explicit {@code null} values.
     *
     * @param node The Micronaut tree
     * @return The Jackson tree
     */
    private tools.jackson.databind.JsonNode toJacksonTree(@Nullable JsonNode node) {
        JsonNodeFactory factory = objectMapper.getNodeFactory();
        if (node == null || node.isNull()) {
            return factory.nullNode();
        }
        if (node.isObject()) {
            ObjectNode result = factory.objectNode();
            for (Map.Entry<String, JsonNode> entry : node.entries()) {
                result.set(entry.getKey(), toJacksonTree(entry.getValue()));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = factory.arrayNode(node.size());
            for (JsonNode value : node.values()) {
                result.add(toJacksonTree(value));
            }
            return result;
        }
        if (node.isString()) {
            return factory.stringNode(node.getStringValue());
        }
        if (node.isBoolean()) {
            return factory.booleanNode(node.getBooleanValue());
        }
        if (node.isNumber()) {
            return numberNode(factory, node.getNumberValue());
        }
        return objectMapper.valueToTree(node.getValue());
    }

    private static tools.jackson.databind.JsonNode numberNode(JsonNodeFactory factory, Number number) {
        return switch (number) {
            case Integer i -> factory.numberNode(i);
            case Long l -> factory.numberNode(l);
            case Short s -> factory.numberNode(s);
            case Byte b -> factory.numberNode(b);
            case Double d -> factory.numberNode(d);
            case Float f -> factory.numberNode(f);
            case BigInteger bi -> factory.numberNode(bi);
            case BigDecimal bd -> factory.numberNode(bd);
            default -> factory.numberNode(new BigDecimal(number.toString()));
        };
    }

    /**
     * Converts a Jackson tree into a Micronaut tree, keeping explicit {@code null} values.
     *
     * @param node The Jackson tree
     * @return The Micronaut tree
     */
    private JsonNode fromJacksonTree(tools.jackson.databind.@Nullable JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return JsonNode.nullNode();
        }
        if (node.isObject()) {
            var values = new LinkedHashMap<String, JsonNode>();
            for (Map.Entry<String, tools.jackson.databind.JsonNode> entry : node.properties()) {
                values.put(entry.getKey(), fromJacksonTree(entry.getValue()));
            }
            return JsonNode.createObjectNode(values);
        }
        if (node.isArray()) {
            var values = new ArrayList<JsonNode>(node.size());
            for (tools.jackson.databind.JsonNode value : node.values()) {
                values.add(fromJacksonTree(value));
            }
            return JsonNode.createArrayNode(values);
        }
        if (node.isString()) {
            return JsonNode.createStringNode(node.stringValue());
        }
        if (node.isBoolean()) {
            return JsonNode.createBooleanNode(node.booleanValue());
        }
        if (node.isNumber()) {
            return JsonNode.createNumberNodeImpl(node.numberValue());
        }
        // binary and POJO nodes: fall back to their plain Java value
        Object value = objectMapper.convertValue(node, Object.class);
        return value == null ? JsonNode.nullNode() : JsonNode.from(value);
    }

    private static IOException wrap(RuntimeException e) {
        return new IOException(e.getMessage(), e);
    }
}
