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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.management.EndpointProperties;
import io.micronaut.openapi.visitor.management.SpringActuatorUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.jspecify.annotations.NonNull;

import javax.annotation.processing.SupportedOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.micronaut.openapi.visitor.ConfigUtils.ALL_ENDPOINTS_NAME;
import static io.micronaut.openapi.visitor.ConfigUtils.getEndpointsConfig;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_DESCRIPTION;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_EXTENSIONS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_PROPS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS;
import static io.micronaut.openapi.visitor.ContextUtils.EXTENSIONS_MAP_ARGUMENT;
import static io.micronaut.openapi.visitor.ContextUtils.SERVERS_LIST_ARGUMENT;
import static io.micronaut.openapi.visitor.ContextUtils.TAGS_LIST_ARGUMENT;
import static io.micronaut.openapi.visitor.ElementUtils.isIgnored;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_HIDDEN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ID;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_VALUE;
import static io.micronaut.openapi.visitor.SecurityUtils.readSecurityRequirements;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;
import static io.micronaut.openapi.visitor.TagUtils.readTags;
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
    private String description;
    private Map<String, Object> extensions;
    private List<Server> servers;
    private List<Tag> tags;
    private List<SecurityRequirement> securityRequirements;

    private List<AnnotationValue<Extension>> additionalExtensions;
    private List<AnnotationValue<io.swagger.v3.oas.annotations.tags.Tag>> additionalTags;
    private List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements;

    public OpenApiEndpointVisitor() {
    }

    public OpenApiEndpointVisitor(boolean enabled) {
        this.enabled = enabled;
    }

    public OpenApiEndpointVisitor(boolean enabled,
                                  String description,
                                  List<AnnotationValue<Extension>> additionalExtensions,
                                  List<AnnotationValue<io.swagger.v3.oas.annotations.tags.Tag>> additionalTags,
                                  List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements) {
        this.enabled = enabled;
        this.description = description;
        this.additionalExtensions = additionalExtensions;
        this.additionalTags = additionalTags;
        this.additionalSecurityRequirements = additionalSecurityRequirements;
    }

    @Override
    public void start(@NonNull VisitorContext context) {
        Utils.init(context);
    }

    @Override
    public @NonNull Set<String> getSupportedAnnotationNames() {
        return Set.of(
            "io.micronaut.management.endpoint.annotation.Endpoint",
            "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint"
        );
    }

    @Override
    public void visitClass(@NonNull ClassElement element, @NonNull VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
            return;
        }
        var endpointsConfig = getEndpointsConfig(context);
        if (enabled == null) {
            enabled = endpointsConfig.isEnabled();
        }
        if (path == null) {
            path = endpointsConfig.getPath();
            if (path == null) {
                EndpointProperties allEndpointsProps = endpointsConfig.getEndpoints().get(ALL_ENDPOINTS_NAME);

                var allEndpointsContextPath = allEndpointsProps != null && StringUtils.isNotEmpty(allEndpointsProps.getContextPath()) ? allEndpointsProps.getContextPath() : null;
                if (allEndpointsContextPath != null) {
                    path = allEndpointsContextPath;
                }
                if (path != null && path.startsWith(SLASH) && path.length() > 1) {
                    path = SLASH + path;
                }

                var allEndpointsPath = allEndpointsProps != null && StringUtils.isNotEmpty(allEndpointsProps.getPath()) ? allEndpointsProps.getPath() : null;
                if (allEndpointsPath != null) {
                    path = path != null ? StringUtils.prependUri(path, allEndpointsPath) : allEndpointsPath;
                }
                if (path != null && path.endsWith(SLASH) && path.length() > 1) {
                    path = path.substring(0, path.length() - 1);
                }
                if (path == null) {
                    path = SLASH;
                }
            }
        }
        if (ignore(element, context)) {
            return;
        }
        if (servers == null) {
            servers = endpointsConfig.getServers();
            if (servers == null) {
                servers = Collections.emptyList();
            }
        }
        if (tags == null) {
            tags = endpointsConfig.getTags();
            if (tags == null) {
                tags = Collections.emptyList();
            }
        }
        if (extensions == null) {
            extensions = endpointsConfig.getExtensions();
            if (extensions == null) {
                extensions = Collections.emptyMap();
            }
        }
        if (securityRequirements == null) {
            securityRequirements = endpointsConfig.getSecurityRequirements();
            if (securityRequirements == null) {
                securityRequirements = Collections.emptyList();
            }
        }
        if (additionalTags != null) {
            tags = new ArrayList<>(tags);
            tags.addAll(readTags(additionalTags, context));
        }
        if (additionalExtensions != null) {
            if (extensions == null) {
                extensions = readExtensions(additionalExtensions);
            } else {
                extensions.putAll(readExtensions(additionalExtensions));
            }
            if (extensions == null) {
                extensions = Collections.emptyMap();
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
        if (enabled != null && !enabled) {
            return true;
        }
        if (isIgnored(element, context)) {
            return true;
        }
        AnnotationValue<?> ann = null;
        if (element.isAnnotationPresent("io.micronaut.management.endpoint.annotation.Endpoint")) {
            ann = element.getAnnotation("io.micronaut.management.endpoint.annotation.Endpoint");
            // currently, micronaut-spring can't map spring WebEndpoint annotation to micronaut Endpoint annotation
        } else if (element.isAnnotationPresent("org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint")) {
            ann = element.getAnnotation("org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint");
        }
        if (ann == null) {
            return true;
        }
        var idAnn = ann.stringValue(PROP_ID).orElse(ann.stringValue(PROP_VALUE).orElse(null));
        if (StringUtils.isEmpty(idAnn)) {
            idAnn = NameUtils.hyphenate(element.getSimpleName());
        }
        var endpointProps = ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_PROPS, EndpointProperties.class, context);
        var endpointPath = endpointProps != null && StringUtils.isNotEmpty(endpointProps.getPath()) ? endpointProps.getPath() : idAnn;
        if (path.endsWith(SLASH) && endpointPath.startsWith(SLASH)) {
            endpointPath = endpointPath.substring(1);
        } else if (!path.endsWith(SLASH) && !endpointPath.startsWith(SLASH)) {
            endpointPath = SLASH + endpointPath;
        }
        id = path + endpointPath;
        if (!id.startsWith(SLASH)) {
            id = SLASH + id;
        }
        return false;
    }

    @Override
    protected boolean ignore(MethodElement element, VisitorContext context) {
        if (enabled != null && !enabled) {
            return true;
        }
        if (isIgnored(element.getOwningType(), context)) {
            return true;
        }

        var operationAnn = element.getAnnotation(Operation.class);
        boolean isHidden = operationAnn != null && operationAnn.booleanValue(PROP_HIDDEN).orElse(false);
        var jsonAnySetterAnn = element.getAnnotation(JsonAnySetter.class);

        if (isHidden
            || element.isAnnotationPresent(Hidden.class)
            || (jsonAnySetterAnn != null && jsonAnySetterAnn.booleanValue(PROP_ENABLED).orElse(true))) {
            return true;
        }
        methodDescription = httpMethodDescription(element, context);
        return methodDescription == null;
    }

    @Override
    protected HttpMethod httpMethod(MethodElement element) {
        return methodDescription != null ? methodDescription.httpMethod : null;
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
    protected String getClassDescription(VisitorContext context) {
        return null;
    }

    @Override
    protected String getMethodDescription(VisitorContext context) {
        var description = ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_DESCRIPTION, String.class, null, context);
        if (StringUtils.isNotEmpty(description)) {
            return description;
        }
        if (StringUtils.isNotEmpty(this.description)) {
            return this.description;
        }
        return methodDescription.description;
    }

    @Override
    protected List<Tag> getUserDefinedClassTags(ClassElement element, VisitorContext context) {
        var allTags = new ArrayList<>(tags);
        allTags.addAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS, TAGS_LIST_ARGUMENT, Collections.emptyList(), context));
        return allTags;
    }

    @Override
    protected List<Server> methodServers(MethodElement element, VisitorContext context) {
        var servers = new ArrayList<>(this.servers);
        servers.addAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS, SERVERS_LIST_ARGUMENT, Collections.emptyList(), context));
        return servers;
    }

    @Override
    protected Map<String, Object> operationExtensions(MethodElement element, VisitorContext context) {
        var extensions = this.extensions != null ? new HashMap<>(this.extensions) : new HashMap<String, Object>();
        extensions.putAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_EXTENSIONS, EXTENSIONS_MAP_ARGUMENT, Collections.emptyMap(), context));
        return extensions;
    }

    @Override
    protected List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context) {
        var securityRequirements = new ArrayList<>(this.securityRequirements);
        //noinspection unchecked
        securityRequirements.addAll(ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, List.class, Collections.emptyList(), context));
        return securityRequirements;
    }

    @Override
    public int getOrder() {
        return 40;
    }

    private static List<MediaType> mediaTypes(List<String> mediaTypes) {
        if (CollectionUtils.isEmpty(mediaTypes)) {
            return DEFAULT_MEDIA_TYPES;
        }
        return mediaTypes.stream()
            .map(Utils::getMediaType)
            .toList();
    }

    private static HttpMethodDescription httpMethodDescription(MethodElement element, VisitorContext context) {
        HttpMethodDescription httpMethodDescription = methodDescription(element, "io.micronaut.management.endpoint.annotation.Write", HttpMethod.POST, context);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        httpMethodDescription = methodDescription(element, "io.micronaut.management.endpoint.annotation.Read", HttpMethod.GET, context);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        httpMethodDescription = methodDescription(element, "io.micronaut.management.endpoint.annotation.Delete", HttpMethod.DELETE, context);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        // check spring-actuator annotations, if micronaut-spring didn't map them
        httpMethodDescription = methodDescription(element, "org.springframework.boot.actuate.endpoint.annotation.WriteOperation", HttpMethod.POST, context);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        httpMethodDescription = methodDescription(element, "org.springframework.boot.actuate.endpoint.annotation.ReadOperation", HttpMethod.GET, context);
        if (httpMethodDescription != null) {
            return httpMethodDescription;
        }
        return methodDescription(element, "org.springframework.boot.actuate.endpoint.annotation.DeleteOperation", HttpMethod.DELETE, context);
    }

    private static HttpMethodDescription methodDescription(MethodElement element, String endpointManagementAnnName, HttpMethod httpMethod, VisitorContext context) {
        if (element.isAnnotationPresent(endpointManagementAnnName)) {
            AnnotationValue<?> annotation = element.getAnnotation(endpointManagementAnnName);
            assert annotation != null;

            List<String> produces = Arrays.asList(annotation.stringValues("produces"));
            List<String> consumes = Arrays.asList(annotation.stringValues("consumes"));
            if (CollectionUtils.isEmpty(produces)) {
                produces = SpringActuatorUtils.getProducesFrom(context);
                if (CollectionUtils.isNotEmpty(produces)) {
                    consumes = produces;
                }
            }

            return new HttpMethodDescription(httpMethod, annotation.stringValue(PROP_DESCRIPTION).orElse(null), produces, consumes);
        }
        return null;
    }

    /**
     * Endpoint method description.
     *
     * @param httpMethod http method
     * @param description description
     * @param produces produces
     * @param consumes consumes
     *
     * @author croudet
     */
    @Internal
    private record HttpMethodDescription(
        HttpMethod httpMethod,
        String description,
        List<String> produces,
        List<String> consumes
    ) {
    }
}
