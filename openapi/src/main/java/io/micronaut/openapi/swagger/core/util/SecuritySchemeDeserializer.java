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
import java.util.Arrays;
import java.util.Iterator;

import io.micronaut.core.annotation.Internal;
import io.micronaut.openapi.visitor.ConvertUtils;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
@Internal
public class SecuritySchemeDeserializer extends JsonDeserializer<SecurityScheme> {

    protected boolean openapi31;

    @Override
    public SecurityScheme deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        ObjectMapper mapper;
        if (openapi31) {
            mapper = ConvertUtils.getJsonMapper31();
        } else {
            mapper = ConvertUtils.getJsonMapper();
        }
        SecurityScheme result = null;

        JsonNode node = jp.getCodec().readTree(jp);

        JsonNode inNode = node.get("type");

        if (inNode != null) {
            String type = inNode.asText();
            if (Arrays.stream(SecurityScheme.Type.values()).noneMatch(t -> t.toString().equals(type))) {
                // wrong type, throw exception
                throw new JsonParseException(jp, String.format("SecurityScheme type %s not allowed", type));
            }
            result = new SecurityScheme()
                .description(getFieldText("description", node));

            if ("http".equals(type)) {
                result
                    .type(SecurityScheme.Type.HTTP)
                    .scheme(getFieldText("scheme", node))
                    .bearerFormat(getFieldText("bearerFormat", node));
            } else if ("apiKey".equals(type)) {
                result
                    .type(SecurityScheme.Type.APIKEY)
                    .name(getFieldText("name", node))
                    .in(getIn(getFieldText("in", node)));
            } else if ("openIdConnect".equals(type)) {
                result
                    .type(SecurityScheme.Type.OPENIDCONNECT)
                    .openIdConnectUrl(getFieldText("openIdConnectUrl", node));
            } else if ("oauth2".equals(type)) {
                result
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(mapper.convertValue(node.get("flows"), OAuthFlows.class));
            } else if ("mutualTLS".equals(type)) {
                result
                    .type(SecurityScheme.Type.MUTUALTLS);
            }
            final Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                final String fieldName = fieldNames.next();
                if (fieldName.startsWith("x-")) {
                    final JsonNode fieldValue = node.get(fieldName);
                    final Object value = ConvertUtils.getJsonMapper().treeToValue(fieldValue, Object.class);
                    result.addExtension(fieldName, value);
                }
            }
        }

        return result;
    }

    private SecurityScheme.In getIn(String value) {
        return Arrays.stream(SecurityScheme.In.values()).filter(i -> i.toString().equals(value)).findFirst().orElse(null);
    }

    private String getFieldText(String fieldName, JsonNode node) {
        JsonNode inNode = node.get(fieldName);
        if (inNode != null) {
            return inNode.asText();
        }
        return null;
    }
}
