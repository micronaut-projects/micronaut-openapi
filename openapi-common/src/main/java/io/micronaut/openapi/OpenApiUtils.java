/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.openapi;

import io.micronaut.openapi.swagger.core.util.ObjectMapperFactory;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;

/**
 * Convert utilities methods.
 *
 * @since 4.4.1
 */
public final class OpenApiUtils {

    public static final String OPENAPI_31_VERSION = "3.1.0";

    /**
     * The JSON mapper.
     */
    public static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson()
        .rebuild()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        .build();
    /**
     * The JSON 3.1 mapper.
     */
    public static final ObjectMapper JSON_MAPPER_31 = ObjectMapperFactory.createJson31()
        .rebuild()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        .build();
    /**
     * The JSON mapper for security scheme.
     */
    public static final ObjectMapper CONVERT_JSON_MAPPER = ObjectMapperFactory.buildStrictGenericObjectMapper()
        .rebuild()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
        .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        .build();
    /**
     * The YAML mapper.
     */
    public static final ObjectMapper YAML_MAPPER = ObjectMapperFactory.createYaml();
    /**
     * The YAML 3.1 mapper.
     */
    public static final ObjectMapper YAML_MAPPER_31 = ObjectMapperFactory.createYaml31();

    private OpenApiUtils() {
    }

    public static ObjectMapper getJsonMapper() {
        return JSON_MAPPER;
    }

    public static ObjectMapper getJsonMapper31() {
        return JSON_MAPPER_31;
    }

    public static ObjectMapper getConvertJsonMapper() {
        return CONVERT_JSON_MAPPER;
    }

    public static ObjectMapper getYamlMapper() {
        return YAML_MAPPER;
    }

    public static ObjectMapper getYamlMapper31() {
        return YAML_MAPPER_31;
    }
}
