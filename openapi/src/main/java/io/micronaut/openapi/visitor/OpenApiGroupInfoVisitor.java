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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PackageElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIGroupInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import javax.annotation.processing.SupportedOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ConvertUtils.toValue;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SECURITY;

/**
 * A {@link TypeElementVisitor} that read the @{@link OpenAPIGroupInfo} annotations at the compile
 * time.
 *
 * @since 4.10.0
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
    public void start(VisitorContext context) {
        Utils.init(context);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void visitClass(ClassElement classEl, VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
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
        var classAnns = classEl.getAnnotationValuesByType(OpenAPIGroupInfo.class);
        var packageAnns = packageEl.getAnnotationValuesByType(OpenAPIGroupInfo.class);
        if (CollectionUtils.isEmpty(classAnns) && CollectionUtils.isEmpty(packageAnns)) {
            return;
        }

        Map<String, OpenAPI> openApis = Utils.getOpenApis();
        if (openApis == null) {
            openApis = new HashMap<>();
            Utils.setOpenApis(openApis);
        }
        addOpenApis(packageAnns, openApis, classEl, context);
        addOpenApis(classAnns, openApis, classEl, context);
    }

    private void addOpenApis(List<AnnotationValue<OpenAPIGroupInfo>> groupInfoAnns, Map<String, OpenAPI> openApis, ClassElement classEl, VisitorContext context) {
        if (CollectionUtils.isEmpty(groupInfoAnns)) {
            return;
        }

        for (var groupInfoAnn : groupInfoAnns) {
            var openApiAnn = groupInfoAnn.getAnnotation("info", OpenAPIDefinition.class).orElse(null);
            if (openApiAnn == null) {
                continue;
            }
            var openApi = toValue(openApiAnn.getValues(), context, OpenAPI.class);
            if (openApi == null) {
                continue;
            }
            var securityRequirementAnns = openApiAnn.getAnnotations(PROP_SECURITY, io.swagger.v3.oas.annotations.security.SecurityRequirement.class);
            var securityRequirements = new ArrayList<SecurityRequirement>();
            for (var securityRequirementAnn : securityRequirementAnns) {
                securityRequirements.add(ConvertUtils.mapToSecurityRequirement(securityRequirementAnn));
            }
            openApi.setSecurity(securityRequirements);

            var securitySchemeAnns = groupInfoAnn.getAnnotations("securitySchemes", SecurityScheme.class);
            if (CollectionUtils.isNotEmpty(securitySchemeAnns)) {
                ConvertUtils.addSecuritySchemes(openApi, securitySchemeAnns, context);
            }

            for (var groupName : groupInfoAnn.stringValues("names")) {
                openApis.put(groupName, openApi);
            }
        }
    }
}
