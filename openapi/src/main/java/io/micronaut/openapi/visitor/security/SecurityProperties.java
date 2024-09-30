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

import java.util.List;

/**
 * Micronaut security properties.
 *
 * @since 4.8.7
 */
@Internal
public final class SecurityProperties {

    private final boolean enabled;
    private final boolean micronautSecurityEnabled;
    private final String defaultSchemaName;
    private final List<InterceptUrlMapPattern> interceptUrlMapPatterns;
    private final boolean tokenEnabled;
    private final boolean jwtEnabled;
    private final boolean jwtBearerEnabled;
    private final boolean jwtCookieEnabled;
    private final boolean oauth2Enabled;
    private final boolean basicAuthEnabled;

    public SecurityProperties(boolean enabled, boolean micronautSecurityEnabled, String defaultSchemaName, List<InterceptUrlMapPattern> interceptUrlMapPatterns,
                              boolean tokenEnabled, boolean jwtEnabled, boolean jwtBearerEnabled, boolean jwtCookieEnabled,
                              boolean oauth2Enabled, boolean basicAuthEnabled) {
        this.enabled = enabled;
        this.micronautSecurityEnabled = micronautSecurityEnabled;
        this.defaultSchemaName = defaultSchemaName;
        this.interceptUrlMapPatterns = interceptUrlMapPatterns;
        this.tokenEnabled = tokenEnabled;
        this.jwtEnabled = jwtEnabled;
        this.jwtBearerEnabled = jwtBearerEnabled;
        this.jwtCookieEnabled = jwtCookieEnabled;
        this.oauth2Enabled = oauth2Enabled;
        this.basicAuthEnabled = basicAuthEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMicronautSecurityEnabled() {
        return micronautSecurityEnabled;
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    public List<InterceptUrlMapPattern> getInterceptUrlMapPatterns() {
        return interceptUrlMapPatterns;
    }

    public boolean isTokenEnabled() {
        return tokenEnabled;
    }

    public boolean isJwtEnabled() {
        return jwtEnabled;
    }

    public boolean isJwtBearerEnabled() {
        return jwtBearerEnabled;
    }

    public boolean isJwtCookieEnabled() {
        return jwtCookieEnabled;
    }

    public boolean isOauth2Enabled() {
        return oauth2Enabled;
    }

    public boolean isBasicAuthEnabled() {
        return basicAuthEnabled;
    }
}
