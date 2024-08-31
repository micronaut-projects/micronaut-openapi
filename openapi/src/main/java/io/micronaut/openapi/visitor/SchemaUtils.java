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

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.SimpleSchema;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PARSE_VALUE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PROPERTIES;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Some schema util methods.
 *
 * @since 4.5.0
 */
@Internal
public final class SchemaUtils {

    public static final String COMPONENTS_CALLBACKS_PREFIX = "#/components/callbacks/";
    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";

    public static final String TYPE_NULL = "null";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";

    public static final Schema<?> EMPTY_SCHEMA = new Schema<>();
    public static final Schema<?> EMPTY_ARRAY_SCHEMA = new ArraySchema();
    public static final Schema<?> EMPTY_BINARY_SCHEMA = new BinarySchema();
    public static final Schema<?> EMPTY_BOOLEAN_SCHEMA = new BooleanSchema();
    public static final Schema<?> EMPTY_BYTE_ARRAY_SCHEMA = new ByteArraySchema();
    public static final Schema<?> EMPTY_COMPOSED_SCHEMA = new ComposedSchema();
    public static final Schema<?> EMPTY_DATE_SCHEMA = new DateSchema();
    public static final Schema<?> EMPTY_DATE_TIME_SCHEMA = new DateTimeSchema();
    public static final Schema<?> EMPTY_EMAIL_SCHEMA = new EmailSchema();
    public static final Schema<?> EMPTY_FILE_SCHEMA = new FileSchema();
    public static final Schema<?> EMPTY_INTEGER_SCHEMA = new IntegerSchema();
    public static final Schema<?> EMPTY_JSON_SCHEMA = new JsonSchema();
    public static final Schema<?> EMPTY_MAP_SCHEMA = new MapSchema();
    public static final Schema<?> EMPTY_NUMBER_SCHEMA = new NumberSchema();
    public static final Schema<?> EMPTY_OBJECT_SCHEMA = new ObjectSchema();
    public static final Schema<?> EMPTY_PASSWORD_SCHEMA = new PasswordSchema();
    public static final Schema<?> EMPTY_STRING_SCHEMA = new StringSchema();
    public static final Schema<?> EMPTY_UUID_SCHEMA = new UUIDSchema();
    public static final Schema<?> EMPTY_SIMPLE_SCHEMA = new SimpleSchema();

    private static final List<Schema<?>> ALL_EMPTY_SCHEMAS = List.of(
        EMPTY_SCHEMA,
        EMPTY_ARRAY_SCHEMA,
        EMPTY_BINARY_SCHEMA,
        EMPTY_BOOLEAN_SCHEMA,
        EMPTY_BYTE_ARRAY_SCHEMA,
        EMPTY_COMPOSED_SCHEMA,
        EMPTY_DATE_SCHEMA,
        EMPTY_DATE_TIME_SCHEMA,
        EMPTY_EMAIL_SCHEMA,
        EMPTY_FILE_SCHEMA,
        EMPTY_INTEGER_SCHEMA,
        EMPTY_JSON_SCHEMA,
        EMPTY_MAP_SCHEMA,
        EMPTY_NUMBER_SCHEMA,
        EMPTY_OBJECT_SCHEMA,
        EMPTY_PASSWORD_SCHEMA,
        EMPTY_STRING_SCHEMA,
        EMPTY_UUID_SCHEMA,
        EMPTY_SIMPLE_SCHEMA
    );
    private static final String PREFIX_X = "x-";

    private SchemaUtils() {
    }

    public static boolean isEmptySchema(Schema<?> schema) {
        return ALL_EMPTY_SCHEMAS.contains(schema);
    }

