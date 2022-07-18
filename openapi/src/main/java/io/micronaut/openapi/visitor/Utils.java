/*
 * Copyright 2017-2022 original authors
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

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.server.types.files.FileCustomizableResponseType;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;

import org.reactivestreams.Publisher;

/**
 * Some util methods.
 *
 * @since 4.4.0
 */
public final class Utils {

    public static final String ATTR_OPENAPI = "io.micronaut.OPENAPI";
    public static final String ATTR_TEST_MODE = "io.micronaut.OPENAPI_TEST";
    public static final String ATTR_VISITED_ELEMENTS = "io.micronaut.OPENAPI.visited.elements";

    private static OpenAPI testReference;
    private static OpenAPI testReferenceAfterPlaceholders;
    private static String testYamlReference;
    private static String testJsonReference;

    private static JavadocParser javadocParser = new JavadocParser();

    private Utils() {
    }

    public static boolean isContainerType(ClassElement type) {
        return CollectionUtils.setOf(
            Optional.class.getName(),
            Future.class.getName(),
            Publisher.class.getName(),
            "io.reactivex.Single",
            "io.reactivex.Observable",
            "io.reactivex.Maybe",
            "io.reactivex.rxjava3.core.Single",
            "io.reactivex.rxjava3.core.Observable",
            "io.reactivex.rxjava3.core.Maybe"
        ).stream().anyMatch(type::isAssignable);
    }

    public static boolean isReturnTypeFile(ClassElement type) {
        return CollectionUtils.setOf(
            FileCustomizableResponseType.class.getName(),
            File.class.getName(),
            InputStream.class.getName(),
            ByteBuffer.class.getName()
        ).stream().anyMatch(type::isAssignable);
    }

    /**
     * Normalizes enum values stored in the map.
     *
     * @param paramValues The values
     * @param enumTypes The enum types.
     */
    public static void normalizeEnumValues(Map<CharSequence, Object> paramValues, Map<String, Class<? extends Enum>> enumTypes) {
        for (Map.Entry<String, Class<? extends Enum>> entry : enumTypes.entrySet()) {
            final String name = entry.getKey();
            final Class<? extends Enum> enumType = entry.getValue();
            Object in = paramValues.get(name);
            if (in != null) {
                try {
                    final Enum enumInstance = Enum.valueOf(enumType, in.toString());
                    paramValues.put(name, enumInstance.toString());
                } catch (Exception e) {
                    // ignore
                }
            }

        }
    }

    /**
     * Resolve the components.
     *
     * @param openAPI The open API
     *
     * @return The components
     */
    public static Components resolveComponents(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            components = new Components();
            openAPI.setComponents(components);
        }
        return components;
    }

    /**
     * Resolve the {@link OpenAPI} instance.
     *
     * @param context The context
     *
     * @return The {@link OpenAPI} instance
     */
    public static OpenAPI resolveOpenAPI(VisitorContext context) {
        OpenAPI openAPI = context.get(ATTR_OPENAPI, OpenAPI.class).orElse(null);
        if (openAPI == null) {
            openAPI = new OpenAPI();
            context.put(ATTR_OPENAPI, openAPI);
            if (isTestMode()) {
                setTestReference(openAPI);
            }
        }
        return openAPI;
    }

    public static boolean isTestMode() {
        return Boolean.getBoolean(ATTR_TEST_MODE);
    }

    public static OpenAPI getTestReference() {
        return testReference;
    }

    public static void setTestReference(OpenAPI testReference) {
        Utils.testReference = testReference;
    }

    public static OpenAPI getTestReferenceAfterPlaceholders() {
        return testReferenceAfterPlaceholders;
    }

    public static void setTestReferenceAfterPlaceholders(OpenAPI testReferenceAfterPlaceholders) {
        Utils.testReferenceAfterPlaceholders = testReferenceAfterPlaceholders;
    }

    public static String getTestYamlReference() {
        return testYamlReference;
    }

    public static void setTestYamlReference(String testYamlReference) {
        Utils.testYamlReference = testYamlReference;
    }

    public static String getTestJsonReference() {
        return testJsonReference;
    }

    public static void setTestJsonReference(String testJsonReference) {
        Utils.testJsonReference = testJsonReference;
    }

    public static JavadocParser getJavadocParser() {
        return javadocParser;
    }

    public static void setJavadocParser(JavadocParser javadocParser) {
        Utils.javadocParser = javadocParser;
    }
}
