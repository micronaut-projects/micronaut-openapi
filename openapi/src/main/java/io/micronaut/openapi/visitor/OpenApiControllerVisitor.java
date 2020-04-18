/*
 * Copyright 2017-2019 original authors
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

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.value.OptionalValues;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.HttpMethodMapping;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.UriMapping;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.Hidden;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
@Experimental
public class OpenApiControllerVisitor extends AbstractOpenApiEndpointVisitor<Controller, HttpMethodMapping> implements TypeElementVisitor<Controller, HttpMethodMapping> {

    @Override
    protected boolean ignore(ClassElement element, VisitorContext context) {
        return !element.isAnnotationPresent(Controller.class);
    }

    @Override
    protected boolean ignore(MethodElement element, VisitorContext context) {
        return element.isAnnotationPresent(Hidden.class);
    }

    @Override
    protected HttpMethod httpMethod(MethodElement element) {
        Optional<Class<? extends Annotation>> httpMethodOpt = element
                .getAnnotationTypeByStereotype(HttpMethodMapping.class);
        if (!httpMethodOpt.isPresent()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(httpMethodOpt.get().getSimpleName().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return null;
    }

    @Override
    protected List<MediaType> consumesMediaTypes(MethodElement element) {
        return mediaTypes(element, Consumes.class);
    }

    @Override
    protected List<MediaType> producesMediaTypes(MethodElement element) {
        return mediaTypes(element, Produces.class);
    }

    private List<MediaType> mediaTypes(MethodElement element, Class<? extends Annotation> ann) {
        OptionalValues<List> consumesMediaTypes = element.getValues(ann, List.class);
        if (consumesMediaTypes.isEmpty()) {
            return Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);
        } else {
            List<MediaType> mediaTypes = new ArrayList<>();
            consumesMediaTypes.forEach((key, list) ->
                list.stream().map(mt -> MediaType.of(mt.toString())).forEach(mt -> mediaTypes.add((MediaType) mt)));
            return mediaTypes;
        }
    }

    @Override
    protected UriMatchTemplate uriMatchTemplate(MethodElement element) {
        String controllerValue = element.getDeclaringType().getValue(UriMapping.class, String.class).orElse("/");
        controllerValue = getPropertyPlaceholderResolver().resolvePlaceholders(controllerValue).orElse(controllerValue);
        UriMatchTemplate matchTemplate = UriMatchTemplate.of(controllerValue);
        String methodValue = element.getValue(HttpMethodMapping.class, String.class).orElse("/");
        methodValue = getPropertyPlaceholderResolver().resolvePlaceholders(methodValue).orElse(methodValue);
        return matchTemplate.nest(methodValue);
    }

    @Override
    protected String description(MethodElement element) {
        return null;
    }

    @Override
    protected List<io.swagger.v3.oas.models.tags.Tag> classTags(ClassElement element, VisitorContext context) {
        return Collections.emptyList();
    }
}
