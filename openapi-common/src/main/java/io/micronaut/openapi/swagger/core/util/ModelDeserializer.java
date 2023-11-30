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
package io.micronaut.openapi.swagger.core.util;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.SimpleSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class ModelDeserializer extends JsonDeserializer<Schema> {

    protected boolean openapi31;

    @Override
    public Schema deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Schema schema = null;

        if (openapi31) {
            schema = deserializeJsonSchema(node);
            return schema;
        }
        if (node.isBoolean()) {
            return new Schema().booleanSchemaValue(node.booleanValue());
        }

        List<String> composed = List.of("allOf", "anyOf", "oneOf");
        for (String field : composed) {
            if (node.get(field) != null) {
                return OpenApiUtils.getJsonMapper().convertValue(node, ComposedSchema.class);
            }
        }

        JsonNode type = node.get("type");
        String format = node.get("format") == null ? "" : node.get("format").textValue();

        if (type != null && "array".equals(type.textValue())) {
            schema = OpenApiUtils.getJsonMapper().convertValue(node, ArraySchema.class);
        } else if (type != null) {
            if (type.textValue().equals("integer")) {
                schema = OpenApiUtils.getJsonMapper().convertValue(node, IntegerSchema.class);
                if (format == null || format.isBlank()) {
                    schema.setFormat(null);
                }
            } else if (type.textValue().equals("number")) {
                schema = OpenApiUtils.getJsonMapper().convertValue(node, NumberSchema.class);
            } else if (type.textValue().equals("boolean")) {
                schema = OpenApiUtils.getJsonMapper().convertValue(node, BooleanSchema.class);
            } else if (type.textValue().equals("string")) {
                if ("date".equals(format)) {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, DateSchema.class);
                } else if ("date-time".equals(format)) {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, DateTimeSchema.class);
                } else if ("email".equals(format)) {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, EmailSchema.class);
                } else if ("password".equals(format)) {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, PasswordSchema.class);
                } else if ("uuid".equals(format)) {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, UUIDSchema.class);
                } else {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, StringSchema.class);
                }
            } else if (type.textValue().equals("object")) {
                schema = deserializeObjectSchema(node, true);
            }
        } else if (node.get("$ref") != null) {
            schema = new Schema().$ref(node.get("$ref").asText());
        } else { // assume object
            schema = deserializeObjectSchema(node, false);
        }

        return schema;
    }

    private Schema deserializeObjectSchema(JsonNode node, boolean withType) {
        JsonNode additionalProperties = node.get("additionalProperties");
        Schema schema;
        if (additionalProperties != null) {
            if (additionalProperties.isBoolean()) {
                Boolean additionalPropsBoolean = OpenApiUtils.getJsonMapper().convertValue(additionalProperties, Boolean.class);
                ((ObjectNode) node).remove("additionalProperties");
                if (additionalPropsBoolean) {
                    schema = OpenApiUtils.getJsonMapper().convertValue(node, MapSchema.class);
                } else {
                    if (withType) {
                        schema = OpenApiUtils.getJsonMapper().convertValue(node, ObjectSchema.class);
                    } else {
                        schema = OpenApiUtils.getJsonMapper().convertValue(node, SimpleSchema.class);
                    }
                }
                schema.setAdditionalProperties(additionalPropsBoolean);
            } else {
                Schema innerSchema = OpenApiUtils.getJsonMapper().convertValue(additionalProperties, Schema.class);
                ((ObjectNode) node).remove("additionalProperties");
                MapSchema ms = OpenApiUtils.getJsonMapper().convertValue(node, MapSchema.class);
                ms.setAdditionalProperties(innerSchema);
                schema = ms;
            }
        } else {
            if (withType) {
                schema = OpenApiUtils.getJsonMapper().convertValue(node, ObjectSchema.class);
            } else {
                schema = OpenApiUtils.getJsonMapper().convertValue(node, SimpleSchema.class);
            }
        }
        if (schema != null) {
            try {
                schema.jsonSchema(OpenApiUtils.getJsonMapper31().readValue(OpenApiUtils.getJsonMapper31().writeValueAsString(node), Map.class));
            } catch (JsonProcessingException e) {
                System.err.println("Exception converting jsonSchema to Map " + e.getMessage());
            }
        }
        return schema;
    }

    private Schema deserializeJsonSchema(JsonNode node) {
        if (node.isBoolean()) {
            return new Schema().booleanSchemaValue(node.booleanValue());
        }
        JsonNode additionalProperties = node.get("additionalProperties");
        JsonNode type = node.get("type");
        Schema schema;

        if (type != null || additionalProperties != null) {
            if (type != null) {
                ((ObjectNode) node).remove("type");
            }
            if (additionalProperties != null) {
                ((ObjectNode) node).remove("additionalProperties");
            }
            schema = OpenApiUtils.getJsonMapper31().convertValue(node, JsonSchema.class);
            if (type instanceof TextNode) {
                schema.types(new LinkedHashSet<>(Collections.singletonList(type.textValue())));
            } else if (type instanceof ArrayNode) {
                Set<String> types = new LinkedHashSet<>();
                type.elements().forEachRemaining(n -> types.add(n.textValue()));
                schema.types(types);
            }
            if (additionalProperties != null) {
                try {
                    if (additionalProperties.isBoolean()) {
                        schema.setAdditionalProperties(additionalProperties.booleanValue());
                    } else {
                        Schema innerSchema = deserializeJsonSchema(additionalProperties);
                        schema.setAdditionalProperties(innerSchema);
                    }
                } catch (Exception e) {
                    Boolean additionalPropsBoolean = OpenApiUtils.getJsonMapper31().convertValue(additionalProperties, Boolean.class);
                    schema.setAdditionalProperties(additionalPropsBoolean);
                }
            }

        } else {
            schema = OpenApiUtils.getJsonMapper31().convertValue(node, JsonSchema.class);
        }
        return schema;
    }
}
