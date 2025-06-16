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
package io.micronaut.openapi.visitor.management;

import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.ContextUtils;
import io.micronaut.openapi.visitor.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static io.micronaut.openapi.visitor.ConfigUtils.ALL_ENDPOINTS_NAME;
import static io.micronaut.openapi.visitor.ConfigUtils.ALL_SPRING_ACTUATOR_ENDPOINTS_NAME;
import static io.micronaut.openapi.visitor.ConfigUtils.getConfigProperty;
import static io.micronaut.openapi.visitor.ConfigUtils.getEnv;
import static io.micronaut.openapi.visitor.ConfigUtils.readOpenApiConfigFile;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_SPRING_OPENAPI_ENDPOINTS;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.SPRING_ENDPOINTS_CONTEXT_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.SPRING_ENDPOINTS_PREFIX;
import static io.micronaut.openapi.visitor.StringUtil.COMMA;
import static io.micronaut.openapi.visitor.StringUtil.DOT;
import static java.util.Map.entry;

/**
 * Spring Boot actuator Configuration utilities methods.
 *
 * @since 6.16.0
 */
@Internal
public final class SpringActuatorConfigUtils {

    public static final String DEFAULT_ACTUATOR_BASE_PATH = "/actuator";

    private SpringActuatorConfigUtils() {
    }

    public static SpringActuatorProperties getSpringActuatorProperties(VisitorContext context) {

        var springActuatorProperties = ContextUtils.get(MICRONAUT_INTERNAL_SPRING_OPENAPI_ENDPOINTS, SpringActuatorProperties.class, context);
        if (springActuatorProperties != null) {
            return springActuatorProperties;
        }

        springActuatorProperties = new SpringActuatorProperties();

        // first read system properties
        Properties sysProps = System.getProperties();
        readActuatorProperties(sysProps, springActuatorProperties, context);

        // second read openapi.properties file
        Properties fileProps = readOpenApiConfigFile(context);
        readActuatorProperties(fileProps, springActuatorProperties, context);

        // third read environments properties
        Environment environment = getEnv(context);
        if (environment != null) {
            for (Map.Entry<String, Object> entry : environment.getProperties(SPRING_ENDPOINTS_PREFIX, StringConvention.RAW).entrySet()) {
                setEndpointProperty(entry.getKey(), entry.getValue(), springActuatorProperties, context);
            }
        }

        ContextUtils.put(MICRONAUT_INTERNAL_SPRING_OPENAPI_ENDPOINTS, springActuatorProperties, context);

        return springActuatorProperties;
    }

    private static void readActuatorProperties(Properties props, SpringActuatorProperties springActuatorProperties, VisitorContext context) {
        for (String prop : props.stringPropertyNames()) {
            if (!prop.startsWith(SPRING_ENDPOINTS_PREFIX)) {
                continue;
            }
            int endpointNameIndexEnd = prop.indexOf('.', SPRING_ENDPOINTS_PREFIX.length() + 1);
            if (endpointNameIndexEnd < 0) {
                continue;
            }
            String propertyName = prop.substring(SPRING_ENDPOINTS_PREFIX.length() + 1, endpointNameIndexEnd).toLowerCase(Locale.ENGLISH);
            setEndpointProperty(propertyName, props.get(prop), springActuatorProperties, context);
        }
    }

    private static void setEndpointProperty(String propertyName, Object value, SpringActuatorProperties springActuatorProperties, VisitorContext context) {
        if (value == null) {
            return;
        }
        String valueStr = value.toString();
        propertyName = propertyName.toLowerCase(Locale.ENGLISH);
        var simplePropName = propertyName;
        if (propertyName.contains(DOT)) {
            simplePropName = propertyName.substring(0, propertyName.indexOf(DOT));
        }
        switch (simplePropName) {
            case "base-path", "basepath":
                if (springActuatorProperties.getBasePath() == null) {
                    springActuatorProperties.setBasePath(valueStr);
                }
                break;
            case "exposure":
                if (propertyName.contains("include")) {
                    if (springActuatorProperties.getIncludedEndpoints() == null) {
                        var includedEndpoints = new ArrayList<String>();
                        if (value instanceof List<?> includedList) {
                            for (var endpointName : includedList) {
                                includedEndpoints.add(endpointName.toString());
                            }
                        } else if (valueStr != null) {
                            for (var endpointName : valueStr.split(COMMA)) {
                                includedEndpoints.add(endpointName.trim().toLowerCase(Locale.ENGLISH));
                            }
                        }
                        springActuatorProperties.setIncludedEndpoints(includedEndpoints);
                    }
                }
                if (propertyName.contains("exclude")) {
                    if (springActuatorProperties.getExcludedEndpoints() == null) {
                        var excludedEndpoints = new ArrayList<String>();
                        if (value instanceof List<?> excludedList) {
                            for (var endpointName : excludedList) {
                                excludedEndpoints.add(endpointName.toString());
                            }
                        } else if (valueStr != null) {
                            for (var endpointName : valueStr.split(COMMA)) {
                                excludedEndpoints.add(endpointName.trim().toLowerCase(Locale.ENGLISH));
                            }
                            springActuatorProperties.setIncludedEndpoints(excludedEndpoints);
                        }
                    }
                }
                break;
            case "path-mapping", "pathmapping":
                var endpointPaths = springActuatorProperties.getPathMapping() != null ? springActuatorProperties.getPathMapping() : new HashMap<String, String>();
                var endpointName = propertyName.substring(simplePropName.length() + 1);
                if (!endpointPaths.containsKey(endpointName)) {
                    endpointPaths.put(endpointName, valueStr);
                }
                springActuatorProperties.setPathMapping(endpointPaths);
                break;
            default:
                break;
        }
    }

