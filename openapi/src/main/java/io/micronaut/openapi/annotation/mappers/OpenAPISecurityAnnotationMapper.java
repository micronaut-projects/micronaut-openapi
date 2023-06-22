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
package io.micronaut.openapi.annotation.mappers;

import java.util.Collections;
import java.util.List;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.TypedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.openapi.annotation.OpenAPISecurity;

/**
 * Mapper for security endpoints.
 */
public class OpenAPISecurityAnnotationMapper implements TypedAnnotationMapper<OpenAPISecurity> {

    @Override
    public Class<OpenAPISecurity> annotationType() {
        return OpenAPISecurity.class;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<OpenAPISecurity> annotation, VisitorContext visitorContext) {
        return Collections.singletonList(
            AnnotationValue.builder(OpenAPIInclude.class)
                .values(
                    "io.micronaut.security.endpoints.LoginController",
                    "io.micronaut.security.endpoints.LogoutController",
                    "io.micronaut.security.token.jwt.endpoints.KeysController",
                    "io.micronaut.security.token.jwt.endpoints.OauthController"
                )
                .member("tags", annotation.getAnnotations("tags").toArray(new AnnotationValue[0]))
                .member("security", annotation.getAnnotations("security").toArray(new AnnotationValue[0]))
                .build()
        );
    }

}
