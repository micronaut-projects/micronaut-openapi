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
package io.micronaut.openapi.swagger.core.jackson;

import java.io.IOException;

import io.swagger.v3.oas.models.examples.Example;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

/**
 * This class is copied from swagger-core library.
 *
 * @since 5.0.0
 */
public class ExampleSerializer extends JsonSerializer<Example> implements ResolvableSerializer {

    private final JsonSerializer<Object> defaultSerializer;

    public ExampleSerializer(JsonSerializer<Object> serializer) {
        defaultSerializer = serializer;
    }

    @Override
    public void resolve(SerializerProvider serializerProvider) throws JsonMappingException {
        if (defaultSerializer instanceof ResolvableSerializer resolvableSerializer) {
            resolvableSerializer.resolve(serializerProvider);
        }
    }

    @Override
    public void serialize(Example example, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        if (example.getValueSetFlag() && example.getValue() == null) {
            jgen.writeStartObject();
            defaultSerializer.unwrappingSerializer(null).serialize(example, jgen, provider);
            jgen.writeNullField("value");
            jgen.writeEndObject();
        } else {
            defaultSerializer.serialize(example, jgen, provider);
        }
    }
}
