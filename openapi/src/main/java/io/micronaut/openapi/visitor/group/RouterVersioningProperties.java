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

/**
 * Micronaut router versioning properties.
 *
 * @since 4.9.2
 */
@Internal
public final class RouterVersioningProperties {

    public static final String DEFAULT_HEADER_NAME = "X-API-VERSION";
    public static final String DEFAULT_PARAMETER_NAME = "api-version";

    private final boolean enabled;
    private final boolean routerVersiningEnabled;
    private final boolean headerEnabled;
    private final List<String> headerNames;

    private final boolean parameterEnabled;
    private final List<String> parameterNames;

    public RouterVersioningProperties(boolean enabled, boolean routerVersiningEnabled, boolean headerEnabled, List<String> headerNames, boolean parameterEnabled, List<String> parameterNames) {
        this.enabled = enabled;
        this.routerVersiningEnabled = routerVersiningEnabled;
        this.headerEnabled = headerEnabled;
        this.headerNames = headerNames;
        this.parameterEnabled = parameterEnabled;
        this.parameterNames = parameterNames;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRouterVersiningEnabled() {
        return routerVersiningEnabled;
    }

    public boolean isHeaderEnabled() {
        return headerEnabled;
    }

    public List<String> getHeaderNames() {
        return headerNames;
    }

    public boolean isParameterEnabled() {
        return parameterEnabled;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }
}
