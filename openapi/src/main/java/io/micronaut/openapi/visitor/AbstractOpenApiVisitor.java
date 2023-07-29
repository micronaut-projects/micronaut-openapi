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

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
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
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.http.uri.UriMatchVariable;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.EnumConstantElement;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.GenericPlaceholderElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.ast.WildcardElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue;
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
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.micronaut.openapi.visitor.ConvertUtils.parseJsonString;
import static io.micronaut.openapi.visitor.ConvertUtils.resolveExtensions;
import static io.micronaut.openapi.visitor.ConvertUtils.setDefaultValueObject;
import static io.micronaut.openapi.visitor.ElementUtils.isFileUpload;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.expandProperties;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.getConfigurationProperty;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.getExpandableProperties;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.isJsonViewDefaultInclusion;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.resolvePlaceholders;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static java.util.stream.Collectors.toMap;

/**
 * Abstract base class for OpenAPI visitors.
 *
 * @author graemerocher
 * @since 1.0
 */
@Internal
abstract class AbstractOpenApiVisitor {

    private static final Lock VISITED_ELEMENTS_LOCK = new ReentrantLock();

    /**
     * Stores relations between schema names and class names.
     */
    private final Map<String, String> schemaNameToClassNameMap = new HashMap<>();
    /**
     * Stores class name counters for schema suffix, when found classes with same name in different packages.
     */
    private final Map<String, Integer> shemaNameSuffixCounterMap = new HashMap<>();
    /**
     * Stores the current in progress type.
     */
    private final List<String> inProgressSchemas = new ArrayList<>(10);
    /**
     * {@link PropertyNamingStrategy} instances cache.
     */
    private final Map<String, PropertyNamingStrategy> propertyNamingStrategyInstances = new HashMap<>();

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
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The node
     */
    JsonNode toJson(Map<CharSequence, Object> values, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        Map<CharSequence, Object> newValues = toValueMap(values, context, jsonViewClass);
        return ConvertUtils.getJsonMapper().valueToTree(newValues);
    }

    /**
     * Convert the given Map to a JSON node and then to the specified type.
     *
     * @param <T> The output class type
     * @param values The values
     * @param context The visitor context
     * @param type The class
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The converted instance
     */
    <T> Optional<T> toValue(Map<CharSequence, Object> values, VisitorContext context, Class<T> type, @Nullable ClassElement jsonViewClass) {
        JsonNode node = toJson(values, context, jsonViewClass);
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
    Map<String, List<PathItem>> resolvePathItems(VisitorContext context, List<UriMatchTemplate> matchTemplates) {
        OpenAPI openAPI = Utils.resolveOpenApi(context);
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            paths = new Paths();
            openAPI.setPaths(paths);
        }

        Map<String, List<PathItem>> resultPathItemsMap = new HashMap<>();

        for (UriMatchTemplate matchTemplate : matchTemplates) {

            StringBuilder result = new StringBuilder();

            boolean optionalPathVar = false;
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
                            needToSkip = true;
                            result.deleteCharAt(result.length() - 1);
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

            String resultPath = OpenApiApplicationVisitor.replacePlaceholders(result.toString(), context);

            if (!resultPath.startsWith("/") && !resultPath.startsWith("$")) {
                resultPath = "/" + resultPath;
            }
            String contextPath = OpenApiApplicationVisitor.getConfigurationProperty(OpenApiApplicationVisitor.MICRONAUT_SERVER_CONTEXT_PATH, context);
            if (StringUtils.isNotEmpty(contextPath)) {
                if (!contextPath.startsWith("/") && !contextPath.startsWith("$")) {
                    contextPath = "/" + contextPath;
                }
                if (contextPath.endsWith("/")) {
                    contextPath = contextPath.substring(0, contextPath.length() - 1);
                }
                resultPath = contextPath + resultPath;
            }

            Map<Integer, String> finalPaths = new HashMap<>();
            finalPaths.put(-1, resultPath);
            if (CollectionUtils.isNotEmpty(matchTemplate.getVariables())) {
                List<String> optionalVars = new ArrayList<>();
                // need check not required path varibales
                for (UriMatchVariable var : matchTemplate.getVariables()) {
                    if (var.isQuery() || !var.isOptional() || var.isExploded()) {
                        continue;
                    }
                    optionalVars.add(var.getName());
                }
                if (CollectionUtils.isNotEmpty(optionalVars)) {

                    int i = 0;
                    for (String var : optionalVars) {
                        if (finalPaths.isEmpty()) {
                            finalPaths.put(i, resultPath + "/{" + var + '}');
                            i++;
                            continue;
                        }
                        for (Map.Entry<Integer, String> entry : finalPaths.entrySet()) {
                            if (entry.getKey() + 1 < i) {
                                continue;
                            }
                            finalPaths.put(i, entry.getValue() + "/{" + var + '}');
                        }
                        i++;
                    }
                }
            }

            for (String finalPath : finalPaths.values()) {
                List<PathItem> resultPathItems = resultPathItemsMap.computeIfAbsent(finalPath, k -> new ArrayList<>());
                resultPathItems.add(paths.computeIfAbsent(finalPath, key -> new PathItem()));
            }
        }

        return resultPathItemsMap;
    }

    private List<String> addOptionalVars(List<String> paths, String var, int level) {
        List<String> additionalPaths = new ArrayList<>(paths);
        if (paths.isEmpty()) {
            additionalPaths.add("/{" + var + '}');
        } else {
            for (String path : paths) {
                additionalPaths.add(path + "/{" + var + '}');
            }
        }
        return additionalPaths;
    }

