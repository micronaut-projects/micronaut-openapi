/*
 * Copyright 2017-2024 original authors
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.micronaut.openapi.visitor.ConvertUtils.findJsonValueType;
import static io.micronaut.openapi.visitor.ElementUtils.isAnnotationPresent;
import static io.micronaut.openapi.visitor.ElementUtils.isDeprecated;
import static io.micronaut.openapi.visitor.GeneratorExt.DEPRECATED_MESSAGE;
import static io.micronaut.openapi.visitor.GeneratorExt.ENUM_DEPRECATED;
import static io.micronaut.openapi.visitor.GeneratorExt.ENUM_DESCRIPTIONS;
import static io.micronaut.openapi.visitor.GeneratorExt.ENUM_VAR_NAMES;
import static io.micronaut.openapi.visitor.GeneratorExt.FORMAT;
import static io.micronaut.openapi.visitor.GeneratorExt.TYPE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEPRECATED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_HIDDEN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;

/**
 * OpenAPI Generator utilities methods.
 *
 * @since 4.12.4
 */
@Internal
public final class GeneratorUtils {

    private GeneratorUtils() {
    }

    public static void addSchemaDeprecatedExtension(Element el, Schema<?> schema, VisitorContext context) {
        if (!ConfigUtils.isGeneratorExtensionsEnabled(context) || schema == null || el == null) {
            return;
        }

        var extensions = schema.getExtensions() != null ? schema.getExtensions() : new HashMap<String, Object>();
        addDeprecatedMessage(el, extensions, context);
        if (schema.getExtensions() == null && !extensions.isEmpty()) {
            schema.setExtensions(extensions);
        }
    }

    public static void addParameterDeprecatedExtension(TypedElement el, Parameter parameter, VisitorContext context) {
        if (!ConfigUtils.isGeneratorExtensionsEnabled(context) || parameter == null || el == null) {
            return;
        }

        var extensions = parameter.getExtensions() != null ? parameter.getExtensions() : new HashMap<String, Object>();
        addDeprecatedMessage(el, extensions, context);
        if (parameter.getExtensions() == null && !extensions.isEmpty()) {
            parameter.setExtensions(extensions);
        }
    }

    public static void addOperationDeprecatedExtension(MethodElement el, Operation operation, VisitorContext context) {
        if (!ConfigUtils.isGeneratorExtensionsEnabled(context) || operation == null || el == null) {
            return;
        }

        var extensions = operation.getExtensions() != null ? operation.getExtensions() : new HashMap<String, Object>();
        addDeprecatedMessage(el, extensions, context);
        if (!extensions.containsKey(DEPRECATED_MESSAGE)) {
            addDeprecatedMessage(el.getOwningType(), extensions, context);
        }
        if (operation.getExtensions() == null && !extensions.isEmpty()) {
            operation.setExtensions(extensions);
        }
    }

    private static void addDeprecatedMessage(Element el, Map<String, Object> extensions, VisitorContext context) {
        if (extensions.containsKey(DEPRECATED_MESSAGE)) {
            return;
        }

        String deprecatedMessage = null;
        var deprecatedAnn = el.getAnnotation("kotlin.Deprecated");
        if (deprecatedAnn != null) {
            deprecatedMessage = deprecatedAnn.stringValue().orElse(deprecatedAnn.stringValue("message").orElse(null));
        }
        if (deprecatedMessage == null) {
            var javadoc = el.getDocumentation().orElse(null);
            var javadocDescription = Utils.getJavadocParser().parse(javadoc);
            if (javadocDescription != null) {
                deprecatedMessage = javadocDescription.getDeprecatedDescription();
            }
        }
        if (StringUtils.isNotEmpty(deprecatedMessage)) {
            extensions.put(DEPRECATED_MESSAGE, deprecatedMessage);
        }
    }

    public static void addValidationMessages(Element el, Schema<?> schema, Map<String, String> messages, VisitorContext context) {
        if (!ConfigUtils.isGeneratorExtensionsEnabled(context) || schema == null || el == null) {
            return;
        }

        var extensions = schema.getExtensions() != null ? schema.getExtensions() : new HashMap<String, Object>();
        for (var entry : messages.entrySet()) {
            if (!extensions.containsKey(entry.getKey())) {
                extensions.put(entry.getKey(), entry.getValue());
            }
        }
        if (schema.getExtensions() == null && !extensions.isEmpty()) {
            schema.setExtensions(extensions);
        }
    }

