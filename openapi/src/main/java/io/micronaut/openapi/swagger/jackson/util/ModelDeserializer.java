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
package io.micronaut.openapi.swagger.jackson.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micronaut.core.util.StringUtils;
import io.micronaut.openapi.visitor.ConvertUtils;
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


        List<String> composed = Arrays.asList("allOf", "anyOf", "oneOf");
        for (String field : composed) {
            if (node.get(field) != null) {
                return ConvertUtils.getJsonMapper().convertValue(node, ComposedSchema.class);
            }
        }

        JsonNode type = node.get("type");
        String format = node.get("format") == null ? "" : node.get("format").textValue();

        if (type != null && "array".equals(((TextNode) type).textValue())) {
            schema = ConvertUtils.getJsonMapper().convertValue(node, ArraySchema.class);
        } else if (type != null) {
            if (type.textValue().equals("integer")) {
                schema = ConvertUtils.getJsonMapper().convertValue(node, IntegerSchema.class);
                if (!StringUtils.hasText(format)) {
                    schema.setFormat(null);
                }
            } else if (type.textValue().equals("number")) {
                schema = ConvertUtils.getJsonMapper().convertValue(node, NumberSchema.class);
            } else if (type.textValue().equals("boolean")) {
                schema = ConvertUtils.getJsonMapper().convertValue(node, BooleanSchema.class);
            } else if (type.textValue().equals("string")) {
                if ("date".equals(format)) {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, DateSchema.class);
                } else if ("date-time".equals(format)) {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, DateTimeSchema.class);
                } else if ("email".equals(format)) {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, EmailSchema.class);
                } else if ("password".equals(format)) {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, PasswordSchema.class);
                } else if ("uuid".equals(format)) {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, UUIDSchema.class);
                } else {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, StringSchema.class);
                }
            } else if (type.textValue().equals("object")) {
                schema = deserializeObjectSchema(node);
            }
        } else if (node.get("$ref") != null) {
            schema = new Schema().$ref(node.get("$ref").asText());
        } else { // assume object
            schema = deserializeObjectSchema(node);
        }

        return schema;
    }

    private Schema deserializeObjectSchema(JsonNode node) {
        JsonNode additionalProperties = node.get("additionalProperties");
        Schema schema = null;
        if (additionalProperties != null) {
            // try first to convert to Schema, if it fails it must be a boolean
            try {
                Schema innerSchema = ConvertUtils.getJsonMapper().convertValue(additionalProperties, Schema.class);
                ((ObjectNode) node).remove("additionalProperties");
                MapSchema ms = ConvertUtils.getJsonMapper().convertValue(node, MapSchema.class);
                ms.setAdditionalProperties(innerSchema);
                schema = ms;
            } catch (Exception e) {
                Boolean additionalPropsBoolean = ConvertUtils.getJsonMapper().convertValue(additionalProperties, Boolean.class);
                if (additionalPropsBoolean) {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, MapSchema.class);
                } else {
                    schema = ConvertUtils.getJsonMapper().convertValue(node, ObjectSchema.class);
                }
                schema.setAdditionalProperties(additionalPropsBoolean);
            }

        } else {
            schema = ConvertUtils.getJsonMapper().convertValue(node, ObjectSchema.class);
        }
        if (schema != null) {
            try {
                schema.jsonSchema(ConvertUtils.getJsonMapper31().readValue(ConvertUtils.getJsonMapper31().writeValueAsString(node), Map.class));
            } catch (JsonProcessingException e) {
                System.err.println("Exception converting jsonSchema to Map " + e.getMessage());
                e.printStackTrace();
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
            schema = ConvertUtils.getJsonMapper31().convertValue(node, JsonSchema.class);
            if (type instanceof TextNode) {
                schema.types(new LinkedHashSet<>(Collections.singletonList(type.textValue())));
            } else if (type instanceof ArrayNode) {
                Set<String> types = new LinkedHashSet<>();
                type.elements().forEachRemaining(n -> {
                    types.add(n.textValue());
                });
                schema.types(types);
            }
            if (additionalProperties != null) {
                try {
                    Schema innerSchema = ConvertUtils.getJsonMapper31().convertValue(additionalProperties, JsonSchema.class);
                    schema.setAdditionalProperties(innerSchema);
                } catch (Exception e) {
                    Boolean additionalPropsBoolean = ConvertUtils.getJsonMapper31().convertValue(additionalProperties, Boolean.class);
                    schema.setAdditionalProperties(additionalPropsBoolean);
                }
            }

        } else {
            schema = ConvertUtils.getJsonMapper31().convertValue(node, JsonSchema.class);
        }
        return schema;
    }
}
