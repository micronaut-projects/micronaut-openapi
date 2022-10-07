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
package io.micronaut.openapi.swagger.jackson;

import java.io.IOException;

import io.micronaut.core.annotation.Internal;
import io.swagger.v3.oas.models.media.Schema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
@Internal
public class Schema31Serializer extends JsonSerializer<Schema> implements ResolvableSerializer {

    private JsonSerializer<Object> defaultSerializer;

    public Schema31Serializer(JsonSerializer<Object> serializer) {
        defaultSerializer = serializer;
    }

    @Override
    public void resolve(SerializerProvider serializerProvider) throws JsonMappingException {
        if (defaultSerializer instanceof ResolvableSerializer) {
            ((ResolvableSerializer) defaultSerializer).resolve(serializerProvider);
        }
    }

    @Override
    public void serialize(Schema value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        if (value.getBooleanSchemaValue() != null) {
            jgen.writeBoolean(value.getBooleanSchemaValue());
            return;
        }
        if (value.getExampleSetFlag() && value.getExample() == null) {
            jgen.writeStartObject();
            defaultSerializer.unwrappingSerializer(null).serialize(value, jgen, provider);
            jgen.writeNullField("example");
            jgen.writeEndObject();
        } else {
            defaultSerializer.serialize(value, jgen, provider);
        }
    }
}
