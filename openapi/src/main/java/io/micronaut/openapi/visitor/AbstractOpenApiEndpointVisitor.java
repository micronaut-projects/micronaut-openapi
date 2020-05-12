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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

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
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.CookieValue;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.PathVariable;
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
import io.swagger.v3.oas.annotations.tags.Tags;
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

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 * @param <C> The annotation required on the class. Use {@link Object} for all classes.
 * @param <E> The annotation required on the element. Use {@link Object} for all elements.
 * @author graemerocher
 * @since 1.0
 */
@Experimental
public abstract class AbstractOpenApiEndpointVisitor<C, E> extends AbstractOpenApiVisitor implements TypeElementVisitor<C, E> {
    protected List<io.swagger.v3.oas.models.tags.Tag> classTags;
    protected PropertyPlaceholderResolver propertyPlaceholderResolver;

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (ignore(element, context)) {
            return;
        }
        processSecuritySchemes(element, context);
        processTags(element, context);
    }

    private void processTags(ClassElement element, VisitorContext context) {
        classTags = readTags(element, context);
        List<io.swagger.v3.oas.models.tags.Tag> userDefinedClassTags = classTags(element, context);
        if (classTags == null || classTags.isEmpty()) {
            classTags = userDefinedClassTags == null ? Collections.emptyList() : userDefinedClassTags;
        } else if (userDefinedClassTags != null) {
            for (io.swagger.v3.oas.models.tags.Tag tag : userDefinedClassTags) {
                if (!containsTag(tag.getName(), classTags)) {
                    classTags.add(tag);
                }
            }
        }
    }

    private boolean containsTag(String name, List<io.swagger.v3.oas.models.tags.Tag> tags) {
        return tags.stream().anyMatch(tag -> name.equals(tag.getName()));
    }

    /**
     * Returns the security requirements at method level.
     * @param element The MethodElement.
     * @param context The context.
     * @return The security requirements.
     */
    protected abstract List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context);

    /**
     * Returns the servers at method level.
     * @param element The MethodElement.
     * @param context The context.
     * @return The servers.
     */
    protected abstract List<io.swagger.v3.oas.models.servers.Server> methodServers(MethodElement element, VisitorContext context);

    /**
     * Returns the class tags.
     * @param element The ClassElement.
     * @param context The context.
     * @return The class tags.
     */
    protected abstract List<io.swagger.v3.oas.models.tags.Tag> classTags(ClassElement element, VisitorContext context);

    /**
     * Returns true if the specified element should not be processed.
     * @param element The ClassElement.
     * @param context The context.
     * @return true if the specified element should not be processed.
     */
    protected abstract boolean ignore(ClassElement element, VisitorContext context);

    /**
     * Returns true if the specified element should not be processed.
     * @param element The ClassElement.
     * @param context The context.
     * @return true if the specified element should not be processed.
     */
    protected abstract boolean ignore(MethodElement element, VisitorContext context);

    /**
     * Returns the HttpMethod of the element.
     * @param element The MethodElement.
     * @return The HttpMethod of the element.
     */
    protected abstract HttpMethod httpMethod(MethodElement element);

    /**
     * Returns the uri paths of the element.
     * @param element The MethodElement.
     * @return The uri paths of the element.
     */
    protected abstract List<UriMatchTemplate> uriMatchTemplates(MethodElement element);

    /**
     * Returns the consumes media types.
     * @param element The MethodElement.
     * @return The consumes media types.
     */
    protected abstract List<MediaType> consumesMediaTypes(MethodElement element);

    /**
     * Returns the produces media types.
     * @param element The MethodElement.
     * @return The produces media types.
     */
    protected abstract List<MediaType> producesMediaTypes(MethodElement element);

    /**
     * Returns the description for the element.
     * @param element The MethodElement.
     * @return The description for the element.
     */
    protected abstract String description(MethodElement element);

    private boolean hasNoBindingAnnotationOrType(ParameterElement parameter) {
        return !parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class) &&
               !parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.parameters.RequestBody.class) &&
               !parameter.isAnnotationPresent(Hidden.class) &&

               !parameter.isAnnotationPresent(QueryValue.class) &&
               !parameter.isAnnotationPresent(PathVariable.class) &&
               !parameter.isAnnotationPresent(Body.class) &&
               !parameter.isAnnotationPresent(Part.class) &&
               !parameter.isAnnotationPresent(CookieValue.class) &&
               !parameter.isAnnotationPresent(Header.class) &&

               !isIgnoredParameterType(parameter.getType()) &&
               !isResponseType(parameter.getType()) &&
               !parameter.getType().isAssignable(HttpRequest.class) &&
               !parameter.getType().isAssignable("io.micronaut.http.BasicAuth");
    }

    @Override
    public void visitMethod(MethodElement element, VisitorContext context) {
        if (ignore(element, context)) {
            return;
        }

        HttpMethod httpMethod = httpMethod(element);
        if (httpMethod == null) {
            return;
        }
        Iterator<UriMatchTemplate> matchTemplates = uriMatchTemplates(element).iterator();
        if (!matchTemplates.hasNext()) {
            return;
        }
        UriMatchTemplate matchTemplate = matchTemplates.next();
        PathItem pathItem = resolvePathItem(context, matchTemplate);
        OpenAPI openAPI = resolveOpenAPI(context);

        io.swagger.v3.oas.models.Operation swaggerOperation = readOperation(element, context);

        readTags(element, swaggerOperation, classTags == null ? Collections.emptyList() : classTags);

        readSecurityRequirements(element, context, swaggerOperation);

        readApiResponses(element, context, swaggerOperation);

        readServers(element, context, swaggerOperation);

        readCallbacks(element, context, swaggerOperation);

        JavadocDescription javadocDescription = getMethodDescription(element, swaggerOperation);

        setOperationOnPathItem(pathItem, swaggerOperation, httpMethod);

        if (element.isAnnotationPresent(Deprecated.class)) {
            swaggerOperation.setDeprecated(true);
        }

        readResponse(element, context, openAPI, swaggerOperation, javadocDescription);

        boolean permitsRequestBody = HttpMethod.permitsRequestBody(httpMethod);

        if (permitsRequestBody) {
            readSwaggerRequestBody(element, context, swaggerOperation);
        }

        List<Parameter> swaggerParameters = swaggerOperation.getParameters();
        boolean hasExistingParameters = CollectionUtils.isNotEmpty(swaggerParameters);
        if (!hasExistingParameters) {
            swaggerParameters = new ArrayList<>();
            swaggerOperation.setParameters(swaggerParameters);
        }

        Map<String, UriMatchVariable> pathVariables = pathVariables(matchTemplate);
        List<MediaType> consumesMediaTypes = consumesMediaTypes(element);
        for (ParameterElement parameter : element.getParameters()) {

            ClassElement parameterType = parameter.getGenericType();

            if (isIgnoredParameterType(parameterType)) {
                continue;
            }

            if (permitsRequestBody && swaggerOperation.getRequestBody() == null) {
                readSwaggerRequestBody(parameter, context, swaggerOperation);
            }

            if (parameter.isAnnotationPresent(Body.class)) {
                processBody(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody,
                        consumesMediaTypes, parameter, parameterType);
                continue;
            }

            if (hasExistingParameters) {
                continue;
            }

            Parameter newParameter = processMethodParameterAnnotation(context, permitsRequestBody, pathVariables,
                    parameter);
            if (newParameter == null) {
                continue;
            }
            if (StringUtils.isEmpty(newParameter.getName())) {
                newParameter.setName(parameter.getName());
            }

            if (newParameter.getRequired() == null) {
                newParameter.setRequired(!parameter.isAnnotationPresent(Nullable.class));
            }
            if (javadocDescription != null && StringUtils.isEmpty(newParameter.getDescription())) {

                CharSequence desc = javadocDescription.getParameters().get(parameter.getName());
                if (desc != null) {
                    newParameter.setDescription(desc.toString());
                }
            }
            swaggerParameters.add(newParameter);

            Schema schema = newParameter.getSchema();
            if (schema == null) {
                schema = resolveSchema(openAPI, parameter, parameterType, context, consumesMediaTypes);
            }

            if (schema != null) {
                bindSchemaForElement(context, parameter, parameterType, schema);
                newParameter.setSchema(schema);
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
                        ? Collections.singletonList(MediaType.APPLICATION_JSON_TYPE)
                        : consumesMediaTypes;
                consumesMediaTypes.forEach(mediaType -> {
                    io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
                    ObjectSchema schema = new ObjectSchema();
                    for (ParameterElement parameter : bodyParameters) {
                        Schema propertySchema = resolveSchema(openAPI, parameter, parameter.getType(), context,
                                Collections.singletonList(mediaType));
                        if (propertySchema != null) {

                            Optional<String> description = parameter.getValue(
                                    io.swagger.v3.oas.annotations.Parameter.class, "description", String.class);
                            if (description.isPresent()) {
                                propertySchema.setDescription(description.get());
                            }
                            processSchemaProperty(context, parameter, parameter.getType(), schema, propertySchema);

                            propertySchema.setNullable(parameter.isAnnotationPresent(Nullable.class));
                            if (javadocDescription != null && StringUtils.isEmpty(propertySchema.getDescription())) {
                                CharSequence doc = javadocDescription.getParameters().get(parameter.getName());
                                if (doc != null) {
                                    propertySchema.setDescription(doc.toString());
                                }
                            }
                        }

                    }
                    mt.setSchema(schema);
                    content.addMediaType(mediaType.toString(), mt);
                });

                requestBody.setContent(content);
                requestBody.setRequired(true);
                swaggerOperation.setRequestBody(requestBody);
            }
        }
        // if we have multiple uris, process them
        while (matchTemplates.hasNext()) {
            pathItem = resolvePathItem(context, matchTemplates.next());
            setOperationOnPathItem(pathItem, swaggerOperation, httpMethod);
        }
    }

    private Parameter processMethodParameterAnnotation(VisitorContext context, boolean permitsRequestBody,
            Map<String, UriMatchVariable> pathVariables, ParameterElement parameter) {
        Parameter newParameter = null;
        String parameterName = parameter.getName();
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
                return null;
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
        } else if (parameter.hasAnnotation("io.micronaut.management.endpoint.annotation.Selector")) {
            newParameter = new PathParameter();
            newParameter.setName(parameterName);
        } else if (!permitsRequestBody && hasNoBindingAnnotationOrType(parameter)) {
            // default to QueryValue -
            // https://github.com/micronaut-projects/micronaut-openapi/issues/130
            newParameter = new QueryParameter();
            newParameter.setName(parameterName);
        }

        if (parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class)) {
            AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn = parameter
                    .findAnnotation(io.swagger.v3.oas.annotations.Parameter.class).orElse(null);

            if (paramAnn != null) {

                if (paramAnn.get("hidden", Boolean.class, false).booleanValue()) {
                    // ignore hidden parameters
                    return null;
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
                        if (v == null) {
                            BeanMap<Parameter> target = BeanMap.of(newParameter);
                            for (CharSequence name : paramValues.keySet()) {
                                Object o = paramValues.get(name.toString());
                                try {
                                    target.put(name.toString(), o);
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        } else {
                            // horrible hack because Swagger
                            // ParameterDeserializer breaks updating
                            // existing objects
                            BeanMap<Parameter> beanMap = BeanMap.of(v);
                            BeanMap<Parameter> target = BeanMap.of(newParameter);
                            for (CharSequence name : paramValues.keySet()) {
                                Object o = beanMap.get(name.toString());
                                target.put(name.toString(), o);
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
        return newParameter;
    }

    private void processBody(VisitorContext context, OpenAPI openAPI,
            io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
            boolean permitsRequestBody, List<MediaType> consumesMediaTypes, ParameterElement parameter,
            ClassElement parameterType) {
        if (!permitsRequestBody) {
            return;
        }
        RequestBody requestBody = swaggerOperation.getRequestBody();
        if (requestBody == null) {
            requestBody = new RequestBody();
            swaggerOperation.setRequestBody(requestBody);
        }
        if (requestBody.getDescription() == null && javadocDescription != null) {
            CharSequence desc = javadocDescription.getParameters().get(parameter.getName());
            if (desc != null) {
                requestBody.setDescription(desc.toString());
            }
        }
        if (requestBody.getRequired() == null) {
            requestBody.setRequired(
                    !parameter.isAnnotationPresent(Nullable.class) && !parameterType.isAssignable(Optional.class));
        }
        if (requestBody.getContent() == null) {
            Content content;
            if (consumesMediaTypes.isEmpty()) {
                content = buildContent(parameter, parameterType,
                        Collections.singletonList(MediaType.APPLICATION_JSON_TYPE), openAPI, context);
            } else {
                content = buildContent(parameter, parameterType, consumesMediaTypes, openAPI, context);
            }
            requestBody.setContent(content);
        }
    }

    private void readResponse(MethodElement element, VisitorContext context, OpenAPI openAPI,
            io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription) {
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

            ClassElement returnType = element.getGenericReturnType();
            if (returnType.isAssignable("io.reactivex.Completable")) {
                returnType = null;
            } else if (isResponseType(returnType)) {
                returnType = returnType.getFirstTypeArgument().orElse(returnType);
            } else if (isSingleResponseType(returnType)) {
                returnType = returnType.getFirstTypeArgument().get();
                returnType = returnType.getFirstTypeArgument().orElse(returnType);
            }

            if (returnType != null) {
                List<MediaType> producesMediaTypes = producesMediaTypes(element);
                Content content;
                if (producesMediaTypes.isEmpty()) {
                    content = buildContent(element, returnType, Collections.singletonList(MediaType.APPLICATION_JSON_TYPE), openAPI, context);
                } else {
                    content = buildContent(element, returnType, producesMediaTypes, openAPI, context);
                }
                okResponse.setContent(content);
            }
            responses.put(ApiResponses.DEFAULT, okResponse);
        }
    }

    private Map<String, UriMatchVariable> pathVariables(UriMatchTemplate matchTemplate) {
        List<UriMatchVariable> pv = matchTemplate.getVariables();
        Map<String, UriMatchVariable> pathVariables = new LinkedHashMap<>(pv.size());
        for (UriMatchVariable variable : pv) {
            pathVariables.put(variable.getName(), variable);
        }
        return pathVariables;
    }

    private JavadocDescription getMethodDescription(MethodElement element,
            io.swagger.v3.oas.models.Operation swaggerOperation) {
        String descr = description(element);
        JavadocDescription javadocDescription = element.getDocumentation().map(s -> new JavadocParser().parse(s))
                .orElse(null);
        if (StringUtils.isNotEmpty(descr) && StringUtils.isEmpty(swaggerOperation.getDescription())) {
            swaggerOperation.setDescription(descr);
            swaggerOperation.setSummary(descr);
        }
        if (javadocDescription != null && StringUtils.isEmpty(swaggerOperation.getDescription())) {
            swaggerOperation.setDescription(javadocDescription.getMethodDescription());
            swaggerOperation.setSummary(javadocDescription.getMethodDescription());
        }
        return javadocDescription;
    }

    private io.swagger.v3.oas.models.Operation readOperation(MethodElement element, VisitorContext context) {
        final Optional<AnnotationValue<Operation>> operationAnnotation = element.findAnnotation(Operation.class);
        io.swagger.v3.oas.models.Operation swaggerOperation = operationAnnotation
                .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.Operation.class))
                .orElse(new io.swagger.v3.oas.models.Operation());

        if (StringUtils.isEmpty(swaggerOperation.getOperationId())) {
            swaggerOperation.setOperationId(element.getName());
        }
        return swaggerOperation;
    }

    private void readSecurityRequirements(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        for (SecurityRequirement securityItem : methodSecurityRequirements(element, context)) {
            swaggerOperation.addSecurityItem(securityItem);
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
            parameterType.isAssignable("io.micronaut.security.authentication.Authentication") ||
            parameterType.isAssignable("kotlin.coroutines.Continuation");
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
        for (Server server: methodServers(element, context)) {
            swaggerOperation.addServersItem(server);
        }
    }

    private void readCallbacks(MethodElement element, VisitorContext context,
            io.swagger.v3.oas.models.Operation swaggerOperation) {
        List<AnnotationValue<Callback>> callbackAnnotations = element.getAnnotationValuesByType(Callback.class);
        if (CollectionUtils.isEmpty(callbackAnnotations)) {
            return;
        }
        for (AnnotationValue<Callback> callbackAnn : callbackAnnotations) {
            final Optional<String> name = callbackAnn.get("name", String.class);
            if (!name.isPresent()) {
                continue;
            }
            String callbackName = name.get();
            final Optional<String> expr = callbackAnn.get("callbackUrlExpression", String.class);
            if (expr.isPresent()) {
                processUrlCallbackExpression(context, swaggerOperation, callbackAnn, callbackName, expr.get());
            } else {
                processCallbackReference(context, swaggerOperation, callbackName);
            }
        }
    }

    private void processCallbackReference(VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation,
            String callbackName) {
        final Components components = resolveComponents(resolveOpenAPI(context));
        final Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbackComponents = components
                .getCallbacks();
        if (callbackComponents != null && callbackComponents.containsKey(callbackName)) {
            Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(
                    swaggerOperation);
            final io.swagger.v3.oas.models.callbacks.Callback callbackRef = new io.swagger.v3.oas.models.callbacks.Callback();
            callbackRef.set$ref("#/components/callbacks/" + callbackName);
            callbacks.put(callbackName, callbackRef);
        }
    }

    private void processUrlCallbackExpression(VisitorContext context,
            io.swagger.v3.oas.models.Operation swaggerOperation, AnnotationValue<Callback> callbackAnn,
            String callbackName, final String callbackUrl) {
        final List<AnnotationValue<Operation>> operations = callbackAnn.getAnnotations("operation",
                Operation.class);
        if (CollectionUtils.isEmpty(operations)) {
            Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(
                    swaggerOperation);
            final io.swagger.v3.oas.models.callbacks.Callback c = new io.swagger.v3.oas.models.callbacks.Callback();
            c.addPathItem(callbackUrl, new PathItem());
            callbacks.put(callbackName, c);
        } else {
            final PathItem pathItem = new PathItem();
            for (AnnotationValue<Operation> operation : operations) {
                final Optional<HttpMethod> operationMethod = operation.get("method", HttpMethod.class);
                operationMethod.ifPresent(httpMethod -> toValue(operation.getValues(), context,
                        io.swagger.v3.oas.models.Operation.class)
                                .ifPresent(op -> setOperationOnPathItem(pathItem, op, httpMethod)));
            }
            Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(
                    swaggerOperation);
            final io.swagger.v3.oas.models.callbacks.Callback c = new io.swagger.v3.oas.models.callbacks.Callback();
            c.addPathItem(callbackUrl, pathItem);
            callbacks.put(callbackName, c);
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

    private void addTagIfNotPresent(String tag, io.swagger.v3.oas.models.Operation swaggerOperation) {
        List<String> tags = swaggerOperation.getTags();
        if (tags == null || ! tags.contains(tag)) {
            swaggerOperation.addTagsItem(tag);
        }
    }

    private void readTags(MethodElement element, io.swagger.v3.oas.models.Operation swaggerOperation, List<io.swagger.v3.oas.models.tags.Tag> classTags) {
        element.getAnnotationValuesByType(Tag.class).forEach(av -> av.get("name", String.class).ifPresent(swaggerOperation::addTagsItem));
        // only way to get inherited tags
        element.getValues(Tags.class, AnnotationValue.class).forEach((k, v) -> v.get("name", String.class).ifPresent(name -> addTagIfNotPresent((String) name, swaggerOperation)));

        classTags.forEach(tag -> addTagIfNotPresent(tag.getName(), swaggerOperation));
    }

    private List<io.swagger.v3.oas.models.tags.Tag> readTags(ClassElement element, VisitorContext context) {
        return element.getAnnotationValuesByType(Tag.class).stream()
                .map(av -> toValue(av.getValues(), context, io.swagger.v3.oas.models.tags.Tag.class))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    private Content buildContent(Element definingElement, ClassElement type, List<MediaType> mediaTypes, OpenAPI openAPI, VisitorContext context) {
        Content content = new Content();
        mediaTypes.forEach(mediaType ->  {
                io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
                mt.setSchema(resolveSchema(openAPI, definingElement, type, context, Collections.singletonList(mediaType)));
                content.addMediaType(mediaType.toString(), mt);

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
