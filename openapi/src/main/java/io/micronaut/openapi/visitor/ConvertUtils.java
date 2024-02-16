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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.micronaut.openapi.OpenApiUtils.CONVERT_JSON_MAPPER;
import static io.micronaut.openapi.OpenApiUtils.JSON_MAPPER;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;

/**
 * Convert utilities methods.
 *
 * @since 4.4.1
 */
@Internal
public final class ConvertUtils {

    public static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private ConvertUtils() {
    }

    /**
     * Convert the given Map to a JSON node and then to the specified type.
     *
     * @param <T> The output class type
     * @param values The values
     * @param context The visitor context
     * @param type The class
     *
     * @return The converted instance
     */
    public static <T> T toValue(Map<CharSequence, Object> values, VisitorContext context, Class<T> type) {
        JsonNode node = toJson(values, context);
        try {
            return ConvertUtils.treeToValue(node, type, context);
        } catch (JsonProcessingException e) {
            warn("Error converting  [" + node + "]: to " + type + ": " + e.getMessage(), context);
        }
        return null;
    }

    /**
     * Convert the given map to a JSON node.
     *
     * @param values The values
     * @param context The visitor context
     *
     * @return The node
     */
    public static JsonNode toJson(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = toValueMap(values, context);
        return JSON_MAPPER.valueToTree(newValues);
    }

