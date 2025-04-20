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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Endpoints configuration.
 *
 * @author croudet
 */
@Internal
public final class EndpointsConfig {

    private boolean enabled;
    private String path;
    private List<Tag> tags = Collections.emptyList();
    private List<Server> servers = Collections.emptyList();
    private List<SecurityRequirement> securityRequirements = Collections.emptyList();
    private Map<String, EndpointProperties> endpoints = Collections.emptyMap();
    private List<String> groups = Collections.emptyList();
    private List<String> groupsExcluded = Collections.emptyList();
    /**
     * Extensions to add to the Endpoint entry in the spec file.
     */
    private Map<String, Object> extensions = Collections.emptyMap();

    public EndpointsConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public List<SecurityRequirement> getSecurityRequirements() {
        return securityRequirements;
    }

    public void setSecurityRequirements(List<SecurityRequirement> securityRequirements) {
        this.securityRequirements = securityRequirements;
    }

    public Map<String, EndpointProperties> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointProperties> endpoints) {
        this.endpoints = endpoints;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getGroupsExcluded() {
        return groupsExcluded;
    }

    public void setGroupsExcluded(List<String> groupsExcluded) {
        this.groupsExcluded = groupsExcluded;
    }
}
