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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

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

    public static final String TYPE_OBJECT = "object";

    private static final List<Schema<?>> ALL_EMPTY_SCHEMAS;

    static {
        List<Schema<?>> schemas = new ArrayList<>();
        schemas.add(EMPTY_SCHEMA);
        schemas.add(EMPTY_ARRAY_SCHEMA);
        schemas.add(EMPTY_BINARY_SCHEMA);
        schemas.add(EMPTY_BOOLEAN_SCHEMA);
        schemas.add(EMPTY_BYTE_ARRAY_SCHEMA);
        schemas.add(EMPTY_COMPOSED_SCHEMA);
        schemas.add(EMPTY_DATE_SCHEMA);
        schemas.add(EMPTY_DATE_TIME_SCHEMA);
        schemas.add(EMPTY_EMAIL_SCHEMA);
        schemas.add(EMPTY_FILE_SCHEMA);
        schemas.add(EMPTY_INTEGER_SCHEMA);
        schemas.add(EMPTY_JSON_SCHEMA);
        schemas.add(EMPTY_MAP_SCHEMA);
        schemas.add(EMPTY_NUMBER_SCHEMA);
        schemas.add(EMPTY_OBJECT_SCHEMA);
        schemas.add(EMPTY_PASSWORD_SCHEMA);
        schemas.add(EMPTY_STRING_SCHEMA);
        schemas.add(EMPTY_UUID_SCHEMA);
        schemas.add(EMPTY_SIMPLE_SCHEMA);

        ALL_EMPTY_SCHEMAS = Collections.unmodifiableList(schemas);
    }

    private SchemaUtils() {
    }

    public static boolean isEmptySchema(Schema<?> schema) {
        return ALL_EMPTY_SCHEMAS.contains(schema);
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

    public static ArraySchema arraySchema(Schema schema) {
        if (schema == null) {
            return null;
        }
        ArraySchema arraySchema = new ArraySchema();
        arraySchema.setItems(schema);
        return arraySchema;
    }

    public static String schemaRef(String schemaName) {
        return COMPONENTS_SCHEMAS_REF + schemaName;
    }

    public static Operation getOperationOnPathItem(PathItem pathItem, HttpMethod httpMethod) {

        if (pathItem == null) {
            return null;
        }

        switch (httpMethod) {
            case GET:
                return pathItem.getGet();
            case POST:
                return pathItem.getPost();
            case PUT:
                return pathItem.getPut();
            case PATCH:
                return pathItem.getPatch();
            case DELETE:
                return pathItem.getDelete();
            case HEAD:
                return pathItem.getHead();
            case OPTIONS:
                return pathItem.getOptions();
            case TRACE:
                return pathItem.getTrace();
            default:
                return null;
        }
    }

    public static void setOperationOnPathItem(PathItem pathItem, HttpMethod httpMethod, Operation operation) {
        if (pathItem == null) {
            return;
        }
        switch (httpMethod) {
            case GET:
                pathItem.setGet(operation);
                break;
            case POST:
                pathItem.setPost(operation);
                break;
            case PUT:
                pathItem.setPut(operation);
                break;
            case DELETE:
                pathItem.setDelete(operation);
                break;
            case PATCH:
                pathItem.setPatch(operation);
                break;
            default:
                // do nothing
                break;
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
            Set<String> tagsSet = new HashSet<>(op1.getTags());
            tagsSet.addAll(op2.getTags());
            List<String> tags = new ArrayList<>(tagsSet);
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
            Set<SecurityRequirement> securityRequirements = new HashSet<>(op1.getSecurity());
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
            Set<Server> serversSet = new HashSet<>(op1.getServers());
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
                    if (p1.getName().equals(p2.getName())
                        && p1.getIn().equals(p2.getIn())) {
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

            Set<Parameter> serversSet = new HashSet<>(op1.getParameters());
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

        if (CollectionUtils.isEmpty(l1.getHeaders())) {
            l1.setHeaders(l2.getHeaders());
        } else if (CollectionUtils.isNotEmpty(l2.getHeaders())) {
            for (Map.Entry<String, Header> entry1 : l1.getHeaders().entrySet()) {
                Header h2 = l2.getHeaders().get(entry1.getKey());
                entry1.setValue(mergeHeader(entry1.getValue(), h2));
            }
            l2.getHeaders().putAll(l1.getHeaders());
            l1.setHeaders(l2.getHeaders());
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
        Schema<?> finalSchema = new ComposedSchema();
        finalSchema.addOneOfItem(s1);
        finalSchema.addOneOfItem(s2);
        return finalSchema;
    }

}
