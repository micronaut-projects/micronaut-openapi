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

import com.fasterxml.jackson.annotation.JsonView;
import tools.jackson.databind.JsonNode;
import io.micronaut.core.annotation.AnnotationValue;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.format.Format;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
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
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.annotation.OpenAPIDecorator;
import io.micronaut.openapi.annotation.OpenAPIRequest;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.Webhook;
import io.swagger.v3.oas.annotations.callbacks.Callbacks;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
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
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.micronaut.openapi.visitor.ConfigUtils.isJsonViewEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isResponseReadSuccessfulFromCode;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CHILD_OP_ID_PREFIX;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX_ADD_ALWAYS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CHILD_PATH;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_IS_PROCESS_PARENT_CLASS;
import static io.micronaut.openapi.visitor.ContextUtils.get;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.ConvertUtils.MAP_TYPE;
import static io.micronaut.openapi.visitor.ElementUtils.getJsonViewClass;
import static io.micronaut.openapi.visitor.ElementUtils.hasNoBindingAnnotationOrType;
import static io.micronaut.openapi.visitor.ElementUtils.isDeprecated;
import static io.micronaut.openapi.visitor.ElementUtils.isExtraBodyParameter;
import static io.micronaut.openapi.visitor.ElementUtils.isFileUpload;
import static io.micronaut.openapi.visitor.ElementUtils.isIgnoredParameter;
import static io.micronaut.openapi.visitor.ElementUtils.isIterableOfMultipartFiles;
import static io.micronaut.openapi.visitor.ElementUtils.isMapOfListOfMultipartFiles;
import static io.micronaut.openapi.visitor.ElementUtils.isMapOfMultipartFiles;
import static io.micronaut.openapi.visitor.ElementUtils.isMapOfStrings;
import static io.micronaut.openapi.visitor.ElementUtils.isNotNullable;
import static io.micronaut.openapi.visitor.ElementUtils.isNullable;
import static io.micronaut.openapi.visitor.ElementUtils.isResponseType;
import static io.micronaut.openapi.visitor.ElementUtils.isSingleResponseType;
import static io.micronaut.openapi.visitor.ElementUtils.isWrappedBodyParameter;
import static io.micronaut.openapi.visitor.GeneratorUtils.addOperationDeprecatedExtension;
import static io.micronaut.openapi.visitor.GeneratorUtils.addParameterDeprecatedExtension;
import static io.micronaut.openapi.visitor.GroupUtils.processMicronautVersionAndGroup;
import static io.micronaut.openapi.visitor.InternalExt.MICRONAUT_OP_POSTFIX;
import static io.micronaut.openapi.visitor.OpenApiModelProp.MICRONAUT_EXT_PARENT_RESPONSE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ADD_ALWAYS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ALLOW_EMPTY_VALUE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ALLOW_RESERVED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CALLBACK_URL_EXPRESSION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CONTENT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEFAULT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEPRECATED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXAMPLE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXAMPLES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXPLODE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_HIDDEN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_IMPLEMENTATION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_IN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_MEDIA_TYPE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_METHOD;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_OPERATION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_OP_ID_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_OP_ID_SUFFIX;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PARAMETERS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REF_DOLLAR;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REQUIRED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_RESPONSES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_RESPONSE_CODE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_STYLE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_VALUE;
import static io.micronaut.openapi.visitor.ParamUtils.calcIn;
import static io.micronaut.openapi.visitor.ParamUtils.getHeaderName;
import static io.micronaut.openapi.visitor.ParamUtils.paramStyle;
import static io.micronaut.openapi.visitor.ParamUtils.paramStyleByFormat;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.bindSchemaAnnotationValue;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.bindSchemaForElement;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.processSchemaProperty;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.resolveSchema;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.toValue;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.toValueMap;
import static io.micronaut.openapi.visitor.SchemaUtils.COMPONENTS_CALLBACKS_PREFIX;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_STRING;
import static io.micronaut.openapi.visitor.SchemaUtils.appendSchema;
import static io.micronaut.openapi.visitor.SchemaUtils.createComposedSchema;
import static io.micronaut.openapi.visitor.SchemaUtils.createSchema;
import static io.micronaut.openapi.visitor.SchemaUtils.getOperationOnPathItem;
import static io.micronaut.openapi.visitor.SchemaUtils.getReqMode;
import static io.micronaut.openapi.visitor.SchemaUtils.getSchemaByRef;
import static io.micronaut.openapi.visitor.SchemaUtils.setOperationOnPathItem;
import static io.micronaut.openapi.visitor.SecurityUtils.processSecuritySchemes;
import static io.micronaut.openapi.visitor.SecurityUtils.readSecurityRequirements;
import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.DOLLAR;
import static io.micronaut.openapi.visitor.StringUtil.DOT;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.THREE_DOTS;
import static io.micronaut.openapi.visitor.TagUtils.generationTags;
import static io.micronaut.openapi.visitor.TagUtils.readTags;
import static io.micronaut.openapi.visitor.Utils.DEFAULT_MEDIA_TYPES;
import static io.micronaut.openapi.visitor.Utils.getMediaType;
import static io.micronaut.openapi.visitor.Utils.resolveWebhooks;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
public abstract class AbstractOpenApiEndpointVisitor extends AbstractOpenApiVisitor {

    private static final int MAX_SUMMARY_LENGTH = 200;

    protected List<Tag> classTags;
    protected ExternalDocumentation classExternalDocs;

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
        ContextUtils.remove(MICRONAUT_INTERNAL_CHILD_PATH, context);

        if (element.isAnnotationPresent(Controller.class)) {

            element.stringValue(UriMapping.class).ifPresent(url -> ContextUtils.put(MICRONAUT_INTERNAL_CHILD_PATH, url, context));
            String prefix = StringUtils.EMPTY_STRING;
            String suffix = StringUtils.EMPTY_STRING;
            boolean addAlways = true;
            var apiDecoratorAnn = element.getDeclaredAnnotation(OpenAPIDecorator.class);
            if (apiDecoratorAnn != null) {
                prefix = apiDecoratorAnn.stringValue().orElse(StringUtils.EMPTY_STRING);
                if (prefix.isEmpty()) {
                    prefix = apiDecoratorAnn.stringValue(PROP_OP_ID_PREFIX).orElse(StringUtils.EMPTY_STRING);
                }
                suffix = apiDecoratorAnn.stringValue(PROP_OP_ID_SUFFIX).orElse(StringUtils.EMPTY_STRING);
                addAlways = apiDecoratorAnn.booleanValue(PROP_ADD_ALWAYS).orElse(true);
            }
            ContextUtils.put(MICRONAUT_INTERNAL_CHILD_OP_ID_PREFIX, prefix, context);
            ContextUtils.put(MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX, suffix, context);
            ContextUtils.put(MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, addAlways, context);

            var superTypes = new ArrayList<ClassElement>();
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
                ContextUtils.put(MICRONAUT_INTERNAL_IS_PROCESS_PARENT_CLASS, true, context);
                var hasSuperMethods = false;
                for (var superType : superTypes) {
                    if (CollectionUtils.isNotEmpty(superType.getMethods())) {
                        hasSuperMethods = true;
                        break;
                    }
                }
                if (hasSuperMethods) {
                    List<MethodElement> methods = element.getEnclosedElements(ElementQuery.ALL_METHODS.onlyInstance());
                    for (MethodElement method : methods) {
                        visitMethod(method, context);
                    }
                }
                ContextUtils.remove(MICRONAUT_INTERNAL_IS_PROCESS_PARENT_CLASS, context);
            }

