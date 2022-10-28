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
package io.micronaut.openapi.swagger.jackson.mixin;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import io.micronaut.core.annotation.Internal;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
@Internal
@JsonPropertyOrder(value = {"type", "format", "if", "then", "else"}, alphabetic = true)
public abstract class Schema31Mixin {

    @JsonIgnore
    public abstract Map<String, Object> getJsonSchema();

    @JsonIgnore
    public abstract Boolean getNullable();

    @JsonIgnore
    public abstract Boolean getExclusiveMinimum();

    @JsonIgnore
    public abstract Boolean getExclusiveMaximum();

    @JsonProperty("exclusiveMinimum")
    public abstract BigDecimal getExclusiveMinimumValue();

    @JsonProperty("exclusiveMaximum")
    public abstract BigDecimal getExclusiveMaximumValue();

    @JsonIgnore
    public abstract String getType();

    @JsonProperty("type")
    @JsonSerialize(using = TypeSerializer.class)
    public abstract Set<String> getTypes();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @JsonAnySetter
    public abstract void addExtension(String name, Object value);

    @JsonIgnore
    public abstract boolean getExampleSetFlag();

    @JsonInclude(Include.NON_NULL)
    public abstract Object getExample();

    @JsonIgnore
    public abstract Object getJsonSchemaImpl();

    @JsonIgnore
    public abstract Boolean getBooleanSchemaValue();

    public static class TypeSerializer extends JsonSerializer<Set<String>> {

        @Override
        public void serialize(Set<String> types, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (types != null && types.size() == 1) {
                jsonGenerator.writeString((String) types.toArray()[0]);
            } else if (types != null && types.size() > 1) {
                jsonGenerator.writeStartArray();
                for (String t : types) {
                    jsonGenerator.writeString(t);
                }
                jsonGenerator.writeEndArray();
            }
        }
    }

}
