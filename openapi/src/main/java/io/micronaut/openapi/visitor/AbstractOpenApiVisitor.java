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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.UrlUtils.OpPath;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.micronaut.openapi.visitor.InternalExt.MICRONAUT_OP_POSTFIX;
import static io.micronaut.openapi.visitor.UrlUtils.buildUrls;
import static io.micronaut.openapi.visitor.UrlUtils.parsePathSegments;

/**
 * Abstract base class for OpenAPI visitors.
 *
 * @author graemerocher
 * @since 1.0
 */
@Internal
abstract class AbstractOpenApiVisitor {

    private static final Lock VISITED_ELEMENTS_LOCK = new ReentrantLock();

    /**
     * Increments the number of visited elements.
     *
     * @param context The context
     */
    void incrementVisitedElements(VisitorContext context) {
        VISITED_ELEMENTS_LOCK.lock();
        try {
            ContextUtils.put(Utils.ATTR_VISITED_ELEMENTS, ContextUtils.getVisitedElements(context) + 1, context);
        } finally {
            VISITED_ELEMENTS_LOCK.unlock();
        }
    }

    /**
     * Returns the number of visited elements.
     *
     * @param context The context.
     *
     * @return The number of visited elements.
     */
    int visitedElements(VisitorContext context) {
        VISITED_ELEMENTS_LOCK.lock();
        try {
            return ContextUtils.getVisitedElements(context);
        } finally {
            VISITED_ELEMENTS_LOCK.unlock();
        }
    }

    /**
     * Reads the security requirements annotation of the specified element.
     *
     * @param element The Element to process.
     *
     * @return A list of SecurityRequirement
     */
    List<SecurityRequirement> readSecurityRequirements(Element element) {
        return readSecurityRequirements(element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.security.SecurityRequirement.class));
    }

    List<SecurityRequirement> readSecurityRequirements(List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> annotations) {
        var result = new ArrayList<SecurityRequirement>(annotations.size());
        for (var ann : annotations) {
            result.add(ConvertUtils.mapToSecurityRequirement(ann));
        }
        return result;
    }

    /**
     * Resolve the PathItem for the given {@link UriMatchTemplate}.
     *
     * @param context The context
     * @param matchTemplates The match templates
     *
     * @return The {@link PathItem}
     */
    Map<String, List<PathItem>> resolvePathItems(VisitorContext context, List<UriMatchTemplate> matchTemplates) {
        OpenAPI openAPI = Utils.resolveOpenApi(context);
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            paths = new Paths();
            openAPI.setPaths(paths);
        }

        var resultPathItemsMap = new HashMap<String, List<PathItem>>();

        for (UriMatchTemplate matchTemplate : matchTemplates) {
            var segments = parsePathSegments(matchTemplate.toPathString());
            var finalPaths = buildUrls(segments, context);

            for (OpPath finalPath : finalPaths) {
                List<PathItem> resultPathItems = resultPathItemsMap.computeIfAbsent(finalPath.url(), k -> new ArrayList<>());
                var pathItem = paths.computeIfAbsent(finalPath.url(), key -> new PathItem());
                var opIdPostfix = finalPath.opIdPostfix();
                if (!opIdPostfix.isEmpty()) {
                    pathItem.addExtension(MICRONAUT_OP_POSTFIX, opIdPostfix);
                }
                resultPathItems.add(pathItem);
            }
        }

        return resultPathItemsMap;
    }

    /**
     * Processes {@link SecurityScheme}
     * annotations.
     *
     * @param element The element
     * @param context The visitor context
     */
    protected void processSecuritySchemes(ClassElement element, VisitorContext context) {
        var values = element.getAnnotationValuesByType(SecurityScheme.class);
        final OpenAPI openApi = Utils.resolveOpenApi(context);
        ConvertUtils.addSecuritySchemes(openApi, values, context);
    }
}
