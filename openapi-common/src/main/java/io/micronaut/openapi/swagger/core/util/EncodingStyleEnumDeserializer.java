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
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.media.Encoding;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class EncodingStyleEnumDeserializer extends JsonDeserializer<Encoding.StyleEnum> {

    @Override
    public Encoding.StyleEnum deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (node != null) {
            String value = node.asText();
            return getStyleEnum(value);
        }
        return null;
    }

    private Encoding.StyleEnum getStyleEnum(String value) {
        return Arrays.stream(
                Encoding.StyleEnum.values())
            .filter(i -> i.toString().equals(value))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("Can not deserialize value of type Encoding.StyleEnum from String \"%s\": value not one of declared Enum instance names: %s",
                    value,
                    Arrays.stream(Encoding.StyleEnum.values()).map(Encoding.StyleEnum::toString).collect(Collectors.joining(", ", "[", "]")))));
    }
}
