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
package io.micronaut.openapi.visitor;

import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.annotation.Controller;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.openapi.annotation.OpenAPIIncludes;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NonNull;

import javax.annotation.processing.SupportedOptions;
import java.util.List;

import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CLASSES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CLASS_NAMES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXTENSIONS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_GROUPS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_GROUPS_EXCLUDED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PACKAGES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SECURITY;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_TAGS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_URI;

/**
 * A {@link TypeElementVisitor} that builds the Swagger model from Micronaut controllers included by @{@link OpenAPIInclude}
 * at the compile time.
 *
 * @author Denis Stepanov
 */
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiIncludeVisitor implements TypeElementVisitor<OpenAPIIncludes, Object> {

    @Override
    public void start(@NonNull VisitorContext context) {
        Utils.init(context);
    }

    @Override
    public void visitClass(@NonNull ClassElement element, @NonNull VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
            return;
        }
        for (var includeAnn : element.getAnnotationValuesByType(OpenAPIInclude.class)) {
            var description = includeAnn.stringValue(PROP_DESCRIPTION).orElse(null);
            var extensionAnns = includeAnn.getAnnotations(PROP_EXTENSIONS, Extension.class);
            var tagAnns = includeAnn.getAnnotations(PROP_TAGS, Tag.class);
            var securityAnns = includeAnn.getAnnotations(PROP_SECURITY, SecurityRequirement.class);
            String customUri = includeAnn.stringValue(PROP_URI).orElse(null);

            List<String> groups = List.of(includeAnn.stringValues(PROP_GROUPS));
            List<String> groupsExcluded = List.of(includeAnn.stringValues(PROP_GROUPS_EXCLUDED));

            var groupVisitor = new OpenApiGroupInfoVisitor(groups, groupsExcluded);
            var controllerVisitor = new OpenApiControllerVisitor(
                description,
                extensionAnns.isEmpty() ? null : extensionAnns,
                tagAnns,
                securityAnns,
                customUri
            );
            var endpointVisitor = new OpenApiEndpointVisitor(
                true,
                description,
                extensionAnns.isEmpty() ? null : extensionAnns,
                tagAnns.isEmpty() ? null : tagAnns,
                securityAnns.isEmpty() ? null : securityAnns
            );

            var classesValue = List.<String>of();
            var classesValueArr = includeAnn.stringValues();
            if (ArrayUtils.isEmpty(classesValueArr)) {
                classesValueArr = includeAnn.stringValues(PROP_CLASSES);
                if (ArrayUtils.isEmpty(classesValueArr)) {
                    classesValueArr = includeAnn.stringValues(PROP_CLASS_NAMES);
                }
            }
            if (ArrayUtils.isNotEmpty(classesValueArr)) {
                classesValue = List.of(classesValueArr);
            }
            if (CollectionUtils.isNotEmpty(classesValue)) {
                for (String className : classesValue) {
                    var classEl = ContextUtils.getClassElement(className, context);
                    processClassEl(classEl, groupVisitor, controllerVisitor, endpointVisitor, context);
                }
            }
            var packages = List.<String>of();
            var packagesArr = includeAnn.stringValues(PROP_PACKAGES);
            if (ArrayUtils.isNotEmpty(packagesArr)) {
                packages = List.of(packagesArr);
            }
            if (CollectionUtils.isNotEmpty(packages)) {
                for (String includePackage : packages) {
                    var classEls = ContextUtils.getClassElements(includePackage, context);
                    if (ArrayUtils.isEmpty(classEls)) {
                        continue;
                    }
                    for (var classEl : classEls) {
                        processClassEl(classEl, groupVisitor, controllerVisitor, endpointVisitor, context);
                    }
                }
            }
        }
    }

    private void processClassEl(ClassElement classEl, OpenApiGroupInfoVisitor groupVisitor, OpenApiControllerVisitor controllerVisitor, OpenApiEndpointVisitor endpointVisitor, VisitorContext context) {
        if (classEl == null) {
            return;
        }
        groupVisitor.visitClass(classEl, context);

        if (classEl.isAnnotationPresent(Controller.class)) {
            visit(controllerVisitor, context, classEl);
        } else if (classEl.isAnnotationPresent("io.micronaut.management.endpoint.annotation.Endpoint")) {
            visit(endpointVisitor, context, classEl);
        }
    }

    private void visit(TypeElementVisitor<?, ?> visitor, VisitorContext context, ClassElement ce) {
        visitor.visitClass(ce, context);
        ce.getEnclosedElements(ElementQuery.ALL_METHODS
                .modifiers(mods -> !mods.contains(ElementModifier.STATIC) && !mods.contains(ElementModifier.PRIVATE))
                .named(name -> !name.contains(StringUtil.DOLLAR))
            )
            .forEach(method -> visitor.visitMethod(method, context));
    }

    @Override
    public int getOrder() {
        return 70;
    }
}
