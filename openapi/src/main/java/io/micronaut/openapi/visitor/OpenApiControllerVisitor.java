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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Experimental;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
@Experimental
public class OpenApiControllerVisitor extends AbstractOpenApiEndpointVisitor implements TypeElementVisitor<Controller, HttpMethodMapping> {

    private final List<AnnotationValue<Tag>> additionalTags;
    private final List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements;

    public OpenApiControllerVisitor() {
        this.additionalTags = Collections.emptyList();
        this.additionalSecurityRequirements = Collections.emptyList();
    }

    public OpenApiControllerVisitor(List<AnnotationValue<Tag>> additionalTags,
                                    List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements) {
        this.additionalTags = additionalTags;
        this.additionalSecurityRequirements = additionalSecurityRequirements;
    }

    @Override
    protected boolean ignore(ClassElement element, VisitorContext context) {
        return !element.isAnnotationPresent(Controller.class);
    }

    @Override
    protected boolean ignore(MethodElement element, VisitorContext context) {
        return element.isPrivate() || element.isStatic() || element.isAnnotationPresent(Hidden.class);
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
        String[] values = element.stringValues(ann);
        if (values.length == 0) {
            return Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);
        } else {
            return Arrays.stream(values).map(MediaType::of).distinct().collect(Collectors.toList());
        }
    }

    @Override
    protected List<UriMatchTemplate> uriMatchTemplates(MethodElement element) {
        String controllerValue = element.getOwningType().getValue(UriMapping.class, String.class).orElse(element.getDeclaringType().getValue(UriMapping.class, String.class).orElse("/"));
        controllerValue = getPropertyPlaceholderResolver().resolvePlaceholders(controllerValue).orElse(controllerValue);
        UriMatchTemplate matchTemplate = UriMatchTemplate.of(controllerValue);
        // check if we have multiple uris
        String[] uris = element.stringValues(HttpMethodMapping.class, "uris");
        if (uris.length == 0) {
            String methodValue = element.getValue(HttpMethodMapping.class, String.class).orElse("/");
            methodValue = getPropertyPlaceholderResolver().resolvePlaceholders(methodValue).orElse(methodValue);
            return Collections.singletonList(matchTemplate.nest(methodValue));
        } else {
            List<UriMatchTemplate> matchTemplates = new ArrayList<>(uris.length);
            for (String methodValue: uris) {
                methodValue = getPropertyPlaceholderResolver().resolvePlaceholders(methodValue).orElse(methodValue);
                matchTemplates.add(matchTemplate.nest(methodValue));
            }
            return matchTemplates;
        }
    }

    @Override
    protected String description(MethodElement element) {
        return null;
    }

    @Override
    protected List<io.swagger.v3.oas.models.tags.Tag> classTags(ClassElement element, VisitorContext context) {
        return readTags(additionalTags, context);
    }

    @Override
    protected List<Server> methodServers(MethodElement element, VisitorContext context) {
        return processOpenApiAnnotation(
                element,
                context,
                io.swagger.v3.oas.annotations.servers.Server.class,
                io.swagger.v3.oas.models.servers.Server.class,
                Collections.emptyList()
        );
    }

    @Override
    protected List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context) {
        List<SecurityRequirement> securityRequirements = readSecurityRequirements(element);
        if (!additionalSecurityRequirements.isEmpty()) {
            securityRequirements = new ArrayList<>(securityRequirements);
            securityRequirements.addAll(readSecurityRequirements(additionalSecurityRequirements));
        }
        return securityRequirements;
    }
}
