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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
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
    public static final String ENDPOINTS_SERVERS = ENDPOINTS_PREFIX + "servers";
    public static final String ENDPOINTS_SECURITY_REQUIREMENTS = ENDPOINTS_PREFIX + "security-requirements";

    private final boolean enabled;
    private String path;
    private List<Tag> tags;
    private List<Server> servers;
    private List<SecurityRequirement> securityRequirements;
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
        servers = parseServers(properties.getProperty(ENDPOINTS_SERVERS, ""), context);
        securityRequirements = parseSecurityRequirements(properties.getProperty(ENDPOINTS_SECURITY_REQUIREMENTS, ""), context);
        endpoints = new LinkedHashMap<>();
        Map<String, String> map = new HashMap<>(properties.size());
        properties.forEach((key, value) -> map.put((String) key, (String) value));
        map.entrySet().stream()
                .filter(EndpointsConfiguration::validEntry)
                .forEach(entry -> {
                    int idx = entry.getKey().lastIndexOf('.');
                    if (idx <= 0 || idx == entry.getKey().length() || entry.getValue() == null) {
                        return;
                    }
                    String entryType = entry.getKey().substring(idx + 1);
                    String name = entry.getKey().substring(ENDPOINTS_PREFIX.length(), idx);
                    if ("security-requirements".equals(entryType)) {
                        Endpoint endpoint = endpoints.computeIfAbsent(name, key -> new Endpoint());
                        endpoint.setSecurityRequirements(parseSecurityRequirements(entry.getValue(), context));
                    } else  if ("servers".equals(entryType)) {
                        Endpoint endpoint = endpoints.computeIfAbsent(name, key -> new Endpoint());
                        endpoint.setServers(parseServers(entry.getValue(), context));
                    } else if ("tags".equals(entryType)) {
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

    /**
     * The list of global servers to add to all Endpoints.
     * @return the list of global servers to add to all Endpoints.
     */
    List<Server> getServers() {
        return servers;
    }

    /**
     * The list of global security requirements to add to all Endpoints.
     * @return the list of global security requirements to add to all Endpoints.
     */
    List<SecurityRequirement> getSecurityRequirements() {
        return securityRequirements;
    }

    @Override
    public String toString() {
        return "EndpointsConfiguration [enabled=" + enabled + ", path=" + path + ", tags=" + tags + ", endpoints="
                + endpoints + "]";
    }

    private static List<Server> parseServers(String servers, VisitorContext context)  {
        return parseModel(servers, context, new TypeReference<List<Server>>() { });
    }

    private static List<SecurityRequirement> parseSecurityRequirements(String securityRequirements, VisitorContext context)  {
        return parseModel(securityRequirements, context, new TypeReference<List<SecurityRequirement>>() { });
    }

    private static <T> List<T> parseModel(String s, VisitorContext context, TypeReference<List<T>> typeReference)  {
        if (s == null || s.isEmpty() || (! s.startsWith("[") && ! s.endsWith("]"))) {
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(s, typeReference);
        } catch (JsonProcessingException e) {
            context.warn("Fail to parse " + typeReference.getType().toString() + ": " + s + " - " + e.getMessage(), null);
        }
        return Collections.emptyList();
    }

    private static boolean validEntry(Map.Entry<String, String> entry) {
        return entry.getKey().startsWith(ENDPOINTS_PREFIX)
        && !entry.getKey().equals(ENDPOINTS_ENABLED)
        && !entry.getKey().equals(ENDPOINTS_TAGS)
        && !entry.getKey().equals(ENDPOINTS_SERVERS)
        && !entry.getKey().equals(ENDPOINTS_SECURITY_REQUIREMENTS);
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

}
