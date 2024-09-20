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
package io.micronaut.openapi.visitor.security;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpMethod;

import java.util.List;

/**
 * Copy of class io.micronaut.security.config.InterceptUrlMapPattern from micronaut-security.
 *
 * @since 4.8.7
 */
@Internal
public final class InterceptUrlMapPattern {

    private final String pattern;
    private final List<String> access;
    private final HttpMethod httpMethod;

    public InterceptUrlMapPattern(String pattern, List<String> access, @Nullable HttpMethod httpMethod) {
        this.pattern = pattern;
        this.access = access;
        this.httpMethod = httpMethod;
    }

    public String getPattern() {
        return pattern;
    }

    public List<String> getAccess() {
        return access;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }
}