    /**
     * Convert the values to a map.
     *
     * @param values The values
     * @param context The visitor context
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The map
     */
    protected Map<CharSequence, Object> toValueMap(Map<CharSequence, Object> values, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        Map<CharSequence, Object> newValues = new HashMap<>(values.size());
        for (Map.Entry<CharSequence, Object> entry : values.entrySet()) {
            CharSequence key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AnnotationValue<?> av) {
                if (av.getAnnotationName().equals(io.swagger.v3.oas.annotations.media.ArraySchema.class.getName())) {
                    final Map<CharSequence, Object> valueMap = resolveArraySchemaAnnotationValues(context, av, jsonViewClass);
                    newValues.put("schema", valueMap);
                } else {
                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av, jsonViewClass);
                    newValues.put(key, valueMap);
                }
            } else if (value instanceof AnnotationClassValue<?> acv) {
                final Optional<? extends Class<?>> type = acv.getType();
                type.ifPresent(aClass -> newValues.put(key, aClass));
            } else if (value != null) {
                if (value.getClass().isArray()) {
                    Object[] a = (Object[]) value;
                    if (ArrayUtils.isNotEmpty(a)) {
                        Object first = a[0];

                        // are class values
                        if (first instanceof AnnotationClassValue) {
                            List<Class<?>> classes = new ArrayList<>(a.length);
                            for (Object o : a) {
                                AnnotationClassValue<?> acv = (AnnotationClassValue<?>) o;
                                acv.getType().ifPresent(classes::add);
                            }
                            newValues.put(key, classes);
                        } else if (first instanceof AnnotationValue<?> annValue) {
                            String annotationName = annValue.getAnnotationName();
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
                                Map<String, Object> encodings = annotationValueArrayToSubmap(a, "name", context, null);
                                newValues.put(key, encodings);
                            } else if (Content.class.getName().equals(annotationName)) {
                                Map<String, Object> mediaTypes = annotationValueArrayToSubmap(a, "mediaType", context, jsonViewClass);
                                newValues.put(key, mediaTypes);
                            } else if (Link.class.getName().equals(annotationName) || Header.class.getName().equals(annotationName)) {
                                Map<String, Object> linksOrHeaders = annotationValueArrayToSubmap(a, "name", context, jsonViewClass);
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
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, jsonViewClass);
                                    if (map.containsKey("ref")) {
                                        Object ref = map.get("ref");
                                        map.clear();
                                        map.put("$ref", ref);
                                    }

                                    try {
                                        if (!map.containsKey("description")) {
                                            map.put("description", name.equals("default") ? "OK response" : HttpStatus.valueOf(Integer.parseInt(name)).getReason());
                                        }
                                    } catch (Exception e) {
                                        map.put("description", "Response " + name);
                                    }

                                    responses.put(name, map);
                                }
                                newValues.put(key, responses);
                            } else if (ExampleObject.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> examples = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ExampleObject> sv = (AnnotationValue<ExampleObject>) o;
                                    String name = sv.stringValue("name").orElse("example");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, null);
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
                                    Map<CharSequence, Object> variables = new LinkedHashMap<>(toValueMap(sv.getValues(), context, null));
                                    servers.add(variables);
                                }
                                newValues.put(key, servers);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> variables = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    Optional<String> n = sv.stringValue("name");
                                    n.ifPresent(name -> {
                                        Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, null);
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
                                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, dv, null);
                                    mappings.put(valueMap.get("value").toString(), valueMap.get("$ref").toString());
                                }
                                final Map<String, Object> discriminatorMap = getDiscriminatorMap(newValues);
                                discriminatorMap.put("mapping", mappings);
                                newValues.put("discriminator", discriminatorMap);
                            } else {
                                if (a.length == 1) {
                                    final AnnotationValue<?> av = (AnnotationValue<?>) a[0];
                                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av, jsonViewClass);
                                    newValues.put(key, toValueMap(valueMap, context, jsonViewClass));
                                } else {

                                    List<Object> list = new ArrayList<>();
                                    for (Object o : a) {
                                        if (o instanceof AnnotationValue<?> av) {
                                            final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av, jsonViewClass);
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
                } else if (key.equals("additionalProperties")) {
                    if (AdditionalPropertiesValue.TRUE.toString().equals(value.toString())) {
                        newValues.put("additionalProperties", true);
                        // TODO
//                    } else if (AdditionalPropertiesValue.USE_ADDITIONAL_PROPERTIES_ANNOTATION.toString().equals(value.toString())) {
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
                                    break;
                                }
                            }
                        }
                        if (encodingStyle != null) {
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

    private <T extends Schema<?>> void processAnnotationValue(VisitorContext context, AnnotationValue<?> annotationValue,
                                                                                                                       Map<CharSequence, Object> arraySchemaMap, List<String> filters, Class<T> type, @Nullable ClassElement jsonViewClass) {
        Map<CharSequence, Object> values = annotationValue.getValues().entrySet().stream()
            .filter(entry -> filters == null || !filters.contains((String) entry.getKey()))
            .collect(toMap(e -> e.getKey().equals("requiredProperties") ? "required" : e.getKey(), Map.Entry::getValue));
        Optional<T> schema = toValue(values, context, type, jsonViewClass);
        schema.ifPresent(s -> schemaToValueMap(arraySchemaMap, s));
    }

    private Map<CharSequence, Object> resolveArraySchemaAnnotationValues(VisitorContext context, AnnotationValue<?> av, @Nullable ClassElement jsonViewClass) {
        final Map<CharSequence, Object> arraySchemaMap = new HashMap<>(10);
        // properties
        av.get("arraySchema", AnnotationValue.class).ifPresent(annotationValue ->
            processAnnotationValue(context, (AnnotationValue<?>) annotationValue, arraySchemaMap, Arrays.asList("ref", "implementation"), Schema.class, null)
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
                    final ArraySchema schema = SchemaUtils.arraySchema(resolveSchema(null, classElement.get(), context, Collections.emptyList(), jsonViewClass));
                    schemaToValueMap(arraySchemaMap, schema);
                } else {
                    // For primitive type, just copy description field is present.
                    final Schema<?> items = primitiveType.createProperty();
                    items.setDescription((String) annotationValue.stringValue("description").orElse(null));
                    final ArraySchema schema = SchemaUtils.arraySchema(items);
                    schemaToValueMap(arraySchemaMap, schema);
                }
            } else {
                arraySchemaMap.putAll(resolveAnnotationValues(context, annotationValue, jsonViewClass));
            }
        });
        // other properties (minItems,...)
        processAnnotationValue(context, av, arraySchemaMap, Arrays.asList("schema", "arraySchema"), ArraySchema.class, null);
        return arraySchemaMap;
    }

    private Map<CharSequence, Object> resolveAnnotationValues(VisitorContext context, AnnotationValue<?> av, @Nullable ClassElement jsonViewClass) {
        final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context, jsonViewClass);
        bindSchemaIfNeccessary(context, av, valueMap, jsonViewClass);
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
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The schema or null if it cannot be resolved
     */
    @Nullable
    protected Schema<?> resolveSchema(@Nullable Element definingElement, ClassElement type, VisitorContext context, List<MediaType> mediaTypes, @Nullable ClassElement jsonViewClass) {
        return resolveSchema(Utils.resolveOpenApi(context), definingElement, type, context, mediaTypes, jsonViewClass, null, null);
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
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The schema or null if it cannot be resolved
     */
    @Nullable
    protected Schema<?> resolveSchema(OpenAPI openAPI, @Nullable Element definingElement, ClassElement type, VisitorContext context,
                                   List<MediaType> mediaTypes, @Nullable ClassElement jsonViewClass,
                                   JavadocDescription fieldJavadoc, JavadocDescription classJavadoc) {

        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotationValue = null;
        if (definingElement != null) {
            schemaAnnotationValue = definingElement.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        if (type != null && schemaAnnotationValue == null) {
            schemaAnnotationValue = type.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        boolean isSubstitudedType = false;
        if (schemaAnnotationValue != null) {
            String impl = schemaAnnotationValue.stringValue("implementation").orElse(null);
            if (StringUtils.isNotEmpty(impl)) {
                type = context.getClassElement(impl).orElse(type);
                isSubstitudedType = true;
            } else {
                String schemaType = schemaAnnotationValue.stringValue("type").orElse(null);
                if (StringUtils.isNotEmpty(schemaType) && !(type instanceof EnumElement)) {
                    PrimitiveType primitiveType = PrimitiveType.fromName(schemaType);
                    if (primitiveType != null && primitiveType != PrimitiveType.OBJECT) {
                        type = context.getClassElement(primitiveType.getKeyClass()).orElse(type);
                        isSubstitudedType = true;
                    }
                }
            }
        }

        Boolean isArray = null;
        Boolean isIterable = null;

        ClassElement componentType = type != null ? type.getFirstTypeArgument().orElse(null) : null;
        if (type instanceof WildcardElement wildcardEl) {
            type = CollectionUtils.isNotEmpty(wildcardEl.getUpperBounds()) ? wildcardEl.getUpperBounds().get(0) : null;
        } else if (type instanceof GenericPlaceholderElement placeholderEl) {
            isArray = type.isArray();
            isIterable = type.isIterable();
            type = placeholderEl.getResolved().orElse(CollectionUtils.isNotEmpty(placeholderEl.getBounds()) ? placeholderEl.getBounds().get(0) : null);
        }
        Map<String, ClassElement> typeArgs = type != null ? type.getTypeArguments() : null;

        Schema<?> schema = null;

        if (type instanceof EnumElement enumEl) {
            schema = getSchemaDefinition(openAPI, context, enumEl, typeArgs, definingElement, mediaTypes, jsonViewClass);
            if (isArray != null && isArray) {
                schema = SchemaUtils.arraySchema(schema);
            }
        } else if (type != null) {

            boolean isPublisher = false;
            boolean isObservable = false;
            boolean isNullable = false;
            // MultipartBody implements Publisher<CompletedPart> (Issue : #907)
            if (type.isAssignable("io.micronaut.http.server.multipart.MultipartBody")) {
                isPublisher = true;
                type = type.getInterfaces()
                    .stream()
                    .filter(i -> i.isAssignable("org.reactivestreams.Publisher"))
                    .findFirst()
                    .flatMap(ClassElement::getFirstTypeArgument)
                    .orElse(null);
                // StreamingFileUpload implements Publisher, but it should be not considered as a Publisher in the spec file
            } else if (!type.isAssignable("io.micronaut.http.multipart.StreamingFileUpload") && ElementUtils.isContainerType(type)) {
                isPublisher = (type.isAssignable("org.reactivestreams.Publisher") || type.isAssignable("kotlinx.coroutines.flow.Flow"))
                    && !type.isAssignable("reactor.core.publisher.Mono");
                isObservable = (type.isAssignable("io.reactivex.Observable") || type.isAssignable("io.reactivex.rxjava3.core.Observable"))
                    && !type.isAssignable("reactor.core.publisher.Mono");
                type = componentType;
                if (componentType != null) {
                    typeArgs = componentType.getTypeArguments();
                    componentType = componentType.getFirstTypeArgument().orElse(null);
                }
            } else if (isTypeNullable(type)) {
                isNullable = true;
                type = componentType;
                if (componentType != null) {
                    typeArgs = componentType.getTypeArguments();
                    componentType = componentType.getFirstTypeArgument().orElse(null);
                }
            }

            if (type != null) {

                if (isArray == null) {
                    isArray = type.isArray();
                }
                if (isIterable == null) {
                    isIterable = type.isIterable();
                }

                String typeName = type.getName();
                ClassElement customTypeSchema = OpenApiApplicationVisitor.getCustomSchema(typeName, typeArgs, context);
                if (customTypeSchema != null) {
                    type = customTypeSchema;
                }

                // File upload case
                if (isFileUpload(type)) {
                    isPublisher = isPublisher && !"io.micronaut.http.multipart.PartData".equals(typeName);
                    // For file upload, we use PrimitiveType.BINARY
                    typeName = PrimitiveType.BINARY.name();
                }
                PrimitiveType primitiveType = PrimitiveType.fromName(typeName);
                if (!isArray && ClassUtils.isJavaLangType(typeName)) {
                    schema = getPrimitiveType(typeName);
                } else if (!isArray && primitiveType != null) {
                    schema = primitiveType.createProperty();
                } else if (type.isAssignable(Map.class.getName())) {
                    schema = new MapSchema();
                    if (CollectionUtils.isEmpty(typeArgs)) {
                        schema.setAdditionalProperties(true);
                    } else {
                        ClassElement valueType = typeArgs.get("V");
                        if (valueType.getName().equals(Object.class.getName())) {
                            schema.setAdditionalProperties(true);
                        } else {
                            schema.setAdditionalProperties(resolveSchema(openAPI, type, valueType, context, mediaTypes, jsonViewClass, null, classJavadoc));
                        }
                    }
                } else if (isIterable) {
                    if (isArray) {
                        schema = resolveSchema(openAPI, type, type.fromArray(), context, mediaTypes, jsonViewClass, null, classJavadoc);
                        if (schema != null) {
                            schema = SchemaUtils.arraySchema(schema);
                        }
                    } else {
                        if (componentType != null) {
                            schema = resolveSchema(openAPI, type, componentType, context, mediaTypes, jsonViewClass, null, classJavadoc);
                        } else {
                            schema = getPrimitiveType(Object.class.getName());
                        }
                        List<FieldElement> fields = type.getPackageName().startsWith("java.util") ? Collections.emptyList() : type.getFields();
                        if (schema != null && fields.isEmpty()) {
                            schema = SchemaUtils.arraySchema(schema);
                        } else {
                            schema = getSchemaDefinition(openAPI, context, type, typeArgs, definingElement, mediaTypes, jsonViewClass);
                        }
                    }
                } else if (ElementUtils.isReturnTypeFile(type)) {
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
                } else if (type.getName().equals(Object.class.getName())) {
                    schema = PrimitiveType.OBJECT.createProperty();
                } else {
                    schema = getSchemaDefinition(openAPI, context, type, typeArgs, definingElement, mediaTypes, jsonViewClass);
                }
            }

            if (schema != null) {

                if (isSubstitudedType) {
                    processShemaAnn(schema, context, definingElement, type, schemaAnnotationValue);
                }

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

    private void handleUnwrapped(VisitorContext context, Element element, ClassElement elementType, Schema<?> parentSchema, AnnotationValue<JsonUnwrapped> uw) {
        Map<String, Schema> schemas = SchemaUtils.resolveSchemas(Utils.resolveOpenApi(context));
        ClassElement customElementType = OpenApiApplicationVisitor.getCustomSchema(elementType.getName(), elementType.getTypeArguments(), context);
        String schemaName = element.stringValue(io.swagger.v3.oas.annotations.media.Schema.class, "name")
            .orElse(computeDefaultSchemaName(null, customElementType != null ? customElementType : elementType, elementType.getTypeArguments(), context, null));
        Schema<?> wrappedPropertySchema = schemas.get(schemaName);
        Map<String, Schema> properties = wrappedPropertySchema.getProperties();
        if (CollectionUtils.isEmpty(properties)) {
            return;
        }
        String prefix = uw.stringValue("prefix").orElse(StringUtils.EMPTY_STRING);
        String suffix = uw.stringValue("suffix").orElse(StringUtils.EMPTY_STRING);
        for (Entry<String, Schema> prop : properties.entrySet()) {
            try {
                String propertyName = prop.getKey();
                Schema<?> propertySchema = prop.getValue();
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
    protected void processSchemaProperty(VisitorContext context, TypedElement element, ClassElement elementType, @Nullable Element classElement,
                                                                              Schema<?> parentSchema, Schema<?> propertySchema) {
        if (propertySchema == null) {
            return;
        }
        AnnotationValue<JsonUnwrapped> uw = element.getAnnotation(JsonUnwrapped.class);
        if (uw != null && uw.booleanValue("enabled").orElse(Boolean.TRUE)) {
            handleUnwrapped(context, element, elementType, parentSchema, uw);
        } else {
            // check schema required flag
            AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotationValue = element.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            Optional<Boolean> elementSchemaRequired = Optional.empty();
            boolean isAutoRequiredMode = true;
            boolean isRequiredDefaultValueSet = false;
            if (schemaAnnotationValue != null) {
                elementSchemaRequired = schemaAnnotationValue.get("required", Argument.of(Boolean.TYPE));
                isRequiredDefaultValueSet = !schemaAnnotationValue.contains("required");
                io.swagger.v3.oas.annotations.media.Schema.RequiredMode requiredMode = schemaAnnotationValue.enumValue("requiredMode", io.swagger.v3.oas.annotations.media.Schema.RequiredMode.class).orElse(null);
                if (requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED) {
                    elementSchemaRequired = Optional.of(true);
                    isAutoRequiredMode = false;
                } else if (requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED) {
                    elementSchemaRequired = Optional.of(false);
                    isAutoRequiredMode = false;
                }
            }

            // check field annotaions (@NonNull, @Nullable, etc.)
            boolean isNotNullable = isElementNotNullable(element, classElement);
            // check as mandatory in constructor
            boolean isMandatoryInConstructor = doesParamExistsMandatoryInConstructor(element, classElement);
            boolean required = elementSchemaRequired.orElse(isNotNullable || isMandatoryInConstructor);

            if (isRequiredDefaultValueSet && isAutoRequiredMode && isNotNullable) {
                required = true;
            }

            propertySchema = bindSchemaForElement(context, element, elementType, propertySchema, null);
            String propertyName = resolvePropertyName(element, classElement, propertySchema);
            propertySchema.setRequired(null);
            Schema<?> propertySchemaFinal = propertySchema;
            addProperty(parentSchema, propertyName, propertySchema, required);
            if (schemaAnnotationValue != null) {
                schemaAnnotationValue.stringValue("defaultValue")
                    .ifPresent(value -> {
                        String elType = schemaAnnotationValue.stringValue("type").orElse(null);
                        String elFormat = schemaAnnotationValue.stringValue("format").orElse(null);
                        if (elType == null && elementType != null) {
                            Pair<String, String> typeAndFormat;
                            if (elementType instanceof EnumElement enumEl) {
                                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, elFormat);
                            } else {
                                typeAndFormat = ConvertUtils.getTypeAndFormatByClass(elementType.getName(), elementType.isArray());
                            }
                            elType = typeAndFormat.getFirst();
                            if (elFormat == null) {
                                elFormat = typeAndFormat.getSecond();
                            }
                        }
                        setDefaultValueObject(propertySchemaFinal, value, element, elType, elFormat, false, context);
                });
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
        if (classElement instanceof ClassElement classEl) {
            return classEl.getPrimaryConstructor().flatMap(methodElement -> Arrays.stream(methodElement.getParameters())
                    .filter(parameterElement -> parameterElement.getName().equals(element.getName()))
                    .map(parameterElement -> !parameterElement.isNullable())
                    .findFirst())
                .orElse(false);
        }

        return false;
    }

    private void addProperty(Schema<?> parentSchema, String name, Schema<?> propertySchema, boolean required) {
        parentSchema.addProperty(name, propertySchema);
        if (required) {
            List<String> requiredList = parentSchema.getRequired();
            // Check for duplicates
            if (requiredList == null || !requiredList.contains(name)) {
                parentSchema.addRequiredItem(name);
            }
        }
    }

    private String resolvePropertyName(Element element, Element classElement, Schema<?> propertySchema) {
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
            if (propertyNamingStrategyClass.isEmpty()) {
                return name;
            }
            PropertyNamingStrategy strategy = propertyNamingStrategyInstances.computeIfAbsent(propertyNamingStrategyClass.get(), clazz -> {
                try {
                    return (PropertyNamingStrategy) Class.forName(propertyNamingStrategyClass.get()).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Cannot instantiate: " + clazz);
                }
            });
            if (strategy instanceof PropertyNamingStrategies.NamingBase namingBase) {
                return namingBase.translate(name);
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
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The bound schema
     */
    protected Schema<?> bindSchemaForElement(VisitorContext context, TypedElement element, ClassElement elementType, Schema<?> schemaToBind,
                                          @Nullable ClassElement jsonViewClass) {
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn = element.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        Schema<?> originalSchema = schemaToBind;

        if (originalSchema.get$ref() != null) {
            Schema<?> schemaFromAnn = schemaFromAnnotation(context, element, elementType, schemaAnn);
            if (schemaFromAnn != null) {
                schemaToBind = schemaFromAnn;
            }
        }
        if (originalSchema.get$ref() == null && schemaAnn != null) {
            // Apply @Schema annotation only if not $ref since for $ref schemas
            // we already populated values from right @Schema annotation in previous steps
            schemaToBind = bindSchemaAnnotationValue(context, element, schemaToBind, schemaAnn, jsonViewClass);
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
            schemaToBind = bindArraySchemaAnnotationValue(context, element, schemaToBind, arraySchemaAnn, jsonViewClass);
            Optional<String> schemaName = arraySchemaAnn.stringValue("name");
            if (schemaName.isPresent()) {
                schemaToBind.setName(schemaName.get());
            }
        }

//        Schema finalSchemaToBind = schemaToBind;
        processJavaxValidationAnnotations(element, elementType, schemaToBind);

        final ComposedSchema composedSchema;
        final Schema<?> topLevelSchema;
        if (originalSchema.get$ref() != null) {
            composedSchema = new ComposedSchema();
            topLevelSchema = composedSchema;
        } else {
            composedSchema = new ComposedSchema();
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
            setDefaultValueObject(schemaToBind, defaultValue, elementType, schemaToBind.getType(), schemaToBind.getFormat(), true, context);
            notOnlyRef = true;
        }
        // @Schema annotation takes priority over nullability annotations
        Boolean isSchemaNullable = element.booleanValue(io.swagger.v3.oas.annotations.media.Schema.class, "nullable").orElse(null);
        boolean isNullable = (isSchemaNullable == null && ElementUtils.isNullable(element)) || Boolean.TRUE.equals(isSchemaNullable);
        if (isNullable) {
            topLevelSchema.setNullable(true);
            notOnlyRef = true;
        }
        final String defaultJacksonValue = element.stringValue(JsonProperty.class, "defaultValue").orElse(null);
        if (defaultJacksonValue != null && schemaToBind.getDefault() == null) {
            setDefaultValueObject(topLevelSchema, defaultJacksonValue, elementType, schemaToBind.getType(), schemaToBind.getFormat(), false, context);
            notOnlyRef = true;
        }

        boolean addSchemaToBind = !SchemaUtils.isEmptySchema(schemaToBind);

        if (addSchemaToBind) {
            if (TYPE_OBJECT.equals(originalSchema.getType())) {
                if (composedSchema.getType() == null) {
                    composedSchema.setType(TYPE_OBJECT);
                }
                originalSchema.setType(null);
            }
            if (!SchemaUtils.isEmptySchema(originalSchema)) {
                composedSchema.addAllOfItem(originalSchema);
            }
        } else if (isNullable && CollectionUtils.isEmpty(composedSchema.getAllOf())) {
            composedSchema.addAllOfItem(originalSchema);
        }
        if (addSchemaToBind && !schemaToBind.equals(originalSchema)) {
            if (TYPE_OBJECT.equals(schemaToBind.getType())) {
                if (composedSchema.getType() == null) {
                    composedSchema.setType(TYPE_OBJECT);
                }
                originalSchema.setType(null);
            }
            composedSchema.addAllOfItem(schemaToBind);
        }

        if (!SchemaUtils.isEmptySchema(composedSchema)
            && ((CollectionUtils.isNotEmpty(composedSchema.getAllOf()) && composedSchema.getAllOf().size() > 1)
            || CollectionUtils.isNotEmpty(composedSchema.getOneOf())
            || CollectionUtils.isNotEmpty(composedSchema.getAnyOf())
            || notOnlyRef)) {
            return composedSchema;
        }
        if (CollectionUtils.isNotEmpty(composedSchema.getAllOf()) && composedSchema.getAllOf().size() == 1) {
            return composedSchema.getAllOf().get(0);
        }

        return originalSchema;
    }

    protected void processJavaxValidationAnnotations(Element element, ClassElement elementType, Schema<?> schemaToBind) {

        final boolean isIterableOrMap = elementType.isIterable() || elementType.isAssignable(Map.class);

        if (isIterableOrMap) {
            if (element.isAnnotationPresent("javax.validation.constraints.NotEmpty$List")
                || element.isAnnotationPresent("jakarta.validation.constraints.NotEmpty$List")) {
                schemaToBind.setMinItems(1);
            }

            element.findAnnotation("javax.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("min")
                        .ifPresent(schemaToBind::setMinItems)));
            element.findAnnotation("jakarta.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("min")
                        .ifPresent(schemaToBind::setMinItems)));

            element.findAnnotation("javax.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("max")
                        .ifPresent(schemaToBind::setMaxItems)));
            element.findAnnotation("jakarta.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("max")
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
                        for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                            ann.intValue("min").ifPresent(schemaToBind::setMinLength);
                            ann.intValue("max").ifPresent(schemaToBind::setMaxLength);
                        }
                    });
                element.findAnnotation("jakarta.validation.constraints.Size$List")
                    .ifPresent(listAnn -> {
                        for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                            ann.intValue("min").ifPresent(schemaToBind::setMinLength);
                            ann.intValue("max").ifPresent(schemaToBind::setMaxLength);
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
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Min$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.Max$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Max$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.DecimalMin$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.DecimalMin$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.DecimalMax$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.DecimalMax$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });

            element.findAnnotation("javax.validation.constraints.Email$List")
                .ifPresent(listAnn -> {
                    schemaToBind.setFormat(PrimitiveType.EMAIL.getCommonName());
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Email$List")
                .ifPresent(listAnn -> {
                    schemaToBind.setFormat(PrimitiveType.EMAIL.getCommonName());
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });

            element.findAnnotation("javax.validation.constraints.Pattern$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });
            element.findAnnotation("jakarta.validation.constraints.Pattern$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations("value")) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });

            element.getValue("io.micronaut.http.annotation.Part", String.class).ifPresent(schemaToBind::setName);
        }
    }

    Schema<?> schemaFromAnnotation(VisitorContext context, Element element, ClassElement type, AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {
        if (schemaAnn == null) {
            return null;
        }

        var schemaToBind = new Schema<>();
        processShemaAnn(schemaToBind, context, element, type, schemaAnn);

        return schemaToBind;
    }

    void processShemaAnn(Schema<?> schemaToBind, VisitorContext context, Element element, ClassElement type, @NonNull AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {

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
            externalDocs = toValue(schemaExtDocs.getValues(), context, ExternalDocumentation.class, null).orElse(null);
        }
        if (externalDocs != null) {
            schemaToBind.setExternalDocs(externalDocs);
        }

        String schemaDefaultValue = (String) annValues.get("defaultValue");
        if (schemaDefaultValue != null) {
            setDefaultValueObject(schemaToBind, schemaDefaultValue, type, schemaToBind.getType(), schemaToBind.getFormat(), false, context);
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
            AccessMode schemaAccessMode = AccessMode.valueOf(accessModeStr);
            if (schemaAccessMode != AccessMode.AUTO) {
                if (schemaAccessMode == AccessMode.READ_ONLY) {
                    schemaToBind.setReadOnly(true);
                    schemaToBind.setWriteOnly(null);
                } else if (schemaAccessMode == AccessMode.WRITE_ONLY) {
                    schemaToBind.setReadOnly(false);
                    schemaToBind.setWriteOnly(null);
                } else if (schemaAccessMode == AccessMode.READ_WRITE) {
                    schemaToBind.setReadOnly(null);
                    schemaToBind.setWriteOnly(null);
                }
            }
        }

        OpenAPI openAPI = Utils.resolveOpenApi(context);
        Components components = resolveComponents(openAPI);

        processClassValues(schemaToBind, annValues, Collections.emptyList(), context, null);

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
    }

    private void setSchemaDocumentation(Element element, Schema<?> schemaToBind) {
        if (StringUtils.isEmpty(schemaToBind.getDescription())) {
            // First, find getter method javadoc
            String doc = element.getDocumentation().orElse(null);
            if (StringUtils.isEmpty(doc)) {
                // next, find field javadoc
                if (element instanceof MemberElement memberEl) {
                    List<FieldElement> fields = memberEl.getDeclaringType().getFields();
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
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The bound schema
     */
    protected Schema<?> bindSchemaAnnotationValue(VisitorContext context, Element element, Schema<?> schemaToBind,
                                               AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn,
                                               @Nullable ClassElement jsonViewClass) {

        ClassElement classElement = ((TypedElement) element).getType();
        Pair<String, String> typeAndFormat;
        if (classElement.isIterable()) {
            typeAndFormat = Pair.of("array", null);
        } else if (classElement instanceof EnumElement enumEl) {
            typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, null);
        } else {
            typeAndFormat = ConvertUtils.getTypeAndFormatByClass(classElement.getName(), classElement.isArray());
        }

        JsonNode schemaJson = toJson(schemaAnn.getValues(), context, jsonViewClass);
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson,
            schemaAnn.stringValue("type").orElse(typeAndFormat.getFirst()),
            schemaAnn.stringValue("format").orElse(typeAndFormat.getSecond()),
            schemaAnn, jsonViewClass);
    }

    private Schema<?> doBindSchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind,
                                               JsonNode schemaJson, String elType, String elFormat, AnnotationValue<?> schemaAnn,
                                               @Nullable ClassElement jsonViewClass) {

        // need to set placeholders to set correct values and types to example field
        schemaJson = resolvePlaceholders(schemaJson, s -> expandProperties(s, getExpandableProperties(context), context));
        try {
            schemaToBind = ConvertUtils.getJsonMapper().readerForUpdating(schemaToBind).readValue(schemaJson);
        } catch (IOException e) {
            context.warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), element);
        }

        String defaultValue = null;
        String[] allowableValues = null;
        if (schemaAnn != null) {
            defaultValue = schemaAnn.stringValue("defaultValue").orElse(null);
            allowableValues = schemaAnn.get("allowableValues", String[].class).orElse(null);
            Map<CharSequence, Object> annValues = schemaAnn.getValues();
            Map<CharSequence, Object> valueMap = toValueMap(annValues, context, jsonViewClass);
            bindSchemaIfNeccessary(context, schemaAnn, valueMap, jsonViewClass);
            processClassValues(schemaToBind, annValues, Collections.emptyList(), context, jsonViewClass);
        }

        if (elType == null && element != null) {
            ClassElement typeEl = ((TypedElement) element).getType();
            Pair<String, String> typeAndFormat;
            if (typeEl instanceof EnumElement enumEl) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, elFormat);
            } else {
                typeAndFormat = ConvertUtils.getTypeAndFormatByClass(typeEl.getName(), typeEl.isArray());
            }
            elType = typeAndFormat.getFirst();
            if (elFormat == null) {
                elFormat = typeAndFormat.getSecond();
            }
        }

        if (StringUtils.isNotEmpty(defaultValue)) {
            setDefaultValueObject(schemaToBind, defaultValue, element, elType, elFormat, false, context);
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
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return The bound schema
     */
    protected Schema<?> bindArraySchemaAnnotationValue(VisitorContext context, Element element, Schema<?> schemaToBind,
                                                    AnnotationValue<io.swagger.v3.oas.annotations.media.ArraySchema> schemaAnn,
                                                    @Nullable ClassElement jsonViewClass) {
        JsonNode schemaJson = toJson(schemaAnn.getValues(), context, jsonViewClass);
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
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, elType, elFormat, null, jsonViewClass);
    }

    private Map<String, Object> annotationValueArrayToSubmap(Object[] a, String classifier, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        Map<String, Object> mediaTypes = new LinkedHashMap<>();
        for (Object o : a) {
            AnnotationValue<?> sv = (AnnotationValue<?>) o;
            String name = sv.stringValue(classifier).orElse(null);
            if (name == null && classifier.equals("mediaType")) {
                name = MediaType.APPLICATION_JSON;
            }
            if (name != null) {
                Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, jsonViewClass);
                mediaTypes.put(name, map);
            }
        }
        return mediaTypes;
    }

    private void schemaToValueMap(Map<CharSequence, Object> valueMap, Schema<?> schema) {
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

    private void bindSchemaIfNeccessary(VisitorContext context, AnnotationValue<?> av, Map<CharSequence, Object> valueMap, @Nullable ClassElement jsonViewClass) {
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
                bindSchemaForClassName(context, valueMap, className, jsonViewClass);
            }
            if (not.isPresent()) {
                final Schema<?> schemaNot = resolveSchema(null, context.getClassElement(not.get()).get(), context, Collections.emptyList(), jsonViewClass);
                Map<CharSequence, Object> schemaMap = new HashMap<>();
                schemaToValueMap(schemaMap, schemaNot);
                valueMap.put("not", schemaMap);
            }
            anyOf.ifPresent(anyOfList -> bindSchemaForComposite(context, valueMap, anyOfList, "anyOf", jsonViewClass));
            oneOf.ifPresent(oneOfList -> bindSchemaForComposite(context, valueMap, oneOfList, "oneOf", jsonViewClass));
            allOf.ifPresent(allOfList -> bindSchemaForComposite(context, valueMap, allOfList, "allOf", jsonViewClass));
        }
        if (DiscriminatorMapping.class.getName().equals(av.getAnnotationName()) && schema.isPresent()) {
            final String className = schema.get();
            bindSchemaForClassName(context, valueMap, className, jsonViewClass);
        }
    }

    private void bindSchemaForComposite(VisitorContext context, Map<CharSequence, Object> valueMap, String[] classNames, String key, @Nullable ClassElement jsonViewClass) {
        final List<Map<CharSequence, Object>> namesToSchemas = Arrays.stream(classNames).map(className -> {
            final Optional<ClassElement> classElement = context.getClassElement(className);
            Map<CharSequence, Object> schemaMap = new HashMap<>();
            if (classElement.isPresent()) {
                final Schema<?> schema = resolveSchema(null, classElement.get(), context, Collections.emptyList(), jsonViewClass);
                schemaToValueMap(schemaMap, schema);
            }
            return schemaMap;
        }).collect(Collectors.toList());
        valueMap.put(key, namesToSchemas);
    }

    private void bindSchemaForClassName(VisitorContext context, Map<CharSequence, Object> valueMap, String className, @Nullable ClassElement jsonViewClass) {
        final Optional<ClassElement> classElement = context.getClassElement(className);
        if (classElement.isPresent()) {
            final Schema<?> schema = resolveSchema(null, classElement.get(), context, Collections.emptyList(), jsonViewClass);
            schemaToValueMap(valueMap, schema);
        }
    }

    private void checkAllOf(Schema<Object> composedSchema) {
        if (composedSchema == null || CollectionUtils.isEmpty(composedSchema.getAllOf()) || CollectionUtils.isEmpty(composedSchema.getProperties())) {
            return;
        }
        if (composedSchema.getType() == null) {
            composedSchema.setType(TYPE_OBJECT);
        }
        // put all properties as siblings of allOf
        Schema<?> propSchema = new Schema<>();
        propSchema.properties(composedSchema.getProperties());
        propSchema.setDescription(composedSchema.getDescription());
        propSchema.setRequired(composedSchema.getRequired());
        propSchema.setType(null);
        composedSchema.setProperties(null);
        composedSchema.setDescription(null);
        composedSchema.setRequired(null);
        composedSchema.addAllOfItem(propSchema);
    }

    private Schema<?> getSchemaDefinition(OpenAPI openAPI,
                                       VisitorContext context,
                                       ClassElement type,
                                       Map<String, ClassElement> typeArgs,
                                       @Nullable Element definingElement,
                                       List<MediaType> mediaTypes,
                                       @Nullable ClassElement jsonViewClass
                                       ) {

        // Here we need to skip Schema nnotation on field level, because with micronaut 3.x method getDeclaredAnnotation
        // returned always null and found Schema annotation only on getters and setters
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue = null;
        if (definingElement != null) {
            if (definingElement instanceof PropertyElement propertyEl) {
                var getterOpt = propertyEl.getReadMethod();
                if (getterOpt.isPresent()) {
                    schemaValue = getterOpt.get().getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                }
                if (schemaValue == null) {
                    var setterOpt = propertyEl.getWriteMethod();
                    if (setterOpt.isPresent()) {
                        schemaValue = setterOpt.get().getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    }
                }
            } else {
                schemaValue = definingElement.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            }
        }
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
                String schemaName = computeDefaultSchemaName(definingElement, type, typeArgs, context, jsonViewClass);
                schema = schemas.get(schemaName);
                JavadocDescription javadoc = Utils.getJavadocParser().parse(type.getDocumentation().orElse(null));
                if (schema == null) {

                    if (type instanceof EnumElement enumEl) {
                        schema = new Schema<>();
                        schema.setName(schemaName);
                        if (javadoc != null && StringUtils.hasText(javadoc.getMethodDescription())) {
                            schema.setDescription(javadoc.getMethodDescription());
                        }
                        schemas.put(schemaName, schema);

                        Pair<String, String> typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, schema.getType(), schema.getFormat());
                        schema.setType(typeAndFormat.getFirst());
                        schema.setFormat(typeAndFormat.getSecond());
                        if (CollectionUtils.isEmpty(schema.getEnum())) {
                            schema.setEnum(getEnumValues(enumEl, schema.getType(), schema.getFormat(), context));
                        }
                    } else {
                        Schema<?> schemaWithSuperTypes = processSuperTypes(null, schemaName, type, definingElement, openAPI, mediaTypes, schemas, context, jsonViewClass);
                        if (schemaWithSuperTypes != null) {
                            schema = schemaWithSuperTypes;
                        }
                        if (schema != null && javadoc != null && StringUtils.hasText(javadoc.getMethodDescription())) {
                            schema.setDescription(javadoc.getMethodDescription());
                        }

                        populateSchemaProperties(openAPI, context, type, typeArgs, schema, mediaTypes, javadoc, jsonViewClass);
                        checkAllOf(schema);
                    }
                }
            } else {
                return primitiveType.createProperty();
            }
        } else {
            String schemaName = schemaValue.stringValue("name")
                .orElse(computeDefaultSchemaName(definingElement, type, typeArgs, context, jsonViewClass));
            schema = schemas.get(schemaName);
            if (schema == null) {
                if (inProgressSchemas.contains(schemaName)) {
                    // Break recursion
                    return new Schema<>().$ref(SchemaUtils.schemaRef(schemaName));
                }
                inProgressSchemas.add(schemaName);
                try {
                    schema = readSchema(schemaValue, openAPI, context, type, typeArgs, mediaTypes, jsonViewClass);
                    AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> typeSchema = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    if (typeSchema != null) {
                        Schema<?> originalTypeSchema = readSchema(typeSchema, openAPI, context, type, typeArgs, mediaTypes, jsonViewClass);
                        if (originalTypeSchema != null && schema != null) {
                            if (StringUtils.isNotEmpty(originalTypeSchema.getDescription())) {
                                schema.setDescription(originalTypeSchema.getDescription());
                            }
                            schema.setNullable(originalTypeSchema.getNullable());
                            schema.setRequired(originalTypeSchema.getRequired());
                        }
                    }

                    if (schema != null) {
                        processSuperTypes(schema, schemaName, type, definingElement, openAPI, mediaTypes, schemas, context, jsonViewClass);
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
                externalDocs = toValue(externalDocsValue.getValues(), context, ExternalDocumentation.class, null).orElse(null);
            }
            if (externalDocs != null) {
                schema.setExternalDocs(externalDocs);
            }
            setSchemaDocumentation(type, schema);
            var schemaRef = new Schema<>();
            schemaRef.set$ref(SchemaUtils.schemaRef(schema.getName()));
            if (definingElement instanceof ClassElement classEl && classEl.isIterable()) {
                schemaRef.setDescription(schema.getDescription());
            }
            return schemaRef;
        }
        return null;
    }

    private Schema<?> processSuperTypes(Schema<?> schema,
                                     String schemaName,
                                     ClassElement type, @Nullable Element definingElement,
                                     OpenAPI openAPI,
                                     List<MediaType> mediaTypes,
                                     Map<String, Schema> schemas,
                                     VisitorContext context,
                                     @Nullable ClassElement jsonViewClass) {

        if (type.getName().equals(Object.class.getName())) {
            return null;
        }

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
            // skip if it is Enum or Object super class
            String firstSuperTypeName = superTypes.get(0).getName();
            if (superTypes.size() == 1
                && (firstSuperTypeName.equals(Enum.class.getName()) || firstSuperTypeName.equals(Object.class.getName()))) {
                schema.setName(schemaName);
                schemas.put(schemaName, schema);

                return null;
            }

            if (schema == null) {
                schema = new ComposedSchema();
                schema.setType(TYPE_OBJECT);
            }
            for (ClassElement sType : superTypes) {
                Map<String, ClassElement> sTypeArgs = sType.getTypeArguments();
                ClassElement customStype = OpenApiApplicationVisitor.getCustomSchema(sType.getName(), sTypeArgs, context);
                if (customStype != null) {
                    sType = customStype;
                }
                readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, sType, schemas, sTypeArgs, jsonViewClass);
            }
        } else {
            if (schema == null) {
                schema = new Schema<>();
                schema.setType(TYPE_OBJECT);
            }
        }

        schema.setName(schemaName);
        schemas.put(schemaName, schema);

        return schema;
    }

    @SuppressWarnings("java:S3655") // false positive
    private void readAllInterfaces(OpenAPI openAPI, VisitorContext context, @Nullable Element definingElement, List<MediaType> mediaTypes,
                                   Schema<?> schema, ClassElement superType, Map<String, Schema> schemas, Map<String, ClassElement> superTypeArgs,
                                   @Nullable ClassElement jsonViewClass) {
        String parentSchemaName = superType.stringValue(io.swagger.v3.oas.annotations.media.Schema.class, "name")
            .orElse(computeDefaultSchemaName(definingElement, superType, superTypeArgs, context, jsonViewClass));

        if (schemas.get(parentSchemaName) != null
            || getSchemaDefinition(openAPI, context, superType, superTypeArgs, null, mediaTypes, jsonViewClass) != null) {
            var parentSchema = new Schema<>();
            parentSchema.set$ref(SchemaUtils.schemaRef(parentSchemaName));
            if (schema.getAllOf() == null || !schema.getAllOf().contains(parentSchema)) {
                schema.addAllOfItem(parentSchema);
            }
        }
        if (superType.isInterface()) {
            for (ClassElement interfaceElement : superType.getInterfaces()) {
                if (ClassUtils.isJavaLangType(interfaceElement.getName())
                    || interfaceElement.getBeanProperties().isEmpty()) {
                    continue;
                }

                Map<String, ClassElement> interfaceTypeArgs = interfaceElement.getTypeArguments();
                ClassElement customInterfaceType = OpenApiApplicationVisitor.getCustomSchema(interfaceElement.getName(), interfaceTypeArgs, context);
                if (customInterfaceType != null) {
                    interfaceElement = customInterfaceType;
                }

                readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, interfaceElement, schemas, interfaceTypeArgs, jsonViewClass);
            }
        } else if (superType.getSuperType().isPresent()) {
            ClassElement superSuperType = superType.getSuperType().get();
            Map<String, ClassElement> superSuperTypeArgs = superSuperType.getTypeArguments();
            ClassElement customSuperSuperType = OpenApiApplicationVisitor.getCustomSchema(superSuperType.getName(), superSuperTypeArgs, context);
            if (customSuperSuperType != null) {
                superSuperType = customSuperSuperType;
            }
            readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, superSuperType, schemas, superSuperTypeArgs, jsonViewClass);
        }
    }

    private void processClassValues(Schema<?> schemaToBind, Map<CharSequence, Object> annValues, List<MediaType> mediaTypes, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        OpenAPI openAPI = Utils.resolveOpenApi(context);
        final AnnotationClassValue<?> not = (AnnotationClassValue<?>) annValues.get("not");
        if (not != null) {
            final Schema<?> schemaNot = resolveSchema(null, context.getClassElement(not.getName()).get(), context, Collections.emptyList(), jsonViewClass);
            schemaToBind.setNot(schemaNot);
        }
        final AnnotationClassValue<?>[] allOf = (AnnotationClassValue<?>[]) annValues.get("allOf");
        if (ArrayUtils.isNotEmpty(allOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, allOf, mediaTypes, jsonViewClass);
            for (Schema<?> s : schemaList) {
                if (TYPE_OBJECT.equals(s.getType())) {
                    if (schemaToBind.getType() == null) {
                        schemaToBind.setType(TYPE_OBJECT);
                    }
                    s.setType(null);
                }
                if (schemaToBind.getAllOf() == null || !schemaToBind.getAllOf().contains(s)) {
                    schemaToBind.addAllOfItem(s);
                }
            }
        }
        final AnnotationClassValue<?>[] anyOf = (AnnotationClassValue<?>[]) annValues.get("anyOf");
        if (ArrayUtils.isNotEmpty(anyOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, anyOf, mediaTypes, jsonViewClass);
            for (Schema<?> s : schemaList) {
                if (TYPE_OBJECT.equals(s.getType())) {
                    if (schemaToBind.getType() == null) {
                        schemaToBind.setType(TYPE_OBJECT);
                    }
                    s.setType(null);
                }
                if (schemaToBind.getAnyOf() == null || !schemaToBind.getAnyOf().contains(s)) {
                    schemaToBind.addAnyOfItem(s);
                }
            }
        }
        final AnnotationClassValue<?>[] oneOf = (AnnotationClassValue<?>[]) annValues.get("oneOf");
        if (ArrayUtils.isNotEmpty(oneOf)) {
            List<Schema<?>> schemaList = namesToSchemas(openAPI, context, oneOf, mediaTypes, jsonViewClass);
            for (Schema<?> s : schemaList) {
                if (TYPE_OBJECT.equals(s.getType())) {
                    if (schemaToBind.getType() == null) {
                        schemaToBind.setType(TYPE_OBJECT);
                    }
                    s.setType(null);
                }
                if (schemaToBind.getOneOf() == null || !schemaToBind.getOneOf().contains(s)) {
                    schemaToBind.addOneOfItem(s);
                }
            }
        }

    }

    /**
     * Reads schema.
     *
     * @param schemaValue annotation value
     * @param openAPI The OpenApi
     * @param context The VisitorContext
     * @param type type element
     * @param typeArgs type arguments
     * @param mediaTypes The media types of schema
     * @param jsonViewClass Class from JsonView annotation
     *
     * @return New schema instance
     *
     * @throws JsonProcessingException when Json parsing fails
     */
    @SuppressWarnings("java:S3776")
    protected Schema<?> readSchema(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue, OpenAPI openAPI, VisitorContext context,
                                @Nullable Element type, Map<String, ClassElement> typeArgs, List<MediaType> mediaTypes,
                                @Nullable ClassElement jsonViewClass) throws JsonProcessingException {
        Map<CharSequence, Object> values = schemaValue.getValues()
            .entrySet()
            .stream()
            .collect(toMap(e -> e.getKey().equals("requiredProperties") ? "required" : e.getKey(), Map.Entry::getValue));
        Optional<Schema> schemaOpt = toValue(values, context, Schema.class, jsonViewClass);
        if (schemaOpt.isEmpty()) {
            return null;
        }
        var schema = schemaOpt.get();

        String elType = (String) values.get("type");
        String elFormat = (String) values.get("format");
        if (elType == null && type instanceof TypedElement typedType) {
            Pair<String, String> typeAndFormat;
            if (typedType instanceof EnumElement enumEl) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, elFormat);
            } else {
                typeAndFormat = ConvertUtils.getTypeAndFormatByClass(typedType.getName(), typedType.isArray());
            }
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
        setDefaultValueObject(schema, defaultValue, type, elType, elFormat, false, context);

        processClassValues(schema, values, mediaTypes, context, jsonViewClass);

        if (schema.getType() == null) {
            schema.setType(elType);
        }
        if (schema.getFormat() == null) {
            schema.setFormat(elFormat);
        }
        if (type instanceof EnumElement enumEl) {
            if (CollectionUtils.isEmpty(schema.getEnum())) {
                schema.setEnum(getEnumValues(enumEl, schema.getType(), elFormat, context));
            }
        } else {
            JavadocDescription javadoc = Utils.getJavadocParser().parse(type.getDescription());
            populateSchemaProperties(openAPI, context, type, typeArgs, schema, mediaTypes, javadoc, jsonViewClass);
            checkAllOf(schema);
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
            String jacksonValue = jsonProperty != null ? jsonProperty.stringValue("value").orElse(null) : null;
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

    private List<Schema<?>> namesToSchemas(OpenAPI openAPI, VisitorContext context, AnnotationClassValue<?>[] names, List<MediaType> mediaTypes, @Nullable ClassElement jsonViewClass) {
        return Arrays.stream(names)
            .flatMap((Function<AnnotationClassValue<?>, Stream<Schema<?>>>) classAnn -> {
                final Optional<ClassElement> classElementOpt = context.getClassElement(classAnn.getName());
                if (classElementOpt.isPresent()) {
                    ClassElement classElement = classElementOpt.get();
                    Map<String, ClassElement> classElementTypeArgs = classElement.getTypeArguments();
                    ClassElement customClassElement = OpenApiApplicationVisitor.getCustomSchema(classElement.getName(), classElementTypeArgs, context);
                    if (customClassElement != null) {
                        classElement = customClassElement;
                    }
                    final Schema<?> schemaDefinition = getSchemaDefinition(openAPI, context, classElement, classElementTypeArgs, null, mediaTypes, jsonViewClass);
                    if (schemaDefinition != null) {
                        return Stream.of(schemaDefinition);
                    }
                }

                return Stream.empty();
            }).collect(Collectors.toList());
    }

    private String computeDefaultSchemaName(Element definingElement, Element type, Map<String, ClassElement> typeArgs, VisitorContext context,
                                            @Nullable ClassElement jsonViewClass) {

        String jsonViewPostfix = StringUtils.EMPTY_STRING;
        if (jsonViewClass != null) {
            String jsonViewClassName = jsonViewClass.getName();
            jsonViewClassName = jsonViewClassName.replaceAll("\\$", ".");
            jsonViewPostfix = "_" + (jsonViewClassName.contains(".") ? jsonViewClassName.substring(jsonViewClassName.lastIndexOf('.') + 1) : jsonViewClassName);
        }

        final String metaAnnName = definingElement == null ? null : definingElement.getAnnotationNameByStereotype(io.swagger.v3.oas.annotations.media.Schema.class).orElse(null);
        if (metaAnnName != null && !io.swagger.v3.oas.annotations.media.Schema.class.getName().equals(metaAnnName)) {
            return NameUtils.getSimpleName(metaAnnName) + jsonViewPostfix;
        }
        String packageName;
        String resultSchemaName;
        if (type instanceof TypedElement typedEl && !(type instanceof EnumElement)) {
            ClassElement typeType = typedEl.getType();
            packageName = typeType.getPackageName();
            if (CollectionUtils.isNotEmpty(typeType.getTypeArguments())) {
                resultSchemaName = computeNameWithGenerics(typeType, typeArgs, context);
            } else {
                resultSchemaName = computeNameWithGenerics(typeType, Collections.emptyMap(), context);
            }
        } else {
            resultSchemaName = type.getSimpleName();
            packageName = NameUtils.getPackageName(type.getName());
        }

        OpenApiApplicationVisitor.SchemaDecorator schemaDecorator = OpenApiApplicationVisitor.getSchemaDecoration(packageName, context);
        resultSchemaName = resultSchemaName.replaceAll("\\$", ".") + jsonViewPostfix;
        if (schemaDecorator != null) {
            resultSchemaName = (StringUtils.hasText(schemaDecorator.getPrefix()) ? schemaDecorator.getPrefix() : StringUtils.EMPTY_STRING)
                + resultSchemaName
                + (StringUtils.hasText(schemaDecorator.getPostfix()) ? schemaDecorator.getPostfix() : StringUtils.EMPTY_STRING);
        }
        String fullClassNameWithGenerics = packageName + '.' + resultSchemaName;

        // Check if the class exists in other packages. If so, you need to add a suffix,
        // because there are two classes in different packages, but with the same class name.
        String storedClassName = schemaNameToClassNameMap.get(resultSchemaName);
        if (storedClassName != null && !storedClassName.equals(fullClassNameWithGenerics)) {
            int index = shemaNameSuffixCounterMap.getOrDefault(resultSchemaName, 0);
            index++;
            shemaNameSuffixCounterMap.put(resultSchemaName, index);
            resultSchemaName += "_" + index;
        }
        schemaNameToClassNameMap.put(resultSchemaName, fullClassNameWithGenerics);

        return resultSchemaName;
    }

    private String computeNameWithGenerics(ClassElement classElement, Map<String, ClassElement> typeArgs, VisitorContext context) {
        StringBuilder builder = new StringBuilder(classElement.getSimpleName());
        computeNameWithGenerics(classElement, builder, new HashSet<>(), typeArgs, context);
        return builder.toString();
    }

    private void computeNameWithGenerics(ClassElement classElement, StringBuilder builder, Set<String> computed, Map<String, ClassElement> typeArgs, VisitorContext context) {
        computed.add(classElement.getName());
        final Iterator<ClassElement> i = typeArgs.values().iterator();
        if (i.hasNext()) {

            builder.append('_');
            while (i.hasNext()) {
                ClassElement ce = i.next();
                builder.append(ce.getSimpleName());
                Map<String, ClassElement> ceTypeArgs = ce.getTypeArguments();
                ClassElement customElement = OpenApiApplicationVisitor.getCustomSchema(ce.getName(), ceTypeArgs, context);
                if (customElement != null) {
                    ce = customElement;
                }
                if (!computed.contains(ce.getName())) {
                    computeNameWithGenerics(ce, builder, computed, ceTypeArgs, context);
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

    private void populateSchemaProperties(OpenAPI openAPI, VisitorContext context, Element type, Map<String, ClassElement> typeArgs, Schema<?> schema,
                                          List<MediaType> mediaTypes, JavadocDescription classJavadoc, @Nullable ClassElement jsonViewClass) {
        ClassElement classElement = null;
        if (type instanceof ClassElement classEl) {
            classElement = classEl;
        } else if (type instanceof TypedElement typedEl) {
            classElement = typedEl.getType();
        }

        if (classElement != null) {
            List<PropertyElement> beanProperties;
            try {
                beanProperties = classElement.getBeanProperties().stream()
                        .filter(p -> !"groovy.lang.MetaClass".equals(p.getType().getName()))
                        .toList();
            } catch (Exception e) {
                context.warn("Error with getting properties for class " + classElement.getName() + ": " + e + "\n" + Utils.printStackTrace(e), classElement);
                // Workaround for https://github.com/micronaut-projects/micronaut-openapi/issues/313
                beanProperties = Collections.emptyList();
            }
            processPropertyElements(openAPI, context, type, typeArgs, schema, beanProperties, mediaTypes, classJavadoc, jsonViewClass);

            String visibilityLevelProp = getConfigurationProperty(MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, context);
            VisibilityLevel visibilityLevel = VisibilityLevel.PUBLIC;
            if (StringUtils.hasText(visibilityLevelProp)) {
                try {
                    visibilityLevel = VisibilityLevel.valueOf(visibilityLevelProp.toUpperCase());
                } catch (Exception e) {
                    throw new IllegalStateException("Wrong value for visibility level property: " + getConfigurationProperty(MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, context));
                }
            }

            final List<FieldElement> publicFields = new ArrayList<>();

            for (FieldElement field : classElement.getFields()) {
                if (field.isStatic()) {
                    continue;
                }
                if (visibilityLevel == VisibilityLevel.PUBLIC
                    && !field.isPublic()) {
                    continue;
                } else if (visibilityLevel == VisibilityLevel.PROTECTED
                    && (!field.isPublic() && !field.isProtected())) {
                    continue;
                } else if (visibilityLevel == VisibilityLevel.PACKAGE
                    && (!field.isPublic() && !field.isProtected() && !field.isPackagePrivate())) {
                    continue;
                }
                boolean alreadyProcessed = false;
                for (PropertyElement prop : beanProperties) {
                    if (prop.getName().equals(field.getName())) {
                        alreadyProcessed = true;
                        break;
                    }
                }
                if (alreadyProcessed) {
                    continue;
                }
                publicFields.add(field);
            }

            processPropertyElements(openAPI, context, type, typeArgs, schema, publicFields, mediaTypes, classJavadoc, jsonViewClass);
        }
    }

    @SuppressWarnings("java:S3776")
    private void processPropertyElements(OpenAPI openAPI, VisitorContext context, Element type, Map<String, ClassElement> typeArgs, Schema<?> schema,
                                         List<? extends TypedElement> publicFields, List<MediaType> mediaTypes, JavadocDescription classJavadoc,
                                         @Nullable ClassElement jsonViewClass) {

        ClassElement classElement = null;
        if (type instanceof ClassElement classEl) {
            classElement = classEl;
        } else if (type instanceof TypedElement typedEl) {
            classElement = typedEl.getType();
        }

        boolean withJsonView = jsonViewClass != null;
        String[] classLvlJsonViewClasses = null;
        if (withJsonView && classElement != null) {
            classLvlJsonViewClasses = classElement.getAnnotationMetadata().stringValues(JsonView.class);
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

            var isGetterOverriden = false;
            JavadocDescription fieldJavadoc = null;
            if (classElement != null) {
                for (FieldElement field : classElement.getFields()) {
                    if (field.getName().equals(publicField.getName())) {
                        fieldJavadoc = Utils.getJavadocParser().parse(publicField.getDocumentation().orElse(field.getDocumentation().orElse(null)));
                        break;
                    }
                }

                // checking if the getter is overridden and has javadoc and other annotations
                if (publicField instanceof PropertyElement propertyEl) {
                    var readerMethod = propertyEl.getReadMethod().orElse(null);
                    if (readerMethod != null) {
                        var methods = classElement.getEnclosedElements(ElementQuery.ALL_METHODS.includeOverriddenMethods());
                        for (var method : methods) {
                            if (readerMethod.overrides(method)) {
                                isGetterOverriden = CollectionUtils.isNotEmpty(readerMethod.getAnnotationNames()) || fieldJavadoc != null;
                                break;
                            }
                        }
                    }
                }
            }

            if (publicField instanceof MemberElement memberEl && (memberEl.getDeclaringType().getType().getName().equals(type.getName()) || isGetterOverriden)) {

                ClassElement fieldType = publicField.getType();
                if (publicField.getType() instanceof GenericPlaceholderElement genericPlaceholderEl) {
                    ClassElement genericType = typeArgs.get(genericPlaceholderEl.getVariableName());
                    if (genericType != null) {
                        fieldType = genericType;
                    }
                }

                if (withJsonView && !allowedByJsonView(publicField, classLvlJsonViewClasses, jsonViewClass, context)) {
                    continue;
                }

                Schema<?> propertySchema = resolveSchema(openAPI, publicField, fieldType, context, mediaTypes, jsonViewClass, fieldJavadoc, classJavadoc);

                processSchemaProperty(
                    context,
                    publicField,
                    fieldType,
                    type,
                    schema,
                    propertySchema
                );
            }
        }
    }

    private boolean allowedByJsonView(TypedElement publicField, String[] classLvlJsonViewClasses, ClassElement jsonViewClassEl, VisitorContext context) {
        String[] fieldJsonViewClasses = publicField.getAnnotationMetadata().stringValues(JsonView.class);
        if (ArrayUtils.isEmpty(fieldJsonViewClasses)) {
            fieldJsonViewClasses = classLvlJsonViewClasses;
        }
        if (ArrayUtils.isEmpty(fieldJsonViewClasses)) {
            return isJsonViewDefaultInclusion(context);
        }

        for (String fieldJsonViewClass : fieldJsonViewClasses) {
            if (jsonViewClassEl.isAssignable(context.getClassElement(fieldJsonViewClass).get())) {
                return true;
            }
        }

        return false;
    }

    private Schema<?> getPrimitiveType(String typeName) {
        Schema<?> schema = null;
        Class<?> aClass = ClassUtils.getPrimitiveType(typeName).orElse(null);
        if (aClass == null) {
            aClass = ClassUtils.forName(typeName, getClass().getClassLoader()).orElse(null);
        }

        if (aClass != null) {
            Class<?> wrapperType = ReflectionUtils.getWrapperType(aClass);

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
        final OpenAPI openAPI = Utils.resolveOpenApi(context);
        for (AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityScheme> securityRequirementAnnotationValue : values) {

            final Map<CharSequence, Object> map = toValueMap(securityRequirementAnnotationValue.getValues(), context, null);

            securityRequirementAnnotationValue.stringValue("name")
                .ifPresent(name -> {
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
                        JsonNode node = toJson(map, context, null);
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
        removeAndWarnSecSchemeProp(map, prop, context, true);
    }

    private void removeAndWarnSecSchemeProp(Map<CharSequence, Object> map, String prop, VisitorContext context, boolean withWarn) {
        if (map.containsKey(prop) && withWarn) {
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
                Optional<T> tagOpt = toValue(values, context, modelType, null);
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
