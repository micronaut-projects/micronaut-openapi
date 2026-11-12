/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.openapi.mapper;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Locates the {@link OpenApiMapperProvider} to use.
 *
 * @since 8.0.0
 */
@Experimental
public final class OpenApiMapperProviders {

    /**
     * The property that selects the provider by name when more than one is available.
     */
    public static final String MAPPER_PROPERTY = "micronaut.openapi.mapper";

    /**
     * The name of the provider chosen when several are available and none is selected.
     */
    public static final String DEFAULT_PROVIDER = "jackson";

    private OpenApiMapperProviders() {
    }

    /**
     * Finds all available providers, ordered by name.
     *
     * @param classLoader The class loader to search
     * @return The providers
     */
    public static List<OpenApiMapperProvider> findAll(ClassLoader classLoader) {
        var providers = new ArrayList<OpenApiMapperProvider>();
        for (OpenApiMapperProvider provider : ServiceLoader.load(OpenApiMapperProvider.class, classLoader)) {
            providers.add(provider);
        }
        providers.sort(Comparator.comparing(OpenApiMapperProvider::getName));
        return providers;
    }

    /**
     * Selects a provider.
     *
     * <ul>
     *     <li>If {@code name} is set, the provider with that name is used.</li>
     *     <li>Otherwise, if exactly one provider is available it is used.</li>
     *     <li>Otherwise, the {@value #DEFAULT_PROVIDER} provider is used if available.</li>
     * </ul>
     *
     * @param name The requested provider name, or {@code null}
     * @param classLoader The class loader to search
     * @return The provider
     * @throws IllegalStateException if no suitable provider is available
     */
    public static OpenApiMapperProvider select(@Nullable String name, ClassLoader classLoader) {
        List<OpenApiMapperProvider> providers = findAll(classLoader);
        if (providers.isEmpty()) {
            throw new IllegalStateException("No OpenAPI mapper found. Add micronaut-openapi-jackson (or another "
                + OpenApiMapperProvider.class.getName() + " implementation) to the annotation processor classpath.");
        }
        if (name != null && !name.isBlank()) {
            String requested = name.trim().toLowerCase(Locale.ENGLISH);
            return providers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(requested))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OpenAPI mapper '" + requested + "' set by " + MAPPER_PROPERTY
                    + " was not found. Available: " + names(providers)));
        }
        if (providers.size() == 1) {
            return providers.get(0);
        }
        return providers.stream()
            .filter(p -> p.getName().equals(DEFAULT_PROVIDER))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Several OpenAPI mappers are available (" + names(providers)
                + "). Select one with " + MAPPER_PROPERTY + "."));
    }

    private static String names(List<OpenApiMapperProvider> providers) {
        return providers.stream().map(OpenApiMapperProvider::getName).collect(Collectors.joining(", "));
    }
}
