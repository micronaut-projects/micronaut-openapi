/*
 * Copyright 2017-2022 original authors
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.util.ArrayUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Convert utilities methods.
 *
 * @since 4.4.1
 */
public final class ConvertUtils {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };

    /**
     * The JSON mapper.
     */
    private static ObjectMapper jsonMapper = Json.mapper()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The JSON mapper for security scheme.
     */
    private static ObjectMapper convertJsonMapper = ObjectMapperFactory.buildStrictGenericObjectMapper()
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS, SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The YAML mapper.
     */
    private static ObjectMapper yamlMapper = Yaml.mapper();

    private ConvertUtils() {
    }

    /**
     * Converts Json node into a class' instance or throws 'com.fasterxml.jackson.core.JsonProcessingException', adds extensions if present.
     *
     * @param jn The json node
     * @param clazz The output class instance
     * @param <T> The output class type
     *
     * @return The converted instance
     *
     * @throws JsonProcessingException if error
     */
    public static <T> T treeToValue(JsonNode jn, Class<T> clazz) throws JsonProcessingException {
        T value = convertJsonMapper.treeToValue(jn, clazz);

        if (value == null) {
            return null;
        }

        resolveExtensions(jn).ifPresent(extensions -> BeanMap.of(value).put("extensions", extensions));
        String elType = jn.has("type") ? jn.get("type").textValue() : null;
        JsonNode defaultValueNode = jn.get("defaultValue");
        JsonNode allowableValuesNode = jn.get("allowableValues");
        // fix for default value
        Object defaultValue = convertJsonNodeValue(defaultValueNode, elType);
        BeanMap<T> beanMap = BeanMap.of(value);
        if (defaultValue != null) {
            beanMap.put("default", defaultValue);
        }
        if (allowableValuesNode != null && allowableValuesNode.isArray()) {
            List<Object> allowableValues = new ArrayList<>(allowableValuesNode.size());
            for (JsonNode allowableValueNode : allowableValuesNode) {
                allowableValues.add(convertJsonNodeValue(allowableValueNode, elType));
            }
            beanMap.put("allowableValues", allowableValues);
        }

        return value;
    }

    private static Object convertJsonNodeValue(JsonNode node, String type) throws JsonProcessingException {
        if (node == null) {
            return null;
        }
        return normalizeValue(node.textValue(), type);
    }

    public static List<Object> normalizeValues(String[] valuesStr, String type) throws JsonProcessingException {
        if (ArrayUtils.isEmpty(valuesStr)) {
            return null;
        }
        List<Object> values = new ArrayList<>(valuesStr.length);
        for (String valueStr : valuesStr) {
            Object normalizedValue = normalizeValue(valueStr, type);
            if (normalizedValue != null) {
                values.add(normalizeValue(valueStr, type));
            }
        }
        return values;
    }

    public static Object normalizeValue(String valueStr, String type) throws JsonProcessingException {
        if (valueStr == null) {
            return null;
        }
        if (type == null || type.equals("object")) {
            return convertJsonMapper.readValue(valueStr, Map.class);
        }
        PrimitiveType primitiveType = PrimitiveType.fromName(type);
        switch (primitiveType) {
            case INT:
                return Integer.parseInt(valueStr);
            case LONG:
                return Long.parseLong(valueStr);
            case FLOAT:
                return Float.parseFloat(valueStr);
            case DOUBLE:
                return Double.parseDouble(valueStr);
            case DECIMAL:
            case NUMBER:
                return new BigDecimal(valueStr);
            case INTEGER:
                return new BigInteger(valueStr);
            case BOOLEAN:
                return Boolean.parseBoolean(valueStr);
            default:
                return valueStr;
        }
    }

    public static Optional<Map<String, Object>> resolveExtensions(JsonNode jn) {
        try {
            JsonNode extensionsNode = jn.get("extensions");
            if (extensionsNode != null) {
                return Optional.ofNullable(convertJsonMapper.convertValue(extensionsNode, MAP_TYPE_REFERENCE));
            }
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        return Optional.empty();
    }

    /**
     * Maps annotation value to {@link io.swagger.v3.oas.annotations.security.SecurityRequirement}.
     * Correct format is:
     * custom_name:
     * - custom_scope1
     * - custom_scope2
     *
     * @param r The value of {@link SecurityRequirement}.
     *
     * @return converted object.
     */
    public static SecurityRequirement mapToSecurityRequirement(AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement> r) {
        String name = r.getRequiredValue("name", String.class);
        List<String> scopes = r.get("scopes", String[].class).map(Arrays::asList).orElse(Collections.emptyList());
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }

    public static ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    public static ObjectMapper getConvertJsonMapper() {
        return convertJsonMapper;
    }

    public static ObjectMapper getYamlMapper() {
        return yamlMapper;
    }
}
