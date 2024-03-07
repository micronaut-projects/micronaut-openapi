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

import java.io.IOException;
import java.lang.annotation.Annotation;
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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.PathMatcher;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.CookieValue;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Headers;
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
import io.micronaut.inject.ast.PackageElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.annotation.OpenAPIDecorator;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.EndpointGroupInfo;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.GroupProperties.PackageProperties;
import io.micronaut.openapi.visitor.group.RouterVersioningProperties;
import io.micronaut.openapi.visitor.security.InterceptUrlMapPattern;
import io.micronaut.openapi.visitor.security.SecurityProperties;
import io.micronaut.openapi.visitor.security.SecurityRule;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Webhook;
import io.swagger.v3.oas.annotations.callbacks.Callback;
import io.swagger.v3.oas.annotations.callbacks.Callbacks;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Encoding;
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

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import static io.micronaut.openapi.visitor.ConfigUtils.getGroupsPropertiesMap;
import static io.micronaut.openapi.visitor.ConfigUtils.getRouterVersioningProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getSecurityProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.isJsonViewEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.ElementUtils.isFileUpload;
import static io.micronaut.openapi.visitor.ElementUtils.isIgnoredParameter;
import static io.micronaut.openapi.visitor.ElementUtils.isNotNullable;
import static io.micronaut.openapi.visitor.ElementUtils.isNullable;
import static io.micronaut.openapi.visitor.SchemaUtils.COMPONENTS_CALLBACKS_PREFIX;
import static io.micronaut.openapi.visitor.SchemaUtils.COMPONENTS_SCHEMAS_PREFIX;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.getOperationOnPathItem;
import static io.micronaut.openapi.visitor.SchemaUtils.isIgnoredHeader;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;
import static io.micronaut.openapi.visitor.SchemaUtils.setOperationOnPathItem;
import static io.micronaut.openapi.visitor.SchemaUtils.setSpecVersion;
import static io.micronaut.openapi.visitor.Utils.DEFAULT_MEDIA_TYPES;
import static io.micronaut.openapi.visitor.Utils.getMediaType;
import static io.micronaut.openapi.visitor.Utils.resolveWebhooks;

