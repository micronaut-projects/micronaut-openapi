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
import io.micronaut.inject.ast.GenericElement;
import io.micronaut.inject.ast.GenericPlaceholderElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.PropertyElementQuery;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.ast.WildcardElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.micronaut.openapi.visitor.ConfigUtils.SchemaDecorator;
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
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;

import javax.xml.datatype.XMLGregorianCalendar;
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
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.micronaut.openapi.visitor.ConfigUtils.getConfigProperty;
import static io.micronaut.openapi.visitor.ConfigUtils.getCustomSchema;
import static io.micronaut.openapi.visitor.ConfigUtils.getExpandableProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getSchemaDecoration;
import static io.micronaut.openapi.visitor.ConfigUtils.isJsonViewDefaultInclusion;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.ConvertUtils.parseJsonString;
import static io.micronaut.openapi.visitor.ConvertUtils.setDefaultValueObject;
import static io.micronaut.openapi.visitor.ConvertUtils.toTupleSubMap;
import static io.micronaut.openapi.visitor.ElementUtils.findAnnotation;
import static io.micronaut.openapi.visitor.ElementUtils.getAnnotation;
import static io.micronaut.openapi.visitor.ElementUtils.getAnnotationMetadata;
import static io.micronaut.openapi.visitor.ElementUtils.isAnnotationPresent;
import static io.micronaut.openapi.visitor.ElementUtils.isFileUpload;
import static io.micronaut.openapi.visitor.ElementUtils.isNotNullable;
import static io.micronaut.openapi.visitor.ElementUtils.isNullable;
import static io.micronaut.openapi.visitor.ElementUtils.stringValue;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.expandProperties;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.replacePlaceholders;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.resolvePlaceholders;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_SERVER_CONTEXT_PATH;
import static io.micronaut.openapi.visitor.OpenApiModelProp.DISCRIMINATOR;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ACCESS_MODE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ADDITIONAL_PROPERTIES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ALLOWABLE_VALUES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ALL_OF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ANY_OF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ARRAY_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEFAULT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEFAULT_VALUE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEPRECATED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DISCRIMINATOR_MAPPING;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DISCRIMINATOR_PROPERTY;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ENUM;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXAMPLE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXAMPLES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXPRESSION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXTENSIONS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXTERNAL_DOCS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_HIDDEN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_IMPLEMENTATION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_IN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_MAPPING;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_MEDIA_TYPE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NOT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NULLABLE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ONE_FORMAT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ONE_OF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ONE_TYPES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PROPERTY_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_READ_ONLY;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REF_DOLLAR;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REQUIRED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REQUIRED_MODE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REQUIRED_PROPERTIES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_RESPONSE_CODE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCOPES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_STYLE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_TITLE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_TYPE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_VALUE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_WRITE_ONLY;
import static io.micronaut.openapi.visitor.ProtoUtils.filterProtobufProperties;
import static io.micronaut.openapi.visitor.ProtoUtils.isProtobufGenerated;
import static io.micronaut.openapi.visitor.ProtoUtils.isProtobufMessageClass;
import static io.micronaut.openapi.visitor.ProtoUtils.normalizePropertyName;
import static io.micronaut.openapi.visitor.ProtoUtils.normalizeProtobufClassName;
import static io.micronaut.openapi.visitor.ProtoUtils.protobufTypeSchema;
import static io.micronaut.openapi.visitor.SchemaUtils.EMPTY_SCHEMA;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_ARRAY;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.getSchemaByRef;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;
import static io.micronaut.openapi.visitor.SchemaUtils.setAllowableValues;
import static io.micronaut.openapi.visitor.SchemaUtils.setSpecVersion;
import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.DOLLAR;
import static io.micronaut.openapi.visitor.StringUtil.DOT;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;
import static io.micronaut.openapi.visitor.StringUtil.UNDERSCORE;
import static io.micronaut.openapi.visitor.Utils.isOpenapi31;
import static io.micronaut.openapi.visitor.Utils.resolveOpenApi;
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
            ContextUtils.put(Utils.ATTR_VISITED_ELEMENTS, ContextUtils.getVisitedElements(context) + 1, context);
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
        return Utils.getJsonMapper().valueToTree(newValues);
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
            warn("Error converting  [" + node + "]: to " + type + ":\n" + Utils.printStackTrace(e), context);
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
        var result = new ArrayList<SecurityRequirement>(annotations.size());
        for (var ann : annotations) {
            result.add(ConvertUtils.mapToSecurityRequirement(ann));
        }
        return result;
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

        var resultPathItemsMap = new HashMap<String, List<PathItem>>();

        for (UriMatchTemplate matchTemplate : matchTemplates) {

            var result = new StringBuilder();

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

            String resultPath = replacePlaceholders(result.toString(), context);

            if (!resultPath.startsWith(StringUtil.SLASH) && !resultPath.startsWith(DOLLAR)) {
                resultPath = StringUtil.SLASH + resultPath;
            }
            String contextPath = ConfigUtils.getConfigProperty(MICRONAUT_SERVER_CONTEXT_PATH, context);
            if (StringUtils.isNotEmpty(contextPath)) {
                if (!contextPath.startsWith(StringUtil.SLASH) && !contextPath.startsWith(DOLLAR)) {
                    contextPath = StringUtil.SLASH + contextPath;
                }
                if (contextPath.endsWith(StringUtil.SLASH)) {
                    contextPath = contextPath.substring(0, contextPath.length() - 1);
                }
                resultPath = contextPath + resultPath;
            }

            var finalPaths = new HashMap<Integer, String>();
            finalPaths.put(-1, resultPath);
            if (CollectionUtils.isNotEmpty(matchTemplate.getVariables())) {
                var optionalVars = new ArrayList<String>();
                // need check not required path variables
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
                            finalPaths.put(i, resultPath + SLASH + OPEN_BRACE + var + CLOSE_BRACE);
                            i++;
                            continue;
                        }
                        for (Map.Entry<Integer, String> entry : finalPaths.entrySet()) {
                            if (entry.getKey() + 1 < i) {
                                continue;
                            }
                            finalPaths.put(i, entry.getValue() + SLASH + OPEN_BRACE + var + CLOSE_BRACE);
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
        var newValues = new HashMap<CharSequence, Object>(values.size());
        for (Map.Entry<CharSequence, Object> entry : values.entrySet()) {
            CharSequence key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AnnotationValue<?> av) {
                if (av.getAnnotationName().equals(io.swagger.v3.oas.annotations.media.ArraySchema.class.getName())) {
                    final Map<CharSequence, Object> valueMap = resolveArraySchemaAnnotationValues(context, av, jsonViewClass);
                    newValues.put(PROP_SCHEMA, valueMap);
                } else {
                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av, jsonViewClass);
                    newValues.put(key, valueMap);
                }
            } else if (value instanceof AnnotationClassValue<?> acv) {
                acv.getType().ifPresent(aClass -> newValues.put(key, aClass));
            } else if (value != null) {
                if (value.getClass().isArray()) {
                    var a = (Object[]) value;
                    if (ArrayUtils.isNotEmpty(a)) {
                        Object first = a[0];

                        // are class values
                        if (first instanceof AnnotationClassValue) {
                            var classes = new ArrayList<Class<?>>(a.length);
                            for (Object o : a) {
                                var acv = (AnnotationClassValue<?>) o;
                                acv.getType().ifPresent(classes::add);
                            }
                            newValues.put(key, classes);
                        } else if (first instanceof AnnotationValue<?> annValue) {
                            String annotationName = annValue.getAnnotationName();
                            if (io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName().equals(annotationName)) {
                                var securityRequirements = new ArrayList<SecurityRequirement>(a.length);
                                for (Object o : a) {
                                    securityRequirements.add(ConvertUtils.mapToSecurityRequirement((AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>) o));
                                }
                                newValues.put(key, securityRequirements);
                            } else if (Extension.class.getName().equals(annotationName)) {
                                var extensions = new HashMap<String, Object>();
                                for (Object o : a) {
                                    processExtensions(extensions, (AnnotationValue<Extension>) o);
                                }
                                newValues.put(PROP_EXTENSIONS, extensions);
                            } else if (Encoding.class.getName().equals(annotationName)) {
                                Map<String, Object> encodings = annotationValueArrayToSubmap(a, PROP_NAME, context, null);
                                newValues.put(key, encodings);
                            } else if (Content.class.getName().equals(annotationName)) {
                                Map<String, Object> mediaTypes = annotationValueArrayToSubmap(a, PROP_MEDIA_TYPE, context, jsonViewClass);
                                newValues.put(key, mediaTypes);
                            } else if (Link.class.getName().equals(annotationName) || Header.class.getName().equals(annotationName)) {
                                Map<String, Object> linksOrHeaders = annotationValueArrayToSubmap(a, PROP_NAME, context, jsonViewClass);
                                var newLinksOrHeaders = new HashMap<String, Object>(linksOrHeaders.size());
                                for (var linkOrHeaderEntry : linksOrHeaders.entrySet()) {
                                    var linkOrHeaderMap = (Map<String, Object>) linkOrHeaderEntry.getValue();
                                    if (linkOrHeaderMap.containsKey(PROP_HIDDEN) && (Boolean) linkOrHeaderMap.get(PROP_HIDDEN)) {
                                        continue;
                                    }
                                    if (linkOrHeaderMap.containsKey(PROP_REF)) {
                                        linkOrHeaderMap.put(PROP_REF_DOLLAR, linkOrHeaderMap.remove(PROP_REF));
                                    }
                                    if (linkOrHeaderMap.containsKey(PROP_SCHEMA)) {
                                        var schemaMap = (Map<String, Object>) linkOrHeaderMap.get(PROP_SCHEMA);
                                        if (schemaMap.containsKey(PROP_REF)) {
                                            Object ref = schemaMap.get(PROP_REF);
                                            schemaMap.clear();
                                            schemaMap.put(PROP_REF_DOLLAR, ref);
                                        }
                                        if (schemaMap.containsKey(PROP_DEFAULT_VALUE)) {
                                            schemaMap.put(PROP_DEFAULT, schemaMap.remove(PROP_DEFAULT_VALUE));
                                        }
                                        if (schemaMap.containsKey(PROP_ALLOWABLE_VALUES)) {
                                            // The key in the generated openapi needs to be "enum"
                                            schemaMap.put(PROP_ENUM, schemaMap.remove(PROP_ALLOWABLE_VALUES));
                                        }
                                    }
                                    if (linkOrHeaderMap.containsKey(PROP_EXAMPLE)) {
                                        var headerExample = linkOrHeaderMap.get(PROP_EXAMPLE);
                                        if (headerExample != null) {
                                            try {
                                                var headerSchema = (Map<String, Object>) linkOrHeaderMap.get(PROP_SCHEMA);
                                                String type = null;
                                                String format = null;
                                                if (headerSchema != null) {
                                                    type = (String) headerSchema.get(PROP_TYPE);
                                                    format = (String) headerSchema.get(PROP_ONE_FORMAT);
                                                    if (type == null) {
                                                        type = SchemaUtils.getType(type, (Collection<String>) headerSchema.get(PROP_ONE_TYPES));
                                                    }
                                                }
                                                var headerExampleStr = OpenApiUtils.getConvertJsonMapper().writeValueAsString(headerExample);
                                                // need to set placeholders to set correct values and types to example field
                                                headerExampleStr = replacePlaceholders(headerExampleStr, context);
                                                linkOrHeaderMap.put(PROP_EXAMPLE, ConvertUtils.parseByTypeAndFormat(headerExampleStr, type, format, context, false));
                                            } catch (JsonProcessingException e) {
                                                // do nothing
                                            }
                                        }
                                    }
                                    newLinksOrHeaders.put(linkOrHeaderEntry.getKey(), linkOrHeaderEntry.getValue());
                                }
                                newValues.put(key, newLinksOrHeaders);
                            } else if (LinkParameter.class.getName().equals(annotationName)) {
                                Map<String, String> params = toTupleSubMap(a, PROP_NAME, PROP_EXPRESSION);
                                newValues.put(key, params);
                            } else if (OAuthScope.class.getName().equals(annotationName)) {
                                Map<String, String> params = toTupleSubMap(a, PROP_NAME, PROP_DESCRIPTION);
                                newValues.put(key, params);
                            } else if (ApiResponse.class.getName().equals(annotationName)) {
                                var responses = new LinkedHashMap<String, Map<CharSequence, Object>>();
                                for (Object o : a) {
                                    var sv = (AnnotationValue<ApiResponse>) o;
                                    String name = sv.stringValue(PROP_RESPONSE_CODE).orElse(PROP_DEFAULT);
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, jsonViewClass);
                                    if (map.containsKey(PROP_REF)) {
                                        Object ref = map.get(PROP_REF);
                                        map.clear();
                                        map.put(PROP_REF_DOLLAR, ref);
                                    }

                                    try {
                                        if (!map.containsKey(PROP_DESCRIPTION)) {
                                            map.put(PROP_DESCRIPTION, name.equals(PROP_DEFAULT) ? "OK response" : HttpStatus.valueOf(Integer.parseInt(name)).getReason());
                                        }
                                    } catch (Exception e) {
                                        map.put(PROP_DESCRIPTION, "Response " + name);
                                    }

                                    responses.put(name, map);
                                }
                                newValues.put(key, responses);
                            } else if (ExampleObject.class.getName().equals(annotationName)) {
                                var examples = new LinkedHashMap<String, Map<CharSequence, Object>>();
                                for (Object o : a) {
                                    var sv = (AnnotationValue<ExampleObject>) o;
                                    String name = sv.stringValue(PROP_NAME).orElse(PROP_EXAMPLE);
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, null);
                                    if (map.containsKey(PROP_REF)) {
                                        Object ref = map.get(PROP_REF);
                                        map.clear();
                                        map.put(PROP_REF_DOLLAR, ref);
                                    }
                                    examples.put(name, map);
                                }
                                newValues.put(key, examples);
                            } else if (Server.class.getName().equals(annotationName)) {
                                var servers = new ArrayList<Map<CharSequence, Object>>();
                                for (Object o : a) {
                                    var sv = (AnnotationValue<ServerVariable>) o;
                                    var variables = new LinkedHashMap<>(toValueMap(sv.getValues(), context, null));
                                    servers.add(variables);
                                }
                                newValues.put(key, servers);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                var variables = new LinkedHashMap<String, Map<CharSequence, Object>>();
                                for (Object o : a) {
                                    var sv = (AnnotationValue<ServerVariable>) o;
                                    Optional<String> n = sv.stringValue(PROP_NAME);
                                    n.ifPresent(name -> {
                                        Map<CharSequence, Object> map = toValueMap(sv.getValues(), context, null);
                                        Object dv = map.get(PROP_DEFAULT_VALUE);
                                        if (dv != null) {
                                            map.put(PROP_DEFAULT, dv);
                                        }
                                        if (map.containsKey(PROP_ALLOWABLE_VALUES)) {
                                            // The key in the generated openapi needs to be "enum"
                                            map.put(PROP_ENUM, map.remove(PROP_ALLOWABLE_VALUES));
                                        }
                                        variables.put(name, map);
                                    });
                                }
                                newValues.put(key, variables);
                            } else if (DiscriminatorMapping.class.getName().equals(annotationName)) {
                                var extensions = new HashMap<String, Object>();
                                var mappings = new HashMap<String, String>();
                                for (Object o : a) {
                                    var dv = (AnnotationValue<DiscriminatorMapping>) o;
                                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, dv, null);
                                    mappings.put(valueMap.get(PROP_VALUE).toString(), valueMap.get(PROP_REF_DOLLAR).toString());
                                    var extValue = (Map<String, Object>) valueMap.get(PROP_EXTENSIONS);
                                    if (extValue != null) {
                                        extensions.putAll(extValue);
                                    }
                                }
                                final Map<String, Object> discriminatorMap = getDiscriminatorMap(newValues);
                                discriminatorMap.put(PROP_MAPPING, mappings);
                                if (CollectionUtils.isNotEmpty(extensions)) {
                                    discriminatorMap.put(PROP_EXTENSIONS, extensions);
                                }
                                newValues.put(DISCRIMINATOR, discriminatorMap);
                            } else {
                                if (a.length == 1) {
                                    var av = (AnnotationValue<?>) a[0];
                                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, av, jsonViewClass);
                                    newValues.put(key, toValueMap(valueMap, context, jsonViewClass));
                                } else {

                                    var list = new ArrayList<>();
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
                        } else if (key.equals(PROP_ONE_TYPES) && isOpenapi31()) {
                            newValues.put(PROP_TYPE, value);
                        } else {
                            newValues.put(key, value);
                        }
                    } else {
                        newValues.put(key, a);
                    }
                } else if (key.equals(PROP_ADDITIONAL_PROPERTIES)) {
                    if (AdditionalPropertiesValue.TRUE.toString().equals(value.toString())) {
                        newValues.put(PROP_ADDITIONAL_PROPERTIES, true);
                        // TODO
//                    } else if (AdditionalPropertiesValue.USE_ADDITIONAL_PROPERTIES_ANNOTATION.toString().equals(value.toString())) {
                    }
                } else if (key.equals(PROP_ONE_TYPES) && isOpenapi31()) {
                    newValues.put(PROP_TYPE, value);
                } else if (key.equals(PROP_DISCRIMINATOR_PROPERTY)) {
                    final Map<String, Object> discriminatorMap = getDiscriminatorMap(newValues);
                    discriminatorMap.put(PROP_PROPERTY_NAME, parseJsonString(value).orElse(value));
                    newValues.put(DISCRIMINATOR, discriminatorMap);
                } else if (key.equals(PROP_STYLE)) {
                    io.swagger.v3.oas.models.parameters.Parameter.StyleEnum paramStyle = null;
                    try {
                        paramStyle = io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.valueOf((String) value);
                    } catch (Exception e) {
                        // ignore
                    }
                    if (paramStyle == null) {
                        for (var styleValue : io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.values()) {
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
                            for (var styleValue : io.swagger.v3.oas.models.media.Encoding.StyleEnum.values()) {
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
                } else if (key.equals(PROP_REF)) {
                    newValues.put(PROP_REF_DOLLAR, value);
                } else if (key.equals(PROP_ACCESS_MODE)) {
                    if (AccessMode.READ_ONLY.toString().equals(value)) {
                        newValues.put(PROP_READ_ONLY, Boolean.TRUE);
                        newValues.remove(PROP_WRITE_ONLY);
                    } else if (AccessMode.WRITE_ONLY.toString().equals(value)) {
                        newValues.remove(PROP_READ_ONLY);
                        newValues.put(PROP_WRITE_ONLY, Boolean.TRUE);
                    } else if (AccessMode.READ_WRITE.toString().equals(value)) {
                        newValues.remove(PROP_READ_ONLY);
                        newValues.remove(PROP_WRITE_ONLY);
                    }
                } else {
                    newValues.put(key, parseJsonString(value).orElse(value));
                }
            }
        }
        return newValues;
    }

    private Map<String, Object> getDiscriminatorMap(Map<CharSequence, Object> newValues) {
        return newValues.containsKey(DISCRIMINATOR) ? (Map<String, Object>) newValues.get(DISCRIMINATOR) : new HashMap<>();
    }

    private <T extends Schema<?>> void processAnnotationValue(VisitorContext context, AnnotationValue<?> annotationValue,
                                                              Map<CharSequence, Object> arraySchemaMap, List<String> filters,
                                                              Class<T> type, @Nullable ClassElement jsonViewClass) {
        Map<CharSequence, Object> values = annotationValue.getValues().entrySet().stream()
            .filter(entry -> filters == null || !filters.contains((String) entry.getKey()))
            .collect(toMap(e -> e.getKey().equals(PROP_REQUIRED_PROPERTIES) ? PROP_REQUIRED : e.getKey(), Map.Entry::getValue));
        toValue(values, context, type, jsonViewClass)
            .ifPresent(s -> schemaToValueMap(arraySchemaMap, s));
    }

    private Map<CharSequence, Object> resolveArraySchemaAnnotationValues(VisitorContext context, AnnotationValue<?> av, @Nullable ClassElement jsonViewClass) {
        var arraySchemaMap = new HashMap<CharSequence, Object>(10);
        // properties
        av.get(PROP_ARRAY_SCHEMA, AnnotationValue.class).ifPresent(annotationValue ->
            processAnnotationValue(context, (AnnotationValue<?>) annotationValue, arraySchemaMap, List.of(PROP_REF, PROP_IMPLEMENTATION), Schema.class, null)
        );
        // items
        av.get(PROP_SCHEMA, AnnotationValue.class).ifPresent(annotationValue -> {
            Optional<String> impl = annotationValue.stringValue(PROP_IMPLEMENTATION);
            Optional<String> type = annotationValue.stringValue(PROP_TYPE);
            Optional<String> format = annotationValue.stringValue(PROP_ONE_FORMAT);
            ClassElement classElement = null;
            PrimitiveType primitiveType = null;
            if (impl.isPresent()) {
                classElement = ContextUtils.getClassElement(impl.get(), context);
            } else if (type.isPresent()) {
                // if format is "binary", we want PrimitiveType.BINARY
                primitiveType = PrimitiveType.fromName(format.isPresent() && format.get().equals("binary") ? format.get() : type.get());
                if (primitiveType == null) {
                    classElement = ContextUtils.getClassElement(type.get(), context);
                } else {
                    classElement = ContextUtils.getClassElement(primitiveType.getKeyClass().getName(), context);
                }
            }
            if (classElement != null) {
                if (primitiveType == null) {
                    final ArraySchema schema = SchemaUtils.arraySchema(resolveSchema(null, classElement, context, Collections.emptyList(), jsonViewClass));
                    schemaToValueMap(arraySchemaMap, schema);
                } else {
                    // For primitive type, just copy description field is present.
                    final Schema<?> items = setSpecVersion(primitiveType.createProperty());
                    items.setDescription((String) annotationValue.stringValue(PROP_DESCRIPTION).orElse(null));
                    final ArraySchema schema = SchemaUtils.arraySchema(items);
                    schemaToValueMap(arraySchemaMap, schema);
                }
            } else {
                arraySchemaMap.putAll(resolveAnnotationValues(context, annotationValue, jsonViewClass));
            }
        });
        // other properties (minItems,...)
        processAnnotationValue(context, av, arraySchemaMap, List.of(PROP_SCHEMA, PROP_ARRAY_SCHEMA), ArraySchema.class, null);
        return arraySchemaMap;
    }

    private Map<CharSequence, Object> resolveAnnotationValues(VisitorContext context, AnnotationValue<?> av, @Nullable ClassElement jsonViewClass) {
        final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context, jsonViewClass);
        bindSchemaIfNecessary(context, av, valueMap, jsonViewClass);
        final String annotationName = av.getAnnotationName();
        if (Parameter.class.getName().equals(annotationName)) {
            Utils.normalizeEnumValues(valueMap, CollectionUtils.mapOf(
                PROP_IN, ParameterIn.class,
                    PROP_STYLE, ParameterStyle.class
            ));
        }
        return valueMap;
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
     * @param openApi The OpenAPI object
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
    protected Schema<?> resolveSchema(OpenAPI openApi, @Nullable Element definingElement, ClassElement type, VisitorContext context,
                                      List<MediaType> mediaTypes, @Nullable ClassElement jsonViewClass,
                                      JavadocDescription fieldJavadoc, JavadocDescription classJavadoc) {

        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotationValue = null;
        if (definingElement != null) {
            schemaAnnotationValue = getAnnotation(definingElement, io.swagger.v3.oas.annotations.media.Schema.class);
        }
        if (type != null && schemaAnnotationValue == null) {
            schemaAnnotationValue = getAnnotation(type, io.swagger.v3.oas.annotations.media.Schema.class);
        }
        boolean isSubstitutedType = false;
        if (schemaAnnotationValue != null) {
            String impl = schemaAnnotationValue.stringValue(PROP_IMPLEMENTATION).orElse(null);
            if (StringUtils.isNotEmpty(impl)) {
                var typeEl = ContextUtils.getClassElement(impl, context);
                if (typeEl != null) {
                    type = typeEl;
                }
                isSubstitutedType = true;
            } else {
                String typeFromAnn = schemaAnnotationValue.stringValue(PROP_TYPE).orElse(null);
                List<String> schemaTypes;
                if (isOpenapi31() && StringUtils.isEmpty(typeFromAnn)) {
                    schemaTypes = Arrays.asList(schemaAnnotationValue.stringValues(PROP_ONE_TYPES));
                } else {
                    schemaTypes = Collections.singletonList(typeFromAnn);
                }
                for (var schemaType : schemaTypes) {
                    if (StringUtils.isNotEmpty(schemaType) && !(type instanceof EnumElement)) {
                        PrimitiveType primitiveType = PrimitiveType.fromName(schemaType);
                        if (primitiveType != null && primitiveType != PrimitiveType.OBJECT) {
                            var typeEl = ContextUtils.getClassElement(primitiveType.getKeyClass().getName(), context);
                            if (typeEl != null) {
                                type = typeEl;
                            }
                            isSubstitutedType = true;
                            break;
                        }
                    }
                }
            }
        }

        if (type != null && isProtobufMessageClass(type)) {
            type = type.getInterfaces().iterator().next();
        }

        Boolean isArray = null;
        Boolean isIterable = null;

        ClassElement componentType = type != null ? type.getFirstTypeArgument().orElse(null) : null;
        if (type instanceof WildcardElement wildcardEl) {
            type = CollectionUtils.isNotEmpty(wildcardEl.getUpperBounds()) ? wildcardEl.getUpperBounds().get(0) : null;
        } else if (type instanceof GenericPlaceholderElement placeholderEl) {
            isArray = type.isArray();
            isIterable = type.isIterable();
            if (!isArray) {
                type = placeholderEl.getResolved().orElse(CollectionUtils.isNotEmpty(placeholderEl.getBounds()) ? placeholderEl.getBounds().get(0) : null);
            }
        } else if (type instanceof GenericElement genericEl) {
            isArray = type.isArray();
            isIterable = type.isIterable();
            type = genericEl.getResolved().orElse(null);
        }
        Map<String, ClassElement> typeArgs = type != null ? type.getTypeArguments() : null;

        Schema<?> schema = null;

        if (type instanceof EnumElement enumEl) {
            schema = getSchemaDefinition(openApi, context, enumEl, typeArgs, definingElement, mediaTypes, jsonViewClass);
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

                String typeName = type.getName();
                ClassElement customTypeSchema = getCustomSchema(typeName, typeArgs, context);
                if (customTypeSchema != null) {
                    Map<String, ClassElement> customTypeArgs = customTypeSchema.getTypeArguments();
                    if (customTypeArgs.isEmpty()) {
                        type = customTypeSchema;
                    } else {
                        var inheritedTypeArgs = new HashMap<>(customTypeArgs);
                        for (String generic : customTypeArgs.keySet()) {
                            ClassElement element = typeArgs.get(generic);
                            if (element != null) {
                                inheritedTypeArgs.put(generic, element);
                            }
                        }
                        type = customTypeSchema.withTypeArguments(inheritedTypeArgs);
                    }
                }

                if (isArray == null) {
                    isArray = type.isArray();
                }
                if (isIterable == null) {
                    isIterable = type.isIterable();
                }

                // File upload case
                if (isFileUpload(type)) {
                    isPublisher = isPublisher && !"io.micronaut.http.multipart.PartData".equals(typeName);
                    // For file upload, we use PrimitiveType.BINARY
                    typeName = PrimitiveType.BINARY.name();
                }
                PrimitiveType primitiveType = PrimitiveType.fromName(typeName);
                schema = protobufTypeSchema(type);
                if (schema != null) {
                    return schema;
                }
                if (!isArray && ClassUtils.isJavaLangType(typeName)) {
                    schema = getPrimitiveType(type, typeName);
                } else if (!isArray && primitiveType != null) {
                    schema = setSpecVersion(primitiveType.createProperty());
                } else if (type.isAssignable(Map.class)) {
                    schema = processMapSchema(type, typeArgs, mediaTypes, openApi, jsonViewClass, classJavadoc, context);
                } else if (isIterable) {
                    if (isArray) {
                        schema = resolveSchema(openApi, type, type.fromArray(), context, mediaTypes, jsonViewClass, null, classJavadoc);
                        if (schema != null) {
                            schema = SchemaUtils.arraySchema(schema);
                        }
                    } else {
                        if (componentType != null) {
                            schema = resolveSchema(openApi, type, componentType, context, mediaTypes, jsonViewClass, null, classJavadoc);
                        } else {
                            schema = getPrimitiveType(null, Object.class.getName());
                        }
                        List<FieldElement> fields = type.getPackageName().startsWith("java.util") ? Collections.emptyList() : type.getFields();
                        if (schema != null && fields.isEmpty()) {
                            schema = processGenericAnnotations(schema, componentType);
                            schema = SchemaUtils.arraySchema(schema);
                        } else {
                            schema = getSchemaDefinition(openApi, context, type, typeArgs, definingElement, mediaTypes, jsonViewClass);
                        }
                    }
                } else if (ElementUtils.isReturnTypeFile(type)) {
                    schema = setSpecVersion(PrimitiveType.FILE.createProperty());
                } else if (type.isAssignable(Boolean.class) || type.isAssignable(boolean.class)) {
                    schema = setSpecVersion(PrimitiveType.BOOLEAN.createProperty());
                } else if (type.isAssignable(Byte.class) || type.isAssignable(byte.class)) {
                    schema = setSpecVersion(PrimitiveType.BYTE.createProperty());
                } else if (type.isAssignable(UUID.class)) {
                    schema = setSpecVersion(PrimitiveType.UUID.createProperty());
                } else if (type.isAssignable(URL.class)) {
                    schema = setSpecVersion(PrimitiveType.URL.createProperty());
                } else if (type.isAssignable(URI.class)) {
                    schema = setSpecVersion(PrimitiveType.URI.createProperty());
                } else if (type.isAssignable(Character.class) || type.isAssignable(char.class)) {
                    schema = setSpecVersion(PrimitiveType.STRING.createProperty());
                } else if (type.isAssignable(Integer.class) || type.isAssignable(int.class)
                        || type.isAssignable(Short.class) || type.isAssignable(short.class)) {
                    schema = setSpecVersion(PrimitiveType.INT.createProperty());
                } else if (type.isAssignable(Long.class) || type.isAssignable(long.class)) {
                    schema = setSpecVersion(PrimitiveType.LONG.createProperty());
                } else if (type.isAssignable(Float.class) || type.isAssignable(float.class)) {
                    schema = setSpecVersion(PrimitiveType.FLOAT.createProperty());
                } else if (type.isAssignable(Double.class) || type.isAssignable(double.class)) {
                    schema = setSpecVersion(PrimitiveType.DOUBLE.createProperty());
                } else if (type.isAssignable(BigInteger.class)) {
                    schema = setSpecVersion(PrimitiveType.INTEGER.createProperty());
                } else if (type.isAssignable(BigDecimal.class)) {
                    schema = setSpecVersion(PrimitiveType.DECIMAL.createProperty());
                } else if (type.isAssignable(Date.class)
                    || type.isAssignable(Calendar.class)
                    || type.isAssignable(LocalDateTime.class)
                    || type.isAssignable(ZonedDateTime.class)
                    || type.isAssignable(OffsetDateTime.class)
                    || type.isAssignable(Instant.class)
                    || type.isAssignable(XMLGregorianCalendar.class)) {
                    schema = setSpecVersion(new StringSchema().format("date-time"));
                } else if (type.isAssignable(LocalDate.class)) {
                    schema = setSpecVersion(new StringSchema().format("date"));
                } else if (type.isAssignable(LocalTime.class)) {
                    schema = setSpecVersion(new StringSchema().format("partial-time"));
                } else if (type.isAssignable(Number.class)) {
                    schema = setSpecVersion(PrimitiveType.NUMBER.createProperty());
                } else if (type.getName().equals(Object.class.getName())) {
                    schema = setSpecVersion(PrimitiveType.OBJECT.createProperty());
                } else {
                    schema = getSchemaDefinition(openApi, context, type, typeArgs, definingElement, mediaTypes, jsonViewClass);
                    schema = processGenericAnnotations(schema, componentType);
                }
            }

            if (schema != null) {

                if (isSubstitutedType) {
                    processSchemaAnn(schema, context, definingElement, type, schemaAnnotationValue);
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
                if (CollectionUtils.isNotEmpty(mediaTypes)) {
                    for (MediaType mediaType : mediaTypes) {
                        if (MediaType.TEXT_EVENT_STREAM_TYPE.equals(mediaType) || MediaType.APPLICATION_JSON_STREAM_TYPE.equals(mediaType)) {
                            isStream = true;
                            break;
                        }
                    }
                }

                if (!isStream && (isPublisher || isObservable)) {
                    schema = SchemaUtils.arraySchema(schema);
                } else if (isNullable) {
                    SchemaUtils.setNullable(schema);
                }
            }
        }
        return schema;
    }

    private Schema<?> processMapSchema(ClassElement type, Map<String, ClassElement> typeArgs,
                                       List<MediaType> mediaTypes,
                                       OpenAPI openApi, ClassElement jsonViewClass,
                                       JavadocDescription classJavadoc,
                                       VisitorContext context) {
        var schema = setSpecVersion(new Schema<>());
        if (CollectionUtils.isEmpty(typeArgs)) {
            schema.setAdditionalProperties(true);
            return schema;
        }
        // Case, when map key is enumeration
        ClassElement keyType = typeArgs.get("K");
        ClassElement valueType = typeArgs.get("V");
        if (keyType.isEnum()) {
            var enumSchema = getSchemaDefinition(openApi, context, keyType, keyType.getTypeArguments(), null, mediaTypes, null);
            if (enumSchema != null && enumSchema.get$ref() != null) {
                enumSchema = getSchemaByRef(enumSchema, openApi);
                var values = enumSchema.getEnum();
                if (CollectionUtils.isNotEmpty(values)) {
                    var valueSchema = getSchemaDefinition(openApi, context, valueType, valueType.getTypeArguments(), null, mediaTypes, jsonViewClass);
                    for (var value : values) {
                        schema.addProperty(value.toString(), valueSchema);
                    }
                }
                return schema;
            }
        }
        if (valueType.getName().equals(Object.class.getName())) {
            schema.setAdditionalProperties(true);
        } else {
            schema.setAdditionalProperties(resolveSchema(openApi, type, valueType, context, mediaTypes, jsonViewClass, null, classJavadoc));
        }
        return schema;
    }

    private Schema<?> processGenericAnnotations(Schema<?> schema, ClassElement componentType) {
        if (componentType == null) {
            return schema;
        }
        var primitiveComponentType = getPrimitiveType(componentType, componentType.getName());
        if (primitiveComponentType == null) {
            var schemaFromTypeArgAnnotations = setSpecVersion(new Schema<>());
            processArgTypeAnnotations(componentType, schemaFromTypeArgAnnotations);
            if (schemaFromTypeArgAnnotations.equals(EMPTY_SCHEMA)) {
                return schema;
            }
            var composedSchema = setSpecVersion(new ComposedSchema());
            composedSchema.addAllOfItem(schema);
            composedSchema.addAllOfItem(schemaFromTypeArgAnnotations);
            return composedSchema;
        }

        return schema;
    }

    private void handleUnwrapped(VisitorContext context, Element element, ClassElement elementType, Schema<?> parentSchema, AnnotationValue<JsonUnwrapped> uw) {
        Map<String, Schema> schemas = SchemaUtils.resolveSchemas(Utils.resolveOpenApi(context));
        ClassElement customElementType = getCustomSchema(elementType.getName(), elementType.getTypeArguments(), context);
        var elType = customElementType != null ? customElementType : elementType;
        String schemaName = stringValue(element, io.swagger.v3.oas.annotations.media.Schema.class, PROP_NAME)
            .orElse(computeDefaultSchemaName(null, elType, elementType.getTypeArguments(), context, null));
        Schema<?> wrappedPropertySchema = schemas.get(schemaName);
        if (wrappedPropertySchema == null) {
            getSchemaDefinition(resolveOpenApi(context), context, elType, elType.getTypeArguments(), element, Collections.emptyList(), null);
            wrappedPropertySchema = schemas.get(schemaName);
        }
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
                    propertySchema = Utils.getJsonMapper().readValue(Utils.getJsonMapper().writeValueAsString(prop.getValue()), Schema.class);
                    propertySchema.setName(propertyName);
                }
                addProperty(parentSchema, propertyName, propertySchema, isRequired);
            } catch (IOException e) {
                warn("Exception cloning property " + e.getMessage(), context);
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
    protected void processSchemaProperty(VisitorContext context, TypedElement element, ClassElement elementType, @Nullable ClassElement classElement,
                                         Schema<?> parentSchema, Schema<?> propertySchema) {
        if (propertySchema == null) {
            return;
        }
        var jsonUnwrappedAnn = getAnnotation(element, JsonUnwrapped.class);
        if (jsonUnwrappedAnn != null && jsonUnwrappedAnn.booleanValue("enabled").orElse(Boolean.TRUE)) {
            handleUnwrapped(context, element, elementType, parentSchema, jsonUnwrappedAnn);
        } else {
            // check schema required flag
            var schemaAnn = getAnnotation(element, io.swagger.v3.oas.annotations.media.Schema.class);
            Optional<Boolean> elementSchemaRequired = Optional.empty();
            boolean isAutoRequiredMode = true;
            boolean isRequiredDefaultValueSet = false;
            if (schemaAnn != null) {
                elementSchemaRequired = schemaAnn.get(PROP_REQUIRED, Argument.BOOLEAN);
                isRequiredDefaultValueSet = !schemaAnn.contains(PROP_REQUIRED);
                var requiredMode = schemaAnn.enumValue(PROP_REQUIRED_MODE, io.swagger.v3.oas.annotations.media.Schema.RequiredMode.class)
                    .orElse(null);
                if (requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED) {
                    elementSchemaRequired = Optional.of(true);
                    isAutoRequiredMode = false;
                } else if (requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED) {
                    elementSchemaRequired = Optional.of(false);
                    isAutoRequiredMode = false;
                }
            }

            // check field annotations (@NonNull, @Nullable, etc.)
            boolean isNotNullable = isNotNullable(element);
            // check as mandatory in constructor
            boolean isMandatoryInConstructor = doesParamExistsMandatoryInConstructor(element, classElement);
            boolean required = elementSchemaRequired.orElse(isNotNullable || isMandatoryInConstructor);

            if (isRequiredDefaultValueSet && isAutoRequiredMode && isNotNullable) {
                required = true;
            }

            propertySchema = bindSchemaForElement(context, element, elementType, propertySchema, null);
            String propertyName = resolvePropertyName(element, classElement, propertySchema);
            propertyName = normalizePropertyName(propertyName, classElement, elementType);
            propertySchema.setRequired(null);
            Schema<?> propertySchemaFinal = propertySchema;
            addProperty(parentSchema, propertyName, propertySchema, required);
            if (schemaAnn != null) {
                schemaAnn.stringValue(PROP_DEFAULT_VALUE)
                    .ifPresent(value -> {
                        String elType = schemaAnn.stringValue(PROP_TYPE).orElse(null);
                        String elFormat = schemaAnn.stringValue(PROP_ONE_FORMAT).orElse(null);
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

        if (isAnnotationPresent(element, io.swagger.v3.oas.annotations.media.Schema.class)) {
            Optional<String> nameFromSchema = stringValue(element, io.swagger.v3.oas.annotations.media.Schema.class, PROP_NAME);
            if (nameFromSchema.isPresent()) {
                return nameFromSchema.get();
            }
        }
        if (isAnnotationPresent(element, JsonProperty.class)) {
            return stringValue(element, JsonProperty.class, PROP_VALUE).orElse(name);
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
        var schemaAnn = getAnnotation(element, io.swagger.v3.oas.annotations.media.Schema.class);
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
            Optional<String> schemaName = schemaAnn.stringValue(PROP_NAME);
            if (schemaName.isPresent()) {
                schemaToBind.setName(schemaName.get());
            }

            var impl = schemaAnn.stringValue(PROP_IMPLEMENTATION).orElse(null);
            if (StringUtils.isNotEmpty(impl)) {
                var implEl = ContextUtils.getClassElement(impl, context);
                if (implEl != null) {
                    elementType = implEl;
                }
            }
        }
        var arraySchemaAnn = getAnnotation(element, io.swagger.v3.oas.annotations.media.ArraySchema.class);
        if (arraySchemaAnn != null) {
            schemaToBind = bindArraySchemaAnnotationValue(context, element, schemaToBind, arraySchemaAnn, jsonViewClass);
            arraySchemaAnn.stringValue(PROP_NAME).ifPresent(schemaToBind::setName);
        }

        processJakartaValidationAnnotations(element, elementType, schemaToBind);

        final ComposedSchema composedSchema;
        final Schema<?> topLevelSchema;
        if (originalSchema.get$ref() != null) {
            composedSchema = setSpecVersion(new ComposedSchema());
            topLevelSchema = composedSchema;
        } else {
            composedSchema = setSpecVersion(new ComposedSchema());
            topLevelSchema = schemaToBind;
        }

        boolean notOnlyRef = false;
        setSchemaDocumentation(element, topLevelSchema);
        if (StringUtils.isNotEmpty(topLevelSchema.getDescription())) {
            notOnlyRef = true;
        }
        if (isAnnotationPresent(element, Deprecated.class)
                && !(element instanceof PropertyElement propertyEl
                    && isProtobufGenerated(propertyEl.getOwningType())
                    && elementType.getName().equals(Map.class.getName())
        )) {
            topLevelSchema.setDeprecated(true);
            notOnlyRef = true;
        }
        final String defaultValue = stringValue(element, Bindable.class, PROP_DEFAULT_VALUE).orElse(null);
        if (defaultValue != null && schemaToBind.getDefault() == null) {
            setDefaultValueObject(schemaToBind, defaultValue, elementType, schemaToBind.getType(), schemaToBind.getFormat(), true, context);
            notOnlyRef = true;
        }
        // @Schema annotation takes priority over nullability annotations
        Boolean isSchemaNullable = element.booleanValue(io.swagger.v3.oas.annotations.media.Schema.class, PROP_NULLABLE).orElse(null);
        boolean isNullable = (isSchemaNullable == null && isNullable(element) && !isNotNullable(element)) || Boolean.TRUE.equals(isSchemaNullable);
        if (isNullable) {
            SchemaUtils.setNullable(topLevelSchema);
            notOnlyRef = true;
        }
        final String defaultJacksonValue = stringValue(element, JsonProperty.class, PROP_DEFAULT_VALUE).orElse(null);
        if (defaultJacksonValue != null && schemaToBind.getDefault() == null) {
            setDefaultValueObject(topLevelSchema, defaultJacksonValue, elementType, schemaToBind.getType(), schemaToBind.getFormat(), false, context);
            notOnlyRef = true;
        }

        boolean addSchemaToBind = !SchemaUtils.isEmptySchema(schemaToBind);

        if (addSchemaToBind) {
            if (TYPE_OBJECT.equals(originalSchema.getType()) && !(originalSchema instanceof MapSchema)) {
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
            if (TYPE_OBJECT.equals(schemaToBind.getType()) && !(originalSchema instanceof MapSchema)) {
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

    protected void processJakartaValidationAnnotations(Element element, ClassElement elementType, Schema<?> schemaToBind) {

        final boolean isIterableOrMap = elementType.isIterable() || elementType.isAssignable(Map.class);

        if (isIterableOrMap) {
            if (isAnnotationPresent(element, "javax.validation.constraints.NotEmpty$List")
                || isAnnotationPresent(element, "jakarta.validation.constraints.NotEmpty$List")) {
                schemaToBind.setMinItems(1);
            }

            findAnnotation(element, "javax.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("min")
                        .ifPresent(schemaToBind::setMinItems)));
            findAnnotation(element, "jakarta.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("min")
                        .ifPresent(schemaToBind::setMinItems)));

            findAnnotation(element, "javax.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("max")
                        .ifPresent(schemaToBind::setMaxItems)));
            findAnnotation(element, "jakarta.validation.constraints.Size$List")
                .ifPresent(listAnn -> listAnn.getValue(AnnotationValue.class)
                    .ifPresent(ann -> ann.intValue("max")
                        .ifPresent(schemaToBind::setMaxItems)));

        } else {
            if (PrimitiveType.STRING.getCommonName().equals(schemaToBind.getType())) {
                if (isAnnotationPresent(element, "javax.validation.constraints.NotEmpty$List")
                    || isAnnotationPresent(element, "jakarta.validation.constraints.NotEmpty$List")
                    || isAnnotationPresent(element, "javax.validation.constraints.NotBlank$List")
                    || isAnnotationPresent(element, "jakarta.validation.constraints.NotBlank$List")) {
                    schemaToBind.setMinLength(1);
                }

                findAnnotation(element, "javax.validation.constraints.Size$List")
                    .ifPresent(listAnn -> {
                        for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                            ann.intValue("min").ifPresent(schemaToBind::setMinLength);
                            ann.intValue("max").ifPresent(schemaToBind::setMaxLength);
                        }
                    });
                findAnnotation(element, "jakarta.validation.constraints.Size$List")
                    .ifPresent(listAnn -> {
                        for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                            ann.intValue("min").ifPresent(schemaToBind::setMinLength);
                            ann.intValue("max").ifPresent(schemaToBind::setMaxLength);
                        }
                    });
            }

            if (isAnnotationPresent(element, "javax.validation.constraints.Negative$List")
                || isAnnotationPresent(element, "jakarta.validation.constraints.Negative$List")) {
                schemaToBind.setMaximum(BigDecimal.ZERO);
                schemaToBind.exclusiveMaximum(true);
            }
            if (isAnnotationPresent(element, "javax.validation.constraints.NegativeOrZero$List")
                || isAnnotationPresent(element, "jakarta.validation.constraints.NegativeOrZero$List")) {
                schemaToBind.setMaximum(BigDecimal.ZERO);
            }
            if (isAnnotationPresent(element, "javax.validation.constraints.Positive$List")
                || isAnnotationPresent(element, "jakarta.validation.constraints.Positive$List")) {
                schemaToBind.setMinimum(BigDecimal.ZERO);
                schemaToBind.exclusiveMinimum(true);
            }
            if (isAnnotationPresent(element, "javax.validation.constraints.PositiveOrZero$List")
                || isAnnotationPresent(element, "jakarta.validation.constraints.PositiveOrZero$List")) {
                schemaToBind.setMinimum(BigDecimal.ZERO);
            }

            findAnnotation(element, "javax.validation.constraints.Min$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });
            findAnnotation(element, "jakarta.validation.constraints.Min$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });

            findAnnotation(element, "javax.validation.constraints.Max$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });
            findAnnotation(element, "jakarta.validation.constraints.Max$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });

            findAnnotation(element, "javax.validation.constraints.DecimalMin$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });
            findAnnotation(element, "jakarta.validation.constraints.DecimalMin$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMinimum);
                    }
                });

            findAnnotation(element, "javax.validation.constraints.DecimalMax$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });
            findAnnotation(element, "jakarta.validation.constraints.DecimalMax$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.getValue(BigDecimal.class)
                            .ifPresent(schemaToBind::setMaximum);
                    }
                });

            findAnnotation(element, "javax.validation.constraints.Email$List")
                .ifPresent(listAnn -> {
                    schemaToBind.setFormat(PrimitiveType.EMAIL.getCommonName());
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });
            findAnnotation(element, "jakarta.validation.constraints.Email$List")
                .ifPresent(listAnn -> {
                    schemaToBind.setFormat(PrimitiveType.EMAIL.getCommonName());
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });

            findAnnotation(element, "javax.validation.constraints.Pattern$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });
            findAnnotation(element, "jakarta.validation.constraints.Pattern$List")
                .ifPresent(listAnn -> {
                    for (AnnotationValue<?> ann : listAnn.getAnnotations(PROP_VALUE)) {
                        ann.stringValue("regexp")
                            .ifPresent(schemaToBind::setPattern);
                    }
                });

            element.getValue("io.micronaut.http.annotation.Part", String.class)
                .ifPresent(schemaToBind::setName);
        }
    }

    Schema<?> schemaFromAnnotation(VisitorContext context, Element element, ClassElement type, AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {
        if (schemaAnn == null) {
            return null;
        }

        var schemaToBind = setSpecVersion(new Schema<>());
        processSchemaAnn(schemaToBind, context, element, type, schemaAnn);

        return schemaToBind;
    }

    void processSchemaAnn(Schema schemaToBind, VisitorContext context, Element element,
                          @Nullable ClassElement classEl,
                          @NonNull AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {

        Map<CharSequence, Object> annValues = schemaAnn.getValues();
        if (annValues.containsKey(PROP_NAME)) {
            schemaToBind.setName((String) annValues.get(PROP_NAME));
        }
        if (annValues.containsKey(PROP_TITLE)) {
            schemaToBind.setTitle((String) annValues.get(PROP_TITLE));
        }
        if (annValues.containsKey(PROP_REF)) {
            schemaToBind.set$ref((String) annValues.get(PROP_REF));
        }
        var schemaMultipleOf = (Double) annValues.get("multipleOf");
        if (schemaMultipleOf != null) {
            schemaToBind.setMultipleOf(BigDecimal.valueOf(schemaMultipleOf));
        }
        var schemaMaximum = (String) annValues.get("maximum");
        if (NumberUtils.isCreatable(schemaMaximum)) {
            schemaToBind.setMaximum(new BigDecimal(schemaMaximum));
        }
        if (!isOpenapi31()) {
            var schemaExclusiveMaximum = (Boolean) annValues.get("exclusiveMaximum");
            if (schemaExclusiveMaximum != null && schemaExclusiveMaximum) {
                schemaToBind.setExclusiveMaximum(true);
            }
        }
        var schemaMinimum = (String) annValues.get("minimum");
        if (NumberUtils.isCreatable(schemaMinimum)) {
            schemaToBind.setMinimum(new BigDecimal(schemaMinimum));
        }
        if (!isOpenapi31()) {
            var schemaExclusiveMinimum = (Boolean) annValues.get("exclusiveMinimum");
            if (schemaExclusiveMinimum != null && schemaExclusiveMinimum) {
                schemaToBind.setExclusiveMinimum(true);
            }
        }
        if (annValues.containsKey("maxLength")) {
            schemaToBind.setMaxLength((Integer) annValues.get("maxLength"));
        }
        if (annValues.containsKey("minLength")) {
            schemaToBind.setMinLength((Integer) annValues.get("minLength"));
        }
        if (annValues.containsKey("pattern")) {
            schemaToBind.setPattern((String) annValues.get("pattern"));
        }
        if (annValues.containsKey("maxProperties")) {
            schemaToBind.setMaxProperties((Integer) annValues.get("maxProperties"));
        }
        if (annValues.containsKey("minProperties")) {
            schemaToBind.setMinProperties((Integer) annValues.get("minProperties"));
        }
        if (annValues.containsKey(PROP_REQUIRED_PROPERTIES)) {
            var requiredProperties = (String[]) annValues.get(PROP_REQUIRED_PROPERTIES);
            schemaToBind.setRequired(new ArrayList<>(Arrays.asList(requiredProperties)));
        }
        if (annValues.containsKey(PROP_DESCRIPTION)) {
            schemaToBind.setDescription((String) annValues.get(PROP_DESCRIPTION));
        }
        String format = null;
        if (annValues.containsKey(PROP_ONE_FORMAT)) {
            format = (String) annValues.get(PROP_ONE_FORMAT);
            schemaToBind.setFormat(format);
        }
        if (annValues.containsKey(PROP_NULLABLE)) {
            if (!(element instanceof MemberElement)) {
                SchemaUtils.setNullable(schemaToBind);
            }
        }
        var accessModeStr = (String) annValues.get(PROP_ACCESS_MODE);
        if (StringUtils.isNotEmpty(accessModeStr)) {
            var schemaAccessMode = AccessMode.valueOf(accessModeStr);
            if (schemaAccessMode != AccessMode.AUTO) {
                if (schemaAccessMode == AccessMode.READ_ONLY) {
                    schemaToBind.setReadOnly(true);
                    schemaToBind.setWriteOnly(null);
                } else if (schemaAccessMode == AccessMode.WRITE_ONLY) {
                    schemaToBind.setReadOnly(null);
                    schemaToBind.setWriteOnly(true);
                } else if (schemaAccessMode == AccessMode.READ_WRITE) {
                    schemaToBind.setReadOnly(null);
                    schemaToBind.setWriteOnly(null);
                }
            }
        }
        var schemaExtDocs = (AnnotationValue<io.swagger.v3.oas.annotations.ExternalDocumentation>) annValues.get(PROP_EXTERNAL_DOCS);
        ExternalDocumentation externalDocs = null;
        if (schemaExtDocs != null) {
            externalDocs = toValue(schemaExtDocs.getValues(), context, ExternalDocumentation.class, null).orElse(null);
        }
        if (externalDocs != null) {
            schemaToBind.setExternalDocs(externalDocs);
        }
        var schemaDeprecated = (Boolean) annValues.get(PROP_DEPRECATED);
        if (schemaDeprecated != null && schemaDeprecated) {
            schemaToBind.setDeprecated(true);
        }
        String type = null;
        if (annValues.containsKey(PROP_TYPE)) {
            type = (String) annValues.get(PROP_TYPE);
            schemaToBind.setType(type);
        }

        Pair<String, String> typeAndFormat = null;
        if (element instanceof ClassElement classElement) {
            if (classElement.isIterable()) {
                typeAndFormat = Pair.of(TYPE_ARRAY, null);
            } else if (element instanceof EnumElement enumEl) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, null);
            } else {
                typeAndFormat = ConvertUtils.getTypeAndFormatByClass(classElement.getName(), classElement.isArray());
            }
        }
        var elType = type != null ? type : typeAndFormat != null ? typeAndFormat.getFirst() : null;
        var elFormat = format != null ? format : typeAndFormat != null ? typeAndFormat.getSecond() : null;

        var allowableValues = schemaAnn.stringValues(PROP_ALLOWABLE_VALUES);
        setAllowableValues(schemaToBind, allowableValues, element, elType, elFormat, context);

        if (isOpenapi31()) {
            var schemaExamples = (String[]) annValues.get(PROP_EXAMPLES);
            if (ArrayUtils.isNotEmpty(schemaExamples)) {
                for (var schemaExample : schemaExamples) {
                    // need to set placeholders to set correct values and types to example field
                    schemaExample = replacePlaceholders(schemaExample, context);
                    schemaToBind.addExample(ConvertUtils.parseByTypeAndFormat(schemaExample, elType, elFormat, context, false));
                }
            }
        } else {
            var schemaExample = (String) annValues.get(PROP_EXAMPLE);
            if (StringUtils.isNotEmpty(schemaExample)) {
                // need to set placeholders to set correct values and types to example field
                schemaExample = replacePlaceholders(schemaExample, context);
                schemaToBind.setExample(ConvertUtils.parseByTypeAndFormat(schemaExample, elType, elFormat, context, false));
            }
        }


        var schemaDefaultValue = (String) annValues.get(PROP_DEFAULT_VALUE);
        if (schemaDefaultValue != null) {
            setDefaultValueObject(schemaToBind, schemaDefaultValue, classEl, schemaToBind.getType(), schemaToBind.getFormat(), false, context);
        }
        if (annValues.containsKey(PROP_DISCRIMINATOR_PROPERTY)) {
            var discriminator = new Discriminator();
            schemaToBind.setDiscriminator(discriminator);
            discriminator.setPropertyName(annValues.get(PROP_DISCRIMINATOR_PROPERTY).toString());

            if (annValues.containsKey(PROP_DISCRIMINATOR_MAPPING)) {
                var discriminatorMapping = (AnnotationValue<DiscriminatorMapping>[]) annValues.get(PROP_DISCRIMINATOR_MAPPING);
                var extensions = new HashMap<String, Object>();
                var mappings = new HashMap<String, String>();
                for (var discriminatorMappingAnn : discriminatorMapping) {
                    final Map<CharSequence, Object> valueMap = resolveAnnotationValues(context, discriminatorMappingAnn, null);
                    mappings.put(valueMap.get(PROP_VALUE).toString(), valueMap.get(PROP_REF_DOLLAR).toString());
                    var extValue = (Map<String, Object>) valueMap.get(PROP_EXTENSIONS);
                    if (extValue != null) {
                        extensions.putAll(extValue);
                    }
                }
                discriminator.setMapping(mappings);
                if (CollectionUtils.isNotEmpty(extensions)) {
                    extensions.forEach(discriminator::addExtension);
                }
            }
        }

        if (annValues.containsKey(PROP_EXTENSIONS)) {
            var extensionAnns = (AnnotationValue<Extension>[]) annValues.get(PROP_EXTENSIONS);
            var extensions = new HashMap<String, Object>();
            for (var extensionAnn : extensionAnns) {
                processExtensions(extensions, extensionAnn);
            }
            if (!extensions.isEmpty()) {
                extensions.forEach(schemaToBind::addExtension);
            }
        }

        var addProps = (String) annValues.get(PROP_ADDITIONAL_PROPERTIES);
        if (StringUtils.isNotEmpty(addProps)) {
            var schemaAdditionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.valueOf(addProps);
            if (schemaAdditionalProperties == io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE) {
                schemaToBind.additionalProperties(true);
            } else if (schemaAdditionalProperties == io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE) {
                schemaToBind.additionalProperties(null);
            }
        }

        if (isOpenapi31()) {

            if (annValues.containsKey("contains")) {
                if (annValues.containsKey("minContains")) {
                    schemaToBind.setMinContains((Integer) annValues.get("minContains"));
                }
                if (annValues.containsKey("maxContains")) {
                    schemaToBind.setMaxContains((Integer) annValues.get("maxContains"));
                }
            }

            if (annValues.containsKey(PROP_ONE_TYPES)) {
                schemaToBind.setTypes(new HashSet<>((Collection<String>) annValues.get(PROP_ONE_TYPES)));
            }
            if (annValues.containsKey("$id")) {
                schemaToBind.set$id((String) annValues.get("$id"));
            }
            if (annValues.containsKey("$schema")) {
                schemaToBind.set$schema((String) annValues.get("$schema"));
            }
            if (annValues.containsKey("$anchor")) {
                schemaToBind.set$anchor((String) annValues.get("$anchor"));
            }
            if (annValues.containsKey("$vocabulary")) {
                schemaToBind.set$vocabulary((String) annValues.get("$vocabulary"));
            }
            if (annValues.containsKey("$dynamicAnchor")) {
                schemaToBind.set$dynamicAnchor((String) annValues.get("$dynamicAnchor"));
            }
            if (annValues.containsKey("contentEncoding")) {
                schemaToBind.setContentEncoding((String) annValues.get("contentEncoding"));
            }
            if (annValues.containsKey("contentMediaType")) {
                schemaToBind.setContentMediaType((String) annValues.get("contentMediaType"));
            }
            if (annValues.containsKey("$comment")) {
                schemaToBind.set$comment((String) annValues.get("$comment"));
            }
            if (annValues.containsKey("_const")) {
                schemaToBind.setConst(annValues.get("_const"));
            }
            String exclusiveMinimum = (String) annValues.get("exclusiveMinimumValue");
            if (NumberUtils.isCreatable(exclusiveMinimum)) {
                schemaToBind.setExclusiveMinimumValue(new BigDecimal(exclusiveMinimum));
            }
            String exclusiveMaximum = (String) annValues.get("exclusiveMaximumValue");
            if (NumberUtils.isCreatable(exclusiveMaximum)) {
                schemaToBind.setExclusiveMaximumValue(new BigDecimal(exclusiveMaximum));
            }
            parseAndSetClassValue("contains", Schema::contains, annValues, schemaToBind, context);
            parseAndSetClassValue("contentSchema", Schema::contentSchema, annValues, schemaToBind, context);
            parseAndSetClassValue("propertyNames", Schema::propertyNames, annValues, schemaToBind, context);
            parseAndSetClassValue("unevaluatedProperties", Schema::unevaluatedProperties, annValues, schemaToBind, context);
            parseAndSetClassValue("additionalItems", Schema::additionalItems, annValues, schemaToBind, context);
            parseAndSetClassValue("unevaluatedItems", Schema::unevaluatedItems, annValues, schemaToBind, context);
            parseAndSetClassValue("_if", Schema::_if, annValues, schemaToBind, context);
            parseAndSetClassValue("_else", Schema::_else, annValues, schemaToBind, context);
            parseAndSetClassValue("then", Schema::then, annValues, schemaToBind, context);
        }

        processClassValues(schemaToBind, annValues, Collections.emptyList(), context, null);
    }

    private void processClassValues(Schema<?> schemaToBind, Map<CharSequence, Object> annValues, List<MediaType> mediaTypes, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        var openApi = Utils.resolveOpenApi(context);
        parseAndSetClassValue(PROP_NOT, Schema::not, annValues, schemaToBind, context);
        processSchemasArray(schemaToBind, openApi, PROP_ALL_OF, annValues, mediaTypes, jsonViewClass, Schema::getAllOf, Schema::addAllOfItem, context);
        processSchemasArray(schemaToBind, openApi, PROP_ANY_OF, annValues, mediaTypes, jsonViewClass, Schema::getAnyOf, Schema::addAnyOfItem, context);
        processSchemasArray(schemaToBind, openApi, PROP_ONE_OF, annValues, mediaTypes, jsonViewClass, Schema::getOneOf, Schema::addOneOfItem, context);
    }

    private void parseAndSetClassValue(String propName,
                                       BiConsumer<Schema, Schema> setter,
                                       Map<CharSequence, Object> annValues, Schema schema,
                                       VisitorContext context) {
        if (annValues.containsKey(propName)) {
            var classEl = ContextUtils.getClassElement(annValues.get(propName).toString(), context);
            var resolvedSchema = resolveSchema(null, classEl, context, Collections.emptyList(), null);
            setter.accept(schema, resolvedSchema);
        }
    }

    private void processSchemasArray(Schema<?> schemaToBind, OpenAPI openApi,
                                     String propName,
                                     Map<CharSequence, Object> annValues,
                                     List<MediaType> mediaTypes,
                                     @Nullable ClassElement jsonViewClass,
                                     Function<Schema, List<Schema>> getter,
                                     BiConsumer<Schema, Schema> methodAdd,
                                     VisitorContext context) {

        var oneOf = (AnnotationClassValue<?>[]) annValues.get(propName);
        if (ArrayUtils.isEmpty(oneOf)) {
            return;
        }

        for (var classAnn : oneOf) {
            ClassElement classElement = ContextUtils.getClassElement(classAnn.getName(), context);
            if (classElement == null) {
                continue;
            }
            Map<String, ClassElement> classElementTypeArgs = classElement.getTypeArguments();
            ClassElement customClassElement = getCustomSchema(classElement.getName(), classElementTypeArgs, context);
            if (customClassElement != null) {
                classElement = customClassElement;
            }
            final Schema<?> schema = getSchemaDefinition(openApi, context, classElement, classElementTypeArgs, null, mediaTypes, jsonViewClass);
            if (schema == null) {
                continue;
            }
            if (TYPE_OBJECT.equals(schema.getType())) {
                if (schemaToBind.getType() == null) {
                    schemaToBind.setType(TYPE_OBJECT);
                }
                schema.setType(null);
            }
            var schemas = getter.apply(schemaToBind);
            if (CollectionUtils.isEmpty(schemas) || !schemas.contains(schema)) {
                methodAdd.accept(schemaToBind, schema);
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
     * @return The bound schema
     */
    protected Schema<?> bindSchemaAnnotationValue(VisitorContext context, TypedElement element, Schema<?> schemaToBind,
                                                  AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn,
                                                  @Nullable ClassElement jsonViewClass) {

        ClassElement classElement = element.getType();
        Pair<String, String> typeAndFormat;
        if (classElement.isIterable()) {
            typeAndFormat = Pair.of(TYPE_ARRAY, null);
        } else if (classElement instanceof EnumElement enumEl) {
            typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, null);
        } else {
            typeAndFormat = ConvertUtils.getTypeAndFormatByClass(classElement.getName(), classElement.isArray());
        }

        JsonNode schemaJson = toJson(schemaAnn.getValues(), context, jsonViewClass);
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson,
            schemaAnn.stringValue(PROP_TYPE).orElse(typeAndFormat.getFirst()),
            schemaAnn.stringValue(PROP_ONE_FORMAT).orElse(typeAndFormat.getSecond()),
            schemaAnn, jsonViewClass);
    }

    private Schema<?> doBindSchemaAnnotationValue(VisitorContext context, TypedElement element, Schema schemaToBind,
                                                  JsonNode schemaJson, String elType, String elFormat,
                                                  AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn,
                                                  @Nullable ClassElement jsonViewClass) {

        // need to set placeholders to set correct values and types to example field
        schemaJson = resolvePlaceholders(schemaJson, s -> expandProperties(s, getExpandableProperties(context), context));
        try {
            schemaToBind = Utils.getJsonMapper().readerForUpdating(schemaToBind).readValue(schemaJson);
        } catch (IOException e) {
            warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), context, element);
        }

        String defaultValue = null;
        String[] allowableValues = null;
        if (schemaAnn != null) {
            defaultValue = schemaAnn.stringValue(PROP_DEFAULT_VALUE).orElse(null);
            allowableValues = schemaAnn.stringValues(PROP_ALLOWABLE_VALUES);
            Map<CharSequence, Object> annValues = schemaAnn.getValues();
            Map<CharSequence, Object> valueMap = toValueMap(annValues, context, jsonViewClass);
            bindSchemaIfNecessary(context, schemaAnn, valueMap, jsonViewClass);
            processClassValues(schemaToBind, annValues, Collections.emptyList(), context, jsonViewClass);
        }

        if (elType == null && element != null) {
            ClassElement typeEl = element.getType();
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
        setAllowableValues(schemaToBind, allowableValues, element, elType, elFormat, context);
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
    protected Schema<?> bindArraySchemaAnnotationValue(VisitorContext context, TypedElement element, Schema<?> schemaToBind,
                                                       AnnotationValue<io.swagger.v3.oas.annotations.media.ArraySchema> schemaAnn,
                                                       @Nullable ClassElement jsonViewClass) {
        JsonNode schemaJson = toJson(schemaAnn.getValues(), context, jsonViewClass);
        if (schemaJson.isObject()) {
            ObjectNode objNode = (ObjectNode) schemaJson;
            JsonNode arraySchema = objNode.remove(PROP_ARRAY_SCHEMA);
            // flatten
            if (arraySchema != null && arraySchema.isObject()) {
                ((ObjectNode) arraySchema).remove(PROP_IMPLEMENTATION);
                objNode.setAll((ObjectNode) arraySchema);
            }
            // remove schema that maps to 'items'
            JsonNode items = objNode.remove(PROP_SCHEMA);
            if (items != null && schemaToBind != null && (schemaToBind.getType() != null && schemaToBind.getType().equals(TYPE_ARRAY))) {
                try {
                    schemaToBind.items(Utils.getJsonMapper().readerForUpdating(schemaToBind.getItems()).readValue(items));
                } catch (IOException e) {
                    warn("Error reading Swagger Schema for element [" + element + "]: " + e.getMessage(), context, element);
                }
            }
        }

        String elType = schemaJson.has(PROP_TYPE) ? schemaJson.get(PROP_TYPE).textValue() : null;
        String elFormat = schemaJson.has(PROP_ONE_FORMAT) ? schemaJson.get(PROP_ONE_FORMAT).textValue() : null;
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, elType, elFormat, null, jsonViewClass);
    }

    private Map<String, Object> annotationValueArrayToSubmap(Object[] a, String classifier, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        Map<String, Object> mediaTypes = new LinkedHashMap<>();
        for (Object o : a) {
            AnnotationValue<?> sv = (AnnotationValue<?>) o;
            String name = sv.stringValue(classifier).orElse(null);
            if (name == null && classifier.equals(PROP_MEDIA_TYPE)) {
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
        if (schema == null) {
            return;
        }
        var beanMap = BeanMap.of(schema);
        for (var entry : beanMap.entrySet()) {
            var value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (entry.getKey().equals("specVersion")) {
                continue;
            }
            if (entry.getKey().equals("exampleSetFlag") && StringUtils.FALSE.equals(value.toString())) {
                continue;
            }
            valueMap.put(entry.getKey(), value);
        }
        if (schema.get$ref() != null) {
            valueMap.put(PROP_REF_DOLLAR, schema.get$ref());
        }
    }

    private void bindSchemaIfNecessary(VisitorContext context, AnnotationValue<?> av, Map<CharSequence, Object> valueMap, @Nullable ClassElement jsonViewClass) {
        final Optional<String> impl = av.stringValue(PROP_IMPLEMENTATION);
        final Optional<String> not = av.stringValue(PROP_NOT);
        final Optional<String> schema = av.stringValue(PROP_SCHEMA);
        var anyOfList = av.stringValues(PROP_ANY_OF);
        var oneOfList = av.stringValues(PROP_ONE_OF);
        var allOfList = av.stringValues(PROP_ALL_OF);
        // remap keys.
        Object o = valueMap.remove(PROP_DEFAULT_VALUE);
        if (o != null) {
            valueMap.put(PROP_DEFAULT, o);
        }
        o = valueMap.remove(PROP_ALLOWABLE_VALUES);
        if (o != null) {
            valueMap.put(PROP_ENUM, o);
        }
        boolean isSchema = io.swagger.v3.oas.annotations.media.Schema.class.getName().equals(av.getAnnotationName());
        if (isSchema) {
            if (impl.isPresent()) {
                final String className = impl.get();
                bindSchemaForClassName(context, valueMap, className, jsonViewClass);
            }
            if (not.isPresent()) {
                final Schema<?> schemaNot = resolveSchema(null, ContextUtils.getClassElement(not.get(), context), context, Collections.emptyList(), jsonViewClass);
                var schemaMap = new HashMap<CharSequence, Object>();
                schemaToValueMap(schemaMap, schemaNot);
                valueMap.put(PROP_NOT, schemaMap);
            }
            if (anyOfList.length > 0) {
                bindSchemaForComposite(context, valueMap, anyOfList, PROP_ANY_OF, jsonViewClass);
            }
            if (oneOfList.length > 0) {
                bindSchemaForComposite(context, valueMap, oneOfList, PROP_ONE_OF, jsonViewClass);
            }
            if (allOfList.length > 0) {
                bindSchemaForComposite(context, valueMap, allOfList, PROP_ALL_OF, jsonViewClass);
            }
        }
        if (DiscriminatorMapping.class.getName().equals(av.getAnnotationName()) && schema.isPresent()) {
            final String className = schema.get();
            bindSchemaForClassName(context, valueMap, className, jsonViewClass);
        }
    }

    private void bindSchemaForComposite(VisitorContext context, Map<CharSequence, Object> valueMap, String[] classNames, String key, @Nullable ClassElement jsonViewClass) {
        var namesToSchemas = new ArrayList<Map<CharSequence, Object>>();
        for (var className : classNames) {
            ClassElement classElement = ContextUtils.getClassElement(className, context);
            var schemaMap = new HashMap<CharSequence, Object>();
            if (classElement != null) {
                final Schema<?> schema = resolveSchema(null, classElement, context, Collections.emptyList(), jsonViewClass);
                schemaToValueMap(schemaMap, schema);
            }
            namesToSchemas.add(schemaMap);
        }
        valueMap.put(key, namesToSchemas);
    }

    private void bindSchemaForClassName(VisitorContext context, Map<CharSequence, Object> valueMap, String className, @Nullable ClassElement jsonViewClass) {
        ClassElement classElement = ContextUtils.getClassElement(className, context);
        if (classElement != null) {
            final Schema<?> schema = resolveSchema(null, classElement, context, Collections.emptyList(), jsonViewClass);
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
        var propSchema = setSpecVersion(new Schema<>());
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

        // Here we need to skip Schema annotation on field level, because with micronaut 3.x method getDeclaredAnnotation
        // returned always null and found Schema annotation only on getters and setters
        var schemaAnnOnField = false;
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue = null;
        if (definingElement != null) {
            if (definingElement instanceof PropertyElement propertyEl) {
                var getterOpt = propertyEl.getReadMethod();
                if (getterOpt.isPresent()) {
                    schemaValue = getterOpt.get().getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    schemaAnnOnField = schemaValue != null;
                }
                if (schemaValue == null) {
                    var setterOpt = propertyEl.getWriteMethod();
                    if (setterOpt.isPresent()) {
                        schemaValue = setterOpt.get().getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                        schemaAnnOnField = schemaValue != null;
                    }
                }
            } else {
                schemaValue = definingElement.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                schemaAnnOnField = schemaValue != null && definingElement instanceof FieldElement;
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
                primitiveType = ClassUtils.forName(type.getName(), getClass().getClassLoader())
                    .map(PrimitiveType::fromType)
                    .orElse(null);
            } else {
                primitiveType = null;
            }
            if (primitiveType == null) {
                String schemaName = computeDefaultSchemaName(definingElement, type, typeArgs, context, jsonViewClass);
                schema = schemas.get(schemaName);
                JavadocDescription javadoc = Utils.getJavadocParser().parse(type.getDocumentation().orElse(null));
                if (schema == null) {

                    if (type instanceof EnumElement enumEl) {
                        schema = setSpecVersion(new Schema<>());
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
                return setSpecVersion(primitiveType.createProperty());
            }
        } else {
            // Schema annotation property `name` on field level means, that this property must be with this name.
            // This is not a schema name
            String schemaName = !schemaAnnOnField ? schemaValue.stringValue(PROP_NAME).orElse(null) : null;
            if (schemaName == null) {
                schemaName = computeDefaultSchemaName(definingElement, type, typeArgs, context, jsonViewClass);
            }
            schema = schemas.get(schemaName);
            if (schema == null) {
                if (inProgressSchemas.contains(schemaName)) {
                    // Break recursion
                    return setSpecVersion(new Schema<>().$ref(SchemaUtils.schemaRef(schemaName)));
                }
                inProgressSchemas.add(schemaName);
                try {
                    schema = readSchema(schemaValue, openAPI, context, type, typeArgs, schemaAnnOnField ? definingElement : type, mediaTypes, jsonViewClass);
                    var typeSchema = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    if (typeSchema != null) {
                        Schema<?> originalTypeSchema = readSchema(typeSchema, openAPI, context, type, typeArgs, schemaAnnOnField ? definingElement : type, mediaTypes, jsonViewClass);
                        if (originalTypeSchema != null && schema != null) {
                            if (StringUtils.isNotEmpty(originalTypeSchema.getDescription())) {
                                schema.setDescription(originalTypeSchema.getDescription());
                            }
                            if ((originalTypeSchema.getNullable() != null && originalTypeSchema.getNullable())
                                || (isOpenapi31()
                                && CollectionUtils.isNotEmpty(originalTypeSchema.getTypes())
                                && originalTypeSchema.getTypes().contains(SchemaUtils.TYPE_NULL))
                            ) {
                                SchemaUtils.setNullable(schema);
                            }
                            schema.setRequired(originalTypeSchema.getRequired());
                        }
                    }

                    if (schema != null) {
                        processSuperTypes(schema, schemaName, type, definingElement, openAPI, mediaTypes, schemas, context, jsonViewClass);
                    }
                } finally {
                    inProgressSchemas.remove(schemaName);
                }
            }
        }
        if (schema != null) {
            var externalDocsValue = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.ExternalDocumentation.class);
            ExternalDocumentation externalDocs = null;
            if (externalDocsValue != null) {
                externalDocs = toValue(externalDocsValue.getValues(), context, ExternalDocumentation.class, null).orElse(null);
            }
            if (externalDocs != null) {
                schema.setExternalDocs(externalDocs);
            }
            setSchemaDocumentation(type, schema);
            var schemaRef = setSpecVersion(new Schema<>());
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

        if (type == null || type.getName().equals(Object.class.getName())) {
            return null;
        }

        var classElement = ((TypedElement) type).getType();
        var superTypes = new ArrayList<ClassElement>();
        Collection<ClassElement> parentInterfaces = classElement.getInterfaces();
        if (classElement.isInterface() && !parentInterfaces.isEmpty()) {
            for (ClassElement parentInterface : parentInterfaces) {
                if (ClassUtils.isJavaLangType(parentInterface.getName())
                    || isProtobufGenerated(parentInterface)
                    || parentInterface.getBeanProperties().isEmpty()) {
                    continue;
                }
                superTypes.add(parentInterface);
            }
        } else {
            var superType = classElement.getSuperType().orElse(null);
            if (superType != null
                    // check protobuf generated classes
                    && !isProtobufMessageClass(superType)) {
                superTypes.add(superType);
            }
        }
        if (!type.isRecord() && !superTypes.isEmpty()) {
            // skip if it is Enum or Object super class
            String firstSuperTypeName = superTypes.get(0).getName();
            if (superTypes.size() == 1
                && (firstSuperTypeName.equals(Enum.class.getName()) || firstSuperTypeName.equals(Object.class.getName()))) {
                if (schema != null) {
                    schema.setName(schemaName);
                    schemas.put(schemaName, schema);
                }

                return null;
            }

            if (schema == null) {
                schema = setSpecVersion(new ComposedSchema());
                schema.setType(TYPE_OBJECT);
            }
            for (ClassElement sType : superTypes) {
                Map<String, ClassElement> sTypeArgs = sType.getTypeArguments();
                ClassElement customStype = getCustomSchema(sType.getName(), sTypeArgs, context);
                if (customStype != null) {
                    sType = customStype;
                }
                readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, sType, schemas, sTypeArgs, jsonViewClass);
            }
        } else {
            if (schema == null) {
                schema = setSpecVersion(new Schema<>());
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
        String parentSchemaName = superType.stringValue(io.swagger.v3.oas.annotations.media.Schema.class, PROP_NAME)
            .orElse(computeDefaultSchemaName(definingElement, superType, superTypeArgs, context, jsonViewClass));

        if (schemas.get(parentSchemaName) != null
            || getSchemaDefinition(openAPI, context, superType, superTypeArgs, null, mediaTypes, jsonViewClass) != null) {
            var parentSchema = setSpecVersion(new Schema<>());
            parentSchema.set$ref(SchemaUtils.schemaRef(parentSchemaName));
            if (schema.getAllOf() == null || !schema.getAllOf().contains(parentSchema)) {
                schema.addAllOfItem(parentSchema);
            }
        }
        if (superType.isInterface()) {
            for (var interfaceEl : superType.getInterfaces()) {
                if (ClassUtils.isJavaLangType(interfaceEl.getName())
                    || interfaceEl.getBeanProperties().isEmpty()) {
                    continue;
                }

                Map<String, ClassElement> interfaceTypeArgs = interfaceEl.getTypeArguments();
                ClassElement customInterfaceType = getCustomSchema(interfaceEl.getName(), interfaceTypeArgs, context);
                if (customInterfaceType != null) {
                    interfaceEl = customInterfaceType;
                }

                readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, interfaceEl, schemas, interfaceTypeArgs, jsonViewClass);
            }
        } else if (superType.getSuperType().isPresent()) {
            ClassElement superSuperType = superType.getSuperType().get();
            Map<String, ClassElement> superSuperTypeArgs = superSuperType.getTypeArguments();
            ClassElement customSuperSuperType = getCustomSchema(superSuperType.getName(), superSuperTypeArgs, context);
            if (customSuperSuperType != null) {
                superSuperType = customSuperSuperType;
            }
            readAllInterfaces(openAPI, context, definingElement, mediaTypes, schema, superSuperType, schemas, superSuperTypeArgs, jsonViewClass);
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
     */
    protected Schema<?> readSchema(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue, OpenAPI openAPI, VisitorContext context,
                                   @Nullable Element type, Map<String, ClassElement> typeArgs,
                                   @Nullable Element definingElement, List<MediaType> mediaTypes,
                                   @Nullable ClassElement jsonViewClass) {

        var schema = setSpecVersion(new Schema<>());
        processSchemaAnn(schema, context, definingElement, (ClassElement) type, schemaValue);

        String elType = SchemaUtils.getType(schema);
        String elFormat = schema.getFormat();
        if (elType == null && type instanceof TypedElement typedEl) {
            Pair<String, String> typeAndFormat;
            if (typedEl instanceof EnumElement enumEl) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, null, elFormat);
            } else {
                typeAndFormat = ConvertUtils.getTypeAndFormatByClass(typedEl.getName(), typedEl.isArray());
            }
            elType = typeAndFormat.getFirst();
            schema.setType(elType);
            if (elFormat == null) {
                elFormat = typeAndFormat.getSecond();
                schema.setFormat(elFormat);
            }
        }

        if (type instanceof EnumElement enumEl) {
            if (CollectionUtils.isEmpty(schema.getEnum())) {
                schema.setEnum(getEnumValues(enumEl, schema.getType(), schema.getFormat(), context));
            }
        } else {
            JavadocDescription javadoc = type != null ? Utils.getJavadocParser().parse(type.getDescription()) : null;
            populateSchemaProperties(openAPI, context, type, typeArgs, schema, mediaTypes, javadoc, jsonViewClass);
            checkAllOf(schema);
        }
        return schema;
    }

    private List<Object> getEnumValues(EnumElement type, String schemaType, String schemaFormat, VisitorContext context) {
        var isProtobufGenerated = isProtobufGenerated(type);
        var enumValues = new ArrayList<>();
        for (EnumConstantElement element : type.elements()) {

            if (isProtobufGenerated) {
                Integer protoValue = null;
                for (var field : type.getFields()) {
                    if (field.getName().equals(element.getSimpleName() + "_VALUE")) {
                        try {
                            protoValue = (Integer) field.getConstantValue();
                        } catch (Exception e) {
                            // do nothing
                        }
                        break;
                    }
                }
                if (protoValue != null) {
                    enumValues.add(protoValue);
                }
                continue;
            }

            var schemaAnn = getAnnotation(element, io.swagger.v3.oas.annotations.media.Schema.class);
            boolean isHidden = schemaAnn != null && schemaAnn.booleanValue(PROP_HIDDEN).orElse(false);

            if (isHidden
                || isAnnotationPresent(element, Hidden.class)
                || isAnnotationPresent(element, JsonIgnore.class)) {
                continue;
            }
            var jsonPropertyAnn = getAnnotation(element, JsonProperty.class);
            String jacksonValue = jsonPropertyAnn != null ? jsonPropertyAnn.stringValue(PROP_VALUE).orElse(null) : null;
            if (StringUtils.hasText(jacksonValue)) {
                try {
                    enumValues.add(ConvertUtils.normalizeValue(jacksonValue, schemaType, schemaFormat, context));
                } catch (JsonProcessingException e) {
                    warn("Error converting jacksonValue " + jacksonValue + " : to " + type + ":\n" + Utils.printStackTrace(e), context, element);
                    enumValues.add(element.getSimpleName());
                }
            } else {
                enumValues.add(element.getSimpleName());
            }
        }
        return !enumValues.isEmpty() ? enumValues : null;
    }

    private String computeDefaultSchemaName(Element definingElement, Element type, Map<String, ClassElement> typeArgs, VisitorContext context,
                                            @Nullable ClassElement jsonViewClass) {

        String jsonViewPostfix = StringUtils.EMPTY_STRING;
        if (jsonViewClass != null) {
            String jsonViewClassName = jsonViewClass.getName();
            jsonViewClassName = jsonViewClassName.replaceAll("\\$", DOT);
            jsonViewPostfix = UNDERSCORE + (jsonViewClassName.contains(DOT) ? jsonViewClassName.substring(jsonViewClassName.lastIndexOf('.') + 1) : jsonViewClassName);
        }

        final String metaAnnName = definingElement == null ? null : definingElement.getAnnotationNameByStereotype(io.swagger.v3.oas.annotations.media.Schema.class).orElse(null);
        if (metaAnnName != null && !io.swagger.v3.oas.annotations.media.Schema.class.getName().equals(metaAnnName)) {
            return NameUtils.getSimpleName(metaAnnName) + jsonViewPostfix;
        }
        String packageName;
        String resultSchemaName;
        if (type instanceof TypedElement typedEl && !(type instanceof EnumElement)) {
            ClassElement typeType = typedEl.getType();
            var isProtobufGenerated = isProtobufGenerated(typeType);
            packageName = typeType.getPackageName();
            if (CollectionUtils.isNotEmpty(typeType.getTypeArguments())) {
                resultSchemaName = computeNameWithGenerics(typeType, typeArgs, context, isProtobufGenerated);
            } else {
                resultSchemaName = computeNameWithGenerics(typeType, Collections.emptyMap(), context, isProtobufGenerated);
            }
        } else {
            resultSchemaName = type.getSimpleName();
            packageName = NameUtils.getPackageName(type.getName());
        }

        SchemaDecorator schemaDecorator = getSchemaDecoration(packageName, context);
        resultSchemaName = resultSchemaName.replace(DOLLAR, DOT) + jsonViewPostfix;
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
            resultSchemaName += UNDERSCORE + index;
        }
        schemaNameToClassNameMap.put(resultSchemaName, fullClassNameWithGenerics);

        return resultSchemaName;
    }

    private String computeNameWithGenerics(ClassElement classElement, Map<String, ClassElement> typeArgs, VisitorContext context, boolean isProtobufGenerated) {
        var className = classElement.getSimpleName();
        if (isProtobufGenerated) {
            className = normalizeProtobufClassName(className);
        }
        var builder = new StringBuilder(className);
        computeNameWithGenerics(classElement, builder, new HashSet<>(), typeArgs, context);
        return builder.toString();
    }

    private String addTypeArgsAnnotations(String memberName, Object annValue) {

        var result = new StringBuilder();

        if (annValue instanceof AnnotationValue aValue) {
            var annName = aValue.getAnnotationName();
            var values = ((Map<String, Object>) aValue.getValues());
            var endPos = annName.contains(DOLLAR) ? annName.lastIndexOf(DOLLAR) : annName.length();
            result.append(annName, annName.lastIndexOf('.') + 1, endPos);
            if (CollectionUtils.isNotEmpty(values)) {
                result.append('_');
                for (var entry : values.entrySet()) {
                    result.append(addTypeArgsAnnotations(entry.getKey(), entry.getValue()));
                }
                result.append('_');
            }
        } else if (annValue instanceof Iterable<?> iterable) {
            var isFirst = true;
            for (var item : iterable) {
                if (!isFirst) {
                    result.append('_');
                }
                if (memberName != null) {
                    result.append(memberName).append('_');
                }
                result.append(addTypeArgsAnnotations(null, item));
                isFirst = false;
            }
        } else {
            if (memberName != null && !memberName.equals(PROP_VALUE)) {
                result.append(memberName).append('_');
            }
            result.append(annValue);
        }
        return result.toString();
    }

    private void computeNameWithGenerics(ClassElement classElement, StringBuilder builder, Set<String> computed, Map<String, ClassElement> typeArgs, VisitorContext context) {
        computed.add(classElement.getName());
        final Iterator<ClassElement> i = typeArgs.values().iterator();
        if (i.hasNext()) {

            builder.append('_');
            while (i.hasNext()) {
                ClassElement ce = i.next();
                if (ClassUtils.isJavaLangType(ce.getName())) {
                    var typeArgAnnotations = ce.getAnnotationNames();
                    if (CollectionUtils.isNotEmpty(typeArgAnnotations)) {
                        for (var typeArgAnnName : typeArgAnnotations) {
                            var annValue = ce.getAnnotation(typeArgAnnName);
                            builder.append(addTypeArgsAnnotations(null, typeArgAnnName.endsWith("$List") ? annValue.getValues().get(PROP_VALUE) : annValue));
                        }
                    }
                }
                builder.append(ce.getSimpleName());
                Map<String, ClassElement> ceTypeArgs = ce.getTypeArguments();
                ClassElement customElement = getCustomSchema(ce.getName(), ceTypeArgs, context);
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

    private void populateSchemaProperties(OpenAPI openAPI, VisitorContext context, Element type, Map<String, ClassElement> typeArgs, Schema<?> schema,
                                          List<MediaType> mediaTypes, JavadocDescription classJavadoc, @Nullable ClassElement jsonViewClass) {
        ClassElement classElement = null;
        if (type instanceof ClassElement classEl) {
            classElement = classEl;
        } else if (type instanceof TypedElement typedEl) {
            classElement = typedEl.getType();
        }

        if (classElement != null && !ClassUtils.isJavaLangType(classElement.getName())) {
            List<PropertyElement> beanProperties;
            try {
                beanProperties = classElement.getBeanProperties(
                        PropertyElementQuery.of(classElement)
                            .excludedAnnotations(Set.of(
                                Hidden.class.getName(),
                                JsonIgnore.class.getName()
                            ))
                    ).stream()
                    .filter(p ->
                        !"groovy.lang.MetaClass".equals(p.getType().getName())
                            && !"java.lang.Class".equals(p.getType().getName())
                    )
                    .toList();
            } catch (Exception e) {
                warn("Error with getting properties for class " + classElement.getName() + ": " + e + "\n" + Utils.printStackTrace(e), context, classElement);
                // Workaround for https://github.com/micronaut-projects/micronaut-openapi/issues/313
                beanProperties = Collections.emptyList();
            }
            beanProperties = filterProtobufProperties(classElement, beanProperties);
            processPropertyElements(openAPI, context, type, typeArgs, schema, beanProperties, mediaTypes, classJavadoc, jsonViewClass);

            String visibilityLevelProp = getConfigProperty(MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, context);
            var visibilityLevel = VisibilityLevel.PUBLIC;
            if (StringUtils.hasText(visibilityLevelProp)) {
                try {
                    visibilityLevel = VisibilityLevel.valueOf(visibilityLevelProp.toUpperCase());
                } catch (Exception e) {
                    throw new IllegalStateException("Wrong value for visibility level property: " + getConfigProperty(MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, context));
                }
            }

            var publicFields = new ArrayList<FieldElement>();

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
            if (isHiddenElement(publicField)) continue;

            var isGetterOverridden = false;
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
                                isGetterOverridden = CollectionUtils.isNotEmpty(readerMethod.getAnnotationNames()) || fieldJavadoc != null;
                                break;
                            }
                        }
                    }
                }
            }

            if (publicField instanceof MemberElement memberEl && (memberEl.getDeclaringType().getType().getName().equals(type.getName()) || isGetterOverridden)) {

                ClassElement fieldType = publicField.getGenericType();
                if (withJsonView && !allowedByJsonView(publicField, classLvlJsonViewClasses, jsonViewClass, context)) {
                    continue;
                }

                Schema<?> propertySchema = resolveSchema(openAPI, publicField, fieldType, context, mediaTypes, jsonViewClass, fieldJavadoc, classJavadoc);

                processSchemaProperty(
                    context,
                    publicField,
                    fieldType,
                    classElement,
                    schema,
                    propertySchema
                );
            }
        }
    }

    private static boolean isHiddenElement(TypedElement elementType) {
        boolean isHidden = getAnnotationMetadata(elementType)
            .booleanValue(io.swagger.v3.oas.annotations.media.Schema.class, PROP_HIDDEN).orElse(false);
        var jsonAnySetterAnn = getAnnotation(elementType, JsonAnySetter.class);
        return elementType.getType().isAssignable(Class.class)
                || isAnnotationPresent(elementType, JsonIgnore.class)
                || isAnnotationPresent(elementType, Hidden.class)
                || (jsonAnySetterAnn != null && jsonAnySetterAnn.booleanValue("enabled").orElse(true))
                || isHidden;
    }

    private boolean allowedByJsonView(TypedElement publicField, String[] classLvlJsonViewClasses, ClassElement jsonViewClassEl, VisitorContext context) {
        String[] fieldJsonViewClasses = getAnnotationMetadata(publicField).stringValues(JsonView.class);
        if (ArrayUtils.isEmpty(fieldJsonViewClasses)) {
            fieldJsonViewClasses = classLvlJsonViewClasses;
        }
        if (ArrayUtils.isEmpty(fieldJsonViewClasses)) {
            return isJsonViewDefaultInclusion(context);
        }

        for (String fieldJsonViewClass : fieldJsonViewClasses) {
            var classEl = ContextUtils.getClassElement(fieldJsonViewClass, context);
            if (classEl != null && jsonViewClassEl.isAssignable(classEl)) {
                return true;
            }
        }

        return false;
    }

    private void processArgTypeAnnotations(ClassElement type, @Nullable Schema<?> schema) {
        if (schema == null || type == null || type.getAnnotationNames().isEmpty()) {
            return;
        }
        if (isNullable(type) && !isNotNullable(type)) {
            SchemaUtils.setNullable(schema);
        }
        processJakartaValidationAnnotations(type, type, schema);
    }

    private Schema<?> getPrimitiveType(ClassElement type, String typeName) {
        Schema<?> schema = null;
        Class<?> aClass = ClassUtils.getPrimitiveType(typeName).orElse(null);
        if (aClass == null) {
            aClass = ClassUtils.forName(typeName, getClass().getClassLoader()).orElse(null);
        }

        if (aClass != null) {
            Class<?> wrapperType = ReflectionUtils.getWrapperType(aClass);

            var primitiveType = PrimitiveType.fromType(wrapperType);
            if (primitiveType != null) {
                schema = setSpecVersion(primitiveType.createProperty());
            }
        }

        processArgTypeAnnotations(type, schema);

        return schema;
    }

    /**
     * Processes {@link SecurityScheme}
     * annotations.
     *
     * @param element The element
     * @param context The visitor context
     */
    protected void processSecuritySchemes(ClassElement element, VisitorContext context) {
        var values = element.getAnnotationValuesByType(SecurityScheme.class);
        final OpenAPI openApi = Utils.resolveOpenApi(context);
        ConvertUtils.addSecuritySchemes(openApi, values, context);
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
                var tagValues = tag.getValues();
                if (tag.getAnnotationName().equals(io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName())
                    && !tagValues.isEmpty()) {
                    Object name = tagValues.get(PROP_NAME);
                    Object scopes = Optional.ofNullable(tagValues.get(PROP_SCOPES)).orElse(new ArrayList<String>());
                    values = Collections.singletonMap((CharSequence) name, scopes);
                } else {
                    values = tagValues;
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
