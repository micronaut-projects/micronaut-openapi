/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.openapi.swagger.core.jackson;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Custom deserializer to handle the polymorphic 'additionalProperties' field.
 * It prevents IllegalArgumentException by converting JsonNode into Boolean or Schema
 * BEFORE the setter is called.
 */
public class AdditionalPropertiesDeserializer extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken token = p.currentToken();

        // Check if the value is a boolean (true/false)
        if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
            return p.getBooleanValue();
        }

        // Check if the value is an object {...}
        if (token == JsonToken.START_OBJECT) {
            // Recursively deserialize the object into a Swagger Schema instance
            // This ensures the setter receives a Schema object, not a JsonNode
            return p.readValueAs(Schema.class);
        }

        // Return null for any other types (like strings or nulls)
        return null;
    }
}
