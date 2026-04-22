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

import io.micronaut.openapi.OpenApiUtils;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ValueDeserializer;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class ParameterDeserializer extends ValueDeserializer<Parameter> {

    protected boolean openapi31;

    @Override
    public Parameter deserialize(JsonParser jp, DeserializationContext ctxt)
        throws JacksonException {
        Parameter result = null;

        JsonNode node = ctxt.readTree(jp);
        JsonNode sub = node.get("$ref");
        JsonNode inNode = node.get("in");
        JsonNode desc = node.get("description");

        if (sub != null) {
            result = new Parameter().$ref(sub.asString());
            if (desc != null && openapi31) {
                result.description(desc.asString());
            }

        } else if (inNode != null) {
            String in = inNode.asString();

            ObjectReader reader = null;
            ObjectMapper mapper;
            if (openapi31) {
                mapper = OpenApiUtils.getJsonMapper31();
            } else {
                mapper = OpenApiUtils.getJsonMapper();
            }

            if ("query".equals(in)) {
                reader = mapper.readerFor(QueryParameter.class);
            } else if ("header".equals(in)) {
                reader = mapper.readerFor(HeaderParameter.class);
            } else if ("path".equals(in)) {
                reader = mapper.readerFor(PathParameter.class);
            } else if ("cookie".equals(in)) {
                reader = mapper.readerFor(CookieParameter.class);
            }
            if (reader != null) {
                result = reader.readValue(node);
            }
        }

        return result;
    }
}
