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

import java.io.IOException;
import java.lang.annotation.Annotation;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.datatype.XMLGregorianCalendar;

import io.micronaut.context.env.Environment;
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
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.EnumConstantElement;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.swagger.PrimitiveType;
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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.micronaut.openapi.visitor.ConvertUtils.resolveExtensions;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.expandProperties;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.getExpandableProperties;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.resolvePlaceholders;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static java.util.stream.Collectors.toMap;

/**
 * Abstract base class for OpenAPI visitors.
 *
 * @author graemerocher
 * @since 1.0
 */
abstract class AbstractOpenApiVisitor {

    private static final Lock VISITED_ELEMENTS_LOCK = new ReentrantLock();
    private static final Schema<?> EMPTY_SCHEMA = new Schema<>();
    private static final ComposedSchema EMPTY_COMPOSED_SCHEMA = new ComposedSchema();

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
            context.put(Utils.ATTR_VISITED_ELEMENTS, ContextUtils.getVisitedElements(context) + 1);
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
            return ContextUtils.getVisitedElements(context);
        } finally {
            VISITED_ELEMENTS_LOCK.unlock();
        }
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
        return ConvertUtils.getJsonMapper().valueToTree(newValues);
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
            return Optional.ofNullable(ConvertUtils.treeToValue(node, type, context));
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
            .map(ConvertUtils::mapToSecurityRequirement)
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
        OpenAPI openAPI = Utils.resolveOpenAPI(context);
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

            String resultPath = result.toString();
            Environment environment = OpenApiApplicationVisitor.getEnv(context);
            if (environment != null) {
                resultPath = environment.getPlaceholderResolver().resolvePlaceholders(resultPath).orElse(resultPath);
            }
            if (!resultPath.startsWith("/") && !resultPath.startsWith("$")) {
                resultPath = "/" + resultPath;
            }
            String contextPath = OpenApiApplicationVisitor.getConfigurationProperty("micronaut.server.context-path", context);
            if (StringUtils.isNotEmpty(contextPath)) {
                if (!contextPath.startsWith("/") && !contextPath.startsWith("$")) {
                    contextPath = "/" + contextPath;
                }
                if (contextPath.endsWith("/")) {
                    contextPath = contextPath.substring(0, contextPath.length() - 1);
                }
                resultPath = contextPath + resultPath;
            }

            resultPaths.add(paths.computeIfAbsent(resultPath, key -> new PathItem()));
        }

        return resultPaths;
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
                                    securityRequirements.add(ConvertUtils.mapToSecurityRequirement((AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>) o));
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
                                Map<String, Object> linksOrHeaders = annotationValueArrayToSubmap(a, "name", context);
                                for (Object linkOrHeader : linksOrHeaders.values()) {
                                    Map<String, Object> linkOrHeaderMap = (Map<String, Object>) linkOrHeader;
                                    if (linkOrHeaderMap.containsKey("ref")) {
                                        linkOrHeaderMap.put("$ref", linkOrHeaderMap.remove("ref"));
                                    }
                                    if (linkOrHeaderMap.containsKey("schema")) {
                                        Map<String, Object> schemaMap = (Map<String, Object>) linkOrHeaderMap.get("schema");
                                        if (schemaMap.containsKey("ref")) {
                                            Object ref = schemaMap.get("ref");
                                            schemaMap.clear();
                                            schemaMap.put("$ref", ref);
                                        }
                                        if (schemaMap.containsKey("defaultValue")) {
                                            schemaMap.put("default", schemaMap.remove("defaultValue"));
                                        }
                                        if (schemaMap.containsKey("allowableValues")) {
                                            // The key in the generated openapi needs to be "enum"
                                            schemaMap.put("enum", schemaMap.remove("allowableValues"));
                                        }
                                    }
                                }
                                newValues.put(key, linksOrHeaders);
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
                                    String name = sv.stringValue("responseCode").orElse("default");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                    if (map.containsKey("ref")) {
                                        Object ref = map.get("ref");
                                        map.clear();
                                        map.put("$ref", ref);
                                    }
                                    responses.put(name, map);
                                }
                                newValues.put(key, responses);
                            } else if (ExampleObject.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> examples = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ExampleObject> sv = (AnnotationValue<ExampleObject>) o;
                                    String name = sv.stringValue("name").orElse("example");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                    if (map.containsKey("ref")) {
                                        Object ref = map.get("ref");
                                        map.clear();
                                        map.put("$ref", ref);
                                    }
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
                                    Optional<String> n = sv.stringValue("name");
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
                    io.swagger.v3.oas.models.parameters.Parameter.StyleEnum paramStyle = null;
                    try {
                        paramStyle = io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.valueOf((String) value);
                    } catch (Exception e) {
                        // ignore
                    }
                    if (paramStyle == null) {
                        for (io.swagger.v3.oas.models.parameters.Parameter.StyleEnum styleValue : io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.values()) {
                            if (styleValue.toString().equals(value)) {
                                paramStyle = styleValue;
                                newValues.put(key, styleValue.toString());
                                break;
                            }
                        }
                    } else {
                        newValues.put(key, paramStyle.toString());
                    }

                    if (paramStyle == null) {
                        io.swagger.v3.oas.models.media.Encoding.StyleEnum encodingStyle = null;
                        try {
                            encodingStyle = io.swagger.v3.oas.models.media.Encoding.StyleEnum.valueOf((String) value);
                        } catch (Exception e) {
                            // ignore
                        }
                        if (encodingStyle == null) {
                            for (io.swagger.v3.oas.models.media.Encoding.StyleEnum styleValue : io.swagger.v3.oas.models.media.Encoding.StyleEnum.values()) {
                                if (styleValue.toString().equals(value)) {
                                    encodingStyle = styleValue;
                                    newValues.put(key, styleValue.toString());
                                    break;
                                }
                            }
                        } else {
                            newValues.put(key, encodingStyle.toString());
                        }
                    }
                } else if (key.equals("ref")) {
                    newValues.put("$ref", value);
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
        final String key = !name.isEmpty() ? prependIfMissing(name, "x-") : name;
        for (AnnotationValue<ExtensionProperty> prop : extension.getAnnotations("properties", ExtensionProperty.class)) {
            final String propertyName = prop.getRequiredValue("name", String.class);
            final String propertyValue = prop.getRequiredValue(String.class);
            JsonNode processedValue;
            final boolean propertyAsJson = prop.get("parseValue", boolean.class, false);
            if (StringUtils.hasText(propertyName) && StringUtils.hasText(propertyValue)) {
                if (key.isEmpty()) {
                    if (propertyAsJson) {
                        try {
                            processedValue = ConvertUtils.getJsonMapper().readTree(propertyValue);
                            map.put(prependIfMissing(propertyName, "x-"), processedValue);
                        } catch (Exception e) {
                            map.put(prependIfMissing(propertyName, "x-"), propertyValue);
                        }
                    } else {
                        map.put(prependIfMissing(propertyName, "x-"), propertyValue);
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
                            processedValue = ConvertUtils.getJsonMapper().readTree(propertyValue);
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

    public static String prependIfMissing(final String str, final String prefix) {
        if (str == null || StringUtils.isEmpty(prefix) || str.startsWith(prefix)) {
            return str;
        }
        return prefix + str;
    }

    private Optional<Object> parseJsonString(Object object) {
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

    private <T extends Schema> void processAnnotationValue(VisitorContext context, AnnotationValue<?> annotationValue, Map<CharSequence, Object> arraySchemaMap, List<String> filters, Class<T> type) {
        Map<CharSequence, Object> values = annotationValue.getValues().entrySet().stream()
            .filter(entry -> filters == null || !filters.contains((String) entry.getKey()))
            .collect(toMap(e -> e.getKey().equals("requiredProperties") ? "required" : e.getKey(), Map.Entry::getValue));
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
            Optional<String> impl = annotationValue.stringValue("implementation");
            Optional<String> type = annotationValue.stringValue("type");
            Optional<String> format = annotationValue.stringValue("format");
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
                    final ArraySchema schema = SchemaUtils.arraySchema(resolveSchema(null, classElement.get(), context, Collections.emptyList()));
                    schemaToValueMap(arraySchemaMap, schema);
                } else {
                    // For primitive type, just copy description field is present.
                    final Schema items = primitiveType.createProperty();
                    items.setDescription((String) annotationValue.stringValue("description").orElse(null));
                    final ArraySchema schema = SchemaUtils.arraySchema(items);
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
            Utils.normalizeEnumValues(valueMap, CollectionUtils.mapOf(
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
            final Optional<String> n = sv.stringValue(entryKey);
            final Optional<String> expr = sv.stringValue(entryValue);
            if (n.isPresent() && expr.isPresent()) {
                params.put(n.get(), expr.get());
            }
        }
        return params;
    }

    private boolean isTypeNullable(ClassElement type) {
        return type.isAssignable(Optional.class);
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
        return resolveSchema(Utils.resolveOpenAPI(context), definingElement, type, context, mediaTypes, null, null);
    }

    /**
     * Resolves the schema for the given type element.
     *
     * @param openAPI The OpenAPI object
     * @param definingElement The defining element
     * @param type The type element
     * @param context The context
     * @param mediaTypes An optional media type
     * @param fieldJavadoc Field-level java doc
     * @param classJavadoc Class-level java doc
     *
     * @return The schema or null if it cannot be resolved
     */
    @Nullable
    protected Schema resolveSchema(OpenAPI openAPI, @Nullable Element definingElement, ClassElement type, VisitorContext context,
                                   List<MediaType> mediaTypes, JavadocDescription fieldJavadoc, JavadocDescription classJavadoc) {
        Schema schema = null;

        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotationValue = null;
        if (definingElement != null) {
            schemaAnnotationValue = definingElement.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        if (type != null && schemaAnnotationValue == null) {
            schemaAnnotationValue = type.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        if (schemaAnnotationValue != null) {
            String impl = schemaAnnotationValue.stringValue("implementation").orElse(null);
            if (StringUtils.isNotEmpty(impl)) {
                type = context.getClassElement(impl).orElse(type);
            } else {
                String schemaType = schemaAnnotationValue.stringValue("type").orElse(null);
                if (StringUtils.isNotEmpty(schemaType) && !(type instanceof EnumElement)) {
                    PrimitiveType primitiveType = PrimitiveType.fromName(schemaType);
                    if (primitiveType != null && primitiveType != PrimitiveType.OBJECT) {
                        type = context.getClassElement(primitiveType.getKeyClass()).orElse(type);
                    }
                }
            }
        }

        if (type instanceof EnumElement) {
            schema = getSchemaDefinition(openAPI, context, type, definingElement, mediaTypes);
        } else {

            boolean isPublisher = false;
            boolean isObservable = false;
            boolean isNullable = false;

            // StreamingFileUpload implements Publisher, but it should be not considered as a Publisher in the spec file
            if (!type.isAssignable("io.micronaut.http.multipart.StreamingFileUpload") && Utils.isContainerType(type)) {
                isPublisher = type.isAssignable("org.reactivestreams.Publisher") && !type.isAssignable("reactor.core.publisher.Mono");
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
                            schema.setAdditionalProperties(resolveSchema(openAPI, type, valueType, context, mediaTypes, null, classJavadoc));
                        }
                    }
                } else if (type.isIterable()) {
                    if (type.isArray()) {
                        schema = resolveSchema(openAPI, type, type.fromArray(), context, mediaTypes, null, classJavadoc);
                        if (schema != null) {
                            schema = SchemaUtils.arraySchema(schema);
                        }
                    } else {
                        Optional<ClassElement> componentType = type.getFirstTypeArgument();
                        if (componentType.isPresent()) {
                            schema = resolveSchema(openAPI, type, componentType.get(), context, mediaTypes, null, classJavadoc);
                        } else {
                            schema = getPrimitiveType(Object.class.getName());
                        }
                        List<FieldElement> fields = type.getFields();
                        if (schema != null && fields.isEmpty()) {
                            schema = SchemaUtils.arraySchema(schema);
                        } else {
                            schema = getSchemaDefinition(openAPI, context, type, definingElement, mediaTypes);
                        }
                    }
                } else if (Utils.isReturnTypeFile(type)) {
                    schema = PrimitiveType.FILE.createProperty();
                } else if (type.isAssignable(Boolean.class) || type.isAssignable(boolean.class)) {
                    schema = PrimitiveType.BOOLEAN.createProperty();
                } else if (type.isAssignable(Byte.class) || type.isAssignable(byte.class)) {
                    schema = PrimitiveType.BYTE.createProperty();
                } else if (type.isAssignable(UUID.class)) {
                    schema = PrimitiveType.UUID.createProperty();
                } else if (type.isAssignable(URL.class)) {
                    schema = PrimitiveType.URL.createProperty();
                } else if (type.isAssignable(URI.class)) {
                    schema = PrimitiveType.URI.createProperty();
                } else if (type.isAssignable(Character.class) || type.isAssignable(char.class)) {
                    schema = PrimitiveType.STRING.createProperty();
                } else if (type.isAssignable(Integer.class) || type.isAssignable(int.class)
                    || type.isAssignable(Short.class) || type.isAssignable(short.class)) {
                    schema = PrimitiveType.INT.createProperty();
                } else if (type.isAssignable(Long.class) || type.isAssignable(long.class)) {
                    schema = PrimitiveType.LONG.createProperty();
                } else if (type.isAssignable(Float.class) || type.isAssignable(float.class)) {
                    schema = PrimitiveType.FLOAT.createProperty();
                } else if (type.isAssignable(Double.class) || type.isAssignable(double.class)) {
                    schema = PrimitiveType.DOUBLE.createProperty();
                } else if (type.isAssignable(BigInteger.class)) {
                    schema = PrimitiveType.INTEGER.createProperty();
                } else if (type.isAssignable(BigDecimal.class)) {
                    schema = PrimitiveType.DECIMAL.createProperty();
                } else if (type.isAssignable(Date.class)
                    || type.isAssignable(Calendar.class)
                    || type.isAssignable(LocalDateTime.class)
                    || type.isAssignable(ZonedDateTime.class)
                    || type.isAssignable(OffsetDateTime.class)
                    || type.isAssignable(Instant.class)
                    || type.isAssignable(XMLGregorianCalendar.class)) {
                    schema = new StringSchema().format("date-time");
                } else if (type.isAssignable(LocalDate.class)) {
                    schema = new StringSchema().format("date");
                } else if (type.isAssignable(LocalTime.class)) {
                    schema = new StringSchema().format("partial-time");
                } else if (type.isAssignable(Number.class)) {
                    schema = PrimitiveType.NUMBER.createProperty();
                } else {
                    schema = getSchemaDefinition(openAPI, context, type, definingElement, mediaTypes);
                }
            }

            if (schema != null) {

                if (definingElement != null && StringUtils.isEmpty(schema.getDescription())) {
                    if (fieldJavadoc != null) {
                        if (StringUtils.hasText(fieldJavadoc.getMethodDescription())) {
                            schema.setDescription(fieldJavadoc.getMethodDescription());
                        }
                    } else if (classJavadoc != null) {
                        String paramJavadoc = classJavadoc.getParameters().get(definingElement.getName());
                        if (StringUtils.hasText(paramJavadoc)) {
                            schema.setDescription(paramJavadoc);
                        }
                    }
                }

                boolean isStream = false;
                for (MediaType mediaType : mediaTypes) {
                    if (MediaType.TEXT_EVENT_STREAM_TYPE.equals(mediaType) || MediaType.APPLICATION_JSON_STREAM_TYPE.equals(mediaType)) {
                        isStream = true;
                        break;
                    }
                }

                if (!isStream && (isPublisher || isObservable)) {
                    schema = SchemaUtils.arraySchema(schema);
                } else if (isNullable) {
                    schema.setNullable(true);
                }
            }
        }
        return schema;
    }

    private void handleUnwrapped(VisitorContext context, Element element, ClassElement elementType, Schema parentSchema, AnnotationValue<JsonUnwrapped> uw) {
        Map<String, Schema> schemas = SchemaUtils.resolveSchemas(Utils.resolveOpenAPI(context));
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
                    propertySchema = ConvertUtils.getJsonMapper().readValue(ConvertUtils.getJsonMapper().writeValueAsString(prop.getValue()), Schema.class);
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
                Schema propertySchemaFinal = propertySchema;
                addProperty(parentSchema, propertyName, propertySchema, required);
                if (schemaAnnotationValue != null) {
                    schemaAnnotationValue.stringValue("defaultValue")
                        .ifPresent(value -> {
                            String elType = schemaAnnotationValue.stringValue("type").orElse(null);
                            String elFormat = schemaAnnotationValue.stringValue("format").orElse(null);
                            if (elType == null && elementType != null) {
                                Pair<String, String> typeAndFormat = ConvertUtils.getTypeAndFormatByClass(elementType.getName());
                                elType = typeAndFormat.getFirst();
                                if (elFormat == null) {
                                    elFormat = typeAndFormat.getSecond();
                                }
                            }
                            try {
                                propertySchemaFinal.setDefault(ConvertUtils.normalizeValue(value, elType, elFormat, context));
                            } catch (JsonProcessingException e) {
                                context.warn("Can't parse value " + value + " to " + elType + ": " + e.getMessage(), element);
                                propertySchemaFinal.setDefault(value);
                            }
                        });
                }
            }
        }
    }

    protected boolean isElementNotNullable(Element element, @Nullable Element classElement) {
        return element.isAnnotationPresent("javax.validation.constraints.NotNull$List")
            || element.isAnnotationPresent("jakarta.validation.constraints.NotNull$List")
            || element.isAnnotationPresent("javax.validation.constraints.NotBlank$List")
            || element.isAnnotationPresent("jakarta.validation.constraints.NotBlank$List")
            || element.isAnnotationPresent("javax.validation.constraints.NotEmpty$List")
            || element.isAnnotationPresent("jakarta.validation.constraints.NotEmpty$List")
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
            Optional<String> nameFromSchema = element.stringValue(io.swagger.v3.oas.annotations.media.Schema.class, "name");
            if (nameFromSchema.isPresent()) {
                return nameFromSchema.get();
            }
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
            Schema schemaFromAnn = shemaFromAnnotation(context, element, schemaAnn);
            if (schemaFromAnn != null) {
                schemaToBind = schemaFromAnn;
            }
        }
        if (originalSchema.get$ref() == null && schemaAnn != null) {
            // Apply @Schema annotation only if not $ref since for $ref schemas
            // we already populated values from right @Schema annotation in previous steps
            schemaToBind = bindSchemaAnnotationValue(context, element, schemaToBind, schemaAnn);
            Optional<String> schemaName = schemaAnn.stringValue("name");
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
            Optional<String> schemaName = arraySchemaAnn.stringValue("name");
            if (schemaName.isPresent()) {
                schemaToBind.setName(schemaName.get());
            }
        }

        Schema finalSchemaToBind = schemaToBind;
        processJavaxValidationAnnotations(element, elementType, finalSchemaToBind);

        final ComposedSchema composedSchema;
        final Schema<?> topLevelSchema;
        if (originalSchema.get$ref() != null) {
            composedSchema = new ComposedSchema();
            topLevelSchema = composedSchema;
        } else {
            composedSchema = null;
            topLevelSchema = schemaToBind;
        }

        boolean notOnlyRef = false;
        setSchemaDocumentation(element, topLevelSchema);
        if (StringUtils.isNotEmpty(topLevelSchema.getDescription())) {
            notOnlyRef = true;
        }
        if (element.isAnnotationPresent(Deprecated.class)) {
            topLevelSchema.setDeprecated(true);
            notOnlyRef = true;
        }
        final String defaultValue = element.getValue(Bindable.class, "defaultValue", String.class).orElse(null);
        if (defaultValue != null && schemaToBind.getDefault() == null) {
            try {
                topLevelSchema.setDefault(ConvertUtils.normalizeValue(defaultValue, schemaToBind.getType(), schemaToBind.getFormat(), context));
            } catch (JsonProcessingException e) {
                context.warn("Can't convert " + defaultValue + " to " + schemaToBind.getType() + ": " + e.getMessage(), element);
                topLevelSchema.setDefault(defaultValue);
            }
            notOnlyRef = true;
        }
        // @Schema annotation takes priority over nullability annotations
        Boolean isSchemaNullable = element.booleanValue(io.swagger.v3.oas.annotations.media.Schema.class, "nullable").orElse(null);
        boolean isNullable = (isSchemaNullable == null && (element.isNullable() || isTypeNullable(elementType)))
            || Boolean.TRUE.equals(isSchemaNullable);
        if (isNullable) {
            topLevelSchema.setNullable(true);
            notOnlyRef = true;
        }
        final String defaultJacksonValue = element.stringValue(JsonProperty.class, "defaultValue").orElse(null);
        if (defaultJacksonValue != null && schemaToBind.getDefault() == null) {
            try {
                topLevelSchema.setDefault(ConvertUtils.normalizeValue(defaultJacksonValue, schemaToBind.getType(), schemaToBind.getFormat(), context));
            } catch (JsonProcessingException e) {
                context.warn("Can't convert " + defaultJacksonValue + " to " + schemaToBind.getType() + ": " + e.getMessage(), element);
                topLevelSchema.setDefault(defaultJacksonValue);
            }
            notOnlyRef = true;
        }

        if (composedSchema != null) {
            boolean addSchemaToBind = !schemaToBind.equals(EMPTY_SCHEMA);

            if (addSchemaToBind) {
                composedSchema.addAllOfItem(originalSchema);
            } else if (isNullable && CollectionUtils.isEmpty(composedSchema.getAllOf())) {
                composedSchema.addOneOfItem(originalSchema);
            }
            if (addSchemaToBind && !schemaToBind.equals(originalSchema)) {
                composedSchema.addAllOfItem(schemaToBind);
            }

            if (!composedSchema.equals(EMPTY_COMPOSED_SCHEMA)
                && ((CollectionUtils.isNotEmpty(composedSchema.getAllOf()) && composedSchema.getAllOf().size() > 1)
                || CollectionUtils.isNotEmpty(composedSchema.getOneOf())
                || notOnlyRef)) {
                return composedSchema;
            }
        }

        return originalSchema;
    }

    protected void processJavaxValidationAnnotations(Element element, ClassElement elementType, Schema schemaToBind) {

        final boolean isIterableOrMap = elementType.isIterable() || elementType.isAssignable(Map.class);

        if (isIterableOrMap) {
            if (element.isAnnotationPresent("javax.validation.constraints.NotEmpty$List")
                || element.isAnnotationPresent("jakarta.validation.constraints.NotEmpty$List")) {
                schemaToBind.setMinItems(1);
            }

            element.findAnnotation("javax.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ((Optional<Integer>) ann.get("min", Integer.class))
                        .ifPresent(schemaToBind::setMinItems)));
            element.findAnnotation("jakarta.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ((Optional<Integer>) ann.get("min", Integer.class))
                        .ifPresent(schemaToBind::setMinItems)));

            element.findAnnotation("javax.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ((Optional<Integer>) ann.get("max", Integer.class))
                        .ifPresent(schemaToBind::setMaxItems)));
            element.findAnnotation("jakarta.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ((Optional<Integer>) ann.get("max", Integer.class))
                        .ifPresent(schemaToBind::setMaxItems)));

        } else {
            if (PrimitiveType.STRING.getCommonName().equals(schemaToBind.getType())) {
                if (element.isAnnotationPresent("javax.validation.constraints.NotEmpty$List")
                    || element.isAnnotationPresent("jakarta.validation.constraints.NotEmpty$List")
                    || element.isAnnotationPresent("javax.validation.constraints.NotBlank$List")
                    || element.isAnnotationPresent("jakarta.validation.constraints.NotBlank$List")) {
                    schemaToBind.setMinLength(1);
                }

                element.findAnnotation("javax.validation.constraints.Size$List")
                    .ifPresent(listAnn -> {
                        for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                            ((Optional<Integer>) ann.get("min", Integer.class))
                                .ifPresent(schemaToBind::setMinLength);
                            ((Optional<Integer>) ann.get("max", Integer.class))
                                .ifPresent(schemaToBind::setMaxLength);
                        }
                    });
                element.findAnnotation("jakarta.validation.constraints.Size$List")
                    .ifPresent(listAnn -> {
                        for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                            ((Optional<Integer>) ann.get("min", Integer.class))
                                .ifPresent(schemaToBind::setMinLength);
                            ((Optional<Integer>) ann.get("max", Integer.class))
                                .ifPresent(schemaToBind::setMaxLength);
                        }
                    });
            }

            if (element.isAnnotationPresent("javax.validation.constraints.Negative$List")
                || element.isAnnotationPresent("jakarta.validation.constraints.Negative$List")) {
                schemaToBind.setMaximum(BigDecimal.ZERO);
            }
            if (element.isAnnotationPresent("javax.validation.constraints.NegativeOrZero$List")
                || element.isAnnotationPresent("jakarta.validation.constraints.NegativeOrZero$List")) {
                schemaToBind.setMaximum(BigDecimal.ZERO);
            }
            if (element.isAnnotationPresent("javax.validation.constraints.Positive$List")
                || element.isAnnotationPresent("jakarta.validation.constraints.Positive$List")) {
                schemaToBind.setMinimum(BigDecimal.ZERO);
            }
            if (element.isAnnotationPresent("javax.validation.constraints.PositiveOrZero$List")
                || element.isAnnotationPresent("jakarta.validation.constraints.PositiveOrZero$List")) {
                schemaToBind.setMinimum(BigDecimal.ZERO);
            }

            element.findAnnotation("javax.validation.constraints.Min$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Min$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.Max$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Max$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.DecimalMin$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.DecimalMin$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.DecimalMax$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.DecimalMax$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<BigDecimal>) ann.getValue(BigDecimal.class))
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.Email$List")
                .ifPresent(listAnn -> {
                    schemaToBind.setFormat(PrimitiveType.EMAIL.getCommonName());
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<String>) ann.stringValue("regexp"))
                            .ifPresent(schemaToBind::setPattern);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Email$List")
                .ifPresent(listAnn -> {
                    schemaToBind.setFormat(PrimitiveType.EMAIL.getCommonName());
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<String>) ann.stringValue("regexp"))
                            .ifPresent(schemaToBind::setPattern);
                    }
                });

            element.findAnnotation("javax.validation.constraints.Pattern$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<String>) ann.stringValue("regexp"))
                            .ifPresent(schemaToBind::setPattern);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Pattern$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue ann : (Set<AnnotationValue>) listAnn.getValues().get("value")) {
                        ((Optional<String>) ann.stringValue("regexp"))
                            .ifPresent(schemaToBind::setPattern);
                    }
                });

            element.getValue("io.micronaut.http.annotation.Part", String.class).ifPresent(schemaToBind::setName);
        }
    }

    Schema shemaFromAnnotation(VisitorContext context, Element element, AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {
        if (schemaAnn == null) {
            return null;
        }

        Schema schemaToBind = new Schema();

        Map<CharSequence, Object> annValues = schemaAnn.getValues();
        if (annValues.containsKey("description")) {
            schemaToBind.setDescription((String) annValues.get("description"));
        }
        if (annValues.containsKey("type")) {
            schemaToBind.setType((String) annValues.get("type"));
        }
        if (annValues.containsKey("format")) {
            schemaToBind.setFormat((String) annValues.get("format"));
        }
        if (annValues.containsKey("title")) {
            schemaToBind.setTitle((String) annValues.get("title"));
        }
        if (annValues.containsKey("minLength")) {
            schemaToBind.setMinLength((Integer) annValues.get("minLength"));
        }
        if (annValues.containsKey("maxLength")) {
            schemaToBind.setMaxLength((Integer) annValues.get("maxLength"));
        }
        if (annValues.containsKey("minProperties")) {
            schemaToBind.setMinProperties((Integer) annValues.get("minProperties"));
        }
        if (annValues.containsKey("maxProperties")) {
            schemaToBind.setMaxProperties((Integer) annValues.get("maxProperties"));
        }
        if (annValues.containsKey("pattern")) {
            schemaToBind.setPattern((String) annValues.get("pattern"));
        }

        String schemaMinimum = (String) annValues.get("minimum");
        if (NumberUtils.isCreatable(schemaMinimum)) {
            schemaToBind.setMinimum(new BigDecimal(schemaMinimum));
        }
        String schemaMaximum = (String) annValues.get("maximum");
        if (NumberUtils.isCreatable(schemaMaximum)) {
            schemaToBind.setMaximum(new BigDecimal(schemaMaximum));
        }
        Boolean schemaExclusiveMinimum = (Boolean) annValues.get("exclusiveMinimum");
        if (schemaExclusiveMinimum != null && schemaExclusiveMinimum) {
            schemaToBind.setExclusiveMinimum(true);
        }
        Boolean schemaExclusiveMaximum = (Boolean) annValues.get("exclusiveMaximum");
        if (schemaExclusiveMaximum != null && schemaExclusiveMaximum) {
            schemaToBind.setExclusiveMaximum(true);
        }
        Double schemaMultipleOf = (Double) annValues.get("multipleOf");
        if (schemaMultipleOf != null) {
            schemaToBind.setMultipleOf(BigDecimal.valueOf(schemaMultipleOf));
        }

        AnnotationValue<io.swagger.v3.oas.annotations.ExternalDocumentation> schemaExtDocs = (AnnotationValue<io.swagger.v3.oas.annotations.ExternalDocumentation>) annValues.get("externalDocs");
        ExternalDocumentation externalDocs = null;
        if (schemaExtDocs != null) {
            externalDocs = toValue(schemaExtDocs.getValues(), context, ExternalDocumentation.class).orElse(null);
        }
        if (externalDocs != null) {
            schemaToBind.setExternalDocs(externalDocs);
        }

        String schemaDefaultValue = (String) annValues.get("defaultValue");
        if (schemaDefaultValue != null) {
            try {
                schemaToBind.setDefault(ConvertUtils.normalizeValue(schemaDefaultValue, schemaToBind.getType(), schemaToBind.getFormat(), context));
            } catch (JsonProcessingException e) {
                context.warn("Can't convert " + schemaDefaultValue + " to " + schemaToBind.getType() + ": " + e.getMessage(), element);
                schemaToBind.setDefault(schemaDefaultValue);
            }
        }
        String schemaExample = (String) annValues.get("example");
        if (StringUtils.isNotEmpty(schemaExample)) {
            try {
                schemaToBind.setExample(ConvertUtils.getConvertJsonMapper().readValue(schemaExample, Map.class));
            } catch (JsonProcessingException e) {
                schemaToBind.setExample(schemaExample);
            }
        }
        Boolean schemaDeprecated = (Boolean) annValues.get("deprecated");
        if (schemaDeprecated != null && schemaDeprecated) {
            schemaToBind.setDeprecated(true);
        }
        String accessModeStr = (String) annValues.get("accessMode");
        if (StringUtils.isNotEmpty(accessModeStr)) {
            io.swagger.v3.oas.annotations.media.Schema.AccessMode schemaAccessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.valueOf(accessModeStr);
            if (schemaAccessMode != io.swagger.v3.oas.annotations.media.Schema.AccessMode.AUTO) {
                if (schemaAccessMode == io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY) {
                    schemaToBind.setReadOnly(true);
                    schemaToBind.setWriteOnly(null);
                } else if (schemaAccessMode == io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY) {
                    schemaToBind.setReadOnly(false);
                    schemaToBind.setWriteOnly(null);
                } else if (schemaAccessMode == io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_WRITE) {
                    schemaToBind.setReadOnly(null);
                    schemaToBind.setWriteOnly(null);
                }
            }
        }

        OpenAPI openAPI = Utils.resolveOpenAPI(context);
        Components components = resolveComponents(openAPI);

        final AnnotationClassValue<?> not = (AnnotationClassValue<?>) annValues.get("not");
        if (not != null) {
            final Schema<?> schemaNot = resolveSchema(null, context.getClassElement(not.getName()).get(), context, Collections.emptyList());
            schemaToBind.setNot(schemaNot);
        }
        final AnnotationClassValue<?>[] allOf = (AnnotationClassValue<?>[]) annValues.get("allOf");
        if (ArrayUtils.isNotEmpty(allOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, allOf, Collections.emptyList());
            for (Schema<?> s : schemaList) {
                schemaToBind.addAllOfItem(s);
            }
        }
        final AnnotationClassValue<?>[] anyOf = (AnnotationClassValue<?>[]) annValues.get("anyOf");
        if (ArrayUtils.isNotEmpty(anyOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, anyOf, Collections.emptyList());
            for (Schema<?> s : schemaList) {
                schemaToBind.addAnyOfItem(s);
            }
        }
        final AnnotationClassValue<?>[] oneOf = (AnnotationClassValue<?>[]) annValues.get("oneOf");
        if (ArrayUtils.isNotEmpty(oneOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, oneOf, Collections.emptyList());
            for (Schema<?> s : schemaList) {
                schemaToBind.addOneOfItem(s);
            }
        }

        String addProps = (String) annValues.get("additionalProperties");
        if (StringUtils.isNotEmpty(addProps)) {
            io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue schemaAdditionalProperties =
                io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.valueOf(addProps);
            if (schemaAdditionalProperties == io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE) {
                schemaToBind.additionalProperties(true);
            } else if (schemaAdditionalProperties == io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE) {
                schemaToBind.additionalProperties(false);
            }
        }

        return schemaToBind;
    }

    private void setSchemaDocumentation(Element element, Schema schemaToBind) {
        if (StringUtils.isEmpty(schemaToBind.getDescription())) {
            // First, find getter method javadoc
            String doc = element.getDocumentation().orElse(null);
            if (StringUtils.isEmpty(doc)) {
                // next, find field javadoc
                if (element instanceof MemberElement) {
                    List<FieldElement> fields = ((MemberElement) element).getDeclaringType().getFields();
                    if (CollectionUtils.isNotEmpty(fields)) {
                        for (FieldElement field : fields) {
                            if (field.getName().equals(element.getName())) {
                                doc = field.getDocumentation().orElse(null);
                                break;
                            }
                        }
                    }
                }
            }
            if (doc != null) {
                JavadocDescription desc = Utils.getJavadocParser().parse(doc);
                if (StringUtils.hasText(desc.getMethodDescription())) {
                    schemaToBind.setDescription(desc.getMethodDescription());
                }
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
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson,
            schemaAnn.stringValue("type").orElse(null),
            schemaAnn.stringValue("format").orElse(null),
            schemaAnn.stringValue("defaultValue").orElse(null),
            schemaAnn.get("allowableValues", String[].class).orElse(null));
    }

    private Schema doBindSchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind,
                                               JsonNode schemaJson, String elType, String elFormat, String defaultValue, String... allowableValues) {
        // need to set placeholders to set correct values to example field
        schemaJson = resolvePlaceholders(schemaJson, s -> expandProperties(s, getExpandableProperties(context), context));
        try {
            schemaToBind = ConvertUtils.getJsonMapper().readerForUpdating(schemaToBind).readValue(schemaJson);
        } catch (IOException e) {
            context.warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), element);
        }

        if (elType == null && element != null) {
            Pair<String, String> typeAndFormat = ConvertUtils.getTypeAndFormatByClass(((TypedElement) element).getType().getName());
            elType = typeAndFormat.getFirst();
            if (elFormat == null) {
                elFormat = typeAndFormat.getSecond();
            }
        }

        if (StringUtils.isNotEmpty(defaultValue)) {
            try {
                schemaToBind.setDefault(ConvertUtils.normalizeValue(defaultValue, elType, elFormat, context));
            } catch (IOException e) {
                context.warn("Can't convert " + defaultValue + " to " + elType + ", format: " + elFormat + ": " + e.getMessage(), element);
                schemaToBind.setDefault(defaultValue);
            }
        }
        if (ArrayUtils.isNotEmpty(allowableValues)) {
            for (String allowableValue : allowableValues) {
                if (schemaToBind.getEnum() == null || !schemaToBind.getEnum().contains(allowableValue)) {
                    try {
                        schemaToBind.addEnumItemObject(ConvertUtils.normalizeValue(allowableValue, elType, elFormat, context));
                    } catch (IOException e) {
                        context.warn("Can't convert " + allowableValue + " to " + elType + ", format: " + elFormat + ": " + e.getMessage(), element);
                        schemaToBind.addEnumItemObject(allowableValue);
                    }
                }
            }
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
            if (items != null && schemaToBind != null && schemaToBind.getType().equals("array") && schemaToBind.getItems() != null) {
                // if it has no $ref add properties, otherwise we are good
                if (schemaToBind.getItems().get$ref() == null) {
                    try {
                        schemaToBind.items(ConvertUtils.getJsonMapper().readerForUpdating(schemaToBind.getItems()).readValue(items));
                    } catch (IOException e) {
                        context.warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), element);
                    }
                }
            }
        }
        String elType = schemaJson.has("type") ? schemaJson.get("type").textValue() : null;
        String elFormat = schemaJson.has("format") ? schemaJson.get("format").textValue() : null;
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, elType, elFormat, null);
    }

    private Map<String, Object> annotationValueArrayToSubmap(Object[] a, String classifier, VisitorContext context) {
        Map<String, Object> mediaTypes = new LinkedHashMap<>();
        for (Object o : a) {
            AnnotationValue<?> sv = (AnnotationValue<?>) o;
            String name = sv.stringValue(classifier).orElse(null);
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
        final Optional<String> impl = av.stringValue("implementation");
        final Optional<String> not = av.stringValue("not");
        final Optional<String> schema = av.stringValue("schema");
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
        if (isSchema) {
            if (impl.isPresent()) {
                final String className = impl.get();
                bindSchemaForClassName(context, valueMap, className);
            }
            if (not.isPresent()) {
                final Schema schemaNot = resolveSchema(null, context.getClassElement(not.get()).get(), context, Collections.emptyList());
                Map<CharSequence, Object> schemaMap = new HashMap<>();
                schemaToValueMap(schemaMap, schemaNot);
                valueMap.put("not", schemaMap);
            }
            anyOf.ifPresent(anyOfList -> bindSchemaForComposite(context, valueMap, anyOfList, "anyOf"));
            oneOf.ifPresent(oneOfList -> bindSchemaForComposite(context, valueMap, oneOfList, "oneOf"));
            allOf.ifPresent(allOfList -> bindSchemaForComposite(context, valueMap, allOfList, "allOf"));
        }
        if (DiscriminatorMapping.class.getName().equals(av.getAnnotationName()) && schema.isPresent()) {
            final String className = schema.get();
            bindSchemaForClassName(context, valueMap, className);
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

    private void checkAllOf(Schema<Object> composedSchema) {
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
        Map<String, Schema> schemas = SchemaUtils.resolveSchemas(openAPI);
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
                JavadocDescription javadoc = Utils.getJavadocParser().parse(type.getDocumentation().orElse(null));
                if (schema == null) {

                    if (type instanceof EnumElement) {
                        schema = new Schema();
                        schema.setName(schemaName);
                        if (javadoc != null && StringUtils.hasText(javadoc.getMethodDescription())) {
                            schema.setDescription(javadoc.getMethodDescription());
                        }
                        schemas.put(schemaName, schema);
                        if (schema.getType() == null) {
                            schema.setType(PrimitiveType.STRING.getCommonName());
                        }
                        if (CollectionUtils.isEmpty(schema.getEnum())) {
                            schema.setEnum(getEnumValues((EnumElement) type, schema.getType(), schema.getFormat(), context));
                        }
                    } else {
                        if (type instanceof TypedElement) {
                            ClassElement classElement = ((TypedElement) type).getType();

                            List<ClassElement> superTypes = new ArrayList<>();
                            Collection<ClassElement> parentInterfaces = classElement.getInterfaces();
                            if (classElement.isInterface() && !parentInterfaces.isEmpty()) {
                                for (ClassElement parentInterface : parentInterfaces) {
                                    if (ClassUtils.isJavaLangType(parentInterface.getName())
                                        || parentInterface.getBeanProperties().isEmpty()) {
                                        continue;
                                    }
                                    superTypes.add(parentInterface);
                                }
                            } else {
                                classElement.getSuperType().ifPresent(superTypes::add);
                            }

                            if (!type.isRecord() && !superTypes.isEmpty()) {
                                schema = new ComposedSchema();
                                for (ClassElement sType : superTypes) {
                                    if (!type.isRecord()) {
                                        readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, sType, schemas);
                                    }
                                }
                            } else {
                                schema = new Schema();
                            }
                        } else {
                            schema = new Schema();
                        }
                        schema.setType("object");
                        schema.setName(schemaName);
                        if (javadoc != null && StringUtils.hasText(javadoc.getMethodDescription())) {
                            schema.setDescription(javadoc.getMethodDescription());
                        }
                        schemas.put(schemaName, schema);

                        populateSchemaProperties(openAPI, context, type, schema, mediaTypes, javadoc);
                        checkAllOf(schema);
                    }
                }
            } else {
                return primitiveType.createProperty();
            }
        } else {
            String schemaName = schemaValue.stringValue("name").orElse(computeDefaultSchemaName(definingElement, type));
            schema = schemas.get(schemaName);
            if (schema == null) {
                if (inProgressSchemas.contains(schemaName)) {
                    // Break recursion
                    return new Schema<>().$ref(SchemaUtils.schemaRef(schemaName));
                }
                inProgressSchemas.add(schemaName);
                try {
                    schema = readSchema(schemaValue, openAPI, context, type, mediaTypes);
                    AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> typeSchema = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    if (typeSchema != null) {
                        Schema originalTypeSchema = readSchema(typeSchema, openAPI, context, type, mediaTypes);
                        if (originalTypeSchema != null && schema != null) {
                            if (StringUtils.isNotEmpty(originalTypeSchema.getDescription())) {
                                schema.setDescription(originalTypeSchema.getDescription());
                            }
                            schema.setNullable(originalTypeSchema.getNullable());
                            schema.setRequired(originalTypeSchema.getRequired());
                        }
                    }

                    if (schema != null) {

                        ClassElement classElement = ((TypedElement) type).getType();
                        List<ClassElement> superTypes = new ArrayList<>();
                        Collection<ClassElement> parentInterfaces = classElement.getInterfaces();
                        if (classElement.isInterface() && !parentInterfaces.isEmpty()) {
                            for (ClassElement parentInterface : parentInterfaces) {
                                if (ClassUtils.isJavaLangType(parentInterface.getName())
                                    || parentInterface.getBeanProperties().isEmpty()) {
                                    continue;
                                }
                                superTypes.add(parentInterface);
                            }
                        }
                        if (!superTypes.isEmpty()) {
                            ComposedSchema schema1 = new ComposedSchema();
                            schema1.addAllOfItem(schema);

                            for (ClassElement sType : superTypes) {
                                String schemaName2 = computeDefaultSchemaName(definingElement, sType);
                                Schema parentSchema = new Schema();
                                parentSchema.set$ref(SchemaUtils.schemaRef(schemaName2));
                                schema1.addAllOfItem(parentSchema);
                            }

                            schema = schema1;
                        }

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
            schemaRef.set$ref(SchemaUtils.schemaRef(schema.getName()));
            if (definingElement instanceof ClassElement && ((ClassElement) definingElement).isIterable()) {
                schemaRef.setDescription(schema.getDescription());
            }
            return schemaRef;
        }
        return null;
    }

    private void readAllInterfaces(OpenAPI openAPI, VisitorContext context, @Nullable Element definingElement, List<MediaType> mediaTypes,
                                   Schema schema, ClassElement superType, Map<String, Schema> schemas) {
        String parentSchemaName = computeDefaultSchemaName(definingElement, superType);
        if (schemas.get(parentSchemaName) != null
            || getSchemaDefinition(openAPI, context, superType, null, mediaTypes) != null) {
            Schema parentSchema = new Schema();
            parentSchema.set$ref(SchemaUtils.schemaRef(parentSchemaName));
            schema.addAllOfItem(parentSchema);
        }
        if (superType.isInterface()) {
            for (ClassElement interfaceElement : superType.getInterfaces()) {
                if (ClassUtils.isJavaLangType(interfaceElement.getName())
                    || interfaceElement.getBeanProperties().isEmpty()) {
                    continue;
                }
                readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, interfaceElement, schemas);
            }
        } else if (superType.getSuperType().isPresent()) {
            readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, superType.getSuperType().get(), schemas);
        }
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

        String elType = (String) values.get("type");
        String elFormat = (String) values.get("format");
        if (elType == null && type != null) {
            Pair<String, String> typeAndFormat = ConvertUtils.getTypeAndFormatByClass(type.getName());
            elType = typeAndFormat.getFirst();
            if (elFormat == null) {
                elFormat = typeAndFormat.getSecond();
            }
        }

        if (values.containsKey("allowableValues")) {
            String[] allowableValues = (String[]) values.get("allowableValues");
            if (ArrayUtils.isNotEmpty(allowableValues)) {
                for (String allowableValue : allowableValues) {
                    if (schema.getEnum() == null || !schema.getEnum().contains(allowableValue)) {
                        try {
                            schema.addEnumItemObject(ConvertUtils.normalizeValue(allowableValue, elType, elFormat, context));
                        } catch (IOException e) {
                            context.warn("Can't convert " + allowableValue + " to " + elType + ": " + e.getMessage(), type);
                            schema.addEnumItemObject(allowableValue);
                        }
                    }
                }
            }
        }
        String defaultValue = schemaValue.stringValue("defaultValue").orElse(null);
        try {
            schema.setDefault(ConvertUtils.normalizeValue(defaultValue, elType, elFormat, context));
        } catch (IOException e) {
            context.warn("Can't convert " + defaultValue + " to " + elType + ": " + e.getMessage(), type);
            schema.setDefault(defaultValue);
        }

        Schema composedSchema = schema;
        final AnnotationClassValue<?> not = (AnnotationClassValue<?>) values.get("not");
        if (not != null) {
            final Schema schemaNot = resolveSchema(null, context.getClassElement(not.getName()).get(), context, Collections.emptyList());
            composedSchema.setNot(schemaNot);
        }
        final AnnotationClassValue<?>[] allOf = (AnnotationClassValue<?>[]) values.get("allOf");
        if (ArrayUtils.isNotEmpty(allOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, allOf, mediaTypes);
            for (Schema<?> s : schemaList) {
                composedSchema.addAllOfItem(s);
            }
        }
        final AnnotationClassValue<?>[] anyOf = (AnnotationClassValue<?>[]) values.get("anyOf");
        if (ArrayUtils.isNotEmpty(anyOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, anyOf, mediaTypes);
            for (Schema<?> s : schemaList) {
                composedSchema.addAnyOfItem(s);
            }
        }
        final AnnotationClassValue<?>[] oneOf = (AnnotationClassValue<?>[]) values.get("oneOf");
        if (ArrayUtils.isNotEmpty(oneOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, oneOf, mediaTypes);
            for (Schema<?> s : schemaList) {
                composedSchema.addOneOfItem(s);
            }
        }

        if (schema.getType() == null) {
            schema.setType("object");
        }
        if (type instanceof EnumElement) {
            elType = elType != null ? elType : PrimitiveType.STRING.getCommonName();
            schema.setType(elType);
            if (CollectionUtils.isEmpty(schema.getEnum())) {
                schema.setEnum(getEnumValues((EnumElement) type, elType, elFormat, context));
            }
        } else {
            JavadocDescription javadoc = Utils.getJavadocParser().parse(type.getDescription());
            populateSchemaProperties(openAPI, context, type, schema, mediaTypes, javadoc);
            checkAllOf(composedSchema);
        }
        return schema;
    }

    private List<Object> getEnumValues(EnumElement type, String schemaType, String schemaFormat, VisitorContext context) {
        List<Object> enumValues = new ArrayList<>();
        for (EnumConstantElement element : type.elements()) {

            AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn = element.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            boolean isHidden = schemaAnn != null && schemaAnn.booleanValue("hidden").orElse(false);

            if (isHidden
                || element.isAnnotationPresent(Hidden.class)
                || element.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            AnnotationValue<JsonProperty> jsonProperty = element.getAnnotation(JsonProperty.class);
            String jacksonValue = jsonProperty != null ? jsonProperty.stringValue("value").get() : null;
            if (StringUtils.hasText(jacksonValue)) {
                try {
                    enumValues.add(ConvertUtils.normalizeValue(jacksonValue, schemaType, schemaFormat, context));
                } catch (JsonProcessingException e) {
                    context.warn("Error converting jacksonValue " + jacksonValue + " : to " + type + ": " + e.getMessage(), element);
                    enumValues.add(element.getSimpleName());
                }
            } else {
                enumValues.add(element.getSimpleName());
            }
        }
        return enumValues;
    }

    private List<Schema<?>> namesToSchemas(OpenAPI openAPI, VisitorContext context, AnnotationClassValue<?>[] names, List<MediaType> mediaTypes) {
        return Arrays.stream(names)
            .flatMap((Function<AnnotationClassValue<?>, Stream<Schema<?>>>) classAnn -> {
                final Optional<ClassElement> classElement = context.getClassElement(classAnn.getName());
                if (classElement.isPresent()) {
                    final Schema<?> schemaDefinition = getSchemaDefinition(openAPI, context, classElement.get(), null, mediaTypes);
                    if (schemaDefinition != null) {
                        return Stream.of(schemaDefinition);
                    }
                }

                return Stream.empty();
            }).collect(Collectors.toList());
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

    private void populateSchemaProperties(OpenAPI openAPI, VisitorContext context, Element type, Schema schema, List<MediaType> mediaTypes, JavadocDescription classJavadoc) {
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
            processPropertyElements(openAPI, context, type, schema, beanProperties, mediaTypes, classJavadoc);

            final List<FieldElement> publicFields = classElement.getEnclosedElements(ElementQuery.ALL_FIELDS.modifiers(mods -> mods.contains(ElementModifier.PUBLIC) && mods.size() == 1));

            processPropertyElements(openAPI, context, type, schema, publicFields, mediaTypes, classJavadoc);
        }
    }

    private void processPropertyElements(OpenAPI openAPI, VisitorContext context, Element type, Schema schema, List<? extends TypedElement> publicFields, List<MediaType> mediaTypes, JavadocDescription classJavadoc) {

        ClassElement classElement = null;
        if (type instanceof ClassElement) {
            classElement = (ClassElement) type;
        } else if (type instanceof TypedElement) {
            classElement = ((TypedElement) type).getType();
        }


        for (TypedElement publicField : publicFields) {
            boolean isHidden = publicField.getAnnotationMetadata().booleanValue(io.swagger.v3.oas.annotations.media.Schema.class, "hidden").orElse(false);
            AnnotationValue<JsonAnySetter> jsonAnySetterAnn = publicField.getAnnotation(JsonAnySetter.class);
            if (publicField.isAnnotationPresent(JsonIgnore.class)
                || publicField.isAnnotationPresent(Hidden.class)
                || (jsonAnySetterAnn != null && jsonAnySetterAnn.booleanValue("enabled").orElse(true))
                || isHidden) {
                continue;
            }

            JavadocDescription fieldJavadoc = null;
            if (classElement != null) {
                for (FieldElement field : classElement.getFields()) {
                    if (field.getName().equals(publicField.getName())) {
                        fieldJavadoc = Utils.getJavadocParser().parse(field.getDocumentation().orElse(null));
                        break;
                    }
                }
            }

            if (publicField instanceof MemberElement && ((MemberElement) publicField).getDeclaringType().getType().getName().equals(type.getName())) {

                Schema propertySchema = resolveSchema(openAPI, publicField, publicField.getType(), context, mediaTypes, fieldJavadoc, classJavadoc);

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
        final OpenAPI openAPI = Utils.resolveOpenAPI(context);
        for (AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityScheme> securityRequirementAnnotationValue : values) {

            final Map<CharSequence, Object> map = toValueMap(securityRequirementAnnotationValue.getValues(), context);

            securityRequirementAnnotationValue.stringValue("name")
                .ifPresent(name -> {
                    if (map.containsKey("paramName")) {
                        map.put("name", map.remove("paramName"));
                    }

                    Utils.normalizeEnumValues(map, CollectionUtils.mapOf("type", SecurityScheme.Type.class, "in", SecurityScheme.In.class));

                    String type = (String) map.get("type");
                    if (!SecurityScheme.Type.APIKEY.toString().equals(type)) {
                        removeAndWarnSecSchemeProp(map, "name", context);
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
                            context.warn("Can't use http security scheme without 'scheme' property", null);
                        } else if (!map.get("scheme").equals("bearer") && map.containsKey("bearerFormat")) {
                            context.warn("Should NOT have a `bearerFormat` property without `scheme: bearer` being set", null);
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
                            resolveComponents(openAPI).addSecuritySchemes(name, securityScheme);
                        }
                    } catch (JsonProcessingException e) {
                        // ignore
                    }
                });
        }
    }

    private void removeAndWarnSecSchemeProp(Map<CharSequence, Object> map, String prop, VisitorContext context) {
        if (map.containsKey(prop)) {
            context.warn("'" + prop + "' property can't set for securityScheme with type " + map.get("type") + ". Skip it", null);
        }
        map.remove(prop);
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
}
