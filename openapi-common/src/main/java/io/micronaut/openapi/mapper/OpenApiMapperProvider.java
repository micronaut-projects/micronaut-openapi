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
package io.micronaut.openapi.mapper;

import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;

import java.io.IOException;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Service provider that supplies the {@link JsonMapper} implementations used by the OpenAPI visitor
 * to read, write and convert OpenAPI model objects.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader}. When more than one provider is
 * available, the one to use is selected with the {@value OpenApiMapperProviders#MAPPER_PROPERTY} property.</p>
 *
 * @since 8.0.0
 */
public interface OpenApiMapperProvider {

    /**
     * @return The provider name, used to select it with {@value OpenApiMapperProviders#MAPPER_PROPERTY}, for example {@code jackson} or {@code serde}.
     */
    String getName();

    /**
     * Returns the mapper for the given type and OpenAPI version.
     *
     * <p>The returned mapper must serialize the OpenAPI model classes (swagger-models) according to the OpenAPI
     * specification: extensions written as top-level {@code x-} properties, enums written by their specification
     * value and so on. Trees exchanged with the visitor are {@link io.micronaut.json.tree.JsonNode} instances.</p>
     *
     * @param type The mapper type
     * @param openapi31 Whether the document is OpenAPI 3.1
     * @return The mapper
     */
    JsonMapper getMapper(OpenApiMapperType type, boolean openapi31);

    /**
     * Updates an existing value from a tree and returns the result.
     *
     * <p>The result may be a different instance than {@code valueToUpdate}, for example when the type has a custom
     * deserializer that cannot merge into an existing instance. The default implementation updates the value in place
     * with {@link JsonMapper#updateValueFromTree(Object, JsonNode)} and returns it.</p>
     *
     * @param mapper A mapper obtained from {@link #getMapper(OpenApiMapperType, boolean)}
     * @param valueToUpdate The value to update
     * @param tree The tree to read
     * @param <T> The value type
     * @return The updated value
     * @throws IOException if the tree cannot be read
     */
    default <T> T updateValue(JsonMapper mapper, T valueToUpdate, JsonNode tree) throws IOException {
        mapper.updateValueFromTree(valueToUpdate, tree);
        return valueToUpdate;
    }

    /**
     * Resolves one of the well-known property naming strategies, such as {@code SNAKE_CASE} or {@code KEBAB_CASE}.
     *
     * @param name The upper case strategy name
     * @return The function translating a property name, or empty if the strategy is unknown
     */
    default Optional<UnaryOperator<String>> findNamingStrategy(String name) {
        return Optional.empty();
    }

    /**
     * Resolves a naming strategy declared by class name, for example from a {@code @JsonNaming} annotation.
     *
     * @param className The fully qualified class name of the strategy
     * @param classLoader The class loader to load it with
     * @return The function translating a property name, or empty if the strategy is not supported
     */
    default Optional<UnaryOperator<String>> findNamingStrategyClass(String className, ClassLoader classLoader) {
        return Optional.empty();
    }
}
