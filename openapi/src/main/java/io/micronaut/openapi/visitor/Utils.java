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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.MediaType;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.javadoc.JavadocParser;
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.SpecVersion;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED;
import static io.swagger.v3.oas.models.media.Schema.BIND_TYPE_AND_TYPES;

/**
 * Some util methods.
 *
 * @since 4.4.0
 */
@Internal
public final class Utils {

    public static final String ATTR_OPENAPI = "io.micronaut.OPENAPI";
    public static final String ATTR_TEST_MODE = "io.micronaut.OPENAPI_TEST";
    public static final String ATTR_VISITED_ELEMENTS = "io.micronaut.OPENAPI.visited.elements";

    public static final List<MediaType> DEFAULT_MEDIA_TYPES = Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);

    private static Map<String, MethodElement> creatorConstructorsCache = new HashMap<>();

    private static boolean openapi31;
    private static boolean initialized;
    private static Set<String> allKnownVersions;
    private static Set<String> allKnownGroups;
    private static Map<String, List<EndpointInfo>> endpointInfos;
    /**
     * Groups openAPI objects, described by OpenAPIDefinition annotations.
     */
    private static Map<String, OpenAPI> openApis;
    /**
     * Group names by included controller or endpoint class names with OpenAPIInclude annotation.
     */
    private static Map<String, List<String>> includedClassesGroups;
    /**
     * Excluded group names by included controller or endpoint class names with OpenAPIInclude annotation.
     */
    private static Map<String, List<String>> includedClassesGroupsExcluded;

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

    public static void init(VisitorContext context) {
        if (initialized) {
            return;
        }
        openapi31 = ConfigUtils.getBooleanProperty(MICRONAUT_OPENAPI_31_ENABLED, false, context);
        initialized = true;
    }

    /**
     * Get or create MediaType object by name.
     *
     * @param mediaTypeName name of mediaType
     * @return MediaType object
     */
    public static MediaType getMediaType(String mediaTypeName) {
        try {
            return MediaType.of(mediaTypeName);
        } catch (Exception e) {
            return new MediaType(mediaTypeName);
        }
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
     * Find and remove duplicates in lists.
     *
     * @param elements list of elements
     * @param predicate predicate for calculating duplicate element
     * @param <T> elements class
     *
     * @return list of elements without duplicates
     */
    public static <T> List<T> findAndRemoveDuplicates(List<T> elements, BiPredicate<T, T> predicate) {
        if (CollectionUtils.isEmpty(elements)) {
            return elements;
        }
        var result = new ArrayList<T>();
        for (var element : elements) {
            boolean found = false;
            for (var el : result) {
                if (predicate.test(element, el)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(element);
            }
        }
        if (result.size() != elements.size()) {
            return result;
        }
        return elements;
    }

    /**
     * Resolve the components.
     *
     * @param openApi The open API
     *
     * @return The components
     */
    public static Components resolveComponents(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        return components;
    }

    /**
     * Resolve the webhooks.
     *
     * @param openApi The open API
     *
     * @return The webhooks
     */
    public static Map<String, PathItem> resolveWebhooks(OpenAPI openApi) {
        var webhooks = openApi.getWebhooks();
        if (webhooks == null) {
            webhooks = new HashMap<>();
            openApi.setWebhooks(webhooks);
        }
        return webhooks;
    }

    /**
     * Resolve the {@link OpenAPI} instance.
     *
     * @param context The context
     *
     * @return The {@link OpenAPI} instance
     */
    public static OpenAPI resolveOpenApi(VisitorContext context) {
        var openApi = ContextUtils.get(ATTR_OPENAPI, OpenAPI.class, context);
        if (openApi == null) {
            openApi = new OpenAPI();
            if (Utils.isOpenapi31()) {
                openApi.openapi(OpenApiUtils.OPENAPI_31_VERSION)
                    .jsonSchemaDialect(ConfigUtils.getJsonSchemaDialect(context))
                    .specVersion(SpecVersion.V31);
            }
            ContextUtils.put(ATTR_OPENAPI, openApi, context);
        }
        return openApi;
    }

    /**
     * Return stacktrace for throwable and message.
     *
     * @param t throwable
     *
     * @return stacktrace
     */
    public static String printStackTrace(Throwable t) {
        var sw = new StringWriter();
        sw.append(t.getMessage()).append('\n');
        var pw = new PrintWriter(sw);
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

    public static Map<String, List<String>> getIncludedClassesGroups() {
        return includedClassesGroups;
    }

    public static void setIncludedClassesGroups(Map<String, List<String>> includedClassesGroups) {
        Utils.includedClassesGroups = includedClassesGroups;
    }

    public static Map<String, List<String>> getIncludedClassesGroupsExcluded() {
        return includedClassesGroupsExcluded;
    }

    public static void setIncludedClassesGroupsExcluded(Map<String, List<String>> includedClassesGroupsExcluded) {
        Utils.includedClassesGroupsExcluded = includedClassesGroupsExcluded;
    }

    public static Map<String, MethodElement> getCreatorConstructorsCache() {
        return creatorConstructorsCache;
    }

    public static ObjectMapper getJsonMapper() {
        return openapi31 ? OpenApiUtils.getJsonMapper31() : OpenApiUtils.getJsonMapper();
    }

    public static ObjectMapper getYamlMapper() {
        return openapi31 ? OpenApiUtils.getYamlMapper31() : OpenApiUtils.getYamlMapper();
    }

    public static boolean isOpenapi31() {
        return openapi31;
    }

    public static void setOpenapi31(boolean openapi31) {
        System.setProperty(BIND_TYPE_AND_TYPES, "true");
        Utils.openapi31 = openapi31;
    }

    public static void clean() {
        openapi31 = false;
        initialized = false;
        openApis = null;
        endpointInfos = null;
        includedClassesGroups = null;
        includedClassesGroupsExcluded = null;

        allKnownGroups = null;
        allKnownVersions = null;

        testReference = null;
        testReferences = null;
        testFileName = null;
        testYamlReference = null;
        testJsonReference = null;
        creatorConstructorsCache = new HashMap<>();
        System.clearProperty(BIND_TYPE_AND_TYPES);
        SchemaDefinitionUtils.clean();
        OpenApiExtraSchemaVisitor.clean();
    }
}
