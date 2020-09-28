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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import edu.umd.cs.findbugs.annotations.NonNull;
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
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.http.uri.UriMatchVariable;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
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
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
@Experimental
abstract class AbstractOpenApiEndpointVisitor extends AbstractOpenApiVisitor {
    protected List<io.swagger.v3.oas.models.tags.Tag> classTags;
    protected PropertyPlaceholderResolver propertyPlaceholderResolver;

    private static boolean isAnnotationPresent(Element element, String className) {
        return element.findAnnotation(className).isPresent();
    }

    private static RequestBody mergeRequestBody(RequestBody rq1, RequestBody rq2) {
        if (rq1.getRequired() == null) {
            rq1.setRequired(rq2.getRequired());
        }
        if (rq1.get$ref() == null) {
            rq1.set$ref(rq2.get$ref());
        }
        if (rq1.getDescription() == null) {
            rq1.setDescription(rq2.getDescription());
        }
        if (rq1.getExtensions() == null) {
            rq1.setExtensions(rq2.getExtensions());
        } else if (rq2.getExtensions() != null) {
            rq1.getExtensions().forEach((key, value) -> rq1.getExtensions().putIfAbsent(key, value));
        }
        if (rq1.getContent() == null) {
            rq1.setContent(rq2.getContent());
        } else if (rq2.getContent() != null) {
            Content c1 = rq1.getContent();
            Content c2 = rq2.getContent();
            c2.forEach((key, value) -> c1.putIfAbsent(key, value));
        }
        return rq1;
    }

    /**
     * Executed when a class is encountered that matches the <C> generic.
     *
     * @param element The element
     * @param context The visitor context
     */
    public void visitClass(ClassElement element, VisitorContext context) {
        if (ignore(element, context)) {
            return;
        }
        incrementVisitedElements(context);
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
     *
     * @param element The MethodElement.
     * @param context The context.
     * @return The security requirements.
     */
    protected abstract List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context);

    /**
     * Returns the servers at method level.
     *
     * @param element The MethodElement.
     * @param context The context.
     * @return The servers.
     */
    protected abstract List<io.swagger.v3.oas.models.servers.Server> methodServers(MethodElement element, VisitorContext context);

    /**
     * Returns the class tags.
     *
     * @param element The ClassElement.
     * @param context The context.
     * @return The class tags.
     */
    protected abstract List<io.swagger.v3.oas.models.tags.Tag> classTags(ClassElement element, VisitorContext context);

    /**
     * Returns true if the specified element should not be processed.
     *
     * @param element The ClassElement.
     * @param context The context.
     * @return true if the specified element should not be processed.
     */
    protected abstract boolean ignore(ClassElement element, VisitorContext context);

    /**
     * Returns true if the specified element should not be processed.
     *
     * @param element The ClassElement.
     * @param context The context.
     * @return true if the specified element should not be processed.
     */
    protected abstract boolean ignore(MethodElement element, VisitorContext context);

    /**
     * Returns the HttpMethod of the element.
     *
     * @param element The MethodElement.
     * @return The HttpMethod of the element.
     */
    protected abstract HttpMethod httpMethod(MethodElement element);

    /**
     * Returns the uri paths of the element.
     *
     * @param element The MethodElement.
     * @return The uri paths of the element.
     */
    protected abstract List<UriMatchTemplate> uriMatchTemplates(MethodElement element);

    /**
     * Returns the consumes media types.
     *
     * @param element The MethodElement.
     * @return The consumes media types.
     */
    protected abstract List<MediaType> consumesMediaTypes(MethodElement element);

    /**
     * Returns the produces media types.
     *
     * @param element The MethodElement.
     * @return The produces media types.
     */
    protected abstract List<MediaType> producesMediaTypes(MethodElement element);

    /**
     * Returns the description for the element.
     *
     * @param element The MethodElement.
     * @return The description for the element.
     */
    protected abstract String description(MethodElement element);

