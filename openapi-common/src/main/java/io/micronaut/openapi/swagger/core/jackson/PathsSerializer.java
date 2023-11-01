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
import java.util.Map.Entry;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class PathsSerializer extends JsonSerializer<Paths> {

    @Override
    public void serialize(Paths value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        if (value != null && value.getExtensions() != null && !value.getExtensions().isEmpty()) {
            jgen.writeStartObject();

            if (!value.isEmpty()) {
                for (Entry<String, PathItem> entry : value.entrySet()) {
                    jgen.writeObjectField(entry.getKey(), entry.getValue());
                }
            }
            for (Entry<String, Object> entry : value.getExtensions().entrySet()) {
                jgen.writeObjectField(entry.getKey(), entry.getValue());
            }
            jgen.writeEndObject();
        } else {
            provider.defaultSerializeValue(value, jgen);
        }
    }
}
