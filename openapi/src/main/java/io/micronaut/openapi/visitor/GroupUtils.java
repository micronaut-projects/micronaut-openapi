/*
 * Copyright 2017-2025 original authors
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
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PackageElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import io.micronaut.openapi.visitor.group.EndpointGroupInfo;
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.RouterVersioningProperties;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.models.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.micronaut.openapi.visitor.ConfigUtils.getGroupsPropertiesMap;
import static io.micronaut.openapi.visitor.ConfigUtils.getRouterVersioningProperties;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXCLUDE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXTENSIONS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAMES;
import static io.micronaut.openapi.visitor.ParamUtils.addVersionParameters;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;
import static io.micronaut.openapi.visitor.Utils.DEFAULT_MEDIA_TYPES;

/**
 * OpenAPI Groups util methods.
 *
 * @since 6.16.0
 */
@Internal
public final class GroupUtils {

    private GroupUtils() {
    }

    public static void processMicronautVersionAndGroup(Operation swaggerOperation, String url,
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

        processGroupsFromIncludedEndpoints(groups, excludedGroups, classEl.getName());

        // properties from system properties or from environment more priority than annotations
        for (GroupProperties groupProperties : groupPropertiesMap.values()) {
            if (CollectionUtils.isNotEmpty(groupProperties.getPackages())) {
                for (GroupProperties.PackageProperties groupPackage : groupProperties.getPackages()) {
                    boolean isInclude = groupPackage.isIncludeSubpackages() ? packageName.startsWith(groupPackage.getName()) : packageName.equals(groupPackage.getName());
                    if (isInclude) {
                        groups.put(groupProperties.getName(), new EndpointGroupInfo(groupProperties.getName()));
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(groupProperties.getPackagesExclude())) {
                for (GroupProperties.PackageProperties excludePackage : groupProperties.getPackagesExclude()) {
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

            List<AnnotationValue<Version>> versionAnns = methodEl.getAnnotationValuesByType(Version.class);
            if (CollectionUtils.isNotEmpty(versionAnns)) {
                version = versionAnns.get(0).stringValue().orElse(null);
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
            excludedGroups
        ));
    }

    public static void processGroups(Map<String, EndpointGroupInfo> groups,
                                     List<String> excludedGroups,
                                     List<AnnotationValue<OpenAPIGroup>> annotationValues,
                                     Map<String, GroupProperties> groupPropertiesMap) {
        if (CollectionUtils.isEmpty(annotationValues)) {
            return;
        }
        for (AnnotationValue<OpenAPIGroup> groupAnn : annotationValues) {
            excludedGroups.addAll(List.of(groupAnn.stringValues(PROP_EXCLUDE)));

            var extensionAnns = groupAnn.getAnnotations(PROP_EXTENSIONS);
            var groupNames = groupAnn.stringValues();
            if (ArrayUtils.isEmpty(groupNames)) {
                groupNames = groupAnn.stringValues(PROP_NAMES);
            }
            for (var groupName : groupNames) {
                var extensions = new HashMap<String, Object>();
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

    public static void processGroupsFromIncludedEndpoints(Map<String, EndpointGroupInfo> groups, List<String> excludedGroups, String className) {
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
}