    private boolean hasNoBindingAnnotationOrType(ParameterElement parameter) {
        return !parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.parameters.RequestBody.class) &&
                !parameter.isAnnotationPresent(QueryValue.class) &&
                !parameter.isAnnotationPresent(PathVariable.class) &&
                !parameter.isAnnotationPresent(Body.class) &&
                !parameter.isAnnotationPresent(Part.class) &&
                !parameter.isAnnotationPresent(CookieValue.class) &&
                !parameter.isAnnotationPresent(Header.class) &&
                !isResponseType(parameter.getType());
    }

    /**
     * Executed when a method is encountered that matches the <E> generic.
     *
     * @param element The element
     * @param context The visitor context
     */
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
        incrementVisitedElements(context);
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

        swaggerOperation = setOperationOnPathItem(pathItem, swaggerOperation, httpMethod);

        if (element.isAnnotationPresent(Deprecated.class)) {
            swaggerOperation.setDeprecated(true);
        }

        readResponse(element, context, openAPI, swaggerOperation, javadocDescription);

        boolean permitsRequestBody = HttpMethod.permitsRequestBody(httpMethod);
        if (permitsRequestBody) {
            Optional<RequestBody> requestBody = readSwaggerRequestBody(element, context);
            if (requestBody.isPresent()) {
                RequestBody currentRequestBody = swaggerOperation.getRequestBody();
                if (currentRequestBody != null) {
                    swaggerOperation.setRequestBody(mergeRequestBody(currentRequestBody, requestBody.get()));
                } else {
                    swaggerOperation.setRequestBody(requestBody.get());
                }
            }
        }


        Map<String, UriMatchVariable> pathVariables = pathVariables(matchTemplate);
        List<MediaType> consumesMediaTypes = consumesMediaTypes(element);
        List<ParameterElement> extraBodyParameters = new ArrayList<>();
        processParameters(element, context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables, consumesMediaTypes, extraBodyParameters);

        processExtraBodyParameters(context, httpMethod, openAPI, swaggerOperation, javadocDescription, consumesMediaTypes, extraBodyParameters);

        // if we have multiple uris, process them
        while (matchTemplates.hasNext()) {
            pathItem = resolvePathItem(context, matchTemplates.next());
            swaggerOperation = setOperationOnPathItem(pathItem, swaggerOperation, httpMethod);
        }
    }

    private void processExtraBodyParameters(VisitorContext context, HttpMethod httpMethod, OpenAPI openAPI,
                                            io.swagger.v3.oas.models.Operation swaggerOperation,
                                            JavadocDescription javadocDescription,
                                            List<MediaType> consumesMediaTypes,
                                            List<ParameterElement> extraBodyParameters) {
        RequestBody requestBody = swaggerOperation.getRequestBody();
        if (HttpMethod.requiresRequestBody(httpMethod) || HttpMethod.permitsRequestBody(httpMethod) && !extraBodyParameters.isEmpty()) {
            if (requestBody == null) {
                requestBody = new RequestBody();
                Content content = new Content();
                requestBody.setContent(content);
                requestBody.setRequired(true);
                swaggerOperation.setRequestBody(requestBody);

                consumesMediaTypes = consumesMediaTypes.isEmpty() ? Collections.singletonList(MediaType.APPLICATION_JSON_TYPE) : consumesMediaTypes;
                consumesMediaTypes.forEach(mediaType -> {
                    io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
                    mt.setSchema(new ObjectSchema());
                    content.addMediaType(mediaType.toString(), mt);
                });
            }
        }
        if (requestBody != null && !extraBodyParameters.isEmpty()) {
            requestBody.getContent().forEach((mediaTypeName, mediaType) -> {
                Schema schema = mediaType.getSchema();
                if (schema.get$ref() != null) {
                    ComposedSchema composedSchema = new ComposedSchema();
                    Schema extraBodyParametersSchema = new Schema();
                    // Composition of existing + a new schema where extra body parameters are going to be added
                    composedSchema.addAllOfItem(schema);
                    composedSchema.addAllOfItem(extraBodyParametersSchema);
                    schema = extraBodyParametersSchema;
                    mediaType.setSchema(composedSchema);
                }
                for (ParameterElement parameter : extraBodyParameters) {
                    processBodyParameter(context, openAPI, javadocDescription, MediaType.of(mediaTypeName), schema, parameter);
                }
            });
        }
    }

    private void processParameters(MethodElement element, VisitorContext context, OpenAPI openAPI,
                                   io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
                                   boolean permitsRequestBody,
                                   Map<String, UriMatchVariable> pathVariables,
                                   List<MediaType> consumesMediaTypes,
                                   List<ParameterElement> extraBodyParameters) {
        List<Parameter> swaggerParameters = swaggerOperation.getParameters();
        boolean hasExistingParameters = CollectionUtils.isNotEmpty(swaggerParameters);
        if (!hasExistingParameters) {
            swaggerParameters = new ArrayList<>();
            swaggerOperation.setParameters(swaggerParameters);
        }
        for (ParameterElement parameter : element.getParameters()) {
            processParameter(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables,
                    consumesMediaTypes, swaggerParameters, hasExistingParameters, parameter, extraBodyParameters);
        }
    }

    private void processParameter(VisitorContext context, OpenAPI openAPI,
                                  io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
                                  boolean permitsRequestBody, Map<String, UriMatchVariable> pathVariables, List<MediaType> consumesMediaTypes,
                                  List<Parameter> swaggerParameters, boolean hasExistingParameters, ParameterElement parameter,
                                  List<ParameterElement> extraBodyParameters) {
        ClassElement parameterType = parameter.getGenericType();

        if (ignoreParameter(parameter)) {
            return;
        }
        if (isJavaElement(parameterType, context)) {
            parameterType = new JavaClassElementExt(parameterType, context);
        }
        if (permitsRequestBody && swaggerOperation.getRequestBody() == null) {
            readSwaggerRequestBody(parameter, context).ifPresent(swaggerOperation::setRequestBody);
        }

        if (parameter.isAnnotationPresent(Body.class)) {
            processBody(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody,
                    consumesMediaTypes, parameter, parameterType);
            return;
        }

        if (hasExistingParameters) {
            return;
        }

        Parameter newParameter = processMethodParameterAnnotation(context, permitsRequestBody, pathVariables,
                parameter, extraBodyParameters);
        if (newParameter == null) {
            return;
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

    private void processBodyParameter(VisitorContext context, OpenAPI openAPI, JavadocDescription javadocDescription,
                                      MediaType mediaType, Schema schema, ParameterElement parameter) {
        Schema propertySchema = resolveSchema(openAPI, parameter, parameter.getType(), context,
                Collections.singletonList(mediaType));
        if (propertySchema != null) {

            Optional<String> description = parameter.getValue(io.swagger.v3.oas.annotations.Parameter.class,
                    "description", String.class);
            if (description.isPresent()) {
                propertySchema.setDescription(description.get());
            }
            processSchemaProperty(context, parameter, parameter.getType(), null, schema, propertySchema);
            if (parameter.isAnnotationPresent(Nullable.class) || parameter.getType().isAssignable(Optional.class)) {
                // Keep null if not
                propertySchema.setNullable(true);
            }
            if (javadocDescription != null && StringUtils.isEmpty(propertySchema.getDescription())) {
                CharSequence doc = javadocDescription.getParameters().get(parameter.getName());
                if (doc != null) {
                    propertySchema.setDescription(doc.toString());
                }
            }
        }
    }

    private Parameter processMethodParameterAnnotation(VisitorContext context, boolean permitsRequestBody,
                                                       Map<String, UriMatchVariable> pathVariables, ParameterElement parameter,
                                                       List<ParameterElement> extraBodyParameters) {
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
        } else if (hasNoBindingAnnotationOrType(parameter)) {
            AnnotationValue<io.swagger.v3.oas.annotations.Parameter> parameterAnnotation = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            // Skip recognizing parameter if it's manually defined by "in"
            if (parameterAnnotation == null || !parameterAnnotation.booleanValue("hidden").orElse(false)
                    && !parameterAnnotation.stringValue("in").isPresent()) {
                if (permitsRequestBody) {
                    extraBodyParameters.add(parameter);
                } else {
                    // default to QueryValue -
                    // https://github.com/micronaut-projects/micronaut-openapi/issues/130
                    newParameter = new QueryParameter();
                    newParameter.setName(parameterName);
                }
            }
        }

        if (parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class)) {
            AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn = parameter
                    .findAnnotation(io.swagger.v3.oas.annotations.Parameter.class).orElse(null);

            if (paramAnn != null) {

                if (paramAnn.get("hidden", Boolean.class, false)) {
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

        final Content content;
        if (consumesMediaTypes.isEmpty()) {
            content = buildContent(parameter, parameterType,
                    Collections.singletonList(MediaType.APPLICATION_JSON_TYPE), openAPI, context);
        } else {
            content = buildContent(parameter, parameterType, consumesMediaTypes, openAPI, context);
        }
        if (requestBody.getContent() == null) {
            requestBody.setContent(content);
        } else {
            final Content currentContent = requestBody.getContent();
            content.forEach((key, value) -> currentContent.putIfAbsent(key, value));
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
            ClassElement returnType = returnType(element, context);
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

    private ClassElement returnType(MethodElement element, VisitorContext context) {
        ClassElement returnType = isJavaElement(element.getOwningType(), context) ? JavaClassElementExt.getGenericReturnType(element, context)
                : element.getGenericReturnType();
        if (returnType.isAssignable("io.reactivex.Completable")) {
            returnType = null;
        } else if (isResponseType(returnType)) {
            returnType = returnType.getFirstTypeArgument().orElse(returnType);
        } else if (isSingleResponseType(returnType)) {
            returnType = returnType.getFirstTypeArgument().get();
            returnType = returnType.getFirstTypeArgument().orElse(returnType);
        }
        if (isJavaElement(returnType, context)) {
            returnType = new JavaClassElementExt(returnType, context);
        }
        return returnType;
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
        if (StringUtils.isNotEmpty(descr) && StringUtils.isEmpty(swaggerOperation.getDescription())) {
            swaggerOperation.setDescription(descr);
            swaggerOperation.setSummary(descr);
        }
        JavadocDescription javadocDescription = element.getDocumentation().map(s -> new JavadocParser().parse(s))
                .orElse(null);
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

    private boolean ignoreParameter(ParameterElement parameter) {
        return parameter.isAnnotationPresent(Hidden.class) ||
                parameter.isAnnotationPresent(JsonIgnore.class) ||
                parameter.getValue(io.swagger.v3.oas.annotations.Parameter.class, "hidden", Boolean.class)
                        .orElse(false) ||
                isAnnotationPresent(parameter, "io.micronaut.session.annotation.SessionValue") ||
                isIgnoredParameterType(parameter.getType());
    }

    private boolean isIgnoredParameterType(ClassElement parameterType) {
        return parameterType == null ||
                parameterType.isAssignable(Principal.class) ||
                parameterType.isAssignable("io.micronaut.session.Session") ||
                parameterType.isAssignable("io.micronaut.security.authentication.Authentication") ||
                parameterType.isAssignable("kotlin.coroutines.Continuation") ||
                parameterType.isAssignable(HttpRequest.class) ||
                parameterType.isAssignable("io.micronaut.http.BasicAuth");
    }

    private boolean isResponseType(ClassElement returnType) {
        return returnType.isAssignable(HttpResponse.class) || returnType.isAssignable("org.springframework.http.HttpEntity");
    }

    private boolean isSingleResponseType(ClassElement returnType) {
        return returnType.isAssignable("io.reactivex.Single")
                && returnType.getFirstTypeArgument().isPresent()
                && isResponseType(returnType.getFirstTypeArgument().get());
    }

    private io.swagger.v3.oas.models.Operation setOperationOnPathItem(PathItem pathItem, io.swagger.v3.oas.models.Operation swaggerOperation, HttpMethod httpMethod) {
        io.swagger.v3.oas.models.Operation operation = swaggerOperation;
        switch (httpMethod) {
            case GET:
                if (pathItem.getGet() != null) {
                    operation = pathItem.getGet();
                } else {
                    pathItem.get(swaggerOperation);
                }
                break;
            case POST:
                if (pathItem.getPost() != null) {
                    operation = pathItem.getPost();
                } else {
                    pathItem.post(swaggerOperation);
                }
                break;
            case PUT:
                if (pathItem.getPut() != null) {
                    operation = pathItem.getPut();
                } else {
                    pathItem.put(swaggerOperation);
                }
                break;
            case PATCH:
                if (pathItem.getPatch() != null) {
                    operation = pathItem.getPatch();
                } else {
                    pathItem.patch(swaggerOperation);
                }
                break;
            case DELETE:
                if (pathItem.getDelete() != null) {
                    operation = pathItem.getDelete();
                } else {
                    pathItem.delete(swaggerOperation);
                }
                break;
            case HEAD:
                if (pathItem.getHead() != null) {
                    operation = pathItem.getHead();
                } else {
                    pathItem.head(swaggerOperation);
                }
                break;
            case OPTIONS:
                if (pathItem.getOptions() != null) {
                    operation = pathItem.getOptions();
                } else {
                    pathItem.options(swaggerOperation);
                }
                break;
            case TRACE:
                if (pathItem.getTrace() != null) {
                    operation = pathItem.getTrace();
                } else {
                    pathItem.trace(swaggerOperation);
                }
                break;
            default:
                operation = swaggerOperation;
                // unprocessable
        }
        return operation;
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

    private Optional<RequestBody> readSwaggerRequestBody(Element element, VisitorContext context) {
        return element.findAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class)
                .flatMap(annotation -> toValue(annotation.getValues(), context, RequestBody.class));
    }

    private void readServers(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        for (Server server : methodServers(element, context)) {
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
        if (tags == null || !tags.contains(tag)) {
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
        mediaTypes.forEach(mediaType -> {
            io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
            mt.setSchema(resolveSchema(openAPI, definingElement, type, context, Collections.singletonList(mediaType)));
            content.addMediaType(mediaType.toString(), mt);

        });
        return content;
    }

    /**
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

                @NonNull
                @Override
                public Collection<String> getPropertyEntries(@NonNull String name) {
                    return null;
                }
            }, new DefaultConversionService());
        }
        return this.propertyPlaceholderResolver;
    }
}
