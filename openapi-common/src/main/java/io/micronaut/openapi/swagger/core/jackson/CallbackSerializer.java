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
import io.swagger.v3.oas.models.callbacks.Callback;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class CallbackSerializer extends ValueSerializer<Callback> {

    @Override
    public void serialize(Callback value, JsonGenerator jgen, SerializationContext provider) throws JacksonIOException {

        // has extensions
        if (value != null && value.getExtensions() != null && !value.getExtensions().isEmpty()) {
            jgen.writeStartObject();

            // not a ref
            if (value.get$ref() == null || value.get$ref().isBlank()) {
                if (!value.isEmpty()) {
                    // write map
                    for (Entry<String, PathItem> entry : value.entrySet()) {
                        jgen.writeObjectField(entry.getKey(), entry.getValue());
                    }
                }
            } else { // handle ref schema serialization skipping all other props ...
                jgen.writeStringField("$ref", value.get$ref());
            }
            for (String ext : value.getExtensions().keySet()) {
                jgen.writeObjectField(ext, value.getExtensions().get(ext));
            }
            jgen.writeEndObject();
        } else {
            if (value == null || value.get$ref() == null || value.get$ref().isBlank()) {
                provider.defaultSerializeValue(value, jgen);
            } else { // handle ref schema serialization skipping all other props
                jgen.writeStartObject();
                jgen.writeStringField("$ref", value.get$ref());
                jgen.writeEndObject();
            }
        }
    }
}
