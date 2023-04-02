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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;

import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Some schema util methods.
 *
 * @since 4.5.0
 */
public final class SchemaUtils {

    public static final String TYPE_OBJECT = "object";

    private SchemaUtils() {
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

    public static Schema objectShemaToSchema(ObjectSchema objectSchema) {
        Schema schema = new Schema();
        schema.setDefault(objectSchema.getDefault());
        schema.setName(objectSchema.getName());
        schema.setTitle(objectSchema.getTitle());
        schema.setMultipleOf(objectSchema.getMultipleOf());
        schema.setMaximum(objectSchema.getMaximum());
        schema.setExclusiveMaximum(objectSchema.getExclusiveMaximum());
        schema.setMinimum(objectSchema.getMinimum());
        schema.setExclusiveMinimum(objectSchema.getExclusiveMinimum());
        schema.setMaxLength(objectSchema.getMaxLength());
        schema.setMinLength(objectSchema.getMinLength());
        schema.setPattern(objectSchema.getPattern());
        schema.setMaxItems(objectSchema.getMaxItems());
        schema.setMinItems(objectSchema.getMinItems());
        schema.setUniqueItems(objectSchema.getUniqueItems());
        schema.setMaxProperties(objectSchema.getMaxProperties());
        schema.setMinProperties(objectSchema.getMinProperties());
        schema.setRequired(objectSchema.getRequired());
        schema.setType(objectSchema.getType());
        schema.setNot(objectSchema.getNot());
        schema.setProperties(objectSchema.getProperties());
        schema.setAdditionalProperties(objectSchema.getAdditionalProperties());

        private String description = null;
        private String format = null;
        private String $ref = null;
        private Boolean nullable = null;
        private Boolean readOnly = null;
        private Boolean writeOnly = null;
        protected T example = null;
        private ExternalDocumentation externalDocs = null;
        private Boolean deprecated = null;
        private XML xml = null;
        private Map<String, Object> extensions = null;
        protected List<T> _enum = null;
        private Discriminator discriminator = null;
        private boolean exampleSetFlag;
        private List<Schema> prefixItems = null;
        private List<Schema> allOf = null;
        private List<Schema> anyOf = null;
        private List<Schema> oneOf = null;
        private Schema<?> items = null;
        protected T _const;
        private SpecVersion specVersion = SpecVersion.V30;
        private Set<String> types;
        private Map<String, Schema> patternProperties = null;
        private BigDecimal exclusiveMaximumValue = null;
        private BigDecimal exclusiveMinimumValue = null;
        private Schema contains = null;
        private String $id;
        private String $schema;
        private String $anchor;
        private String contentEncoding;
        private String contentMediaType;
        private Schema contentSchema;
        private Schema propertyNames;
        private Schema unevaluatedProperties;
        private Integer maxContains;
        private Integer minContains;
        private Schema additionalItems;
        private Schema unevaluatedItems;
        private Schema _if;
        private Schema _else;
        private Schema then;
        private Map<String, Schema> dependentSchemas;
        private Map<String, List<String>> dependentRequired;
        private String $comment;
        private List<T> examples;
        private Boolean booleanSchemaValue;
    }
}
