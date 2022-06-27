/*
 * Copyright 2017-2020 original authors
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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.server.types.files.FileCustomizableResponseType;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.javadoc.JavadocParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static java.util.stream.Collectors.toMap;

/**
 * Abstract base class for OpenAPI visitors.
 *
 * @author graemerocher
 * @since 1.0
 */
abstract class AbstractOpenApiVisitor {

    static final String ATTR_OPENAPI = "io.micronaut.OPENAPI";
    static OpenAPI testReference;
    static OpenAPI testReferenceAfterPlaceholders;
    static String testYamlReference;
    static String testJsonReference;

    private static final String ATTR_TEST_MODE = "io.micronaut.OPENAPI_TEST";
    private static final Lock VISITED_ELEMENTS_LOCK = new ReentrantLock();
    private static final String ATTR_VISITED_ELEMENTS = "io.micronaut.OPENAPI.visited.elements";
    private static final Schema<?> EMPTY_SCHEMA = new Schema<>();
    private static final ComposedSchema EMPTY_COMPOSED_SCHEMA = new ComposedSchema();

    /**
     * The JSON mapper.
     */
    ObjectMapper jsonMapper = Json.mapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The JSON mapper for security scheme.
     */
    ObjectMapper jsonMapperForSecurityScheme = ObjectMapperFactory.buildStrictGenericObjectMapper()
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS, SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The YAML mapper.
     */
    ObjectMapper yamlMapper = Yaml.mapper();

    /**
     * Stores the current in progress type.
     */
    private List<String> inProgressSchemas = new ArrayList<>(10);

    /**
     * {@link PropertyNamingStrategy} instances cache.
     */
    private Map<String, PropertyNamingStrategy> propertyNamingStrategyInstances = new HashMap<>();


    /**
     * Increments the number of visited elements.
     *
     * @param context The context
     */
    void incrementVisitedElements(VisitorContext context) {
        VISITED_ELEMENTS_LOCK.lock();
        try {
            context.put(ATTR_VISITED_ELEMENTS, getVisitedElements(context) + 1);
        } finally {
            VISITED_ELEMENTS_LOCK.unlock();
        }

    }

    /**
     * Returns the number of visited elements.
     *
     * @param context The context.
     *
     * @return The number of visited elements.
     */
    int visitedElements(VisitorContext context) {
        VISITED_ELEMENTS_LOCK.lock();
        try {
            return getVisitedElements(context);
        } finally {
            VISITED_ELEMENTS_LOCK.unlock();
        }
    }

    private static Integer getVisitedElements(VisitorContext context) {
        Integer visitedElements = context.get(ATTR_VISITED_ELEMENTS, Integer.class).orElse(null);
        if (visitedElements == null) {
            visitedElements = 0;
            context.put(ATTR_VISITED_ELEMENTS, visitedElements);
        }
        return visitedElements;
    }

    /**
     * Convert the given map to a JSON node.
     *
     * @param values The values
     * @param context The visitor context
     *
     * @return The node
     */
    JsonNode toJson(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = toValueMap(values, context);
        return jsonMapper.valueToTree(newValues);
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
    <T> Optional<T> toValue(Map<CharSequence, Object> values, VisitorContext context, Class<T> type) {
        JsonNode node = toJson(values, context);
        try {
            return Optional.of(treeToValue(node, type));
        } catch (JsonProcessingException e) {
            context.warn("Error converting  [" + node + "]: to " + type + ": " + e.getMessage(), null);
        }
        return Optional.empty();
    }

    /**
     * Reads the security requirements annotation of the specified element.
     *
     * @param element The Element to process.
     *
     * @return A list of SecurityRequirement
     */
    List<SecurityRequirement> readSecurityRequirements(Element element) {
        return readSecurityRequirements(element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.security.SecurityRequirement.class));
    }

    List<SecurityRequirement> readSecurityRequirements(List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> annotations) {
        return annotations
                .stream()
                .map(this::mapToSecurityRequirement)
                .collect(Collectors.toList());
    }

    /**
     * Resolve the PathItem for the given {@link UriMatchTemplate}.
     *
     * @param context The context
     * @param matchTemplates The match templates
     *
     * @return The {@link PathItem}
     */
    List<PathItem> resolvePathItems(VisitorContext context, List<UriMatchTemplate> matchTemplates) {
        OpenAPI openAPI = resolveOpenAPI(context);
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            paths = new Paths();
            openAPI.setPaths(paths);
        }

        List<PathItem> resultPaths = new ArrayList<>();

        for (UriMatchTemplate matchTemplate : matchTemplates) {

            StringBuilder result = new StringBuilder();

            boolean varProcess = false;
            boolean valueProcess = false;
            boolean isFirstVarChar = true;
            boolean needToSkip = false;
            final String pathString = matchTemplate.toPathString();
            for (char c : pathString.toCharArray()) {
                if (varProcess) {
                    if (isFirstVarChar) {
                        isFirstVarChar = false;
                        if (c == '?' || c == '.') {
                            needToSkip = true;
                            result.deleteCharAt(result.length() - 1);
                            continue;
                        } else if (c == '+' || c == '0') {
                            continue;
                        } else if (c == '/') {
                            result.deleteCharAt(result.length() - 1).append(c).append('{');
                            continue;
                        }
                    }
                    if (c == ':') {
                        valueProcess = true;
                        continue;
                    }
                    if (c == '}') {
                        varProcess = false;
                        valueProcess = false;
                        if (!needToSkip) {
                            result.append('}');
                        }
                        needToSkip = false;
                        continue;
                    }
                    if (valueProcess || needToSkip) {
                        continue;
                    }
                }
                if (c == '{') {
                    varProcess = true;
                    isFirstVarChar = true;
                }
                result.append(c);
            }
            resultPaths.add(paths.computeIfAbsent(result.toString(), key -> new PathItem()));
        }

