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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.management.endpoint.annotation.Delete;
import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Read;
import io.micronaut.management.endpoint.annotation.Selector;
import io.micronaut.management.endpoint.annotation.Write;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.tags.Tag;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut
 * controllers at compile time.
 *
 * @author croudet
 * @since 1.4
 */
@Experimental
public class OpenApiEndpointVisitor extends AbstractOpenApiEndpointVisitor<Endpoint, Object>
        implements TypeElementVisitor<Endpoint, Object> {
    private String id;
    private boolean skip;
    private HttpMethodDesciption methodDescription;

    @Override
    protected boolean ignore(ClassElement element, VisitorContext context) {
        EndpointsConfiguration cfg = OpenApiApplicationVisitor.endPointsConfiguration(context);
        if (!cfg.isEnabled()) {
            skip = true;
            return skip;
        }
        boolean endpoint = element.isAnnotationPresent(Endpoint.class);
        if (endpoint) {
            AnnotationValue<Endpoint> ann = element.getAnnotation(Endpoint.class);
            id = cfg.getPath() + ann.stringValue("id").orElse(NameUtils.hyphenate(element.getSimpleName()));
            if (id.charAt(0) != '/') {
                id = '/' + id;
            }
        }
        skip = !endpoint;
        return skip;
    }

    @Override
    protected boolean ignore(MethodElement element, VisitorContext context) {
        if (skip || element.isAnnotationPresent(Hidden.class)) {
            return true;
        }
        methodDescription = httpMethodDescription(element);
        return methodDescription == null;
    }

    @Override
    protected HttpMethod httpMethod(MethodElement element) {
        return methodDescription == null ? null : methodDescription.httpMethod;
    }

    @Override
    protected List<UriMatchTemplate> uriMatchTemplates(MethodElement element) {
        UriMatchTemplate uriTemplate = UriMatchTemplate.of(id);
        for (ParameterElement param : element.getParameters()) {
            if (param.hasAnnotation(Selector.class)) {
                uriTemplate = uriTemplate.nest("/{" + param.getName() + "}");
            }
        }
        return Collections.singletonList(uriTemplate);
    }

    @Override
    protected List<MediaType> consumesMediaTypes(MethodElement element) {
        return mediaTypes(methodDescription.consumes);
    }

    @Override
    protected List<MediaType> producesMediaTypes(MethodElement element) {
        return mediaTypes(methodDescription.produces);
    }

    @Override
    protected String description(MethodElement element) {
        return methodDescription.description;
    }

    @Override
    protected List<Tag> classTags(ClassElement element, VisitorContext context) {
        EndpointsConfiguration cfg = OpenApiApplicationVisitor.endPointsConfiguration(context);
        List<Tag> allTags = new ArrayList<>(cfg.getTags());
        allTags.addAll(context.get(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENDPOINT_CLASS_TAGS, List.class,
                Collections.emptyList()));
        return allTags;
    }

    private static List<MediaType> mediaTypes(String... arr) {
        if (arr == null || arr.length == 0) {
            return Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);
        }
        return Arrays.stream(arr).map(MediaType::of).collect(Collectors.toList());
    }

    private static HttpMethodDesciption httpMethodDescription(MethodElement element) {
        HttpMethodDesciption httpMethodDescription = methodDescription(element, Write.class, HttpMethod.POST);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        httpMethodDescription = methodDescription(element, Read.class, HttpMethod.GET);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        return methodDescription(element, Delete.class, HttpMethod.DELETE);
    }

    private static HttpMethodDesciption methodDescription(MethodElement element, Class<? extends Annotation> ann, HttpMethod httpMethod) {
        Optional<Class<? extends Annotation>> httpMethodOpt = element.getAnnotationTypeByStereotype(ann);
        if (httpMethodOpt.isPresent()) {
            AnnotationValue<?> annotation = element.getAnnotation(ann);
            return new HttpMethodDesciption(httpMethod, annotation.stringValue("description").orElse(null),
                    annotation.stringValues("produces"), annotation.stringValues("consumes"));
        }
        return null;
    }

    /**
     * Endpoint method description.
     *
     * @author croudet
     */
    private static class HttpMethodDesciption {
        HttpMethod httpMethod;
        String description;
        String[] produces;
        String[] consumes;

        HttpMethodDesciption(HttpMethod httpMethod, String description, String[] produces, String[] consumes) {
            this.httpMethod = httpMethod;
            this.description = description;
            this.produces = produces;
            this.consumes = consumes;
        }

        @Override
        public String toString() {
            return "HttpMethodDesciption [httpMethod=" + httpMethod + ", description=" + description + ", produces="
                    + Arrays.toString(produces) + ", consumes=" + Arrays.toString(consumes) + "]";
        }
    }
}
