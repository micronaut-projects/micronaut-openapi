/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.openapi.visitor.management;

import io.micronaut.core.annotation.Internal;

import java.util.List;
import java.util.Map;

/**
 * Spring Boot actuator properties.
 *
 * @since 6.16.0
 */
@Internal
public final class SpringActuatorProperties {

    private String basePath;
    private List<String> includedEndpoints;
    private List<String> excludedEndpoints;
    private Map<String, String> pathMapping;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<String> getIncludedEndpoints() {
        return includedEndpoints;
    }

    public void setIncludedEndpoints(List<String> includedEndpoints) {
        this.includedEndpoints = includedEndpoints;
    }

    public List<String> getExcludedEndpoints() {
        return excludedEndpoints;
    }

    public void setExcludedEndpoints(List<String> excludedEndpoints) {
        this.excludedEndpoints = excludedEndpoints;
    }

    public Map<String, String> getPathMapping() {
        return pathMapping;
    }

    public void setPathMapping(Map<String, String> pathMapping) {
        this.pathMapping = pathMapping;
    }
}
