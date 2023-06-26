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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.http.annotation.Controller;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.openapi.annotation.OpenAPIIncludes;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.isOpenApiEnabled;

/**
 * A {@link TypeElementVisitor} that builds the Swagger model from Micronaut controllers included by @{@link OpenAPIInclude} at the compile time.
 *
 * @author Denis Stepanov
 */
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiIncludeVisitor implements TypeElementVisitor<OpenAPIIncludes, Object> {

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context)) {
            return;
        }
        for (AnnotationValue<OpenAPIInclude> includeAnnotation : element.getAnnotationValuesByType(OpenAPIInclude.class)) {
            String[] classes = includeAnnotation.stringValues();
            if (ArrayUtils.isNotEmpty(classes)) {
                List<AnnotationValue<Tag>> tags = includeAnnotation.getAnnotations("tags", Tag.class);
                List<AnnotationValue<SecurityRequirement>> security = includeAnnotation.getAnnotations("security", SecurityRequirement.class);
                Optional<String> customUri = includeAnnotation.stringValue("uri");
                List<String> groups = Arrays.asList(includeAnnotation.stringValues("groups"));
                List<String> groupsExcluded = Arrays.asList(includeAnnotation.stringValues("groupsExcluded"));

                OpenApiGroupInfoVisitor groupVisitor = new OpenApiGroupInfoVisitor(groups, groupsExcluded);
                OpenApiControllerVisitor controllerVisitor = new OpenApiControllerVisitor(tags, security, customUri.orElse(null));
                OpenApiEndpointVisitor endpointVisitor = new OpenApiEndpointVisitor(true, tags.isEmpty() ? null : tags, security.isEmpty() ? null : security);
                for (String className : classes) {
                    context.getClassElement(className)
                        .ifPresent(ce -> {
                            groupVisitor.visitClass(ce, context);

                            if (ce.isAnnotationPresent(Controller.class)) {
                                visit(controllerVisitor, context, ce);
                            } else if (ce.isAnnotationPresent("io.micronaut.management.endpoint.annotation.Endpoint")) {
                                visit(endpointVisitor, context, ce);
                            }
                        });
                }
            }
        }
    }

    private void visit(TypeElementVisitor<?, ?> visitor, VisitorContext context, ClassElement ce) {
        visitor.visitClass(ce, context);
        ce.getEnclosedElements(ElementQuery.ALL_METHODS
                .modifiers(mods -> !mods.contains(ElementModifier.STATIC) && !mods.contains(ElementModifier.PRIVATE))
                .named(name -> !name.contains("$"))
            )
            .forEach(method -> visitor.visitMethod(method, context));
    }
}