    // Copy of io.swagger.v3.core.util.AnnotationsUtils.getExtensions
    public static void processExtensions(Map<String, Object> map, AnnotationValue<Extension> extension) {
        String extName = extension.stringValue(PROP_NAME).orElse(StringUtils.EMPTY_STRING);
        String decoratedName = prependIfMissing(extName, PREFIX_X);
        final String key = !extName.isEmpty() ? decoratedName : extName;
        for (var propAnn : extension.getAnnotations(PROP_PROPERTIES, ExtensionProperty.class)) {
            var propertyName = propAnn.getRequiredValue(PROP_NAME, String.class);
            var propertyValue = propAnn.getRequiredValue(String.class);
            JsonNode processedValue;
            final boolean propertyAsJson = propAnn.get(PROP_PARSE_VALUE, boolean.class, false);
            if (StringUtils.hasText(propertyName) && StringUtils.hasText(propertyValue)) {
                if (key.isEmpty()) {
                    decoratedName = prependIfMissing(propertyName, PREFIX_X);
                    if (propertyAsJson) {
                        try {
                            processedValue = Utils.getJsonMapper().readTree(propertyValue);
                            map.put(decoratedName, processedValue);
                        } catch (Exception e) {
                            map.put(decoratedName, propertyValue);
                        }
                    } else {
                        map.put(decoratedName, propertyValue);
                    }
                } else {
                    Object value = map.get(key);
                    if (!(value instanceof Map)) {
                        value = new LinkedHashMap<>();
                        map.put(key, value);
                    }
                    @SuppressWarnings("unchecked")
                    var mapValue = (Map<String, Object>) value;
                    if (propertyAsJson) {
                        try {
                            processedValue = Utils.getJsonMapper().readTree(propertyValue);
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

    public static Map<String, Schema> resolveSchemas(OpenAPI openAPI) {
        Components components = resolveComponents(openAPI);
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            schemas = new LinkedHashMap<>();
            components.setSchemas(schemas);
        }
        return schemas;
    }

    public static <T extends Schema> T setSpecVersion(T schema) {
        schema.specVersion(Utils.isOpenapi31() ? SpecVersion.V31 : SpecVersion.V30);
        return schema;
    }

    public static ArraySchema arraySchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        var arraySchema = new ArraySchema();
        setSpecVersion(arraySchema);
        arraySchema.items(schema);
        return arraySchema;
    }

    public static String schemaRef(String schemaName) {
        return COMPONENTS_SCHEMAS_REF + schemaName;
    }

    public static Operation getOperationOnPathItem(PathItem pathItem, HttpMethod httpMethod) {

        if (pathItem == null) {
            return null;
        }

        return switch (httpMethod) {
            case GET -> pathItem.getGet();
            case PUT -> pathItem.getPut();
            case POST -> pathItem.getPost();
            case DELETE -> pathItem.getDelete();
            case OPTIONS -> pathItem.getOptions();
            case HEAD -> pathItem.getHead();
            case PATCH -> pathItem.getPatch();
            case TRACE -> pathItem.getTrace();
            default -> null;
        };
    }

    public static void setOperationOnPathItem(PathItem pathItem, HttpMethod httpMethod, Operation operation) {
        if (pathItem == null) {
            return;
        }
        switch (httpMethod) {
            case GET -> pathItem.setGet(operation);
            case PUT -> pathItem.setPut(operation);
            case POST -> pathItem.setPost(operation);
            case DELETE -> pathItem.setDelete(operation);
            case OPTIONS -> pathItem.setOptions(operation);
            case HEAD -> pathItem.setHead(operation);
            case PATCH -> pathItem.setPatch(operation);
            case TRACE -> pathItem.setTrace(operation);
            default -> {
                // do nothing
            }
        }
    }

    public static Operation mergeOperations(Operation op1, Operation op2) {

        if (op1 == null) {
            return op2;
        }
        if (op2 == null) {
            return op1;
        }
        if (op1.equals(op2)) {
            return op1;
        }

        if (CollectionUtils.isEmpty(op1.getTags())) {
            op1.setTags(op2.getTags());
        } else if (CollectionUtils.isNotEmpty(op2.getTags())) {
            var tagsSet = new HashSet<>(op1.getTags());
            tagsSet.addAll(op2.getTags());
            var tags = new ArrayList<>(tagsSet);
            Collections.sort(tags);
            op1.setTags(tags);
        }

        if (StringUtils.isEmpty(op1.getSummary())) {
            op1.setSummary(op2.getSummary());
        }
        if (StringUtils.isEmpty(op1.getDescription())) {
            op1.setDescription(op2.getDescription());
        }
        if (op1.getExternalDocs() == null) {
            op1.setExternalDocs(op2.getExternalDocs());
        }
        if (op1.getDeprecated() == null) {
            op1.setDeprecated(op2.getDeprecated());
        }
        if (CollectionUtils.isEmpty(op1.getSecurity())) {
            op1.setSecurity(op2.getSecurity());
        } else if (CollectionUtils.isNotEmpty(op2.getSecurity())) {
            var securityRequirements = new HashSet<>(op1.getSecurity());
            securityRequirements.addAll(op2.getSecurity());
            op1.setSecurity(new ArrayList<>(securityRequirements));
        }
        if (CollectionUtils.isEmpty(op1.getExtensions())) {
            op1.setExtensions(op2.getExtensions());
        } else if (CollectionUtils.isNotEmpty(op2.getExtensions())) {
            op2.getExtensions().putAll(op1.getExtensions());
            op1.setExtensions(op2.getExtensions());
        }
        if (CollectionUtils.isEmpty(op1.getCallbacks())) {
            op1.setCallbacks(op2.getCallbacks());
        } else if (CollectionUtils.isNotEmpty(op2.getCallbacks())) {
            op2.getCallbacks().putAll(op1.getCallbacks());
            op1.setCallbacks(op2.getCallbacks());
        }
        if (CollectionUtils.isEmpty(op1.getResponses())) {
            op1.setResponses(op2.getResponses());
        } else if (CollectionUtils.isNotEmpty(op2.getResponses())) {
            for (Map.Entry<String, ApiResponse> entry1 : op1.getResponses().entrySet()) {
                ApiResponse ar2 = op2.getResponses().get(entry1.getKey());
                entry1.setValue(mergeApiResponse(entry1.getValue(), ar2));
            }
            op2.getResponses().putAll(op1.getResponses());
            op1.setResponses(op2.getResponses());
        }

        if (CollectionUtils.isEmpty(op1.getServers())) {
            op1.setServers(op2.getServers());
        } else if (CollectionUtils.isNotEmpty(op2.getServers())) {
            var serversSet = new HashSet<>(op1.getServers());
            serversSet.addAll(op2.getServers());
            op1.setServers(new ArrayList<>(serversSet));
        }

        mergeRequestBody(op1.getRequestBody(), op2.getRequestBody());

        if (CollectionUtils.isEmpty(op1.getParameters())) {
            op1.setParameters(op2.getParameters());
        } else if (CollectionUtils.isNotEmpty(op2.getParameters())) {

            for (Parameter p2 : op2.getParameters()) {
                Parameter existedParameter = null;
                int i = 0;
                for (Parameter p1 : op1.getParameters()) {
                    if (Objects.equals(p1.getName(), p2.getName())
                        && Objects.equals(p1.getIn(), p2.getIn())) {
                        existedParameter = p1;
                        break;
                    }
                    i++;
                }
                if (existedParameter == null) {
                    op1.addParametersItem(p2);
                } else {
                    op1.getParameters().set(i, mergeParameter(existedParameter, p2));
                }
            }

            var serversSet = new HashSet<>(op1.getParameters());
            serversSet.addAll(op2.getParameters());
            op1.setParameters(new ArrayList<>(serversSet));
        }

        return op1;
    }

    public static ApiResponse mergeApiResponse(ApiResponse ar1, ApiResponse ar2) {

        if (ar1 == null) {
            return ar2;
        }
        if (ar2 == null) {
            return null;
        }
        if (ar1.equals(ar2)) {
            return ar1;
        }

        if (ar1.getDescription() == null) {
            ar1.setDescription(ar2.getDescription());
        }
        if (ar1.get$ref() == null) {
            ar1.set$ref(ar2.get$ref());
        }
        if (CollectionUtils.isEmpty(ar1.getHeaders())) {
            ar1.setHeaders(ar2.getHeaders());
        } else if (CollectionUtils.isNotEmpty(ar2.getHeaders())) {
            for (Map.Entry<String, Header> entry1 : ar1.getHeaders().entrySet()) {
                Header h2 = ar2.getHeaders().get(entry1.getKey());
                entry1.setValue(mergeHeader(entry1.getValue(), h2));
            }
            ar2.getHeaders().putAll(ar1.getHeaders());
            ar1.setHeaders(ar2.getHeaders());
        }

        if (CollectionUtils.isEmpty(ar1.getLinks())) {
            ar1.setLinks(ar2.getLinks());
        } else if (CollectionUtils.isNotEmpty(ar2.getLinks())) {
            for (Map.Entry<String, Link> entry1 : ar1.getLinks().entrySet()) {
                Link l2 = ar2.getLinks().get(entry1.getKey());
                entry1.setValue(mergeLink(entry1.getValue(), l2));
            }
            ar2.getLinks().putAll(ar1.getLinks());
            ar1.setLinks(ar2.getLinks());
        }

        if (CollectionUtils.isEmpty(ar1.getExtensions())) {
            ar1.setExtensions(ar2.getExtensions());
        } else if (CollectionUtils.isNotEmpty(ar2.getExtensions())) {
            ar2.getExtensions().putAll(ar1.getExtensions());
            ar1.setExtensions(ar2.getExtensions());
        }
        ar1.setContent(mergeContent(ar1.getContent(), ar2.getContent()));

        return ar1;
    }

    public static Link mergeLink(Link l1, Link l2) {

        if (l1 == null) {
            return l2;
        }
        if (l2 == null) {
            return null;
        }
        if (l1.equals(l2)) {
            return l1;
        }

        if (l1.getDescription() == null) {
            l1.setDescription(l2.getDescription());
        }
        if (l1.getOperationRef() == null) {
            l1.setOperationRef(l2.getOperationRef());
        }
        if (l1.getOperationId() == null) {
            l1.setOperationId(l2.getOperationId());
        }
        if (l1.getRequestBody() == null) {
            l1.setRequestBody(l2.getRequestBody());
        }
        if (l1.get$ref() == null) {
            l1.set$ref(l2.get$ref());
        }
        if (CollectionUtils.isEmpty(l1.getParameters())) {
            l1.setParameters(l2.getParameters());
        } else if (CollectionUtils.isNotEmpty(l2.getParameters())) {
            l2.getParameters().putAll(l1.getParameters());
            l1.setParameters(l2.getParameters());
        }
        if (CollectionUtils.isEmpty(l1.getExtensions())) {
            l1.setExtensions(l2.getExtensions());
        } else if (CollectionUtils.isNotEmpty(l2.getExtensions())) {
            l2.getExtensions().putAll(l1.getExtensions());
            l1.setExtensions(l2.getExtensions());
        }

        return l1;
    }

    public static Header mergeHeader(Header h1, Header h2) {

        if (h1 == null) {
            return h2;
        }
        if (h2 == null) {
            return null;
        }
        if (h1.equals(h2)) {
            return h1;
        }

        if (h1.getDescription() == null) {
            h1.setDescription(h2.getDescription());
        }
        if (h1.getRequired() == null) {
            h1.setRequired(h2.getRequired());
        }
        if (h1.getDeprecated() == null) {
            h1.setDeprecated(h2.getDeprecated());
        }
        if (h1.get$ref() == null) {
            h1.set$ref(h2.get$ref());
        }
        if (h1.getStyle() == null) {
            h1.setStyle(h2.getStyle());
        }
        if (h1.getExplode() == null) {
            h1.setExplode(h2.getExplode());
        }
        if (h1.getExample() == null) {
            h2.setExample(h1.getExample());
        }

        h1.setContent(mergeContent(h1.getContent(), h2.getContent()));
        if (CollectionUtils.isEmpty(h1.getExtensions())) {
            h1.setExtensions(h2.getExtensions());
        } else if (CollectionUtils.isNotEmpty(h2.getExtensions())) {
            h2.getExtensions().putAll(h1.getExtensions());
            h1.setExtensions(h2.getExtensions());
        }
        h1.setSchema(mergeSchema(h1.getSchema(), h2.getSchema()));
        if (CollectionUtils.isEmpty(h1.getExamples())) {
            h1.setExamples(h2.getExamples());
        } else if (CollectionUtils.isNotEmpty(h2.getExamples())) {
            h1.getExamples().putAll(h2.getExamples());
        }

        return h1;
    }

    public static Parameter mergeParameter(Parameter p1, Parameter p2) {

        if (p1 == null) {
            return p2;
        }
        if (p2 == null) {
            return null;
        }
        if (p1.equals(p2)) {
            return p1;
        }

        if (p1.getDescription() == null) {
            p1.setDescription(p2.getDescription());
        }
        if (p1.getRequired() == null) {
            p1.setRequired(p2.getRequired());
        }
        if (p1.getDeprecated() == null) {
            p1.setDeprecated(p2.getDeprecated());
        }
        if (p1.getAllowEmptyValue() == null) {
            p1.setAllowEmptyValue(p2.getAllowEmptyValue());
        }
        if (p1.get$ref() == null) {
            p1.set$ref(p2.get$ref());
        }
        if (p1.getStyle() == null) {
            p1.setStyle(p2.getStyle());
        }
        if (p1.getExplode() == null) {
            p1.setExplode(p2.getExplode());
        }
        if (p1.getAllowReserved() == null) {
            p1.setAllowReserved(p2.getAllowReserved());
        }
        if (p1.getExample() == null) {
            p2.setExample(p1.getExample());
        }

        p1.setContent(mergeContent(p1.getContent(), p2.getContent()));
        if (CollectionUtils.isEmpty(p1.getExtensions())) {
            p1.setExtensions(p2.getExtensions());
        } else if (CollectionUtils.isNotEmpty(p2.getExtensions())) {
            p2.getExtensions().putAll(p1.getExtensions());
            p1.setExtensions(p2.getExtensions());
        }
        p1.setSchema(mergeSchema(p1.getSchema(), p2.getSchema()));
        if (CollectionUtils.isEmpty(p1.getExamples())) {
            p1.setExamples(p2.getExamples());
        } else if (CollectionUtils.isNotEmpty(p2.getExamples())) {
            p1.getExamples().putAll(p2.getExamples());
        }

        return p1;
    }

    public static RequestBody mergeRequestBody(RequestBody rb1, RequestBody rb2) {

        if (rb1 == null) {
            return rb2;
        }
        if (rb2 == null) {
            return rb1;
        }
        if (rb1.equals(rb2)) {
            return rb1;
        }

        if (StringUtils.isEmpty(rb1.getDescription())) {
            rb1.setDescription(rb2.getDescription());
        }
        if (CollectionUtils.isEmpty(rb1.getExtensions())) {
            rb1.setExtensions(rb2.getExtensions());
        } else if (CollectionUtils.isNotEmpty(rb2.getExtensions())) {
            rb2.getExtensions().putAll(rb1.getExtensions());
            rb1.setExtensions(rb2.getExtensions());
        }
        if (rb1.getRequired() == null) {
            rb1.setRequired(rb2.getRequired());
        }
        if (rb1.get$ref() == null) {
            rb1.set$ref(rb2.get$ref());
        }
        rb1.setContent(mergeContent(rb1.getContent(), rb2.getContent()));

        return rb1;
    }

    public static Content mergeContent(Content c1, Content c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return null;
        }
        if (c1.equals(c2)) {
            return c1;
        }
        c2.forEach(c1::putIfAbsent);
        for (Map.Entry<String, MediaType> entry : c1.entrySet()) {
            MediaType mt1 = entry.getValue();
            MediaType mt2 = c2.get(entry.getKey());
            if (mt2 == null) {
                continue;
            }
            // this can be with different value in @Version annotation
            mt1.setSchema(mergeSchema(mt1.getSchema(), mt2.getSchema()));
            if (CollectionUtils.isEmpty(mt1.getEncoding())) {
                mt1.setEncoding(mt2.getEncoding());
            } else if (CollectionUtils.isNotEmpty(mt2.getEncoding())) {
                mt1.getEncoding().putAll(mt2.getEncoding());
            }
            if (CollectionUtils.isEmpty(mt1.getExtensions())) {
                mt1.setExtensions(mt2.getExtensions());
            } else if (CollectionUtils.isNotEmpty(mt2.getExtensions())) {
                mt2.getExtensions().putAll(mt1.getExtensions());
                mt1.setExtensions(mt2.getExtensions());
            }
            if (mt1.getExample() == null) {
                mt1.setExample(mt2.getExample());
            }
            if (CollectionUtils.isEmpty(mt1.getExamples())) {
                mt1.setExamples(mt2.getExamples());
            } else if (CollectionUtils.isNotEmpty(mt2.getExamples())) {
                mt1.getExamples().putAll(mt2.getExamples());
            }
        }
        return c1;
    }

    public static Schema<?> mergeSchema(Schema<?> s1, Schema<?> s2) {
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return null;
        }
        if (s1.equals(s2)) {
            return s1;
        }
        if (s1 instanceof ComposedSchema && CollectionUtils.isNotEmpty(s1.getOneOf())) {
            s1.addOneOfItem(s2);
            return s1;
        }
        Schema<?> finalSchema = setSpecVersion(new ComposedSchema());
        finalSchema.addOneOfItem(s1);
        finalSchema.addOneOfItem(s2);
        return finalSchema;
    }

    public static Schema<?> appendSchema(Schema<?> s1, Schema<?> s2) {
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return null;
        }
        if (s1.equals(s2)) {
            return s1;
        }

        if ((s1.getType() == null || TYPE_OBJECT.equals(s1.getType())) && s2.getType() != null && !TYPE_OBJECT.equals(s2.getType())) {
            s1.setType(s2.getType());
            if (s1.getFormat() == null && s2.getFormat() != null) {
                s1.setFormat(s2.getFormat());
            }
        }
        if (s1.getName() == null && s2.getName() != null) {
            s1.setName(s2.getName());
        }
        if (s1.getTitle() == null && s2.getTitle() != null) {
            s1.setTitle(s2.getTitle());
        }
        if (s1.getMultipleOf() == null && s2.getMultipleOf() != null) {
            s1.setMultipleOf(s2.getMultipleOf());
        }
        if (s1.getMaximum() == null && s2.getMaximum() != null) {
            s1.setMaximum(s2.getMaximum());
        }
        if (s1.getExclusiveMaximum() == null && s2.getExclusiveMaximum() != null) {
            s1.setExclusiveMaximum(s2.getExclusiveMaximum());
        }
        if (s1.getMinimum() == null && s2.getMinimum() != null) {
            s1.setMinimum(s2.getMinimum());
        }
        if (s1.getExclusiveMinimum() == null && s2.getExclusiveMinimum() != null) {
            s1.setExclusiveMinimum(s2.getExclusiveMinimum());
        }
        if (s1.getMaxLength() == null && s2.getMaxLength() != null) {
            s1.setMaxLength(s2.getMaxLength());
        }
        if (s1.getMinLength() == null && s2.getMinLength() != null) {
            s1.setMinLength(s2.getMinLength());
        }
        if (s1.getPattern() == null && s2.getPattern() != null) {
            s1.setPattern(s2.getPattern());
        }
        if (s1.getMaxItems() == null && s2.getMaxItems() != null) {
            s1.setMaxItems(s2.getMaxItems());
        }
        if (s1.getMinItems() == null && s2.getMinItems() != null) {
            s1.setMinItems(s2.getMinItems());
        }
        if (s1.getUniqueItems() == null && s2.getUniqueItems() != null) {
            s1.setUniqueItems(s2.getUniqueItems());
        }
        if (s1.getMaxProperties() == null && s2.getMaxProperties() != null) {
            s1.setMaxProperties(s2.getMaxProperties());
        }
        if (s1.getMinProperties() == null && s2.getMinProperties() != null) {
            s1.setMinProperties(s2.getMinProperties());
        }
        if (s1.getRequired() == null && s2.getRequired() != null) {
            s1.setRequired(s2.getRequired());
        }
        if (s1.getType() == null && s2.getType() != null) {
            s1.setType(s2.getType());
        }
        if (s1.getNot() == null && s2.getNot() != null) {
            s1.setNot(s2.getNot());
        }
        if (s1.getProperties() == null && s2.getProperties() != null) {
            s1.setProperties(s2.getProperties());
        }
        if (s1.getAdditionalProperties() == null && s2.getAdditionalProperties() != null) {
            s1.setAdditionalProperties(s2.getAdditionalProperties());
        }
        if (s1.getDescription() == null && s2.getDescription() != null) {
            s1.setDescription(s2.getDescription());
        }
        if (s1.get$ref() == null && s2.get$ref() != null) {
            s1.set$ref(s2.get$ref());
        }
        if (s1.getNullable() == null && s2.getNullable() != null) {
            s1.setNullable(s2.getNullable());
        }
        if (s1.getReadOnly() == null && s2.getReadOnly() != null) {
            s1.setReadOnly(s2.getReadOnly());
        }
        if (s1.getWriteOnly() == null && s2.getWriteOnly() != null) {
            s1.setWriteOnly(s2.getWriteOnly());
        }
        if (s1.getExample() == null && s2.getExample() != null) {
            s1.setExample(s2.getExample());
        }
        if (s1.getExternalDocs() == null && s2.getExternalDocs() != null) {
            s1.setExternalDocs(s2.getExternalDocs());
        }
        if (s1.getDeprecated() == null && s2.getDeprecated() != null) {
            s1.setDeprecated(s2.getDeprecated());
        }
        if (s1.getXml() == null && s2.getXml() != null) {
            s1.setXml(s2.getXml());
        }
        if (s1.getExtensions() == null && s2.getExtensions() != null) {
            s1.setExtensions(s2.getExtensions());
        }
        if (s1.getDiscriminator() == null && s2.getDiscriminator() != null) {
            s1.setDiscriminator(s2.getDiscriminator());
        }
        if (s1.getPrefixItems() == null && s2.getPrefixItems() != null) {
            s1.setPrefixItems(s2.getPrefixItems());
        }
        if (s1.getElse() == null && s2.getElse() != null) {
            s1.setElse(s2.getElse());
        }
        if (s1.getAnyOf() == null && s2.getAnyOf() != null) {
            s1.setAnyOf(s2.getAnyOf());
        }
        if (s1.getOneOf() == null && s2.getOneOf() != null) {
            s1.setOneOf(s2.getOneOf());
        }
        if (s1.getItems() == null && s2.getItems() != null) {
            s1.setItems(s2.getItems());
        }
        if (s1.getTypes() == null && s2.getTypes() != null) {
            s1.setTypes(s2.getTypes());
        }
        if (s1.getPatternProperties() == null && s2.getPatternProperties() != null) {
            s1.setPatternProperties(s2.getPatternProperties());
        }
        if (s1.getExclusiveMaximumValue() == null && s2.getExclusiveMaximumValue() != null) {
            s1.setExclusiveMaximumValue(s2.getExclusiveMaximumValue());
        }
        if (s1.getExclusiveMinimumValue() == null && s2.getExclusiveMinimumValue() != null) {
            s1.setExclusiveMinimumValue(s2.getExclusiveMinimumValue());
        }
        if (s1.getContains() == null && s2.getContains() != null) {
            s1.setContains(s2.getContains());
        }
        if (s1.get$id() == null && s2.get$id() != null) {
            s1.set$id(s2.get$id());
        }
        if (s1.get$schema() == null && s2.get$schema() != null) {
            s1.set$schema(s2.get$schema());
        }
        if (s1.get$anchor() == null && s2.get$anchor() != null) {
            s1.set$anchor(s2.get$anchor());
        }
        if (s1.get$vocabulary() == null && s2.get$vocabulary() != null) {
            s1.set$vocabulary(s2.get$vocabulary());
        }
        if (s1.get$dynamicAnchor() == null && s2.get$dynamicAnchor() != null) {
            s1.set$dynamicAnchor(s2.get$dynamicAnchor());
        }
        if (s1.getContentEncoding() == null && s2.getContentEncoding() != null) {
            s1.setContentEncoding(s2.getContentEncoding());
        }
        if (s1.getContentMediaType() == null && s2.getContentMediaType() != null) {
            s1.setContentMediaType(s2.getContentMediaType());
        }
        if (s1.getContentSchema() == null && s2.getContentSchema() != null) {
            s1.setContentSchema(s2.getContentSchema());
        }
        if (s1.getPropertyNames() == null && s2.getPropertyNames() != null) {
            s1.setPropertyNames(s2.getPropertyNames());
        }
        if (s1.getUnevaluatedProperties() == null && s2.getUnevaluatedProperties() != null) {
            s1.setUnevaluatedProperties(s2.getUnevaluatedProperties());
        }
        if (s1.getMaxContains() == null && s2.getMaxContains() != null) {
            s1.setMaxContains(s2.getMaxContains());
        }
        if (s1.getMinContains() == null && s2.getMinContains() != null) {
            s1.setMinContains(s2.getMinContains());
        }
        if (s1.getAdditionalItems() == null && s2.getAdditionalItems() != null) {
            s1.setAdditionalItems(s2.getAdditionalItems());
        }
        if (s1.getUnevaluatedItems() == null && s2.getUnevaluatedItems() != null) {
            s1.setUnevaluatedItems(s2.getUnevaluatedItems());
        }
        if (s1.getIf() == null && s2.getIf() != null) {
            s1.setIf(s2.getIf());
        }
        if (s1.getThen() == null && s2.getThen() != null) {
            s1.setThen(s2.getThen());
        }
        if (s1.getDependentSchemas() == null && s2.getDependentSchemas() != null) {
            s1.setDependentSchemas(s2.getDependentSchemas());
        }
        if (s1.getDependentRequired() == null && s2.getDependentRequired() != null) {
            s1.setDependentRequired(s2.getDependentRequired());
        }
        if (s1.get$comment() == null && s2.get$comment() != null) {
            s1.set$comment(s2.get$comment());
        }
        if (s1.getExamples() == null && s2.getExamples() != null) {
            s1.setExamples((List) s2.getExamples());
        }
        if (s1.getBooleanSchemaValue() == null && s2.getBooleanSchemaValue() != null) {
            s1.setBooleanSchemaValue(s2.getBooleanSchemaValue());
        }
        if (s1.getJsonSchema() == null && s2.getJsonSchema() != null) {
            s1.setJsonSchema(s2.getJsonSchema());
        }
        if (s1.getJsonSchemaImpl() == null && s2.getJsonSchemaImpl() != null) {
            s1.setJsonSchemaImpl(s2.getJsonSchemaImpl());
        }
        return s1;
    }

    /**
     * Copy information from one {@link OpenAPI} object to another.
     *
     * @param to The {@link OpenAPI} object to copy to
     * @param from The {@link OpenAPI} object to copy from
     */
    public static void copyOpenApi(OpenAPI to, OpenAPI from) {
        if (to == null || from == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(from.getTags())) {
            from.getTags().forEach(to::addTagsItem);
        }
        if (CollectionUtils.isNotEmpty(from.getServers())) {
            from.getServers().forEach(to::addServersItem);
        }
        if (CollectionUtils.isNotEmpty(from.getSecurity())) {
            from.getSecurity().forEach(to::addSecurityItem);
        }
        if (CollectionUtils.isNotEmpty(from.getPaths())) {
            from.getPaths().forEach(to::path);
        }
        if (from.getExternalDocs() != null) {
            to.setExternalDocs(from.getExternalDocs());
        }
        if (CollectionUtils.isNotEmpty(from.getExtensions())) {
            from.getExtensions().forEach(to::addExtension);
        }

        if (from.getComponents() != null) {

            var components = from.getComponents();

            Map<String, Schema> schemas = components.getSchemas();
            if (CollectionUtils.isNotEmpty(schemas)) {
                schemas.forEach((k, v) -> {
                    if (v.getName() == null) {
                        v.setName(k);
                    }
                    to.schema(k, v);
                });
            }

            var securitySchemes = components.getSecuritySchemes();
            if (CollectionUtils.isNotEmpty(securitySchemes)) {
                securitySchemes.forEach(to::schemaRequirement);
            }
            var links = components.getLinks();
            if (CollectionUtils.isNotEmpty(links)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().links(links);
            }
            var callbacks = components.getCallbacks();
            if (CollectionUtils.isNotEmpty(callbacks)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().callbacks(callbacks);
            }
            var headers = components.getHeaders();
            if (CollectionUtils.isNotEmpty(headers)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().headers(headers);
            }
            var parameters = components.getParameters();
            if (CollectionUtils.isNotEmpty(parameters)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().parameters(parameters);
            }
            var responses = components.getResponses();
            if (CollectionUtils.isNotEmpty(responses)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().responses(responses);
            }
            var requestBodies = components.getRequestBodies();
            if (CollectionUtils.isNotEmpty(requestBodies)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().requestBodies(requestBodies);
            }
            var extensions = components.getExtensions();
            if (CollectionUtils.isNotEmpty(extensions)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().extensions(extensions);
            }
            var pathItems = components.getPathItems();
            if (CollectionUtils.isNotEmpty(extensions)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().pathItems(pathItems);
            }
            var examples = components.getExamples();
            if (CollectionUtils.isNotEmpty(examples)) {
                if (to.getComponents() == null) {
                    to.setComponents(new Components());
                }
                to.getComponents().examples(examples);
            }

        }
    }

    public static boolean isIgnoredHeader(String headerName) {
        // Header parameter named "Authorization" are ignored. Use the `securitySchemes` and `security` sections instead to define authorization
        // Header parameter named "Content-Type" are ignored. The values for the "Content-Type" header are defined by `request.body.content.<media-type>`
        // Header parameter named "Accept" are ignored. The values for the "Accept" header are defined by `responses.<code>.content.<media-type>`
        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)
            || HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(headerName)
            || HttpHeaders.ACCEPT.equalsIgnoreCase(headerName);
    }

