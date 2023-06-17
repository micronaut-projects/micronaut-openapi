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
package io.micronaut.openapi.visitor.group;

import java.util.List;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpMethod;
import io.micronaut.inject.ast.MethodElement;
import io.swagger.v3.oas.models.Operation;

/**
 * Entity to storage information about same swagger operations, but with different versions api.
 * Need it to merge them in post-processing.
 *
 * @since 4.9.2
 */
@Internal
public final class EndpointInfo {

    private final String url;
    private final HttpMethod httpMethod;
    private final MethodElement method;
    private final Operation operation;
    private final String version;
    private final List<String> groups;
    private final List<String> excludedGroups;

    public EndpointInfo(String url, HttpMethod httpMethod, MethodElement method, Operation operation, String version, List<String> groups, List<String> excludedGroups) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.method = method;
        this.operation = operation;
        this.version = version;
        this.groups = groups;
        this.excludedGroups = excludedGroups;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public MethodElement getMethod() {
        return method;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }
}
