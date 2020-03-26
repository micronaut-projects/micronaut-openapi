/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
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
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.javadoc.JavadocParser;
import io.micronaut.openapi.util.Yaml;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
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
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Abstract base class for OpenAPI visitors.
 *
 * @author graemerocher
 * @since 1.0
 */
abstract class AbstractOpenApiVisitor  {

    static final String ATTR_TEST_MODE = "io.micronaut.OPENAPI_TEST";
    static final String ATTR_OPENAPI = "io.micronaut.OPENAPI";
    static OpenAPI testReference;

    /**
     * The JSON mapper.
     */
    ObjectMapper jsonMapper = Json.mapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    /**
     * The YAML mapper.
     */
    ObjectMapper yamlMapper = Yaml.mapper();

    /**
     * Stores the current in progress type.
     */
    private List<String> inProgressSchemas = new ArrayList<>(10);

    /**
     * Convert the given map to a JSON node.
     *
     * @param values The values
     * @param context The visitor context
     * @return The node
     */
    JsonNode toJson(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = toValueMap(values, context);
        return jsonMapper.valueToTree(newValues);
    }

    /**
     * Convert the given Map to a JSON node and then to the specified type.
     * @param <T> The output class type
     * @param values The values
     * @param context The visitor context
     * @param type The class
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
     * Resolve the PathItem for the given {@link UriMatchTemplate}.
     *
     * @param context The context
     * @param matchTemplate The match template
     * @return The {@link PathItem}
     */
    PathItem resolvePathItem(VisitorContext context, UriMatchTemplate matchTemplate) {
        OpenAPI openAPI = resolveOpenAPI(context);
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            paths = new Paths();
            openAPI.setPaths(paths);
        }


