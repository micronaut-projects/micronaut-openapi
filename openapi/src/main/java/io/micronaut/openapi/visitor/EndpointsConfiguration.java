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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

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
        String enabledStr = OpenApiApplicationVisitor.getConfigurationProperty(ENDPOINTS_ENABLED, context);
        enabled = StringUtils.hasText(enabledStr) && Boolean.parseBoolean(enabledStr);
        if (!enabled) {
            return;
        }
        path = parsePath(OpenApiApplicationVisitor.getConfigurationProperty(ENDPOINTS_PATH, context));
        tags = parseTags(OpenApiApplicationVisitor.getConfigurationProperty(ENDPOINTS_TAGS, context));
        servers = parseServers(OpenApiApplicationVisitor.getConfigurationProperty(ENDPOINTS_SERVERS, context), context);
        securityRequirements = parseSecurityRequirements(OpenApiApplicationVisitor.getConfigurationProperty(ENDPOINTS_SECURITY_REQUIREMENTS, context), context);
        endpoints = new LinkedHashMap<>();
        Map<String, String> map = new HashMap<>(properties.size());
        properties.forEach((key, value) -> map.put((String) key, (String) value));
        map.entrySet().stream()
                .filter(EndpointsConfiguration::validEntry)
                .forEach(entry -> {
                    int idx = entry.getKey().lastIndexOf('.');
                    if (idx <= 0 || idx == entry.getKey().length() - 1 || entry.getValue() == null) {
                        return;
                    }
                    String entryType = entry.getKey().substring(idx + 1);
                    String name = entry.getKey().substring(ENDPOINTS_PREFIX.length(), idx);
                    Endpoint endpoint = endpoints.computeIfAbsent(name, key -> new Endpoint());
                    if ("security-requirements".equals(entryType)) {
                        endpoint.setSecurityRequirements(parseSecurityRequirements(entry.getValue(), context));
                    } else  if ("servers".equals(entryType)) {
                        endpoint.setServers(parseServers(entry.getValue(), context));
                    } else if ("tags".equals(entryType)) {
                        endpoint.setTags(parseTags(entry.getValue()));
                    } else if ("class".equals(entryType)) {
                        endpoint.setClassElement(context.getClassElement(entry.getValue()));
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
        return parseModel(servers, context, new TypeReference<>() { });
    }

    private static List<SecurityRequirement> parseSecurityRequirements(String securityRequirements, VisitorContext context)  {
        return parseModel(securityRequirements, context, new TypeReference<>() { });
    }

    private static <T> List<T> parseModel(String s, VisitorContext context, TypeReference<List<T>> typeReference)  {
        if (StringUtils.isEmpty(s) || (!s.startsWith("[") && !s.endsWith("]"))) {
            return Collections.emptyList();
        }
        try {
            return ConvertUtils.getConvertJsonMapper().readValue(s, typeReference);
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
        && !entry.getKey().equals(ENDPOINTS_SECURITY_REQUIREMENTS)
        && !entry.getKey().equals(ENDPOINTS_PATH);
    }

    private static List<Tag> parseTags(String stringTagsStr) {

        if (StringUtils.isEmpty(stringTagsStr)) {
            return Collections.emptyList();
        }
        String[] stringTags = stringTagsStr.split(",");
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

    private static String parsePath(String path) {
        if (StringUtils.isNotEmpty(path) && !path.endsWith("/")) {
            return path + "/";
        } else {
            return path;
        }
    }

}
