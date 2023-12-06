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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import static io.micronaut.openapi.visitor.ConfigUtils.endpointsConfiguration;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS;
import static io.micronaut.openapi.visitor.ContextUtils.SERVERS_LIST_ARGUMENT;
import static io.micronaut.openapi.visitor.ContextUtils.TAGS_LIST_ARGUMENT;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.Utils.DEFAULT_MEDIA_TYPES;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut
 * controllers at compile time.
 *
 * @author croudet
 * @since 1.4
 */
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiEndpointVisitor extends AbstractOpenApiEndpointVisitor implements TypeElementVisitor<Object, Object> {

    private String id;
    private HttpMethodDescription methodDescription;

    private Boolean enabled;
    private String path;
    private List<Server> servers;
    private List<Tag> tags;
    private List<SecurityRequirement> securityRequirements;

    private List<AnnotationValue<io.swagger.v3.oas.annotations.tags.Tag>> additionalTags;
    private List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements;

    public OpenApiEndpointVisitor() {
    }

    public OpenApiEndpointVisitor(boolean enabled) {
        this.enabled = enabled;
    }

    public OpenApiEndpointVisitor(boolean enabled,
                                  List<AnnotationValue<io.swagger.v3.oas.annotations.tags.Tag>> additionalTags,
                                  List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements) {
        this.enabled = enabled;
        this.additionalTags = additionalTags;
        this.additionalSecurityRequirements = additionalSecurityRequirements;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
            return;
        }
        EndpointsConfiguration cfg = endpointsConfiguration(context);
        if (enabled == null) {
            enabled = cfg.isEnabled();
        }
        if (path == null) {
            path = cfg.getPath();
            if (path == null) {
                path = "/";
            }
        }
        if (servers == null) {
            servers = cfg.getServers();
            if (servers == null) {
                servers = Collections.emptyList();
            }
        }
        if (tags == null) {
            tags = cfg.getTags();
            if (tags == null) {
                tags = Collections.emptyList();
            }
        }
        if (securityRequirements == null) {
            securityRequirements = cfg.getSecurityRequirements();
            if (securityRequirements == null) {
                securityRequirements = Collections.emptyList();
            }
        }
        if (additionalTags != null) {
            if (tags == null) {
                tags = readTags(additionalTags, context);
            } else {
                tags = new ArrayList<>(tags);
                tags.addAll(readTags(additionalTags, context));
            }
        }
        if (additionalSecurityRequirements != null) {
            if (securityRequirements == null) {
                securityRequirements = readSecurityRequirements(additionalSecurityRequirements);
            } else {
                securityRequirements = new ArrayList<>(securityRequirements);
                securityRequirements.addAll(readSecurityRequirements(additionalSecurityRequirements));
            }
        }
        super.visitClass(element, context);
    }

    @Override
    protected boolean ignore(ClassElement element, VisitorContext context) {
        if (!enabled) {
            return true;
        }
        if (element.isAnnotationPresent("io.micronaut.management.endpoint.annotation.Endpoint")) {
            AnnotationValue<?> ann = element.getAnnotation("io.micronaut.management.endpoint.annotation.Endpoint");
            String idAnn = ann.stringValue("id").orElse(NameUtils.hyphenate(element.getSimpleName()));
            if (idAnn.isEmpty()) {
                idAnn = ann.stringValue("value").orElse(idAnn);
            }
            id = path + idAnn;
            if (id.isEmpty() || id.charAt(0) != '/') {
                id = "/" + id;
            }
            return false;
        }
        return true;
    }

    @Override
    protected boolean ignore(MethodElement element, VisitorContext context) {
        if (!enabled) {
            return true;
        }

        AnnotationValue<Operation> operationAnn = element.getAnnotation(Operation.class);
        boolean isHidden = operationAnn != null && operationAnn.booleanValue("hidden").orElse(false);
        AnnotationValue<JsonAnySetter> jsonAnySetterAnn = element.getAnnotation(JsonAnySetter.class);

        if (isHidden || element.isAnnotationPresent(Hidden.class)
            || (jsonAnySetterAnn != null && jsonAnySetterAnn.booleanValue("enabled").orElse(true))) {
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
    protected List<UriMatchTemplate> uriMatchTemplates(MethodElement element, VisitorContext context) {
        UriMatchTemplate uriTemplate = UriMatchTemplate.of(id);
        for (ParameterElement param : element.getParameters()) {
            if (param.hasAnnotation("io.micronaut.management.endpoint.annotation.Selector")) {
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
        List<Tag> allTags = new ArrayList<>(tags);
        allTags.addAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS, TAGS_LIST_ARGUMENT, Collections.emptyList(), context));
        return allTags;
    }

    @Override
    protected List<Server> methodServers(MethodElement element, VisitorContext context) {
        List<Server> servers = new ArrayList<>(this.servers);
        servers.addAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS, SERVERS_LIST_ARGUMENT, Collections.emptyList(), context));
        return servers;
    }

    @Override
    protected List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context) {
        List<SecurityRequirement> securityRequirements = new ArrayList<>(this.securityRequirements);
        securityRequirements.addAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, List.class, Collections.emptyList(), context));
        return securityRequirements;
    }

    @Override
    public int getOrder() {
        return 40;
    }

    private static List<MediaType> mediaTypes(String... arr) {
        if (ArrayUtils.isEmpty(arr)) {
            return DEFAULT_MEDIA_TYPES;
        }
        return Arrays.stream(arr)
            .map(MediaType::of)
            .toList();
    }

    private static HttpMethodDescription httpMethodDescription(MethodElement element) {
        HttpMethodDescription httpMethodDescription = methodDescription(element, "io.micronaut.management.endpoint.annotation.Write", HttpMethod.POST);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        httpMethodDescription = methodDescription(element, "io.micronaut.management.endpoint.annotation.Read", HttpMethod.GET);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        return methodDescription(element, "io.micronaut.management.endpoint.annotation.Delete", HttpMethod.DELETE);
    }

    private static HttpMethodDescription methodDescription(MethodElement element, String endpointManagementAnnName, HttpMethod httpMethod) {
        if (element.isAnnotationPresent(endpointManagementAnnName)) {
            AnnotationValue<?> annotation = element.getAnnotation(endpointManagementAnnName);
            assert annotation != null;
            return new HttpMethodDescription(httpMethod, annotation.stringValue("description").orElse(null),
                annotation.stringValues("produces"), annotation.stringValues("consumes"));
        }
        return null;
    }

    /**
     * Endpoint method description.
     *
     * @author croudet
     */
    private static class HttpMethodDescription {

        HttpMethod httpMethod;
        String description;
        String[] produces;
        String[] consumes;

        HttpMethodDescription(HttpMethod httpMethod, String description, String[] produces, String[] consumes) {
            this.httpMethod = httpMethod;
            this.description = description;
            this.produces = produces;
            this.consumes = consumes;
        }

        @Override
        public String toString() {
            return "HttpMethodDescription [httpMethod=" + httpMethod + ", description=" + description + ", produces="
                + Arrays.toString(produces) + ", consumes=" + Arrays.toString(consumes) + "]";
        }
    }

}
