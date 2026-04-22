/*
 * Copyright 2017-2025 original authors
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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.PathMatcher;
import io.micronaut.http.HttpMethod;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.security.InterceptUrlMapPattern;
import io.micronaut.openapi.visitor.security.SecurityProperties;
import io.micronaut.openapi.visitor.security.SecurityRule;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static io.micronaut.openapi.visitor.ConfigUtils.getSecurityProperties;

/**
 * Security util methods.
 *
 * @since 6.16.0
 */
@Internal
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static void readSecurityRequirements(MethodElement element, HttpMethod httpMethod, String path, Operation operation, List<SecurityRequirement> securityRequirements, VisitorContext context) {
        if (CollectionUtils.isNotEmpty(securityRequirements)) {
            for (SecurityRequirement securityItem : securityRequirements) {
                operation.addSecurityItem(securityItem);
            }
            return;
        }

        processMicronautSecurityConfig(element, httpMethod, path, operation, context);
    }

    private static void processMicronautSecurityConfig(MethodElement element, HttpMethod httpMethod, String path, Operation operation, VisitorContext context) {

        SecurityProperties securityProperties = getSecurityProperties(context);
        if (!securityProperties.isEnabled()
            || !securityProperties.isMicronautSecurityEnabled()
            || (!securityProperties.isTokenEnabled()
            && !securityProperties.isJwtEnabled()
            && !securityProperties.isBasicAuthEnabled()
            && !securityProperties.isOauth2Enabled()
        )) {
            return;
        }

        OpenAPI openApi = Utils.resolveOpenApi(context);
        Components components = openApi.getComponents();

        String securitySchemeName;
        if (components != null && CollectionUtils.isNotEmpty(components.getSecuritySchemes())) {
            securitySchemeName = components.getSecuritySchemes().keySet().iterator().next();
        } else {
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            if (components.getSecuritySchemes() == null) {
                components.setSecuritySchemes(new HashMap<>());
            }
            securitySchemeName = securityProperties.getDefaultSchemaName();
            SecurityScheme securityScheme = components.getSecuritySchemes().get(securitySchemeName);
            if (securityScheme == null) {
                securityScheme = new SecurityScheme();
                if (securityProperties.isOauth2Enabled()) {
                    securityScheme.setType(SecurityScheme.Type.OAUTH2);
                } else if (securityProperties.isBasicAuthEnabled()
                    || securityProperties.isTokenEnabled()
                    || securityProperties.isJwtEnabled()) {

                    securityScheme.setType(SecurityScheme.Type.HTTP);
                    if (securityProperties.isJwtEnabled()) {
                        securityScheme.setBearerFormat("JWT");
                    }
                }
                if (securityProperties.isJwtEnabled() || securityProperties.isJwtBearerEnabled()) {
                    securityScheme.setScheme("bearer");
                } else if (securityProperties.isBasicAuthEnabled()) {
                    securityScheme.setScheme("basic");
                }

                components.addSecuritySchemes(securitySchemeName, securityScheme);
            }
        }

        var classLevelSecuredAnn = element.getOwningType().getAnnotation("io.micronaut.security.annotation.Secured");
        var methodLevelSecuredAnn = element.getAnnotation("io.micronaut.security.annotation.Secured");
        List<String> access = Collections.emptyList();
        if (methodLevelSecuredAnn != null) {
            access = methodLevelSecuredAnn.getValue(Argument.LIST_OF_STRING).orElse(null);
        } else if (classLevelSecuredAnn != null) {
            access = classLevelSecuredAnn.getValue(Argument.LIST_OF_STRING).orElse(null);
        }
        processSecurityAccess(securitySchemeName, access, operation);

        List<InterceptUrlMapPattern> securityRules = securityProperties.getInterceptUrlMapPatterns();
        if (CollectionUtils.isNotEmpty(securityRules)) {
            for (InterceptUrlMapPattern securityRule : securityRules) {
                if (PathMatcher.ANT.matches(securityRule.getPattern(), path)
                    && (httpMethod == null || securityRule.getHttpMethod() == null || httpMethod == securityRule.getHttpMethod())) {

                    processSecurityAccess(securitySchemeName, securityRule.getAccess(), operation);
                }
            }
        }
    }

    private static void processSecurityAccess(String securitySchemeName, List<String> access, Operation operation) {
        if (securitySchemeName == null || CollectionUtils.isEmpty(access)) {
            return;
        }
        String firstAccessItem = access.getFirst();
        if (access.size() == 1 && (firstAccessItem.equals(SecurityRule.IS_ANONYMOUS) || firstAccessItem.equals(SecurityRule.DENY_ALL))) {
            return;
        }
        if (access.size() == 1 && firstAccessItem.equals(SecurityRule.IS_AUTHENTICATED)) {
            access = Collections.emptyList();
        }
        SecurityRequirement existedSecurityRequirement = null;
        List<String> existedSecList = null;
        if (CollectionUtils.isNotEmpty(operation.getSecurity())) {
            for (SecurityRequirement securityRequirement : operation.getSecurity()) {
                if (securityRequirement.containsKey(securitySchemeName)) {
                    existedSecList = securityRequirement.get(securitySchemeName);
                    existedSecurityRequirement = securityRequirement;
                    break;
                }
            }
        }
        if (existedSecList != null) {
            if (access.isEmpty()) {
                return;
            }
            if (existedSecList.isEmpty()) {
                existedSecurityRequirement.put(securitySchemeName, access);
            } else {
                var finalAccess = new HashSet<>(existedSecList);
                finalAccess.addAll(access);
                existedSecurityRequirement.put(securitySchemeName, new ArrayList<>(finalAccess));
            }
        } else {
            var securityRequirement = new SecurityRequirement();
            securityRequirement.put(securitySchemeName, access);
            operation.addSecurityItem(securityRequirement);
        }
    }

    /**
     * Reads the security requirements annotation of the specified element.
     *
     * @param element The Element to process.
     *
     * @return A list of SecurityRequirement
     */
    public static List<SecurityRequirement> readSecurityRequirements(Element element) {
        return readSecurityRequirements(element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.security.SecurityRequirement.class));
    }

    public static List<SecurityRequirement> readSecurityRequirements(List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> annotations) {
        var result = new ArrayList<SecurityRequirement>(annotations.size());
        for (var ann : annotations) {
            result.add(ConvertUtils.mapToSecurityRequirement(ann));
        }
        return result;
    }

    /**
     * Processes {@link io.swagger.v3.oas.annotations.security.SecurityScheme}
     * annotations.
     *
     * @param element The element
     * @param context The visitor context
     */
    public static void processSecuritySchemes(ClassElement element, VisitorContext context) {
        var values = element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.security.SecurityScheme.class);
        final OpenAPI openApi = Utils.resolveOpenApi(context);
        ConvertUtils.addSecuritySchemes(openApi, values, context);
    }
}
