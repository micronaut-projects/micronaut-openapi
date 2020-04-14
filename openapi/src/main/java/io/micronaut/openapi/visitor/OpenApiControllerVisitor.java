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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.annotation.processing.AnnotationUtils;
import io.micronaut.annotation.processing.GenericUtils;
import io.micronaut.annotation.processing.visitor.JavaClassElementExt;
import io.micronaut.context.env.DefaultPropertyPlaceholderResolver;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.DefaultConversionService;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.value.OptionalValues;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.CookieValue;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.HttpMethodMapping;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.http.uri.UriMatchVariable;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.javadoc.JavadocParser;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.callbacks.Callback;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
@Experimental
public class OpenApiControllerVisitor extends AbstractOpenApiVisitor implements TypeElementVisitor<Controller, HttpMethodMapping> {
    private static final String CLASS_TAGS = "MICRONAUT_OPENAPI_CLASS_TAGS";

    private PropertyPlaceholderResolver propertyPlaceholderResolver;

    private static boolean isGeneric(Map<String, Map<String, TypeMirror>> typeArguments, TypeMirror tm) {
        for (Map<String, TypeMirror> m: typeArguments.values()) {
            for (String typeMirror: m.keySet()) {
                if (tm.toString().equals(typeMirror)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!element.hasAnnotation(Controller.class)) {
            return;
        }
        processSecuritySchemes(element, context);
        context.put(CLASS_TAGS, readTags(element, context));
        if (!"io.micronaut.annotation.processing.visitor.JavaClassElement".equals(element.getClass().getName()) ||
            !"io.micronaut.annotation.processing.visitor.JavaVisitorContext".equals(context.getClass().getName())) {
            return;
        }
        // Issue #193 - Inherited methods are not processed
        GenericUtils generics = JavaClassElementExt.getGenericUtils(context);
        TypeElement t = (TypeElement) element.getNativeType();
        Map<String, Map<String, TypeMirror>> typeArguments = generics.buildGenericTypeArgumentElementInfo(t);
        AnnotationUtils annotationUtils = JavaClassElementExt.getAnnotationUtils(context);
        List<MethodElement> methodElements = new JavaClassElementExt(element, context).getMethods();
        for (MethodElement methodElement: methodElements) {
            ExecutableElement method = (ExecutableElement) methodElement.getNativeType();
            Optional<AnnotationValue<HttpMethodMapping>> mapping = annotationUtils.getAnnotationMetadata(method)
                    .findAnnotation(HttpMethodMapping.class);
            if (!mapping.isPresent()) {
                continue;
            }
            boolean hasGenerics = isGeneric(typeArguments, method.getReturnType())
                    || method.getParameters().stream().anyMatch(v -> isGeneric(typeArguments, v.asType()));
            visitMethod(methodElement, context, hasGenerics);
        }
    }

    private boolean hasNoBindingAnnotationOrType(ParameterElement parameter, ClassElement parameterType) {
        return !parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class) &&
               !parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.parameters.RequestBody.class) &&
               !parameter.isAnnotationPresent(Hidden.class) &&

               !parameter.isAnnotationPresent(QueryValue.class) &&
               !parameter.isAnnotationPresent(PathVariable.class) &&
               !parameter.isAnnotationPresent(Body.class) &&
               !parameter.isAnnotationPresent(Part.class) &&
               !parameter.isAnnotationPresent(CookieValue.class) &&
               !parameter.isAnnotationPresent(Header.class) &&

               !isIgnoredParameterType(parameterType) &&
               !isResponseType(parameterType) &&
               !parameterType.isAssignable(HttpRequest.class) &&
               !parameterType.isAssignable("io.micronaut.http.BasicAuth");
    }

