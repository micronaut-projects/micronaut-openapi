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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private Boolean refactorAllOfInlineSchemas; // refactor allOf inline schemas into $ref

    // a set to keep track of names generated for inline schemas
    private Set<String> uniqueNames = new HashSet<>();

    public MicronautInlineModelResolver(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public void flattenPaths() {
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            return;
        }

        for (Map.Entry<String, PathItem> pathsEntry : paths.entrySet()) {
            PathItem path = pathsEntry.getValue();
            Map<PathItem.HttpMethod, Operation> operationsMap = new LinkedHashMap<>(path.readOperationsMap());

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
            if (parameter.getSchema() == null) {
                continue;
            }

            Schema parameterSchema = parameter.getSchema();

            if (parameterSchema == null) {
                continue;
            }
            String schemaName = resolveModelName(parameterSchema.getTitle(),
                (operation.getOperationId() == null ? modelName : operation.getOperationId()) + "_" + parameter.getName() + "_parameter");
            // Recursively gather/make inline models within this schema if any
            gatherInlineModels(parameterSchema, schemaName);
            if (isModelNeeded(parameterSchema)) {
                // If this schema should be split into its own model, do so
                Schema refSchema = makeSchemaInComponents(schemaName, parameterSchema);
                parameter.setSchema(refSchema);
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
            if (schema.getItems() != null) {
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
                ComposedSchema c = new ComposedSchema();
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
}
