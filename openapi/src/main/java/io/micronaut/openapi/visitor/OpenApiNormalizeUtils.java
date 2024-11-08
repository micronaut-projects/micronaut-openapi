/*
 * Copyright 2017-2024 original authors
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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.SchemaUtils.EMPTY_SIMPLE_SCHEMA;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_STRING;
import static io.micronaut.openapi.visitor.SchemaUtils.appendSchema;
import static io.micronaut.openapi.visitor.SchemaUtils.isEmptySchema;
import static io.micronaut.openapi.visitor.SchemaUtils.setSpecVersion;

/**
 * Normalization methods for openAPI objects.
 *
 * @since 6.6.0
 */
@Internal
public final class OpenApiNormalizeUtils {

    private OpenApiNormalizeUtils() {
    }

    public static void normalizeOpenApi(OpenAPI openAPI, VisitorContext context) {

        if (CollectionUtils.isEmpty(openAPI.getExtensions())) {
            openAPI.setExtensions(null);
        }

        if (CollectionUtils.isNotEmpty(openAPI.getServers())) {
            for (var server : openAPI.getServers()) {
                if (CollectionUtils.isEmpty(server.getExtensions())) {
                    server.setExtensions(null);
                }
            }
        }

        // Sort paths
        if (openAPI.getPaths() != null) {
            var sortedPaths = new Paths();
            new TreeMap<>(openAPI.getPaths()).forEach(sortedPaths::addPathItem);
            if (openAPI.getPaths().getExtensions() != null) {
                sortedPaths.setExtensions(new TreeMap<>(openAPI.getPaths().getExtensions()));
            }
            openAPI.setPaths(sortedPaths);
            for (PathItem pathItem : sortedPaths.values()) {
                if (CollectionUtils.isEmpty(pathItem.getExtensions())) {
                    pathItem.setExtensions(null);
                }
                normalizeOperation(pathItem.getGet(), context);
                normalizeOperation(pathItem.getPut(), context);
                normalizeOperation(pathItem.getPost(), context);
                normalizeOperation(pathItem.getDelete(), context);
                normalizeOperation(pathItem.getOptions(), context);
                normalizeOperation(pathItem.getHead(), context);
                normalizeOperation(pathItem.getPatch(), context);
                normalizeOperation(pathItem.getTrace(), context);
            }
        }

        // Sort all reusable Components
        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }

        normalizeSchemas(components.getSchemas(), context);

