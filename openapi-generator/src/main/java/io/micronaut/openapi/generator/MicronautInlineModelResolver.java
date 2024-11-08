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
package io.micronaut.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Inline model resolver.
 *
 * @since 6.5.0
 */
public final class MicronautInlineModelResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicronautInlineModelResolver.class);

    // structure mapper sorts properties alphabetically on write to ensure models are
    // serialized consistently for lookup of existing models
    private static final ObjectMapper STRUCTURE_MAPPER;

    static {
        STRUCTURE_MAPPER = JsonMapper.builder()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .build();
        STRUCTURE_MAPPER.writer(new DefaultPrettyPrinter());
    }

    private OpenAPI openAPI;
    private Map<String, Schema> addedModels = new HashMap<>();
    private Map<String, String> generatedSignature = new HashMap<>();
    private Map<String, String> inlineSchemaNameMapping = new HashMap<>();
    private Map<String, String> inlineSchemaOptions = new HashMap<>();
    private Set<String> inlineSchemaNameMappingValues = new HashSet<>();
    private boolean resolveInlineEnums = true;
    private boolean skipSchemaReuse = false; // skip reusing inline schema if set to true
    private Boolean refactorAllOfInlineSchemas; // refactor allOf inline schemas into $ref

    // a set to keep track of names generated for inline schemas
    private Set<String> uniqueNames = new HashSet<>();

    public MicronautInlineModelResolver(OpenAPI openAPI) {
        this.openAPI = openAPI;
        this.inlineSchemaOptions.put("ARRAY_ITEM_SUFFIX", "_inner");
        this.inlineSchemaOptions.put("MAP_ITEM_SUFFIX", "_value");
    }

    public void setInlineSchemaNameMapping(Map inlineSchemaNameMapping) {
        this.inlineSchemaNameMapping = inlineSchemaNameMapping;
        this.inlineSchemaNameMappingValues = new HashSet<>(inlineSchemaNameMapping.values());
    }

    public void setInlineSchemaOptions(Map inlineSchemaOptions) {
        this.inlineSchemaOptions.putAll(inlineSchemaOptions);

        if ("true".equalsIgnoreCase(
                this.inlineSchemaOptions.getOrDefault("SKIP_SCHEMA_REUSE", "false"))) {
            this.skipSchemaReuse = true;
        }

        if (this.inlineSchemaOptions.containsKey("REFACTOR_ALLOF_INLINE_SCHEMAS")) {
            this.refactorAllOfInlineSchemas = Boolean.valueOf(this.inlineSchemaOptions.get("REFACTOR_ALLOF_INLINE_SCHEMAS"));
        }

        if (this.inlineSchemaOptions.containsKey("RESOLVE_INLINE_ENUMS")) {
            this.resolveInlineEnums = Boolean.parseBoolean(this.inlineSchemaOptions.get("RESOLVE_INLINE_ENUMS"));
        }
    }

    private void normalizeArraySchema(Schema schema) {
        if (ModelUtils.isArraySchema(schema)) {
            schema.setEnum(null);
        }
    }

    public void flatten() {
        if (this.openAPI.getComponents() == null) {
            this.openAPI.setComponents(new Components());
        }

        if (this.openAPI.getComponents().getSchemas() == null) {
            this.openAPI.getComponents().setSchemas(new HashMap<>());
        }

        flattenPaths();
        flattenComponents();
    }

    public void flattenPaths() {
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            return;
        }

        for (Map.Entry<String, PathItem> pathsEntry : paths.entrySet()) {
            PathItem path = pathsEntry.getValue();
            if (path.get$ref() != null && path.get$ref().startsWith("#/components/pathItems/")) {
                path = openAPI.getComponents().getPathItems().get(path.get$ref().substring("#/components/pathItems/".length()));
            }
            var operationsMap = new LinkedHashMap<>(path.readOperationsMap());

            // use path name (e.g. /foo/bar) and HTTP verb to come up with a name
            // in case operationId is not defined later in other methods
            String pathname = pathsEntry.getKey();

            // Include callback operation as well
            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : new LinkedHashMap<>(path.readOperationsMap()).entrySet()) {
                Operation operation = operationEntry.getValue();
                Map<String, Callback> callbacks = operation.getCallbacks();
                if (callbacks != null) {
                    for (Map.Entry<String, Callback> callbackEntry : callbacks.entrySet()) {
                        Callback callback = callbackEntry.getValue();
                        for (Map.Entry<String, PathItem> pathItemEntry : callback.entrySet()) {
                            PathItem pathItem = pathItemEntry.getValue();
                            operationsMap.putAll(pathItem.readOperationsMap());
                        }
                    }
                }
            }

            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : operationsMap.entrySet()) {
                Operation operation = operationEntry.getValue();
                String inlineSchemaName = getInlineSchemaName(operationEntry.getKey(), pathname);
                flattenPathItemParameters(inlineSchemaName, operation, path);
                flattenParameters(inlineSchemaName, operation);
                flattenRequestBody(inlineSchemaName, operation);
                flattenResponses(inlineSchemaName, operation);
            }
        }
    }

    private void flattenResponses(String modelName, Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return;
        }

        for (Map.Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String key = responsesEntry.getKey();
            ApiResponse response = responsesEntry.getValue();

            if (response.get$ref() != null && response.get$ref().startsWith("#/components/responses/")) {
                response = openAPI.getComponents().getResponses().get(response.get$ref().substring("#/components/responses/".length()));
            }

            flattenContent(response.getContent(),
                    (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_" + key + "_response");
        }
    }

    private void flattenRequestBody(String modelName, Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) {
            return;
        }

        // unalias $ref
        if (requestBody.get$ref() != null) {
            String ref = ModelUtils.getSimpleRef(requestBody.get$ref());
            requestBody = openAPI.getComponents().getRequestBodies().get(ref);

            if (requestBody == null) {
                return;
            }
        }

        flattenContent(requestBody.getContent(),
                (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_request");
    }

    private void flattenContent(Content content, String name) {
        if (content == null || content.isEmpty()) {
            return;
        }

        for (String contentType : content.keySet()) {
            MediaType mediaType = content.get(contentType);
            if (mediaType == null) {
                continue;
            }
            Schema schema = mediaType.getSchema();
            if (schema == null) {
                continue;
            }
            normalizeArraySchema(schema);

            String schemaName = resolveModelName(schema.getTitle(), name); // name example: testPost_request
            // Recursively gather/make inline models within this schema if any
            gatherInlineModels(schema, schemaName);
            if (isModelNeeded(schema)) {
                // If this schema should be split into its own model, do so
                //Schema refSchema = this.makeSchema(schemaName, schema);
                mediaType.setSchema(this.makeSchemaInComponents(schemaName, schema));
            }
            if (schema.getItems() != null && schema.getItems().get$ref() == null) {
                String itemsSchemaName = resolveModelName(schema.getTitle(), name); // name example: testPost_request
                // Recursively gather/make inline models within this schema if any
                gatherInlineModels(schema, itemsSchemaName);
                if (isModelNeeded(schema)) {
                    // If this schema should be split into its own model, do so
                    //Schema refSchema = this.makeSchema(schemaName, schema);
                    schema.setItems(this.makeSchemaInComponents(itemsSchemaName, schema));
                }
            }
        }
    }

    private void flattenParameters(String modelName, Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return;
        }

        for (Parameter parameter : parameters) {
            Schema parameterSchema = parameter.getSchema();
            if (parameterSchema == null && parameter.get$ref() == null) {
                continue;
            }

            if (parameter.get$ref() != null && parameter.get$ref().startsWith("#/components/parameters/")) {
                if (openAPI.getComponents().getParameters() != null) {
                    parameter = openAPI.getComponents().getParameters().get(parameter.get$ref().substring("#/components/parameters/".length()));
                }
            }
            parameterSchema = parameter.getSchema();

            if (parameterSchema == null) {
                continue;
            }

            normalizeArraySchema(parameterSchema);

            String schemaName = resolveModelName(parameterSchema.getTitle(),
                    (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_" + parameter.getName() + "_parameter");
            // Recursively gather/make inline models within this schema if any
            gatherInlineModels(parameterSchema, schemaName);
            if (isModelNeeded(parameterSchema)) {
                // If this schema should be split into its own model, do so
                parameter.setSchema(this.makeSchemaInComponents(schemaName, parameterSchema));
            }

            var parameterItemsSchema = parameterSchema.getItems();
            if (parameterItemsSchema != null && parameterItemsSchema.get$ref() == null) {
                String itemsSchemaName = resolveModelName(parameterItemsSchema.getTitle(),
                        (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_" + parameter.getName() + "_parameter");
                // Recursively gather/make inline models within this schema if any
                gatherInlineModels(parameterItemsSchema, itemsSchemaName);
                if (isModelNeeded(parameterItemsSchema)) {
                    // If this schema should be split into its own model, do so
                    parameterItemsSchema.setItems(this.makeSchemaInComponents(itemsSchemaName, parameterItemsSchema));
                }
            }
        }
    }

    private void flattenPathItemParameters(String modelName, Operation operation, PathItem pathItem) {
        List<Parameter> parameters = new ArrayList<>();
        if (pathItem.getParameters() != null) {
            parameters.addAll(pathItem.getParameters());
        }
        if (parameters.isEmpty()) {
            return;
        }

        for (Parameter parameter : parameters) {
            if (parameter.getSchema() == null && parameter.get$ref() == null) {
                continue;
            }

            Schema parameterSchema = null;
            if (parameter.get$ref() != null && parameter.get$ref().startsWith("#/components/parameters/")) {
                if (openAPI.getComponents().getParameters() != null) {
                    var param = openAPI.getComponents().getParameters().get(parameter.get$ref().substring("#/components/parameters/".length()));
                    parameterSchema = param.getSchema();
                }
            } else {
                parameterSchema = parameter.getSchema();
            }

            if (parameterSchema == null) {
                continue;
            }

            normalizeArraySchema(parameterSchema);

            String schemaName = resolveModelName(parameterSchema.getTitle(),
                (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_" + parameter.getName() + "_parameter");
            // Recursively gather/make inline models within this schema if any
            gatherInlineModels(parameterSchema, schemaName);
            if (isModelNeeded(parameterSchema)) {
                // If this schema should be split into its own model, do so
                Schema refSchema = makeSchemaInComponents(schemaName, parameterSchema);
                parameter.setSchema(refSchema);
            }
            var parameterItemsSchema = parameterSchema.getItems();
            if (parameterItemsSchema != null && parameterItemsSchema.get$ref() == null) {
                String itemsSchemaName = resolveModelName(parameterItemsSchema.getTitle(),
                        (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_" + parameter.getName() + "_parameter");
                // Recursively gather/make inline models within this schema if any
                gatherInlineModels(parameterItemsSchema, itemsSchemaName);
                if (isModelNeeded(parameterItemsSchema)) {
                    // If this schema should be split into its own model, do so
                    parameterSchema.setItems(this.makeSchemaInComponents(itemsSchemaName, parameterItemsSchema));
                }
            }
        }
    }

    /**
     * Recursively gather inline models that need to be generated and
     * replace inline schemas with $ref to schema to-be-generated.
     *
     * @param schema target schema
     * @param modelPrefix model name (usually the prefix of the inline model name)
     */
    private void gatherInlineModels(Schema schema, String modelPrefix) {
        if (schema.get$ref() != null) {
            // if ref already, no inline schemas should be present but check for
            // any to catch OpenAPI violations
            if (isModelNeeded(schema) || "object".equals(schema.getType()) ||
                schema.getProperties() != null || schema.getAdditionalProperties() != null ||
                ModelUtils.isComposedSchema(schema)) {
                LOGGER.error("Illegal schema found with $ref combined with other properties, no properties should be defined alongside a $ref:\n {}", schema);
            }
            return;
        }
        // Check object models / any type models / composed models for properties,
        // if the schema has a type defined that is not "object" it should not define
        // any properties
        if (schema.getType() == null || "object".equals(schema.getType())) {
            // Check properties and recurse, each property could be its own inline model
            Map<String, Schema> props = schema.getProperties();
            if (props != null) {
                for (String propName : props.keySet()) {
                    Schema prop = props.get(propName);

                    if (prop == null) {
                        continue;
                    }

                    normalizeArraySchema(prop);

                    String schemaName = resolveModelName(prop.getTitle(), modelPrefix + "_" + propName);
                    // Recurse to create $refs for inner models
                    gatherInlineModels(prop, schemaName);
                    if (isModelNeeded(prop)) {
                        // If this schema should be split into its own model, do so
                        Schema refSchema = makeSchemaInComponents(schemaName, prop);
                        props.put(propName, refSchema);
                    } else if (ModelUtils.isComposedSchema(prop)) {
                        if (prop.getAllOf() != null && prop.getAllOf().size() == 1 &&
                            !(((Schema) prop.getAllOf().get(0)).getType() == null ||
                                "object".equals(((Schema) prop.getAllOf().get(0)).getType()))) {
                            // allOf with only 1 type (non-model)
                            LOGGER.info("allOf schema used by the property `{}` replaced by its only item (a type)", propName);
                            props.put(propName, (Schema) prop.getAllOf().get(0));
                        }
                    }
                }
            }
            // Check additionalProperties for inline models
            if (schema.getAdditionalProperties() != null
                && schema.getAdditionalProperties() instanceof Schema<?> inner) {
                String schemaName = resolveModelName(schema.getTitle(), modelPrefix + inlineSchemaOptions.get("MAP_ITEM_SUFFIX"));
                // Recurse to create $refs for inner models
                gatherInlineModels(inner, schemaName);
                if (isModelNeeded(inner)) {
                    // If this schema should be split into its own model, do so
                    Schema refSchema = makeSchemaInComponents(schemaName, inner);
                    schema.setAdditionalProperties(refSchema);
                }
            }
            if (schema.getItems() != null && schema.getItems().get$ref() == null) {
                String schemaName = resolveModelName(schema.getTitle(), modelPrefix + "Enum");
                // Recurse to create $refs for inner models
                gatherInlineModels(schema.getItems(), schemaName);
                if (isModelNeeded(schema.getItems())) {
                    // If this schema should be split into its own model, do so
                    Schema refSchema = makeSchemaInComponents(schemaName, schema.getItems());
                    schema.setAdditionalProperties(refSchema);
                }
            }
        } else if (schema.getProperties() != null) {
            // If non-object type is specified but also properties
            LOGGER.error("Illegal schema found with non-object type combined with properties, no properties should be defined:\n {}", schema);
            return;
        } else if (schema.getAdditionalProperties() != null) {
            // If non-object type is specified but also additionalProperties
            LOGGER.error("Illegal schema found with non-object type combined with additionalProperties, no additionalProperties should be defined:\n {}", schema);
            return;
        }
        // Check array items
        if (schema instanceof ArraySchema array) {
            var items = array.getItems();
            if (items == null) {
                LOGGER.error("Illegal schema found with array type but no items, items must be defined for array schemas:\n {}", schema);
                return;
            }
            String schemaName = resolveModelName(items.getTitle(), modelPrefix + inlineSchemaOptions.get("ARRAY_ITEM_SUFFIX"));

            // Recurse to create $refs for inner models
            gatherInlineModels(items, schemaName);

            if (isModelNeeded(items)) {
                // If this schema should be split into its own model, do so
                Schema refSchema = makeSchemaInComponents(schemaName, items);
                array.setItems(refSchema);
            }
        }
        // Check allOf, anyOf, oneOf for inline models
        if (ModelUtils.isComposedSchema(schema)) {
            if (schema.getAllOf() != null) {
                List<Schema> newAllOf = new ArrayList<>();
                boolean atLeastOneModel = false;
                for (Object inner : schema.getAllOf()) {
                    if (inner == null) {
                        continue;
                    }

                    normalizeArraySchema((Schema) inner);

                    String schemaName = resolveModelName(((Schema) inner).getTitle(), modelPrefix + "_allOf");
                    // Recurse to create $refs for inner models
                    gatherInlineModels((Schema) inner, schemaName);
                    if (isModelNeeded((Schema) inner)) {
                        if (Boolean.TRUE.equals(refactorAllOfInlineSchemas)) {
                            Schema refSchema = makeSchemaInComponents(schemaName, (Schema) inner);
                            newAllOf.add(refSchema); // replace with ref
                            atLeastOneModel = true;
                        } else { // do not refactor allOf inline schemas
                            newAllOf.add((Schema) inner);
                            atLeastOneModel = true;
                        }
                    } else {
                        newAllOf.add((Schema) inner);
                    }
                }
                if (atLeastOneModel) {
                    schema.setAllOf(newAllOf);
                } else {
                    // allOf is just one or more types only so do not generate the inline allOf model
                    if (schema.getAllOf().size() > 1) {
                        LOGGER.warn("allOf schema `{}` containing multiple types (not model) is not supported at the moment.", schema.getName());
                    } else if (schema.getAllOf().size() != 1) {
                        // handle earlier in this function when looping through properties
                        LOGGER.error("allOf schema `{}` contains no items.", schema.getName());
                    }
                }
            }
            if (schema.getAnyOf() != null) {
                List<Schema> newAnyOf = new ArrayList<>();
                for (Object inner : schema.getAnyOf()) {
                    if (inner == null) {
                        continue;
                    }

                    normalizeArraySchema((Schema) inner);
                    String schemaName = resolveModelName(((Schema) inner).getTitle(), modelPrefix + "_anyOf");
                    // Recurse to create $refs for inner models
                    gatherInlineModels((Schema) inner, schemaName);
                    if (isModelNeeded((Schema) inner)) {
                        Schema refSchema = makeSchemaInComponents(schemaName, (Schema) inner);
                        newAnyOf.add(refSchema); // replace with ref
                    } else {
                        newAnyOf.add((Schema) inner);
                    }
                }
                schema.setAnyOf(newAnyOf);
            }
            if (schema.getOneOf() != null) {
                List<Schema> newOneOf = new ArrayList<>();
                for (Object inner : schema.getOneOf()) {
                    if (inner == null) {
                        continue;
                    }

                    normalizeArraySchema((Schema) inner);
                    String schemaName = resolveModelName(((Schema) inner).getTitle(), modelPrefix + "_oneOf");
                    // Recurse to create $refs for inner models
                    gatherInlineModels((Schema) inner, schemaName);
                    if (isModelNeeded((Schema) inner)) {
                        Schema refSchema = makeSchemaInComponents(schemaName, (Schema) inner);
                        newOneOf.add(refSchema); // replace with ref
                    } else {
                        newOneOf.add((Schema) inner);
                    }
                }
                schema.setOneOf(newOneOf);
            }
        }
        // Check not schema
        if (schema.getNot() != null) {
            Schema not = schema.getNot();
            if (not != null) {
                String schemaName = resolveModelName(schema.getTitle(), modelPrefix + "_not");
                // Recurse to create $refs for inner models
                gatherInlineModels(not, schemaName);
                if (isModelNeeded(not)) {
                    Schema refSchema = makeSchemaInComponents(schemaName, not);
                    schema.setNot(refSchema);
                }
            }
        }
    }

    private String resolveModelName(String title, String key) {
        return title == null ? uniqueName(key) : uniqueName(title);
    }

    private String getInlineSchemaName(PathItem.HttpMethod httpVerb, String pathname) {
        if (pathname.startsWith("/")) {
            pathname = pathname.substring(1);
        }
        String name = pathname.replace('/', '_')
            .replaceAll("[{}]", "");

        if (httpVerb == PathItem.HttpMethod.DELETE) {
            name += "_delete";
        } else if (httpVerb == PathItem.HttpMethod.GET) {
            name += "_get";
        } else if (httpVerb == PathItem.HttpMethod.HEAD) {
            name += "_head";
        } else if (httpVerb == PathItem.HttpMethod.OPTIONS) {
            name += "_options";
        } else if (httpVerb == PathItem.HttpMethod.PATCH) {
            name += "_patch";
        } else if (httpVerb == PathItem.HttpMethod.POST) {
            name += "_post";
        } else if (httpVerb == PathItem.HttpMethod.PUT) {
            name += "_put";
        } else if (httpVerb == PathItem.HttpMethod.TRACE) {
            name += "_trace";
        }
        return name;
    }

    /**
     * Move schema to components (if new) and return $ref to schema or
     * existing schema.
     *
     * @param name new schema name
     * @param schema schema to move to components or find existing ref
     *
     * @return {@link Schema} $ref schema to new or existing schema
     */
    private Schema makeSchemaInComponents(String name, Schema schema) {
        String existing = matchGenerated(schema);
        Schema refSchema;
        if (existing != null) {
            refSchema = new Schema().$ref(existing);
        } else {
            if (resolveInlineEnums && schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                LOGGER.warn("Model {} promoted to its own schema due to resolveInlineEnums=true", name);
            }
            name = addSchemas(name, schema);
            refSchema = new Schema().$ref(name);
        }
        copyVendorExtensions(schema, refSchema);

        return refSchema;
    }

    /**
     * Copy vendor extensions from Model to another Model.
     *
     * @param source source property
     * @param target target property
     */
    private void copyVendorExtensions(Schema source, Schema target) {
        Map<String, Object> vendorExtensions = source.getExtensions();
        if (vendorExtensions == null) {
            return;
        }
        for (String extName : vendorExtensions.keySet()) {
            target.addExtension(extName, vendorExtensions.get(extName));
        }
    }

    private String matchGenerated(Schema model) {
        if (skipSchemaReuse) { // skip reusing schema
            return null;
        }
        try {
            String json = STRUCTURE_MAPPER.writeValueAsString(model);
            if (generatedSignature.containsKey(json)) {
                return generatedSignature.get(json);
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Return false if model can be represented by primitives e.g. string, object
     * without properties, array or map of other model (model container), etc.
     * <p>
     * Return true if a model should be generated e.g. object with properties,
     * enum, oneOf, allOf, anyOf, etc.
     *
     * @param schema target schema
     */
    private boolean isModelNeeded(Schema schema) {
        return isModelNeeded(schema, new HashSet<>());
    }

    /**
     * Return false if model can be represented by primitives e.g. string, object
     * without properties, array or map of other model (model container), etc.
     * <p>
     * Return true if a model should be generated e.g. object with properties,
     * enum, oneOf, allOf, anyOf, etc.
     *
     * @param schema target schema
     * @param visitedSchemas Visited schemas
     */
    private boolean isModelNeeded(Schema schema, Set<Schema> visitedSchemas) {
        if (visitedSchemas.contains(schema)) { // circular reference
            return true;
        } else {
            visitedSchemas.add(schema);
        }

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return true;
        }
        if (schema.getType() == null || "object".equals(schema.getType())) {
            // object or undeclared type with properties
            if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                return true;
            }
        }
        if (ModelUtils.isComposedSchema(schema)) {
            // allOf, anyOf, oneOf
            boolean isSingleAllOf = schema.getAllOf() != null && schema.getAllOf().size() == 1;
            boolean isReadOnly = schema.getReadOnly() != null && schema.getReadOnly();
            boolean isNullable = schema.getNullable() != null && schema.getNullable();

            if (isSingleAllOf && (isReadOnly || isNullable)) {
                // Check if this composed schema only contains an allOf and a readOnly or nullable.
                var c = new ComposedSchema();
                c.setAllOf(schema.getAllOf());
                c.setReadOnly(schema.getReadOnly());
                c.setNullable(schema.getNullable());
                if (schema.equals(c)) {
                    return isModelNeeded((Schema) schema.getAllOf().get(0), visitedSchemas);
                }
            } else if (isSingleAllOf && StringUtils.isNotEmpty(((Schema) schema.getAllOf().get(0)).get$ref())) {
                // single allOf and it's a ref
                return isModelNeeded((Schema) schema.getAllOf().get(0), visitedSchemas);
            }

            if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
                // check to ensure at least one of the allOf item is model
                for (Object inner : schema.getAllOf()) {
                    if (isModelNeeded(ModelUtils.getReferencedSchema(openAPI, (Schema) inner), visitedSchemas)) {
                        return true;
                    }
                }
                // allOf items are all non-model (e.g. type: string) only
                return false;
            }

            if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
                return true;
            }
            return schema.getOneOf() != null && !schema.getOneOf().isEmpty();
        }

        return false;
    }

    /**
     * Add the schemas to the components.
     *
     * @param name name of the inline schema
     * @param schema inline schema
     *
     * @return the actual model name (based on inlineSchemaNameMapping if provided)
     */
    private String addSchemas(String name, Schema schema) {
        //check inlineSchemaNameMapping
        if (inlineSchemaNameMapping.containsKey(name)) {
            name = inlineSchemaNameMapping.get(name);
        }

        addGenerated(name, schema);
        openAPI.getComponents().addSchemas(name, schema);
        if (!name.equals(schema.getTitle()) && !inlineSchemaNameMappingValues.contains(name)) {
            LOGGER.info("Inline schema created as {}. To have complete control of the model name, set the `title` field or use the modelNameMapping option (e.g. --model-name-mappings {}=NewModel,ModelA=NewModelA in CLI) or inlineSchemaNameMapping option (--inline-schema-name-mappings {}=NewModel,ModelA=NewModelA in CLI).", name, name, name);
        }

        uniqueNames.add(name);

        return name;
    }

    private void addGenerated(String name, Schema model) {
        try {
            String json = STRUCTURE_MAPPER.writeValueAsString(model);
            generatedSignature.put(json, name);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error: {}", e.getMessage());
        }
    }

    /**
     * Generate a unique name for the input.
     *
     * @param name name to be processed to make sure it's unique
     */
    private String uniqueName(final String name) {
        if (openAPI.getComponents().getSchemas() == null) { // no schema has been created
            return name;
        }

        String uniqueName = name;
        int count = 0;
        while (true) {
            if (!openAPI.getComponents().getSchemas().containsKey(uniqueName) && !uniqueNames.contains(uniqueName)) {
                return uniqueName;
            }
            uniqueName = name + "_" + ++count;
        }
    }

    /**
     * Flatten inline models in components.
     */
    private void flattenComponents() {
        Map<String, Schema> models = openAPI.getComponents().getSchemas();
        if (models == null) {
            return;
        }

        var modelNames = new ArrayList<>(models.keySet());
        for (String modelName : modelNames) {
            Schema model = models.get(modelName);
            if (model == null) {
                continue;
            }
            normalizeArraySchema(model);
            if (ModelUtils.isAnyOf(model)) { // contains anyOf only
                gatherInlineModels(model, modelName);
            } else if (ModelUtils.isOneOf(model)) { // contains oneOf only
                gatherInlineModels(model, modelName);
            } else if (ModelUtils.isComposedSchema(model)) {
                // inline child schemas
                flattenComposedChildren(modelName + "_allOf", model.getAllOf(), !Boolean.TRUE.equals(this.refactorAllOfInlineSchemas));
                flattenComposedChildren(modelName + "_anyOf", model.getAnyOf(), false);
                flattenComposedChildren(modelName + "_oneOf", model.getOneOf(), false);
            } else {
                gatherInlineModels(model, modelName);
            }
        }
    }

    private void flattenComposedChildren(String key, List<Schema> children, boolean skipAllOfInlineSchemas) {
        if (children == null || children.isEmpty()) {
            return;
        }
        ListIterator<Schema> listIterator = children.listIterator();
        while (listIterator.hasNext()) {
            Schema component = listIterator.next();
            if ((component != null) &&
                    (component.get$ref() == null) &&
                    ((component.getProperties() != null && !component.getProperties().isEmpty()) ||
                            (component.getEnum() != null && !component.getEnum().isEmpty()))) {
                // If a `title` attribute is defined in the inline schema, codegen uses it to name the
                // inline schema. Otherwise, we'll use the default naming such as InlineObject1, etc.
                // We know that this is not the best way to name the model.
                //
                // Such naming strategy may result in issues. If the value of the 'title' attribute
                // happens to match a schema defined elsewhere in the specification, 'innerModelName'
                // will be the same as that other schema.
                //
                // To have complete control of the model naming, one can define the model separately
                // instead of inline.
                String innerModelName = resolveModelName(component.getTitle(), key);
                Schema innerModel = modelFromProperty(openAPI, component, innerModelName);
                // Recurse to create $refs for inner models
                gatherInlineModels(innerModel, innerModelName);
                if (!skipAllOfInlineSchemas) {
                    String existing = matchGenerated(innerModel);
                    if (existing == null) {
                        innerModelName = addSchemas(innerModelName, innerModel);
                        Schema schema = new Schema().$ref(innerModelName);
                        schema.setRequired(component.getRequired());
                        listIterator.set(schema);
                    } else {
                        Schema schema = new Schema().$ref(existing);
                        schema.setRequired(component.getRequired());
                        listIterator.set(schema);
                    }
                } else {
                    LOGGER.debug("Inline allOf schema {} not refactored into a separate model using $ref.", innerModelName);
                }
            }
        }
    }

    private Schema modelFromProperty(OpenAPI openAPI, Schema object, String path) {
        String description = object.getDescription();
        String example = null;
        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        XML xml = object.getXml();
        Map<String, Schema> properties = object.getProperties();

        // NOTE:
        // No need to null check setters below. All defaults in the new'd Schema are null, so setting to null would just be a noop.
        Schema model = new Schema();
        model.setType(object.getType());

        // Even though the `format` keyword typically applies to primitive types only,
        // the JSON schema specification states `format` can be used for any model type instance
        // including object types.
        model.setFormat(object.getFormat());

        if (object.getExample() != null) {
            model.setExample(example);
        }
        model.setDescription(description);
        model.setName(object.getName());
        model.setXml(xml);
        model.setRequired(object.getRequired());
        model.setNullable(object.getNullable());
        model.setEnum(object.getEnum());
        model.setType(object.getType());
        model.setDiscriminator(object.getDiscriminator());
        model.setWriteOnly(object.getWriteOnly());
        model.setUniqueItems(object.getUniqueItems());
        model.setTitle(object.getTitle());
        model.setReadOnly(object.getReadOnly());
        model.setPattern(object.getPattern());
        model.setNot(object.getNot());
        model.setMinProperties(object.getMinProperties());
        model.setMinLength(object.getMinLength());
        model.setMinItems(object.getMinItems());
        model.setMinimum(object.getMinimum());
        model.setMaxProperties(object.getMaxProperties());
        model.setMaxLength(object.getMaxLength());
        model.setMaxItems(object.getMaxItems());
        model.setMaximum(object.getMaximum());
        model.setExternalDocs(object.getExternalDocs());
        model.setExtensions(object.getExtensions());
        model.setExclusiveMinimum(object.getExclusiveMinimum());
        model.setExclusiveMaximum(object.getExclusiveMaximum());
        // no need to set example again as it's set earlier
        model.setDeprecated(object.getDeprecated());

        if (properties != null) {
            flattenProperties(openAPI, properties, path);
            model.setProperties(properties);
        }
        return model;
    }

    private void flattenProperties(OpenAPI openAPI, Map<String, Schema> properties, String path) {
        if (properties == null) {
            return;
        }
        Map<String, Schema> propsToUpdate = new HashMap<>();
        Map<String, Schema> modelsToAdd = new HashMap<>();
        for (Map.Entry<String, Schema> propertiesEntry : properties.entrySet()) {
            String key = propertiesEntry.getKey();
            Schema property = propertiesEntry.getValue();
            if (ModelUtils.isObjectSchema(property)) {
                Schema op = property;
                String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                Schema model = modelFromProperty(openAPI, op, modelName);
                String existing = matchGenerated(model);
                if (existing != null) {
                    Schema schema = new Schema().$ref(existing);
                    schema.setRequired(op.getRequired());
                    propsToUpdate.put(key, schema);
                } else {
                    modelName = addSchemas(modelName, model);
                    Schema schema = new Schema().$ref(modelName);
                    schema.setRequired(op.getRequired());
                    propsToUpdate.put(key, schema);
                    modelsToAdd.put(modelName, model);
                }
            } else if (ModelUtils.isArraySchema(property)) {
                Schema inner = ModelUtils.getSchemaItems(property);
                if (ModelUtils.isObjectSchema(inner)) {
                    Schema op = inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(openAPI, op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Schema innerModel = modelFromProperty(openAPI, op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            Schema schema = new Schema().$ref(existing);
                            schema.setRequired(op.getRequired());
                            property.setItems(schema);
                        } else {
                            modelName = addSchemas(modelName, innerModel);
                            Schema schema = new Schema().$ref(modelName);
                            schema.setRequired(op.getRequired());
                            property.setItems(schema);
                        }
                    }
                } else if (ModelUtils.isComposedSchema(inner)) {
                    String innerModelName = resolveModelName(inner.getTitle(), path + "_" + key);
                    gatherInlineModels(inner, innerModelName);
                    innerModelName = addSchemas(innerModelName, inner);
                    Schema schema = new Schema().$ref(innerModelName);
                    schema.setRequired(inner.getRequired());
                    property.setItems(schema);
                } else {
                    LOGGER.debug("Schema not yet handled in model resolver: {}", inner);
                }
            } else if (ModelUtils.isMapSchema(property)) {
                Schema inner = ModelUtils.getAdditionalProperties(property);
                if (ModelUtils.isObjectSchema(inner)) {
                    Schema op = inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(openAPI, op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Schema innerModel = modelFromProperty(openAPI, op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            Schema schema = new Schema().$ref(existing);
                            schema.setRequired(op.getRequired());
                            property.setAdditionalProperties(schema);
                        } else {
                            modelName = addSchemas(modelName, innerModel);
                            Schema schema = new Schema().$ref(modelName);
                            schema.setRequired(op.getRequired());
                            property.setAdditionalProperties(schema);
                        }
                    }
                } else if (ModelUtils.isComposedSchema(inner)) {
                    String innerModelName = resolveModelName(inner.getTitle(), path + "_" + key);
                    gatherInlineModels(inner, innerModelName);
                    innerModelName = addSchemas(innerModelName, inner);
                    Schema schema = new Schema().$ref(innerModelName);
                    schema.setRequired(inner.getRequired());
                    property.setAdditionalProperties(schema);
                } else {
                    LOGGER.debug("Schema not yet handled in model resolver: {}", inner);
                }
            } else if (ModelUtils.isComposedSchema(property)) { // oneOf, anyOf, allOf etc
                if (property.getAllOf() != null && property.getAllOf().size() == 1 // allOf with a single item
                        && (property.getOneOf() == null || property.getOneOf().isEmpty()) // not oneOf
                        && (property.getAnyOf() == null || property.getAnyOf().isEmpty()) // not anyOf
                        && (property.getProperties() == null || property.getProperties().isEmpty())) { // no property
                    // don't do anything if it's allOf with a single item
                    LOGGER.debug("allOf with a single item (which can be handled by default codegen) skipped by inline model resolver: {}", property);
                } else {
                    String propertyModelName = resolveModelName(property.getTitle(), path + "_" + key);
                    gatherInlineModels(property, propertyModelName);
                    propertyModelName = addSchemas(propertyModelName, property);
                    var schema = new Schema<>()
                        .$ref(propertyModelName)
                        .required(property.getRequired());
                    propsToUpdate.put(key, schema);
                }
            } else {
                LOGGER.debug("Schema not yet handled in model resolver: {}", property);
            }
        }
        if (!propsToUpdate.isEmpty()) {
            properties.putAll(propsToUpdate);
        }
        for (var entry : modelsToAdd.entrySet()) {
            openAPI.getComponents().addSchemas(entry.getKey(), entry.getValue());
            this.addedModels.put(entry.getKey(), entry.getValue());
        }
    }

}
