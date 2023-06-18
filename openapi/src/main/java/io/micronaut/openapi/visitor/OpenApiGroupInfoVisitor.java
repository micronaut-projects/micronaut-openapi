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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PackageElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIGroupInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import static io.micronaut.openapi.visitor.ConvertUtils.parseJsonString;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;

/**
 * @since 4.9.2
 */
@Internal
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiGroupInfoVisitor implements TypeElementVisitor<Object, Object> {

    @Override
    @NonNull
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void visitClass(ClassElement classEl, VisitorContext context) {
        if (!isOpenApiEnabled(context)) {
            return;
        }

        PackageElement packageEl = classEl.getPackage();
        List<AnnotationValue<OpenAPIGroupInfo>> classAnnotations = classEl.getAnnotationValuesByType(OpenAPIGroupInfo.class);
        List<AnnotationValue<OpenAPIGroupInfo>> packageAnnotations = packageEl.getAnnotationValuesByType(OpenAPIGroupInfo.class);
        if (CollectionUtils.isEmpty(classAnnotations) && CollectionUtils.isEmpty(packageAnnotations)) {
            return;
        }

        Map<String, OpenAPI> openApis = Utils.getOpenApis();
        addOpenApis(packageAnnotations, openApis, classEl, context);
        addOpenApis(classAnnotations, openApis, classEl, context);
    }

    private void addOpenApis(List<AnnotationValue<OpenAPIGroupInfo>> annotationValues, Map<String, OpenAPI> openApis, ClassElement classEl, VisitorContext context) {
        if (CollectionUtils.isEmpty(annotationValues)) {
            return;
        }

        for (AnnotationValue<OpenAPIGroupInfo> infoAnn : annotationValues) {
            AnnotationValue<OpenAPIDefinition> openApiAnn = infoAnn.getAnnotation("info", OpenAPIDefinition.class).orElse(null);
            if (openApiAnn == null) {
                continue;
            }
            OpenAPI openApi = toValue(openApiAnn.getValues(), context, OpenAPI.class);
            if (openApi == null) {
                continue;
            }
            List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> securityRequirementAnns =
                openApiAnn.getAnnotations("security", io.swagger.v3.oas.annotations.security.SecurityRequirement.class);
            List<io.swagger.v3.oas.models.security.SecurityRequirement> securityRequirements = new ArrayList<>();
            for (AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement> securityRequirementAnn : securityRequirementAnns) {
                securityRequirements.add(ConvertUtils.mapToSecurityRequirement(securityRequirementAnn));
            }
            openApi.setSecurity(securityRequirements);

            for (String groupName : infoAnn.stringValues("names")) {
                openApis.put(groupName, openApi);
            }
        }
    }

    /**
     * Convert the given Map to a JSON node and then to the specified type.
     *
     * @param <T> The output class type
     * @param values The values
     * @param context The visitor context
     * @param type The class
     *
     * @return The converted instance
     */
    private <T> T toValue(Map<CharSequence, Object> values, VisitorContext context, Class<T> type) {
        JsonNode node = toJson(values, context);
        try {
            return ConvertUtils.treeToValue(node, type, context);
        } catch (JsonProcessingException e) {
            context.warn("Error converting  [" + node + "]: to " + type + ": " + e.getMessage(), null);
        }
        return null;
    }

    /**
     * Convert the given map to a JSON node.
     *
     * @param values The values
     * @param context The visitor context
     *
     * @return The node
     */
    private JsonNode toJson(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = toValueMap(values, context);
        return ConvertUtils.getJsonMapper().valueToTree(newValues);
    }

    private Map<CharSequence, Object> toValueMap(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = new HashMap<>(values.size());
        for (Map.Entry<CharSequence, Object> entry : values.entrySet()) {
            CharSequence key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AnnotationValue) {
                AnnotationValue<?> av = (AnnotationValue<?>) value;
                final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                newValues.put(key, valueMap);
            } else if (value instanceof AnnotationClassValue) {
                AnnotationClassValue<?> acv = (AnnotationClassValue<?>) value;
                acv.getType().ifPresent(aClass -> newValues.put(key, aClass));
            } else if (value != null) {
                if (value.getClass().isArray()) {
                    Object[] a = (Object[]) value;
                    if (ArrayUtils.isNotEmpty(a)) {
                        Object first = a[0];
                        boolean areAnnotationValues = first instanceof AnnotationValue;
                        boolean areClassValues = first instanceof AnnotationClassValue;

                        if (areClassValues) {
                            List<Class<?>> classes = new ArrayList<>(a.length);
                            for (Object o : a) {
                                AnnotationClassValue<?> acv = (AnnotationClassValue<?>) o;
                                acv.getType().ifPresent(classes::add);
                            }
                            newValues.put(key, classes);
                        } else if (areAnnotationValues) {
                            String annotationName = ((AnnotationValue<?>) first).getAnnotationName();
                            if (io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName().equals(annotationName)) {
                                List<SecurityRequirement> securityRequirements = new ArrayList<>(a.length);
                                for (Object o : a) {
                                    securityRequirements.add(ConvertUtils.mapToSecurityRequirement((AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>) o));
                                }
                                newValues.put(key, securityRequirements);
                            } else if (Extension.class.getName().equals(annotationName)) {
                                Map<CharSequence, Object> extensions = new HashMap<>();
                                for (Object o : a) {
                                    processExtensions(extensions, (AnnotationValue<Extension>) o);
                                }
                                newValues.put("extensions", extensions);
                            } else if (Server.class.getName().equals(annotationName)) {
                                List<Map<CharSequence, Object>> servers = new ArrayList<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    Map<CharSequence, Object> variables = new LinkedHashMap<>(toValueMap(sv.getValues(), context));
                                    servers.add(variables);
                                }
                                newValues.put(key, servers);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                Map<String, Map<CharSequence, Object>> variables = new LinkedHashMap<>();
                                for (Object o : a) {
                                    AnnotationValue<ServerVariable> sv = (AnnotationValue<ServerVariable>) o;
                                    sv.stringValue("name").ifPresent(name -> {
                                        Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                        Object dv = map.get("defaultValue");
                                        if (dv != null) {
                                            map.put("default", dv);
                                        }
                                        if (map.containsKey("allowableValues")) {
                                            // The key in the generated openapi needs to be "enum"
                                            map.put("enum", map.remove("allowableValues"));
                                        }
                                        variables.put(name, map);
                                    });
                                }
                                newValues.put(key, variables);
                            } else {
                                if (a.length == 1) {
                                    final AnnotationValue<?> av = (AnnotationValue<?>) a[0];
                                    final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                                    newValues.put(key, toValueMap(valueMap, context));
                                } else {

                                    List<Object> list = new ArrayList<>();
                                    for (Object o : a) {
                                        if (o instanceof AnnotationValue) {
                                            final AnnotationValue<?> av = (AnnotationValue<?>) o;
                                            final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                                            list.add(valueMap);
                                        } else {
                                            list.add(o);
                                        }
                                    }
                                    newValues.put(key, list);
                                }
                            }
                        } else {
                            newValues.put(key, value);
                        }
                    } else {
                        newValues.put(key, a);
                    }
                } else {
                    newValues.put(key, parseJsonString(value).orElse(value));
                }
            }
        }
        return newValues;
    }
}
