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

import io.micronaut.core.annotation.Internal;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
@Internal
public class DeserializationModule extends SimpleModule {

    public DeserializationModule() {

        addDeserializer(Schema.class, new ModelDeserializer());
        addDeserializer(Parameter.class, new ParameterDeserializer());
        addDeserializer(Header.StyleEnum.class, new HeaderStyleEnumDeserializer());
        addDeserializer(Encoding.StyleEnum.class, new EncodingStyleEnumDeserializer());
        addDeserializer(EncodingProperty.StyleEnum.class, new EncodingPropertyStyleEnumDeserializer());

        addDeserializer(SecurityScheme.class, new SecuritySchemeDeserializer());

        addDeserializer(ApiResponses.class, new ApiResponsesDeserializer());
        addDeserializer(Paths.class, new PathsDeserializer());
        addDeserializer(Callback.class, new CallbackDeserializer());
    }
}
