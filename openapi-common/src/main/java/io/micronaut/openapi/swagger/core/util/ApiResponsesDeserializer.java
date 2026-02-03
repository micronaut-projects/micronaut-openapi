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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.micronaut.openapi.OpenApiUtils;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class ApiResponsesDeserializer extends ValueDeserializer<ApiResponses> {

    protected boolean openapi31;

    @Override
    public ApiResponses deserialize(JsonParser jp, DeserializationContext ctxt)
        throws JacksonIOException {

        final ObjectMapper mapper;
        if (openapi31) {
            mapper = OpenApiUtils.getJsonMapper31();
        } else {
            mapper = OpenApiUtils.getJsonMapper();
        }
        ApiResponses result = new ApiResponses();
        JsonNode node = jp.readValueAsTree();
        ObjectNode objectNode = (ObjectNode) node;
        Map<String, Object> extensions = new LinkedHashMap<>();
        for (String childName : objectNode.propertyNames()) {
            JsonNode child = objectNode.get(childName);
            // if name start with `x-` consider it an extension
            if (childName.startsWith("x-")) {
                extensions.put(childName, mapper.convertValue(child, Object.class));
            } else {
                result.put(childName, mapper.convertValue(child, ApiResponse.class));
            }
        }
        if (!extensions.isEmpty()) {
            result.setExtensions(extensions);
        }
        return result;
    }
}
