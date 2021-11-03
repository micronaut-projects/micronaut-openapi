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
package io.micronaut.openapi.visitor;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.http.annotation.Controller;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.openapi.annotation.OpenAPIIncludes;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;

/**
 * A {@link TypeElementVisitor} that builds the Swagger model from Micronaut controllers included by @{@link OpenAPIInclude} at the compile time.
 *
 * @author Denis Stepanov
 */
@Experimental
public class OpenApiIncludeVisitor implements TypeElementVisitor<OpenAPIIncludes, Object> {

    @Override
    public void visitClass(ClassElement element, VisitorContext visitorContext) {
        if (!"io.micronaut.annotation.processing.visitor.JavaVisitorContext".equals(visitorContext.getClass().getName())) {
            return;
        }
        for (AnnotationValue<OpenAPIInclude> includeAnnotation : element.getAnnotationValuesByType(OpenAPIInclude.class)) {
            String[] classes = includeAnnotation.stringValues();
            if (ArrayUtils.isNotEmpty(classes)) {
                List<AnnotationValue<Tag>> tags = includeAnnotation.getAnnotations("tags", Tag.class);
                List<AnnotationValue<SecurityRequirement>> security = includeAnnotation.getAnnotations("security", SecurityRequirement.class);

                OpenApiControllerVisitor controllerVisitor = new OpenApiControllerVisitor(tags, security);
                OpenApiEndpointVisitor endpointVisitor = new OpenApiEndpointVisitor(true, tags.isEmpty() ? null : tags, security.isEmpty() ? null : security);
                for (String className : classes) {
                    visitorContext.getClassElement(className)
                            .ifPresent(ce -> {
                                if (ce.isAnnotationPresent(Controller.class)) {
                                    visit(controllerVisitor, visitorContext, ce);
                                } else if (ce.isAnnotationPresent(Endpoint.class)) {
                                    visit(endpointVisitor, visitorContext, ce);
                                }
                            });
                }
            }
        }
    }

    private void visit(TypeElementVisitor<?, ?> visitor, VisitorContext visitorContext, ClassElement ce) {
        visitor.visitClass(ce, visitorContext);
        ce.getEnclosedElements(ElementQuery.ALL_METHODS).stream()
                .filter(OpenApiIncludeVisitor::isCandidateMethod)
                .forEach(method -> visitor.visitMethod(method, visitorContext));
    }

    private static boolean isCandidateMethod(MethodElement method) {
        Set<ElementModifier> modifiers = method.getModifiers();
        return !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.PRIVATE)
                && !method.getSimpleName().contains("$");

    }
}