        return resultPaths;
    }

    /**
     * Resolve the {@link OpenAPI} instance.
     *
     * @param context The context
     *
     * @return The {@link OpenAPI} instance
     */
    OpenAPI resolveOpenAPI(VisitorContext context) {
        OpenAPI openAPI = context.get(ATTR_OPENAPI, OpenAPI.class).orElse(null);
        if (openAPI == null) {
            openAPI = new OpenAPI();
            context.put(ATTR_OPENAPI, openAPI);
            if (isTestMode()) {
                testReference = openAPI;
            }
        }
        return openAPI;
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
    protected <T> T treeToValue(JsonNode jn, Class<T> clazz) throws JsonProcessingException {
        T value = jsonMapper.treeToValue(jn, clazz);
        if (value != null) {
            resolveExtensions(jn).ifPresent(extensions -> BeanMap.of(value).put("extensions", extensions));
            // fix for default value
            if (jn.has("defaultValue")) {
                BeanMap.of(value).put("default", jsonMapper.treeToValue(jn.get("defaultValue"), Map.class));
            }
        }
        return value;
    }

    /**
     * Convert the values to a map.
     *
     * @param values The values
     * @param context The visitor context
     *
     * @return The map
     */
    protected Map<CharSequence, Object> toValueMap(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = new HashMap<>(values.size());
        for (Map.Entry<CharSequence, Object> entry : values.entrySet()) {
            CharSequence key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AnnotationValue) {
                AnnotationValue<?> av = (AnnotationValue<?>) value;
                if (av.getAnnotationName().equals(io.swagger.v3.oas.annotations.media.ArraySchema.class.getName())) {
                    final Map<CharSequence, Object> valueMap = resolveArraySchemaAnnotationValues(context, av);
                    newValues.put("schema", valueMap);
                } else {
                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av);
                    newValues.put(key, valueMap);
                }
            } else if (value instanceof AnnotationClassValue) {
                AnnotationClassValue<?> acv = (AnnotationClassValue<?>) value;
                final Optional<? extends Class<?>> type = acv.getType();
                type.ifPresent(aClass -> newValues.put(key, aClass));
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
                                    securityRequirements.add(mapToSecurityRequirement((AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>) o));
                                }
                                newValues.put(key, securityRequirements);
                            } else if (Extension.class.getName().equals(annotationName)) {
                                Map<CharSequence, Object> extensions = new HashMap<>();
                                for (Object o : a) {
                                    processExtensions(extensions, (AnnotationValue<Extension>) o);
                                }
                                newValues.put("extensions", extensions);
                            } else if (Encoding.class.getName().equals(annotationName)) {
                                Map<String, Object> encodings = annotationValueArrayToSubmap(a, "name", context);
                                newValues.put(key, encodings);
                            } else if (Content.class.getName().equals(annotationName)) {
                                Map<String, Object> mediaTypes = annotationValueArrayToSubmap(a, "mediaType", context);
                                newValues.put(key, mediaTypes);
                            } else if (Link.class.getName().equals(annotationName) || Header.class.getName().equals(annotationName)) {
                                Map<String, Object> links = annotationValueArrayToSubmap(a, "name", context);
                                newValues.put(key, links);
                            } else if (LinkParameter.class.getName().equals(annotationName)) {
                                Map<String, String> params = toTupleSubMap(a, "name", "expression");
                                newValues.put(key, params);
                            } else if (OAuthScope.class.getName().equals(annotationName)) {
                                Map<String, String> params = toTupleSubMap(a, "name", "description");
                                newValues.put(key, params);
                            } else if (ApiResponse.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> responses = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ApiResponse> sv = (AnnotationValue<ApiResponse>) o;
                                    String name = sv.get("responseCode", String.class).orElse("default");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                    responses.put(name, map);
                                }
                                newValues.put(key, responses);
                            } else if (ExampleObject.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> examples = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ExampleObject> sv = (AnnotationValue<ExampleObject>) o;
                                    String name = sv.get("name", String.class).orElse("example");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                    examples.put(name, map);
                                }
                                newValues.put(key, examples);
                            } else if (Server.class.getName().equals(annotationName)) {
                                List<Map<CharSequence, Object>> servers = new ArrayList<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    Map<CharSequence, Object> variables = new LinkedHashMap<>(toValueMap(sv.getValues(), context));
                                    servers.add(variables);
                                }
                                newValues.put(key, servers);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> variables = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    Optional<String> n = sv.get("name", String.class);
                                    n.ifPresent(name -> {
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
                            } else if (DiscriminatorMapping.class.getName().equals(annotationName)) {
                                final Map<String, String> mappings = new HashMap<>();
                                for (Object o : a) {
                                    final AnnotationValue<DiscriminatorMapping> dv = (AnnotationValue<DiscriminatorMapping>) o;
                                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, dv);
                                    mappings.put(valueMap.get("value").toString(), valueMap.get("$ref").toString());
                                }
                                final Map<String, Object> discriminatorMap = getDiscriminatorMap(newValues);
                                discriminatorMap.put("mapping", mappings);
                                newValues.put("discriminator", discriminatorMap);
                            } else {
                                if (a.length == 1) {
                                    final AnnotationValue<?> av = (AnnotationValue<?>) a[0];
                                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av);
                                    newValues.put(key, toValueMap(valueMap, context));
                                } else {

                                    List<Object> list = new ArrayList<>();
                                    for (Object o : a) {
                                        if (o instanceof AnnotationValue) {
                                            final AnnotationValue<?> av = (AnnotationValue<?>) o;
                                            final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av);
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
                } else if (key.equals("discriminatorProperty")) {
                    final Map<String, Object> discriminatorMap = getDiscriminatorMap(newValues);
                    discriminatorMap.put("propertyName", parseJsonString(value).orElse(value));
                    newValues.put("discriminator", discriminatorMap);
                } else if (key.equals("style")) {
                    newValues.put(key, io.swagger.v3.oas.models.media.Encoding.StyleEnum.valueOf((String) value).toString());
                } else if (key.equals("accessMode")) {
                    if (io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY.toString().equals(value)) {
                        newValues.put("readOnly", Boolean.TRUE);
                    } else if (io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY.toString().equals(value)) {
                        newValues.put("writeOnly", Boolean.TRUE);
                    }
                } else {
                    newValues.put(key, parseJsonString(value).orElse(value));
                }
            }
        }
        return newValues;
    }

    private Map<String, Object> getDiscriminatorMap(Map<CharSequence, Object> newValues) {
        return newValues.containsKey("discriminator") ? (Map<String, Object>) newValues.get("discriminator") : new HashMap<>();
    }

    // Copy of io.swagger.v3.core.util.AnnotationsUtils.getExtensions
    private void processExtensions(Map<CharSequence, Object> map, AnnotationValue<Extension> extension) {
        String name = extension.stringValue("name").orElse(StringUtils.EMPTY_STRING);
        final String key = !name.isEmpty() ? org.apache.commons.lang3.StringUtils.prependIfMissing(name, "x-") : name;
        for (AnnotationValue<ExtensionProperty> prop : extension.getAnnotations("properties", ExtensionProperty.class)) {
            final String propertyName = prop.getRequiredValue("name", String.class);
            final String propertyValue = prop.getRequiredValue(String.class);
            JsonNode processedValue;
            final boolean propertyAsJson = prop.get("parseValue", boolean.class, false);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(propertyName) && org.apache.commons.lang3.StringUtils.isNotBlank(propertyValue)) {
                if (key.isEmpty()) {
                    if (propertyAsJson) {
                        try {
                            processedValue = Json.mapper().readTree(propertyValue);
                            map.put(org.apache.commons.lang3.StringUtils.prependIfMissing(propertyName, "x-"), processedValue);
                        } catch (Exception e) {
                            map.put(org.apache.commons.lang3.StringUtils.prependIfMissing(propertyName, "x-"), propertyValue);
                        }
                    } else {
                        map.put(org.apache.commons.lang3.StringUtils.prependIfMissing(propertyName, "x-"), propertyValue);
                    }
                } else {
                    Object value = map.get(key);
                    if (!(value instanceof Map)) {
                        value = new LinkedHashMap<>();
                        map.put(key, value);
                    }
                    @SuppressWarnings("unchecked") final Map<String, Object> mapValue = (Map<String, Object>) value;
                    if (propertyAsJson) {
                        try {
                            processedValue = Json.mapper().readTree(propertyValue);
                            mapValue.put(propertyName, processedValue);
                        } catch (Exception e) {
                            mapValue.put(propertyName, propertyValue);
                        }
                    } else {
                        mapValue.put(propertyName, propertyValue);
                    }
                }
            }
        }
    }

    private Optional<Object> parseJsonString(Object object) {
        if (object instanceof String) {
            String string = (String) object;
            try {
                return Optional.of(jsonMapper.readValue(string, Map.class));
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private <T extends Schema> void processAnnotationValue(VisitorContext context, AnnotationValue<?> annotationValue, Map<CharSequence, Object> arraySchemaMap, List<String> filters, Class<T> type) {
        Map<CharSequence, Object> values = annotationValue.getValues().entrySet().stream()
                .filter(entry -> filters == null || !filters.contains((String) entry.getKey()))
                .collect(toMap(
                        e -> e.getKey().equals("requiredProperties") ? "required" : e.getKey(), Map.Entry::getValue));
        Optional<T> schema = toValue(values, context, type);
        schema.ifPresent(s -> schemaToValueMap(arraySchemaMap, s));
    }

    private Map<CharSequence, Object> resolveArraySchemaAnnotationValues(VisitorContext context, AnnotationValue<?> av) {
        final Map<CharSequence, Object> arraySchemaMap = new HashMap<>(10);
        // properties
        av.get("arraySchema", AnnotationValue.class).ifPresent(annotationValue ->
                processAnnotationValue(context, (AnnotationValue<?>) annotationValue, arraySchemaMap, Arrays.asList("ref", "implementation"), Schema.class)
        );
        // items
        av.get("schema", AnnotationValue.class).ifPresent(annotationValue -> {
            Optional<String> impl = annotationValue.get("implementation", String.class);
            Optional<String> type = annotationValue.get("type", String.class);
            Optional<String> format = annotationValue.get("format", String.class);
            Optional<ClassElement> classElement = Optional.empty();
            PrimitiveType primitiveType = null;
            if (impl.isPresent()) {
                classElement = context.getClassElement(impl.get());
            } else if (type.isPresent()) {
                // if format is "binary", we want PrimitiveType.BINARY
                primitiveType = PrimitiveType.fromName(format.isPresent() && format.get().equals("binary") ? format.get() : type.get());
                if (primitiveType == null) {
                    classElement = context.getClassElement(type.get());
                } else {
                    classElement = context.getClassElement(primitiveType.getKeyClass());
                }
            }
            if (classElement.isPresent()) {
                if (primitiveType == null) {
                    final ArraySchema schema = arraySchema(resolveSchema(null, classElement.get(), context, Collections.emptyList()));
                    schemaToValueMap(arraySchemaMap, schema);
                } else {
                    // For primitive type, just copy description field is present.
                    final Schema items = primitiveType.createProperty();
                    items.setDescription((String) annotationValue.get("description", String.class).orElse(null));
                    final ArraySchema schema = arraySchema(items);
                    schemaToValueMap(arraySchemaMap, schema);
                }
            } else {
                arraySchemaMap.putAll(resolveAnnotationValues(context, annotationValue));
            }
        });
        // other properties (minItems,...)
        processAnnotationValue(context, av, arraySchemaMap, Arrays.asList("schema", "arraySchema"), ArraySchema.class);
        return arraySchemaMap;
    }

    private Map<CharSequence, Object> resolveAnnotationValues(VisitorContext context, AnnotationValue<?> av) {
        final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
        bindSchemaIfNeccessary(context, av, valueMap);
        final String annotationName = av.getAnnotationName();
        if (Parameter.class.getName().equals(annotationName)) {
            normalizeEnumValues(valueMap, CollectionUtils.mapOf(
                    "in", ParameterIn.class,
                    "style", ParameterStyle.class
            ));
        }
        return valueMap;
    }

    private Map<String, String> toTupleSubMap(Object[] a, String entryKey, String entryValue) {
        Map<String, String> params = new LinkedHashMap<>();
        for (Object o : a) {
            AnnotationValue<?> sv = (AnnotationValue<?>) o;
            final Optional<String> n = sv.get(entryKey, String.class);
            final Optional<String> expr = sv.get(entryValue, String.class);
            if (n.isPresent() && expr.isPresent()) {
                params.put(n.get(), expr.get());
            }
        }
        return params;
    }

    private boolean isTypeNullable(ClassElement type) {
        return type.isAssignable("java.util.Optional");
    }

    /**
     * Resolves the schema for the given type element.
     *
     * @param definingElement The defining element
     * @param type The type element
     * @param context The context
     * @param mediaTypes An optional media type
     *
     * @return The schema or null if it cannot be resolved
     */
    protected @Nullable Schema resolveSchema(@Nullable Element definingElement, ClassElement type, VisitorContext context, List<MediaType> mediaTypes) {
        return resolveSchema(resolveOpenAPI(context), definingElement, type, context, mediaTypes);
    }

    /**
     * Resolves the schema for the given type element.
     *
     * @param openAPI The OpenAPI object
     * @param definingElement The defining element
     * @param type The type element
     * @param context The context
     * @param mediaTypes An optional media type
     *
     * @return The schema or null if it cannot be resolved
     */
    protected @Nullable Schema resolveSchema(OpenAPI openAPI, @Nullable Element definingElement, ClassElement type, VisitorContext context, List<MediaType> mediaTypes) {
        Schema schema = null;

        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotationValue = null;
        if (definingElement != null) {
            schemaAnnotationValue = definingElement.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        if (type != null && schemaAnnotationValue == null) {
            schemaAnnotationValue = type.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        if (schemaAnnotationValue != null) {
            type = schemaAnnotationValue
                    .stringValue("implementation")
                    .flatMap(context::getClassElement)
                    .orElse(type);
        }

        if (type instanceof EnumElement) {
            schema = getSchemaDefinition(openAPI, context, type, definingElement, mediaTypes);
        } else {

            boolean isPublisher = false;
            boolean isObservable = false;
            boolean isNullable = false;

            // StreamingFileUpload implements Publisher, but it should be not considered as a Publisher in the spec file
            if (!type.isAssignable("io.micronaut.http.multipart.StreamingFileUpload") && isContainerType(type)) {
                isPublisher = type.isAssignable(Publisher.class.getName()) && !type.isAssignable("reactor.core.publisher.Mono");
                isObservable = type.isAssignable("io.reactivex.Observable") && !type.isAssignable("reactor.core.publisher.Mono");
                type = type.getFirstTypeArgument().orElse(null);
            } else if (isTypeNullable(type)) {
                isNullable = true;
                type = type.getFirstTypeArgument().orElse(null);
            }

            if (type != null) {

                String typeName = type.getName();
                // File upload case
                if ("io.micronaut.http.multipart.StreamingFileUpload".equals(typeName) ||
                        "io.micronaut.http.multipart.CompletedFileUpload".equals(typeName) ||
                        "io.micronaut.http.multipart.CompletedPart".equals(typeName) ||
                        "io.micronaut.http.multipart.PartData".equals(typeName)) {
                    isPublisher = isPublisher && !"io.micronaut.http.multipart.PartData".equals(typeName);
                    // For file upload, we use PrimitiveType.BINARY
                    typeName = PrimitiveType.BINARY.name();
                }
                PrimitiveType primitiveType = PrimitiveType.fromName(typeName);
                if (!type.isArray() && ClassUtils.isJavaLangType(typeName)) {
                    schema = getPrimitiveType(typeName);
                } else if (!type.isArray() && primitiveType != null) {
                    schema = primitiveType.createProperty();
                } else if (type.isAssignable(Map.class.getName())) {
                    schema = new MapSchema();
                    if (type.getTypeArguments().isEmpty()) {
                        schema.setAdditionalProperties(true);
                    } else {
                        ClassElement valueType = type.getTypeArguments().get("V");
                        if (valueType.getName().equals(Object.class.getName())) {
                            schema.setAdditionalProperties(true);
                        } else {
                            schema.setAdditionalProperties(resolveSchema(openAPI, type, valueType, context, mediaTypes));
                        }
                    }
                } else if (type.isIterable()) {
                    if (type.isArray()) {
                        schema = resolveSchema(openAPI, type, type.fromArray(), context, mediaTypes);
                    } else {
                        Optional<ClassElement> componentType = type.getFirstTypeArgument();
                        if (componentType.isPresent()) {
                            schema = resolveSchema(openAPI, type, componentType.get(), context, mediaTypes);
                        } else {
                            schema = getPrimitiveType(Object.class.getName());
                        }
                    }
                    if (schema != null) {
                        schema = arraySchema(schema);
                    }
                } else if (isReturnTypeFile(type)) {
                    schema = new StringSchema();
                    schema.setFormat("binary");
                } else if (type.isAssignable(UUID.class)) {
                    schema = new UUIDSchema();
                } else {
                    schema = getSchemaDefinition(openAPI, context, type, definingElement, mediaTypes);
                }
            }

            if (schema != null) {
                boolean isStream = false;
                for (MediaType mediaType : mediaTypes) {
                    if (MediaType.TEXT_EVENT_STREAM_TYPE.equals(mediaType) || MediaType.APPLICATION_JSON_STREAM_TYPE.equals(mediaType)) {
                        isStream = true;
                        break;
                    }
                }

                if (!isStream && (isPublisher || isObservable)) {
                    schema = arraySchema(schema);
                } else if (isNullable) {
                    schema.setNullable(true);
                }
            }
        }
        return schema;
    }

    /**
     * Resolve the components.
     *
     * @param openAPI The open API
     *
     * @return The components
     */
    protected Components resolveComponents(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            components = new Components();
            openAPI.setComponents(components);
        }
        return components;
    }

    private void handleUnwrapped(VisitorContext context, Element element, ClassElement elementType, Schema parentSchema, AnnotationValue<JsonUnwrapped> uw) {
        Map<String, Schema> schemas = resolveSchemas(resolveOpenAPI(context));
        String schemaName = element.stringValue(io.swagger.v3.oas.annotations.media.Schema.class, "name").orElse(computeDefaultSchemaName(null, elementType));
        Schema wrappedPropertySchema = schemas.get(schemaName);
        Map<String, Schema> properties = wrappedPropertySchema.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        String prefix = uw.stringValue("prefix").orElse("");
        String suffix = uw.stringValue("suffix").orElse("");
        for (Entry<String, Schema> prop : properties.entrySet()) {
            try {
                String propertyName = prop.getKey();
                Schema propertySchema = prop.getValue();
                boolean isRequired = wrappedPropertySchema.getRequired() != null && wrappedPropertySchema.getRequired().contains(propertyName);
                if (StringUtils.isNotEmpty(suffix) || StringUtils.isNotEmpty(prefix)) {
                    propertyName = prefix + propertyName + suffix;
                    propertySchema = jsonMapper.readValue(Json.pretty(prop.getValue()), Schema.class);
                    propertySchema.setName(propertyName);
                }
                addProperty(parentSchema, propertyName, propertySchema, isRequired);
            } catch (IOException e) {
                context.warn("Exception cloning property " + e.getMessage(), null);
            }
        }
    }

    /**
     * Processes a schema property.
     *
     * @param context The visitor context
     * @param element The element
     * @param elementType The element type
     * @param classElement The class element
     * @param parentSchema The parent schema
     * @param propertySchema The property schema
     */
    protected void processSchemaProperty(VisitorContext context, Element element, ClassElement elementType, @Nullable Element classElement, Schema parentSchema, Schema propertySchema) {
        if (propertySchema != null) {
            AnnotationValue<JsonUnwrapped> uw = element.getAnnotation(JsonUnwrapped.class);
            if (uw != null && uw.booleanValue("enabled").orElse(Boolean.TRUE)) {
                handleUnwrapped(context, element, elementType, parentSchema, uw);
            } else {
                // check schema required flag
                AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotationValue = element.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                Optional<Boolean> elementSchemaRequired = Optional.empty();
                boolean isRequiredDefaultValueSet = false;
                if (schemaAnnotationValue != null) {
                    elementSchemaRequired = schemaAnnotationValue.get("required", Argument.of(Boolean.TYPE));
                    isRequiredDefaultValueSet = !schemaAnnotationValue.contains("required");
                }

                // check field annotaions (@NonNull, @Nullable, etc.)
                boolean isNotNullable = isElementNotNullable(element, classElement);
                // check as mandatory in constructor
                boolean isMandatoryInConstructor = doesParamExistsMandatoryInConstructor(element, classElement);
                boolean required = elementSchemaRequired.orElse(isNotNullable || isMandatoryInConstructor);

                if (isRequiredDefaultValueSet && isNotNullable) {
                    required = true;
                }

                propertySchema = bindSchemaForElement(context, element, elementType, propertySchema);
                String propertyName = resolvePropertyName(element, classElement, propertySchema);
                propertySchema.setRequired(null);
                addProperty(parentSchema, propertyName, propertySchema, required);
                if (schemaAnnotationValue != null) {
                    schemaAnnotationValue.get("defaultValue", String.class)
                            .ifPresent(parentSchema::setDefault);
                }
            }
        }
    }

    private boolean isElementNotNullable(Element element, @Nullable Element classElement) {
        return element.isAnnotationPresent(NotNull.class)
                || element.isAnnotationPresent(NotBlank.class)
                || element.isAnnotationPresent(NotEmpty.class)
                || element.isNonNull()
                || element.booleanValue(JsonProperty.class, "required").orElse(false);
    }

    private boolean doesParamExistsMandatoryInConstructor(Element element, @Nullable Element classElement) {
        if (classElement instanceof ClassElement) {
            return ((ClassElement) classElement).getPrimaryConstructor().flatMap(methodElement -> Arrays.stream(methodElement.getParameters())
                            .filter(parameterElement -> parameterElement.getName().equals(element.getName()))
                            .map(parameterElement -> !parameterElement.isNullable())
                            .findFirst())
                    .orElse(false);
        }

        return false;
    }

    private void addProperty(Schema parentSchema, String name, Schema propertySchema, boolean required) {
        parentSchema.addProperty(name, propertySchema);
        if (required) {
            List<String> requiredList = parentSchema.getRequired();
            // Check for duplicates
            if (requiredList == null || !requiredList.contains(name)) {
                parentSchema.addRequiredItem(name);
            }
        }
    }

    private String resolvePropertyName(Element element, Element classElement, Schema propertySchema) {
        String name = Optional.ofNullable(propertySchema.getName()).orElse(element.getName());

        if (element.hasAnnotation(io.swagger.v3.oas.annotations.media.Schema.class)) {
            return element.stringValue(io.swagger.v3.oas.annotations.media.Schema.class, "name").orElse(name);
        }
        if (element.hasAnnotation(JsonProperty.class)) {
            return element.stringValue(JsonProperty.class, "value").orElse(name);
        }
        if (classElement != null && classElement.hasAnnotation(JsonNaming.class)) {
            // INVESTIGATE: "classValue" doesn't work in this case
            Optional<String> propertyNamingStrategyClass = classElement.stringValue(JsonNaming.class);
            if (!propertyNamingStrategyClass.isPresent()) {
                return name;
            }
            PropertyNamingStrategy strategy = propertyNamingStrategyInstances.computeIfAbsent(propertyNamingStrategyClass.get(), clazz -> {
                try {
                    return (PropertyNamingStrategy) Class.forName(propertyNamingStrategyClass.get()).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Cannot instantiate: " + clazz);
                }
            });
            if (strategy instanceof PropertyNamingStrategies.NamingBase) {
                return ((PropertyNamingStrategies.NamingBase) strategy).translate(name);
            }
        }
        return name;
    }

    /**
     * Binds the schema for the given element.
     *
     * @param context The context
     * @param element The element
     * @param elementType The element type
     * @param schemaToBind The schema to bind
     *
     * @return The bound schema
     */
    protected Schema bindSchemaForElement(VisitorContext context, Element element, ClassElement elementType, Schema schemaToBind) {
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn = element.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        Schema originalSchema = schemaToBind;
        if (originalSchema.get$ref() != null) {
            schemaToBind = new Schema();
            if (schemaAnn != null) {
                Optional<String> schemaDescription = schemaAnn.get("description", String.class);
                if (schemaDescription.isPresent()) {
                    schemaToBind.setDescription(schemaDescription.get());
                }
                Optional<String> schemaFormat = schemaAnn.get("format", String.class);
                if (schemaFormat.isPresent()) {
                    schemaToBind.setFormat(schemaFormat.get());
                }
                Optional<String> schemaTitle = schemaAnn.get("title", String.class);
                if (schemaTitle.isPresent()) {
                    schemaToBind.setTitle(schemaTitle.get());
                }
                Optional<BigDecimal> schemaMinimum = schemaAnn.get("minimum", BigDecimal.class);
                if (schemaMinimum.isPresent()) {
                    schemaToBind.setMinimum(schemaMinimum.get());
                }
                Optional<BigDecimal> schemaMaximum = schemaAnn.get("maximum", BigDecimal.class);
                if (schemaMaximum.isPresent()) {
                    schemaToBind.setMaximum(schemaMaximum.get());
                }
                Optional<Boolean> schemaExclusiveMinimum = schemaAnn.get("exclusiveMinimum", Boolean.class);
                if (schemaExclusiveMinimum.isPresent()) {
                    schemaToBind.setExclusiveMinimum(schemaExclusiveMinimum.get());
                }
                Optional<Boolean> schemaExclusiveMaximum = schemaAnn.get("exclusiveMaximum", Boolean.class);
                if (schemaExclusiveMaximum.isPresent()) {
                    schemaToBind.setExclusiveMaximum(schemaExclusiveMaximum.get());
                }
                Optional<Integer> schemaMinLength = schemaAnn.get("minLength", Integer.class);
                if (schemaMinLength.isPresent()) {
                    schemaToBind.setMinLength(schemaMinLength.get());
                }
                Optional<Integer> schemaMaxLength = schemaAnn.get("maxLength", Integer.class);
                if (schemaMaxLength.isPresent()) {
                    schemaToBind.setMaxLength(schemaMaxLength.get());
                }
                Optional<Integer> schemaMinProperties = schemaAnn.get("minProperties", Integer.class);
                if (schemaMinProperties.isPresent()) {
                    schemaToBind.setMinProperties(schemaMinProperties.get());
                }
                Optional<Integer> schemaMaxProperties = schemaAnn.get("maxProperties", Integer.class);
                if (schemaMaxProperties.isPresent()) {
                    schemaToBind.setMaxProperties(schemaMaxProperties.get());
                }
                Optional<BigDecimal> schemaMultipleOf = schemaAnn.get("multipleOf", BigDecimal.class);
                if (schemaMultipleOf.isPresent()) {
                    schemaToBind.setMultipleOf(schemaMultipleOf.get());
                }
                Optional<String> schemaPattern = schemaAnn.get("pattern", String.class);
                if (schemaPattern.isPresent()) {
                    schemaToBind.setPattern(schemaPattern.get());
                }

                Optional<AnnotationValue<io.swagger.v3.oas.annotations.ExternalDocumentation>> schemaExtDocs = schemaAnn.getAnnotation("externalDocs", io.swagger.v3.oas.annotations.ExternalDocumentation.class);
                ExternalDocumentation externalDocs = null;
                if (schemaExtDocs.isPresent()) {
                    externalDocs = toValue(schemaExtDocs.get().getValues(), context, ExternalDocumentation.class).orElse(null);
                }
                if (externalDocs != null) {
                    schemaToBind.setExternalDocs(externalDocs);
                }
                Optional<String> schemaDefaultValue = schemaAnn.get("defaultValue", String.class);
                if (schemaDefaultValue.isPresent()) {
                    try {
                        schemaToBind.setDefault(jsonMapper.readValue(schemaDefaultValue.get(), Map.class));
                    } catch (JsonProcessingException e) {
                        schemaToBind.setDefault(schemaDefaultValue.get());
                    }
                }
                Optional<String> schemaExample = schemaAnn.get("example", String.class);
                if (schemaExample.isPresent()) {
                    try {
                        schemaToBind.setExample(jsonMapper.readValue(schemaExample.get(), Map.class));
                    } catch (JsonProcessingException e) {
                        schemaToBind.setExample(schemaExample.get());
                    }
                }
                Optional<Boolean> schemaDeprecated = schemaAnn.get("deprecated", Boolean.class);
                if (schemaDeprecated.isPresent()) {
                    schemaToBind.setDeprecated(schemaDeprecated.get());
                }
                Optional<io.swagger.v3.oas.annotations.media.Schema.AccessMode> schemaAccessMode = schemaAnn.get("accessMode", io.swagger.v3.oas.annotations.media.Schema.AccessMode.class);
                if (schemaAccessMode.isPresent()) {
                    if (schemaAccessMode.get() == io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY) {
                        schemaToBind.setReadOnly(true);
                        schemaToBind.setWriteOnly(null);
                    } else if (schemaAccessMode.get() == io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY) {
                        schemaToBind.setReadOnly(false);
                        schemaToBind.setWriteOnly(null);
                    } else if (schemaAccessMode.get() == io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_WRITE) {
                        schemaToBind.setReadOnly(null);
                        schemaToBind.setWriteOnly(null);
                    }
                }
            }
        }
        if (originalSchema.get$ref() == null && schemaAnn != null) {
            // Apply @Schema annotation only if not $ref since for $ref schemas
            // we already populated values from right @Schema annotation in previous steps
            schemaToBind = bindSchemaAnnotationValue(context, element, schemaToBind, schemaAnn);
            Optional<String> schemaName = schemaAnn.get("name", String.class);
            if (schemaName.isPresent()) {
                schemaToBind.setName(schemaName.get());
            }
            elementType = schemaAnn
                    .stringValue("implementation")
                    .flatMap(context::getClassElement)
                    .orElse(elementType);
        }
        AnnotationValue<io.swagger.v3.oas.annotations.media.ArraySchema> arraySchemaAnn = element.getAnnotation(io.swagger.v3.oas.annotations.media.ArraySchema.class);
        if (arraySchemaAnn != null) {
            schemaToBind = bindArraySchemaAnnotationValue(context, element, schemaToBind, arraySchemaAnn);
            Optional<String> schemaName = arraySchemaAnn.get("name", String.class);
            if (schemaName.isPresent()) {
                schemaToBind.setName(schemaName.get());
            }
        }

        Schema finalSchemaToBind = schemaToBind;
        final boolean isIterableOrMap = elementType.isIterable() || elementType.isAssignable(Map.class);

        if (isIterableOrMap) {
            if (element.isAnnotationPresent(NotEmpty.class)) {
                finalSchemaToBind.setMinItems(1);
            }
            element.getValue(Size.class, "min", Integer.class).ifPresent(finalSchemaToBind::setMinItems);
            element.getValue(Size.class, "max", Integer.class).ifPresent(finalSchemaToBind::setMaxItems);
        } else {
            if ("string".equals(finalSchemaToBind.getType())) {
                if (element.isAnnotationPresent(NotEmpty.class) || element.isAnnotationPresent(NotBlank.class)) {
                    finalSchemaToBind.setMinLength(1);
                }
                element.getValue(Size.class, "min", Integer.class).ifPresent(finalSchemaToBind::setMinLength);
                element.getValue(Size.class, "max", Integer.class).ifPresent(finalSchemaToBind::setMaxLength);
            }

            if (element.isAnnotationPresent(Negative.class)) {
                finalSchemaToBind.setMaximum(BigDecimal.ZERO);
            }
            if (element.isAnnotationPresent(NegativeOrZero.class)) {
                finalSchemaToBind.setMaximum(BigDecimal.ZERO);
            }
            if (element.isAnnotationPresent(Positive.class)) {
                finalSchemaToBind.setMinimum(BigDecimal.ZERO);
            }
            if (element.isAnnotationPresent(PositiveOrZero.class)) {
                finalSchemaToBind.setMinimum(BigDecimal.ZERO);
            }
            element.getValue(Max.class, BigDecimal.class).ifPresent(finalSchemaToBind::setMaximum);
            element.getValue(Min.class, BigDecimal.class).ifPresent(finalSchemaToBind::setMinimum);
            element.getValue(DecimalMax.class, BigDecimal.class).ifPresent(finalSchemaToBind::setMaximum);
            element.getValue(DecimalMin.class, BigDecimal.class).ifPresent(finalSchemaToBind::setMinimum);
            if (element.isAnnotationPresent(Email.class)) {
                finalSchemaToBind.setFormat("email");
            }

            element.findAnnotation(Pattern.class).flatMap(p -> p.get("regexp", String.class)).ifPresent(finalSchemaToBind::setPattern);
            element.getValue(Part.class, String.class).ifPresent(finalSchemaToBind::setName);
        }

        final ComposedSchema composedSchema;
        final Schema<?> topLevelSchema;
        if (originalSchema.get$ref() != null) {
            composedSchema = new ComposedSchema();
            topLevelSchema = composedSchema;
        } else {
            composedSchema = null;
            topLevelSchema = schemaToBind;
        }

        setSchemaDocumentation(element, topLevelSchema);
        if (element.isAnnotationPresent(Deprecated.class)) {
            topLevelSchema.setDeprecated(true);
        }
        final String defaultValue = element.getValue(Bindable.class, "defaultValue", String.class).orElse(null);
        if (defaultValue != null && schemaToBind.getDefault() == null) {
            topLevelSchema.setDefault(defaultValue);
        }
        // @Schema annotation takes priority over nullability annotations
        Boolean isSchemaNullable = element.booleanValue(io.swagger.v3.oas.annotations.media.Schema.class, "nullable").orElse(null);
        boolean isNullable = (isSchemaNullable == null && (element.isNullable() || isTypeNullable(elementType)))
                || Boolean.TRUE.equals(isSchemaNullable);
        if (isNullable) {
            topLevelSchema.setNullable(true);
        }
        final String defaultJacksonValue = element.stringValue(JsonProperty.class, "defaultValue").orElse(null);
        if (defaultJacksonValue != null && schemaToBind.getDefault() == null) {
            topLevelSchema.setDefault(defaultJacksonValue);
        }

        if (composedSchema != null) {
            boolean addSchemaToBind = !schemaToBind.equals(EMPTY_SCHEMA);

            if (addSchemaToBind) {
                composedSchema.addAllOfItem(originalSchema);
            } else if (isNullable && (composedSchema.getAllOf() == null || composedSchema.getAllOf().isEmpty())) {
                composedSchema.addOneOfItem(originalSchema);
            }
            if (addSchemaToBind) {
                composedSchema.addAllOfItem(schemaToBind);
            }

            if (!composedSchema.equals(EMPTY_COMPOSED_SCHEMA)) {
                return composedSchema;
            }
        }

        return originalSchema;
    }

    private void setSchemaDocumentation(Element element, Schema schemaToBind) {
        if (StringUtils.isEmpty(schemaToBind.getDescription())) {
            Optional<String> documentation = element.getDocumentation();
            String doc = documentation.orElse(null);
            if (doc != null) {
                JavadocDescription desc = new JavadocParser().parse(doc);
                schemaToBind.setDescription(desc.getMethodDescription());
            }
        }
    }

    /**
     * Binds the schema for the given element.
     *
     * @param context The context
     * @param element The element
     * @param schemaToBind The schema to bind
     * @param schemaAnn The schema annotation
     *
     * @return The bound schema
     */
    protected Schema bindSchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind, AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {
        JsonNode schemaJson = toJson(schemaAnn.getValues(), context);
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, schemaAnn.get("defaultValue", String.class).orElse(null),
                schemaAnn.get("allowableValues", String[].class).orElse(null));
    }

    private Schema doBindSchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind,
                                               JsonNode schemaJson, String defaultValue, String... allowableValues) {
        try {
            schemaToBind = jsonMapper.readerForUpdating(schemaToBind).readValue(schemaJson);
            if (StringUtils.isNotEmpty(defaultValue)) {
                schemaToBind.setDefault(defaultValue);
            }
            if (ArrayUtils.isNotEmpty(allowableValues)) {
                for (String allowableValue : allowableValues) {
                    if (schemaToBind.getEnum() == null || !schemaToBind.getEnum().contains(allowableValue)) {
                        schemaToBind.addEnumItemObject(allowableValue);
                    }
                }
            }
        } catch (IOException e) {
            context.warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), element);
        }
        return schemaToBind;
    }

    /**
     * Binds the array schema for the given element.
     *
     * @param context The context
     * @param element The element
     * @param schemaToBind The schema to bind
     * @param schemaAnn The schema annotation
     *
     * @return The bound schema
     */
    protected Schema bindArraySchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind, AnnotationValue<io.swagger.v3.oas.annotations.media.ArraySchema> schemaAnn) {
        JsonNode schemaJson = toJson(schemaAnn.getValues(), context);
        if (schemaJson.isObject()) {
            ObjectNode objNode = (ObjectNode) schemaJson;
            JsonNode arraySchema = objNode.remove("arraySchema");
            // flatten
            if (arraySchema != null && arraySchema.isObject()) {
                ((ObjectNode) arraySchema).remove("implementation");
                objNode.setAll((ObjectNode) arraySchema);
            }
            // remove schema that maps to 'items'
            JsonNode items = objNode.remove("schema");
            if (items != null && schemaToBind instanceof ArraySchema && ((ArraySchema) schemaToBind).getItems() != null) {
                ArraySchema arrSchemaToBind = (ArraySchema) schemaToBind;
                // if it has no $ref add properties, otherwise we are good
                if (arrSchemaToBind.getItems().get$ref() == null) {
                    try {
                        arrSchemaToBind.items(jsonMapper.readerForUpdating(arrSchemaToBind.getItems()).readValue(items));
                    } catch (IOException e) {
                        context.warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), element);
                    }
                }
            }
        }
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, null);
    }

    private Optional<Map<String, Object>> resolveExtensions(JsonNode jn) {
        try {
            JsonNode extensionsNode = jn.get("extensions");
            if (extensionsNode != null) {
                return Optional.ofNullable(jsonMapper.treeToValue(extensionsNode, Map.class));
            }
        } catch (JsonProcessingException e) {
            // Ignore
        }
        return Optional.empty();
    }

    private Map<String, Object> annotationValueArrayToSubmap(Object[] a, String classifier, VisitorContext context) {
        Map<String, Object> mediaTypes = new LinkedHashMap<>();
        for (Object o : a) {
            AnnotationValue<?> sv = (AnnotationValue<?>) o;
            String name = sv.get(classifier, String.class).orElse(null);
            if (name == null && classifier.equals("mediaType")) {
                name = MediaType.APPLICATION_JSON;
            }
            if (name != null) {
                Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                mediaTypes.put(name, map);
            }
        }
        return mediaTypes;
    }

    private void schemaToValueMap(Map<CharSequence, Object> valueMap, Schema schema) {
        if (schema != null) {
            final BeanMap<Schema> beanMap = BeanMap.of(schema);
            for (Map.Entry<String, Object> e : beanMap.entrySet()) {
                final Object v = e.getValue();
                if (v != null) {
                    valueMap.put(e.getKey(), v);
                }
            }
            if (schema.get$ref() != null) {
                valueMap.put("$ref", schema.get$ref());
            }
        }
    }

    private void bindSchemaIfNeccessary(VisitorContext context, AnnotationValue<?> av, Map<CharSequence, Object> valueMap) {
        final Optional<String> impl = av.get("implementation", String.class);
        final Optional<String> schema = av.get("schema", String.class);
        final Optional<String[]> anyOf = av.get("anyOf", Argument.of(String[].class));
        final Optional<String[]> oneOf = av.get("oneOf", Argument.of(String[].class));
        final Optional<String[]> allOf = av.get("allOf", Argument.of(String[].class));
        // remap keys.
        Object o = valueMap.remove("defaultValue");
        if (o != null) {
            valueMap.put("default", o);
        }
        o = valueMap.remove("allowableValues");
        if (o != null) {
            valueMap.put("enum", o);
        }
        boolean isSchema = io.swagger.v3.oas.annotations.media.Schema.class.getName().equals(av.getAnnotationName());
        if (isSchema && impl.isPresent()) {
            final String className = impl.get();
            bindSchemaForClassName(context, valueMap, className);
        }
        if (DiscriminatorMapping.class.getName().equals(av.getAnnotationName()) && schema.isPresent()) {
            final String className = schema.get();
            bindSchemaForClassName(context, valueMap, className);
        }
        if (isSchema && (anyOf.isPresent() || oneOf.isPresent() || allOf.isPresent())) {
            anyOf.ifPresent(anyOfList -> bindSchemaForComposite(context, valueMap, anyOfList, "anyOf"));
            oneOf.ifPresent(oneOfList -> bindSchemaForComposite(context, valueMap, oneOfList, "oneOf"));
            allOf.ifPresent(allOfList -> bindSchemaForComposite(context, valueMap, allOfList, "allOf"));
        }
    }

    private void bindSchemaForComposite(VisitorContext context, Map<CharSequence, Object> valueMap, String[] classNames, String key) {
        final List<Map<CharSequence, Object>> namesToSchemas = Arrays.stream(classNames).map(className -> {
            final Optional<ClassElement> classElement = context.getClassElement(className);
            Map<CharSequence, Object> schemaMap = new HashMap<>();
            if (classElement.isPresent()) {
                final Schema schema = resolveSchema(null, classElement.get(), context, Collections.emptyList());
                schemaToValueMap(schemaMap, schema);
            }
            return schemaMap;
        }).collect(Collectors.toList());
        valueMap.put(key, namesToSchemas);
    }

    private void bindSchemaForClassName(VisitorContext context, Map<CharSequence, Object> valueMap, String className) {
        final Optional<ClassElement> classElement = context.getClassElement(className);
        if (classElement.isPresent()) {
            final Schema schema = resolveSchema(null, classElement.get(), context, Collections.emptyList());
            schemaToValueMap(valueMap, schema);
        }
    }

    private void checkAllOf(ComposedSchema composedSchema) {
        if (composedSchema != null && composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty() && composedSchema.getProperties() != null
                && !composedSchema.getProperties().isEmpty()) {
            // put all properties as siblings of allOf
            ObjectSchema propSchema = new ObjectSchema();
            propSchema.properties(composedSchema.getProperties());
            propSchema.setDescription(composedSchema.getDescription());
            propSchema.setRequired(composedSchema.getRequired());
            composedSchema.setProperties(null);
            composedSchema.setDescription(null);
            composedSchema.setRequired(null);
            composedSchema.setType(null);
            composedSchema.addAllOfItem(propSchema);
        }
    }

    private Schema getSchemaDefinition(
            OpenAPI openAPI,
            VisitorContext context,
            ClassElement type,
            @Nullable Element definingElement,
            List<MediaType> mediaTypes) {
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue = definingElement == null ? null : definingElement.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (schemaValue == null) {
            schemaValue = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        Schema schema;
        Map<String, Schema> schemas = resolveSchemas(openAPI);
        if (schemaValue == null) {
            final boolean isBasicType = ClassUtils.isJavaBasicType(type.getName());
            final PrimitiveType primitiveType;
            if (isBasicType) {
                primitiveType = ClassUtils.forName(type.getName(), getClass().getClassLoader()).map(PrimitiveType::fromType).orElse(null);
            } else {
                primitiveType = null;
            }
            if (primitiveType == null) {
                String schemaName = computeDefaultSchemaName(definingElement, type);
                schema = schemas.get(schemaName);
                if (schema == null) {

                    if (type instanceof EnumElement) {
                        schema = new Schema();
                        schema.setName(schemaName);
                        schemas.put(schemaName, schema);

                        schema.setType("string");
                        schema.setEnum(((EnumElement) type).values());
                    } else {
                        if (type instanceof TypedElement) {
                            ClassElement classElement = ((TypedElement) type).getType();
                            Optional<ClassElement> superType = classElement == null ? Optional.empty() : classElement.getSuperType();
                            if (superType.isPresent() && !type.isRecord()) {
                                schema = new ComposedSchema();
                                while (superType.isPresent()) {
                                    final ClassElement superElement = superType.get();
                                    String parentSchemaName = computeDefaultSchemaName(definingElement, superElement);
                                    if (schemas.get(parentSchemaName) != null
                                            || getSchemaDefinition(openAPI, context, superElement, null, mediaTypes) != null) {
                                        Schema parentSchema = new Schema();
                                        parentSchema.set$ref(schemaRef(parentSchemaName));
                                        schema.addAllOfItem(parentSchema);
                                    }
                                    superType = superElement.getSuperType();
                                }
                            } else {
                                schema = new Schema();
                            }
                        } else {
                            schema = new Schema();
                        }
                        schema.setType("object");
                        schema.setName(schemaName);
                        schemas.put(schemaName, schema);

                        populateSchemaProperties(openAPI, context, type, schema, mediaTypes);
                        if (schema instanceof ComposedSchema) {
                            checkAllOf((ComposedSchema) schema);
                        }
                    }
                }
            } else {
                return primitiveType.createProperty();
            }
        } else {
            String schemaName = schemaValue.get("name", String.class).orElse(computeDefaultSchemaName(definingElement, type));
            schema = schemas.get(schemaName);
            if (schema == null) {
                if (inProgressSchemas.contains(schemaName)) {
                    // Break recursion
                    return new Schema<>().$ref(schemaRef(schemaName));
                }
                inProgressSchemas.add(schemaName);
                try {
                    schema = readSchema(schemaValue, openAPI, context, type, mediaTypes);
                    AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> typeSchema = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    if (typeSchema != null) {
                        Schema originalTypeSchema = readSchema(typeSchema, openAPI, context, type, mediaTypes);

                        if (originalTypeSchema.getDescription() != null && !originalTypeSchema.getDescription().isEmpty()) {
                            schema.setDescription(originalTypeSchema.getDescription());
                        }
                        schema.setNullable(originalTypeSchema.getNullable());
                        schema.setRequired(originalTypeSchema.getRequired());
                    }

                    if (schema != null) {
                        schema.setName(schemaName);
                        schemas.put(schemaName, schema);
                    }
                } catch (JsonProcessingException e) {
                    context.warn("Error reading Swagger Parameter for element [" + type + "]: " + e.getMessage(), type);
                } finally {
                    inProgressSchemas.remove(schemaName);
                }
            }
        }
        if (schema != null) {
            AnnotationValue<io.swagger.v3.oas.annotations.ExternalDocumentation> externalDocsValue = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.ExternalDocumentation.class);
            ExternalDocumentation externalDocs = null;
            if (externalDocsValue != null) {
                externalDocs = toValue(externalDocsValue.getValues(), context, ExternalDocumentation.class).orElse(null);
            }
            if (externalDocs != null) {
                schema.setExternalDocs(externalDocs);
            }
            setSchemaDocumentation(type, schema);
            Schema schemaRef = new Schema();
            schemaRef.set$ref(schemaRef(schema.getName()));
            schemaRef.setDescription(schema.getDescription());
            return schemaRef;
        }
        return null;
    }

    /**
     * Reads schema.
     *
     * @param schemaValue annotation value
     * @param openAPI The OpenApi
     * @param context The VisitorContext
     * @param type The element
     * @param mediaTypes The media types of schema
     *
     * @return New schema instance
     *
     * @throws JsonProcessingException when Json parsing fails
     */
    protected Schema readSchema(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue, OpenAPI openAPI, VisitorContext context, @Nullable Element type, List<MediaType> mediaTypes) throws JsonProcessingException {
        Map<CharSequence, Object> values = schemaValue.getValues()
                .entrySet()
                .stream()
                .collect(toMap(e -> e.getKey().equals("requiredProperties") ? "required" : e.getKey(), Map.Entry::getValue));
        Optional<Schema> schemaOpt = toValue(values, context, Schema.class);
        if (!schemaOpt.isPresent()) {
            return null;
        }
        Schema schema = schemaOpt.get();
        ComposedSchema composedSchema = null;
        if (schema instanceof ComposedSchema) {
            composedSchema = (ComposedSchema) schema;
            final Optional<String[]> allOf = schemaValue.get("allOf", String[].class);
            if (allOf.isPresent() && allOf.get().length > 0) {
                final String[] names = allOf.get();
                List<Schema> schemaList = namesToSchemas(openAPI, context, names, mediaTypes);
                for (Schema s : schemaList) {
                    composedSchema.addAllOfItem(s);
                }
            }

            final Optional<String[]> anyOf = schemaValue.get("anyOf", String[].class);
            if (anyOf.isPresent() && anyOf.get().length > 0) {
                final String[] names = anyOf.get();
                List<Schema> schemaList = namesToSchemas(openAPI, context, names, mediaTypes);
                for (Schema s : schemaList) {
                    composedSchema.addAnyOfItem(s);
                }
            }

            final Optional<String[]> oneof = schemaValue.get("oneOf", String[].class);
            if (oneof.isPresent() && oneof.get().length > 0) {
                final String[] names = oneof.get();
                List<Schema> schemaList = namesToSchemas(openAPI, context, names, mediaTypes);
                for (Schema s : schemaList) {
                    composedSchema.addOneOfItem(s);
                }
            }

            schema.setType("object");
        }
        if (type instanceof EnumElement) {
            schema.setType("string");
            schema.setEnum(((EnumElement) type).values());
        } else if (schema instanceof ObjectSchema || composedSchema != null) {
            populateSchemaProperties(openAPI, context, type, schema, mediaTypes);
            checkAllOf(composedSchema);
        }
        return schema;
    }

    private List<Schema> namesToSchemas(OpenAPI openAPI, VisitorContext context, String[] names, List<MediaType> mediaTypes) {
        return Arrays.stream(names).flatMap((Function<String, Stream<Schema>>) className -> {
            final Optional<ClassElement> classElement = context.getClassElement(className);
            if (classElement.isPresent()) {
                final Schema schemaDefinition = getSchemaDefinition(openAPI, context, classElement.get(), null, mediaTypes);
                if (schemaDefinition != null) {
                    return Stream.of(schemaDefinition);
                }
            }

            return Stream.empty();
        }).collect(Collectors.toList());
    }

    private String schemaRef(String schemaName) {
        return "#/components/schemas/" + schemaName;
    }

    private String computeDefaultSchemaName(Element definingElement, Element type) {
        final String metaAnnName = definingElement == null ? null : definingElement.getAnnotationNameByStereotype(io.swagger.v3.oas.annotations.media.Schema.class).orElse(null);
        if (metaAnnName != null && !io.swagger.v3.oas.annotations.media.Schema.class.getName().equals(metaAnnName)) {
            return NameUtils.getSimpleName(metaAnnName);
        }
        String javaName;
        if (type instanceof TypedElement) {
            javaName = computeNameWithGenerics(((TypedElement) type).getType());
        } else {
            javaName = type.getSimpleName();
        }
        return javaName.replace("$", ".");
    }

    private String computeNameWithGenerics(ClassElement classElement) {
        StringBuilder builder = new StringBuilder(classElement.getSimpleName());
        computeNameWithGenerics(classElement, builder, new HashSet<>());
        return builder.toString();
    }

    private void computeNameWithGenerics(ClassElement classElement, StringBuilder builder, Set<String> computed) {
        computed.add(classElement.getName());
        final Map<String, ClassElement> typeArguments = classElement.getTypeArguments();
        final Iterator<ClassElement> i = typeArguments.values().iterator();
        if (i.hasNext()) {

            builder.append('_');
            while (i.hasNext()) {
                final ClassElement ce = i.next();
                builder.append(ce.getSimpleName());
                if (!computed.contains(ce.getName())) {
                    computeNameWithGenerics(ce, builder, computed);
                }
                if (i.hasNext()) {
                    builder.append('.');
                }
            }

            builder.append('_');
        }
    }

    /**
     * Returns true if classElement is a JavaClassElement.
     *
     * @param classElement A ClassElement.
     * @param context The context.
     *
     * @return true if classElement is a JavaClassElement.
     */
    static boolean isJavaElement(ClassElement classElement, VisitorContext context) {
        return classElement != null &&
                "io.micronaut.annotation.processing.visitor.JavaClassElement".equals(classElement.getClass().getName()) &&
                "io.micronaut.annotation.processing.visitor.JavaVisitorContext".equals(context.getClass().getName());
    }

    private void populateSchemaProperties(OpenAPI openAPI, VisitorContext context, Element type, Schema schema, List<MediaType> mediaTypes) {
        ClassElement classElement = null;
        if (type instanceof ClassElement) {
            classElement = (ClassElement) type;
        } else if (type instanceof TypedElement) {
            classElement = ((TypedElement) type).getType();
        }

        if (classElement != null) {
            List<PropertyElement> beanProperties;
            try {
                beanProperties = classElement.getBeanProperties().stream().filter(p -> !"groovy.lang.MetaClass".equals(p.getType().getName())).collect(Collectors.toList());
            } catch (Exception e) {
                // Workaround for https://github.com/micronaut-projects/micronaut-openapi/issues/313
                beanProperties = Collections.emptyList();
            }
            processPropertyElements(openAPI, context, type, schema, beanProperties, mediaTypes);

            final List<FieldElement> publicFields = classElement.getEnclosedElements(ElementQuery.ALL_FIELDS.modifiers(mods -> mods.contains(ElementModifier.PUBLIC) && mods.size() == 1));

            processPropertyElements(openAPI, context, type, schema, publicFields, mediaTypes);
        }
    }

    private void processPropertyElements(OpenAPI openAPI, VisitorContext context, Element type, Schema schema, List<? extends TypedElement> publicFields, List<MediaType> mediaTypes) {
        for (TypedElement publicField : publicFields) {
            if (publicField.isAnnotationPresent(JsonIgnore.class) || publicField.isAnnotationPresent(Hidden.class)) {
                continue;
            }

            if (publicField instanceof MemberElement && ((MemberElement) publicField).getDeclaringType().getType().getName().equals(type.getName())) {

                Schema propertySchema = resolveSchema(openAPI, publicField, publicField.getType(), context, mediaTypes);

                processSchemaProperty(
                        context,
                        publicField,
                        publicField.getType(),
                        type,
                        schema,
                        propertySchema
                );
            }
        }
    }

    private Map<String, Schema> resolveSchemas(OpenAPI openAPI) {
        Components components = resolveComponents(openAPI);
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            schemas = new LinkedHashMap<>();
            components.setSchemas(schemas);
        }
        return schemas;
    }

    private ArraySchema arraySchema(Schema schema) {
        if (schema == null) {
            return null;
        }
        ArraySchema arraySchema = new ArraySchema();
        arraySchema.setItems(schema);
        return arraySchema;
    }

    private Schema getPrimitiveType(String typeName) {
        Schema schema = null;
        Optional<Class> aClass = ClassUtils.getPrimitiveType(typeName);
        if (!aClass.isPresent()) {
            aClass = ClassUtils.forName(typeName, getClass().getClassLoader());
        }

        if (aClass.isPresent()) {
            Class concreteType = aClass.get();
            Class wrapperType = ReflectionUtils.getWrapperType(concreteType);

            PrimitiveType primitiveType = PrimitiveType.fromType(wrapperType);
            if (primitiveType != null) {
                schema = primitiveType.createProperty();
            }
        }
        return schema;
    }

    private boolean isContainerType(ClassElement type) {
        return CollectionUtils.setOf(
                Optional.class.getName(),
                Future.class.getName(),
                Publisher.class.getName(),
                "io.reactivex.Single",
                "io.reactivex.Observable",
                "io.reactivex.Maybe",
                "io.reactivex.rxjava3.core.Single",
                "io.reactivex.rxjava3.core.Observable",
                "io.reactivex.rxjava3.core.Maybe"
        ).stream().anyMatch(type::isAssignable);
    }

    private boolean isReturnTypeFile(ClassElement type) {
        return CollectionUtils.setOf(
                FileCustomizableResponseType.class.getName(),
                File.class.getName(),
                InputStream.class.getName(),
                ByteBuffer.class.getName()
        ).stream().anyMatch(type::isAssignable);
    }

    /**
     * Processes {@link io.swagger.v3.oas.annotations.security.SecurityScheme}
     * annotations.
     *
     * @param element The element
     * @param context The visitor context
     */
    protected void processSecuritySchemes(ClassElement element, VisitorContext context) {
        final List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityScheme>> values = element
                .getAnnotationValuesByType(io.swagger.v3.oas.annotations.security.SecurityScheme.class);
        final OpenAPI openAPI = resolveOpenAPI(context);
        for (AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityScheme> securityRequirementAnnotationValue : values) {

            final Optional<String> n = securityRequirementAnnotationValue.get("name", String.class);
            n.ifPresent(name -> {
                final Map<CharSequence, Object> map = toValueMap(securityRequirementAnnotationValue.getValues(), context);
                if (map.containsKey("paramName")) {
                    map.put("name", map.remove("paramName"));
                } else {
                    map.putIfAbsent("name", name);
                }
                normalizeEnumValues(map, CollectionUtils.mapOf("type", SecurityScheme.Type.class, "in", SecurityScheme.In.class));

                try {
                    JsonNode node = toJson(map, context);
                    SecurityScheme securityScheme = jsonMapperForSecurityScheme.treeToValue(node, SecurityScheme.class);
                    if (securityScheme != null) {
                        resolveExtensions(node).ifPresent(extensions -> BeanMap.of(securityScheme).put("extensions", extensions));
                        resolveComponents(openAPI).addSecuritySchemes(name, securityScheme);
                    }
                } catch (JsonProcessingException e) {
                    // ignore
                }
            });
        }
    }

    /**
     * Normalizes enum values stored in the map.
     *
     * @param paramValues The values
     * @param enumTypes The enum types.
     */
    protected void normalizeEnumValues(Map<CharSequence, Object> paramValues, Map<String, Class<? extends Enum>> enumTypes) {
        for (Map.Entry<String, Class<? extends Enum>> entry : enumTypes.entrySet()) {
            final String name = entry.getKey();
            final Class<? extends Enum> enumType = entry.getValue();
            Object in = paramValues.get(name);
            if (in != null) {
                try {
                    final Enum enumInstance = Enum.valueOf(enumType, in.toString());
                    paramValues.put(name, enumInstance.toString());
                } catch (Exception e) {
                    // ignore
                }
            }

        }
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
    protected SecurityRequirement mapToSecurityRequirement(AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement> r) {
        String name = r.getRequiredValue("name", String.class);
        List<String> scopes = r.get("scopes", String[].class).map(Arrays::asList).orElse(Collections.emptyList());
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }

    /**
     * Converts annotation to model.
     *
     * @param <T> The model type.
     * @param <A> The annotation type.
     * @param element The element to process.
     * @param context The context.
     * @param annotationType The annotation type.
     * @param modelType The model type.
     * @param tagList The initial list of models.
     *
     * @return A list of model objects.
     */
    protected <T, A extends Annotation> List<T> processOpenApiAnnotation(Element element, VisitorContext context, Class<A> annotationType, Class<T> modelType, List<T> tagList) {
        List<AnnotationValue<A>> annotations = element.getAnnotationValuesByType(annotationType);
        if (CollectionUtils.isNotEmpty(annotations)) {
            if (CollectionUtils.isEmpty(tagList)) {
                tagList = new ArrayList<>();
            }
            for (AnnotationValue<A> tag : annotations) {
                Map<CharSequence, Object> values;
                if (tag.getAnnotationName().equals(io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName()) && !tag.getValues().isEmpty()) {
                    Object name = tag.getValues().get("name");
                    Object scopes = Optional.ofNullable(tag.getValues().get("scopes")).orElse(new ArrayList<String>());
                    values = Collections.singletonMap((CharSequence) name, scopes);
                } else {
                    values = tag.getValues();
                }
                Optional<T> tagOpt = toValue(values, context, modelType);
                if (tagOpt.isPresent()) {
                    T tagObj = tagOpt.get();
                    // skip all existed tags
                    boolean alreadyExists = false;
                    if (CollectionUtils.isNotEmpty(tagList) && tag.getAnnotationName().equals(io.swagger.v3.oas.annotations.tags.Tag.class.getName())) {
                        for (T existedTag : tagList) {
                            if (((Tag) existedTag).getName().equals(((Tag) tagObj).getName())) {
                                alreadyExists = true;
                                break;
                            }
                        }
                    }
                    if (!alreadyExists) {
                        tagList.add(tagObj);
                    }
                }
            }
        }
        return tagList;
    }

    boolean isTestMode() {
        return Boolean.getBoolean(ATTR_TEST_MODE);
    }

}
