/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.micronaut.inject.ast.ClassElement;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Endpoint definition.
 *
 * @author croudet
 */
class Endpoint {
    private Optional<ClassElement> element;
    private List<Tag> tags = Collections.emptyList();
    private List<Server> servers = Collections.emptyList();
    private List<SecurityRequirement> securityRequirements = Collections.emptyList();

    /**
     * Sets the ClassElement of the endpoint.
     * @param element A ClassElement,
     */
    void setClassElement(Optional<ClassElement> element) {
        this.element = element;
    }

    /**
     * Sets the tags to add to the Endpoint entry in the spec file.
     * @param tags A list of tags.
     */
    void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    /**
     * Returns the Endpoint ClassElement.
     * @return A ClassElement.
     */
    Optional<ClassElement> getClassElement() {
        return element;
    }

    /**
     * Returns the tags to add to the Endpoint entry in the spec file.
     * @return The tags to add to the Endpoint entry in the spec file.
     */
    List<Tag> getTags() {
        return tags;
    }

    /**
     * Returns the servers to add to the Endpoint entry in the spec file.
     * @return The servers to add to the Endpoint entry in the spec file.
     */
    public List<Server> getServers() {
        return servers;
    }

    /**
     * Sets the servers to add to the Endpoint entry in the spec file.
     * @param servers A list of servers.
     */
    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    /**
     * Returns the securityRequirements to add to the Endpoint entry in the spec file.
     * @return The securityRequirements to add to the Endpoint entry in the spec file.
     */
    public List<SecurityRequirement> getSecurityRequirements() {
        return securityRequirements;
    }

    /**
     * Sets the securityRequirements to add to the Endpoint entry in the spec file.
     * @param securityRequirements A list of securityRequirements.
     */
    public void setSecurityRequirements(List<SecurityRequirement> securityRequirements) {
        this.securityRequirements = securityRequirements;
    }

    @Override
    public String toString() {
        return "Endpoint [element=" + element + ", tags=" + tags + ", servers=" + servers + ", securityRequirements="
                + securityRequirements + "]";
    }

}
