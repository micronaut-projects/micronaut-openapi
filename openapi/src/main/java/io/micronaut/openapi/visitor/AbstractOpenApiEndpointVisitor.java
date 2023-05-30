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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.PathMatcher;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.CookieValue;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.RequestBean;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.annotation.UriMapping;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.http.uri.UriMatchVariable;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIDecorator;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.visitor.security.InterceptUrlMapPattern;
import io.micronaut.openapi.visitor.security.SecurityProperties;
import io.micronaut.openapi.visitor.security.SecurityRule;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.callbacks.Callback;
import io.swagger.v3.oas.annotations.callbacks.Callbacks;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
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
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.getSecurityProperties;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.ElementUtils.isNullable;
import static io.micronaut.openapi.visitor.Utils.DEFAULT_MEDIA_TYPES;

/**
 * A {@link io.micronaut.inject.visitor.TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
public abstract class AbstractOpenApiEndpointVisitor extends AbstractOpenApiVisitor {

    public static final String COMPONENTS_CALLBACKS_PREFIX = "#/components/callbacks/";

    protected static final String CONTEXT_CHILD_PATH = "internal.child.path";
    protected static final String CONTEXT_CHILD_OP_ID_PREFIX = "internal.opId.prefix";
    protected static final String CONTEXT_CHILD_OP_ID_SUFFIX = "internal.opId.suffix";
    protected static final String CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS = "internal.opId.suffixes.add.always";
    protected static final String IS_PROCESS_PARENT_CLASS = "internal.is.process.parent";

    private static final TypeReference<Map<CharSequence, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final String THREE_DOTS = "...";

    protected List<io.swagger.v3.oas.models.tags.Tag> classTags;
    protected io.swagger.v3.oas.models.ExternalDocumentation classExternalDocs;

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
            c2.forEach(c1::putIfAbsent);
        }
        return rq1;
    }

    /**
     * Executed when a class is encountered that matches the generic class.
     *
     * @param element The element
     * @param context The visitor context
     */
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context)) {
            return;
        }
        if (ignore(element, context)) {
            return;
        }
        incrementVisitedElements(context);
        processSecuritySchemes(element, context);
        processTags(element, context);
        processExternalDocs(element, context);
        context.remove(CONTEXT_CHILD_PATH);

        if (element.isAnnotationPresent(Controller.class)) {

            element.stringValue(UriMapping.class).ifPresent(url -> context.put(CONTEXT_CHILD_PATH, url));
            String prefix = "";
            String suffix = "";
            boolean addAlways = true;
            AnnotationValue<OpenAPIDecorator> apiDecorator = element.getDeclaredAnnotation(OpenAPIDecorator.class);
            if (apiDecorator != null) {
                prefix = apiDecorator.stringValue().orElse("");
                suffix = apiDecorator.stringValue("opIdSuffix").orElse("");
                addAlways = apiDecorator.booleanValue("addAlways").orElse(true);
            }
            context.put(CONTEXT_CHILD_OP_ID_PREFIX, prefix);
            context.put(CONTEXT_CHILD_OP_ID_SUFFIX, suffix);
            context.put(CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, addAlways);

            List<ClassElement> superTypes = new ArrayList<>();
            Collection<ClassElement> parentInterfaces = element.getInterfaces();
            if (element.isInterface() && !parentInterfaces.isEmpty()) {
                for (ClassElement parentInterface : parentInterfaces) {
                    if (ClassUtils.isJavaLangType(parentInterface.getName())) {
                        continue;
                    }
                    superTypes.add(parentInterface);
                }
            } else {
                element.getSuperType().ifPresent(superTypes::add);
            }

            if (CollectionUtils.isNotEmpty(superTypes)) {
                context.put(IS_PROCESS_PARENT_CLASS, true);
                List<MethodElement> methods = element.getEnclosedElements(ElementQuery.ALL_METHODS);
                for (MethodElement method : methods) {
                    visitMethod(method, context);
                }
                context.remove(IS_PROCESS_PARENT_CLASS);
            }

            context.remove(CONTEXT_CHILD_OP_ID_PREFIX);
            context.remove(CONTEXT_CHILD_OP_ID_SUFFIX);
            context.remove(CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS);
        }
    }

    private void processTags(ClassElement element, VisitorContext context) {
        classTags = readTags(element, context);
        List<io.swagger.v3.oas.models.tags.Tag> userDefinedClassTags = classTags(element, context);
        if (CollectionUtils.isEmpty(classTags)) {
            classTags = userDefinedClassTags == null ? Collections.emptyList() : userDefinedClassTags;
        } else if (userDefinedClassTags != null) {
            for (io.swagger.v3.oas.models.tags.Tag tag : userDefinedClassTags) {
                if (!containsTag(tag.getName(), classTags)) {
                    classTags.add(tag);
                }
            }
        }
    }

    private void processExternalDocs(ClassElement element, VisitorContext context) {
        final Optional<AnnotationValue<ExternalDocumentation>> externalDocsAnn = element.findAnnotation(ExternalDocumentation.class);
        classExternalDocs = externalDocsAnn
            .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.ExternalDocumentation.class))
            .orElse(null);
    }

    private boolean containsTag(String name, List<io.swagger.v3.oas.models.tags.Tag> tags) {
        return tags.stream().anyMatch(tag -> name.equals(tag.getName()));
    }

    /**
     * Returns the security requirements at method level.
     *
     * @param element The MethodElement.
     * @param context The context.
     *
     * @return The security requirements.
     */
    protected abstract List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context);

    /**
     * Returns the servers at method level.
     *
     * @param element The MethodElement.
     * @param context The context.
     *
     * @return The servers.
     */
    protected abstract List<Server> methodServers(MethodElement element, VisitorContext context);

    /**
     * Returns the class tags.
     *
     * @param element The ClassElement.
     * @param context The context.
     *
     * @return The class tags.
     */
    protected abstract List<io.swagger.v3.oas.models.tags.Tag> classTags(ClassElement element, VisitorContext context);

    /**
     * Returns true if the specified element should not be processed.
     *
     * @param element The ClassElement.
     * @param context The context.
     *
     * @return true if the specified element should not be processed.
     */
    protected abstract boolean ignore(ClassElement element, VisitorContext context);

    /**
     * Returns true if the specified element should not be processed.
     *
     * @param element The ClassElement.
     * @param context The context.
     *
     * @return true if the specified element should not be processed.
     */
    protected abstract boolean ignore(MethodElement element, VisitorContext context);

    /**
     * Returns the HttpMethod of the element.
     *
     * @param element The MethodElement.
     *
     * @return The HttpMethod of the element.
     */
    protected abstract HttpMethod httpMethod(MethodElement element);

    /**
     * Returns the uri paths of the element.
     *
     * @param element The MethodElement.
     * @param context The context
     *
     * @return The uri paths of the element.
     */
    protected abstract List<UriMatchTemplate> uriMatchTemplates(MethodElement element, VisitorContext context);

    /**
     * Returns the consumes media types.
     *
     * @param element The MethodElement.
     *
     * @return The consumes media types.
     */
    protected abstract List<MediaType> consumesMediaTypes(MethodElement element);

    /**
     * Returns the produces media types.
     *
     * @param element The MethodElement.
     *
     * @return The produces media types.
     */
    protected abstract List<MediaType> producesMediaTypes(MethodElement element);

    /**
     * Returns the description for the element.
     *
     * @param element The MethodElement.
     *
     * @return The description for the element.
     */
    protected abstract String description(MethodElement element);

    private boolean hasNoBindingAnnotationOrType(TypedElement parameter) {
        return !parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.parameters.RequestBody.class) &&
            !parameter.isAnnotationPresent(QueryValue.class) &&
            !parameter.isAnnotationPresent(PathVariable.class) &&
            !parameter.isAnnotationPresent(Body.class) &&
            !parameter.isAnnotationPresent(Part.class) &&
            !parameter.isAnnotationPresent(CookieValue.class) &&
            !parameter.isAnnotationPresent(Header.class) &&
            !parameter.isAnnotationPresent(RequestBean.class) &&
            !isResponseType(parameter.getType());
    }

    /**
     * Executed when a method is encountered that matches the generic element.
     *
     * @param element The element
     * @param context The visitor context
     */
    public void visitMethod(MethodElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context)) {
            return;
        }
        if (ignore(element, context)) {
            return;
        }
        HttpMethod httpMethod = httpMethod(element);
        if (httpMethod == null) {
            return;
        }
        List<UriMatchTemplate> matchTemplates = uriMatchTemplates(element, context);
        if (CollectionUtils.isEmpty(matchTemplates)) {
            return;
        }
        incrementVisitedElements(context);
        OpenAPI openAPI = Utils.resolveOpenApi(context);
        JavadocDescription javadocDescription = null;
        boolean permitsRequestBody = HttpMethod.permitsRequestBody(httpMethod);

        Map<String, List<PathItem>> pathItemsMap = resolvePathItems(context, matchTemplates);

        for (Map.Entry<String, List<PathItem>> pathItemEntry : pathItemsMap.entrySet()) {
            List<PathItem> pathItems = pathItemEntry.getValue();

            Map<PathItem, io.swagger.v3.oas.models.Operation> swaggerOperations = readOperations(pathItemEntry.getKey(), httpMethod, pathItems, element, context);

            for (Map.Entry<PathItem, io.swagger.v3.oas.models.Operation> operationEntry : swaggerOperations.entrySet()) {
                io.swagger.v3.oas.models.Operation swaggerOperation = operationEntry.getValue();
                io.swagger.v3.oas.models.ExternalDocumentation externalDocs = readExternalDocs(element, context);
                if (externalDocs == null) {
                    externalDocs = classExternalDocs;
                }
                if (externalDocs != null) {
                    swaggerOperation.setExternalDocs(externalDocs);
                }

                readTags(element, context, swaggerOperation, classTags == null ? Collections.emptyList() : classTags, openAPI);

                readSecurityRequirements(element, pathItemEntry.getKey(), swaggerOperation, context);

                readApiResponses(element, context, swaggerOperation);

                readServers(element, context, swaggerOperation);

                readCallbacks(element, context, swaggerOperation);

                javadocDescription = getMethodDescription(element, swaggerOperation);

                if (element.isAnnotationPresent(Deprecated.class)) {
                    swaggerOperation.setDeprecated(true);
                }

                readResponse(element, context, openAPI, swaggerOperation, javadocDescription);

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

                swaggerOperation = setOperationOnPathItem(operationEntry.getKey(), swaggerOperation, httpMethod);
            }

            Map<String, UriMatchVariable> pathVariables = new HashMap<>();
            for (UriMatchTemplate matchTemplate : matchTemplates) {
                for (Map.Entry<String, UriMatchVariable> varEntry : pathVariables(matchTemplate).entrySet()) {
                    if (pathItemEntry.getKey().contains("{" + varEntry.getKey() + '}')) {
                        pathVariables.put(varEntry.getKey(), varEntry.getValue());
                    }
                }
                // @Parameters declared at method level take precedence over the declared as method arguments, so we process them first
                processParameterAnnotationInMethod(element, openAPI, matchTemplate, httpMethod, pathVariables);
            }
            List<MediaType> consumesMediaTypes = consumesMediaTypes(element);
            List<TypedElement> extraBodyParameters = new ArrayList<>();
            for (io.swagger.v3.oas.models.Operation operation : swaggerOperations.values()) {
                processParameters(element, context, openAPI, operation, javadocDescription, permitsRequestBody, pathVariables, consumesMediaTypes, extraBodyParameters, httpMethod, matchTemplates, pathItems);
                processExtraBodyParameters(context, httpMethod, openAPI, operation, javadocDescription, consumesMediaTypes, extraBodyParameters);
            }
        }
    }

    private void processExtraBodyParameters(VisitorContext context, HttpMethod httpMethod, OpenAPI openAPI,
                                            io.swagger.v3.oas.models.Operation swaggerOperation,
                                            JavadocDescription javadocDescription,
                                            List<MediaType> consumesMediaTypes,
                                            List<TypedElement> extraBodyParameters) {
        RequestBody requestBody = swaggerOperation.getRequestBody();
        if (HttpMethod.permitsRequestBody(httpMethod) && !extraBodyParameters.isEmpty()) {
            if (requestBody == null) {
                requestBody = new RequestBody();
                Content content = new Content();
                requestBody.setContent(content);
                requestBody.setRequired(true);
                swaggerOperation.setRequestBody(requestBody);

                consumesMediaTypes = consumesMediaTypes.isEmpty() ? DEFAULT_MEDIA_TYPES : consumesMediaTypes;
                consumesMediaTypes.forEach(mediaType -> {
                    io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
                    Schema schema = new Schema();
                    schema.setType(TYPE_OBJECT);
                    mt.setSchema(schema);
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
                for (TypedElement parameter : extraBodyParameters) {
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
                                   List<TypedElement> extraBodyParameters,
                                   HttpMethod httpMethod,
                                   List<UriMatchTemplate> matchTemplates,
                                   List<PathItem> pathItems) {
        if (ArrayUtils.isEmpty(element.getParameters())) {
            return;
        }
        List<Parameter> swaggerParameters = swaggerOperation.getParameters();
        if (CollectionUtils.isEmpty(swaggerParameters)) {
            swaggerParameters = new ArrayList<>();
        }

        for (ParameterElement parameter : element.getParameters()) {
            if (!alreadyProcessedParameter(swaggerParameters, parameter)) {
                processParameter(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables,
                    consumesMediaTypes, swaggerParameters, parameter, extraBodyParameters, httpMethod, matchTemplates, pathItems);
            }
        }
        if (CollectionUtils.isNotEmpty(swaggerParameters)) {
            swaggerOperation.setParameters(swaggerParameters);
        }
    }

    private boolean alreadyProcessedParameter(List<Parameter> swaggerParameters, ParameterElement parameter) {
        return swaggerParameters.stream()
            .anyMatch(p -> p.getName().equals(parameter.getName()) && p.getIn() != null);
    }

    private void processParameterAnnotationInMethod(MethodElement element,
                                                    OpenAPI openAPI,
                                                    UriMatchTemplate matchTemplate,
                                                    HttpMethod httpMethod,
                                                    Map<String, UriMatchVariable> pathVariables) {

        List<AnnotationValue<io.swagger.v3.oas.annotations.Parameter>> parameterAnnotations = element
            .getDeclaredAnnotationValuesByType(io.swagger.v3.oas.annotations.Parameter.class);

        for (AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn : parameterAnnotations) {
            if (paramAnn.get("hidden", Boolean.class, false)) {
                continue;
            }

            Parameter parameter = new Parameter();
            parameter.schema(new Schema());

            paramAnn.stringValue("name").ifPresent(parameter::name);
            paramAnn.enumValue("in", ParameterIn.class).ifPresent(in -> parameter.in(in.toString()));
            paramAnn.stringValue("description").ifPresent(parameter::description);
            paramAnn.booleanValue("required").ifPresent(value -> parameter.setRequired(value ? true : null));
            paramAnn.booleanValue("deprecated").ifPresent(value -> parameter.setDeprecated(value ? true : null));
            paramAnn.booleanValue("allowEmptyValue").ifPresent(value -> parameter.setAllowEmptyValue(value ? true : null));
            paramAnn.booleanValue("allowReserved").ifPresent(value -> parameter.setAllowReserved(value ? true : null));
            paramAnn.stringValue("example").ifPresent(parameter::example);
            paramAnn.stringValue("ref").ifPresent(parameter::$ref);
            paramAnn.enumValue("style", ParameterStyle.class).ifPresent(style -> parameter.setStyle(paramStyle(style)));

            if (parameter.getIn() == null) {
                for (ParameterElement paramEl : element.getParameters()) {
                    if (!paramEl.getName().equals(parameter.getName())) {
                        continue;
                    }
                    if (paramEl.isAnnotationPresent(PathVariable.class)) {
                        parameter.setIn("path");
                    } else if (paramEl.isAnnotationPresent(QueryValue.class)) {
                        parameter.setIn("query");
                    } else if (paramEl.isAnnotationPresent(CookieValue.class)) {
                        parameter.setIn("cookie");
                    } else if (paramEl.isAnnotationPresent(Header.class)) {
                        parameter.setIn("header");
                    } else {
                        UriMatchVariable pathVariable = pathVariables.get(parameter.getName());
                        // check if this parameter is optional path variable
                        if (pathVariable == null) {
                            for (UriMatchVariable variable : matchTemplate.getVariables()) {
                                if (variable.getName().equals(parameter.getName()) && variable.isOptional() && !variable.isQuery() && !variable.isExploded()) {
                                    break;
                                }
                            }
                        }
                        if (pathVariable != null && !pathVariable.isOptional() && !pathVariable.isQuery() && !pathVariable.isExploded()) {
                            parameter.setIn("path");
                        }

                        if (parameter.getIn() == null) {
                            if (httpMethod == HttpMethod.GET) {
                                // default to QueryValue -
                                // https://github.com/micronaut-projects/micronaut-openapi/issues/130
                                parameter.setIn("query");
                            }
                        }

                    }
                }
            }

            PathItem pathItem = openAPI.getPaths().get(matchTemplate.toPathString());
            switch (httpMethod) {
                case GET -> pathItem.getGet().addParametersItem(parameter);
                case POST -> pathItem.getPost().addParametersItem(parameter);
                case PUT -> pathItem.getPut().addParametersItem(parameter);
                case DELETE -> pathItem.getDelete().addParametersItem(parameter);
                case PATCH -> pathItem.getPatch().addParametersItem(parameter);
                // do nothing
                default -> { }
            }
        }
    }

    private void processParameter(VisitorContext context, OpenAPI openAPI,
                                  io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
                                  boolean permitsRequestBody, Map<String, UriMatchVariable> pathVariables, List<MediaType> consumesMediaTypes,
                                  List<Parameter> swaggerParameters, TypedElement parameter,
                                  List<TypedElement> extraBodyParameters,
                                  HttpMethod httpMethod,
                                  List<UriMatchTemplate> matchTemplates,
                                  List<PathItem> pathItems) {
        ClassElement parameterType = parameter.getGenericType();

        if (ignoreParameter(parameter)) {
            return;
        }
        if (permitsRequestBody && swaggerOperation.getRequestBody() == null) {
            readSwaggerRequestBody(parameter, context).ifPresent(swaggerOperation::setRequestBody);
        }

        consumesMediaTypes = CollectionUtils.isNotEmpty(consumesMediaTypes) ? consumesMediaTypes : DEFAULT_MEDIA_TYPES;

        if (parameter.isAnnotationPresent(Body.class)) {
            io.swagger.v3.oas.models.Operation existedOpertion = null;
            // check existed operations
            for (PathItem pathItem : pathItems) {
                existedOpertion = getOperationOnPathItem(pathItem, httpMethod);
                if (existedOpertion != null) {
                    swaggerOperation = existedOpertion;
                    break;
                }
            }

            processBody(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody,
                consumesMediaTypes, parameter, parameterType);

            RequestBody requestBody = swaggerOperation.getRequestBody();
            if (requestBody != null && requestBody.getContent() != null) {
                if (existedOpertion != null) {
                    for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry : existedOpertion.getRequestBody().getContent().entrySet()) {
                        boolean found = false;
                        for (MediaType mediaType : consumesMediaTypes) {
                            if (entry.getKey().equals(mediaType.getName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            continue;
                        }

                        io.swagger.v3.oas.models.media.MediaType mediaType = entry.getValue();

                        Schema propertySchema = bindSchemaForElement(context, parameter, parameterType, mediaType.getSchema());

                        String bodyAnnValue = parameter.getAnnotation(Body.class).getValue(String.class).orElse(null);
                        if (StringUtils.isNotEmpty(bodyAnnValue)) {
                            Schema wrapperSchema = new Schema();
                            wrapperSchema.setType(TYPE_OBJECT);
                            if (isElementNotNullable(parameter, parameterType)) {
                                wrapperSchema.addRequiredItem(bodyAnnValue);
                            }
                            wrapperSchema.addProperty(bodyAnnValue, propertySchema);
                            mediaType.setSchema(wrapperSchema);
                        }
                    }
                }
            }

            return;
        }

        if (parameter.isAnnotationPresent(RequestBean.class)) {
            processRequestBean(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables,
                consumesMediaTypes, swaggerParameters, parameter, extraBodyParameters, httpMethod, matchTemplates, pathItems);
            return;
        }

        Parameter newParameter = processMethodParameterAnnotation(context, swaggerOperation, permitsRequestBody, pathVariables, parameter, extraBodyParameters, httpMethod, matchTemplates);
        if (newParameter == null) {
            return;
        }
        if (newParameter.get$ref() != null) {
            addSwaggerParamater(newParameter, swaggerParameters);
            return;
        }

        if (newParameter.getExplode() != null && newParameter.getExplode() && "query".equals(newParameter.getIn()) && !parameterType.isIterable()) {
            Schema explodedSchema = resolveSchema(openAPI, parameter, parameterType, context, consumesMediaTypes, null, null);
            if (explodedSchema != null) {
                if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null && StringUtils.isNotEmpty(explodedSchema.get$ref())) {
                    explodedSchema = openAPI.getComponents().getSchemas().get(explodedSchema.get$ref().substring(Components.COMPONENTS_SCHEMAS_REF.length()));
                }
                if (CollectionUtils.isNotEmpty(explodedSchema.getProperties())) {
                    Map<String, Schema> props = explodedSchema.getProperties();
                    for (Map.Entry<String, Schema> entry : props.entrySet()) {
                        Parameter unwrappedParameter = new QueryParameter();
                        if (CollectionUtils.isNotEmpty(explodedSchema.getRequired()) && explodedSchema.getRequired().contains(entry.getKey())) {
                            unwrappedParameter.setRequired(true);
                        }
                        unwrappedParameter.setName(entry.getKey());
                        unwrappedParameter.setSchema(entry.getValue());
                        addSwaggerParamater(unwrappedParameter, swaggerParameters);
                    }
                }
            }
        } else {

            if (StringUtils.isEmpty(newParameter.getName())) {
                newParameter.setName(parameter.getName());
            }

            if (newParameter.getRequired() == null && !isNullable(parameter)) {
                newParameter.setRequired(true);
            }
            if (javadocDescription != null && StringUtils.isEmpty(newParameter.getDescription())) {
                CharSequence desc = javadocDescription.getParameters().get(parameter.getName());
                if (desc != null) {
                    newParameter.setDescription(desc.toString());
                }
            }

            addSwaggerParamater(newParameter, swaggerParameters);

            Schema schema = newParameter.getSchema();
            if (schema == null) {
                schema = resolveSchema(openAPI, parameter, parameterType, context, consumesMediaTypes, null, null);
            }

            if (schema != null) {
                schema = bindSchemaForElement(context, parameter, parameterType, schema);
                newParameter.setSchema(schema);
            }
        }
    }

    private void addSwaggerParamater(Parameter newParameter, List<Parameter> swaggerParameters) {
        if (newParameter.get$ref() != null) {
            swaggerParameters.add(newParameter);
            return;
        }
        for (Parameter swaggerParameter : swaggerParameters) {
            if (newParameter.getName().equals(swaggerParameter.getName())) {
                return;
            }
        }
        swaggerParameters.add(newParameter);
    }

    private void processBodyParameter(VisitorContext context, OpenAPI openAPI, JavadocDescription javadocDescription,
                                      MediaType mediaType, Schema schema, TypedElement parameter) {
        Schema propertySchema = resolveSchema(openAPI, parameter, parameter.getType(), context,
            Collections.singletonList(mediaType), null, null);
        if (propertySchema != null) {

            Optional<String> description = parameter.getValue(io.swagger.v3.oas.annotations.Parameter.class, "description", String.class);
            description.ifPresent(propertySchema::setDescription);
            processSchemaProperty(context, parameter, parameter.getType(), null, schema, propertySchema);
            if (isNullable(parameter)) {
                // Keep null if not
                propertySchema.setNullable(true);
            }
            if (javadocDescription != null && StringUtils.isEmpty(propertySchema.getDescription())) {
                String doc = javadocDescription.getParameters().get(parameter.getName());
                if (doc != null) {
                    propertySchema.setDescription(doc);
                }
            }
        }
    }

    private Parameter processMethodParameterAnnotation(VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation,
                                                       boolean permitsRequestBody,
                                                       Map<String, UriMatchVariable> pathVariables, TypedElement parameter,
                                                       List<TypedElement> extraBodyParameters,
                                                       HttpMethod httpMethod,
                                                       List<UriMatchTemplate> matchTemplates) {
        Parameter newParameter = null;
        String parameterName = parameter.getName();
        if (!parameter.hasStereotype(Bindable.class) && pathVariables.containsKey(parameterName)) {
            UriMatchVariable urlVar = pathVariables.get(parameterName);
            newParameter = urlVar.isQuery() ? new QueryParameter() : new PathParameter();
            newParameter.setName(parameterName);
            final boolean exploded = urlVar.isExploded();
            if (exploded) {
                newParameter.setExplode(exploded);
            }
        } else if (parameter.isAnnotationPresent(PathVariable.class)) {
            String paramName = parameter.getValue(PathVariable.class, String.class).orElse(parameterName);
            UriMatchVariable variable = pathVariables.get(paramName);
            if (variable == null) {
                context.warn("Path variable name: '" + paramName + "' not found in path, operation: " + swaggerOperation.getOperationId(), parameter);
                return null;
            }
            newParameter = new PathParameter();
            newParameter.setName(paramName);
            final boolean exploded = variable.isExploded();
            if (exploded) {
                newParameter.setExplode(exploded);
            }
        } else if (parameter.isAnnotationPresent(Header.class)) {
            String headerName = parameter.getValue(Header.class, "name", String.class).orElse(parameter
                .getValue(Header.class, String.class).orElseGet(() -> NameUtils.hyphenate(parameterName)));
            // Header parameter named "Authorization" are ignored. Use the `securitySchemes` and `security` sections instead to define authorization
            // Header parameter named "Content-Type" are ignored. The values for the "Content-Type" header are defined by `request.body.content.<media-type>`
            // Header parameter named "Accept" are ignored. The values for the "Accept" header are defined by `responses.<code>.content.<media-type>`
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)
                || HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(headerName)
                || HttpHeaders.ACCEPT.equalsIgnoreCase(headerName)
            ) {
                return null;
            }
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
        } else if (parameter.isAnnotationPresent(Part.class) && permitsRequestBody) {
            extraBodyParameters.add(parameter);
        } else if (parameter.hasAnnotation("io.micronaut.management.endpoint.annotation.Selector")) {
            newParameter = new PathParameter();
            newParameter.setName(parameterName);
        } else if (hasNoBindingAnnotationOrType(parameter)) {
            AnnotationValue<io.swagger.v3.oas.annotations.Parameter> parameterAnnotation = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            // Skip recognizing parameter if it's manually defined by "in"
            if (parameterAnnotation == null || !parameterAnnotation.booleanValue("hidden").orElse(false)
                && parameterAnnotation.stringValue("in").isEmpty()) {
                if (permitsRequestBody) {
                    extraBodyParameters.add(parameter);
                } else {

                    UriMatchVariable pathVariable = pathVariables.get(parameterName);
                    boolean isExploded = false;
                    // check if this parameter is optional path variable
                    if (pathVariable == null) {
                        for (UriMatchTemplate matchTemplate : matchTemplates) {
                            for (UriMatchVariable variable : matchTemplate.getVariables()) {
                                if (variable.getName().equals(parameterName)) {
                                    isExploded = variable.isExploded();
                                    if (variable.isOptional() && !variable.isQuery() && !isExploded) {
                                        return null;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (pathVariable != null && !pathVariable.isOptional() && !pathVariable.isQuery()) {
                        newParameter = new PathParameter();
                        newParameter.setName(parameterName);
                        if (pathVariable.isExploded()) {
                            newParameter.setExplode(true);
                        }
                    }

                    if (newParameter == null) {
                        if (httpMethod == HttpMethod.GET) {
                            // default to QueryValue -
                            // https://github.com/micronaut-projects/micronaut-openapi/issues/130
                            newParameter = new QueryParameter();
                            newParameter.setName(parameterName);
                        }
                    }
                    if (newParameter != null && isExploded) {
                        newParameter.setExplode(true);
                    }
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
                Utils.normalizeEnumValues(paramValues, Collections.singletonMap("in", ParameterIn.class));
                if (parameter.isAnnotationPresent(Header.class)) {
                    paramValues.put("in", ParameterIn.HEADER.toString());
                } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                    paramValues.put("in", ParameterIn.COOKIE.toString());
                } else if (parameter.isAnnotationPresent(QueryValue.class)) {
                    paramValues.put("in", ParameterIn.QUERY.toString());
                }
                processExplode(paramAnn, paramValues);

                JsonNode jsonNode = ConvertUtils.getJsonMapper().valueToTree(paramValues);

                if (newParameter == null) {
                    try {
                        newParameter = ConvertUtils.treeToValue(jsonNode, Parameter.class, context);
                        if (jsonNode.has("schema")) {
                            JsonNode schemaNode = jsonNode.get("schema");
                            if (schemaNode.has("$ref")) {
                                if (newParameter == null) {
                                    newParameter = new Parameter();
                                }
                                newParameter.schema(new Schema().$ref(schemaNode.get("$ref").asText()));
                            }
                        }
                    } catch (Exception e) {
                        context.warn("Error reading Swagger Parameter for element [" + parameter + "]: " + e.getMessage(), parameter);
                    }
                } else {
                    try {
                        Parameter v = ConvertUtils.treeToValue(jsonNode, Parameter.class, context);
                        if (v == null) {
                            Map<CharSequence, Object> target = ConvertUtils.getConvertJsonMapper().convertValue(newParameter, MAP_TYPE);
                            for (CharSequence name : paramValues.keySet()) {
                                Object o = paramValues.get(name.toString());
                                if (o != null) {
                                    target.put(name.toString(), o);
                                }
                            }
                            newParameter = ConvertUtils.getConvertJsonMapper().convertValue(target, Parameter.class);
                        } else {
                            // horrible hack because Swagger
                            // ParameterDeserializer breaks updating
                            // existing objects
                            BeanMap<Parameter> beanMap = BeanMap.of(v);
                            Map<CharSequence, Object> target = ConvertUtils.getConvertJsonMapper().convertValue(newParameter, MAP_TYPE);
                            for (CharSequence name : beanMap.keySet()) {
                                Object o = beanMap.get(name.toString());
                                if (o != null) {
                                    target.put(name.toString(), o);
                                }
                            }
                            newParameter = ConvertUtils.getConvertJsonMapper().convertValue(target, Parameter.class);
                        }
                    } catch (IOException e) {
                        context.warn("Error reading Swagger Parameter for element [" + parameter + "]: " + e.getMessage(), parameter);
                    }
                }

                if (newParameter != null && newParameter.get$ref() != null) {
                    return newParameter;
                }

                if (newParameter != null) {
                    final Schema parameterSchema = newParameter.getSchema();
                    if (paramAnn.contains("schema") && parameterSchema != null) {
                        paramAnn.get("schema", AnnotationValue.class)
                                .ifPresent(schemaAnn -> bindSchemaAnnotationValue(context, parameter, parameterSchema, schemaAnn));
                    }
                }
            }
        }

        if (newParameter != null && isNullable(parameter)) {
            newParameter.setRequired(null);
        }

        return newParameter;
    }

    private void processBody(VisitorContext context, OpenAPI openAPI,
                             io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
                             boolean permitsRequestBody, List<MediaType> consumesMediaTypes, TypedElement parameter,
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
        if (requestBody.getRequired() == null && !isNullable(parameterType)) {
            requestBody.setRequired(true);
        }

        final Content content = buildContent(parameter, parameterType, consumesMediaTypes, openAPI, context);
        if (requestBody.getContent() == null) {
            requestBody.setContent(content);
        } else {
            final Content currentContent = requestBody.getContent();
            for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry : content.entrySet()) {
                io.swagger.v3.oas.models.media.MediaType mediaType = entry.getValue();
                io.swagger.v3.oas.models.media.MediaType existedMediaType = currentContent.get(entry.getKey());
                if (existedMediaType == null) {
                    currentContent.put(entry.getKey(), mediaType);
                    continue;
                }
                if (existedMediaType.getSchema() == null) {
                    existedMediaType.setSchema(mediaType.getSchema());
                }
                if (existedMediaType.getEncoding() == null) {
                    existedMediaType.setEncoding(mediaType.getEncoding());
                }
                if (existedMediaType.getExtensions() == null) {
                    existedMediaType.setExtensions(mediaType.getExtensions());
                }
                if (existedMediaType.getExamples() == null) {
                    existedMediaType.setExamples(mediaType.getExamples());
                }
                if (existedMediaType.getExample() == null && mediaType.getExampleSetFlag()) {
                    existedMediaType.setExample(mediaType.getExample());
                }
            }
        }
    }

    private void processRequestBean(VisitorContext context, OpenAPI openAPI,
                                    io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
                                    boolean permitsRequestBody, Map<String, UriMatchVariable> pathVariables, List<MediaType> consumesMediaTypes,
                                    List<Parameter> swaggerParameters, TypedElement parameter,
                                    List<TypedElement> extraBodyParameters,
                                    HttpMethod httpMethod,
                                    List<UriMatchTemplate> matchTemplates,
                                    List<PathItem> pathItems) {
        for (FieldElement field : parameter.getType().getFields()) {
            if (field.isStatic()) {
                continue;
            }
            processParameter(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables,
                consumesMediaTypes, swaggerParameters, field, extraBodyParameters, httpMethod, matchTemplates, pathItems);
        }
    }

    private void readResponse(MethodElement element, VisitorContext context, OpenAPI openAPI,
                              io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription) {

        boolean withMethodResponses = element.hasDeclaredAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponse.class)
            || element.hasDeclaredAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponse.class);

        HttpStatus methodResponseStatus = element.enumValue(Status.class, HttpStatus.class).orElse(HttpStatus.OK);
        String responseCode = String.valueOf(methodResponseStatus.getCode());
        ApiResponses responses = swaggerOperation.getResponses();
        ApiResponse response = null;

        if (responses == null) {
            responses = new ApiResponses();
            swaggerOperation.setResponses(responses);
        } else {
            ApiResponse defaultResponse = responses.remove("default");
            response = responses.get(responseCode);
            if (response == null && defaultResponse != null) {
                response = defaultResponse;
                responses.put(responseCode, response);
            }
        }
        if (response == null && !withMethodResponses) {
            response = new ApiResponse();
            if (javadocDescription == null || StringUtils.isEmpty(javadocDescription.getReturnDescription())) {
                response.setDescription(swaggerOperation.getOperationId() + " " + responseCode + " response");
            } else {
                response.setDescription(javadocDescription.getReturnDescription());
            }
            addResponseContent(element, context, openAPI, response);
            responses.put(responseCode, response);
        } else if (response != null && response.getContent() == null) {
            addResponseContent(element, context, openAPI, response);
        }
    }

    private void addResponseContent(MethodElement element, VisitorContext context, OpenAPI openAPI, ApiResponse response) {
        ClassElement returnType = returnType(element, context);
        if (returnType != null && !returnType.getCanonicalName().equals(Void.class.getName())) {
            List<MediaType> producesMediaTypes = producesMediaTypes(element);
            Content content;
            if (producesMediaTypes.isEmpty()) {
                content = buildContent(element, returnType, DEFAULT_MEDIA_TYPES, openAPI, context);
            } else {
                content = buildContent(element, returnType, producesMediaTypes, openAPI, context);
            }
            response.setContent(content);
        }
    }

    private ClassElement returnType(MethodElement element, VisitorContext context) {
        ClassElement returnType = element.getGenericReturnType();

        if (ElementUtils.isVoid(returnType) || ElementUtils.isReactiveAndVoid(returnType)) {
            returnType = null;
        } else if (isResponseType(returnType)) {
            returnType = returnType.getFirstTypeArgument().orElse(returnType);
        } else if (isSingleResponseType(returnType)) {
            returnType = returnType.getFirstTypeArgument().get();
            returnType = returnType.getFirstTypeArgument().orElse(returnType);
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
            String summary = descr.substring(0, descr.indexOf('.') + 1);
            if (summary.length() > MAX_SUMMARY_LENGTH) {
                summary = summary.substring(0, MAX_SUMMARY_LENGTH) + THREE_DOTS;
            }
            swaggerOperation.setSummary(summary);
        }
        JavadocDescription javadocDescription = element.getDocumentation()
            .map(Utils.getJavadocParser()::parse)
            .orElse(null);

        if (javadocDescription != null) {
            if (StringUtils.isEmpty(swaggerOperation.getDescription()) && StringUtils.hasText(javadocDescription.getMethodDescription())) {
                swaggerOperation.setDescription(javadocDescription.getMethodDescription());
            }
            if (StringUtils.isEmpty(swaggerOperation.getSummary()) && StringUtils.hasText(javadocDescription.getMethodSummary())) {
                swaggerOperation.setSummary(javadocDescription.getMethodSummary());
            }
        }
        return javadocDescription;
    }

    private Map<PathItem, io.swagger.v3.oas.models.Operation> readOperations(String path, HttpMethod httpMethod, List<PathItem> pathItems, MethodElement element, VisitorContext context) {
        Map<PathItem, io.swagger.v3.oas.models.Operation> swaggerOperations = new HashMap<>(pathItems.size());
        final Optional<AnnotationValue<Operation>> operationAnnotation = element.findAnnotation(Operation.class);
        for (PathItem pathItem : pathItems) {
            io.swagger.v3.oas.models.Operation swaggerOperation = operationAnnotation
                .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.Operation.class))
                .orElse(new io.swagger.v3.oas.models.Operation());

            if (CollectionUtils.isNotEmpty(swaggerOperation.getParameters())) {
                swaggerOperation.getParameters().removeIf(Objects::isNull);
            }

            ParameterElement[] methodParams = element.getParameters();
            if (ArrayUtils.isNotEmpty(methodParams) && operationAnnotation.isPresent()) {
                List<AnnotationValue<io.swagger.v3.oas.annotations.Parameter>> params = operationAnnotation.get().getAnnotations("parameters", io.swagger.v3.oas.annotations.Parameter.class);
                if (CollectionUtils.isNotEmpty(params)) {
                    for (ParameterElement methodParam : methodParams) {
                        AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn = null;
                        for (AnnotationValue<io.swagger.v3.oas.annotations.Parameter> param : params) {
                            String paramName = param.stringValue("name").orElse(null);
                            if (methodParam.getName().equals(paramName)) {
                                paramAnn = param;
                                break;
                            }
                        }

                        Parameter swaggerParam = null;
                        if (paramAnn != null && !paramAnn.booleanValue("hidden").orElse(false)) {
                            String paramName = paramAnn.stringValue("name").orElse(null);
                            if (paramName != null) {
                                if (CollectionUtils.isNotEmpty(swaggerOperation.getParameters())) {
                                    for (Parameter createdParameter : swaggerOperation.getParameters()) {
                                        if (createdParameter.getName().equals(paramName)) {
                                            swaggerParam = createdParameter;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (swaggerParam == null) {
                                if (swaggerOperation.getParameters() == null) {
                                    swaggerOperation.setParameters(new ArrayList<>());
                                }
                                swaggerParam = new Parameter();
                                swaggerOperation.getParameters().add(swaggerParam);
                            }
                            if (paramName != null) {
                                swaggerParam.setName(paramName);
                            }
                            paramAnn.stringValue("description").ifPresent(swaggerParam::setDescription);
                            Optional<Boolean> required = paramAnn.booleanValue("required");
                            if (required.isPresent()) {
                                swaggerParam.setRequired(required.get() ? true : null);
                            }
                            Optional<Boolean> deprecated = paramAnn.booleanValue("deprecated");
                            if (deprecated.isPresent()) {
                                swaggerParam.setDeprecated(deprecated.get() ? true : null);
                            }
                            Optional<Boolean> allowEmptyValue = paramAnn.booleanValue("allowEmptyValue");
                            if (allowEmptyValue.isPresent()) {
                                swaggerParam.setAllowEmptyValue(allowEmptyValue.get() ? true : null);
                            }
                            Optional<Boolean> allowReserved = paramAnn.booleanValue("allowReserved");
                            if (allowReserved.isPresent()) {
                                swaggerParam.setAllowReserved(allowReserved.get() ? true : null);
                            }
                            paramAnn.stringValue("example").ifPresent(swaggerParam::setExample);
                            Optional<ParameterStyle> style = paramAnn.get("style", ParameterStyle.class);
                            if (style.isPresent()) {
                                swaggerParam.setStyle(paramStyle(style.get()));
                            }
                            paramAnn.stringValue("ref").ifPresent(swaggerParam::set$ref);
                            Optional<ParameterIn> in = paramAnn.get("in", ParameterIn.class);
                            if (in.isPresent()) {
                                if (in.get() == ParameterIn.DEFAULT) {
                                    swaggerParam.setIn(calcIn(path, httpMethod, methodParam));
                                } else {
                                    swaggerParam.setIn(in.get().toString());
                                }
                            }
                        }
                        if (swaggerParam != null && StringUtils.isEmpty(swaggerParam.getIn())) {
                            swaggerParam.setIn(calcIn(path, httpMethod, methodParam));
                        }
                    }
                }
            }

            String prefix;
            String suffix;
            boolean addAlways;
            AnnotationValue<OpenAPIDecorator> apiDecorator = element.getDeclaredAnnotation(OpenAPIDecorator.class);
            if (apiDecorator != null) {
                prefix = apiDecorator.stringValue().orElse("");
                suffix = apiDecorator.stringValue("opIdSuffix").orElse("");
                addAlways = apiDecorator.booleanValue("addAlways").orElse(true);
            } else {
                prefix = context.get(CONTEXT_CHILD_OP_ID_PREFIX, String.class).orElse("");
                suffix = context.get(CONTEXT_CHILD_OP_ID_SUFFIX, String.class).orElse("");
                addAlways = context.get(CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, Boolean.class).orElse(true);
            }

            if (StringUtils.isEmpty(swaggerOperation.getOperationId())) {
                swaggerOperation.setOperationId(prefix + element.getName() + suffix);
            } else if (addAlways) {
                swaggerOperation.setOperationId(prefix + swaggerOperation.getOperationId() + suffix);
            }

            if (swaggerOperation.getDescription() != null && swaggerOperation.getDescription().isEmpty()) {
                swaggerOperation.setDescription(null);
            }
            swaggerOperations.put(pathItem, swaggerOperation);
        }
        return swaggerOperations;
    }

    private String calcIn(String path, HttpMethod httpMethod, ParameterElement methodParam) {
        String paramName = methodParam.getName();
        Set<String> paramAnnNames = methodParam.getAnnotationNames();
        if (CollectionUtils.isNotEmpty(paramAnnNames)) {
            if (paramAnnNames.contains(QueryValue.class.getName())) {
                return ParameterIn.QUERY.toString();
            } else if (paramAnnNames.contains(PathVariable.class.getName())) {
                return ParameterIn.PATH.toString();
            } else if (paramAnnNames.contains(Header.class.getName())) {
                return ParameterIn.HEADER.toString();
            } else if (paramAnnNames.contains(CookieValue.class.getName())) {
                return ParameterIn.COOKIE.toString();
            }
        }
        if (httpMethod == HttpMethod.GET) {
            if (path.contains("{" + paramName + "}")) {
                return ParameterIn.PATH.toString();
            } else {
                return ParameterIn.QUERY.toString();
            }
        } else {
            if (path.contains("{" + paramName + "}")) {
                return ParameterIn.PATH.toString();
            }
        }

        return null;
    }

    private Parameter.StyleEnum paramStyle(ParameterStyle paramAnnStyle) {
        if (paramAnnStyle == null) {
            return null;
        }
        return switch (paramAnnStyle) {
            case MATRIX -> Parameter.StyleEnum.MATRIX;
            case LABEL -> Parameter.StyleEnum.LABEL;
            case FORM -> Parameter.StyleEnum.FORM;
            case SPACEDELIMITED -> Parameter.StyleEnum.SPACEDELIMITED;
            case PIPEDELIMITED -> Parameter.StyleEnum.PIPEDELIMITED;
            case DEEPOBJECT -> Parameter.StyleEnum.DEEPOBJECT;
            case SIMPLE -> Parameter.StyleEnum.SIMPLE;
            case DEFAULT -> null;
        };
    }

    private io.swagger.v3.oas.models.ExternalDocumentation readExternalDocs(MethodElement element, VisitorContext context) {
        final Optional<AnnotationValue<ExternalDocumentation>> externalDocsAnn = element.findAnnotation(ExternalDocumentation.class);
        io.swagger.v3.oas.models.ExternalDocumentation externalDocs = externalDocsAnn
            .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.ExternalDocumentation.class))
            .orElse(null);

        return externalDocs;
    }

    private void readSecurityRequirements(MethodElement element, String path, io.swagger.v3.oas.models.Operation operation, VisitorContext context) {
        List<SecurityRequirement> securityRequirements = methodSecurityRequirements(element, context);
        if (CollectionUtils.isNotEmpty(securityRequirements)) {
            for (SecurityRequirement securityItem : securityRequirements) {
                operation.addSecurityItem(securityItem);
            }
            return;
        }

        processMicronautSecurityConfig(element, path, operation, context);
    }

    private void processMicronautSecurityConfig(MethodElement element, String path, io.swagger.v3.oas.models.Operation operation, VisitorContext context) {

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

        OpenAPI openAPI = Utils.resolveOpenApi(context);
        Components components = openAPI.getComponents();

        String securitySchemeName;
        if (components != null && CollectionUtils.isNotEmpty(components.getSecuritySchemes())) {
            securitySchemeName = components.getSecuritySchemes().keySet().iterator().next();
        } else {
            if (components == null) {
                components = new Components();
                openAPI.setComponents(components);
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

        AnnotationValue<Annotation> classLevelSecured = element.getOwningType().getAnnotation("io.micronaut.security.annotation.Secured");
        AnnotationValue<Annotation> methodLevelSecured = element.getAnnotation("io.micronaut.security.annotation.Secured");
        List<String> access = null;
        if (methodLevelSecured != null) {
            access = methodLevelSecured.getValue(Argument.LIST_OF_STRING).orElse(null);
        } else if (classLevelSecured != null) {
            access = classLevelSecured.getValue(Argument.LIST_OF_STRING).orElse(null);
        }
        processSecurityAccess(securitySchemeName, access, operation);

        List<InterceptUrlMapPattern> securityRules = securityProperties.getInterceptUrlMapPatterns();
        if (CollectionUtils.isNotEmpty(securityRules)) {
            HttpMethod httpMethod = httpMethod(element);
            for (InterceptUrlMapPattern securityRule : securityRules) {
                if (PathMatcher.ANT.matches(securityRule.getPattern(), path)
                    && (httpMethod == null || securityRule.getHttpMethod().isEmpty() || httpMethod == securityRule.getHttpMethod().get())) {

                    processSecurityAccess(securitySchemeName, securityRule.getAccess(), operation);
                }
            }
        }
    }

    private void processSecurityAccess(String securitySchemeName, List<String> access, io.swagger.v3.oas.models.Operation operation) {
        if (CollectionUtils.isEmpty(access)) {
            return;
        }
        String firstAccessItem = access.get(0);
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
                Set<String> finalAccess = new HashSet<>(existedSecList);
                finalAccess.addAll(access);
                existedSecurityRequirement.put(securitySchemeName, new ArrayList<>(finalAccess));
            }
        } else {
            SecurityRequirement securityRequirement = new SecurityRequirement();
            securityRequirement.put(securitySchemeName, access);
            operation.addSecurityItem(securityRequirement);
        }
    }

    private void processExplode(AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn, Map<CharSequence, Object> paramValues) {
        Optional<Explode> explode = paramAnn.enumValue("explode", Explode.class);
        if (explode.isPresent()) {
            Explode ex = explode.get();
            switch (ex) {
                case TRUE -> paramValues.put("explode", Boolean.TRUE);
                case FALSE -> paramValues.put("explode", Boolean.FALSE);
                default -> {
                    String in = (String) paramValues.get("in");
                    if (StringUtils.isEmpty(in)) {
                        in = "DEFAULT";
                    }
                    switch (ParameterIn.valueOf(in.toUpperCase(Locale.US))) {
                        case COOKIE, QUERY -> paramValues.put("explode", Boolean.TRUE);
                        default -> paramValues.put("explode", Boolean.FALSE);
                    }
                }
            }
        }
    }

    private boolean ignoreParameter(TypedElement parameter) {

        AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn = parameter.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        boolean isHidden = schemaAnn != null && schemaAnn.booleanValue("hidden").orElse(false);

        return isHidden
            || parameter.isAnnotationPresent(Hidden.class)
            || parameter.isAnnotationPresent(JsonIgnore.class)
            || parameter.booleanValue(io.swagger.v3.oas.annotations.Parameter.class, "hidden")
                .orElse(false)
            || isAnnotationPresent(parameter, "io.micronaut.session.annotation.SessionValue")
            || isIgnoredParameterType(parameter.getType());
    }

    private boolean isIgnoredParameterType(ClassElement parameterType) {
        return parameterType == null ||
            parameterType.isAssignable(Principal.class) ||
            parameterType.isAssignable("io.micronaut.session.Session") ||
            parameterType.isAssignable("io.micronaut.security.authentication.Authentication") ||
            parameterType.isAssignable("io.micronaut.http.HttpHeaders") ||
            parameterType.isAssignable("kotlin.coroutines.Continuation") ||
            parameterType.isAssignable(HttpRequest.class) ||
            parameterType.isAssignable("io.micronaut.http.BasicAuth");
    }

    private boolean isResponseType(ClassElement returnType) {
        return returnType.isAssignable(HttpResponse.class)
            || returnType.isAssignable("org.springframework.http.HttpEntity");
    }

    private boolean isSingleResponseType(ClassElement returnType) {
        return (returnType.isAssignable("io.reactivex.Single")
            || returnType.isAssignable("io.reactivex.rxjava3.core.Single")
            || returnType.isAssignable("org.reactivestreams.Publisher"))
            && returnType.getFirstTypeArgument().isPresent()
            && isResponseType(returnType.getFirstTypeArgument().get());
    }

    private io.swagger.v3.oas.models.Operation getOperationOnPathItem(PathItem pathItem, HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> pathItem.getGet();
            case POST -> pathItem.getPost();
            case PUT -> pathItem.getPut();
            case PATCH -> pathItem.getPatch();
            case DELETE -> pathItem.getDelete();
            case HEAD -> pathItem.getHead();
            case OPTIONS -> pathItem.getOptions();
            case TRACE -> pathItem.getTrace();
            default -> null;
        };
    }

    private io.swagger.v3.oas.models.Operation setOperationOnPathItem(PathItem pathItem, io.swagger.v3.oas.models.Operation swaggerOperation, HttpMethod httpMethod) {
        io.swagger.v3.oas.models.Operation operation = swaggerOperation;
        switch (httpMethod) {
            case GET -> {
                if (pathItem.getGet() != null) {
                    operation = pathItem.getGet();
                } else {
                    pathItem.get(swaggerOperation);
                }
            }
            case POST -> {
                if (pathItem.getPost() != null) {
                    operation = pathItem.getPost();
                } else {
                    pathItem.post(swaggerOperation);
                }
            }
            case PUT -> {
                if (pathItem.getPut() != null) {
                    operation = pathItem.getPut();
                } else {
                    pathItem.put(swaggerOperation);
                }
            }
            case PATCH -> {
                if (pathItem.getPatch() != null) {
                    operation = pathItem.getPatch();
                } else {
                    pathItem.patch(swaggerOperation);
                }
            }
            case DELETE -> {
                if (pathItem.getDelete() != null) {
                    operation = pathItem.getDelete();
                } else {
                    pathItem.delete(swaggerOperation);
                }
            }
            case HEAD -> {
                if (pathItem.getHead() != null) {
                    operation = pathItem.getHead();
                } else {
                    pathItem.head(swaggerOperation);
                }
            }
            case OPTIONS -> {
                if (pathItem.getOptions() != null) {
                    operation = pathItem.getOptions();
                } else {
                    pathItem.options(swaggerOperation);
                }
            }
            case TRACE -> {
                if (pathItem.getTrace() != null) {
                    operation = pathItem.getTrace();
                } else {
                    pathItem.trace(swaggerOperation);
                }
            }
            default -> {
            }
        }
        return operation;
    }

    private void readApiResponses(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> methodResponseAnnotations = element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        processResponses(swaggerOperation, methodResponseAnnotations, element, context);

        List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> classResponseAnnotations = element.getDeclaringType().getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        processResponses(swaggerOperation, classResponseAnnotations, element, context);
    }

    private void processResponses(io.swagger.v3.oas.models.Operation operation, List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> responseAnnotations, MethodElement element, VisitorContext context) {
        ApiResponses apiResponses = operation.getResponses();
        if (apiResponses == null) {
            apiResponses = new ApiResponses();
            operation.setResponses(apiResponses);
        }
        if (CollectionUtils.isNotEmpty(responseAnnotations)) {
            for (AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse> response : responseAnnotations) {
                String responseCode = response.stringValue("responseCode").orElse("default");
                if (apiResponses.containsKey(responseCode)) {
                    continue;
                }
                Optional<ApiResponse> newResponse = toValue(response.getValues(), context, ApiResponse.class);
                if (newResponse.isPresent()) {
                    ApiResponse newApiResponse = newResponse.get();
                    if (response.booleanValue("useReturnTypeSchema").orElse(false) && element != null) {
                        addResponseContent(element, context, Utils.resolveOpenApi(context), newApiResponse);
                    } else {

                        List<MediaType> producesMediaTypes = producesMediaTypes(element);

                        io.swagger.v3.oas.annotations.media.Content[] contentAnns = response.get("content", io.swagger.v3.oas.annotations.media.Content[].class).orElse(null);
                        List<String> mediaTypes = new ArrayList<>();
                        if (ArrayUtils.isNotEmpty(contentAnns)) {
                            for (io.swagger.v3.oas.annotations.media.Content contentAnn : contentAnns) {
                                if (StringUtils.isNotEmpty(contentAnn.mediaType())) {
                                    mediaTypes.add(contentAnn.mediaType());
                                } else {
                                    mediaTypes.add(MediaType.APPLICATION_JSON);
                                }
                            }
                        }
                        Content newContent = newApiResponse.getContent();
                        if (newContent != null) {
                            io.swagger.v3.oas.models.media.MediaType defaultMediaType = newContent.get(MediaType.APPLICATION_JSON);
                            Content contentFromProduces = new Content();
                            for (String mt : mediaTypes) {
                                if (mt.equals(MediaType.APPLICATION_JSON)) {
                                    for (MediaType mediaType : producesMediaTypes) {
                                        contentFromProduces.put(mediaType.toString(), defaultMediaType);
                                    }
                                } else {
                                    contentFromProduces.put(mt, newContent.get(mt));
                                }
                            }
                            newApiResponse.setContent(contentFromProduces);
                        }
                    }
                    try {
                        if (StringUtils.isEmpty(newApiResponse.getDescription())) {
                            newApiResponse.setDescription(responseCode == null || responseCode.equals("default") ? "OK response" : HttpStatus.getDefaultReason(Integer.parseInt(responseCode)));
                        }
                    } catch (Exception e) {
                        newApiResponse.setDescription("Response " + responseCode);
                    }
                    apiResponses.put(responseCode, newApiResponse);
                }
            }
            operation.setResponses(apiResponses);
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
        AnnotationValue<Callbacks> callbacksAnnotation = element.getAnnotation(Callbacks.class);
        List<AnnotationValue<Callback>> callbackAnnotations;
        if (callbacksAnnotation != null) {
            callbackAnnotations = callbacksAnnotation.getAnnotations("value");
        } else {
            callbackAnnotations = element.getAnnotationValuesByType(Callback.class);
        }
        if (CollectionUtils.isEmpty(callbackAnnotations)) {
            return;
        }
        for (AnnotationValue<Callback> callbackAnn : callbackAnnotations) {
            final Optional<String> name = callbackAnn.stringValue("name");
            if (name.isEmpty()) {
                continue;
            }
            String callbackName = name.get();
            final Optional<String> ref = callbackAnn.stringValue("ref");
            if (ref.isPresent()) {
                String refCallback = ref.get().substring(COMPONENTS_CALLBACKS_PREFIX.length());
                processCallbackReference(context, swaggerOperation, callbackName, refCallback);
                continue;
            }
            final Optional<String> expr = callbackAnn.stringValue("callbackUrlExpression");
            if (expr.isPresent()) {
                processUrlCallbackExpression(context, swaggerOperation, callbackAnn, callbackName, expr.get());
            } else {
                processCallbackReference(context, swaggerOperation, callbackName, null);
            }
        }
    }

    private void processCallbackReference(VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation,
                                          String callbackName, String refCallback) {
        final Components components = Utils.resolveComponents(Utils.resolveOpenApi(context));
        Map<String, io.swagger.v3.oas.models.callbacks.Callback> callbacks = initCallbacks(swaggerOperation);
        final io.swagger.v3.oas.models.callbacks.Callback callbackRef = new io.swagger.v3.oas.models.callbacks.Callback();
        callbackRef.set$ref(refCallback != null ? refCallback : COMPONENTS_CALLBACKS_PREFIX + callbackName);
        callbacks.put(callbackName, callbackRef);
    }

    private void processUrlCallbackExpression(VisitorContext context,
                                              io.swagger.v3.oas.models.Operation swaggerOperation, AnnotationValue<Callback> callbackAnn,
                                              String callbackName, final String callbackUrl) {
        final List<AnnotationValue<Operation>> operations = callbackAnn.getAnnotations("operation", Operation.class);
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
                operationMethod.ifPresent(httpMethod -> toValue(operation.getValues(), context, io.swagger.v3.oas.models.Operation.class)
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

    private void readTags(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation, List<io.swagger.v3.oas.models.tags.Tag> classTags, OpenAPI openAPI) {
        element.getAnnotationValuesByType(Tag.class)
            .forEach(av -> av.stringValue("name")
                .ifPresent(swaggerOperation::addTagsItem));

        List<io.swagger.v3.oas.models.tags.Tag> copyTags = openAPI.getTags() != null ? new ArrayList<>(openAPI.getTags()) : null;
        List<io.swagger.v3.oas.models.tags.Tag> operationTags = processOpenApiAnnotation(element, context, Tag.class, io.swagger.v3.oas.models.tags.Tag.class, copyTags);
        // find not simple tags (tags with description or other information), such fields need to be described at the openAPI level.
        List<io.swagger.v3.oas.models.tags.Tag> complexTags = null;
        if (CollectionUtils.isNotEmpty(operationTags)) {
            complexTags = new ArrayList<>();
            for (io.swagger.v3.oas.models.tags.Tag operationTag : operationTags) {
                if (StringUtils.hasText(operationTag.getDescription())
                    || CollectionUtils.isNotEmpty(operationTag.getExtensions())
                    || operationTag.getExternalDocs() != null) {
                    complexTags.add(operationTag);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(complexTags)) {
            if (CollectionUtils.isEmpty(openAPI.getTags())) {
                openAPI.setTags(complexTags);
            } else {
                for (io.swagger.v3.oas.models.tags.Tag complexTag : complexTags) {
                    // skip all existed tags
                    boolean alreadyExists = false;
                    for (io.swagger.v3.oas.models.tags.Tag apiTag : openAPI.getTags()) {
                        if (apiTag.getName().equals(complexTag.getName())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        openAPI.getTags().add(complexTag);
                    }
                }
            }
        }

        // only way to get inherited tags
        element.getValues(Tags.class, AnnotationValue.class).forEach((k, v) -> v.stringValue("name").ifPresent(name -> addTagIfNotPresent((String) name, swaggerOperation)));

        classTags.forEach(tag -> addTagIfNotPresent(tag.getName(), swaggerOperation));
        if (CollectionUtils.isNotEmpty(swaggerOperation.getTags())) {
            swaggerOperation.getTags().sort(Comparator.naturalOrder());
        }
    }

    private List<io.swagger.v3.oas.models.tags.Tag> readTags(ClassElement element, VisitorContext context) {
        return readTags(element.getAnnotationValuesByType(Tag.class), context);
    }

    final List<io.swagger.v3.oas.models.tags.Tag> readTags(List<AnnotationValue<Tag>> annotations, VisitorContext context) {
        return annotations.stream()
            .map(av -> toValue(av.getValues(), context, io.swagger.v3.oas.models.tags.Tag.class))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Content buildContent(Element definingElement, ClassElement type, List<MediaType> mediaTypes, OpenAPI openAPI, VisitorContext context) {
        Content content = new Content();
        mediaTypes.forEach(mediaType -> {
            io.swagger.v3.oas.models.media.MediaType mt = new io.swagger.v3.oas.models.media.MediaType();
            mt.setSchema(resolveSchema(openAPI, definingElement, type, context, Collections.singletonList(mediaType), null, null));
            content.addMediaType(mediaType.toString(), mt);
        });
        return content;
    }

}
