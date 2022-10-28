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
import java.util.stream.Collectors;

import io.micronaut.core.annotation.Internal;
import io.swagger.v3.oas.models.headers.Header;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
@Internal
public class HeaderStyleEnumDeserializer extends JsonDeserializer<Header.StyleEnum> {

    @Override
    public Header.StyleEnum deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (node != null) {
            String value = node.asText();
            return getStyleEnum(value);
        }
        return null;
    }

    private Header.StyleEnum getStyleEnum(String value) {
        return Arrays.stream(
                Header.StyleEnum.values())
            .filter(i -> i.toString().equals(value))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("Can not deserialize value of type Header.StyleEnum from String \"%s\": value not one of declared Enum instance names: %s",
                    value,
                    Arrays.stream(Header.StyleEnum.values()).map(Header.StyleEnum::toString).collect(Collectors.joining(", ", "[", "]")))));
    }
}
