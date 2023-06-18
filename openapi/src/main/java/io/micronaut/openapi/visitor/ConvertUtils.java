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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.swagger.ObjectMapperFactory;
import io.micronaut.openapi.swagger.PrimitiveType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;

/**
 * Convert utilities methods.
 *
 * @since 4.4.1
 */
@Internal
public final class ConvertUtils {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };
    /**
     * The JSON mapper.
     */
    private static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The JSON 3.1 mapper.
     */
    private static final ObjectMapper JSON_MAPPER_31 = ObjectMapperFactory.createJson31()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The JSON mapper for security scheme.
     */
    private static final ObjectMapper CONVERT_JSON_MAPPER = ObjectMapperFactory.buildStrictGenericObjectMapper()
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS, SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The YAML mapper.
     */
    private static final ObjectMapper YAML_MAPPER = ObjectMapperFactory.createYaml();

    private ConvertUtils() {
    }

    public static Optional<Object> parseJsonString(Object object) {
        if (object instanceof String) {
            String string = (String) object;
            try {
                return Optional.of(ConvertUtils.getConvertJsonMapper().readValue(string, Map.class));
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Converts Json node into a class' instance or throws 'com.fasterxml.jackson.core.JsonProcessingException', adds extensions if present.
     *
     * @param jn The json node
     * @param clazz The output class instance
     * @param <T> The output class type
     * @param context visitor context
     *
     * @return The converted instance
     *
     * @throws JsonProcessingException if error
     */
    public static <T> T treeToValue(JsonNode jn, Class<T> clazz, VisitorContext context) throws JsonProcessingException {
        T value = CONVERT_JSON_MAPPER.treeToValue(jn, clazz);

        if (value == null) {
            return null;
        }

        resolveExtensions(jn).ifPresent(extensions -> BeanMap.of(value).put("extensions", extensions));
        String elType = jn.has("type") ? jn.get("type").textValue() : null;
        String elFormat = jn.has("format") ? jn.get("format").textValue() : null;
        JsonNode defaultValueNode = jn.get("defaultValue");
        // fix for default value
        Object defaultValue;
        try {
            defaultValue = ConvertUtils.normalizeValue(defaultValueNode != null ? defaultValueNode.textValue() : null, elType, elFormat, context);
        } catch (JsonProcessingException e) {
            defaultValue = defaultValueNode != null ? defaultValueNode.textValue() : null;
        }

        BeanMap<T> beanMap = BeanMap.of(value);
        if (defaultValue != null) {
            beanMap.put("default", defaultValue);
        }

        JsonNode allowableValuesNode = jn.get("allowableValues");
        if (allowableValuesNode != null && allowableValuesNode.isArray()) {
            List<Object> allowableValues = new ArrayList<>(allowableValuesNode.size());
            for (JsonNode allowableValueNode : allowableValuesNode) {
                if (allowableValueNode == null) {
                    continue;
                }
                try {
                    allowableValues.add(ConvertUtils.normalizeValue(allowableValueNode.textValue(), elType, elFormat, context));
                } catch (IOException e) {
                    allowableValues.add(allowableValueNode.textValue());
                }
            }
            beanMap.put("allowableValues", allowableValues);
        }

        return value;
    }

    public static Object normalizeValue(String valueStr, String type, String format, VisitorContext context) throws JsonProcessingException {
        return normalizeValue(valueStr, type, format, context, false);
    }

    public static Object normalizeValue(String valueStr, String type, String format, VisitorContext context, boolean isMicronautFormat) throws JsonProcessingException {
        if (valueStr == null) {
            return null;
        }
        if (type == null || type.equals(TYPE_OBJECT)) {
            return CONVERT_JSON_MAPPER.readValue(valueStr, Map.class);
        }
        return parseByTypeAndFormat(valueStr, type, format, context, isMicronautFormat);
    }

    public static Optional<Map<String, Object>> resolveExtensions(JsonNode jn) {
        try {
            JsonNode extensionsNode = jn.get("extensions");
            if (extensionsNode != null) {
                return Optional.ofNullable(CONVERT_JSON_MAPPER.convertValue(extensionsNode, MAP_TYPE_REFERENCE));
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

    public static void setDefaultValueObject(Schema<?> schema, String defaultValue, @Nullable Element element, @Nullable String schemaType, @Nullable String schemaFormat, boolean isMicronautFormat, VisitorContext context) {
        try {
            Pair<String, String> typeAndFormat;
            if (element instanceof EnumElement) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, (EnumElement) element, schemaType, schemaFormat);
            } else {
                typeAndFormat = Pair.of(schemaType, schemaFormat);
            }
            schema.setDefault(ConvertUtils.normalizeValue(defaultValue, typeAndFormat.getFirst(), typeAndFormat.getSecond(), context, isMicronautFormat));
        } catch (JsonProcessingException e) {
            context.warn("Can't convert " + defaultValue + " to " + schemaType + ": " + e.getMessage(), element);
            schema.setDefault(defaultValue);
        }
    }

    /**
     * Detect openapi type and format for enums.
     *
     * @param context visitor context
     * @param type enum element
     * @param schemaType type from swagger Schema annotation
     * @param schemaFormat format from swagger Schema annotation
     *
     * @return pair with openapi type and format
     */
    @NonNull
    public static Pair<String, String> checkEnumJsonValueType(VisitorContext context, @NonNull EnumElement type, @Nullable String schemaType, @Nullable String schemaFormat) {
        if (schemaType != null && !schemaType.equals(PrimitiveType.STRING.getCommonName())) {
            return Pair.of(schemaType, schemaFormat);
        }
        Pair<String, String> result = null;
        // check JsonValue method
        List<MethodElement> methods = type.getEnclosedElements(ElementQuery.ALL_METHODS.annotated(metadata -> metadata.isAnnotationPresent(JsonValue.class)));
        if (CollectionUtils.isNotEmpty(methods)) {
            MethodElement firstMethod = methods.get(0);
            if (methods.size() > 1) {
                context.warn("Found " + methods.size() + " methods with @JsonValue. Process method " + firstMethod, type);
            }
            ClassElement returnType = firstMethod.getReturnType();
            if (returnType.isEnum()) {
                return checkEnumJsonValueType(context, (EnumElement) returnType, null, null);
            }
            result = ConvertUtils.getTypeAndFormatByClass(returnType.getName(), firstMethod.getReturnType().isArray());
        }
        return result != null ? result : Pair.of(PrimitiveType.STRING.getCommonName(), schemaFormat);
    }

    /**
     * Detect openapi type and format by java class name.
     *
     * @param className java class name
     * @param isArray is it array
     *
     * @return pair with openapi type and format
     */
    public static Pair<String, String> getTypeAndFormatByClass(String className, boolean isArray) {
        if (className == null) {
            return Pair.of(TYPE_OBJECT, null);
        }

        if (String.class.getName().equals(className)
            || char.class.getName().equals(className)
            || Character.class.getName().equals(className)) {
            return Pair.of("string", null);
        } else if (Boolean.class.getName().equals(className)
            || boolean.class.getName().equals(className)) {
            return Pair.of("boolean", null);
        } else if (Integer.class.getName().equals(className)
            || int.class.getName().equals(className)
            || Short.class.getName().equals(className)
            || short.class.getName().equals(className)) {
            return Pair.of("integer", "int32");
        } else if (BigInteger.class.getName().equals(className)) {
            return Pair.of("integer", null);
        } else if (Long.class.getName().equals(className)
            || long.class.getName().equals(className)) {
            return Pair.of("integer", "int64");
        } else if (Float.class.getName().equals(className)
            || float.class.getName().equals(className)) {
            return Pair.of("number", "float");
        } else if (Double.class.getName().equals(className)
            || double.class.getName().equals(className)) {
            return Pair.of("number", "double");
        } else if (isArray && (Byte.class.getName().equals(className)
            || byte.class.getName().equals(className))) {
            return Pair.of("string", "byte");
            // swagger doesn't support type byte
        } else if (Byte.class.getName().equals(className)
            || byte.class.getName().equals(className)) {
            return Pair.of("integer", "int32");
        } else if (BigDecimal.class.getName().equals(className)) {
            return Pair.of("number", null);
        } else if (URI.class.getName().equals(className)) {
            return Pair.of("string", "uri");
        } else if (URL.class.getName().equals(className)) {
            return Pair.of("string", "url");
        } else if (UUID.class.getName().equals(className)) {
            return Pair.of("string", "uuid");
        } else if (Number.class.getName().equals(className)) {
            return Pair.of("number", null);
        } else if (File.class.getName().equals(className)) {
            return Pair.of("string", "binary");
        } else if (LocalDate.class.getName().equals(className)) {
            return Pair.of("string", "date");
        } else if (Date.class.getName().equals(className)
            || Calendar.class.getName().equals(className)
            || Instant.class.getName().equals(className)
            || LocalDateTime.class.getName().equals(className)
            || OffsetDateTime.class.getName().equals(className)
            || XMLGregorianCalendar.class.getName().equals(className)
            || ZonedDateTime.class.getName().equals(className)
        ) {
            return Pair.of("string", "date-time");
        } else if (LocalTime.class.getName().equals(className)) {
            return Pair.of("string", "partial-time");
        } else {
            return Pair.of(TYPE_OBJECT, null);
        }
    }

    /**
     * Parse value by openapi type and format.
     *
     * @param valueStr string value for parse
     * @param type openapi type
     * @param format openapi value
     * @param context visitor context
     * @param isMicronautFormat is it micronaut format for arrays
     *
     * @return parsed value
     */
    public static Object parseByTypeAndFormat(String valueStr, String type, String format, VisitorContext context, boolean isMicronautFormat) {
        if (valueStr == null) {
            return null;
        }

        // @QueryValue(defaultValue = "")
        if ("array".equals(type) && isMicronautFormat) {
            return valueStr.split(",");
        }

        if (valueStr.isEmpty()) {
            return null;
        }

        try {
            if ("string".equals(type)) {
                if ("uri".equals(format)) {
                    return new URI(valueStr);
                } else if ("url".equals(format)) {
                    return new URL(valueStr);
                } else if ("uuid".equals(format)) {
                    return UUID.fromString(valueStr);
                } else if (format == null) {
                    return valueStr;
                }
            } else if ("boolean".equals(type)) {
                return Boolean.parseBoolean(valueStr);
            } else if ("array".equals(type)) {
                return JSON_MAPPER.readValue(valueStr, List.class);
            } else if ("integer".equals(type)) {
                if ("int32".equals(format)) {
                    return Integer.parseInt(valueStr);
                } else if ("int64".equals(format)) {
                    return Long.parseLong(valueStr);
                } else {
                    return new BigInteger(valueStr);
                }
            } else if ("number".equals(type)) {
                if ("float".equals(format)) {
                    return Float.parseFloat(valueStr);
                } else if ("double".equals(format)) {
                    return Double.parseDouble(valueStr);
                } else {
                    return new BigDecimal(valueStr);
                }
            }
        } catch (Exception e) {
            context.warn("Can't parse value " + valueStr + " with type " + type + " and format " + format, null);
        }

        return valueStr;
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
}
