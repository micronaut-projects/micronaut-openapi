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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.micronaut.inject.ast.ClassElement;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Endpoint definition.
 *
 * @author croudet
 */
class Endpoint {
    private Optional<ClassElement> element;
    private List<Tag> tags = new ArrayList<>(2);

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

    @Override
    public String toString() {
        return "Endpoint [classElement=" + element + ", tags=" + tags + "]";
    }

}
