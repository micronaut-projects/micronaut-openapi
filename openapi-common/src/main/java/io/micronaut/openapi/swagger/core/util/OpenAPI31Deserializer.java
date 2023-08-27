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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class OpenAPI31Deserializer extends StdDeserializer<OpenAPI> implements ResolvableDeserializer {

    private final JsonDeserializer<?> defaultDeserializer;

    public OpenAPI31Deserializer(JsonDeserializer<?> defaultDeserializer) {
        super(OpenAPI.class);
        this.defaultDeserializer = defaultDeserializer;
    }

    @Override
    public OpenAPI deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        OpenAPI openAPI = (OpenAPI) defaultDeserializer.deserialize(jp, ctxt);
        openAPI.setSpecVersion(SpecVersion.V31);
        return openAPI;
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
    }
}