    private HttpMethod httpMethod(MethodElement element) {
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
    public void visitMethod(MethodElement element, VisitorContext context) {
        // Issue #193 - Inherited methods are not processed
    }

    private void visitMethod(MethodElement element, VisitorContext context, boolean useGenericType) {
        if (element.isAnnotationPresent(Hidden.class)) {
            return;
        }

        HttpMethod httpMethod = httpMethod(element);
        if (httpMethod == null) {
            return;
        }
        String controllerValue;
        Optional<String> cv = element.getDeclaringType().getValue(Controller.class, String.class);
        if (cv.isPresent()) {
            controllerValue = cv.get();
        } else {
            cv = element.getOwningType().getValue(Controller.class, String.class);
            if (cv.isPresent()) {
                controllerValue = cv.get();
            } else {
                return;
            }
        }
        controllerValue = getPropertyPlaceholderResolver().resolvePlaceholders(controllerValue).orElse(controllerValue);
        UriMatchTemplate matchTemplate = UriMatchTemplate.of(controllerValue);
        String methodValue = element.getValue(HttpMethodMapping.class, String.class).orElse("/");
        methodValue = getPropertyPlaceholderResolver().resolvePlaceholders(methodValue).orElse(methodValue);
        matchTemplate = matchTemplate.nest(methodValue);

        PathItem pathItem = resolvePathItem(context, matchTemplate);
        OpenAPI openAPI = resolveOpenAPI(context);

        final Optional<AnnotationValue<Operation>> operationAnnotation = element.findAnnotation(Operation.class);
        io.swagger.v3.oas.models.Operation swaggerOperation = operationAnnotation
                .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.Operation.class))
                .orElse(new io.swagger.v3.oas.models.Operation());

        if (StringUtils.isEmpty(swaggerOperation.getOperationId())) {
            swaggerOperation.setOperationId(element.getName());
        }
        readTags(element, swaggerOperation, context.get(CLASS_TAGS, List.class, Collections.emptyList()));
        for (SecurityRequirement securityItem : readSecurityRequirements(element)) {
            swaggerOperation.addSecurityItem(securityItem);
        }

        readApiResponses(element, context, swaggerOperation);

        readServers(element, context, swaggerOperation);

        readCallbacks(element, context, swaggerOperation);

        JavadocDescription javadocDescription = element.getDocumentation().map(s -> new JavadocParser().parse(s))
                .orElse(null);

        if (javadocDescription != null && StringUtils.isEmpty(swaggerOperation.getDescription())) {
            swaggerOperation.setDescription(javadocDescription.getMethodDescription());
            swaggerOperation.setSummary(javadocDescription.getMethodDescription());
        }

        setOperationOnPathItem(pathItem, swaggerOperation, httpMethod);

        if (element.isAnnotationPresent(Deprecated.class)) {
            swaggerOperation.setDeprecated(true);
        }

        List<Parameter> swaggerParameters = swaggerOperation.getParameters();
        List<UriMatchVariable> pv = matchTemplate.getVariables();
        Map<String, UriMatchVariable> pathVariables = new LinkedHashMap<>(pv.size());
        for (UriMatchVariable variable : pv) {
            pathVariables.put(variable.getName(), variable);
        }
        OptionalValues<List> consumesMediaTypes = element.getValues(Consumes.class, List.class);
        String consumesMediaType = element.stringValue(Consumes.class).orElse(MediaType.APPLICATION_JSON);
        ApiResponses responses = swaggerOperation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();

            swaggerOperation.setResponses(responses);

            ApiResponse okResponse = new ApiResponse();

            if (javadocDescription == null) {
                okResponse.setDescription(swaggerOperation.getOperationId() + " default response");
            } else {
                okResponse.setDescription(javadocDescription.getReturnDescription());
            }

            ClassElement returnType = useGenericType ? element.getGenericReturnType() : element.getReturnType();
            if (returnType.isAssignable("io.reactivex.Completable")) {
                returnType = null;
            } else if (isResponseType(returnType)) {
                returnType = returnType.getFirstTypeArgument().orElse(returnType);
            } else if (isSingleResponseType(returnType)) {
                returnType = returnType.getFirstTypeArgument().get();
                returnType = returnType.getFirstTypeArgument().orElse(returnType);
            }