        final String pathString = matchTemplate.toPathString();
        PathItem pathItem = paths.get(pathString);
        if (pathItem == null) {
            pathItem = new PathItem();
            paths.put(pathString, pathItem);
        }
        return pathItem;
    }

    /**
     * Resolve the {@link OpenAPI} instance.
     *
     * @param context The context
     * @return The {@link OpenAPI} instance
     */
    OpenAPI resolveOpenAPI(VisitorContext context) {
        OpenAPI openAPI = context.get(ATTR_OPENAPI, OpenAPI.class).orElse(null);
        if (openAPI == null) {
            openAPI = new OpenAPI();
            context.put(ATTR_OPENAPI, openAPI);
            if (Boolean.getBoolean(ATTR_TEST_MODE)) {
                testReference = openAPI;
            }
        }
        return openAPI;
    }

    /**
     * Converts Json node into a class' instance or throws 'com.fasterxml.jackson.core.JsonProcessingException', adds extensions if present.
     * @param jn The json node
     * @param clazz The output class instance
     * @param <T> The output class type
     * @return The converted instance
     * @throws com.fasterxml.jackson.core.JsonProcessingException if error
     */
    protected <T> T treeToValue(JsonNode jn, Class<T> clazz) throws com.fasterxml.jackson.core.JsonProcessingException {
        T value = jsonMapper.treeToValue(jn, clazz);
        if (value != null) {
            resolveExtensions(jn).ifPresent(extensions -> BeanMap.of(value).put("extensions", extensions));
        }
        return value;
    }

    /**
     * Convert the values to a map.
     * @param values The values
     * @param context The visitor context
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
                AnnotationClassValue<?> acv = (AnnotationClassValue) value;
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
                            List<Class> classes = new ArrayList<>(a.length);
                            for (Object o : a) {
                                AnnotationClassValue<?> acv = (AnnotationClassValue) o;
                                acv.getType().ifPresent(classes::add);
                            }
                            newValues.put(key, classes);
                        } else if (areAnnotationValues) {
                            String annotationName = ((AnnotationValue) first).getAnnotationName();
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
                            } else if (Content.class.getName().equals(annotationName)) {
                                Map mediaTypes = annotationValueArrayToSubmap(a, "mediaType", context);
                                newValues.put(key, mediaTypes);
                            } else if (Link.class.getName().equals(annotationName) || Header.class.getName().equals(annotationName)) {
                                Map links = annotationValueArrayToSubmap(a, "name", context);
                                newValues.put(key, links);
                            } else if (LinkParameter.class.getName().equals(annotationName)) {
                                Map params = toTupleSubMap(a, "name",  "expression");
                                newValues.put(key, params);
                            } else if (OAuthScope.class.getName().equals(annotationName)) {
                                Map params = toTupleSubMap(a, "name",  "description");
                                newValues.put(key, params);
                            } else if (ApiResponse.class.getName().equals(annotationName)) {
                                Map responses = new LinkedHashMap();
                                for (Object o : a) {
                                    AnnotationValue<ApiResponse> sv = (AnnotationValue<ApiResponse>) o;
                                    String name = sv.get("responseCode", String.class).orElse("default");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                    responses.put(name, map);
                                }
                                newValues.put(key, responses);
                            } else if (ExampleObject.class.getName().equals(annotationName)) {
                                Map examples = new LinkedHashMap();
                                for (Object o : a) {
                                    AnnotationValue<ExampleObject> sv = (AnnotationValue<ExampleObject>) o;
                                    String name = sv.get("name", String.class).orElse("example");
                                    Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                    examples.put(name, map);
                                }
                                newValues.put(key, examples);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                Map variables = new LinkedHashMap();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    Optional<String> n = sv.get("name", String.class);
                                    n.ifPresent(name -> {
                                        Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                        Object dv = map.get("defaultValue");
                                        if (dv != null) {
                                            map.put("default", dv);
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

                                    List list = new ArrayList();
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
                    }
                } else if (key.equals("discriminatorProperty")) {
                    final Map<String, Object> discriminatorMap = getDiscriminatorMap(newValues);
                    discriminatorMap.put("propertyName", parseJsonString(value).orElse(value));
                    newValues.put("discriminator", discriminatorMap);
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
        String name = extension.getRequiredValue("name", String.class);
        final String key = name.length() > 0 ? org.apache.commons.lang3.StringUtils.prependIfMissing(name, "x-") : name;
        for (AnnotationValue<ExtensionProperty> prop : extension.getAnnotations("properties", ExtensionProperty.class)) {
            final String propertyName = prop.getRequiredValue("name", String.class);
            final String propertyValue = prop.getRequiredValue(String.class);
            JsonNode processedValue = null;
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
                    if (value == null || !(value instanceof Map)) {
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
            .filter(entry -> filters == null || ! filters.contains(entry.getKey()))
            .collect(toMap(
                 e -> e.getKey().equals("requiredProperties") ? "required" : e.getKey(), Map.Entry::getValue));
        Optional<T> schema = toValue(values, context, type);
        schema.ifPresent(s -> schemaToValueMap(arraySchemaMap, s));
    }

    private Map<CharSequence, Object> resolveArraySchemaAnnotationValues(VisitorContext context, AnnotationValue<?> av) {
        final Map<CharSequence, Object> arraySchemaMap = new HashMap<>(10);
        // properties
        av.get("arraySchema", AnnotationValue.class).ifPresent(annotationValue -> {
            processAnnotationValue(context, (AnnotationValue<?>) annotationValue, arraySchemaMap, Arrays.asList("ref", "implementation"), Schema.class);
        });
        // items
        av.get("schema", AnnotationValue.class).ifPresent(annotationValue -> {
            Optional<String> impl = ((AnnotationValue<?>) annotationValue).get("implementation", String.class);
            Optional<String> type = ((AnnotationValue<?>) annotationValue).get("type", String.class);
            Optional<String> format = ((AnnotationValue<?>) annotationValue).get("format", String.class);
            Optional<ClassElement> classElement = Optional.empty();
            PrimitiveType primitiveType = null;
            if (impl.isPresent()) {
                classElement = context.getClassElement(impl.get());
            } else if (type.isPresent()) {
                // if format is "binary", we want PrimitiveType.BINARY
                primitiveType = PrimitiveType.fromName(format.isPresent() && format.get().equals("binary") ? format.get() : type.get());
                if (primitiveType != null) {
                    classElement = context.getClassElement(primitiveType.getKeyClass());
                } else {
                    classElement = context.getClassElement(type.get());
                }
            }
            if (classElement.isPresent()) {
                if (primitiveType == null) {
                    final OpenAPI openAPI = resolveOpenAPI(context);
                    final ArraySchema schema = arraySchema(resolveSchema(openAPI, null, classElement.get(), context, null));
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

    private Map toTupleSubMap(Object[] a, String entryKey, String entryValue) {
        Map params = new LinkedHashMap();
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

    /**
     * Resolves the schema for the given type element.
     *
     * @param openAPI The OpenAPI object
     * @param definingElement The defining element
     * @param type The type element
     * @param context The context
     * @param mediaType An optional media type
     * @return The schema or null if it cannot be resolved
     */
    protected @Nullable Schema resolveSchema(OpenAPI openAPI, @Nullable Element definingElement, ClassElement type, VisitorContext context, @Nullable String mediaType) {
        Schema schema = null;

        if (type instanceof EnumElement) {
            schema = getSchemaDefinition(mediaType, openAPI, context, type, definingElement);
        } else {

            boolean isPublisher = false;
            boolean isObservable = false;

            // StreamingFileUpload implements Publisher, but it should be not considered as a Publisher in the spec file
            if (!type.isAssignable("io.micronaut.http.multipart.StreamingFileUpload") && isContainerType(type)) {
                isPublisher = type.isAssignable(Publisher.class.getName()) && !type.isAssignable("reactor.core.publisher.Mono");
                isObservable = type.isAssignable("io.reactivex.Observable") && !type.isAssignable("reactor.core.publisher.Mono");
                type = type.getFirstTypeArgument().orElse(null);
            }

            if (type != null) {

                String typeName = type.getName();
                // File upload case
                if ("io.micronaut.http.multipart.StreamingFileUpload".equals(typeName) ||
                    "io.micronaut.http.multipart.CompletedFileUpload".equals(typeName) ||
                    "io.micronaut.http.multipart.CompletedPart".equals(typeName) ||
                    "io.micronaut.http.multipart.PartData".equals(typeName)) {
                    isPublisher = isPublisher && ! "io.micronaut.http.multipart.PartData".equals(typeName);
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
                        Element valueType = type.getTypeArguments().get("V");
                        if (valueType.getName().equals(Object.class.getName())) {
                            schema.setAdditionalProperties(true);
                        } else {
                            Schema additionalPropertiesSchema = getPrimitiveType(valueType.getName());
                            if (additionalPropertiesSchema == null) {
                                additionalPropertiesSchema = getSchemaDefinition(mediaType, openAPI, context, valueType, definingElement);
                            }
                            schema.setAdditionalProperties(additionalPropertiesSchema);
                        }
                    }
                } else if (type.isIterable()) {
                    if (primitiveType != null) {
                        schema = getPrimitiveType(typeName);
                    } else {
                        Optional<ClassElement> componentType = type.getFirstTypeArgument();
                        if (componentType.isPresent()) {
                            schema = getPrimitiveType(componentType.get().getName());
                        } else {
                            schema = getPrimitiveType(Object.class.getName());
                        }

                        if (schema == null && componentType.isPresent()) {
                            ClassElement componentElement = componentType.get();
                            // we must have a POJO so let's create a component
                            schema = getSchemaDefinition(mediaType, openAPI, context, componentElement, definingElement);
                        }
                    }
                    if (schema != null) {
                        schema = arraySchema(schema);
                    }
                } else {
                    schema = getSchemaDefinition(mediaType, openAPI, context, type, definingElement);
                }

            }

            if (schema != null) {
                boolean isStream = MediaType.TEXT_EVENT_STREAM.equals(mediaType) || MediaType.APPLICATION_JSON_STREAM.equals(mediaType);
                if (!isStream && (isPublisher || isObservable)) {
                    schema = arraySchema(schema);
                }
            }
        }
        return schema;
    }

    /**
     * Resolve the components.
     * @param openAPI The open API
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


    /**
     * Processes a schema property.
     *  @param context The visitor context
     * @param element The element
     * @param elementType The elemen type
     * @param parentSchema The parent schema
     * @param propertySchema The property schema
     */
    protected void processSchemaProperty(VisitorContext context, Element element, ClassElement elementType, Schema parentSchema, Schema propertySchema) {
        if (propertySchema != null) {
            propertySchema = bindSchemaForElement(context, element, elementType, propertySchema);
            String propertyName = Optional.ofNullable(propertySchema.getName()).orElse(element.getName());

            parentSchema.addProperties(propertyName, propertySchema);

            if (element.isAnnotationPresent(NotNull.class)
                    || element.isAnnotationPresent(NotBlank.class)
                    || element.isAnnotationPresent(NotEmpty.class)) {

                List<String> requiredList = parentSchema.getRequired();
                // Check for duplicates
                if (requiredList == null || !requiredList.contains(propertyName)) {
                    parentSchema.addRequiredItem(propertyName);
                }
            }
        }
    }

    /**
     * Binds the schema for the given element.
     *
     * @param context The context
     * @param element The element
     * @param elementType The element type
     * @param schemaToBind The schema to bind
     * @return The bound schema
     */
    protected Schema bindSchemaForElement(VisitorContext context, Element element, ClassElement elementType, Schema schemaToBind) {
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn = element.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (schemaAnn != null) {
            schemaToBind = bindSchemaAnnotationValue(context, element, schemaToBind, schemaAnn);
            Optional<String> schemaName = schemaAnn.get("name", String.class);
            if (schemaName.isPresent()) {
                schemaToBind.setName(schemaName.get());
            }
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

            element.findAnnotation(Pattern.class).flatMap((p) -> p.get("regexp", String.class)).ifPresent(finalSchemaToBind::setFormat);
        }

        Optional<String> documentation = element.getDocumentation();
        if (StringUtils.isEmpty(schemaToBind.getDescription())) {
            String doc = documentation.orElse(null);
            if (doc != null) {
                JavadocDescription desc = new JavadocParser().parse(doc);
                schemaToBind.setDescription(desc.getMethodDescription());
            }
        }
        if (element.isAnnotationPresent(Deprecated.class)) {
            schemaToBind.setDeprecated(true);
        }

        final String defaultValue = element.getValue(Bindable.class, "defaultValue", String.class).orElse(null);
        if (defaultValue != null && schemaToBind.getDefault() == null) {
            schemaToBind.setDefault(defaultValue);
        }
        if (element.isAnnotationPresent(Nullable.class)) {
            schemaToBind.setNullable(true);
        }
        return schemaToBind;
    }

    /**
     * Binds the schema for the given element.
     *
     * @param context The context
     * @param element The element
     * @param schemaToBind The schema to bind
     * @param schemaAnn The schema annotation
     * @return The bound schema
     */
    protected Schema bindSchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind, AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {
        JsonNode schemaJson = toJson(schemaAnn.getValues(), context);
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, schemaAnn.get("defaultValue", String.class).orElse(null),
                schemaAnn.get("allowableValues", String[].class).orElse(null));
    }

    private Schema doBindSchemaAnnotationValue(VisitorContext context, Element element, Schema schemaToBind,
            JsonNode schemaJson, String defaultValue, String[] allowableValues) {
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
        return doBindSchemaAnnotationValue(context, element, schemaToBind, schemaJson, null, null);
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

    private Map annotationValueArrayToSubmap(Object[] a, String classifier, VisitorContext context) {
        Map mediaTypes = new LinkedHashMap();
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
        if (io.swagger.v3.oas.annotations.media.DiscriminatorMapping.class.getName().equals(av.getAnnotationName()) && schema.isPresent()) {
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
            final OpenAPI openAPI = resolveOpenAPI(context);
            Map<CharSequence, Object> schemaMap = new HashMap<>();
            if (classElement.isPresent()) {
                final Schema schema = resolveSchema(openAPI, null, classElement.get(), context, null);
                schemaToValueMap(schemaMap, schema);
            }
            return schemaMap;
        }).collect(Collectors.toList());
        valueMap.put(key, namesToSchemas);
    }

    private void bindSchemaForClassName(VisitorContext context, Map<CharSequence, Object> valueMap, String className) {
        final Optional<ClassElement> classElement = context.getClassElement(className);
        final OpenAPI openAPI = resolveOpenAPI(context);
        if (classElement.isPresent()) {
            final Schema schema = resolveSchema(openAPI, null, classElement.get(), context, null);
            schemaToValueMap(valueMap, schema);
        }
    }

    private void checkAllOf(ComposedSchema composedSchema) {
        if (composedSchema != null && composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
            if (composedSchema.getProperties() != null && !composedSchema.getProperties().isEmpty()) {
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
    }

    private Schema getSchemaDefinition(
            @Nullable String mediaType,
            OpenAPI openAPI,
            VisitorContext context,
            Element type,
            @Nullable Element definingElement) {
        // To break the recursion
        if (inProgressSchemas.contains(type.getSimpleName())) {
            return null;
        }
        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue = definingElement != null ? definingElement.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class) : null;
        if (schemaValue == null) {
            schemaValue = type.getDeclaredAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        }
        Schema schema;
        Map<String, Schema> schemas = resolveSchemas(openAPI);
        if (schemaValue != null) {
            String schemaName = schemaValue.get("name", String.class).orElse(computeDefaultSchemaName(definingElement, type));
            schema = schemas.get(schemaName);
            if (schema == null) {
                inProgressSchemas.add(schemaName);
                try {
                    schema = readSchema(schemaValue, openAPI, context, mediaType, type);
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
        } else {
            final boolean isBasicType = ClassUtils.isJavaBasicType(type.getName());
            final PrimitiveType primitiveType;
            if (isBasicType) {
                primitiveType = ClassUtils.forName(type.getName(), getClass().getClassLoader()).map(PrimitiveType::fromType).orElse(null);
            } else {
                primitiveType = null;
            }
            if (primitiveType != null) {
                return primitiveType.createProperty();
            } else {
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
                            Optional<ClassElement> superType = classElement != null ? classElement.getSuperType() : Optional.empty();
                            if (!superType.isPresent()) {
                                schema = new Schema();
                            } else {
                                schema = new ComposedSchema();
                                while (superType.isPresent()) {
                                    final ClassElement superElement = superType.get();
                                    String parentSchemaName = computeDefaultSchemaName(definingElement, superElement);
                                    if (schemas.get(parentSchemaName) != null
                                            || getSchemaDefinition(mediaType, openAPI, context, superElement, null) != null) {
                                        Schema parentSchema = new Schema();
                                        parentSchema.set$ref(schemaRef(parentSchemaName));
                                        ((ComposedSchema) schema).addAllOfItem(parentSchema);
                                    }
                                    superType = superElement.getSuperType();
                                }
                            }
                        } else {
                            schema = new Schema();
                        }
                        schema.setType("object");
                        schema.setName(schemaName);
                        schemas.put(schemaName, schema);

                        populateSchemaProperties(mediaType, openAPI, context, type, schema);
                        if (schema instanceof ComposedSchema) {
                            checkAllOf((ComposedSchema) schema);
                        }
                    }
                }
            }
        }
        if (schema != null) {
            Schema schemaRef = new Schema();
            schemaRef.set$ref(schemaRef(schema.getName()));
            return schemaRef;
        }
        return null;
    }

    /**
     * Reads schema.
     *
     * @param schemaValue annotation value
     * @param openAPI     The OpenApi
     * @param context     The VisitorContext
     * @param mediaType   The media type of schema
     * @param type        The element
     * @return New schema instance
     * @throws JsonProcessingException when Json parsing fails
     */
    protected Schema readSchema(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaValue, OpenAPI openAPI, VisitorContext context, @Nullable String mediaType,  @Nullable Element type) throws JsonProcessingException {
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
                List<Schema> schemaList = namesToSchemas(mediaType, openAPI, context, names);
                for (Schema s: schemaList) {
                    composedSchema.addAllOfItem(s);
                }
            }

            final Optional<String[]> anyOf = schemaValue.get("anyOf", String[].class);
            if (anyOf.isPresent() && anyOf.get().length > 0) {
                final String[] names = anyOf.get();
                List<Schema> schemaList = namesToSchemas(mediaType, openAPI, context, names);
                for (Schema s: schemaList) {
                    composedSchema.addAnyOfItem(s);
                }
            }

            final Optional<String[]> oneof = schemaValue.get("oneOf", String[].class);
            if (oneof.isPresent() && oneof.get().length > 0) {
                final String[] names = oneof.get();
                List<Schema> schemaList = namesToSchemas(mediaType, openAPI, context, names);
                for (Schema s: schemaList) {
                    composedSchema.addOneOfItem(s);
                }
            }

            schema.setType("object");
        }
        if (type instanceof EnumElement) {
            schema.setType("string");
            schema.setEnum(((EnumElement) type).values());
        } else if (schema instanceof ObjectSchema || composedSchema != null) {
            populateSchemaProperties(mediaType, openAPI, context, type, schema);
            checkAllOf(composedSchema);
        }
        return schema;
    }

    private List<Schema> namesToSchemas(@Nullable String mediaType, OpenAPI openAPI, VisitorContext context, String[] names) {
        return Arrays.stream(names).flatMap((Function<String, Stream<Schema>>) className -> {
                                        final Optional<ClassElement> classElement = context.getClassElement(className);
                                        if (classElement.isPresent()) {
                                            final Schema schemaDefinition = getSchemaDefinition(mediaType, openAPI, context, classElement.get(), null);
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
        final String metaAnnName = definingElement != null ? definingElement.getAnnotationNameByStereotype(io.swagger.v3.oas.annotations.media.Schema.class).orElse(null) : null;
        if (metaAnnName != null && !io.swagger.v3.oas.annotations.media.Schema.class.getName().equals(metaAnnName)) {
            return NameUtils.getSimpleName(metaAnnName);
        }
        if (type instanceof TypedElement) {
            ClassElement classElement = ((TypedElement) type).getType();
            if (classElement != null) {
                return computeNameWithGenerics(classElement);
            }

        }
        return type.getSimpleName();
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

            builder.append('<');
            while (i.hasNext()) {
                final ClassElement ce = i.next();
                builder.append(ce.getSimpleName());
                if (!computed.contains(ce.getName())) {
                    computeNameWithGenerics(ce, builder, computed);
                }
                if (i.hasNext()) {
                    builder.append(',');
                }
            }

            builder.append('>');
        }
    }

    private void populateSchemaProperties(String mediaType, OpenAPI openAPI, VisitorContext context, Element type, Schema schema) {
        ClassElement classElement = null;
        if (type instanceof ClassElement) {
            classElement = (ClassElement) type;
        } else if (type instanceof TypedElement) {
            classElement = ((TypedElement) type).getType();
        }

        if (classElement != null) {
            List<PropertyElement> beanProperties = classElement.getBeanProperties();
            processPropertyElements(mediaType, openAPI, context, type, schema, beanProperties);

            final List<FieldElement> publicFields = classElement.getFields(mods -> mods.contains(ElementModifier.PUBLIC) && mods.size() == 1);

            processPropertyElements(mediaType, openAPI, context, type, schema, publicFields);
        }
    }

    private void processPropertyElements(String mediaType, OpenAPI openAPI, VisitorContext context, Element type, Schema schema, List<? extends TypedElement> publicFields) {
        for (TypedElement publicField : publicFields) {
            if (publicField.isAnnotationPresent(JsonIgnore.class) || publicField.isAnnotationPresent(Hidden.class)) {
                continue;
            }

            if (publicField instanceof MemberElement && ((MemberElement) publicField).getDeclaringType().equals(type)) {

                Schema propertySchema = resolveSchema(openAPI, null, publicField.getType(), context, mediaType);

                processSchemaProperty(
                        context,
                        publicField,
                        publicField.getType(),
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
                "io.reactivex.Maybe"
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
                    map.remove("name");
                }
                normalizeEnumValues(map, CollectionUtils.mapOf("type", SecurityScheme.Type.class, "in", SecurityScheme.In.class));
                Optional<SecurityScheme> securityRequirement = toValue(map, context, SecurityScheme.class);
                securityRequirement.ifPresent(securityScheme -> {

                    try {
                        securityScheme.setIn(Enum.valueOf(SecurityScheme.In.class, map.get("in").toString().toUpperCase(Locale.ENGLISH)));
                    } catch (Exception e) {
                        // ignore
                    }
                    resolveComponents(openAPI).addSecuritySchemes(name, securityScheme);
                });
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
     *  custom_name:
     *    - custom_scope1
     *    - custom_scope2
     * @param r The value of {@link SecurityRequirement}.
     * @return converted object.
     */
    protected SecurityRequirement mapToSecurityRequirement(AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement> r) {
        String name = r.getRequiredValue("name", String.class);
        List<String> scopes = r.get("scopes", String[].class).map(Arrays::asList).orElse(Collections.EMPTY_LIST);
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }
}
