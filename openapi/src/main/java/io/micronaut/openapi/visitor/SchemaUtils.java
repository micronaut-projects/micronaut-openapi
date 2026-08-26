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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArbitrarySchema;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PARSE_VALUE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PROPERTIES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REQUIRED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REQUIRED_MODE;
import static io.micronaut.openapi.visitor.Utils.isOpenapi31;
import static io.micronaut.openapi.visitor.Utils.resolveTags;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Some schema util methods.
 *
 * @since 4.5.0
 */
@Internal
public final class SchemaUtils {

    public static final String PREFIX_X = "x-";

    public static final String COMPONENTS_CALLBACKS_PREFIX = "#/components/callbacks/";
    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";

    public static final String TYPE_NULL = "null";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";

    public static final String FORMAT_BINARY = "binary";
    public static final String FORMAT_BYTE = "byte";

    public static final Schema<?> EMPTY_SCHEMA = new Schema<>();
    public static final Schema<?> EMPTY_ARBITRARY_SCHEMA = new ArbitrarySchema();
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
    public static final Schema<?> EMPTY_OBJECT_TYPE_SCHEMA = new Schema<>().type(TYPE_OBJECT);
    public static final Schema<?> EMPTY_PASSWORD_SCHEMA = new PasswordSchema();
    public static final Schema<?> EMPTY_STRING_SCHEMA = new StringSchema();
    public static final Schema<?> EMPTY_UUID_SCHEMA = new UUIDSchema();

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
        EMPTY_OBJECT_TYPE_SCHEMA,
        EMPTY_PASSWORD_SCHEMA,
        EMPTY_STRING_SCHEMA,
        EMPTY_UUID_SCHEMA,
        EMPTY_ARBITRARY_SCHEMA
    );

    private SchemaUtils() {
    }

    public static boolean isArraySchema(Schema<?> schema, OpenAPI openApi) {
        if (schema == null) {
            return false;
        }
        if (schema instanceof ArraySchema || TYPE_ARRAY.equals(schema.getType())) {
            return true;
        }
        if (schema.get$ref() == null) {
            return false;
        }
        var refSchema = getSchemaByRef(schema, openApi);
        return isArraySchema(refSchema, openApi);
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

    public static Map<String, Schema> resolveSchemas(OpenAPI openApi) {
        Components components = resolveComponents(openApi);
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            schemas = new LinkedHashMap<>();
            components.setSchemas(schemas);
        }
        return schemas;
    }

    public static <T extends Schema> T setSpecVersion(T schema) {
        schema.specVersion(isOpenapi31() ? SpecVersion.V31 : SpecVersion.V30);
        return schema;
    }

    public static Schema createSchema() {
        return PrimitiveType.OBJECT.createProperty(isOpenapi31());
    }

    public static ComposedSchema createComposedSchema() {
        return setSpecVersion(new ComposedSchema());
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
        if (isEquals(s1, s2)) {
            return s1;
        }
        if (s1 instanceof ComposedSchema && CollectionUtils.isNotEmpty(s1.getOneOf())) {
            s1.addOneOfItem(s2);
            return s1;
        }
        Schema<?> finalSchema = createComposedSchema();
        finalSchema.addOneOfItem(s1);
        finalSchema.addOneOfItem(s2);
        return finalSchema;
    }

    public static Schema<?> appendSchema(Schema<?> s1, Schema<?> s2) {
        return appendSchema(s1, s2, true, false);
    }

    public static Schema<?> appendSchema(Schema<?> s1, Schema<?> s2, boolean withBlocks, boolean withErase) {
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return null;
        }
        if (isEquals(s1, s2)) {
            return s1;
        }

        if ((s1.getType() == null || TYPE_OBJECT.equals(s1.getType())) && s2.getType() != null && !TYPE_OBJECT.equals(s2.getType())) {
            s1.setType(s2.getType());
            if (withErase) {
                s2.setType(null);
            }
        }
        if (s1.getFormat() == null && s2.getFormat() != null) {
            s1.setFormat(s2.getFormat());
            if (withErase) {
                s2.setFormat(null);
            }
        }
        if (s1.getName() == null && s2.getName() != null) {
            s1.setName(s2.getName());
            if (withErase) {
                s2.setName(null);
            }
        }
        if (s1.getTitle() == null && s2.getTitle() != null) {
            s1.setTitle(s2.getTitle());
            if (withErase) {
                s2.setTitle(null);
            }
        }
        if (s1.getMultipleOf() == null && s2.getMultipleOf() != null) {
            s1.setMultipleOf(s2.getMultipleOf());
            if (withErase) {
                s2.setMultipleOf(null);
            }
        }
        if (s1.getMaximum() == null && s2.getMaximum() != null) {
            s1.setMaximum(s2.getMaximum());
            if (withErase) {
                s2.setMaximum(null);
            }
        }
        if (s1.getExclusiveMaximum() == null && s2.getExclusiveMaximum() != null) {
            s1.setExclusiveMaximum(s2.getExclusiveMaximum());
            if (withErase) {
                s2.setExclusiveMaximum(null);
            }
        }
        if (s1.getMinimum() == null && s2.getMinimum() != null) {
            s1.setMinimum(s2.getMinimum());
            if (withErase) {
                s2.setMinimum(null);
            }
        }
        if (s1.getExclusiveMinimum() == null && s2.getExclusiveMinimum() != null) {
            s1.setExclusiveMinimum(s2.getExclusiveMinimum());
            if (withErase) {
                s2.setExclusiveMinimum(null);
            }
        }
        if (s1.getMaxLength() == null && s2.getMaxLength() != null) {
            s1.setMaxLength(s2.getMaxLength());
            if (withErase) {
                s2.setMaxLength(null);
            }
        }
        if (s1.getMinLength() == null && s2.getMinLength() != null) {
            s1.setMinLength(s2.getMinLength());
            if (withErase) {
                s2.setMinLength(null);
            }
        }
        if (s1.getPattern() == null && s2.getPattern() != null) {
            s1.setPattern(s2.getPattern());
            if (withErase) {
                s2.setPattern(null);
            }
        }
        if (s1.getMaxItems() == null && s2.getMaxItems() != null) {
            s1.setMaxItems(s2.getMaxItems());
            if (withErase) {
                s2.setMaxItems(null);
            }
        }
        if (s1.getMinItems() == null && s2.getMinItems() != null) {
            s1.setMinItems(s2.getMinItems());
            if (withErase) {
                s2.setMinItems(null);
            }
        }
        if (s1.getUniqueItems() == null && s2.getUniqueItems() != null) {
            s1.setUniqueItems(s2.getUniqueItems());
            if (withErase) {
                s2.setUniqueItems(null);
            }
        }
        if (s1.getMaxProperties() == null && s2.getMaxProperties() != null) {
            s1.setMaxProperties(s2.getMaxProperties());
            if (withErase) {
                s2.setMaxProperties(null);
            }
        }
        if (s1.getMinProperties() == null && s2.getMinProperties() != null) {
            s1.setMinProperties(s2.getMinProperties());
            if (withErase) {
                s2.setMinProperties(null);
            }
        }
        if (s1.getRequired() == null && s2.getRequired() != null) {
            s1.setRequired(s2.getRequired());
            if (withErase) {
                s2.setRequired(null);
            }
        }
        if (s1.getType() == null && s2.getType() != null) {
            s1.setType(s2.getType());
            if (withErase) {
                s2.setType(null);
            }
        }
        if (s1.getNot() == null && s2.getNot() != null) {
            s1.setNot(s2.getNot());
            if (withErase) {
                s2.setNot(null);
            }
        }
        if (s1.getProperties() == null && s2.getProperties() != null) {
            s1.setProperties(s2.getProperties());
            if (withErase) {
                s2.setProperties(null);
            }
        }
        if (s1.getAdditionalProperties() == null && s2.getAdditionalProperties() != null) {
            s1.setAdditionalProperties(s2.getAdditionalProperties());
            if (withErase) {
                s2.setAdditionalProperties(null);
            }
        }
        if (s1.getDescription() == null && s2.getDescription() != null) {
            s1.setDescription(s2.getDescription());
            if (withErase) {
                s2.setDescription(null);
            }
        }
        if (s1.get$ref() == null && s2.get$ref() != null) {
            s1.set$ref(s2.get$ref());
            if (withErase) {
                s2.set$ref(null);
            }
        }
        if (s1.getNullable() == null && s2.getNullable() != null) {
            s1.setNullable(s2.getNullable());
            if (withErase) {
                s2.setNullable(null);
            }
        }
        if (s1.getReadOnly() == null && s2.getReadOnly() != null) {
            s1.setReadOnly(s2.getReadOnly());
            if (withErase) {
                s2.setReadOnly(null);
            }
        }
        if (s1.getWriteOnly() == null && s2.getWriteOnly() != null) {
            s1.setWriteOnly(s2.getWriteOnly());
            if (withErase) {
                s2.setWriteOnly(null);
            }
        }
        if (!s1.getExampleSetFlag() && s2.getExampleSetFlag()) {
            s1.setExampleSetFlag(s2.getExampleSetFlag());
            s1.setExample(s2.getExample());
            if (withErase) {
                s2.setExample(null);
                s2.setExampleSetFlag(false);
            }
        }
        if (s1.getExample() == null && s2.getExample() != null) {
            s1.setExample(s2.getExample());
            if (withErase) {
                s2.setExample(null);
            }
        }
        if (s1.getExternalDocs() == null && s2.getExternalDocs() != null) {
            s1.setExternalDocs(s2.getExternalDocs());
            if (withErase) {
                s2.setExternalDocs(null);
            }
        }
        if (s1.getDeprecated() == null && s2.getDeprecated() != null) {
            s1.setDeprecated(s2.getDeprecated());
            if (withErase) {
                s2.setDeprecated(null);
            }
        }
        if (s1.getXml() == null && s2.getXml() != null) {
            s1.setXml(s2.getXml());
            if (withErase) {
                s2.setXml(null);
            }
        }
        if (s1.getExtensions() == null && s2.getExtensions() != null) {
            s1.setExtensions(s2.getExtensions());
            if (withErase) {
                s2.setExtensions(null);
            }
        }
        if (s1.getDiscriminator() == null && s2.getDiscriminator() != null) {
            s1.setDiscriminator(s2.getDiscriminator());
            if (withErase) {
                s2.setDiscriminator(null);
            }
        }
        if (s1.getPrefixItems() == null && s2.getPrefixItems() != null) {
            s1.setPrefixItems(s2.getPrefixItems());
            if (withErase) {
                s2.setPrefixItems(null);
            }
        }
        if (s1.getElse() == null && s2.getElse() != null) {
            s1.setElse(s2.getElse());
            if (withErase) {
                s2.setElse(null);
            }
        }
        if (withBlocks) {
            if (s1.getAnyOf() == null && s2.getAnyOf() != null) {
                s1.setAnyOf(s2.getAnyOf());
            }
            if (s1.getOneOf() == null && s2.getOneOf() != null) {
                s1.setOneOf(s2.getOneOf());
            }
        }
        if (s1.getItems() == null && s2.getItems() != null) {
            s1.setItems(s2.getItems());
            if (withErase) {
                s2.setItems(null);
            }
        }
        if (s1.getTypes() == null && s2.getTypes() != null) {
            s1.setTypes(s2.getTypes());
            if (withErase) {
                s2.setTypes(null);
            }
        }

        if (s1.getPatternProperties() == null && s2.getPatternProperties() != null) {
            s1.setPatternProperties(s2.getPatternProperties());
            if (withErase) {
                s2.setPatternProperties(null);
            }
        }
        if (s1.getExclusiveMaximumValue() == null && s2.getExclusiveMaximumValue() != null) {
            s1.setExclusiveMaximumValue(s2.getExclusiveMaximumValue());
            if (withErase) {
                s2.setExclusiveMaximumValue(null);
            }
        }
        if (s1.getExclusiveMinimumValue() == null && s2.getExclusiveMinimumValue() != null) {
            s1.setExclusiveMinimumValue(s2.getExclusiveMinimumValue());
            if (withErase) {
                s2.setExclusiveMinimumValue(null);
            }
        }
        if (s1.getContains() == null && s2.getContains() != null) {
            s1.setContains(s2.getContains());
            if (withErase) {
                s2.setContains(null);
            }
        }
        if (s1.get$id() == null && s2.get$id() != null) {
            s1.set$id(s2.get$id());
            if (withErase) {
                s2.set$id(null);
            }
        }
        if (s1.get$schema() == null && s2.get$schema() != null) {
            s1.set$schema(s2.get$schema());
            if (withErase) {
                s2.set$schema(null);
            }
        }
        if (s1.get$anchor() == null && s2.get$anchor() != null) {
            s1.set$anchor(s2.get$anchor());
            if (withErase) {
                s2.set$anchor(null);
            }
        }
        if (s1.get$vocabulary() == null && s2.get$vocabulary() != null) {
            s1.set$vocabulary(s2.get$vocabulary());
            if (withErase) {
                s2.set$vocabulary(null);
            }
        }
        if (s1.get$dynamicAnchor() == null && s2.get$dynamicAnchor() != null) {
            s1.set$dynamicAnchor(s2.get$dynamicAnchor());
            if (withErase) {
                s2.set$dynamicAnchor(null);
            }
        }
        if (s1.get$dynamicRef() == null && s2.get$dynamicRef() != null) {
            s1.set$dynamicRef(s2.get$dynamicRef());
            if (withErase) {
                s2.set$dynamicRef(null);
            }
        }
        if (s1.getContentEncoding() == null && s2.getContentEncoding() != null) {
            s1.setContentEncoding(s2.getContentEncoding());
            if (withErase) {
                s2.setContentEncoding(null);
            }
        }
        if (s1.getContentMediaType() == null && s2.getContentMediaType() != null) {
            s1.setContentMediaType(s2.getContentMediaType());
            if (withErase) {
                s2.setContentMediaType(null);
            }
        }
        if (s1.getContentSchema() == null && s2.getContentSchema() != null) {
            s1.setContentSchema(s2.getContentSchema());
            if (withErase) {
                s2.setContentSchema(null);
            }
        }
        if (s1.getPropertyNames() == null && s2.getPropertyNames() != null) {
            s1.setPropertyNames(s2.getPropertyNames());
            if (withErase) {
                s2.setPropertyNames(null);
            }
        }
        if (s1.getUnevaluatedProperties() == null && s2.getUnevaluatedProperties() != null) {
            s1.setUnevaluatedProperties(s2.getUnevaluatedProperties());
            if (withErase) {
                s2.setUnevaluatedProperties(null);
            }
        }
        if (s1.getMaxContains() == null && s2.getMaxContains() != null) {
            s1.setMaxContains(s2.getMaxContains());
            if (withErase) {
                s2.setMaxContains(null);
            }
        }
        if (s1.getMinContains() == null && s2.getMinContains() != null) {
            s1.setMinContains(s2.getMinContains());
            if (withErase) {
                s2.setMinContains(null);
            }
        }
        if (s1.getAdditionalItems() == null && s2.getAdditionalItems() != null) {
            s1.setAdditionalItems(s2.getAdditionalItems());
            if (withErase) {
                s2.setAdditionalItems(null);
            }
        }
        if (s1.getUnevaluatedItems() == null && s2.getUnevaluatedItems() != null) {
            s1.setUnevaluatedItems(s2.getUnevaluatedItems());
            if (withErase) {
                s2.setUnevaluatedItems(null);
            }
        }
        if (s1.getIf() == null && s2.getIf() != null) {
            s1.setIf(s2.getIf());
            if (withErase) {
                s2.setIf(null);
            }
        }
        if (s1.getThen() == null && s2.getThen() != null) {
            s1.setThen(s2.getThen());
            if (withErase) {
                s2.setThen(null);
            }
        }
        if (s1.getDependentSchemas() == null && s2.getDependentSchemas() != null) {
            s1.setDependentSchemas(s2.getDependentSchemas());
            if (withErase) {
                s2.setDependentSchemas(null);
            }
        }
        if (s1.getDependentRequired() == null && s2.getDependentRequired() != null) {
            s1.setDependentRequired(s2.getDependentRequired());
            if (withErase) {
                s2.setDependentRequired(null);
            }
        }
        if (s1.get$comment() == null && s2.get$comment() != null) {
            s1.set$comment(s2.get$comment());
            if (withErase) {
                s2.set$comment(null);
            }
        }
        if (s1.getExamples() == null && s2.getExamples() != null) {
            s1.setExamples((List) s2.getExamples());
            if (withErase) {
                s2.setExamples(null);
            }
        }
        if (s1.getBooleanSchemaValue() == null && s2.getBooleanSchemaValue() != null) {
            s1.setBooleanSchemaValue(s2.getBooleanSchemaValue());
            if (withErase) {
                s2.setBooleanSchemaValue(null);
            }
        }
        if (s1.getJsonSchema() == null && s2.getJsonSchema() != null) {
            s1.setJsonSchema(s2.getJsonSchema());
            if (withErase) {
                s2.setJsonSchema(null);
            }
        }
        if (s1.getJsonSchemaImpl() == null && s2.getJsonSchemaImpl() != null) {
            s1.setJsonSchemaImpl(s2.getJsonSchemaImpl());
            if (withErase) {
                s2.setJsonSchemaImpl(null);
            }
        }
        return s1;
    }

    /**
     * Copy information from one {@link OpenAPI} object to another.
     *
     * @param to The {@link OpenAPI} object to copy to
     * @param from The {@link OpenAPI} object to copy from
     * @param replace replace existed elements or append
     */
    public static void copyOpenApi(OpenAPI to, OpenAPI from, boolean replace) {
        if (to == null || from == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(from.getTags())) {
            if (replace) {
                from.getTags().forEach(to::addTagsItem);
            } else {
                var tags = resolveTags(to);
                for (var tag : from.getTags()) {
                    var found = false;
                    for (var existedTag : tags) {
                        if (existedTag.getName().equals(tag.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        tags.add(tag);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(from.getServers())) {
            if (replace) {
                from.getServers().forEach(to::addServersItem);
            } else {
                var servers = to.getServers();
                if (servers == null) {
                    servers = new ArrayList<>();
                    to.setServers(servers);
                }

                for (var server : from.getServers()) {
                    var found = false;
                    for (var existedServer : servers) {
                        if (existedServer.getUrl().equals(server.getUrl())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        servers.add(server);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(from.getSecurity())) {
            if (replace) {
                from.getSecurity().forEach(to::addSecurityItem);
            } else {
                var security = to.getSecurity();
                if (security == null) {
                    security = new ArrayList<>();
                    to.setSecurity(security);
                }

                for (var securityRequirement : from.getSecurity()) {
                    var found = false;
                    for (var existedSecurityRequirement : security) {
                        if (existedSecurityRequirement.equals(securityRequirement)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        security.add(securityRequirement);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(from.getPaths())) {

            var toPaths = to.getPaths();
            if (toPaths == null) {
                toPaths = new Paths();
                to.setPaths(toPaths);
            }

            for (var fromPathEntry : from.getPaths().entrySet()) {
                if (!toPaths.containsKey(fromPathEntry.getKey())) {
                    toPaths.put(fromPathEntry.getKey(), fromPathEntry.getValue());
                    continue;
                }
                // if we found existed path, need to check every method and content-type
                // can exist same path, but with different content type or method - it's different endpoints
                var fromPathItem = fromPathEntry.getValue();
                for (var method : PathItem.HttpMethod.values()) {
                    replaceOrAppendOperation(method, fromPathItem, toPaths.get(fromPathEntry.getKey()), replace);
                }
            }
        }
        if (from.getExternalDocs() != null) {
            if (replace || to.getExternalDocs() == null) {
                to.setExternalDocs(from.getExternalDocs());
            }
        }
        if (CollectionUtils.isNotEmpty(from.getExtensions())) {
            if (replace) {
                from.getExtensions().forEach(to::addExtension);
            } else {
                var extensions = to.getExtensions();
                if (extensions == null) {
                    extensions = new HashMap<>();
                    to.setExtensions(extensions);
                }

                for (var extensionEntry : from.getExtensions().entrySet()) {
                    extensions.putIfAbsent(extensionEntry.getKey(), extensionEntry.getValue());
                }
            }
        }

        if (from.getComponents() != null) {

            var fromComponents = from.getComponents();
            var toComponents = resolveComponents(to);

            var fromSchemas = fromComponents.getSchemas();
            var toSchemas = toComponents.getSchemas();
            if (toSchemas == null) {
                toComponents.setSchemas(fromSchemas);
            } else if (CollectionUtils.isNotEmpty(fromSchemas)) {
                for (var entry : fromSchemas.entrySet()) {
                    var value = entry.getValue();
                    if (value.getName() == null) {
                        value.setName(entry.getKey());
                    }
                    if (replace || !toSchemas.containsKey(entry.getKey())) {
                        toSchemas.put(entry.getKey(), value);
                    }
                }
            }

            var fromSecuritySchemes = fromComponents.getSecuritySchemes();
            var toSecuritySchemes = toComponents.getSecuritySchemes();
            if (toSecuritySchemes == null) {
                toComponents.setSecuritySchemes(fromSecuritySchemes);
            } else if (CollectionUtils.isNotEmpty(fromSecuritySchemes)) {
                for (var entry : fromSecuritySchemes.entrySet()) {
                    var value = entry.getValue();
                    if (value.getName() == null) {
                        value.setName(entry.getKey());
                    }
                    if (replace || !toSecuritySchemes.containsKey(entry.getKey())) {
                        toSecuritySchemes.put(entry.getKey(), value);
                    }
                }
            }

            if (fromComponents.getLinks() != null && toComponents.getLinks() == null) {
                toComponents.setLinks(fromComponents.getLinks());
            } else {
                replaceOrAppendComponents(fromComponents.getLinks(), toComponents.getLinks(), replace);
            }

            if (fromComponents.getCallbacks() != null && toComponents.getCallbacks() == null) {
                toComponents.setCallbacks(fromComponents.getCallbacks());
            } else {
                replaceOrAppendComponents(fromComponents.getCallbacks(), toComponents.getCallbacks(), replace);
            }

            if (fromComponents.getHeaders() != null && toComponents.getHeaders() == null) {
                toComponents.setHeaders(fromComponents.getHeaders());
            } else {
                replaceOrAppendComponents(fromComponents.getHeaders(), toComponents.getHeaders(), replace);
            }

            if (fromComponents.getParameters() != null && toComponents.getParameters() == null) {
                toComponents.setParameters(fromComponents.getParameters());
            } else {
                replaceOrAppendComponents(fromComponents.getParameters(), toComponents.getParameters(), replace);
            }

            if (fromComponents.getResponses() != null && toComponents.getResponses() == null) {
                toComponents.setResponses(fromComponents.getResponses());
            } else {
                replaceOrAppendComponents(fromComponents.getResponses(), toComponents.getResponses(), replace);
            }

            if (fromComponents.getRequestBodies() != null && toComponents.getRequestBodies() == null) {
                toComponents.setRequestBodies(fromComponents.getRequestBodies());
            } else {
                replaceOrAppendComponents(fromComponents.getRequestBodies(), toComponents.getRequestBodies(), replace);
            }

            if (fromComponents.getExtensions() != null && toComponents.getExtensions() == null) {
                toComponents.setExtensions(fromComponents.getExtensions());
            } else {
                replaceOrAppendComponents(fromComponents.getExtensions(), toComponents.getExtensions(), replace);
            }

            if (fromComponents.getPathItems() != null && toComponents.getPathItems() == null) {
                toComponents.setPathItems(fromComponents.getPathItems());
            } else {
                replaceOrAppendComponents(fromComponents.getPathItems(), toComponents.getPathItems(), replace);
            }

            if (fromComponents.getExamples() != null && toComponents.getExamples() == null) {
                toComponents.setExamples(fromComponents.getExamples());
            } else {
                replaceOrAppendComponents(fromComponents.getExamples(), toComponents.getExamples(), replace);
            }
        }
    }

    private static void replaceOrAppendOperation(PathItem.HttpMethod method, PathItem from, PathItem to, boolean replace) {
        var fromOp = from.readOperationsMap().get(method);
        var toOp = to.readOperationsMap().get(method);
        if (fromOp == null) {
            return;
        }
        if (toOp == null) {
            to.operation(method, fromOp);
            return;
        }

        var fromRb = fromOp.getRequestBody();
        var toRb = toOp.getRequestBody();
        if (fromRb != null) {
            if (toRb == null) {
                toOp.setRequestBody(fromRb);
            } else {
                var fromContent = fromRb.getContent();
                var toContent = toRb.getContent();
                if (fromContent != null) {
                    if (toContent == null) {
                        toRb.setContent(fromContent);
                    } else {

                        for (var fromMediaTypeEntry : fromContent.entrySet()) {
                            if (toContent.containsKey(fromMediaTypeEntry.getKey())) {
                                if (!replace) {
                                    continue;
                                }
                            }
                            toContent.put(fromMediaTypeEntry.getKey(), fromMediaTypeEntry.getValue());
                        }
                    }
                }
                if (toRb.getDescription() == null || replace) {
                    toRb.description(fromRb.getDescription());
                }
                if (toRb.getRequired() == null || replace) {
                    toRb.required(fromRb.getRequired());
                }
                if (toRb.getExtensions() == null || replace) {
                    toRb.extensions(fromRb.getExtensions());
                }
            }
        }
        var fromResponses = fromOp.getResponses();
        var toResponses = toOp.getResponses();
        if (fromResponses != null) {
            if (toResponses == null) {
                toOp.setResponses(fromResponses);
            } else {
                for (var fromResponseEntry : fromResponses.entrySet()) {
                    var fromResponse = fromResponseEntry.getValue();
                    var toResponse = toResponses.get(fromResponseEntry.getKey());
                    if (toResponse == null) {
                        if (!replace) {
                            continue;
                        }
                        toResponses.put(fromResponseEntry.getKey(), fromResponseEntry.getValue());
                        continue;
                    }
                    var fromContent = fromResponse.getContent();
                    var toContent = toResponse.getContent();
                    if (fromContent != null) {
                        if (toContent == null) {
                            toResponse.setContent(fromContent);
                        } else {

                            for (var fromMediaTypeEntry : fromContent.entrySet()) {
                                if (toContent.containsKey(fromMediaTypeEntry.getKey())) {
                                    if (!replace) {
                                        continue;
                                    }
                                }
                                toContent.put(fromMediaTypeEntry.getKey(), fromMediaTypeEntry.getValue());
                            }
                        }
                    }

                    if (toResponse.getHeaders() == null || replace) {
                        toResponse.headers(fromResponse.getHeaders());
                    }
                    if (toResponse.getDescription() == null || replace) {
                        toResponse.description(fromResponse.getDescription());
                    }
                    if (toResponse.getExtensions() == null || replace) {
                        toResponse.extensions(fromResponse.getExtensions());
                    }
                }
            }
        }

        if (toOp.getDeprecated() == null || replace) {
            toOp.deprecated(fromOp.getDeprecated());
        }
        if (toOp.getSummary() == null || replace) {
            toOp.summary(fromOp.getSummary());
        }
        if (toOp.getDescription() == null || replace) {
            toOp.description(fromOp.getDescription());
        }
        if (toOp.getExternalDocs() == null || replace) {
            toOp.externalDocs(fromOp.getExternalDocs());
        }
        if (toOp.getParameters() == null || replace) {
            toOp.parameters(fromOp.getParameters());
        }
        if (toOp.getCallbacks() == null || replace) {
            toOp.callbacks(fromOp.getCallbacks());
        }
        if (toOp.getSecurity() == null || replace) {
            toOp.security(fromOp.getSecurity());
        }
        if (toOp.getServers() == null || replace) {
            toOp.servers(fromOp.getServers());
        }
        if (toOp.getExtensions() == null || replace) {
            toOp.extensions(fromOp.getExtensions());
        }
    }

    private static <T> void replaceOrAppendComponents(Map<String, T> from, Map<String, T> to, boolean replace) {
        if (CollectionUtils.isEmpty(from)) {
            return;
        }
        for (var entry : from.entrySet()) {
            var value = entry.getValue();
            if (replace || !to.containsKey(entry.getKey())) {
                to.put(entry.getKey(), value);
            }
        }
    }

    private static Components resolveComponents(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        return openApi.getComponents();
    }

    public static boolean isIgnoredHeader(String headerName) {
        // Header parameter named "Authorization" is ignored. Use the `securitySchemes` and `security` sections instead to define authorization
        // Header parameter named "Content-Type" is ignored. The values for the "Content-Type" header are defined by `request.body.content.<media-type>`
        // Header parameter named "Accept" is ignored. The values for the "Accept" header are defined by `responses.<code>.content.<media-type>`
        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)
            || HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(headerName)
            || HttpHeaders.ACCEPT.equalsIgnoreCase(headerName);
    }

    public static Schema setNullable(Schema<?> schema) {
        return setNullable(true, schema);
    }

    public static Schema setNullable(Boolean value, Schema<?> schema) {
        if (value == null || !value) {
            schema.setNullable(value);
            if (isOpenapi31() && schema.getTypes() != null) {
                schema.getTypes().removeIf(TYPE_NULL::equals);
            }
        } else {
            if (isOpenapi31()) {
                schema.addType(TYPE_NULL);
                schema.addType(schema.getType() != null ? schema.getType() : TYPE_OBJECT);
            } else {
                schema.setNullable(true);
            }
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
        if (isOpenapi31() && CollectionUtils.isNotEmpty(types)) {
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

    public static boolean isEquals(Schema<?> s1, Schema<?> s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            return false;
        }
        var t1 = s1.getType();
        if (t1 == null) {
            t1 = TYPE_OBJECT;
        }
        var t2 = s2.getType();
        if (t2 == null) {
            t2 = TYPE_OBJECT;
        }
        var types1 = s1.getTypes();
        if (types1 == null) {
            types1 = Set.of(TYPE_OBJECT);
        }
        var types2 = s2.getTypes();
        if (types2 == null) {
            types2 = Set.of(TYPE_OBJECT);
        }

        return Objects.equals(t1, t2)
            && Objects.equals(types1, types2)
            && Objects.equals(s1.getTitle(), s2.getTitle())
            && Objects.equals(s1.getMultipleOf(), s2.getMultipleOf())
            && Objects.equals(s1.getMaximum(), s2.getMaximum())
            && Objects.equals(s1.getExclusiveMaximum(), s2.getExclusiveMaximum())
            && Objects.equals(s1.getExclusiveMaximumValue(), s2.getExclusiveMaximumValue())
            && Objects.equals(s1.getMinimum(), s2.getMinimum())
            && Objects.equals(s1.getExclusiveMinimum(), s2.getExclusiveMinimum())
            && Objects.equals(s1.getExclusiveMinimumValue(), s2.getExclusiveMinimumValue())
            && Objects.equals(s1.getMaxLength(), s2.getMaxLength())
            && Objects.equals(s1.getMinLength(), s2.getMinLength())
            && Objects.equals(s1.getPattern(), s2.getPattern())
            && Objects.equals(s1.getMaxItems(), s2.getMaxItems())
            && Objects.equals(s1.getMinItems(), s2.getMinItems())
            && Objects.equals(s1.getUniqueItems(), s2.getUniqueItems())
            && Objects.equals(s1.getMaxProperties(), s2.getMaxProperties())
            && Objects.equals(s1.getMinProperties(), s2.getMinProperties())
            && Objects.equals(s1.getNot(), s2.getNot())
            && Objects.equals(s1.getAdditionalProperties(), s2.getAdditionalProperties())
            && Objects.equals(s1.getDescription(), s2.getDescription())
            && Objects.equals(s1.getFormat(), s2.getFormat())
            && Objects.equals(s1.get$ref(), s2.get$ref())
            && Objects.equals(s1.getNullable(), s2.getNullable())
            && Objects.equals(s1.getReadOnly(), s2.getReadOnly())
            && Objects.equals(s1.getWriteOnly(), s2.getWriteOnly())
            && isObjectsEqual(s1.getExample(), s2.getExample())
            && Objects.equals(s1.getExternalDocs(), s2.getExternalDocs())
            && Objects.equals(s1.getDeprecated(), s2.getDeprecated())
            && Objects.equals(s1.getXml(), s2.getXml())
            && Objects.equals(s1.getExtensions(), s2.getExtensions())
            && Objects.equals(s1.getDiscriminator(), s2.getDiscriminator())
            && Objects.equals(s1.getEnum(), s2.getEnum())
            && isEquals(s1.getContains(), s2.getContains())
            && isEquals(s1.getPatternProperties(), s2.getPatternProperties())
            && Objects.equals(s1.get$id(), s2.get$id())
            && Objects.equals(s1.get$anchor(), s2.get$anchor())
            && Objects.equals(s1.get$schema(), s2.get$schema())
            && Objects.equals(s1.get$vocabulary(), s2.get$vocabulary())
            && Objects.equals(s1.get$dynamicAnchor(), s2.get$dynamicAnchor())
            && isEquals(s1.getAllOf(), s2.getAllOf())
            && isEquals(s1.getAnyOf(), s2.getAnyOf())
            && isEquals(s1.getOneOf(), s2.getOneOf())
            && isObjectsEqual(s1.getConst(), s2.getConst())
            && isObjectsEqual(s1.getDefault(), s2.getDefault())
            && Objects.equals(s1.getContentEncoding(), s2.getContentEncoding())
            && Objects.equals(s1.getContentMediaType(), s2.getContentMediaType())
            && isEquals(s1.getContentSchema(), s2.getContentSchema())
            && isEquals(s1.getPropertyNames(), s2.getPropertyNames())
            && isEquals(s1.getUnevaluatedProperties(), s2.getUnevaluatedProperties())
            && Objects.equals(s1.getMaxContains(), s2.getMaxContains())
            && Objects.equals(s1.getMinContains(), s2.getMinContains())
            && isEquals(s1.getAdditionalItems(), s2.getAdditionalItems())
            && isEquals(s1.getUnevaluatedItems(), s2.getUnevaluatedItems())
            && isEquals(s1.getIf(), s2.getIf())
            && isEquals(s1.getElse(), s2.getElse())
            && isEquals(s1.getThen(), s2.getThen())
            && Objects.equals(s1.getDependentRequired(), s2.getDependentRequired())
            && isEquals(s1.getDependentSchemas(), s2.getDependentSchemas())
            && Objects.equals(s1.get$comment(), s2.get$comment())
            && isEqualsExamples(s1.getExamples(), s2.getExamples())
            && isEquals(s1.getPrefixItems(), s2.getPrefixItems())
            && isEquals(s1.getItems(), s2.getItems());
    }

    public static boolean isObjectsEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        if (o2 == null) {
            return false;
        }
        if (o1 instanceof Object[] a1 && o2 instanceof Object[] a2) {
            return Arrays.equals(a1, a2);
        }
        return Objects.equals(o1, o2);
    }

    public static boolean isEquals(Map<String, Schema> m1, Map<String, Schema> m2) {
        if (m1 == null) {
            return m2 == null;
        }
        if (m2 == null) {
            return false;
        }
        if (m1.size() != m2.size()) {
            return false;
        }

        return m1.entrySet().stream()
            .allMatch(e -> isEquals(e.getValue(), m2.get(e.getKey())));
    }

    public static boolean isEqualsExamples(List<?> l1, List<?> l2) {
        if (l1 == null) {
            return l2 == null;
        }
        if (l2 == null) {
            return false;
        }
        if (l1.size() != l2.size()) {
            return false;
        }
        for (var e1 : l1) {
            boolean found = false;
            for (var e2 : l2) {
                if (isObjectsEqual(e1, e2)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEquals(List<Schema> l1, List<Schema> l2) {
        if (l1 == null) {
            return l2 == null;
        }
        if (l2 == null) {
            return false;
        }
        if (l1.size() != l2.size()) {
            return false;
        }
        for (var s1 : l1) {
            boolean found = false;
            for (var s2 : l2) {
                if (isEquals(s1, s2)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static RequiredMode getReqMode(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn) {
        Boolean elementSchemaRequired = null;
        boolean isRequiredDefaultValueSet = false;
        if (schemaAnn != null) {
            elementSchemaRequired = schemaAnn.get(PROP_REQUIRED, Argument.BOOLEAN).orElse(null);
            isRequiredDefaultValueSet = !schemaAnn.contains(PROP_REQUIRED);
            var requiredMode = schemaAnn.enumValue(PROP_REQUIRED_MODE, io.swagger.v3.oas.annotations.media.Schema.RequiredMode.class)
                .orElse(null);
            if (requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED) {
                return new RequiredMode(true, false, isRequiredDefaultValueSet);
            } else if (requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED) {
                return new RequiredMode(false, false, isRequiredDefaultValueSet);
            }
        }
        return new RequiredMode(elementSchemaRequired, true, isRequiredDefaultValueSet);
    }

    public static Schema<?> unwrapComposedSchema(Schema<?> schema) {
        if (!(schema instanceof ComposedSchema composedSchema)) {
            return schema;
        }
        if (composedSchema.getAllOf() != null && composedSchema.getAllOf().size() == 1) {
            return unwrapComposedSchema(composedSchema.getAllOf().get(0));
        }
        return schema;
    }

    /**
     * Object helper to understand the required mode for schema properties.
     *
     * @param elementSchemaRequired value from annotation
     * @param isAutoRequiredMode is auto-required mode
     * @param isRequiredDefaultValueSet is required and the default value set
     */
    public record RequiredMode(
        Boolean elementSchemaRequired,
        boolean isAutoRequiredMode,
        boolean isRequiredDefaultValueSet
    ) {
    }
}
