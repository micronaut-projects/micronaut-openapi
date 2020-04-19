/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Endpoints configuration.
 *
 * @author croudet
 */
class EndpointsConfiguration {
    private static final String ENDPOINTS_PREFIX = "endpoints.";

    public static final String ENDPOINTS_ENABLED = ENDPOINTS_PREFIX + "enabled";
    public static final String ENDPOINTS_TAGS = ENDPOINTS_PREFIX + "tags";
    public static final String ENDPOINTS_PATH = ENDPOINTS_PREFIX + "path";

    private boolean enabled;
    private String path;
    private List<Tag> tags;
    private Map<String, Endpoint> endpoints;

    /**
     * List of Endpoints to process.
     *
     * @param context The VisitorContext.
     * @param properties The properties to process.
     */
    EndpointsConfiguration(VisitorContext context, Properties properties) {
        enabled = Boolean.parseBoolean(properties.getProperty(ENDPOINTS_ENABLED, Boolean.FALSE.toString()));
        if (!enabled) {
            return;
        }
        path = properties.getProperty(ENDPOINTS_PATH, "");
        tags = parseTags(properties.getProperty(ENDPOINTS_TAGS, "").split(","));
        endpoints = new LinkedHashMap<>();
        Map<String, String> map = new HashMap<>(properties.size());
        properties.forEach((key, value) -> map.put((String) key, (String) value));
        map.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ENDPOINTS_PREFIX)
                        && !entry.getKey().equals(ENDPOINTS_ENABLED) && !entry.getKey().equals(ENDPOINTS_TAGS))
                .forEach(entry -> {
                    int idx = entry.getKey().lastIndexOf('.');
                    if (idx <= 0 || idx == entry.getKey().length() || entry.getValue() == null) {
                        return;
                    }
                    String entryType = entry.getKey().substring(idx + 1);
                    String name = entry.getKey().substring(ENDPOINTS_PREFIX.length(), idx);
                    if ("tags".equals(entryType)) {
                        Endpoint endpoint = endpoints.computeIfAbsent(name, key -> new Endpoint());
                        endpoint.setTags(parseTags(entry.getValue().split(",")));
                    } else if ("class".equals(entryType)) {
                        Endpoint endpoint = endpoints.computeIfAbsent(name, key -> new Endpoint());
                        endpoint.setClassElement(context.getClassElement(entry.getValue()));
                    } else {
                        return;
                    }
                });
    }

    /**
     * Returns the base path for all Endpoints.
     * @return A path.
     */
    String getPath() {
        return path;
    }

    private static List<Tag> parseTags(String... stringTags) {
        if (stringTags.length == 0) {
            return Collections.emptyList();
        }
        List<Tag> tags = new ArrayList<>(stringTags.length);
        for (String name : stringTags) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            Tag tag = new Tag();
            tag.setName(name);
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Returns true if processing of Endpoints is enabled.
     * @return true if processing of Endpoints is enabled.
     */
    boolean isEnabled() {
        return enabled;
    }

    /**
     * The list of global tags to add to all Endpoints.
     * @return the list of global tags to add to all Endpoints.
     */
    List<Tag> getTags() {
        return tags;
    }

    /**
     * The Endpoints to process.
     * @return The Endpoints to process.
     */
    Map<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public String toString() {
        return "EndpointsConfiguration [enabled=" + enabled + ", path=" + path + ", tags=" + tags + ", endpoints="
                + endpoints + "]";
    }

}