/**
 * A {@link io.micronaut.inject.visitor.TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
public abstract class AbstractOpenApiEndpointVisitor extends AbstractOpenApiVisitor {

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

    /**
     * Executed when a class is encountered that matches the generic class.
     *
     * @param element The element
     * @param context The visitor context
     */
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
            return;
        }
        if (ignore(element, context)) {
            return;
        }
        incrementVisitedElements(context);
        processSecuritySchemes(element, context);
        processTags(element, context);
        processExternalDocs(element, context);
        ContextUtils.remove(CONTEXT_CHILD_PATH, context);

        if (element.isAnnotationPresent(Controller.class)) {

            element.stringValue(UriMapping.class).ifPresent(url -> ContextUtils.put(CONTEXT_CHILD_PATH, url, context));
            String prefix = "";
            String suffix = "";
            boolean addAlways = true;
            AnnotationValue<OpenAPIDecorator> apiDecorator = element.getDeclaredAnnotation(OpenAPIDecorator.class);
            if (apiDecorator != null) {
                prefix = apiDecorator.stringValue().orElse("");
                suffix = apiDecorator.stringValue("opIdSuffix").orElse("");
                addAlways = apiDecorator.booleanValue("addAlways").orElse(true);
            }
            ContextUtils.put(CONTEXT_CHILD_OP_ID_PREFIX, prefix, context);
            ContextUtils.put(CONTEXT_CHILD_OP_ID_SUFFIX, suffix, context);
            ContextUtils.put(CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, addAlways, context);

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
                ContextUtils.put(IS_PROCESS_PARENT_CLASS, true, context);
                List<MethodElement> methods = element.getEnclosedElements(ElementQuery.ALL_METHODS);
                for (MethodElement method : methods) {
                    visitMethod(method, context);
                }
                ContextUtils.remove(IS_PROCESS_PARENT_CLASS, context);
            }

            ContextUtils.remove(CONTEXT_CHILD_OP_ID_PREFIX, context);
            ContextUtils.remove(CONTEXT_CHILD_OP_ID_SUFFIX, context);
            ContextUtils.remove(CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, context);
        }
        ContextUtils.remove(CONTEXT_CHILD_PATH, context);
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
            .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.ExternalDocumentation.class, null))
            .orElse(null);
    }

    private boolean containsTag(String name, List<io.swagger.v3.oas.models.tags.Tag> tags) {
        for (var tag : tags) {
            if (name.equals(tag.getName())) {
                return true;
            }
        }
        return false;
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
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
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
        JavadocDescription javadocDescription;
        boolean permitsRequestBody = HttpMethod.permitsRequestBody(httpMethod);

        Map<String, List<PathItem>> pathItemsMap = resolvePathItems(context, matchTemplates);
        List<MediaType> consumesMediaTypes = consumesMediaTypes(element);
        List<MediaType> producesMediaTypes = producesMediaTypes(element);

        ClassElement jsonViewClass = null;
        if (isJsonViewEnabled(context)) {
            AnnotationValue<JsonView> jsonViewAnnotation = element.findAnnotation(JsonView.class).orElse(null);
            if (jsonViewAnnotation == null) {
                jsonViewAnnotation = element.getOwningType().findAnnotation(JsonView.class).orElse(null);
            }
            if (jsonViewAnnotation != null) {
                String jsonViewClassName = jsonViewAnnotation.stringValue().orElse(null);
                if (jsonViewClassName != null) {
                    jsonViewClass = ContextUtils.getClassElement(jsonViewClassName, context);
                }
            }
        }

        var webhookValue = element.getAnnotation(Webhook.class);
        var webhookPair = readWebhook(webhookValue, httpMethod, context);
        if (webhookPair != null) {
            resolveWebhooks(openAPI).put(webhookPair.getFirst(), webhookPair.getSecond());
        }

        for (Map.Entry<String, List<PathItem>> pathItemEntry : pathItemsMap.entrySet()) {
            List<PathItem> pathItems = pathItemEntry.getValue();

            final OpenAPI openApi = Utils.resolveOpenApi(context);

            Map<PathItem, io.swagger.v3.oas.models.Operation> swaggerOperations = readOperations(pathItemEntry.getKey(), httpMethod, pathItems, element, context, jsonViewClass);

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

                readApiResponses(element, context, swaggerOperation, jsonViewClass);

                readServers(element, context, swaggerOperation);

                readCallbacks(element, context, swaggerOperation, jsonViewClass);

                javadocDescription = getMethodDescription(element, swaggerOperation);

                if (element.isAnnotationPresent(Deprecated.class)) {
                    swaggerOperation.setDeprecated(true);
                }

                readResponse(element, context, openAPI, swaggerOperation, javadocDescription, jsonViewClass);

                boolean isRequestBodySchemaSet = false;

                if (permitsRequestBody) {
                    Pair<RequestBody, Boolean> requestBodyPair = readSwaggerRequestBody(element, consumesMediaTypes, context);
                    RequestBody requestBody = null;
                    if (requestBodyPair != null) {
                        requestBody = requestBodyPair.getFirst();
                        isRequestBodySchemaSet = requestBodyPair.getSecond();
                    }
                    if (requestBody != null) {
                        RequestBody currentRequestBody = swaggerOperation.getRequestBody();
                        if (currentRequestBody != null) {
                            swaggerOperation.setRequestBody(SchemaUtils.mergeRequestBody(currentRequestBody, requestBody));
                        } else {
                            swaggerOperation.setRequestBody(requestBody);
                        }
                    }
                }

                setOperationOnPathItem(operationEntry.getKey(), httpMethod, swaggerOperation);

                Map<String, UriMatchVariable> pathVariables = new HashMap<>();
                for (UriMatchTemplate matchTemplate : matchTemplates) {
                    for (Map.Entry<String, UriMatchVariable> varEntry : pathVariables(matchTemplate).entrySet()) {
                        if (pathItemEntry.getKey().contains("{" + varEntry.getKey() + '}')) {
                            pathVariables.put(varEntry.getKey(), varEntry.getValue());
                        }
                    }
                    // @Parameters declared at method level take precedence over the declared as method arguments, so we process them first
                    processParameterAnnotationInMethod(element, openAPI, matchTemplate, httpMethod, swaggerOperation, pathVariables, context);
                }

                List<TypedElement> extraBodyParameters = new ArrayList<>();
                for (io.swagger.v3.oas.models.Operation operation : swaggerOperations.values()) {
                    processParameters(element, context, openAPI, operation, javadocDescription, permitsRequestBody, pathVariables, consumesMediaTypes, extraBodyParameters, httpMethod, matchTemplates, pathItems);
                    processExtraBodyParameters(context, httpMethod, openAPI, operation, javadocDescription, isRequestBodySchemaSet, consumesMediaTypes, extraBodyParameters);

                    processMicronautVersionAndGroup(operation, pathItemEntry.getKey(), httpMethod, consumesMediaTypes, producesMediaTypes, element, context);
                }

                if (webhookPair != null) {
                    SchemaUtils.mergeOperations(getOperationOnPathItem(webhookPair.getSecond(), httpMethod), swaggerOperation);
                }
            }
        }
    }

    private void processExtraBodyParameters(VisitorContext context, HttpMethod httpMethod, OpenAPI openAPI,
                                            io.swagger.v3.oas.models.Operation swaggerOperation,
                                            JavadocDescription javadocDescription,
                                            boolean isRequestBodySchemaSet,
                                            List<MediaType> consumesMediaTypes,
                                            List<TypedElement> extraBodyParameters) {
        RequestBody requestBody = swaggerOperation.getRequestBody();
        if (HttpMethod.permitsRequestBody(httpMethod) && !extraBodyParameters.isEmpty()) {
            if (requestBody == null) {
                requestBody = new RequestBody();
                var content = new Content();
                requestBody.setContent(content);
                requestBody.setRequired(true);
                swaggerOperation.setRequestBody(requestBody);

                consumesMediaTypes = CollectionUtils.isEmpty(consumesMediaTypes) ? DEFAULT_MEDIA_TYPES : consumesMediaTypes;
                consumesMediaTypes.forEach(mediaType -> {
                    var mt = new io.swagger.v3.oas.models.media.MediaType();
                    var schema = setSpecVersion(new Schema<>());
                    schema.setType(TYPE_OBJECT);
                    mt.setSchema(schema);
                    content.addMediaType(mediaType.toString(), mt);
                });
            }
        }
        if (requestBody != null && requestBody.getContent() != null && !extraBodyParameters.isEmpty()) {
            requestBody.getContent().forEach((mediaTypeName, mediaType) -> {
                var schema = mediaType.getSchema();
                if (schema == null) {
                    schema = setSpecVersion(new Schema<>());
                    mediaType.setSchema(schema);
                }
                if (schema.get$ref() != null) {
                    if (isRequestBodySchemaSet) {
                        schema = openAPI.getComponents().getSchemas().get(schema.get$ref().substring(COMPONENTS_SCHEMAS_PREFIX.length()));
                    } else {
                        var composedSchema = setSpecVersion(new ComposedSchema());
                        var extraBodyParametersSchema = setSpecVersion(new Schema<>());
                        // Composition of existing + a new schema where extra body parameters are going to be added
                        composedSchema.addAllOfItem(schema);
                        composedSchema.addAllOfItem(extraBodyParametersSchema);
                        schema = extraBodyParametersSchema;
                        mediaType.setSchema(composedSchema);
                    }
                }
                for (TypedElement parameter : extraBodyParameters) {
                    if (!isRequestBodySchemaSet) {
                        processBodyParameter(context, openAPI, javadocDescription, getMediaType(mediaTypeName), schema, parameter);
                    }
                    if (mediaTypeName.equals(MediaType.MULTIPART_FORM_DATA)) {
                        if (CollectionUtils.isNotEmpty(schema.getProperties())) {
                            for (String prop : (Set<String>) schema.getProperties().keySet()) {
                                Map<String, Encoding> encodings = mediaType.getEncoding();
                                if (encodings == null) {
                                    encodings = new HashMap<>();
                                    mediaType.setEncoding(encodings);
                                }
                                // if content type doesn't set by annotation,
                                // we can set application/octet-stream for file upload classes
                                Encoding encoding = encodings.get(prop);
                                if (encoding == null && isFileUpload(parameter.getType())) {
                                    encoding = new Encoding();
                                    encodings.put(prop, encoding);

                                    encoding.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                                }
                            }
                        }
                    }
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
        if (CollectionUtils.isEmpty(swaggerParameters)) {
            return false;
        }
        for (var param : swaggerParameters) {
            if (param.getName().equals(parameter.getName()) && param.getIn() != null) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Example> readExamples(List<AnnotationValue<ExampleObject>> exampleAnns,
                             Element element,
                             VisitorContext context) {
        if (CollectionUtils.isEmpty(exampleAnns)) {
            return null;
        }
        var result = new HashMap<String, Example>();
        for (var exampleAnn : exampleAnns) {
            try {
                var exampleMap = toValueMap(exampleAnn.getValues(), context, null);
                result.put((String) exampleMap.get("name"), Utils.getJsonMapper().convertValue(exampleMap, Example.class));
            } catch (Exception e) {
                warn("Error reading Parameter example " + exampleAnn + " for element [" + element + "]: " + e.getMessage(), context, element);
            }
        }

        return !result.isEmpty() ? result : null;
    }

    private void processParameterAnnotationInMethod(MethodElement element,
                                                    OpenAPI openAPI,
                                                    UriMatchTemplate matchTemplate,
                                                    HttpMethod httpMethod,
                                                    io.swagger.v3.oas.models.Operation operation,
                                                    Map<String, UriMatchVariable> pathVariables,
                                                    VisitorContext context) {

        List<AnnotationValue<io.swagger.v3.oas.annotations.Parameter>> parameterAnnotations = element
            .getDeclaredAnnotationValuesByType(io.swagger.v3.oas.annotations.Parameter.class);

        for (var paramAnn : parameterAnnotations) {
            if (paramAnn.get("hidden", Boolean.class, false)) {
                continue;
            }

            Parameter parameter = new Parameter();
            parameter.schema(setSpecVersion(new Schema<>()));

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
            var examples = readExamples(paramAnn.getAnnotations("examples", ExampleObject.class), element, context);
            if (examples != null) {
                examples.forEach(parameter::addExample);
            }

            if (parameter.getIn() == null) {
                for (ParameterElement paramEl : element.getParameters()) {
                    if (!paramEl.getName().equals(parameter.getName())) {
                        continue;
                    }
                    if (paramEl.isAnnotationPresent(PathVariable.class)) {
                        parameter.setIn(ParameterIn.PATH.toString());
                    } else if (paramEl.isAnnotationPresent(QueryValue.class)) {
                        parameter.setIn(ParameterIn.QUERY.toString());
                    } else if (paramEl.isAnnotationPresent(CookieValue.class)) {
                        parameter.setIn(ParameterIn.COOKIE.toString());
                    } else if (paramEl.isAnnotationPresent(Header.class)) {
                        parameter.setIn(ParameterIn.HEADER.toString());
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
                            parameter.setIn(ParameterIn.PATH.toString());
                        }

                        if (parameter.getIn() == null) {
                            if (httpMethod == HttpMethod.GET) {
                                // default to QueryValue -
                                // https://github.com/micronaut-projects/micronaut-openapi/issues/130
                                parameter.setIn(ParameterIn.QUERY.toString());
                            }
                        }

                    }
                }
            }

            operation.addParametersItem(parameter);
            PathItem pathItem = openAPI.getPaths().get(matchTemplate.toPathString());

            setOperationOnPathItem(pathItem, httpMethod, operation);
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

        if (isIgnoredParameter(parameter)) {
            return;
        }
        if (permitsRequestBody && swaggerOperation.getRequestBody() == null) {
            Pair<RequestBody, Boolean> requestBodyPair = readSwaggerRequestBody(parameter, consumesMediaTypes, context);
            if (requestBodyPair != null && requestBodyPair.getFirst() != null) {
                swaggerOperation.setRequestBody(requestBodyPair.getFirst());
            }
        }

        consumesMediaTypes = CollectionUtils.isNotEmpty(consumesMediaTypes) ? consumesMediaTypes : DEFAULT_MEDIA_TYPES;

        if (parameter.isAnnotationPresent(Body.class)) {
            io.swagger.v3.oas.models.Operation existedOperation = null;
            // check existed operations
            for (PathItem pathItem : pathItems) {
                existedOperation = getOperationOnPathItem(pathItem, httpMethod);
                if (existedOperation != null) {
//                    swaggerOperation = existedOperation;
                    break;
                }
            }

            processBody(context, openAPI, swaggerOperation, javadocDescription, permitsRequestBody,
                consumesMediaTypes, parameter, parameterType);

            RequestBody requestBody = swaggerOperation.getRequestBody();
            if (requestBody != null && requestBody.getContent() != null) {
                if (existedOperation != null) {
                    for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry : existedOperation.getRequestBody().getContent().entrySet()) {
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

                        Schema<?> propertySchema = bindSchemaForElement(context, parameter, parameterType, mediaType.getSchema(), null);

                        AnnotationValue<Body> bodyAnn = parameter.getAnnotation(Body.class);

                        String bodyAnnValue = bodyAnn != null ? bodyAnn.getValue(String.class).orElse(null) : null;
                        if (StringUtils.isNotEmpty(bodyAnnValue)) {
                            var wrapperSchema = setSpecVersion(new Schema<>());
                            wrapperSchema.setType(TYPE_OBJECT);
                            if (isNotNullable(parameter)) {
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
            addSwaggerParameter(newParameter, swaggerParameters);
            return;
        }

        if (newParameter.getExplode() != null && newParameter.getExplode() && "query".equals(newParameter.getIn()) && !parameterType.isIterable()) {
            Schema<?> explodedSchema = resolveSchema(openAPI, parameter, parameterType, context, consumesMediaTypes, null, null, null);
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
                        addSwaggerParameter(unwrappedParameter, swaggerParameters);
                    }
                }
            }
        } else {

            if (StringUtils.isEmpty(newParameter.getName())) {
                newParameter.setName(parameter.getName());
            }

            if (newParameter.getRequired() == null && (!isNullable(parameter) || isNotNullable(parameter))) {
                newParameter.setRequired(true);
            }
            if (javadocDescription != null && StringUtils.isEmpty(newParameter.getDescription())) {
                CharSequence desc = javadocDescription.getParameters().get(parameter.getName());
                if (desc != null) {
                    newParameter.setDescription(desc.toString());
                }
            }

            addSwaggerParameter(newParameter, swaggerParameters);

            Schema<?> schema = newParameter.getSchema();
            if (schema == null) {
                schema = resolveSchema(openAPI, parameter, parameterType, context, consumesMediaTypes, null, null, null);
            }

            if (schema != null) {
                schema = bindSchemaForElement(context, parameter, parameterType, schema, null);
                newParameter.setSchema(schema);
            }
        }
    }

    private void addSwaggerParameter(Parameter newParameter, List<Parameter> swaggerParameters) {
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

        ClassElement jsonViewClass = null;
        if (isJsonViewEnabled(context)) {
            AnnotationValue<JsonView> jsonViewAnnotation = parameter.findAnnotation(JsonView.class).orElse(null);
            if (jsonViewAnnotation != null) {
                String jsonViewClassName = jsonViewAnnotation.stringValue().orElse(null);
                if (jsonViewClassName != null) {
                    jsonViewClass = ContextUtils.getClassElement(jsonViewClassName, context);
                }
            }
        }

        Schema<?> propertySchema = resolveSchema(openAPI, parameter, parameter.getType(), context, Collections.singletonList(mediaType), jsonViewClass, null, null);
        if (propertySchema != null) {

            Optional<String> description = parameter.stringValue(io.swagger.v3.oas.annotations.Parameter.class, "description");
            description.ifPresent(propertySchema::setDescription);
            processSchemaProperty(context, parameter, parameter.getType(), null, schema, propertySchema);
            if (isNullable(parameter) && !isNotNullable(parameter)) {
                // Keep null if not
                SchemaUtils.setNullable(propertySchema);
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

        boolean isBodyParameter = false;

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
                warn("Path variable name: '" + paramName + "' not found in path, operation: " + swaggerOperation.getOperationId(), context, parameter);
                return null;
            }
            newParameter = new PathParameter();
            newParameter.setName(paramName);
            final boolean exploded = variable.isExploded();
            if (exploded) {
                newParameter.setExplode(true);
            }
        } else if (parameter.isAnnotationPresent(Header.class)) {
            var headerName = getHeaderName(parameter, parameterName);
            if (headerName == null) {
                return null;
            }
            newParameter = new HeaderParameter();
            newParameter.setName(headerName);
        } else if (parameter.isAnnotationPresent(Headers.class)) {

            List<AnnotationValue<Header>> headerAnnotations = parameter.getAnnotationValuesByType(Header.class);
            if (CollectionUtils.isNotEmpty(headerAnnotations)) {
                var headerName = getHeaderName(parameter, parameterName);
                if (headerName == null) {
                    return null;
                }
                newParameter = new HeaderParameter();
                newParameter.setName(headerName);
            }
        } else if (parameter.isAnnotationPresent(CookieValue.class)) {
            String cookieName = parameter.stringValue(CookieValue.class).orElse(parameterName);
            newParameter = new CookieParameter();
            newParameter.setName(cookieName);
        } else if (parameter.isAnnotationPresent(QueryValue.class)) {
            String queryVar = parameter.stringValue(QueryValue.class).orElse(parameterName);
            newParameter = new QueryParameter();
            newParameter.setName(queryVar);
        } else if (parameter.isAnnotationPresent(Part.class) && permitsRequestBody) {
            extraBodyParameters.add(parameter);
            isBodyParameter = true;
        } else if (parameter.hasAnnotation("io.micronaut.management.endpoint.annotation.Selector")) {
            newParameter = new PathParameter();
            newParameter.setName(parameterName);
        } else if (hasNoBindingAnnotationOrType(parameter)) {
            AnnotationValue<io.swagger.v3.oas.annotations.Parameter> parameterAnnotation = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            // Skip recognizing parameter if it's manually defined by "in"
            var paramIn = parameterAnnotation != null ? parameterAnnotation.stringValue("in").orElse(null) : null;
            if (parameterAnnotation == null || !parameterAnnotation.booleanValue("hidden").orElse(false)
                && (paramIn == null || paramIn.equals(ParameterIn.DEFAULT.toString()))) {
                if (permitsRequestBody) {
                    extraBodyParameters.add(parameter);
                    isBodyParameter = true;
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

        if (isBodyParameter) {
            return null;
        }

        if (parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class)) {
            AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn = parameter
                .findAnnotation(io.swagger.v3.oas.annotations.Parameter.class).orElse(null);

            if (paramAnn != null) {

                if (paramAnn.get("hidden", Boolean.class, false)) {
                    // ignore hidden parameters
                    return null;
                }

                Map<CharSequence, Object> paramValues = toValueMap(paramAnn.getValues(), context, null);
                Utils.normalizeEnumValues(paramValues, Collections.singletonMap("in", ParameterIn.class));
                if (parameter.isAnnotationPresent(Header.class)) {
                    paramValues.put("in", ParameterIn.HEADER.toString());
                } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                    paramValues.put("in", ParameterIn.COOKIE.toString());
                } else if (parameter.isAnnotationPresent(QueryValue.class)) {
                    paramValues.put("in", ParameterIn.QUERY.toString());
                }
                processExplode(paramAnn, paramValues);

                JsonNode jsonNode = Utils.getJsonMapper().valueToTree(paramValues);

                if (newParameter == null) {
                    try {
                        newParameter = ConvertUtils.treeToValue(jsonNode, Parameter.class, context);
                        if (jsonNode.has("schema")) {
                            JsonNode schemaNode = jsonNode.get("schema");
                            if (schemaNode.has("$ref")) {
                                if (newParameter == null) {
                                    newParameter = new Parameter();
                                }
                                newParameter.schema(setSpecVersion(new Schema<>().$ref(schemaNode.get("$ref").asText())));
                            }
                        }
                    } catch (Exception e) {
                        warn("Error reading Swagger Parameter for element [" + parameter + "]: " + e.getMessage(), context, parameter);
                    }
                } else {
                    try {
                        Parameter v = ConvertUtils.treeToValue(jsonNode, Parameter.class, context);
                        if (v == null) {
                            Map<CharSequence, Object> target = OpenApiUtils.getConvertJsonMapper().convertValue(newParameter, MAP_TYPE);
                            for (CharSequence name : paramValues.keySet()) {
                                Object o = paramValues.get(name.toString());
                                if (o != null) {
                                    target.put(name.toString(), o);
                                }
                            }
                            newParameter = OpenApiUtils.getConvertJsonMapper().convertValue(target, Parameter.class);
                        } else {
                            // horrible hack because Swagger
                            // ParameterDeserializer breaks updating
                            // existing objects
                            BeanMap<Parameter> beanMap = BeanMap.of(v);
                            Map<CharSequence, Object> target = OpenApiUtils.getConvertJsonMapper().convertValue(newParameter, MAP_TYPE);
                            for (CharSequence name : beanMap.keySet()) {
                                Object o = beanMap.get(name.toString());
                                if (o != null) {
                                    target.put(name.toString(), o);
                                }
                            }
                            newParameter = OpenApiUtils.getConvertJsonMapper().convertValue(target, Parameter.class);
                        }
                    } catch (IOException e) {
                        warn("Error reading Swagger Parameter for element [" + parameter + "]: " + e.getMessage(), context, parameter);
                    }
                }

                if (newParameter != null && newParameter.get$ref() != null) {
                    return newParameter;
                }

                if (newParameter != null) {
                    final Schema<?> parameterSchema = newParameter.getSchema();
                    if (paramAnn.contains("schema") && parameterSchema != null) {
                        paramAnn.get("schema", AnnotationValue.class)
                            .ifPresent(schemaAnn -> bindSchemaAnnotationValue(context, parameter, parameterSchema, schemaAnn, null));
                    }
                }
            }
        }

        if (newParameter != null && isNullable(parameter) && !isNotNullable(parameter)) {
            newParameter.setRequired(null);
        }

        return newParameter;
    }

    private String getHeaderName(TypedElement parameter, String parameterName) {
        // skip params like this: @Header Map<String, String>
        if (isIgnoredParameter(parameter)) {
            return null;
        }
        String headerName = parameter.stringValue(Header.class, "name")
            .orElse(parameter.stringValue(Header.class)
                .orElseGet(() -> NameUtils.hyphenate(parameterName)));

        if (isIgnoredHeader(headerName)) {
            return null;
        }

        return headerName;
    }

    private boolean isIgnoredHeaderParameter(TypedElement parameter) {
        return parameter.getType().isAssignable(Map.class);
    }

    private void processBody(VisitorContext context, OpenAPI openAPI,
                             io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription,
                             boolean permitsRequestBody, List<MediaType> consumesMediaTypes, TypedElement parameter,
                             ClassElement parameterType) {

        ClassElement jsonViewClass = null;
        if (isJsonViewEnabled(context)) {
            AnnotationValue<JsonView> jsonViewAnnotation = parameter.findAnnotation(JsonView.class).orElse(null);
            if (jsonViewAnnotation != null) {
                String jsonViewClassName = jsonViewAnnotation.stringValue().orElse(null);
                if (jsonViewClassName != null) {
                    jsonViewClass = ContextUtils.getClassElement(jsonViewClassName, context);
                }
            }
        }

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
        if (requestBody.getRequired() == null && (!isNullable(parameterType) || isNotNullable(parameterType))) {
            requestBody.setRequired(true);
        }

        final Content content = buildContent(parameter, parameterType, consumesMediaTypes, openAPI, context, jsonViewClass);
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
                              io.swagger.v3.oas.models.Operation swaggerOperation, JavadocDescription javadocDescription, @Nullable ClassElement jsonViewClass) {

        boolean withMethodResponses = element.hasDeclaredAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponse.class)
            || element.hasDeclaredAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponse.class);

        HttpStatus methodResponseStatus = element.enumValue(Status.class, HttpStatus.class).orElse(HttpStatus.OK);
        String responseCode = Integer.toString(methodResponseStatus.getCode());
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
            addResponseContent(element, context, openAPI, response, jsonViewClass);
            responses.put(responseCode, response);
        } else if (response != null && response.getContent() == null) {
            addResponseContent(element, context, openAPI, response, jsonViewClass);
        }
    }

    private void addResponseContent(MethodElement element, VisitorContext context, OpenAPI openAPI, ApiResponse response, @Nullable ClassElement jsonViewClass) {
        ClassElement returnType = returnType(element, context);
        if (returnType != null && !returnType.getCanonicalName().equals(Void.class.getName())) {
            List<MediaType> producesMediaTypes = producesMediaTypes(element);
            Content content;
            if (producesMediaTypes.isEmpty()) {
                content = buildContent(element, returnType, DEFAULT_MEDIA_TYPES, openAPI, context, jsonViewClass);
            } else {
                content = buildContent(element, returnType, producesMediaTypes, openAPI, context, jsonViewClass);
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

    public Pair<String, PathItem> readWebhook(@Nullable AnnotationValue<Webhook> webhookAnnValue,
                                              HttpMethod httpMethod,
                                              VisitorContext context) {
        if (webhookAnnValue == null) {
            return null;
        }
        final Map<CharSequence, Object> map = toValueMap(webhookAnnValue.getValues(), context, null);

        var name = webhookAnnValue.stringValue("name").orElse(null);
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        var operationAnn = webhookAnnValue.getAnnotation("operation", io.swagger.v3.oas.annotations.Operation.class);
        var operation = operationAnn
            .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.Operation.class, null))
            .orElse(new io.swagger.v3.oas.models.Operation());
        var pathItem = new PathItem();
        setOperationOnPathItem(pathItem, HttpMethod.parse(operationAnn.get().stringValue("method").orElse(httpMethod.name())), operation);
        return Pair.of(name, pathItem);
    }

    private Map<PathItem, io.swagger.v3.oas.models.Operation> readOperations(String path, HttpMethod httpMethod, List<PathItem> pathItems, MethodElement element, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        Map<PathItem, io.swagger.v3.oas.models.Operation> swaggerOperations = new HashMap<>(pathItems.size());
        final Optional<AnnotationValue<Operation>> operationAnnotation = element.findAnnotation(Operation.class);

        for (PathItem pathItem : pathItems) {
            io.swagger.v3.oas.models.Operation swaggerOperation = operationAnnotation
                .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.Operation.class, jsonViewClass))
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
                            var required = paramAnn.booleanValue("required").orElse(false);
                            if (required) {
                                swaggerParam.setRequired(true);
                            }
                            var deprecated = paramAnn.booleanValue("deprecated").orElse(false);
                            if (deprecated) {
                                swaggerParam.setDeprecated(true);
                            }
                            var allowEmptyValue = paramAnn.booleanValue("allowEmptyValue").orElse(false);
                            if (allowEmptyValue) {
                                swaggerParam.setAllowEmptyValue(true);
                            }
                            var allowReserved = paramAnn.booleanValue("allowReserved").orElse(false);
                            if (allowReserved) {
                                swaggerParam.setAllowReserved(true);
                            }
                            paramAnn.stringValue("example").ifPresent(swaggerParam::setExample);
                            var examples = readExamples(paramAnn.getAnnotations("examples", ExampleObject.class), element, context);
                            if (examples != null) {
                                examples.forEach(swaggerParam::addExample);
                            }
                            var style = paramAnn.get("style", ParameterStyle.class).orElse(ParameterStyle.DEFAULT);
                            if (style != ParameterStyle.DEFAULT) {
                                swaggerParam.setStyle(paramStyle(style));
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
                prefix = ContextUtils.get(CONTEXT_CHILD_OP_ID_PREFIX, String.class, "", context);
                suffix = ContextUtils.get(CONTEXT_CHILD_OP_ID_SUFFIX, String.class, "", context);
                addAlways = ContextUtils.get(CONTEXT_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, Boolean.class, true, context);
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

        return externalDocsAnn
            .flatMap(o -> toValue(o.getValues(), context, io.swagger.v3.oas.models.ExternalDocumentation.class, null))
            .orElse(null);
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
                    && (httpMethod == null || securityRule.getHttpMethod() == null || httpMethod == securityRule.getHttpMethod())) {

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

    private boolean isSingleResponseType(ClassElement returnType) {
        return (returnType.isAssignable("io.reactivex.Single")
            || returnType.isAssignable("io.reactivex.rxjava3.core.Single")
            || returnType.isAssignable("org.reactivestreams.Publisher"))
            && returnType.getFirstTypeArgument().isPresent()
            && isResponseType(returnType.getFirstTypeArgument().get());
    }

    private boolean isResponseType(ClassElement returnType) {
        return returnType.isAssignable(HttpResponse.class)
            || returnType.isAssignable("org.springframework.http.HttpEntity");
    }

    private void readApiResponses(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation, @Nullable ClassElement jsonViewClass) {
        List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> methodResponseAnnotations = element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        processResponses(swaggerOperation, methodResponseAnnotations, element, context, jsonViewClass);

        List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> classResponseAnnotations = element.getDeclaringType().getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        processResponses(swaggerOperation, classResponseAnnotations, element, context, jsonViewClass);
    }

    private void processResponses(io.swagger.v3.oas.models.Operation operation, List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> responseAnnotations,
                                  MethodElement element, VisitorContext context, @Nullable ClassElement jsonViewClass) {
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
                Optional<ApiResponse> newResponse = toValue(response.getValues(), context, ApiResponse.class, jsonViewClass);
                if (newResponse.isPresent()) {
                    ApiResponse newApiResponse = newResponse.get();
                    if (response.booleanValue("useReturnTypeSchema").orElse(false) && element != null) {
                        addResponseContent(element, context, Utils.resolveOpenApi(context), newApiResponse, jsonViewClass);
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
                            newApiResponse.setDescription(responseCode.equals("default") ? "OK response" : HttpStatus.getDefaultReason(Integer.parseInt(responseCode)));
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

    // boolean - is swagger schema has implementation
    private Pair<RequestBody, Boolean> readSwaggerRequestBody(Element element, List<MediaType> consumesMediaTypes, VisitorContext context) {
        AnnotationValue<io.swagger.v3.oas.annotations.parameters.RequestBody> requestBodyAnnValue =
            element.findAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class).orElse(null);

        if (requestBodyAnnValue == null) {
            return null;
        }

        boolean hasSchemaImplementation = false;

        AnnotationValue<io.swagger.v3.oas.annotations.media.Content> content = requestBodyAnnValue.getAnnotation("content", io.swagger.v3.oas.annotations.media.Content.class).orElse(null);
        if (content != null) {
            AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> swaggerSchema = content.getAnnotation("schema", io.swagger.v3.oas.annotations.media.Schema.class).orElse(null);
            if (swaggerSchema != null) {
                hasSchemaImplementation = swaggerSchema.stringValue("implementation").orElse(null) != null;
            }
        }

        ClassElement jsonViewClass = null;
        if (isJsonViewEnabled(context) && element instanceof ParameterElement) {
            AnnotationValue<JsonView> jsonViewAnnotation = element.findAnnotation(JsonView.class).orElse(null);
            if (jsonViewAnnotation != null) {
                String jsonViewClassName = jsonViewAnnotation.stringValue().orElse(null);
                if (jsonViewClassName != null) {
                    jsonViewClass = ContextUtils.getClassElement(jsonViewClassName, context);
                }
            }
        }

        RequestBody requestBody = toValue(requestBodyAnnValue.getValues(), context, RequestBody.class, jsonViewClass).orElse(null);
        // if media type doesn't set in swagger annotation, check micronaut annotation
        if (content != null
            && content.stringValue("mediaType").isEmpty()
            && requestBody != null
            && requestBody.getContent() != null
            && !consumesMediaTypes.equals(DEFAULT_MEDIA_TYPES)) {

            io.swagger.v3.oas.models.media.MediaType defaultSwaggerMediaType = requestBody.getContent().remove(MediaType.APPLICATION_JSON);
            for (MediaType mediaType : consumesMediaTypes) {
                requestBody.getContent().put(mediaType.toString(), defaultSwaggerMediaType);
            }
        }

        return Pair.of(requestBody, hasSchemaImplementation);
    }

    private void readServers(MethodElement element, VisitorContext context, io.swagger.v3.oas.models.Operation swaggerOperation) {
        for (Server server : methodServers(element, context)) {
            swaggerOperation.addServersItem(server);
        }
    }

    private void readCallbacks(MethodElement element, VisitorContext context,
                               io.swagger.v3.oas.models.Operation swaggerOperation, @Nullable ClassElement jsonViewClass) {
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
            String callbackName = callbackAnn.stringValue("name").orElse(null);
            if (StringUtils.isEmpty(callbackName)) {
                continue;
            }
            String ref = callbackAnn.stringValue("ref").orElse(null);
            if (StringUtils.isNotEmpty(ref)) {
                String refCallback = ref.substring(COMPONENTS_CALLBACKS_PREFIX.length());
                processCallbackReference(context, swaggerOperation, callbackName, refCallback);
                continue;
            }
            String expr = callbackAnn.stringValue("callbackUrlExpression").orElse(null);
            if (StringUtils.isNotEmpty(expr)) {
                processUrlCallbackExpression(context, swaggerOperation, callbackAnn, callbackName, expr, jsonViewClass);
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
                                              String callbackName, final String callbackUrl, @Nullable ClassElement jsonViewClass) {
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
                operationMethod.ifPresent(httpMethod -> toValue(operation.getValues(), context, io.swagger.v3.oas.models.Operation.class, jsonViewClass)
                    .ifPresent(op -> setOperationOnPathItem(pathItem, httpMethod, op)));
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

    private void processMicronautVersionAndGroup(io.swagger.v3.oas.models.Operation swaggerOperation, String url,
                                                 HttpMethod httpMethod,
                                                 List<MediaType> consumesMediaTypes,
                                                 List<MediaType> producesMediaTypes,
                                                 MethodElement methodEl, VisitorContext context) {

        String methodKey = httpMethod.name()
            + '#' + url
            + '#' + CollectionUtils.toString(CollectionUtils.isEmpty(consumesMediaTypes) ? DEFAULT_MEDIA_TYPES : consumesMediaTypes)
            + '#' + CollectionUtils.toString(CollectionUtils.isEmpty(producesMediaTypes) ? DEFAULT_MEDIA_TYPES : producesMediaTypes);

        Map<String, GroupProperties> groupPropertiesMap = getGroupsPropertiesMap(context);
        var groups = new HashMap<String, EndpointGroupInfo>();
        var excludedGroups = new ArrayList<String>();

        ClassElement classEl = methodEl.getDeclaringType();
        PackageElement packageEl = classEl.getPackage();
        String packageName = packageEl.getName();

        processGroups(groups, excludedGroups, methodEl.getAnnotationValuesByType(OpenAPIGroup.class), groupPropertiesMap);
        processGroups(groups, excludedGroups, packageEl.getAnnotationValuesByType(OpenAPIGroup.class), groupPropertiesMap);

        processGroupsFromIncludedEndpoints(groups, excludedGroups, classEl.getName(), groupPropertiesMap);

        // properties from system properties or from environment more priority than annotations
        for (GroupProperties groupProperties : groupPropertiesMap.values()) {
            if (CollectionUtils.isNotEmpty(groupProperties.getPackages())) {
                for (PackageProperties groupPackage : groupProperties.getPackages()) {
                    boolean isInclude = groupPackage.isIncludeSubpackages() ? packageName.startsWith(groupPackage.getName()) : packageName.equals(groupPackage.getName());
                    if (isInclude) {
                        groups.put(groupProperties.getName(), new EndpointGroupInfo(groupProperties.getName()));
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(groupProperties.getPackagesExclude())) {
                for (PackageProperties excludePackage : groupProperties.getPackagesExclude()) {
                    boolean isExclude = excludePackage.isIncludeSubpackages() ? packageName.startsWith(excludePackage.getName()) : packageName.equals(excludePackage.getName());
                    if (isExclude) {
                        excludedGroups.add(groupProperties.getName());
                    }
                }
            }
        }

        RouterVersioningProperties versioningProperties = getRouterVersioningProperties(context);
        boolean isVersioningEnabled = versioningProperties.isEnabled() && versioningProperties.isRouterVersioningEnabled()
            && (versioningProperties.isHeaderEnabled() || versioningProperties.isParameterEnabled());

        String version = null;

        if (isVersioningEnabled) {

            List<AnnotationValue<Version>> versionsAnnotations = methodEl.getAnnotationValuesByType(Version.class);
            if (CollectionUtils.isNotEmpty(versionsAnnotations)) {
                version = versionsAnnotations.get(0).stringValue().orElse(null);
            }
            if (version != null) {
                Utils.getAllKnownVersions().add(version);
            }
            if (versioningProperties.isParameterEnabled()) {
                addVersionParameters(swaggerOperation, versioningProperties.getParameterNames(), false);
            }
            if (versioningProperties.isHeaderEnabled()) {
                addVersionParameters(swaggerOperation, versioningProperties.getHeaderNames(), true);
            }
        }

        Map<String, List<EndpointInfo>> endpointInfosMap = Utils.getEndpointInfos();
        if (endpointInfosMap == null) {
            endpointInfosMap = new HashMap<>();
            Utils.setEndpointInfos(endpointInfosMap);
        }
        List<EndpointInfo> endpointInfos = endpointInfosMap.computeIfAbsent(methodKey, (k) -> new ArrayList<>());
        endpointInfos.add(new EndpointInfo(
            url,
            httpMethod,
            methodEl,
            swaggerOperation,
            version,
            groups,
            excludedGroups));
    }

    private void processGroups(Map<String, EndpointGroupInfo> groups,
                               List<String> excludedGroups,
                               List<AnnotationValue<OpenAPIGroup>> annotationValues,
                               Map<String, GroupProperties> groupPropertiesMap) {
        if (CollectionUtils.isEmpty(annotationValues)) {
            return;
        }
        for (AnnotationValue<OpenAPIGroup> annValue : annotationValues) {
            excludedGroups.addAll(List.of(annValue.stringValues("exclude")));

            var extensionAnns = annValue.getAnnotations("extensions");
            for (var groupName : annValue.stringValues("value")) {
                var extensions = new HashMap<CharSequence, Object>();
                if (CollectionUtils.isNotEmpty(extensionAnns)) {
                    for (Object extensionAnn : extensionAnns) {
                        processExtensions(extensions, (AnnotationValue<Extension>) extensionAnn);
                    }
                }
                var groupInfo = groups.get(groupName);
                if (groupInfo == null) {
                    groupInfo = new EndpointGroupInfo(groupName);
                    groups.put(groupName, groupInfo);
                }

                groupInfo.getExtensions().putAll(extensions);
            }
        }
        Set<String> allKnownGroups = Utils.getAllKnownGroups();
        allKnownGroups.addAll(groups.keySet());
        allKnownGroups.addAll(excludedGroups);
    }

    private void processGroupsFromIncludedEndpoints(Map<String, EndpointGroupInfo> groups, List<String> excludedGroups, String className, Map<String, GroupProperties> groupPropertiesMap) {
        if (CollectionUtils.isEmpty(Utils.getIncludedClassesGroups()) && CollectionUtils.isEmpty(Utils.getIncludedClassesGroupsExcluded())) {
            return;
        }

        List<String> classGroups = Utils.getIncludedClassesGroups() != null ? Utils.getIncludedClassesGroups().get(className) : Collections.emptyList();
        List<String> classExcludedGroups = Utils.getIncludedClassesGroupsExcluded() != null ? Utils.getIncludedClassesGroupsExcluded().get(className) : Collections.emptyList();

        for (var classGroup : classGroups) {
            if (groups.containsKey(classGroup)) {
                continue;
            }
            groups.put(classGroup, new EndpointGroupInfo(classGroup));
        }
        excludedGroups.addAll(classExcludedGroups);

        Set<String> allKnownGroups = Utils.getAllKnownGroups();
        allKnownGroups.addAll(classGroups);
        allKnownGroups.addAll(classExcludedGroups);
    }

    private void addVersionParameters(io.swagger.v3.oas.models.Operation swaggerOperation, List<String> names, boolean isHeader) {

        String in = isHeader ? ParameterIn.HEADER.toString() : ParameterIn.QUERY.toString();

        for (String parameterName : names) {
            Parameter parameter = new Parameter();
            parameter.in(in)
                .description("API version")
                .name(parameterName)
                .schema(setSpecVersion(PrimitiveType.STRING.createProperty()));

            swaggerOperation.addParametersItem(parameter);
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
        var tags = new ArrayList<io.swagger.v3.oas.models.tags.Tag>();
        for (var ann : annotations) {
            toValue(ann.getValues(), context, io.swagger.v3.oas.models.tags.Tag.class, null)
                .ifPresent(tags::add);
        }
        return tags;
    }

    private Content buildContent(Element definingElement, ClassElement type, List<MediaType> mediaTypes, OpenAPI openAPI, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        var content = new Content();
        for (var mediaType : mediaTypes) {
            var mt = new io.swagger.v3.oas.models.media.MediaType();
            mt.setSchema(resolveSchema(openAPI, definingElement, type, context, Collections.singletonList(mediaType), jsonViewClass, null, null));
            content.addMediaType(mediaType.toString(), mt);
        }
        return content;
    }

}
