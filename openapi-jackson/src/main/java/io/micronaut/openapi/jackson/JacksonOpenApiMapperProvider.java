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

import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.mapper.OpenApiMapperProvider;
import io.micronaut.openapi.mapper.OpenApiMapperType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * {@link OpenApiMapperProvider} backed by Jackson, using the mappers from {@link OpenApiUtils}.
 *
 * @since 8.0.0
 */
public final class JacksonOpenApiMapperProvider implements OpenApiMapperProvider {

    /**
     * The provider name.
     */
    public static final String NAME = "jackson";

    private final JsonMapper json = new JacksonOpenApiJsonMapper(OpenApiUtils.JSON_MAPPER);
    private final JsonMapper json31 = new JacksonOpenApiJsonMapper(OpenApiUtils.JSON_MAPPER_31);
    private final JsonMapper yaml = new JacksonOpenApiJsonMapper(OpenApiUtils.YAML_MAPPER);
    private final JsonMapper yaml31 = new JacksonOpenApiJsonMapper(OpenApiUtils.YAML_MAPPER_31);
    private final JsonMapper convert = new JacksonOpenApiJsonMapper(OpenApiUtils.CONVERT_JSON_MAPPER);
    private final JsonMapper convert31 = new JacksonOpenApiJsonMapper(OpenApiUtils.CONVERT_JSON_MAPPER_31);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JsonMapper getMapper(OpenApiMapperType type, boolean openapi31) {
        return switch (type) {
            case JSON -> openapi31 ? json31 : json;
            case YAML -> openapi31 ? yaml31 : yaml;
            case CONVERT -> openapi31 ? convert31 : convert;
        };
    }

    @Override
    public <T> T updateValue(JsonMapper mapper, T valueToUpdate, JsonNode tree) throws IOException {
        if (mapper instanceof JacksonOpenApiJsonMapper jacksonMapper) {
            return jacksonMapper.updateValue(valueToUpdate, tree);
        }
        return OpenApiMapperProvider.super.updateValue(mapper, valueToUpdate, tree);
    }

    @Override
    public Optional<UnaryOperator<String>> findNamingStrategy(String name) {
        PropertyNamingStrategy strategy = switch (name.toUpperCase(Locale.ENGLISH)) {
            case "LOWER_CAMEL_CASE" -> PropertyNamingStrategies.LOWER_CAMEL_CASE;
            case "UPPER_CAMEL_CASE" -> PropertyNamingStrategies.UPPER_CAMEL_CASE;
            case "SNAKE_CASE" -> PropertyNamingStrategies.SNAKE_CASE;
            case "UPPER_SNAKE_CASE" -> PropertyNamingStrategies.UPPER_SNAKE_CASE;
            case "LOWER_CASE" -> PropertyNamingStrategies.LOWER_CASE;
            case "KEBAB_CASE" -> PropertyNamingStrategies.KEBAB_CASE;
            case "LOWER_DOT_CASE" -> PropertyNamingStrategies.LOWER_DOT_CASE;
            default -> null;
        };
        return Optional.ofNullable(strategy).map(JacksonOpenApiMapperProvider::toFunction);
    }

    @Override
    public Optional<UnaryOperator<String>> findNamingStrategyClass(String className, ClassLoader classLoader) {
        Object strategy;
        try {
            strategy = Class.forName(className, true, classLoader).getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate: " + className, e);
        }
        // Only name-based strategies can be applied without a full Jackson mapper configuration
        if (strategy instanceof PropertyNamingStrategies.NamingBase namingBase) {
            return Optional.of(toFunction(namingBase));
        }
        return Optional.empty();
    }

    private static UnaryOperator<String> toFunction(PropertyNamingStrategy strategy) {
        return name -> strategy.nameForField(null, null, name);
    }
}
