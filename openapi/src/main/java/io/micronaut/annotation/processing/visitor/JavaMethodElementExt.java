/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.annotation.processing.visitor;

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;

import io.micronaut.core.annotation.AnnotationMetadata;

/**
 * A method element returning data from a {@link ExecutableElement}.
 *
 * @author James Kleeh
 * @since 1.0
 */
public class JavaMethodElementExt extends JavaMethodElement {
    private final String doc;

    /**
     * @param declaringClass     The declaring class
     * @param executableElement  The {@link ExecutableElement}
     * @param annotationMetadata The annotation metadata
     * @param visitorContext The visitor context
     * @param doc The method documentation.
     */
    JavaMethodElementExt(JavaClassElement declaringClass, ExecutableElement executableElement,
            AnnotationMetadata annotationMetadata, JavaVisitorContext visitorContext, String doc) {
        super(declaringClass, executableElement, annotationMetadata, visitorContext);
        this.doc = doc;
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(doc);
    }
}
