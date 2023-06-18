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
import java.util.List;
import java.util.Map;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PackageElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIGroupInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;

import static io.micronaut.openapi.visitor.ConvertUtils.toValue;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.isOpenApiEnabled;

/**
 * @since 4.9.2
 */
@Internal
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiGroupInfoVisitor implements TypeElementVisitor<Object, Object> {

    private List<String> groups;
    private List<String> groupsExcluded;

    public OpenApiGroupInfoVisitor() {
    }

    public OpenApiGroupInfoVisitor(List<String> groups, List<String> groupsExcluded) {
        this.groups = groups;
        this.groupsExcluded = groupsExcluded;
    }

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

        if (CollectionUtils.isNotEmpty(groups)) {
            Map<String, List<String>> includedClassesGroups = Utils.getIncludedClassesGroups();
            if (includedClassesGroups == null) {
                includedClassesGroups = new HashMap<>();
                Utils.setIncludedClassesGroups(includedClassesGroups);
            }
            includedClassesGroups.put(classEl.getName(), groups);
        }

        if (CollectionUtils.isNotEmpty(groupsExcluded)) {
            Map<String, List<String>> includedClassesGroupsExcluded = Utils.getIncludedClassesGroupsExcluded();
            if (includedClassesGroupsExcluded == null) {
                includedClassesGroupsExcluded = new HashMap<>();
                Utils.setIncludedClassesGroupsExcluded(includedClassesGroupsExcluded);
            }
            includedClassesGroupsExcluded.put(classEl.getName(), groupsExcluded);
        }

        PackageElement packageEl = classEl.getPackage();
        List<AnnotationValue<OpenAPIGroupInfo>> classAnnotations = classEl.getAnnotationValuesByType(OpenAPIGroupInfo.class);
        List<AnnotationValue<OpenAPIGroupInfo>> packageAnnotations = packageEl.getAnnotationValuesByType(OpenAPIGroupInfo.class);
        if (CollectionUtils.isEmpty(classAnnotations) && CollectionUtils.isEmpty(packageAnnotations)) {
            return;
        }

        Map<String, OpenAPI> openApis = Utils.getOpenApis();
        if (openApis == null) {
            openApis = new HashMap<>();
            Utils.setOpenApis(openApis);
        }
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
}