    public static void mergeWithActuatorProperties(EndpointsConfig endpointsConfig, VisitorContext context) {

        // check is spring-boot-actuator in dependencies
        if (!Utils.isTestSpringActuator() && ContextUtils.getClassElement("org.springframework.boot.actuate.endpoint.InvocationContext", context) == null) {
            return;
        }

        var endpointPropertiesMap = endpointsConfig.getEndpoints();

        var springActuatorProperties = getSpringActuatorProperties(context);
        var allEnabled = (CollectionUtils.isEmpty(springActuatorProperties.getIncludedEndpoints()) && CollectionUtils.isEmpty(springActuatorProperties.getExcludedEndpoints()))
            || springActuatorProperties.getIncludedEndpoints().contains(ALL_SPRING_ACTUATOR_ENDPOINTS_NAME);
        var enabledEndpointIds = new ArrayList<String>();
        var enabledEndpoints = new ArrayList<Map.Entry<String, String>>();
        if (allEnabled) {
            enabledEndpoints.addAll(EndpointUtils.ALL_SPRING_ACTUATOR_ENDPOINTS.entrySet());
            enabledEndpointIds.addAll(EndpointUtils.ALL_SPRING_ACTUATOR_ENDPOINTS.keySet());
        } else {
            for (var endpointName : springActuatorProperties.getIncludedEndpoints()) {
                if (!EndpointUtils.ALL_SPRING_ACTUATOR_ENDPOINTS.containsKey(endpointName)) {
                    warn("Unknown actuator endpoint: " + endpointName + ". Skip it", context);
                    continue;
                }
                enabledEndpoints.add(entry(endpointName, EndpointUtils.ALL_SPRING_ACTUATOR_ENDPOINTS.get(endpointName)));
                enabledEndpointIds.add(endpointName);
            }
        }
        for (var entry : enabledEndpoints) {
            var endpointName = entry.getKey();
            var endpointProperties = endpointPropertiesMap.get(endpointName);
            if (endpointProperties == null) {
                endpointProperties = new EndpointProperties(endpointName);
                endpointPropertiesMap.put(endpointName, endpointProperties);
            }
            if (endpointProperties.getElement() == null) {
                var classEl = ContextUtils.getClassElement(entry.getValue(), context);
                if (classEl != null) {
                    endpointProperties.setElement(classEl);
                }
            }
            if (endpointProperties.getEnabled() == null) {
                endpointProperties.setEnabled(true);
            }
            if (endpointProperties.getPath() == null && CollectionUtils.isNotEmpty(springActuatorProperties.getPathMapping())) {
                endpointProperties.setPath(springActuatorProperties.getPathMapping().get(endpointName));
            }
        }

        // check disabled endpoints
        for (var endpointProperties : endpointsConfig.getEndpoints().values()) {
            if (!enabledEndpointIds.contains(endpointProperties.getId())) {
                endpointProperties.setEnabled(false);
            }
        }
        // check excluded endpoints
        if (CollectionUtils.isNotEmpty(springActuatorProperties.getExcludedEndpoints())) {
            for (var endpointName : springActuatorProperties.getExcludedEndpoints()) {
                var endpointProperties = endpointsConfig.getEndpoints().get(endpointName);
                if (endpointProperties != null) {
                    endpointProperties.setEnabled(false);
                }
            }
        }

        var allEndpointsProperties = endpointsConfig.getEndpoints().get(ALL_ENDPOINTS_NAME);
        var springActuatorContextPath = getConfigProperty(SPRING_ENDPOINTS_CONTEXT_PATH, context);
        if (StringUtils.isNotEmpty(springActuatorProperties.getBasePath())
            || StringUtils.isNotEmpty(springActuatorContextPath)) {

            if (allEndpointsProperties == null) {
                allEndpointsProperties = new EndpointProperties(ALL_ENDPOINTS_NAME);
            }
            if (allEnabled && allEndpointsProperties.getEnabled() == null) {
                allEndpointsProperties.setEnabled(true);
            }
            if (allEndpointsProperties.getPath() == null) {
                allEndpointsProperties.setPath(springActuatorProperties.getBasePath());
            }
            if (allEndpointsProperties.getContextPath() == null) {
                allEndpointsProperties.setContextPath(springActuatorContextPath);
            }
        }
        if (StringUtils.isEmpty(endpointsConfig.getPath())) {
            endpointsConfig.setPath(springActuatorProperties.getBasePath());
        }
        if (StringUtils.isEmpty(endpointsConfig.getPath())) {
            endpointsConfig.setPath(DEFAULT_ACTUATOR_BASE_PATH);
        }
    }
}
