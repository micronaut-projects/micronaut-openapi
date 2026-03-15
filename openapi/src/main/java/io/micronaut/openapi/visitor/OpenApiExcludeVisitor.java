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
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIExclude;
import io.micronaut.openapi.annotation.OpenAPIExcludes;
import org.jspecify.annotations.NonNull;

import javax.annotation.processing.SupportedOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CLASSES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CLASS_NAMES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PACKAGES;

/**
 * A {@link TypeElementVisitor} that builds the Swagger model from Micronaut controllers included by @{@link OpenAPIExclude}
 * at the compile time.
 *
 * @since 6.18.0
 */
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiExcludeVisitor implements TypeElementVisitor<OpenAPIExcludes, Object> {

    private static List<String> excludedPackages = new ArrayList<>();
    private static List<String> excludedClasses = new ArrayList<>();

    public OpenApiExcludeVisitor() {
    }

    public OpenApiExcludeVisitor(List<String> excludedPackages, List<String> excludedClasses) {
        this.excludedPackages = excludedPackages;
        this.excludedClasses = excludedClasses;
    }

    @Override
    public void start(@NonNull VisitorContext context) {
        Utils.init(context);
    }

    @Override
    public void visitClass(@NonNull ClassElement element, @NonNull VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
            return;
        }
        for (var excludeAnn : element.getAnnotationValuesByType(OpenAPIExclude.class)) {
            String[] classesValue = excludeAnn.stringValues();
            if (ArrayUtils.isEmpty(classesValue)) {
                classesValue = excludeAnn.stringValues(PROP_CLASSES);
                if (ArrayUtils.isEmpty(classesValue)) {
                    classesValue = excludeAnn.stringValues(PROP_CLASS_NAMES);
                }
            }
            if (ArrayUtils.isNotEmpty(classesValue)) {
                excludedClasses.addAll(Arrays.asList(classesValue));
            }
            String[] packages = excludeAnn.stringValues(PROP_PACKAGES);
            if (ArrayUtils.isNotEmpty(packages)) {
                Collections.addAll(excludedPackages, packages);
            }
        }
    }

    public static List<String> getExcludedPackages() {
        return excludedPackages;
    }

    public static List<String> getExcludedClasses() {
        return excludedClasses;
    }

    public static void clean() {
        excludedPackages = new ArrayList<>();
        excludedClasses = new ArrayList<>();
    }

    @Override
    public int getOrder() {
        return 80;
    }
}
