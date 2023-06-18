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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.micronaut.context.env.DefaultPropertyPlaceholderResolver;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.DefaultConversionService;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.http.MediaType;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocParser;
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Some util methods.
 *
 * @since 4.4.0
 */
@Internal
public final class Utils {

    public static final String PLACEHOLDER_PREFIX = "${";
    public static final String PLACEHOLDER_POSTFIX = "}";

    public static final String ATTR_OPENAPI = "io.micronaut.OPENAPI";
    public static final String ATTR_TEST_MODE = "io.micronaut.OPENAPI_TEST";
    public static final String ATTR_VISITED_ELEMENTS = "io.micronaut.OPENAPI.visited.elements";

    public static final List<MediaType> DEFAULT_MEDIA_TYPES = Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);

    private static Set<String> allKnownVersions;
    private static Set<String> allKnownGroups;
    private static Map<String, List<EndpointInfo>> endpointInfos;
    /**
     * Groups openAPI objects, described by OpenAPIDefinition annotations.
     */
    private static Map<String, OpenAPI> openApis = new HashMap<>();

    private static PropertyPlaceholderResolver propertyPlaceholderResolver;
    private static OpenAPI testReference;
    /**
     * OpenAPI objects by key - {@code Pair.of(group, version)}.
     */
    private static Map<Pair<String, String>, OpenApiInfo> testReferences;
    private static String testFileName;
    private static String testYamlReference;
    private static String testJsonReference;

    private static JavadocParser javadocParser = new JavadocParser();

    private Utils() {
    }

    @Nullable
    public static Path getProjectPath(VisitorContext context) {
        Path path;
        try {
            path = context.getProjectDir().orElse(Utils.isTestMode() ? Paths.get(System.getProperty("user.dir")) : null);
        } catch (Exception e) {
            // Should never happen
            path = Paths.get(System.getProperty("user.dir"));
        }
        return path;
    }

    /**
     * @return An Instance of default {@link PropertyPlaceholderResolver} to resolve placeholders.
     */
    public static PropertyPlaceholderResolver getPropertyPlaceholderResolver() {
        if (propertyPlaceholderResolver == null) {
            propertyPlaceholderResolver = new DefaultPropertyPlaceholderResolver(new PropertyResolver() {
                @Override
                public boolean containsProperty(@NonNull String name) {
                    return false;
                }

                @Override
                public boolean containsProperties(@NonNull String name) {
                    return false;
                }

                @NonNull
                @Override
                public <T> Optional<T> getProperty(@NonNull String name, @NonNull ArgumentConversionContext<T> conversionContext) {
                    return Optional.empty();
                }
            }, new DefaultConversionService());
        }
        return propertyPlaceholderResolver;
    }

    /**
     * Normalizes enum values stored in the map.
     *
     * @param paramValues The values
     * @param enumTypes The enum types.
     * @param <T> enum class
     */
    public static <T extends Enum<T>> void normalizeEnumValues(Map<CharSequence, Object> paramValues, Map<String, Class<T>> enumTypes) {
        for (Map.Entry<String, Class<T>> entry : enumTypes.entrySet()) {
            final String name = entry.getKey();
            final Class<T> enumType = entry.getValue();
            Object in = paramValues.get(name);
            if (in != null) {
                try {
                    final Enum<T> enumInstance = Enum.valueOf(enumType, in.toString());
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
    public static OpenAPI resolveOpenApi(VisitorContext context) {
        OpenAPI openAPI = context.get(ATTR_OPENAPI, OpenAPI.class).orElse(null);
        if (openAPI == null) {
            openAPI = new OpenAPI();
            context.put(ATTR_OPENAPI, openAPI);
        }
        return openAPI;
    }

    /**
     * Return stacktrace for throwable and message.
     *
     * @param t throwable
     *
     * @return stacktrace
     */
    public static String printStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        sw.append(t.getMessage()).append('\n');
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
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

    public static Map<Pair<String, String>, OpenApiInfo> getTestReferences() {
        return testReferences;
    }

    public static void setTestReferences(Map<Pair<String, String>, OpenApiInfo> testReferences) {
        Utils.testReferences = testReferences;
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

    public static String getTestFileName() {
        return testFileName;
    }

    public static void setTestFileName(String testFileName) {
        Utils.testFileName = testFileName;
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

    public static Set<String> getAllKnownVersions() {
        if (allKnownVersions == null) {
            allKnownVersions = new HashSet<>();
        }
        return allKnownVersions;
    }

    public static void setAllKnownVersions(Set<String> allKnownVersions) {
        Utils.allKnownVersions = allKnownVersions;
    }

    public static Set<String> getAllKnownGroups() {
        if (allKnownGroups == null) {
            allKnownGroups = new HashSet<>();
        }
        return allKnownGroups;
    }

    public static void setAllKnownGroups(Set<String> allKnownGroups) {
        Utils.allKnownGroups = allKnownGroups;
    }

    public static Map<String, List<EndpointInfo>> getEndpointInfos() {
        return endpointInfos;
    }

    public static void setEndpointInfos(Map<String, List<EndpointInfo>> endpointInfos) {
        Utils.endpointInfos = endpointInfos;
    }

    public static Map<String, OpenAPI> getOpenApis() {
        return openApis;
    }

    public static void setOpenApis(Map<String, OpenAPI> openApis) {
        Utils.openApis = openApis;
    }
}
