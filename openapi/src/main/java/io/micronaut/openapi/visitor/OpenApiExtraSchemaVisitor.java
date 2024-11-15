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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIExtraSchema;
import io.micronaut.openapi.annotation.OpenAPIExtraSchemas;
import io.swagger.v3.oas.models.media.Schema;

import javax.annotation.processing.SupportedOptions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.micronaut.core.util.ArrayUtils.isEmpty;
import static io.micronaut.core.util.ArrayUtils.isNotEmpty;
import static io.micronaut.openapi.visitor.ConfigUtils.isExtraSchemasEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ContextUtils.getClassElements;
import static io.micronaut.openapi.visitor.ElementUtils.stringValue;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.computeDefaultSchemaName;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.getSchemaDefinition;
import static io.micronaut.openapi.visitor.SchemaUtils.COMPONENTS_SCHEMAS_PREFIX;
import static io.micronaut.openapi.visitor.SchemaUtils.resolveSchemas;
import static io.micronaut.openapi.visitor.Utils.resolveOpenApi;

/**
 * A {@link TypeElementVisitor} that builds the extra Open API schema definitions included by @{@link OpenAPIExtraSchema}
 * at the compile time.
 *
 * @since 6.12.0
 */
@Internal
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiExtraSchemaVisitor implements TypeElementVisitor<OpenAPIExtraSchemas, Object> {

    private static Map<String, Schema> extraSchemas = new LinkedHashMap<>();
    private static Map<String, String> extraSchemaClassnamesToNames = new LinkedHashMap<>();
    private static Set<String> excludedExtraSchemaClassNames = new LinkedHashSet<>();
    private static Set<String> excludedExtraSchemas = new LinkedHashSet<>();
    private static Set<String> excludedExtraSchemaPackages = new LinkedHashSet<>();

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return CollectionUtils.setOf(
            OpenAPIExtraSchema.class.getName(),
            OpenAPIExtraSchemas.class.getName()
        );
    }

    @Override
    public void start(VisitorContext context) {
        Utils.init(context);
    }

    @Override
    public void finish(VisitorContext context) {

        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context) || !isExtraSchemasEnabled(context)) {
            return;
        }

        for (var excludedExtraSchemaPackage : excludedExtraSchemaPackages) {
            for (var entry : extraSchemaClassnamesToNames.entrySet()) {
                addToExcludeExtraSchema(entry.getKey(), excludedExtraSchemaPackage, context);
            }
            for (var entry : SchemaDefinitionUtils.getSchemaNameToClassNameMap().entrySet()) {
                addToExcludeExtraSchema(entry.getValue(), excludedExtraSchemaPackage, context);
            }
        }

        // remove excluded extra schemas
        for (var excludedExtraSchemaClassName : excludedExtraSchemaClassNames) {
            var schemaName = extraSchemaClassnamesToNames.get(excludedExtraSchemaClassName);
            if (schemaName == null) {
                for (var entry : SchemaDefinitionUtils.getSchemaNameToClassNameMap().entrySet()) {
                    if (entry.getValue().equals(excludedExtraSchemaClassName)) {
                        schemaName = entry.getKey();
                        break;
                    }
                }
            }
            if (schemaName != null) {
                extraSchemas.remove(schemaName);
                excludedExtraSchemas.add(schemaName);
            }
        }
    }

    private void addToExcludeExtraSchema(String className, String excludedExtraSchemaPackage, VisitorContext context) {
        var classEl = ContextUtils.getClassElement(className, context);
        if (classEl != null && classEl.getPackageName().equals(excludedExtraSchemaPackage)) {
            excludedExtraSchemaClassNames.add(className);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context) || !isExtraSchemasEnabled(context)) {
            return;
        }

        for (var extraSchemaAnn : element.getAnnotationValuesByType(OpenAPIExtraSchema.class)) {
            String[] classes = extraSchemaAnn.stringValues();
            String[] excludeClasses = extraSchemaAnn.stringValues("excludeClasses");
            String[] excludeClassNames = extraSchemaAnn.stringValues("excludeClassNames");
            String[] packages = extraSchemaAnn.stringValues("packages");
            String[] excludePackages = extraSchemaAnn.stringValues("excludePackages");

            // Classes with annotation without these members we process as extra schema
            if (isEmpty(classes)
                && isEmpty(excludeClasses)
                && isEmpty(excludeClassNames)
                && isEmpty(packages)
                && isEmpty(excludePackages)) {
                processExtraSchemaClass(element, context);
                continue;
            }
            if (isNotEmpty(excludeClasses)) {
                excludedExtraSchemaClassNames.addAll(Arrays.asList(excludeClasses));
            }
            if (isNotEmpty(excludeClassNames)) {
                excludedExtraSchemaClassNames.addAll(Arrays.asList(excludeClassNames));
            }
            if (isNotEmpty(excludePackages)) {
                excludedExtraSchemaPackages.addAll(Arrays.asList(excludePackages));
            }
            if (isNotEmpty(packages)) {
                for (var packageName : packages) {
                    if (packageName.endsWith(".*")) {
                        packageName = packageName.substring(0, packageName.length() - 2);
                    }
                    var classEls = getClassElements(packageName, context);
                    for (var classEl : classEls) {
                        processExtraSchemaClass(classEl, context);
                    }
                }
            }
            if (isNotEmpty(classes)) {
                for (var className : classes) {
                    var classEl = ContextUtils.getClassElement(className, context);
                    processExtraSchemaClass(classEl, context);
                }
            }
        }
    }

    private void processExtraSchemaClass(ClassElement classEl, VisitorContext context) {
        if (classEl == null) {
            return;
        }
        String schemaName = computeDefaultSchemaName(stringValue(classEl, io.swagger.v3.oas.annotations.media.Schema.class, PROP_NAME).orElse(null),
            null, classEl, classEl.getTypeArguments(), context, null);
        var schema = getSchemaDefinition(resolveOpenApi(context), context, classEl, classEl.getTypeArguments(), null, Collections.emptyList(), null);
        if (schema == null) {
            return;
        }
        if (schema.get$ref().equals(COMPONENTS_SCHEMAS_PREFIX + schemaName)) {
            var schemas = resolveSchemas(Utils.resolveOpenApi(context));
            schema = schemas.get(schemaName);
        }
        extraSchemas.put(schemaName, schema);
        extraSchemaClassnamesToNames.put(classEl.getName(), schemaName);
    }

    public static Map<String, Schema> getExtraSchemas() {
        return extraSchemas;
    }

    public static Collection<String> getExcludedExtraSchemas() {
        return excludedExtraSchemas;
    }

    public static void clean() {
        extraSchemas = new LinkedHashMap<>();
        extraSchemaClassnamesToNames = new LinkedHashMap<>();
        excludedExtraSchemaClassNames = new LinkedHashSet<>();
        excludedExtraSchemas = new LinkedHashSet<>();
        excludedExtraSchemaPackages = new LinkedHashSet<>();
    }
}
