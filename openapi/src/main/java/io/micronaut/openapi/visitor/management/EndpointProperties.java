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
package io.micronaut.openapi.visitor.management;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Endpoint definition.
 *
 * @author croudet
 */
@Internal
public final class EndpointProperties {

    /**
     * Endpoint ID from config properties.
     */
    private String id;
    /**
     * Enabled flag for this endpoint from config properties.
     */
    private Boolean enabled;
    /**
     * Sensitive flag for this endpoint from config properties.
     */
    private Boolean sensitive;
    /**
     * URL path for this endpoint from config properties.
     */
    private String path;
    /**
     * Context path for all endpoints from config properties. Only for `all` endpoint.
     */
    private String contextPath;
    /**
     * ClassElement of the endpoint.
     */
    private ClassElement element;

    private String className;
    /**
     * Description to add to the Endpoint entry in the spec file.
     */
    private String description;
    /**
     * Extensions to add to the Endpoint entry in the spec file.
     */
    private Map<String, Object> extensions = Collections.emptyMap();
    /**
     * Tags to add to the Endpoint entry in the spec file.
     */
    private List<Tag> tags = Collections.emptyList();
    /**
     * Servers to add to the Endpoint entry in the spec file.
     */
    private List<Server> servers = Collections.emptyList();
    /**
     * SecurityRequirements to add to the Endpoint entry in the spec file.
     */
    private List<SecurityRequirement> securityRequirements = Collections.emptyList();

    public EndpointProperties(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getSensitive() {
        return sensitive;
    }

    public void setSensitive(Boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setElement(ClassElement element) {
        this.element = element;
        this.className = element != null ? element.getName() : null;
    }

    public ClassElement getElement() {
        return element;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
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

    @Override
    public String toString() {
        return "EndpointProperties{" +
            "id='" + id + '\'' +
            ", enabled=" + enabled +
            ", sensitive=" + sensitive +
            ", path='" + path + '\'' +
            ", element=" + element +
            ", description='" + description + '\'' +
            ", extensions=" + extensions +
            ", tags=" + tags +
            ", servers=" + servers +
            ", securityRequirements=" + securityRequirements +
            '}';
    }
}