    public static Map<CharSequence, Object> toValueMap(Map<CharSequence, Object> values, VisitorContext context) {
        var newValues = new HashMap<CharSequence, Object>(values.size());
        for (Map.Entry<CharSequence, Object> entry : values.entrySet()) {
            CharSequence key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AnnotationValue<?> av) {
                final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                newValues.put(key, valueMap);
            } else if (value instanceof AnnotationClassValue<?> acv) {
                acv.getType().ifPresent(aClass -> newValues.put(key, aClass));
            } else if (value != null) {
                if (value.getClass().isArray()) {
                    Object[] a = (Object[]) value;
                    if (ArrayUtils.isNotEmpty(a)) {
                        Object first = a[0];
                        boolean areAnnotationValues = first instanceof AnnotationValue;
                        boolean areClassValues = first instanceof AnnotationClassValue;

                        if (areClassValues) {
                            List<Class<?>> classes = new ArrayList<>(a.length);
                            for (Object o : a) {
                                AnnotationClassValue<?> acv = (AnnotationClassValue<?>) o;
                                acv.getType().ifPresent(classes::add);
                            }
                            newValues.put(key, classes);
                        } else if (areAnnotationValues) {
                            String annotationName = ((AnnotationValue<?>) first).getAnnotationName();
                            if (io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName().equals(annotationName)) {
                                List<SecurityRequirement> securityRequirements = new ArrayList<>(a.length);
                                for (Object o : a) {
                                    securityRequirements.add(ConvertUtils.mapToSecurityRequirement((AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>) o));
                                }
                                newValues.put(key, securityRequirements);
                            } else if (Extension.class.getName().equals(annotationName)) {
                                Map<CharSequence, Object> extensions = new HashMap<>();
                                for (Object o : a) {
                                    processExtensions(extensions, (AnnotationValue<Extension>) o);
                                }
                                newValues.put("extensions", extensions);
                            } else if (Server.class.getName().equals(annotationName)) {
                                List<Map<CharSequence, Object>> servers = new ArrayList<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    Map<CharSequence, Object> variables = new LinkedHashMap<>(toValueMap(sv.getValues(), context));
                                    servers.add(variables);
                                }
                                newValues.put(key, servers);
                            } else if (OAuthScope.class.getName().equals(annotationName)) {
                                Map<String, String> params = toTupleSubMap(a, "name", "description");
                                newValues.put(key, params);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> variables = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    sv.stringValue("name").ifPresent(name -> {
                                        Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                        Object dv = map.get("defaultValue");
                                        if (dv != null) {
                                            map.put("default", dv);
                                        }
                                        if (map.containsKey("allowableValues")) {
                                            // The key in the generated openapi needs to be "enum"
                                            map.put("enum", map.remove("allowableValues"));
                                        }
                                        variables.put(name, map);
                                    });
                                }
                                newValues.put(key, variables);
                            } else {
                                if (a.length == 1) {
                                    final AnnotationValue<?> av = (AnnotationValue<?>) a[0];
                                    final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                                    newValues.put(key, toValueMap(valueMap, context));
                                } else {

                                    List<Object> list = new ArrayList<>();
                                    for (Object o : a) {
                                        if (o instanceof AnnotationValue<?> av) {
                                            final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                                            list.add(valueMap);
                                        } else {
                                            list.add(o);
                                        }
                                    }
                                    newValues.put(key, list);
                                }
                            }
                        } else {
                            newValues.put(key, value);
                        }
                    } else {
                        newValues.put(key, a);
                    }
                } else {
                    newValues.put(key, parseJsonString(value).orElse(value));
                }
            }
        }
        return newValues;
    }

    public static Optional<Object> parseJsonString(Object object) {
        if (object instanceof String string) {
            try {
                return Optional.of(CONVERT_JSON_MAPPER.readValue(string, Map.class));
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

        T value;
        try {
            value = CONVERT_JSON_MAPPER.treeToValue(jn, clazz);
        } catch (Exception e) {
            // fix for problem with groovy. Jackson throw exception with ApiResponse class
            if (clazz == ApiResponse.class && jn.has("content")) {
                var contentNode = jn.get("content");
                ((ObjectNode) jn).set("content", null);
                value = CONVERT_JSON_MAPPER.treeToValue(jn, clazz);
                var result = new Content();
                if (contentNode.isArray()) {
                    for (var content : contentNode) {
                        processMediaType(result, content);
                    }
                } else {
                    processMediaType(result, contentNode);
                }
                ((ApiResponse) value).setContent(result);
            } else {
                throw e;
            }
        }

        if (value == null) {
            return null;
        }

        var finalValue = value;

        resolveExtensions(jn).ifPresent(extensions -> BeanMap.of(finalValue).put("extensions", extensions));
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

    private static void processMediaType(Content result, JsonNode content) throws JsonProcessingException {
        var mediaType = content.has("mediaType") ? content.get("mediaType").asText() : io.micronaut.http.MediaType.APPLICATION_JSON;
        var mediaTypeObj = CONVERT_JSON_MAPPER.treeToValue(content, MediaType.class);
        result.addMediaType(mediaType, mediaTypeObj);
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

    public static void addSecuritySchemes(OpenAPI openApi,
                                          List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityScheme>> values,
                                          VisitorContext context) {
        for (var securityRequirementAnnValue : values) {

            final Map<CharSequence, Object> map = toValueMap(securityRequirementAnnValue.getValues(), context);

            var name = securityRequirementAnnValue.stringValue("name").orElse(null);
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            if (map.containsKey("paramName")) {
                map.put("name", map.remove("paramName"));
            }

            Utils.normalizeEnumValues(map, CollectionUtils.mapOf("type", SecurityScheme.Type.class, "in", SecurityScheme.In.class));

            String type = (String) map.get("type");
            if (!SecurityScheme.Type.APIKEY.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, "name", context, false);
                removeAndWarnSecSchemeProp(map, "in", context);
            }
            if (!SecurityScheme.Type.OAUTH2.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, "flows", context);
            }
            if (!SecurityScheme.Type.OPENIDCONNECT.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, "openIdConnectUrl", context);
            }
            if (!SecurityScheme.Type.HTTP.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, "scheme", context);
                removeAndWarnSecSchemeProp(map, "bearerFormat", context);
            }

            if (SecurityScheme.Type.HTTP.toString().equals(type)) {
                if (!map.containsKey("scheme")) {
                    warn("Can't use http security scheme without 'scheme' property", context);
                } else if (!map.get("scheme").equals("bearer") && map.containsKey("bearerFormat")) {
                    warn("Should NOT have a `bearerFormat` property without `scheme: bearer` being set", context);
                }
            }

            if (map.containsKey("ref") || map.containsKey("$ref")) {
                Object ref = map.get("ref");
                if (ref == null) {
                    ref = map.get("$ref");
                }
                map.clear();
                map.put("$ref", ref);
            }

            try {
                JsonNode node = toJson(map, context);
                SecurityScheme securityScheme = ConvertUtils.treeToValue(node, SecurityScheme.class, context);
                if (securityScheme != null) {
                    resolveExtensions(node).ifPresent(extensions -> BeanMap.of(securityScheme).put("extensions", extensions));
                    resolveComponents(openApi).addSecuritySchemes(name, securityScheme);
                }
            } catch (JsonProcessingException e) {
                // ignore
            }
        }
    }

    private static void removeAndWarnSecSchemeProp(Map<CharSequence, Object> map, String prop, VisitorContext context) {
        removeAndWarnSecSchemeProp(map, prop, context, true);
    }

    private static void removeAndWarnSecSchemeProp(Map<CharSequence, Object> map, String prop, VisitorContext context, boolean withWarn) {
        if (map.containsKey(prop) && withWarn) {
            warn("'" + prop + "' property can't set for securityScheme with type " + map.get("type") + ". Skip it", context);
        }
        map.remove(prop);
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
        List<String> scopes = Arrays.asList(r.stringValues("scopes"));
        var securityRequirement = new SecurityRequirement();
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }

    public static void setDefaultValueObject(Schema<?> schema, String defaultValue, @Nullable Element element, @Nullable String schemaType, @Nullable String schemaFormat, boolean isMicronautFormat, VisitorContext context) {
        try {
            Pair<String, String> typeAndFormat;
            if (element instanceof EnumElement enumEl) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, schemaType, schemaFormat);
            } else {
                typeAndFormat = Pair.of(schemaType, schemaFormat);
            }
            schema.setDefault(ConvertUtils.normalizeValue(defaultValue, typeAndFormat.getFirst(), typeAndFormat.getSecond(), context, isMicronautFormat));
        } catch (JsonProcessingException e) {
            warn("Can't convert " + defaultValue + " to " + schemaType + ": " + e.getMessage(), context);
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
                warn("Found " + methods.size() + " methods with @JsonValue. Process method " + firstMethod, context, type);
            }
            ClassElement returnType = firstMethod.getReturnType();
            if (returnType.isEnum()) {
                return checkEnumJsonValueType(context, (EnumElement) returnType, null, null);
            }
            result = ConvertUtils.getTypeAndFormatByClass(returnType.getName(), returnType.isArray());
        }
        if (result == null) {
            // check JsonValue field
            var fields = type.getEnclosedElements(ElementQuery.ALL_FIELDS.annotated(metadata -> metadata.isAnnotationPresent(JsonValue.class)));
            if (CollectionUtils.isNotEmpty(fields)) {
                var firstField = fields.get(0);
                ClassElement fieldType = firstField.getType();
                if (fieldType.isEnum()) {
                    return checkEnumJsonValueType(context, (EnumElement) fieldType, null, null);
                }
                result = ConvertUtils.getTypeAndFormatByClass(fieldType.getName(), fieldType.isArray());
            }
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
            warn("Can't parse value " + valueStr + " with type " + type + " and format " + format, context);
        }

        return valueStr;
    }

    public static Map<String, String> toTupleSubMap(Object[] a, String entryKey, String entryValue) {
        var params = new LinkedHashMap<String, String>();
        for (Object o : a) {
            AnnotationValue<?> sv = (AnnotationValue<?>) o;
            final Optional<String> n = sv.stringValue(entryKey);
            final Optional<String> expr = sv.stringValue(entryValue);
            if (n.isPresent() && expr.isPresent()) {
                params.put(n.get(), expr.get());
            }
        }
        return params;
    }
}
