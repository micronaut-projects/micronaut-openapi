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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class DeserializationModule31 extends SimpleModule {

    public DeserializationModule31() {

        addDeserializer(Schema.class, new Model31Deserializer());
        addDeserializer(Parameter.class, new Parameter31Deserializer());
        addDeserializer(Header.StyleEnum.class, new HeaderStyleEnumDeserializer());
        addDeserializer(Encoding.StyleEnum.class, new EncodingStyleEnumDeserializer());
        addDeserializer(EncodingProperty.StyleEnum.class, new EncodingPropertyStyleEnumDeserializer());

        addDeserializer(SecurityScheme.class, new SecurityScheme31Deserializer());

        addDeserializer(ApiResponses.class, new ApiResponses31Deserializer());
        addDeserializer(Paths.class, new Paths31Deserializer());
        addDeserializer(Callback.class, new Callback31Deserializer());

        setDeserializerModifier(new ValueDeserializerModifier() {
            @Override
            public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
                if (beanDesc.getBeanClass() == OpenAPI.class) {
                    return new OpenAPI31Deserializer(deserializer);
                }
                return deserializer;
            }
        });
    }
}
