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

import io.micronaut.core.annotation.Experimental;
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
@Experimental
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
     * @param version The OpenAPI version of the document
     * @return The mapper
     */
    JsonMapper getMapper(OpenApiMapperType type, OpenApiVersion version);

    /**
     * Updates an existing value from a tree and returns the result.
     *
     * <p>The visitor uses this instead of {@link JsonMapper#updateValueFromTree(Object, JsonNode)} because the result
     * may be a different instance than {@code valueToUpdate}: with Jackson, a type whose deserializer cannot merge into
     * an existing instance (such as {@code Schema}) is read into a new one, and the visitor must continue with that.
     * The default implementation updates the value in place with {@code updateValueFromTree} and returns it, which is
     * what a provider whose deserializers always merge should do.</p>
     *
     * @param mapper A mapper obtained from {@link #getMapper(OpenApiMapperType, OpenApiVersion)}
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
     * @return The function translating a property name, or empty if the provider cannot apply that strategy, in which
     * case the visitor warns and keeps the property names unchanged
     */
    default Optional<UnaryOperator<String>> findNamingStrategyClass(String className, ClassLoader classLoader) {
        return Optional.empty();
    }
}