        sortComponent(components, Components::getSchemas, Components::setSchemas);
        sortComponent(components, Components::getResponses, Components::setResponses);
        sortComponent(components, Components::getParameters, Components::setParameters);
        sortComponent(components, Components::getExamples, Components::setExamples);
        sortComponent(components, Components::getRequestBodies, Components::setRequestBodies);
        sortComponent(components, Components::getHeaders, Components::setHeaders);
        sortComponent(components, Components::getSecuritySchemes, Components::setSecuritySchemes);
        sortComponent(components, Components::getLinks, Components::setLinks);
        sortComponent(components, Components::getCallbacks, Components::setCallbacks);
        if (Utils.isOpenapi31()) {
            sortComponent(components, Components::getPathItems, Components::setPathItems);
            if (CollectionUtils.isNotEmpty(openAPI.getWebhooks())) {
                openAPI.setWebhooks(new TreeMap<>(openAPI.getWebhooks()));
            }
        }
    }

    public static void normalizeOperation(Operation operation, VisitorContext context) {
        if (operation == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(operation.getParameters())) {
            for (Parameter parameter : operation.getParameters()) {
                if (parameter == null) {
                    continue;
                }
                if (CollectionUtils.isEmpty(parameter.getExtensions())) {
                    parameter.setExtensions(null);
                }
                Schema<?> paramSchema = parameter.getSchema();
                if (paramSchema == null) {
                    continue;
                }
                Schema<?> normalizedSchema = normalizeSchema(paramSchema, context);
                if (normalizedSchema != null) {
                    parameter.setSchema(normalizedSchema);
                } else if (paramSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                    paramSchema.setType(TYPE_OBJECT);
                }
                if (parameter.getExample() != null
                    && parameter.getExample() instanceof String exampleStr
                    && parameter.getSchema() != null) {
                    parameter.setExample(ConvertUtils.parseByTypeAndFormat(exampleStr, parameter.getSchema().getType(), parameter.getSchema().getFormat(), context, false));
                }
                normalizeExamples(parameter.getExamples());
            }
        }
        if (CollectionUtils.isEmpty(operation.getExtensions())) {
            operation.setExtensions(null);
        }

        if (operation.getRequestBody() != null) {
            normalizeContent(operation.getRequestBody().getContent(), context);
        }
        if (CollectionUtils.isNotEmpty(operation.getResponses())) {
            for (ApiResponse apiResponse : operation.getResponses().values()) {
                normalizeContent(apiResponse.getContent(), context);
                normalizeHeaders(apiResponse.getHeaders(), context);
                if (CollectionUtils.isEmpty(apiResponse.getExtensions())) {
                    apiResponse.setExtensions(null);
                }
            }
        }
    }

    public static void normalizeHeaders(Map<String, Header> headers, VisitorContext context) {
        if (CollectionUtils.isEmpty(headers)) {
            return;
        }

        for (var header : headers.values()) {
            if (CollectionUtils.isEmpty(header.getExtensions())) {
                header.setExtensions(null);
            }
            Schema<?> headerSchema = header.getSchema();
            if (headerSchema == null) {
                headerSchema = setSpecVersion(new StringSchema());
                header.setSchema(headerSchema);
            }
            Schema<?> normalizedSchema = normalizeSchema(headerSchema, context);
            if (normalizedSchema != null) {
                header.setSchema(normalizedSchema);
            } else if (headerSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                headerSchema.setType(TYPE_OBJECT);
            }
            if (header.getExample() != null
                && header.getExample() instanceof String exampleStr) {
                header.setExample(ConvertUtils.parseByTypeAndFormat(exampleStr, header.getSchema().getType(), header.getSchema().getFormat(), context, false));
            }
            normalizeExamples(header.getExamples());
            normalizeContent(header.getContent(), context);
        }
    }

    public static void normalizeContent(Content content, VisitorContext context) {
        if (CollectionUtils.isEmpty(content)) {
            return;
        }
        for (var mediaType : content.values()) {
            if (CollectionUtils.isEmpty(mediaType.getExtensions())) {
                mediaType.setExtensions(null);
            }
            Schema<?> mediaTypeSchema = mediaType.getSchema();
            if (mediaTypeSchema == null) {
                continue;
            }
            Schema<?> normalizedSchema = normalizeSchema(mediaTypeSchema, context);
            if (normalizedSchema != null) {
                mediaType.setSchema(normalizedSchema);
            } else if (mediaTypeSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                mediaTypeSchema.setType(TYPE_OBJECT);
            }
            normalizeExamples(mediaType.getExamples());
            Map<String, Schema> paramSchemas = mediaTypeSchema.getProperties();
            if (CollectionUtils.isNotEmpty(paramSchemas)) {
                var paramNormalizedSchemas = new HashMap<String, Schema>();
                for (var paramEntry : paramSchemas.entrySet()) {
                    Schema<?> paramSchema = paramEntry.getValue();
                    Schema<?> paramNormalizedSchema = normalizeSchema(paramSchema, context);
                    if (paramNormalizedSchema != null) {
                        paramNormalizedSchemas.put(paramEntry.getKey(), paramNormalizedSchema);
                    }
                }
                if (CollectionUtils.isNotEmpty(paramNormalizedSchemas)) {
                    paramSchemas.putAll(paramNormalizedSchemas);
                }
            }
        }
    }

    public static void normalizeExamples(Map<String, Example> examples) {
        if (CollectionUtils.isEmpty(examples)) {
            return;
        }
        var iter = examples.keySet().iterator();
        while (iter.hasNext()) {
            var exampleName = iter.next();
            var example = examples.get(exampleName);
            if (example == null) {
                iter.remove();
                continue;
            }
            if (CollectionUtils.isEmpty(example.getExtensions())) {
                example.setExtensions(null);
            }
        }
    }

    public static <T> void sortComponent(Components components, Function<Components, Map<String, T>> getter, BiConsumer<Components, Map<String, T>> setter) {
        if (components != null && getter.apply(components) != null) {
            Map<String, T> component = getter.apply(components);
            setter.accept(components, new TreeMap<>(component));
        }
    }

    public static Schema<?> normalizeSchema(Schema<?> schema, VisitorContext context) {
        if (schema == null) {
            return null;
        }

        if (CollectionUtils.isEmpty(schema.getExtensions())) {
            schema.setExtensions(null);
        }

        if (CollectionUtils.isEmpty(schema.getTypes())
            || (schema.getTypes().size() == 1 && schema.getTypes().iterator().next() == null)) {
            schema.setTypes(null);
        }

        List<Schema> allOf = schema.getAllOf();
        if (CollectionUtils.isNotEmpty(allOf)) {
            if (allOf.size() == 1) {

                Schema<?> allOfSchema = allOf.get(0);
                if (CollectionUtils.isEmpty(allOfSchema.getExtensions())) {
                    allOfSchema.setExtensions(null);
                }

                schema.setAllOf(null);
                // if schema has only allOf block with one item or only defaultValue property or only type
                Object defaultValue = schema.getDefault();
                String type = schema.getType();
                String serializedDefaultValue;
                try {
                    serializedDefaultValue = defaultValue != null ? Utils.getJsonMapper().writeValueAsString(defaultValue) : null;
                } catch (JsonProcessingException e) {
                    return null;
                }
                schema.setDefault(null);
                schema.setType(null);
                Schema<?> normalizedSchema = null;

                Object allOfDefaultValue = allOfSchema.getDefault();
                String serializedAllOfDefaultValue;
                try {
                    serializedAllOfDefaultValue = allOfDefaultValue != null ? Utils.getJsonMapper().writeValueAsString(allOfDefaultValue) : null;
                } catch (JsonProcessingException e) {
                    return null;
                }
                boolean isSameType = allOfSchema.getType() == null || allOfSchema.getType().equals(type);

                if (isEmptySchema(schema)
                    && (serializedDefaultValue == null || serializedDefaultValue.equals(serializedAllOfDefaultValue))
                    && (type == null || isSameType)) {
                    normalizedSchema = allOfSchema;
                }
                schema.setType(type);
                schema.setAllOf(allOf);
                schema.setDefault(defaultValue);
                if (!TYPE_STRING.equals(schema.getType()) && schema.getExample() instanceof String exampleStr) {
                    schema.setExample(ConvertUtils.parseByTypeAndFormat(exampleStr, type, schema.getFormat(), context, false));
                }
                normalizeSchemaProperties(schema, context);
                normalizeSchemaProperties(normalizedSchema, context);
                unwrapAllOff(normalizedSchema);
                normalizeSchema(schema.getItems(), context);
                if (normalizedSchema != null) {
                    normalizeSchema(normalizedSchema.getItems(), context);
                }
                return normalizedSchema;
            }

            var finalList = new ArrayList<Schema>(allOf.size());
            var schemasWithoutRef = new ArrayList<Schema>(allOf.size() - 1);
            for (Schema<?> schemaAllOf : allOf) {
                Schema<?> normalizedSchema = normalizeSchema(schemaAllOf, context);
                if (normalizedSchema != null) {
                    schemaAllOf = normalizedSchema;
                }
                Map<String, Schema> paramSchemas = schemaAllOf.getProperties();
                if (CollectionUtils.isNotEmpty(paramSchemas)) {
                    var paramNormalizedSchemas = new HashMap<String, Schema>();
                    for (var paramEntry : paramSchemas.entrySet()) {
                        Schema<?> paramSchema = paramEntry.getValue();
                        Schema<?> paramNormalizedSchema = normalizeSchema(paramSchema, context);
                        if (paramNormalizedSchema != null) {
                            paramNormalizedSchemas.put(paramEntry.getKey(), paramNormalizedSchema);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(paramNormalizedSchemas)) {
                        paramSchemas.putAll(paramNormalizedSchemas);
                    }
                }

                if (StringUtils.isEmpty(schemaAllOf.get$ref())) {
                    schemasWithoutRef.add(schemaAllOf);
                    // remove all description fields, if it's already set in main schema
                    if (StringUtils.isNotEmpty(schema.getDescription())
                        && StringUtils.isNotEmpty(schemaAllOf.getDescription())) {
                        schemaAllOf.setDescription(null);
                    }
                    // remove duplicate default field
                    if (schema.getDefault() != null
                        && schemaAllOf.getDefault() != null && schema.getDefault().equals(schemaAllOf.getDefault())) {
                        schema.setDefault(null);
                    }
                    continue;
                }
                finalList.add(schemaAllOf);
            }
            finalList.addAll(schemasWithoutRef);
            schema.setAllOf(finalList);
        }
        normalizeSchemaProperties(schema, context);
        unwrapAllOff(schema);
        normalizeSchema(schema.getItems(), context);
        return null;
    }

    private static void normalizeSchemaProperties(Schema<?> schema, VisitorContext context) {

        if (schema == null) {
            return;
        }

        String type = schema.getType();

        if (!TYPE_STRING.equals(type) && schema.getExample() instanceof String exampleStr) {
            schema.setExample(ConvertUtils.parseByTypeAndFormat(exampleStr, type, schema.getFormat(), context, false));
        }
    }

    /**
     * Sort schemas list in allOf block: schemas with ref must be first, next other schemas.
     *
     * @param schemas all schema components
     * @param context Visitor context
     */
    public static void normalizeSchemas(Map<String, Schema> schemas, VisitorContext context) {

        if (CollectionUtils.isEmpty(schemas)) {
            return;
        }

        var normalizedSchemas = new HashMap<String, Schema>();

        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            Schema<?> schema = entry.getValue();
            Schema<?> normalizedSchema = normalizeSchema(schema, context);
            if (normalizedSchema != null) {
                normalizedSchemas.put(entry.getKey(), normalizedSchema);
            } else if (schema.equals(EMPTY_SIMPLE_SCHEMA)) {
                schema.setType(TYPE_OBJECT);
            }

            Map<String, Schema> paramSchemas = schema.getProperties();
            if (CollectionUtils.isNotEmpty(paramSchemas)) {
                var paramNormalizedSchemas = new HashMap<String, Schema>();
                for (Map.Entry<String, Schema> paramEntry : paramSchemas.entrySet()) {
                    Schema<?> paramSchema = paramEntry.getValue();
                    Schema<?> paramNormalizedSchema = normalizeSchema(paramSchema, context);
                    if (paramNormalizedSchema != null) {
                        paramNormalizedSchemas.put(paramEntry.getKey(), paramNormalizedSchema);
                    } else if (paramSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                        paramSchema.setType(TYPE_OBJECT);
                    }
                }
                if (CollectionUtils.isNotEmpty(paramNormalizedSchemas)) {
                    paramSchemas.putAll(paramNormalizedSchemas);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(normalizedSchemas)) {
            schemas.putAll(normalizedSchemas);
        }
    }

    public static void removeEmptyComponents(OpenAPI openAPI) {

        if (CollectionUtils.isEmpty(openAPI.getWebhooks())) {
            openAPI.setWebhooks(null);
        }

        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }
        if (CollectionUtils.isEmpty(components.getSchemas())) {
            components.setSchemas(null);
        }
        if (CollectionUtils.isEmpty(components.getResponses())) {
            components.setResponses(null);
        }
        if (CollectionUtils.isEmpty(components.getParameters())) {
            components.setParameters(null);
        }
        if (CollectionUtils.isEmpty(components.getExamples())) {
            components.setExamples(null);
        }
        if (CollectionUtils.isEmpty(components.getRequestBodies())) {
            components.setRequestBodies(null);
        }
        if (CollectionUtils.isEmpty(components.getHeaders())) {
            components.setHeaders(null);
        }
        if (CollectionUtils.isEmpty(components.getSecuritySchemes())) {
            components.setSecuritySchemes(null);
        }
        if (CollectionUtils.isEmpty(components.getLinks())) {
            components.setLinks(null);
        }
        if (CollectionUtils.isEmpty(components.getCallbacks())) {
            components.setCallbacks(null);
        }
        if (CollectionUtils.isEmpty(components.getExtensions())) {
            components.setExtensions(null);
        }
        if (CollectionUtils.isEmpty(components.getPathItems())) {
            components.setPathItems(null);
        }

        if (CollectionUtils.isEmpty(components.getSchemas())
            && CollectionUtils.isEmpty(components.getResponses())
            && CollectionUtils.isEmpty(components.getParameters())
            && CollectionUtils.isEmpty(components.getExamples())
            && CollectionUtils.isEmpty(components.getRequestBodies())
            && CollectionUtils.isEmpty(components.getHeaders())
            && CollectionUtils.isEmpty(components.getSecuritySchemes())
            && CollectionUtils.isEmpty(components.getLinks())
            && CollectionUtils.isEmpty(components.getCallbacks())
            && CollectionUtils.isEmpty(components.getExtensions())
            && CollectionUtils.isEmpty(components.getPathItems())
        ) {
            openAPI.setComponents(null);
        }
    }

    /**
     * Find and remove duplicates in openApi object.
     *
     * @param openApi openAPI object
     */
    public static void findAndRemoveDuplicates(OpenAPI openApi) {
        openApi.setTags(Utils.findAndRemoveDuplicates(openApi.getTags(), (el1, el2) -> el1.getName() != null && el1.getName().equals(el2.getName())));
        openApi.setServers(Utils.findAndRemoveDuplicates(openApi.getServers(), (el1, el2) -> el1.getUrl() != null && el1.getUrl().equals(el2.getUrl())));
        openApi.setSecurity(Utils.findAndRemoveDuplicates(openApi.getSecurity(), (el1, el2) -> el1 != null && el1.equals(el2)));
        if (CollectionUtils.isNotEmpty(openApi.getPaths())) {
            for (var path : openApi.getPaths().values()) {
                path.setServers(Utils.findAndRemoveDuplicates(path.getServers(), (el1, el2) -> el1.getUrl() != null && el1.getUrl().equals(el2.getUrl())));
                path.setParameters(Utils.findAndRemoveDuplicates(path.getParameters(), (el1, el2) -> el1.getName() != null && el1.getName().equals(el2.getName())
                    && el1.getIn() != null && el1.getIn().equals(el2.getIn())));
                findAndRemoveDuplicates(path.getGet());
                findAndRemoveDuplicates(path.getPut());
                findAndRemoveDuplicates(path.getPost());
                findAndRemoveDuplicates(path.getDelete());
                findAndRemoveDuplicates(path.getOptions());
                findAndRemoveDuplicates(path.getHead());
                findAndRemoveDuplicates(path.getPatch());
                findAndRemoveDuplicates(path.getTrace());
            }
        }
        if (openApi.getComponents() != null) {
            if (CollectionUtils.isNotEmpty(openApi.getComponents().getSchemas())) {
                for (var schema : openApi.getComponents().getSchemas().values()) {
                    findAndRemoveDuplicates(schema);
                }
            }
        }
        if (openApi.getComponents() != null) {
            if (CollectionUtils.isNotEmpty(openApi.getComponents().getSchemas())) {
                for (var schema : openApi.getComponents().getSchemas().values()) {
                    findAndRemoveDuplicates(schema);
                }
            }
        }
    }

    public static void findAndRemoveDuplicates(Operation operation) {
        if (operation == null) {
            return;
        }
        operation.setTags(Utils.findAndRemoveDuplicates(operation.getTags(), (el1, el2) -> el1 != null && el1.equals(el2)));
        operation.setServers(Utils.findAndRemoveDuplicates(operation.getServers(), (el1, el2) -> el1.getUrl() != null && el1.getUrl().equals(el2.getUrl())));
        operation.setSecurity(Utils.findAndRemoveDuplicates(operation.getSecurity(), (el1, el2) -> el1 != null && el1.equals(el2)));
        if (CollectionUtils.isNotEmpty(operation.getParameters())) {
            for (var param : operation.getParameters()) {
                findAndRemoveDuplicates(param.getContent());
                findAndRemoveDuplicates(param.getSchema());
            }
            operation.setParameters(Utils.findAndRemoveDuplicates(operation.getParameters(), (el1, el2) -> el1.getName() != null && el1.getName().equals(el2.getName())
                && el1.getIn() != null && el1.getIn().equals(el2.getIn())));
        }

        if (operation.getRequestBody() != null) {
            findAndRemoveDuplicates(operation.getRequestBody().getContent());
        }
        if (CollectionUtils.isNotEmpty(operation.getResponses())) {
            for (var response : operation.getResponses().values()) {
                findAndRemoveDuplicates(response.getContent());
            }
        }
    }

    public static void findAndRemoveDuplicates(Content content) {
        if (CollectionUtils.isEmpty(content)) {
            return;
        }
        for (var mediaType : content.values()) {
            findAndRemoveDuplicates(mediaType.getSchema());
        }
    }

    public static void findAndRemoveDuplicates(Schema<?> schema) {
        if (schema == null) {
            return;
        }
        schema.setRequired(Utils.findAndRemoveDuplicates(schema.getRequired(), (el1, el2) -> el1 != null && el1.equals(el2)));
        schema.setPrefixItems(Utils.findAndRemoveDuplicates(schema.getPrefixItems(), (el1, el2) -> el1 != null && el1.equals(el2)));
        schema.setAllOf(Utils.findAndRemoveDuplicates(schema.getAllOf(), (el1, el2) -> el1 != null && el1.equals(el2)));
        schema.setAnyOf(Utils.findAndRemoveDuplicates(schema.getAnyOf(), (el1, el2) -> el1 != null && el1.equals(el2)));
        schema.setOneOf(Utils.findAndRemoveDuplicates(schema.getOneOf(), (el1, el2) -> el1 != null && el1.equals(el2)));
    }

    private static void unwrapAllOff(Schema<?> schema) {

        if (schema == null || CollectionUtils.isEmpty(schema.getAllOf())) {
            return;
        }

        // trying to merge allOf schemas
        var mergedAllOf = new ArrayList<Schema>();
        Schema firstAllOfSchema = null;
        for (var innerSchema : schema.getAllOf()) {
            if (innerSchema.get$ref() != null) {
                mergedAllOf.add(innerSchema);
                continue;
            }
            if (firstAllOfSchema == null) {
                firstAllOfSchema = innerSchema;
                mergedAllOf.add(firstAllOfSchema);
                continue;
            }
            appendSchema(firstAllOfSchema, innerSchema);
        }
        schema.setAllOf(mergedAllOf);

        var index = 0;
        var innerSchemas = new HashMap<Integer, Schema>();
        for (var s : schema.getAllOf()) {
            if (StringUtils.isEmpty(s.get$ref())) {
                innerSchemas.put(index, s);
            }
            index++;
        }
        if (innerSchemas.isEmpty()) {
            return;
        }

        for (var entry : innerSchemas.entrySet()) {
            var innerSchema = entry.getValue();
            innerSchema.setName(null);
            if (CollectionUtils.isNotEmpty(innerSchema.getExtensions())) {
                innerSchema.getExtensions().forEach((k, v) -> schema.addExtension(k.toString(), v));
            }
            innerSchema.setExtensions(null);
//            appendSchema(schema, innerSchema, false, true);

            if (schema.getType() == null && innerSchema.getType() != null) {
                schema.setType(innerSchema.getType());
                innerSchema.setType(null);
            }
            if (StringUtils.isNotEmpty(innerSchema.getTitle())) {
                schema.setTitle(innerSchema.getTitle());
                innerSchema.setTitle(null);
            }
            if (StringUtils.isNotEmpty(innerSchema.getDescription())) {
                schema.setDescription(innerSchema.getDescription());
                innerSchema.setDescription(null);
                var jsonSchema = innerSchema.getJsonSchema();
                if (CollectionUtils.isNotEmpty(jsonSchema)) {
                    jsonSchema.remove(PROP_DESCRIPTION);
                }
                if (CollectionUtils.isEmpty(jsonSchema)) {
                    innerSchema.setJsonSchema(null);
                }
            }
            if (innerSchema.getDeprecated() != null) {
                schema.setDeprecated(innerSchema.getDeprecated());
                if (!schema.getDeprecated()) {
                    schema.setDeprecated(false);
                }
                innerSchema.setDeprecated(null);
            }
            if (innerSchema.getNullable() != null) {
                schema.setNullable(innerSchema.getNullable());
                if (!schema.getNullable()) {
                    schema.setNullable(false);
                }
                innerSchema.setNullable(null);
            }
            if (innerSchema.getDefault() != null) {
                schema.setDefault(innerSchema.getDefault());
                innerSchema.setDefault(null);
            }
            if (innerSchema.getAnyOf() != null && schema.getAnyOf() == null) {
                schema.setAnyOf(innerSchema.getAnyOf());
                innerSchema.setAnyOf(null);
            }
            if (innerSchema.getOneOf() != null && schema.getOneOf() == null) {
                schema.setOneOf(innerSchema.getOneOf());
                innerSchema.setOneOf(null);
            }
            if (innerSchema.getNot() != null && schema.getNot() == null) {
                schema.setNot(innerSchema.getNot());
                innerSchema.setNot(null);
            }
            if (CollectionUtils.isNotEmpty(innerSchema.getRequired())) {
                schema.setRequired(innerSchema.getRequired());
                innerSchema.setRequired(null);
            }
            if (innerSchema.getExample() != null) {
                schema.setExampleSetFlag(innerSchema.getExampleSetFlag());
                schema.setExample(innerSchema.getExample());
                innerSchema.setExample(null);
                innerSchema.setExampleSetFlag(false);
            }
            if (CollectionUtils.isNotEmpty(innerSchema.getExamples())) {
                schema.setExamples(innerSchema.getExamples());
                innerSchema.setExamples(null);
            }
            if (innerSchema.getExternalDocs() != null) {
                schema.setExternalDocs(innerSchema.getExternalDocs());
                innerSchema.setExternalDocs(null);
            }
            if (innerSchema.getReadOnly() != null) {
                schema.setReadOnly(innerSchema.getReadOnly());
                innerSchema.setReadOnly(null);
            }
            if (innerSchema.getAdditionalProperties() != null) {
                schema.setAdditionalProperties(innerSchema.getAdditionalProperties());
                innerSchema.setAdditionalProperties(null);
            }

            if (isEmptySchema(innerSchema) && CollectionUtils.isNotEmpty(schema.getAllOf())) {
                schema.getAllOf().remove(entry.getKey().intValue());
            }
            if (CollectionUtils.isEmpty(schema.getAllOf())) {
                schema.setAllOf(null);
                // check case, when schema has only 1 element
//            } else if (schema.getAllOf().size() == 1) {
//                var allOf = schema.getAllOf();
//                var inner = allOf.get(0);
//                schema.setAllOf(null);
//                var ref = inner.get$ref();
//                inner.set$ref(null);
//                if (isEmptySchema(schema)) {
//                    schema.set$ref(ref);
////                    if (!isEmptySchema(innerSchema)) {
////                        appendSchema(schema, innerSchema);
////                    }
////                    schema.setAllOf(null);
//                } else {
//                    inner.set$ref(ref);
//                    schema.setAllOf(allOf);
//                }
//                if (inner.get$ref() == null) {
//                    appendSchema(schema, inner, true, true);
//                    if (CollectionUtils.isEmpty(schema.getAllOf())) {
//                        schema.setAllOf(null);
//                    }
//                }
            }
        }
    }
}