            ContextUtils.remove(MICRONAUT_INTERNAL_CHILD_OP_ID_PREFIX, context);
            ContextUtils.remove(MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX, context);
            ContextUtils.remove(MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, context);
        }
        ContextUtils.remove(MICRONAUT_INTERNAL_CHILD_PATH, context);
    }

    private void processTags(ClassElement element, VisitorContext context) {
        classTags = readTags(element, context);
        List<Tag> userDefinedClassTags = getUserDefinedClassTags(element, context);
        if (CollectionUtils.isEmpty(classTags)) {
            classTags = userDefinedClassTags == null ? Collections.emptyList() : userDefinedClassTags;
        } else if (userDefinedClassTags != null) {
            for (Tag tag : userDefinedClassTags) {
                if (!containsTag(tag.getName(), classTags)) {
                    classTags.add(tag);
                }
            }
        }
        if (CollectionUtils.isEmpty(classTags)) {
            classTags = generationTags(element, getClassDescription(context), context);
        }
    }

    private void processExternalDocs(ClassElement element, VisitorContext context) {
        var externalDocsAnn = element.findAnnotation(io.swagger.v3.oas.annotations.ExternalDocumentation.class).orElse(null);
        if (externalDocsAnn == null) {
            return;
        }
        classExternalDocs = toValue(externalDocsAnn.getAnnotationName(), externalDocsAnn.getValues(), context, ExternalDocumentation.class, null);
    }

    private boolean containsTag(String name, List<Tag> tags) {
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
    protected abstract List<Server> methodServers(MethodElement element, VisitorContext context);

    /**
     * Returns the extensions at method level.
     *
     * @param element The MethodElement.
     * @param context The context.
     * @return The extensions.
     */
    protected abstract Map<String, Object> operationExtensions(MethodElement element, VisitorContext context);

    /**
     * Returns the class tags.
     *
     * @param element The ClassElement.
     * @param context The context.
     * @return The class tags.
     */
    protected abstract List<Tag> getUserDefinedClassTags(ClassElement element, VisitorContext context);

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
     * @param context The context
     * @return The uri paths of the element.
     */
    protected abstract List<UriMatchTemplate> uriMatchTemplates(MethodElement element, VisitorContext context);

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
     * Returns the description for the class.
     *
     * @param context the visitor context
     * @return The description for the class.
     */
    protected abstract String getClassDescription(VisitorContext context);

    /**
     * Returns the description for the method.
     *
     * @param context the visitor context
     * @return The description for the method.
     */
    protected abstract String getMethodDescription(VisitorContext context);

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
        OpenAPI openApi = Utils.resolveOpenApi(context);
        JavadocDescription javadocDescription;
        boolean permitsRequestBody = HttpMethod.permitsRequestBody(httpMethod);

        Map<String, List<PathItem>> pathItemsMap = resolvePathItems(context, matchTemplates);
        List<MediaType> consumesMediaTypes = consumesMediaTypes(element);
        List<MediaType> producesMediaTypes = producesMediaTypes(element);

        ClassElement jsonViewClass = null;
        if (isJsonViewEnabled(context)) {
            var jsonViewAnn = element.findAnnotation(JsonView.class).orElse(null);
            if (jsonViewAnn == null) {
                jsonViewAnn = element.getOwningType().findAnnotation(JsonView.class).orElse(null);
            }
            if (jsonViewAnn != null) {
                String jsonViewClassName = jsonViewAnn.stringValue().orElse(null);
                if (jsonViewClassName != null) {
                    jsonViewClass = ContextUtils.getClassElement(jsonViewClassName, context);
                }
            }
        }

        var webhookValue = element.getAnnotation(Webhook.class);
        var webhookPair = readWebhook(webhookValue, httpMethod, context);
        if (webhookPair != null) {
            resolveWebhooks(openApi).put(webhookPair.getFirst(), webhookPair.getSecond());
        }

        for (Map.Entry<String, List<PathItem>> pathItemEntry : pathItemsMap.entrySet()) {
            List<PathItem> pathItems = pathItemEntry.getValue();

            Map<PathItem, Operation> swaggerOperations = readOperations(pathItemEntry.getKey(), httpMethod, pathItems, element, context, jsonViewClass);

            for (Map.Entry<PathItem, Operation> operationEntry : swaggerOperations.entrySet()) {
                Operation swaggerOperation = operationEntry.getValue();
                Operation existedOperation = null;

                for (var pathEntry : openApi.getPaths().entrySet()) {
                    if (pathEntry.getKey().equals(pathItemEntry.getKey())) {
                        var existedPathItem = pathEntry.getValue();
                        if (httpMethod == HttpMethod.GET) {
                            existedOperation = existedPathItem.getGet();
                        } else if (httpMethod == HttpMethod.PUT) {
                            existedOperation = existedPathItem.getPut();
                        } else if (httpMethod == HttpMethod.POST) {
                            existedOperation = existedPathItem.getPost();
                        } else if (httpMethod == HttpMethod.DELETE) {
                            existedOperation = existedPathItem.getDelete();
                        } else if (httpMethod == HttpMethod.OPTIONS) {
                            existedOperation = existedPathItem.getOptions();
                        } else if (httpMethod == HttpMethod.HEAD) {
                            existedOperation = existedPathItem.getHead();
                        } else if (httpMethod == HttpMethod.PATCH) {
                            existedOperation = existedPathItem.getPatch();
                        } else if (httpMethod == HttpMethod.TRACE) {
                            existedOperation = existedPathItem.getTrace();
                        }
                        break;
                    }
                }

                ExternalDocumentation externalDocs = readExternalDocs(element, context);
                if (externalDocs == null) {
                    externalDocs = classExternalDocs;
                }
                if (externalDocs != null) {
                    swaggerOperation.setExternalDocs(externalDocs);
                }

                readTags(element, context, swaggerOperation, classTags == null ? Collections.emptyList() : classTags, openApi);

                readSecurityRequirements(element, httpMethod, pathItemEntry.getKey(), swaggerOperation, methodSecurityRequirements(element, context), context);

                readApiResponses(element, context, swaggerOperation, jsonViewClass);

                readServers(element, context, swaggerOperation);
                readExtensions(element, context, swaggerOperation);

                readCallbacks(element, context, swaggerOperation, jsonViewClass);

                javadocDescription = getMethodDescription(element, swaggerOperation, context);

                if (isDeprecated(element)) {
                    swaggerOperation.setDeprecated(true);
                }

                readResponse(element, context, openApi, swaggerOperation, existedOperation, javadocDescription, jsonViewClass);

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

                var queryParams = new HashMap<String, UriMatchVariable>();
                var pathVariables = new HashMap<String, UriMatchVariable>();
                for (UriMatchTemplate matchTemplate : matchTemplates) {
                    for (Map.Entry<String, UriMatchVariable> varEntry : uriVariables(matchTemplate).entrySet()) {
                        if (pathItemEntry.getKey().contains(OPEN_BRACE + varEntry.getKey() + CLOSE_BRACE)) {
                            pathVariables.put(varEntry.getKey(), varEntry.getValue());
                        }
                        if (varEntry.getValue().isQuery()) {
                            queryParams.put(varEntry.getKey(), varEntry.getValue());
                        }
                    }
                    // @Parameters declared at method level take precedence over the declared as method arguments, so we process them first
                    processParameterAnnotationInMethod(element, openApi, matchTemplate, httpMethod, swaggerOperation, pathVariables, context);
                }

                var extraBodyParameters = new ArrayList<TypedElement>();
                for (Operation operation : swaggerOperations.values()) {
                    processParameters(element, context, openApi, operation, javadocDescription, permitsRequestBody, pathVariables, queryParams, consumesMediaTypes, extraBodyParameters, httpMethod, matchTemplates, pathItems);
                    processExtraBodyParameters(context, httpMethod, openApi, operation, javadocDescription, isRequestBodySchemaSet, consumesMediaTypes, extraBodyParameters);

                    processMicronautVersionAndGroup(operation, pathItemEntry.getKey(), httpMethod, consumesMediaTypes, producesMediaTypes, element, context);
                    addParamsByUriTemplate(pathItemEntry.getKey(), pathVariables, queryParams, swaggerOperation);
                }

                if (webhookPair != null) {
                    SchemaUtils.mergeOperations(getOperationOnPathItem(webhookPair.getSecond(), httpMethod), swaggerOperation);
                }
            }
        }
    }

    private void addParamsByUriTemplate(String path, Map<String, UriMatchVariable> pathVariables,
                                        Map<String, UriMatchVariable> queryParams,
                                        Operation operation) {

        // check path variables in URL template which do not map to method parameters
        for (var entry : pathVariables.entrySet()) {
            var varName = entry.getKey();
            var pathVar = entry.getValue();
            if (pathVar.isExploded()
                || !path.contains(OPEN_BRACE + varName + CLOSE_BRACE)
                // skip placeholders
                || path.contains(DOLLAR + OPEN_BRACE + varName + CLOSE_BRACE)
                || isAlreadyAdded(varName, operation)) {
                continue;
            }

            operation.addParametersItem(new Parameter()
                .in(ParameterIn.PATH.toString())
                .name(varName)
                .required(true)
                .schema(PrimitiveType.STRING.createProperty(Utils.isOpenapi31())));
        }

        for (var entry : queryParams.entrySet()) {
            var varName = entry.getKey();
            var pathVar = entry.getValue();
            if (pathVar.isExploded() || isAlreadyAdded(varName, operation)) {
                continue;
            }

            operation.addParametersItem(new Parameter()
                .in(ParameterIn.QUERY.toString())
                .name(varName)
                .schema(PrimitiveType.STRING.createProperty(Utils.isOpenapi31())));
        }
    }

    private boolean isAlreadyAdded(String paramName, Operation operation) {
        if (CollectionUtils.isEmpty(operation.getParameters())) {
            return false;
        }
        for (var param : operation.getParameters()) {
            if (param.getName().equals(paramName)) {
                return true;
            }
        }
        return false;
    }

    private void processExtraBodyParameters(VisitorContext context, HttpMethod httpMethod, OpenAPI openApi,
                                            Operation swaggerOperation,
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
                    var schema = createSchema();
                    schema.setType(TYPE_OBJECT);
                    mt.setSchema(schema);
                    content.addMediaType(mediaType.toString(), mt);
                });
            }
        }
        if (requestBody != null && requestBody.getContent() != null && !extraBodyParameters.isEmpty()) {
            requestBody.getContent().forEach((mediaTypeName, mediaType) -> {
                var schema = mediaType.getSchema();
                if (schema == null || TYPE_STRING.equals(schema.getType())) {
                    var newSchema = createSchema();
                    if (schema != null) {
                        newSchema.setNullable(schema.getNullable());
                    }
                    mediaType.setSchema(newSchema);
                    schema = newSchema;
                }
                if (schema.get$ref() != null) {
                    if (isRequestBodySchemaSet) {
                        schema = getSchemaByRef(schema, openApi);
                    } else {
                        var composedSchema = createComposedSchema();
                        var extraBodyParametersSchema = createSchema();
                        // Composition of existing + a new schema where extra body parameters are going to be added
                        composedSchema.addAllOfItem(schema);
                        composedSchema.addAllOfItem(extraBodyParametersSchema);
                        schema = extraBodyParametersSchema;
                        mediaType.setSchema(composedSchema);
                    }
                }
                for (TypedElement parameter : extraBodyParameters) {
                    if (!isRequestBodySchemaSet) {
                        processBodyParameter(context, openApi, javadocDescription, getMediaType(mediaTypeName), schema, parameter);
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
                                if (encoding == null) {
                                    if (isFileUpload(parameter.getType())
                                        || isMapOfMultipartFiles(parameter)
                                        || isMapOfListOfMultipartFiles(parameter)
                                        || isIterableOfMultipartFiles(parameter)) {

                                        encodings.put(prop, new Encoding()
                                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                            .explode(isIterableOfMultipartFiles(parameter) || isMapOfListOfMultipartFiles(parameter)));
                                    } else if (isMapOfStrings(parameter)) {
                                        encodings.put(parameter.getName(), new Encoding()
                                            .contentType(MediaType.TEXT_PLAIN));
                                        // also need to replace ObjectSchema to StringSchema
                                        schema.getProperties().put(prop, PrimitiveType.STRING.createProperty());
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void processParameters(MethodElement element, VisitorContext context, OpenAPI openApi,
                                   Operation swaggerOperation, JavadocDescription javadocDescription,
                                   boolean permitsRequestBody,
                                   Map<String, UriMatchVariable> pathVariables,
                                   Map<String, UriMatchVariable> queryParams,
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
                processParameter(context, openApi, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables, queryParams,
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
                var exampleMap = toValueMap(exampleAnn.getAnnotationName(), exampleAnn.getValues(), context, null);
                result.put((String) exampleMap.get(PROP_NAME), Utils.getJsonMapper().convertValue(exampleMap, Example.class));
            } catch (Exception e) {
                warn("Error reading Parameter example " + exampleAnn + " for element [" + element + "]: " + e.getMessage(), context, element);
            }
        }

        return !result.isEmpty() ? result : null;
    }

    private void processParameterAnnotationInMethod(MethodElement element,
                                                    OpenAPI openApi,
                                                    UriMatchTemplate matchTemplate,
                                                    HttpMethod httpMethod,
                                                    Operation operation,
                                                    Map<String, UriMatchVariable> pathVariables,
                                                    VisitorContext context) {

        var parameterAnns = element.getDeclaredAnnotationValuesByType(io.swagger.v3.oas.annotations.Parameter.class);

        for (var paramAnn : parameterAnns) {
            if (paramAnn.get(PROP_HIDDEN, Boolean.class, false)) {
                continue;
            }

            var parameter = new Parameter();
            parameter.schema(createSchema());

            paramAnn.stringValue(PROP_NAME).ifPresent(parameter::name);
            paramAnn.enumValue(PROP_IN, ParameterIn.class).ifPresent(in -> parameter.in(in.toString()));
            paramAnn.stringValue(PROP_DESCRIPTION).ifPresent(parameter::description);
            paramAnn.booleanValue(PROP_REQUIRED).ifPresent(value -> parameter.setRequired(value ? true : null));
            paramAnn.booleanValue(PROP_DEPRECATED).ifPresent(value -> parameter.setDeprecated(value ? true : null));
            paramAnn.booleanValue(PROP_ALLOW_EMPTY_VALUE).ifPresent(value -> parameter.setAllowEmptyValue(value ? true : null));
            paramAnn.booleanValue(PROP_ALLOW_RESERVED).ifPresent(value -> parameter.setAllowReserved(value ? true : null));
            paramAnn.stringValue(PROP_EXAMPLE).ifPresent(parameter::example);
            paramAnn.stringValue(PROP_REF).ifPresent(parameter::$ref);
            paramAnn.enumValue(PROP_STYLE, ParameterStyle.class).ifPresent(style -> parameter.setStyle(paramStyle(style, parameter.getIn())));
            var examples = readExamples(paramAnn.getAnnotations(PROP_EXAMPLES, ExampleObject.class), element, context);
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
            PathItem pathItem = openApi.getPaths().get(matchTemplate.toPathString());

            setOperationOnPathItem(pathItem, httpMethod, operation);
        }
    }

    private void processParameter(VisitorContext context, OpenAPI openApi,
                                  Operation swaggerOperation, JavadocDescription javadocDescription,
                                  boolean permitsRequestBody,
                                  Map<String, UriMatchVariable> pathVariables,
                                  Map<String, UriMatchVariable> queryParams,
                                  List<MediaType> consumesMediaTypes,
                                  List<Parameter> swaggerParameters, TypedElement parameter,
                                  List<TypedElement> extraBodyParameters,
                                  HttpMethod httpMethod,
                                  List<UriMatchTemplate> matchTemplates,
                                  List<PathItem> pathItems) {
        ClassElement parameterType = parameter.getGenericType();

        if (isIgnoredParameter(parameter)) {
            return;
        }
        var hasSwaggerRequestBodyImpl = false;
        if (permitsRequestBody && swaggerOperation.getRequestBody() == null) {
            Pair<RequestBody, Boolean> requestBodyPair = readSwaggerRequestBody(parameter, consumesMediaTypes, context);
            if (requestBodyPair != null) {
                if (requestBodyPair.getFirst() != null) {
                    swaggerOperation.setRequestBody(requestBodyPair.getFirst());
                }
                hasSwaggerRequestBodyImpl = requestBodyPair.getSecond();
            }
        }

        consumesMediaTypes = CollectionUtils.isNotEmpty(consumesMediaTypes) ? consumesMediaTypes : DEFAULT_MEDIA_TYPES;

        if (parameter.isAnnotationPresent(Body.class) && !hasSwaggerRequestBodyImpl) {
            processBody(context, openApi, swaggerOperation, javadocDescription, permitsRequestBody, consumesMediaTypes, parameter, parameterType);
            return;
        }

        if (parameter.isAnnotationPresent(RequestBean.class)
            || parameter.isAnnotationPresent(OpenAPIRequest.class)) {
            processRequestBean(context, openApi, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables, queryParams,
                consumesMediaTypes, swaggerParameters, parameter, extraBodyParameters, httpMethod, matchTemplates, pathItems);
            return;
        }

        Parameter newParameter = processMethodParameterAnnotation(context, swaggerOperation, permitsRequestBody,
            pathVariables, queryParams, parameter, extraBodyParameters, httpMethod, matchTemplates);
        if (newParameter == null) {
            return;
        }
        if (newParameter.get$ref() != null) {
            addSwaggerParameter(newParameter, swaggerParameters);
            return;
        }

        if (newParameter.getExplode() != null && newParameter.getExplode() && "query".equals(newParameter.getIn()) && !parameterType.isIterable()) {
            Schema<?> explodedSchema = resolveSchema(openApi, parameter, parameterType, context, consumesMediaTypes, null, null, null, null);
            if (explodedSchema != null) {
                if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null && StringUtils.isNotEmpty(explodedSchema.get$ref())) {
                    explodedSchema = getSchemaByRef(explodedSchema, openApi);
                }
                if (CollectionUtils.isNotEmpty(explodedSchema.getProperties())) {
                    Map<String, Schema> props = explodedSchema.getProperties();
                    for (Map.Entry<String, Schema> entry : props.entrySet()) {
                        var unwrappedParameter = new QueryParameter();
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
                // exception for spring actuator prometheus endpoint
                if (!get(MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS, Boolean.class, false, context)
                    || !"query".equalsIgnoreCase(newParameter.getIn())) {
                    newParameter.setRequired(true);
                }
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
                schema = resolveSchema(openApi, parameter, parameterType, context, consumesMediaTypes, null, null, null, null);
            }

            if (schema != null) {
                schema = bindSchemaForElement(context, parameter, parameterType, schema, null, false, true);
                newParameter.setSchema(schema);
                var parameterAnn = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
                AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnn = null;
                if (parameterAnn != null) {
                    schemaAnn = parameterAnn.getAnnotation(PROP_SCHEMA, io.swagger.v3.oas.annotations.media.Schema.class).orElse(null);
                }
                if (schemaAnn == null) {
                    schemaAnn = parameter.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                }

                var isParamRequired = parameterAnn != null ? parameterAnn.get(PROP_REQUIRED, Boolean.class).orElse(false) : false;
                if (!isParamRequired) {
                    var reqMode = getReqMode(schemaAnn);
                    if (reqMode.isAutoRequiredMode()) {
                        schema = SchemaUtils.unwrapComposedSchema(schema);
                        if (newParameter.getRequired() != null && newParameter.getRequired() && schema.getDefault() != null) {
                            newParameter.setRequired(null);
                        }
                    }
                }
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

    private void processBodyParameter(VisitorContext context, OpenAPI openApi, JavadocDescription javadocDescription,
                                      MediaType mediaType, Schema schema, TypedElement parameter) {

        var jsonViewClass = getJsonViewClass(parameter, context);

        Schema<?> propertySchema = resolveSchema(openApi, parameter, parameter.getType(), context, Collections.singletonList(mediaType), jsonViewClass, null, null, null);
        if (propertySchema == null) {
            return;
        }
        if (isMapOfMultipartFiles(parameter) || isMapOfListOfMultipartFiles(parameter)) {
            propertySchema = (Schema) propertySchema.getAdditionalProperties();
        }

        parameter.stringValue(io.swagger.v3.oas.annotations.Parameter.class, PROP_DESCRIPTION)
            .ifPresent(propertySchema::setDescription);
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

    private Parameter processMethodParameterAnnotation(VisitorContext context, Operation swaggerOperation,
                                                       boolean permitsRequestBody,
                                                       Map<String, UriMatchVariable> pathVariables,
                                                       Map<String, UriMatchVariable> queryParams,
                                                       TypedElement parameter,
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
            if (paramName.isEmpty()) {
                paramName = parameterName;
            }
            UriMatchVariable variable = pathVariables.get(paramName);
            if (variable == null) {
                if (!isNullable(parameter)) {
                    warn("Path variable name: '" + paramName + "' not found in path, operation: " + swaggerOperation.getOperationId(), context, parameter);
                }
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

            var headerAnns = parameter.getAnnotationValuesByType(Header.class);
            if (CollectionUtils.isNotEmpty(headerAnns)) {
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

            var isExtraBodyParam = false;
            // Fix for Spring boot controller endpoints for Map with MultipartFile like this:
            // @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
            // public void endpoint2(@RequestParam Map<String, MultipartFile> files)
            // because, `@RequestParam` annotation mapped to `@QueryValue`, but it's required for Spring Boot
            // fix only for Map<String, MultipartFile>
            if (isMapOfMultipartFiles(parameter) || isMapOfListOfMultipartFiles(parameter) || isIterableOfMultipartFiles(parameter)) {
                extraBodyParameters.add(parameter);
                isExtraBodyParam = true;
            } else if (isMapOfStrings(parameter)) {
                var parameterAnn = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
                var paramIn = parameterAnn != null ? parameterAnn.stringValue(PROP_IN).orElse(null) : null;
                if (paramIn != null) {
                    return null;
                }
                extraBodyParameters.add(parameter);
                isExtraBodyParam = true;
            }
            if (!isExtraBodyParam) {
                String queryVar = parameter.stringValue(QueryValue.class).orElse(parameterName);
                newParameter = new QueryParameter();
                newParameter.setName(queryVar);
            }
        } else if (parameter.isAnnotationPresent(Part.class) && permitsRequestBody) {
            extraBodyParameters.add(parameter);
            isBodyParameter = true;
        } else if (parameter.hasAnnotation("io.micronaut.management.endpoint.annotation.Selector")) {
            newParameter = new PathParameter();
            newParameter.setName(parameterName);
        } else if (hasNoBindingAnnotationOrType(parameter)) {
            var parameterAnn = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            // Skip recognizing parameter if it's manually defined by PROP_IN
            var paramIn = parameterAnn != null ? parameterAnn.stringValue(PROP_IN).orElse(null) : null;
            if (parameterAnn == null || !parameterAnn.booleanValue(PROP_HIDDEN).orElse(false)
                && (paramIn == null || paramIn.equals(ParameterIn.DEFAULT.toString()))) {
                if (isExtraBodyParameter(parameter, permitsRequestBody, matchTemplates, pathVariables, queryParams)) {
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

        var paramAnn = parameter.findAnnotation(io.swagger.v3.oas.annotations.Parameter.class).orElse(null);
        if (paramAnn != null) {

            if (paramAnn.get(PROP_HIDDEN, Boolean.class, false)) {
                // ignore hidden parameters
                return null;
            }

            var paramValues = new HashMap<>(toValueMap(paramAnn.getAnnotationName(), paramAnn.getValues(), context, null));
            Utils.normalizeEnumValues(paramValues, Collections.singletonMap(PROP_IN, ParameterIn.class));
            if (parameter.isAnnotationPresent(Header.class)) {
                paramValues.put(PROP_IN, ParameterIn.HEADER.toString());
            } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                paramValues.put(PROP_IN, ParameterIn.COOKIE.toString());
            } else if (parameter.isAnnotationPresent(QueryValue.class)) {
                paramValues.put(PROP_IN, ParameterIn.QUERY.toString());
            }
            processExplode(paramAnn, paramValues);

            JsonNode jsonNode = Utils.getJsonMapper().valueToTree(paramValues);

            if (newParameter == null) {
                try {
                    newParameter = ConvertUtils.treeToValue(jsonNode, Parameter.class, context);
                    if (jsonNode.has(PROP_SCHEMA)) {
                        JsonNode schemaNode = jsonNode.get(PROP_SCHEMA);
                        if (schemaNode.has(PROP_REF_DOLLAR)) {
                            if (newParameter == null) {
                                newParameter = new Parameter();
                            }
                            newParameter.schema(createSchema().$ref(schemaNode.get(PROP_REF_DOLLAR).asText()));
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
                if (paramAnn.contains(PROP_SCHEMA) && parameterSchema != null) {
                    paramAnn.get(PROP_SCHEMA, AnnotationValue.class)
                        .ifPresent(schemaAnn -> bindSchemaAnnotationValue(context, parameter, parameterSchema, schemaAnn, null));
                }
            }
        }

        if (newParameter != null && isNullable(parameter) && !isNotNullable(parameter)) {
            newParameter.setRequired(null);
        }

        if (newParameter != null && isDeprecated(parameter)) {
            newParameter.setDeprecated(true);
            addParameterDeprecatedExtension(parameter, newParameter, context);
        }

        if (newParameter != null) {
            var mnFormatValue = parameter.stringValue(Format.class, PROP_VALUE).orElse(null);
            var style = paramStyleByFormat(mnFormatValue, newParameter.getIn());
            // case, when FORMAT_CSV
            if ("query".equalsIgnoreCase(newParameter.getIn()) && (mnFormatValue == null || MnParamFormat.getByName(mnFormatValue) == MnParamFormat.CSV)) {
                if (newParameter.getExplode() == null) {
                    newParameter.setExplode(false);
                }
            }
            if (style != null && newParameter.getStyle() == null) {
                newParameter.setStyle(style);
            }
        }

        return newParameter;
    }

    private void processBody(VisitorContext context, OpenAPI openApi,
                             Operation swaggerOperation, JavadocDescription javadocDescription,
                             boolean permitsRequestBody, List<MediaType> consumesMediaTypes, TypedElement parameter,
                             ClassElement parameterType) {

        var jsonViewClass = getJsonViewClass(parameter, context);

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
            if (desc != null && !desc.isEmpty()) {
                requestBody.setDescription(desc.toString());
            }
        }
        if (requestBody.getRequired() == null && (!isNullable(parameterType) || isNotNullable(parameterType))) {
            requestBody.setRequired(true);
        }

        var content = requestBody.getContent();
        if (content == null) {
            content = new Content();
            requestBody.setContent(content);
        }

        var isWrappedBodyParam = isWrappedBodyParameter(parameter);
        var bodyAnn = parameter.getAnnotation(Body.class);
        String wrappedSchemaPropertyName = bodyAnn != null ? bodyAnn.getValue(String.class).orElse(null) : null;

        for (var mediaType : consumesMediaTypes) {
            var mt = content.get(mediaType.toString());
            if (mt == null) {
                mt = new io.swagger.v3.oas.models.media.MediaType();
                content.addMediaType(mediaType.toString(), mt);
            }
            if (isWrappedBodyParam && wrappedSchemaPropertyName != null) {

                var wrapperSchema = mt.getSchema();
                if (wrapperSchema == null) {
                    wrapperSchema = createSchema();
                    mt.setSchema(wrapperSchema);
                }
                wrapperSchema.setType(TYPE_OBJECT);
                if (isNotNullable(parameter)) {
                    wrapperSchema.addRequiredItem(wrappedSchemaPropertyName);
                }
                processBodyParameter(context, openApi, javadocDescription, mediaType, wrapperSchema, parameter);
            } else {
                var needSetSchema = true;
                var paramSchema = resolveSchema(openApi, parameter, parameterType, context, Collections.singletonList(mediaType), jsonViewClass, null, null, null);
                if (mt.getSchema() != null) {
                    if (mt.getSchema().get$ref() != null
                        || CollectionUtils.isNotEmpty(mt.getSchema().getAnyOf())
                        || CollectionUtils.isNotEmpty(mt.getSchema().getAllOf())
                        || CollectionUtils.isNotEmpty(mt.getSchema().getOneOf())
                    ) {
                        needSetSchema = false;
                    } else {
                        appendSchema(paramSchema, mt.getSchema());
                    }
                }
                if (needSetSchema) {
                    mt.setSchema(paramSchema);
                }
            }
        }

        for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry : content.entrySet()) {
            io.swagger.v3.oas.models.media.MediaType mediaType = entry.getValue();
            io.swagger.v3.oas.models.media.MediaType existedMediaType = content.get(entry.getKey());
            if (existedMediaType == null) {
                content.put(entry.getKey(), mediaType);
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

    private void processRequestBean(VisitorContext context, OpenAPI openApi,
                                    Operation swaggerOperation, JavadocDescription javadocDescription,
                                    boolean permitsRequestBody,
                                    Map<String, UriMatchVariable> pathVariables,
                                    Map<String, UriMatchVariable> queryParams,
                                    List<MediaType> consumesMediaTypes,
                                    List<Parameter> swaggerParameters, TypedElement parameter,
                                    List<TypedElement> extraBodyParameters,
                                    HttpMethod httpMethod,
                                    List<UriMatchTemplate> matchTemplates,
                                    List<PathItem> pathItems) {
        for (FieldElement field : parameter.getType().getFields()) {
            if (field.isStatic()) {
                continue;
            }
            processParameter(context, openApi, swaggerOperation, javadocDescription, permitsRequestBody, pathVariables, queryParams,
                consumesMediaTypes, swaggerParameters, field, extraBodyParameters, httpMethod, matchTemplates, pathItems);
        }
    }

    private void readResponse(MethodElement element, VisitorContext context, OpenAPI openApi, Operation swaggerOperation,
                              Operation existedOperation, JavadocDescription javadocDescription, @Nullable ClassElement jsonViewClass) {

        boolean withMethodResponses = element.hasDeclaredAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponses.class)
            || element.hasDeclaredAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        if (!withMethodResponses) {
            var operationAnn = element.findAnnotation(io.swagger.v3.oas.annotations.Operation.class).orElse(null);
            if (operationAnn != null) {
                var responses = operationAnn.getAnnotations(PROP_RESPONSES);
                withMethodResponses = CollectionUtils.isNotEmpty(responses);
            }
        }

        boolean isSuccessfulFromCode = isResponseReadSuccessfulFromCode(context);

        ApiResponses existedResponses = existedOperation != null ? existedOperation.getResponses() : null;
        ApiResponses responses = swaggerOperation.getResponses();
        HttpStatus methodResponseStatus = element.enumValue(Status.class, HttpStatus.class).orElse(null);
        var responseStatusNotSetOrSetByAnnotation = element.hasAnnotation(Status.class) || methodResponseStatus == null;
        if (methodResponseStatus == null) {
            methodResponseStatus = HttpStatus.OK;
        } else if (existedResponses != null && existedResponses.getExtensions() != null && existedResponses.getExtensions().containsKey(MICRONAUT_EXT_PARENT_RESPONSE)) {
            existedOperation.setResponses(null);
        }
        String responseCode = Integer.toString(methodResponseStatus.getCode());
        ApiResponse response = null;

        var responseFromCode = isSuccessfulFromCode;

        if (responses == null) {
            responses = new ApiResponses();
            swaggerOperation.setResponses(responses);
        } else {
            ApiResponse defaultResponse = responses.remove(PROP_DEFAULT);
            response = responses.get(responseCode);
            if (response == null && defaultResponse != null) {
                response = defaultResponse;
                responses.put(responseCode, response);
            }
            var isResponseSetInAnnotation = false;
            for (var responseCodeFromAnn : responses.keySet()) {
                if (responseCodeFromAnn.startsWith("2") || responseCodeFromAnn.startsWith("3")) {
                    isResponseSetInAnnotation = true;
                    break;
                }
            }
            if (response != null || isResponseSetInAnnotation) {
                responseFromCode = false;
            }
        }
        if (responseStatusNotSetOrSetByAnnotation) {
            responses.addExtension(MICRONAUT_EXT_PARENT_RESPONSE, true);
        }
        if (response == null && (!withMethodResponses || responseFromCode)) {
            response = new ApiResponse();
            if (javadocDescription == null || StringUtils.isEmpty(javadocDescription.getReturnDescription())) {
                response.setDescription(swaggerOperation.getOperationId() + StringUtils.SPACE + responseCode + " response");
            } else {
                response.setDescription(javadocDescription.getReturnDescription());
            }
            addResponseContent(element, context, openApi, response, jsonViewClass);
            responses.put(responseCode, response);
        } else if (response != null && response.getContent() == null) {
            addResponseContent(element, context, openApi, response, jsonViewClass);
        }
    }

    private void addResponseContent(MethodElement element, VisitorContext context, OpenAPI openApi, ApiResponse response, @Nullable ClassElement jsonViewClass) {
        ClassElement returnType = returnType(element, context);
        if (returnType != null && !returnType.getCanonicalName().equals(Void.class.getName())) {
            List<MediaType> producesMediaTypes = producesMediaTypes(element);
            Content content;
            if (producesMediaTypes.isEmpty()) {
                content = buildContent(element, returnType, DEFAULT_MEDIA_TYPES, openApi, context, jsonViewClass);
            } else {
                content = buildContent(element, returnType, producesMediaTypes, openApi, context, jsonViewClass);
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
            returnType = returnType.getFirstTypeArgument().orElse(null);
            if (returnType != null) {
                returnType = returnType.getFirstTypeArgument().orElse(returnType);
            }
        }

        return returnType;
    }

    private Map<String, UriMatchVariable> uriVariables(UriMatchTemplate matchTemplate) {
        List<UriMatchVariable> pv = matchTemplate.getVariables();
        var pathVariables = new LinkedHashMap<String, UriMatchVariable>(pv.size());
        for (UriMatchVariable variable : pv) {
            pathVariables.put(variable.getName(), variable);
        }
        return pathVariables;
    }

    private JavadocDescription getMethodDescription(MethodElement element, Operation swaggerOperation, VisitorContext context) {
        String descr = getMethodDescription(context);
        if (StringUtils.isNotEmpty(descr) && StringUtils.isEmpty(swaggerOperation.getDescription())) {
            swaggerOperation.setDescription(descr);
            String summary = descr.contains(DOT) ? descr.substring(0, descr.indexOf('.') + 1) : descr;
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

    private Pair<String, PathItem> readWebhook(@Nullable AnnotationValue<Webhook> webhookAnnValue,
                                               HttpMethod httpMethod,
                                               VisitorContext context) {
        if (webhookAnnValue == null) {
            return null;
        }
        var name = webhookAnnValue.stringValue(PROP_NAME).orElse(null);
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        var operationAnn = webhookAnnValue.getAnnotation(PROP_OPERATION, io.swagger.v3.oas.annotations.Operation.class).orElse(null);
        Operation operation;
        HttpMethod method;
        if (operationAnn != null) {
            operation = toValue(operationAnn.getAnnotationName(), operationAnn.getValues(), context, Operation.class, null);
            method = HttpMethod.parse(operationAnn.stringValue(PROP_METHOD).orElse(httpMethod.name()));
        } else {
            operation = new Operation();
            method = HttpMethod.POST;
        }
        var pathItem = new PathItem();
        setOperationOnPathItem(pathItem, method, operation);
        return Pair.of(name, pathItem);
    }

    private Map<PathItem, Operation> readOperations(String path, HttpMethod httpMethod, List<PathItem> pathItems, MethodElement methodEl, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        var swaggerOperations = new HashMap<PathItem, Operation>(pathItems.size());
        var operationAnn = methodEl.findAnnotation(io.swagger.v3.oas.annotations.Operation.class).orElse(null);

        for (PathItem pathItem : pathItems) {
            var swaggerOperation = operationAnn != null ? toValue(operationAnn.getAnnotationName(), operationAnn.getValues(), context, Operation.class, jsonViewClass) : null;
            if (swaggerOperation == null) {
                swaggerOperation = new Operation();
            }

            addOperationDeprecatedExtension(methodEl, swaggerOperation, context);

            if (CollectionUtils.isNotEmpty(swaggerOperation.getParameters())) {
                swaggerOperation.getParameters().removeIf(Objects::isNull);
            }

            ParameterElement[] methodParams = methodEl.getParameters();
            if (ArrayUtils.isNotEmpty(methodParams) && operationAnn != null) {
                var paramAnns = operationAnn.getAnnotations(PROP_PARAMETERS, io.swagger.v3.oas.annotations.Parameter.class);
                if (CollectionUtils.isNotEmpty(paramAnns)) {
                    for (ParameterElement methodParam : methodParams) {
                        AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn = null;
                        for (AnnotationValue<io.swagger.v3.oas.annotations.Parameter> param : paramAnns) {
                            String paramName = param.stringValue(PROP_NAME).orElse(null);
                            if (methodParam.getName().equals(paramName)) {
                                paramAnn = param;
                                break;
                            }
                        }

                        Parameter swaggerParam = null;
                        if (paramAnn != null && !paramAnn.booleanValue(PROP_HIDDEN).orElse(false)) {
                            String paramName = paramAnn.stringValue(PROP_NAME).orElse(null);
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
                            paramAnn.stringValue(PROP_DESCRIPTION).ifPresent(swaggerParam::setDescription);
                            var required = paramAnn.booleanValue(PROP_REQUIRED).orElse(false);
                            if (required) {
                                swaggerParam.setRequired(true);
                            }
                            var allowEmptyValue = paramAnn.booleanValue(PROP_ALLOW_EMPTY_VALUE).orElse(false);
                            if (allowEmptyValue) {
                                swaggerParam.setAllowEmptyValue(true);
                            }
                            var allowReserved = paramAnn.booleanValue(PROP_ALLOW_RESERVED).orElse(false);
                            if (allowReserved) {
                                swaggerParam.setAllowReserved(true);
                            }
                            paramAnn.stringValue(PROP_EXAMPLE).ifPresent(swaggerParam::setExample);
                            var examples = readExamples(paramAnn.getAnnotations(PROP_EXAMPLES, ExampleObject.class), methodEl, context);
                            if (examples != null) {
                                examples.forEach(swaggerParam::addExample);
                            }
                            paramAnn.stringValue(PROP_REF).ifPresent(swaggerParam::set$ref);
                            Optional<ParameterIn> inOpt = paramAnn.get(PROP_IN, ParameterIn.class);
                            if (inOpt.isPresent()) {
                                var in = inOpt.get();
                                if (in == ParameterIn.DEFAULT) {
                                    swaggerParam.setIn(calcIn(path, httpMethod, methodParam));
                                } else {
                                    swaggerParam.setIn(in.toString());
                                }
                            }
                            var style = paramAnn.get(PROP_STYLE, ParameterStyle.class).orElse(ParameterStyle.DEFAULT);
                            if (style != ParameterStyle.DEFAULT) {
                                swaggerParam.setStyle(paramStyle(style, swaggerParam.getIn()));
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
            var apiDecoratorAnn = methodEl.getDeclaredAnnotation(OpenAPIDecorator.class);
            if (apiDecoratorAnn != null) {
                prefix = apiDecoratorAnn.stringValue().orElse(StringUtils.EMPTY_STRING);
                if (prefix.isEmpty()) {
                    prefix = apiDecoratorAnn.stringValue(PROP_OP_ID_PREFIX).orElse(StringUtils.EMPTY_STRING);
                }
                suffix = apiDecoratorAnn.stringValue(PROP_OP_ID_SUFFIX).orElse(StringUtils.EMPTY_STRING);
                addAlways = apiDecoratorAnn.booleanValue(PROP_ADD_ALWAYS).orElse(true);
            } else {
                prefix = ContextUtils.get(MICRONAUT_INTERNAL_CHILD_OP_ID_PREFIX, String.class, StringUtils.EMPTY_STRING, context);
                suffix = ContextUtils.get(MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX, String.class, StringUtils.EMPTY_STRING, context);
                addAlways = ContextUtils.get(MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX_ADD_ALWAYS, Boolean.class, true, context);
            }

            String postfix = StringUtils.EMPTY_STRING;
            if (CollectionUtils.isNotEmpty(pathItem.getExtensions()) && pathItem.getExtensions().containsKey(MICRONAUT_OP_POSTFIX)) {
                postfix = pathItem.getExtensions().get(MICRONAUT_OP_POSTFIX).toString();
            }

            if (StringUtils.isEmpty(swaggerOperation.getOperationId())) {
                swaggerOperation.setOperationId(prefix + methodEl.getName() + postfix + suffix);
            } else if (addAlways) {
                swaggerOperation.setOperationId(prefix + swaggerOperation.getOperationId() + postfix + suffix);
            }

            OperationUtils.addOperation(swaggerOperation.getOperationId(), methodEl);

            if (swaggerOperation.getDescription() != null && swaggerOperation.getDescription().isEmpty()) {
                swaggerOperation.setDescription(null);
            }
            swaggerOperations.put(pathItem, swaggerOperation);
        }
        return swaggerOperations;
    }

    private ExternalDocumentation readExternalDocs(MethodElement element, VisitorContext context) {
        var externalDocsAnn = element.findAnnotation(io.swagger.v3.oas.annotations.ExternalDocumentation.class).orElse(null);
        if (externalDocsAnn == null) {
            return null;
        }
        return toValue(externalDocsAnn.getAnnotationName(), externalDocsAnn.getValues(), context, ExternalDocumentation.class, null);
    }

    private void processExplode(AnnotationValue<io.swagger.v3.oas.annotations.Parameter> paramAnn, Map<CharSequence, Object> paramValues) {
        Optional<Explode> explode = paramAnn.enumValue(PROP_EXPLODE, Explode.class);
        if (explode.isEmpty()) {
            return;
        }
        switch (explode.get()) {
            case TRUE -> paramValues.put(PROP_EXPLODE, Boolean.TRUE);
            case FALSE -> paramValues.put(PROP_EXPLODE, Boolean.FALSE);
            default -> {
                var in = (String) paramValues.get(PROP_IN);
                if (StringUtils.isEmpty(in)) {
                    in = PROP_DEFAULT;
                }
                switch (ParameterIn.valueOf(in.toUpperCase(Locale.ENGLISH))) {
                    case COOKIE, QUERY -> paramValues.put(PROP_EXPLODE, Boolean.TRUE);
                    default -> paramValues.put(PROP_EXPLODE, Boolean.FALSE);
                }
            }
        }
    }

    private void readApiResponses(MethodElement element, VisitorContext context, Operation swaggerOperation, @Nullable ClassElement jsonViewClass) {
        var methodResponseAnns = element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        processResponses(swaggerOperation, methodResponseAnns, element, context, jsonViewClass);

        var classResponseAnns = element.getDeclaringType().getAnnotationValuesByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class);
        processResponses(swaggerOperation, classResponseAnns, element, context, jsonViewClass);
    }

    private void processResponses(Operation operation,
                                  List<AnnotationValue<io.swagger.v3.oas.annotations.responses.ApiResponse>> responseAnns,
                                  MethodElement element, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        ApiResponses apiResponses = operation.getResponses();
        if (apiResponses == null) {
            apiResponses = new ApiResponses();
            operation.setResponses(apiResponses);
        }
        if (CollectionUtils.isEmpty(responseAnns)) {
            return;
        }
        for (var responseAnn : responseAnns) {
            String responseCode = responseAnn.stringValue(PROP_RESPONSE_CODE).orElse("default");
            if (apiResponses.containsKey(responseCode)) {
                continue;
            }
            ApiResponse newApiResponse = toValue(responseAnn.getAnnotationName(), responseAnn.getValues(), context, ApiResponse.class, jsonViewClass);
            if (newApiResponse != null) {
                if (responseAnn.booleanValue("useReturnTypeSchema").orElse(false) && element != null) {
                    addResponseContent(element, context, Utils.resolveOpenApi(context), newApiResponse, jsonViewClass);
                } else {

                    List<MediaType> producesMediaTypes = producesMediaTypes(element);

                    var contentAnns = responseAnn.get(PROP_CONTENT, io.swagger.v3.oas.annotations.media.Content[].class).orElse(null);
                    var mediaTypes = new ArrayList<String>();
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
                        var contentFromProduces = new Content();
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
                        newApiResponse.setDescription(responseCode.equals(PROP_DEFAULT) ? "OK response" : HttpStatus.getDefaultReason(Integer.parseInt(responseCode)));
                    }
                } catch (Exception e) {
                    newApiResponse.setDescription("Response " + responseCode);
                }
                apiResponses.put(responseCode, newApiResponse);
            }
        }
        operation.setResponses(apiResponses);
    }

    // boolean - is swagger schema has implementation
    private Pair<RequestBody, Boolean> readSwaggerRequestBody(Element element, List<MediaType> consumesMediaTypes, VisitorContext context) {
        var requestBodyAnn = element.findAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class).orElse(null);

        if (requestBodyAnn == null) {
            return null;
        }

        boolean hasSchemaImplementation = false;

        var contentAnn = requestBodyAnn.getAnnotation(PROP_CONTENT, io.swagger.v3.oas.annotations.media.Content.class).orElse(null);
        if (contentAnn != null) {
            var schemaAnn = contentAnn.getAnnotation(PROP_SCHEMA, io.swagger.v3.oas.annotations.media.Schema.class).orElse(null);
            if (schemaAnn != null) {
                hasSchemaImplementation = schemaAnn.stringValue(PROP_IMPLEMENTATION).orElse(null) != null;
            }
        }

        var jsonViewClass = element instanceof ParameterElement ? getJsonViewClass(element, context) : null;

        RequestBody requestBody = toValue(requestBodyAnn.getAnnotationName(), requestBodyAnn.getValues(), context, RequestBody.class, jsonViewClass);
        // if media type doesn't set in swagger annotation, check micronaut annotation
        if (contentAnn != null
            && contentAnn.stringValue(PROP_MEDIA_TYPE).isEmpty()
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

    private void readServers(MethodElement element, VisitorContext context, Operation swaggerOperation) {
        for (Server server : methodServers(element, context)) {
            swaggerOperation.addServersItem(server);
        }
    }

    private void readExtensions(MethodElement element, VisitorContext context, Operation swaggerOperation) {
        for (var entry : operationExtensions(element, context).entrySet()) {
            swaggerOperation.addExtension(entry.getKey(), entry.getValue());
        }
    }

    private void readCallbacks(MethodElement element, VisitorContext context,
                               Operation swaggerOperation, @Nullable ClassElement jsonViewClass) {
        var callbacksAnn = element.getAnnotation(Callbacks.class);
        List<AnnotationValue<io.swagger.v3.oas.annotations.callbacks.Callback>> callbackAnns;
        if (callbacksAnn != null) {
            callbackAnns = callbacksAnn.getAnnotations(PROP_VALUE);
        } else {
            callbackAnns = element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.callbacks.Callback.class);
        }
        if (CollectionUtils.isEmpty(callbackAnns)) {
            return;
        }
        for (var callbackAnn : callbackAnns) {
            String callbackName = callbackAnn.stringValue(PROP_NAME).orElse(null);
            if (StringUtils.isEmpty(callbackName)) {
                continue;
            }
            String ref = callbackAnn.stringValue(PROP_REF).orElse(null);
            if (StringUtils.isNotEmpty(ref)) {
                String refCallback = ref.substring(COMPONENTS_CALLBACKS_PREFIX.length());
                processCallbackReference(context, swaggerOperation, callbackName, refCallback);
                continue;
            }
            String expr = callbackAnn.stringValue(PROP_CALLBACK_URL_EXPRESSION).orElse(null);
            if (StringUtils.isNotEmpty(expr)) {
                processUrlCallbackExpression(context, swaggerOperation, callbackAnn, callbackName, expr, jsonViewClass);
            } else {
                processCallbackReference(context, swaggerOperation, callbackName, null);
            }
        }
    }

    private void processCallbackReference(VisitorContext context, Operation swaggerOperation,
                                          String callbackName, String refCallback) {
        Utils.resolveComponents(Utils.resolveOpenApi(context));
        Map<String, Callback> callbacks = initCallbacks(swaggerOperation);
        var callbackRef = new Callback();
        callbackRef.set$ref(refCallback != null ? refCallback : COMPONENTS_CALLBACKS_PREFIX + callbackName);
        callbacks.put(callbackName, callbackRef);
    }

    private void processUrlCallbackExpression(VisitorContext context,
                                              Operation operation, AnnotationValue<io.swagger.v3.oas.annotations.callbacks.Callback> callbackAnn,
                                              String callbackName, final String callbackUrl, @Nullable ClassElement jsonViewClass) {
        var operationAnns = callbackAnn.getAnnotations(PROP_OPERATION, io.swagger.v3.oas.annotations.Operation.class);
        var pathItem = new PathItem();
        if (CollectionUtils.isNotEmpty(operationAnns)) {
            for (var operationAnn : operationAnns) {
                HttpMethod httpMethod = operationAnn.get(PROP_METHOD, HttpMethod.class).orElse(null);
                if (httpMethod == null) {
                    continue;
                }
                var op = toValue(operationAnn.getAnnotationName(), operationAnn.getValues(), context, Operation.class, jsonViewClass);
                if (op != null) {
                    setOperationOnPathItem(pathItem, httpMethod, op);
                }
            }
        }
        Map<String, Callback> callbacks = initCallbacks(operation);
        var callback = new Callback();
        callback.addPathItem(callbackUrl, pathItem);
        callbacks.put(callbackName, callback);
    }

    private Map<String, Callback> initCallbacks(Operation swaggerOperation) {
        Map<String, Callback> callbacks = swaggerOperation.getCallbacks();
        if (callbacks == null) {
            callbacks = new LinkedHashMap<>();
            swaggerOperation.setCallbacks(callbacks);
        }
        return callbacks;
    }

    private Content buildContent(Element definingElement, ClassElement type, List<MediaType> mediaTypes, OpenAPI openApi, VisitorContext context, @Nullable ClassElement jsonViewClass) {
        var content = new Content();
        for (var mediaType : mediaTypes) {
            var mt = new io.swagger.v3.oas.models.media.MediaType();
            mt.setSchema(resolveSchema(openApi, definingElement, type, context, Collections.singletonList(mediaType), jsonViewClass, null, null, null));
            content.addMediaType(mediaType.toString(), mt);
        }
        return content;
    }
}
