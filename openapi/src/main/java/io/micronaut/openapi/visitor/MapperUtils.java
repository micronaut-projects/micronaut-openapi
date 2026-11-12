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
package io.micronaut.openapi.visitor;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helpers for using the {@link JsonMapper} of the selected {@link io.micronaut.openapi.mapper.OpenApiMapperProvider}.
 * Mapping failures are rethrown as {@link UncheckedIOException}.
 *
 * @since 8.0.0
 */
@Internal
public final class MapperUtils {

    private MapperUtils() {
    }

    public static <T> @Nullable T readValue(JsonMapper mapper, String content, Class<T> type) {
        return readValue(mapper, content, Argument.of(type));
    }

    public static <T> @Nullable T readValue(JsonMapper mapper, String content, Argument<T> type) {
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public static <T> @Nullable T readValue(JsonMapper mapper, Path path, Class<T> type) {
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readValue(in, Argument.of(type));
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public static JsonNode readTree(JsonMapper mapper, String content) {
        JsonNode node = readValue(mapper, content, JsonNode.class);
        return node != null ? node : JsonNode.nullNode();
    }

    public static JsonNode valueToTree(JsonMapper mapper, @Nullable Object value) {
        try {
            return mapper.writeValueToTree(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public static <T> @Nullable T treeToValue(JsonMapper mapper, JsonNode tree, Class<T> type) {
        return treeToValue(mapper, tree, Argument.of(type));
    }

    public static <T> @Nullable T treeToValue(JsonMapper mapper, JsonNode tree, Argument<T> type) {
        try {
            return mapper.readValueFromTree(tree, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public static <T> @Nullable T convertValue(JsonMapper mapper, @Nullable Object value, Class<T> type) {
        return convertValue(mapper, value, Argument.of(type));
    }

    public static <T> @Nullable T convertValue(JsonMapper mapper, @Nullable Object value, Argument<T> type) {
        return treeToValue(mapper, valueToTree(mapper, value), type);
    }

    public static String writeValueAsString(JsonMapper mapper, @Nullable Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public static void writeValue(JsonMapper mapper, Writer writer, @Nullable Object value) {
        try {
            writer.write(mapper.writeValueAsString(value));
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    /**
     * Updates a value from a tree, see {@link io.micronaut.openapi.mapper.OpenApiMapperProvider#updateValue}.
     *
     * @param mapper The mapper
     * @param valueToUpdate The value to update
     * @param tree The tree
     * @param <T> The value type
     * @return The updated value, possibly a new instance
     */
    public static <T> T updateValue(JsonMapper mapper, T valueToUpdate, JsonNode tree) {
        try {
            return Utils.getMapperProvider().updateValue(mapper, valueToUpdate, tree);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    /**
     * @param node A node, possibly {@code null}
     * @return The string value of a string node, the text of any other scalar node, otherwise {@code null}
     */
    public static @Nullable String stringValue(@Nullable JsonNode node) {
        if (node == null || node.isNull() || node.isContainerNode()) {
            return null;
        }
        return node.isString() ? node.getStringValue() : node.coerceStringValue();
    }

    /**
     * @param node An object node
     * @param name The property name
     * @return Whether the object has the property
     */
    public static boolean has(JsonNode node, String name) {
        return node.isObject() && node.get(name) != null;
    }

    /**
     * @param node An object node
     * @param names The properties to remove
     * @return A copy of the object without the given properties
     */
    public static JsonNode without(JsonNode node, String... names) {
        if (!node.isObject()) {
            return node;
        }
        Set<String> removed = Set.of(names);
        var values = new LinkedHashMap<String, JsonNode>();
        for (Map.Entry<String, JsonNode> entry : node.entries()) {
            if (!removed.contains(entry.getKey())) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        return JsonNode.createObjectNode(values);
    }

    /**
     * @param node An object node
     * @param name The property name
     * @param value The new value
     * @return A copy of the object with the property set to the value
     */
    public static JsonNode with(JsonNode node, String name, JsonNode value) {
        var values = new LinkedHashMap<String, JsonNode>();
        for (Map.Entry<String, JsonNode> entry : node.entries()) {
            values.put(entry.getKey(), entry.getValue());
        }
        values.put(name, value);
        return JsonNode.createObjectNode(values);
    }
}