            if (returnType != null) {
                OptionalValues<List> mediaTypes = element.getValues(Produces.class, List.class);
                Content content;
                if (mediaTypes.isEmpty()) {
                    content = buildContent(element, returnType, MediaType.APPLICATION_JSON, openAPI, context);
                } else {
                    content = buildContent(element, returnType, mediaTypes, openAPI, context);
                }
                okResponse.setContent(content);
            }
            responses.put(ApiResponses.DEFAULT, okResponse);
        }

        boolean permitsRequestBody = HttpMethod.permitsRequestBody(httpMethod);

        if (permitsRequestBody) {
            readSwaggerRequestBody(element, context, swaggerOperation);
        }

        boolean hasExistingParameters = CollectionUtils.isNotEmpty(swaggerParameters);
        if (!hasExistingParameters) {
            swaggerParameters = new ArrayList<>();
            swaggerOperation.setParameters(swaggerParameters);
        }

        for (ParameterElement parameter : element.getParameters()) {

            ClassElement parameterType = useGenericType ? parameter.getGenericType() : parameter.getType();
            String parameterName = parameter.getName();

            if (isIgnoredParameterType(parameterType)) {
                continue;
            }

            if (permitsRequestBody && swaggerOperation.getRequestBody() == null) {
                readSwaggerRequestBody(parameter, context, swaggerOperation);
            }

            if (parameter.isAnnotationPresent(Body.class)) {

                if (permitsRequestBody) {
                    RequestBody requestBody = swaggerOperation.getRequestBody();
                    if (requestBody == null) {
                        requestBody = new RequestBody();
                        swaggerOperation.setRequestBody(requestBody);
                    }
                    if (requestBody.getDescription() == null && javadocDescription != null) {
                        CharSequence desc = javadocDescription.getParameters().get(parameterName);
                        if (desc != null) {
                            requestBody.setDescription(desc.toString());
                        }
                    }
                    if (requestBody.getRequired() == null) {
                        requestBody.setRequired(!parameter.isAnnotationPresent(Nullable.class)
                                && !parameterType.isAssignable(Optional.class));
                    }
                    if (requestBody.getContent() == null) {
                        Content content;
                        if (consumesMediaTypes.isEmpty()) {
                            content = buildContent(parameter, parameterType, MediaType.APPLICATION_JSON, openAPI,
                                    context);
                        } else {
                            content = buildContent(parameter, parameterType, consumesMediaTypes, openAPI, context);
                        }
                        requestBody.setContent(content);
                    }
                }
                continue;
            }

            if (hasExistingParameters) {
                continue;
            }

            Parameter newParameter = null;

            if (!parameter.hasStereotype(Bindable.class) && pathVariables.containsKey(parameterName)) {
                UriMatchVariable var = pathVariables.get(parameterName);
                newParameter = var.isQuery() ? new QueryParameter() : new PathParameter();
                newParameter.setName(parameterName);
                final boolean exploded = var.isExploded();
                if (exploded) {
                    newParameter.setExplode(exploded);
                }
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                String paramName = parameter.getValue(PathVariable.class, String.class).orElse(parameterName);
                UriMatchVariable var = pathVariables.get(paramName);
                if (var == null) {
                    context.fail("Path variable name: '" + paramName + "' not found in path.", parameter);
                    continue;
                }
                newParameter = new PathParameter();
                newParameter.setName(paramName);
                final boolean exploded = var.isExploded();
                if (exploded) {
                    newParameter.setExplode(exploded);
                }
            } else if (parameter.isAnnotationPresent(Header.class)) {
                String headerName = parameter.getValue(Header.class, "name", String.class).orElse(parameter
                        .getValue(Header.class, String.class).orElseGet(() -> NameUtils.hyphenate(parameterName)));
                newParameter = new HeaderParameter();
                newParameter.setName(headerName);
            } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                String cookieName = parameter.getValue(CookieValue.class, String.class).orElse(parameterName);
                newParameter = new CookieParameter();
                newParameter.setName(cookieName);
            } else if (parameter.isAnnotationPresent(QueryValue.class)) {
                String queryVar = parameter.getValue(QueryValue.class, String.class).orElse(parameterName);
                newParameter = new QueryParameter();
                newParameter.setName(queryVar);
            } else if (!permitsRequestBody && hasNoBindingAnnotationOrType(parameter, parameterType)) {
                // default to QueryValue -
                // https://github.com/micronaut-projects/micronaut-openapi/issues/130
                newParameter = new QueryParameter();
                newParameter.setName(parameterName);
            }

            if (parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class)) {
                AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn = parameter
                        .findAnnotation(io.swagger.v3.oas.annotations.Parameter.class).orElse(null);

                if (paramAnn != null) {

                    if (paramAnn.get("hidden", Boolean.class, false)) {
                        // ignore hidden parameters
                        continue;
                    }

                    Map<CharSequence, Object> paramValues = toValueMap(paramAnn.getValues(), context);
                    normalizeEnumValues(paramValues, Collections.singletonMap("in", ParameterIn.class));
                    if (parameter.isAnnotationPresent(Header.class)) {
                        paramValues.put("in", ParameterIn.HEADER.toString());
                    } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                        paramValues.put("in", ParameterIn.COOKIE.toString());
                    } else if (parameter.isAnnotationPresent(QueryValue.class)) {
                        paramValues.put("in", ParameterIn.QUERY.toString());
                    }
                    processExplode(paramAnn, paramValues);

                    JsonNode jsonNode = jsonMapper.valueToTree(paramValues);

                    if (newParameter == null) {
                        try {
                            newParameter = treeToValue(jsonNode, Parameter.class);
                        } catch (Exception e) {
                            context.warn("Error reading Swagger Parameter for element [" + parameter + "]: "
                                    + e.getMessage(), parameter);
                        }
                    } else {
                        try {
                            Parameter v = treeToValue(jsonNode, Parameter.class);
                            if (v != null) {
                                // horrible hack because Swagger
                                // ParameterDeserializer breaks updating
                                // existing objects
                                BeanMap<Parameter> beanMap = BeanMap.of(v);
                                BeanMap<Parameter> target = BeanMap.of(newParameter);
                                for (CharSequence name : paramValues.keySet()) {
                                    Object o = beanMap.get(name.toString());
                                    target.put(name.toString(), o);
                                }
                            } else {
                                BeanMap<Parameter> target = BeanMap.of(newParameter);
                                for (CharSequence name : paramValues.keySet()) {
                                    Object o = paramValues.get(name.toString());
                                    try {
                                        target.put(name.toString(), o);
                                    } catch (Exception e) {
                                        // ignore
                                    }
                                }
                            }
                        } catch (IOException e) {
                            context.warn("Error reading Swagger Parameter for element [" + parameter + "]: "
                                    + e.getMessage(), parameter);
                        }
                    }

                    if (newParameter != null) {
                        final Schema parameterSchema = newParameter.getSchema();
                        if (paramAnn.contains("schema") && parameterSchema != null) {
                            final AnnotationValue schemaAnn = paramAnn.get("schema", AnnotationValue.class)
                                    .orElse(null);
                            if (schemaAnn != null) {
                                bindSchemaAnnotationValue(context, parameter, parameterSchema, schemaAnn);
                            }
                        }
                    }
                }
            }

            if (newParameter != null) {

                if (StringUtils.isEmpty(newParameter.getName())) {
                    newParameter.setName(parameterName);
                }

                if (newParameter.getRequired() == null) {
                    newParameter.setRequired(!parameter.isAnnotationPresent(Nullable.class));
                }
                // calc newParameter.setExplode();
                if (javadocDescription != null && StringUtils.isEmpty(newParameter.getDescription())) {

                    CharSequence desc = javadocDescription.getParameters().get(parameterName);
                    if (desc != null) {
                        newParameter.setDescription(desc.toString());
                    }
                }
                swaggerParameters.add(newParameter);

                Schema schema = newParameter.getSchema();
                if (schema == null) {
                    schema = resolveSchema(openAPI, parameter, parameterType, context, consumesMediaType);
                }

                if (schema != null) {
                    bindSchemaForElement(context, parameter, parameterType, schema);
                    newParameter.setSchema(schema);
                }
            }
        }

        if (HttpMethod.requiresRequestBody(httpMethod) && swaggerOperation.getRequestBody() == null) {
            List<ParameterElement> bodyParameters = Arrays.stream(element.getParameters())
                    .filter(p -> !pathVariables.containsKey(p.getName()) && !p.isAnnotationPresent(Bindable.class)
                            && !p.isAnnotationPresent(JsonIgnore.class) && !p.isAnnotationPresent(Hidden.class)
                            && !p.isAnnotationPresent(Header.class) && !p.isAnnotationPresent(QueryValue.class)
                            && !p.getValue(io.swagger.v3.oas.annotations.Parameter.class, "in", ParameterIn.class)
                                    .isPresent()
                            && !p.getValue(io.swagger.v3.oas.annotations.Parameter.class, "hidden", Boolean.class)
                                    .orElse(false)
                            && !isIgnoredParameterType(p.getType()))
                    .collect(Collectors.toList());

            if (!bodyParameters.isEmpty()) {
                RequestBody requestBody = new RequestBody();
                final Content content = new Content();
                consumesMediaTypes = consumesMediaTypes.isEmpty()
                        ? OptionalValues.of(List.class, Collections.singletonMap("value", MediaType.APPLICATION_JSON))
                        : consumesMediaTypes;
                consumesMediaTypes.forEach((key, mediaTypeList) -> {
                    for (Object mediaType : mediaTypeList) {
                        io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
                        ObjectSchema schema = new ObjectSchema();
                        for (ParameterElement parameter : bodyParameters) {
                            Schema propertySchema = resolveSchema(openAPI, parameter, parameter.getType(), context,
                                    mediaType.toString());
                            if (propertySchema != null) {

                                Optional<String> description = parameter.getValue(
                                        io.swagger.v3.oas.annotations.Parameter.class, "description", String.class);
                                if (description.isPresent()) {
                                    propertySchema.setDescription(description.get());
                                }
                                processSchemaProperty(context, parameter, parameter.getType(), schema, propertySchema);

                                propertySchema.setNullable(parameter.isAnnotationPresent(Nullable.class));
                                if (javadocDescription != null
                                        && StringUtils.isEmpty(propertySchema.getDescription())) {
                                    CharSequence doc = javadocDescription.getParameters().get(parameter.getName());
                                    if (doc != null) {
                                        propertySchema.setDescription(doc.toString());
                                    }
                                }
                            }

                        }
                        mt.setSchema(schema);
                        content.addMediaType(mediaType.toString(), mt);
                    }
                });

                requestBody.setContent(content);
                requestBody.setRequired(true);
                swaggerOperation.setRequestBody(requestBody);
            }
        }
    }

    private void processExplode(AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn, Map<CharSequence, Object> paramValues) {
        Optional<Explode> explode = paramAnn.enumValue("explode", Explode.class);
        if (explode.isPresent()) {
            Explode ex = explode.get();
            switch (ex) {
                case TRUE:
                    paramValues.put("explode", Boolean.TRUE);
                    break;
                case FALSE:
                    paramValues.put("explode", Boolean.FALSE);
                    break;
                case DEFAULT:
                default:
                    String in = (String) paramValues.get("in");
                    if (in == null || in.isEmpty()) {
                        in = "DEFAULT";
                    }
                    switch (ParameterIn.valueOf(in.toUpperCase(Locale.US))) {
                    case COOKIE:
                    case QUERY:
                        paramValues.put("explode", Boolean.TRUE);
                        break;
                    case DEFAULT:
                    case HEADER:
                    case PATH:
                    default:
                        paramValues.put("explode", Boolean.FALSE);
                        break;
                    }
                    break;
            }
        }
    }

    private boolean isIgnoredParameterType(ClassElement parameterType) {
        return parameterType == null ||
                parameterType.isAssignable(Principal.class) ||
            parameterType.isAssignable("io.micronaut.security.authentication.Authentication");
    }

    private boolean isResponseType(ClassElement returnType) {
        return returnType.isAssignable(HttpResponse.class) || returnType.isAssignable("org.springframework.http.HttpEntity");
    }

    private boolean isSingleResponseType(ClassElement returnType) {
        return returnType.isAssignable("io.reactivex.Single")
          && returnType.getFirstTypeArgument().isPresent()
          && isResponseType(returnType.getFirstTypeArgument().get());
    }

    private void setOperationOnPathItem(PathItem pathItem, io.swagger.v3.oas.models.Operation swaggerOperation, HttpMethod httpMethod) {
        switch (httpMethod) {
            case GET:
                pathItem.get(swaggerOperation);
            break;
            case POST:
                pathItem.post(swaggerOperation);
            break;
            case PUT:
                pathItem.put(swaggerOperation);
            break;
            case PATCH:
                pathItem.patch(swaggerOperation);
            break;
            case DELETE:
                pathItem.delete(swaggerOperation);
            break;
            case HEAD:
                pathItem.head(swaggerOperation);
            break;
            case OPTIONS:
                pathItem.options(swaggerOperation);
            break;
            case TRACE:
                pathItem.trace(swaggerOperation);
            break;
            default:
                // unprocessable
        }
    }

    private void readApiResponses(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> responseAnnotations = element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        if (CollectionUtils.isNotEmpty(responseAnnotations)) {
            ApiResponses apiResponses = new ApiResponses();
            for (AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse> r : responseAnnotations) {
                Optional<ApiResponse> newResponse = toValue(r.getValues(), context, ApiResponse.class);
                newResponse.ifPresent(apiResponse ->
                        apiResponses.put(r.get("responseCode", String.class).orElse("default"), apiResponse));
            }
            swaggerOperation.setResponses(apiResponses);
        }
    }

    private void readSwaggerRequestBody(Element element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        element.findAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class)
                .flatMap(annotation -> toValue(annotation.getValues(), context, RequestBody.class))
                .ifPresent(swaggerOperation::setRequestBody);
    }

    private void readServers(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        List<io.swagger.v3.oas.models.servers.Server> servers = processOpenApiAnnotation(
                element,
                context,
                io.swagger.v3.oas.annotations.servers.Server.class,
                io.swagger.v3.oas.models.servers.Server.class,
                Collections.emptyList()
        );
        for (Server server: servers) {
            swaggerOperation.addServersItem(server);
        }
    }

    private void readCallbacks(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        List<AnnotationValue<Callback>> callbackAnnotations = element.getAnnotationValuesByType(Callback.class);
        if (CollectionUtils.isNotEmpty(callbackAnnotations)) {
            for (AnnotationValue<Callback> callbackAnn : callbackAnnotations) {
                final Optional<String> n = callbackAnn.get("name", String.class);
                n.ifPresent(callbackName -> {

                    final Optional<String> expr = callbackAnn.get("callbackUrlExpression", String.class);
                    if (expr.isPresent()) {

                        final String callbackUrl = expr.get();

                        final List<AnnotationValue<Operation>> operations = callbackAnn.getAnnotations("operation", Operation.class);
                        if (CollectionUtils.isEmpty(operations)) {
                            Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(swaggerOperation);
                            final io.swagger.v3.oas.models.callbacks.Callback c = new io.swagger.v3.oas.models.callbacks.Callback();
                            c.addPathItem(callbackUrl, new PathItem());
                            callbacks.put(callbackName, c);
                        } else {
                            final PathItem pathItem = new PathItem();
                            for (AnnotationValue<Operation> operation : operations) {
                                final Optional<HttpMethod> operationMethod = operation.get("method", HttpMethod.class);
                                operationMethod.ifPresent(httpMethod ->
                                    toValue(operation.getValues(), context, io.swagger.v3.oas.models.Operation.class).ifPresent(op -> setOperationOnPathItem(pathItem, op, httpMethod)));
                            }
                            Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(swaggerOperation);
                            final io.swagger.v3.oas.models.callbacks.Callback c = new io.swagger.v3.oas.models.callbacks.Callback();
                            c.addPathItem(callbackUrl, pathItem);
                            callbacks.put(callbackName, c);

                        }

                    } else {
                        final Components components = resolveComponents(resolveOpenAPI(context));
                        final Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbackComponents = components.getCallbacks();
                        if (callbackComponents != null && callbackComponents.containsKey(callbackName)) {
                            Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(swaggerOperation);
                            final io.swagger.v3.oas.models.callbacks.Callback callbackRef = new io.swagger.v3.oas.models.callbacks.Callback();
                            callbackRef.set$ref("#/components/callbacks/" + callbackName);
                            callbacks.put(callbackName, callbackRef);
                        }
                    }
                });

            }
        }
    }

    private Map<String, io.swagger.v3.oas.models.callbacks.Callback> initCallbacks(io.swagger.v3.oas.models.Operation swaggerOperation) {
        Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = swaggerOperation.getCallbacks();
        if (callbacks == null) {
            callbacks = new LinkedHashMap<>();
            swaggerOperation.setCallbacks(callbacks);
        }
        return callbacks;
    }

    private void readTags(MethodElement element, io.swagger.v3.oas.models.Operation swaggerOperation, List<io.swagger.v3.oas.models.tags.Tag> classTags) {
        List<AnnotationValue<Tag>> tagAnnotations = element.getAnnotationValuesByType(Tag.class);
        if (CollectionUtils.isNotEmpty(tagAnnotations)) {
            for (AnnotationValue<Tag> r : tagAnnotations) {
                r.get("name", String.class).ifPresent(swaggerOperation::addTagsItem);
            }
        }
        if (!classTags.isEmpty()) {
            List<String> operationTags = swaggerOperation.getTags();
            if (operationTags == null) {
                operationTags = new ArrayList<>(classTags.size());
                swaggerOperation.setTags(operationTags);
            }
            for (io.swagger.v3.oas.models.tags.Tag tag : classTags) {
                if (!operationTags.contains(tag.getName())) {
                    operationTags.add(tag.getName());
                }
            }
        }
    }

    private List<io.swagger.v3.oas.models.tags.Tag> readTags(ClassElement element, VisitorContext context) {
        List<io.swagger.v3.oas.models.tags.Tag> tagList = new ArrayList<>();
        List<AnnotationValue<Tag>> tagAnnotations = element.getAnnotationValuesByType(Tag.class);
        if (CollectionUtils.isNotEmpty(tagAnnotations)) {
            for (AnnotationValue<Tag> tag : tagAnnotations) {
                toValue(tag.getValues(), context, io.swagger.v3.oas.models.tags.Tag.class).ifPresent(tagList::add);
            }
        }
        return tagList;
    }

    private Content buildContent(Element definingElement, ClassElement type, String mediaType, OpenAPI openAPI, VisitorContext context) {
        Content content = new Content();
        io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
        mt.setSchema(resolveSchema(openAPI, definingElement, type, context, mediaType));
        content.addMediaType(mediaType, mt);
        return content;
    }

    private Content buildContent(Element definingElement, ClassElement type, OptionalValues<List> mediaTypes, OpenAPI openAPI, VisitorContext context) {
        Content content = new Content();
        mediaTypes.forEach((key, mediaTypesList) ->  {
            for (Object mediaType: mediaTypesList) {
                io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
                mt.setSchema(resolveSchema(openAPI, definingElement, type, context, mediaType.toString()));
                content.addMediaType(mediaType.toString(), mt);
            }

        });
        return content;
    }

    /**
     *
     * @return An Instance of {@link PropertyPlaceholderResolver} to resolve placeholders.
     */
    PropertyPlaceholderResolver getPropertyPlaceholderResolver() {
        if (this.propertyPlaceholderResolver == null) {
            this.propertyPlaceholderResolver = new DefaultPropertyPlaceholderResolver(new PropertyResolver() {
                @Override
                public boolean containsProperty(@Nonnull String name) {
                    return false;
                }

                @Override
                public boolean containsProperties(@Nonnull String name) {
                    return false;
                }

                @Nonnull
                @Override
                public <T> Optional<T> getProperty(@Nonnull String name, @Nonnull ArgumentConversionContext<T> conversionContext) {
                    return Optional.empty();
                }
            }, new DefaultConversionService());
        }
        return this.propertyPlaceholderResolver;
    }
}