    public static void addEnumExtensions(EnumElement enumEl, Schema<?> schema, VisitorContext context) {

        if (!ConfigUtils.isGeneratorExtensionsEnabled(context) || enumEl == null) {
            return;
        }

        var extensions = schema.getExtensions();
        if (extensions == null) {
            extensions = new HashMap<>();
            schema.setExtensions(extensions);
        }

        String xType = null;
        String xFormat = null;

        ClassElement fieldType = findJsonValueType(enumEl, context);
        if (fieldType != null) {
            if (fieldType.isPrimitive()) {
                xType = fieldType.getSimpleName();
            } else {
                xType = fieldType.getSimpleName();
                if (xType.equalsIgnoreCase("byte")) {
                    xType = null;
                    xFormat = "int8";
                } else if (xType.equalsIgnoreCase("short")) {
                    xType = null;
                    xFormat = "int16";
                } else if (xType.equalsIgnoreCase("int")
                    || xType.equalsIgnoreCase("integer")
                    || xType.equalsIgnoreCase("long")
                    || xType.equalsIgnoreCase("float")
                    || xType.equalsIgnoreCase("double")) {
                    xType = null;
                    // because boolean enums generated as Boolean type
                } else if (xType.equalsIgnoreCase("boolean")) {
                    xType = null;
                } else if (xType.equalsIgnoreCase("char") || xType.equalsIgnoreCase("character")) {
                    xType = "char";
                }
            }
        }

        if (xType != null && !extensions.containsKey(TYPE)) {
            extensions.put(TYPE, xType);
        }
        if (xFormat != null && !extensions.containsKey(FORMAT)) {
            extensions.put(FORMAT, xFormat);
        }

        var enumVarNameList = new ArrayList<String>();
        var enumVarDocList = new ArrayList<String>();
        var enumVarDeprecatedList = new ArrayList<String>();

        for (var enumConstEl : enumEl.elements()) {

            var schemaAnn = enumConstEl.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            boolean isHidden = schemaAnn != null && schemaAnn.booleanValue(PROP_HIDDEN).orElse(false);

            if (isHidden
                || isAnnotationPresent(enumConstEl, Hidden.class)
                || isAnnotationPresent(enumConstEl, JsonIgnore.class)) {
                continue;
            }

            String enumVarName = null;
            if (schemaAnn != null) {
                enumVarName = schemaAnn.stringValue(PROP_NAME).orElse(null);
            }
            if (enumVarName == null) {
                enumVarName = enumConstEl.getName();
            }
            enumVarNameList.add(enumVarName);

            String enumVarDoc = null;
            if (schemaAnn != null) {
                enumVarDoc = schemaAnn.stringValue(PROP_DESCRIPTION).orElse(null);
            }
            if (enumVarDoc == null) {
                var enumConstJavadoc = enumConstEl.getDocumentation().orElse(null);
                if (enumConstJavadoc != null) {
                    var javadocDesc = Utils.getJavadocParser().parse(enumConstJavadoc);
                    if (javadocDesc != null && StringUtils.isNotEmpty(javadocDesc.getMethodDescription())) {
                        enumVarDoc = javadocDesc.getMethodDescription();
                    }
                }
            }
            enumVarDocList.add(enumVarDoc != null ? enumVarDoc : StringUtils.EMPTY_STRING);

            Boolean isDeprecated = null;
            if (schemaAnn != null) {
                isDeprecated = schemaAnn.booleanValue(PROP_DEPRECATED).orElse(null);
            }
            if (isDeprecated == null) {
                isDeprecated = isDeprecated(enumConstEl);
            }
            if (isDeprecated) {
                enumVarDeprecatedList.add(enumVarName);
            }
        }

        if (!enumVarNameList.isEmpty() && !extensions.containsKey(ENUM_VAR_NAMES)
            && !schema.getEnum().equals(enumVarNameList)) {
            extensions.put(ENUM_VAR_NAMES, enumVarNameList);
        }
        if (!enumVarDocList.isEmpty() && !extensions.containsKey(ENUM_DESCRIPTIONS)) {
            var foundNotEmpty = false;
            for (var enumVarDoc : enumVarDocList) {
                if (StringUtils.isNotEmpty(enumVarDoc)) {
                    foundNotEmpty = true;
                    break;
                }
            }
            if (foundNotEmpty) {
                extensions.put(ENUM_DESCRIPTIONS, enumVarDocList);
            }
        }
        if (!enumVarDeprecatedList.isEmpty() && !extensions.containsKey(ENUM_DEPRECATED)) {
            extensions.put(ENUM_DEPRECATED, enumVarDeprecatedList);
        }
    }
}
