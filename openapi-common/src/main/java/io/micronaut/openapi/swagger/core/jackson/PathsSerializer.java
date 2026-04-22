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

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Map.Entry;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class PathsSerializer extends ValueSerializer<Paths> {

    @Override
    public void serialize(Paths value, JsonGenerator jgen, SerializationContext provider) throws JacksonException {

        if (value != null && value.getExtensions() != null && !value.getExtensions().isEmpty()) {
            jgen.writeStartObject();

            if (!value.isEmpty()) {
                for (Entry<String, PathItem> entry : value.entrySet()) {
                    jgen.writePOJOProperty(entry.getKey(), entry.getValue());
                }
            }
            for (Entry<String, Object> entry : value.getExtensions().entrySet()) {
                jgen.writePOJOProperty(entry.getKey(), entry.getValue());
            }
            jgen.writeEndObject();
        } else {
            provider.writeValue(jgen, value);
        }
    }
}