    public static Schema setNullable(Schema<?> schema) {
        if (Utils.isOpenapi31()) {
            schema.addType(TYPE_NULL);
            schema.addType(schema.getType() != null ? schema.getType() : TYPE_OBJECT);
        } else {
            schema.setNullable(true);
        }
        return schema;
    }

    public static String getType(Schema<?> schema) {
        return getType(schema.getType(), schema.getTypes());
    }

    public static String getType(String type, Collection<String> types) {
        if (type != null) {
            return type;
        }
        if (Utils.isOpenapi31() && CollectionUtils.isNotEmpty(types)) {
            for (var t : types) {
                if (!t.equals(TYPE_NULL)) {
                    return t;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void setAllowableValues(Schema schema, String[] allowableValues, Element element, String elType, String elFormat, VisitorContext context) {
        if (ArrayUtils.isEmpty(allowableValues)) {
            return;
        }
        for (String allowableValue : allowableValues) {
            if (schema.getEnum() != null && schema.getEnum().contains(allowableValue)) {
                continue;
            }
            try {
                schema.addEnumItemObject(ConvertUtils.normalizeValue(allowableValue, elType, elFormat, context));
            } catch (IOException e) {
                warn("Can't convert " + allowableValue + " to " + elType + ", format: " + elFormat + ": " + e.getMessage(), context, element);
                schema.addEnumItemObject(allowableValue);
            }
        }
    }

    public static Schema<?> getSchemaByRef(Schema<?> schema, OpenAPI openApi) {
        return getSchemaByRef(schema.get$ref(), openApi);
    }

    public static Schema<?> getSchemaByRef(String schemaRef, OpenAPI openApi) {
        if (StringUtils.isEmpty(schemaRef)) {
            return null;
        }
        return resolveSchemas(openApi).get(schemaRef.substring(COMPONENTS_SCHEMAS_PREFIX.length()));
    }
}
