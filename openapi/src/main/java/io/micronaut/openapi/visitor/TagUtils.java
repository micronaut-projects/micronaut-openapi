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
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.javadoc.JavadocDescription;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.micronaut.core.naming.NameUtils.camelCase;
import static io.micronaut.openapi.visitor.ConfigUtils.getTagGenerationDescriptionMaxLength;
import static io.micronaut.openapi.visitor.ConfigUtils.getTagGenerationNamingStrategy;
import static io.micronaut.openapi.visitor.ConfigUtils.getTagGenerationRemovePostfixes;
import static io.micronaut.openapi.visitor.ConfigUtils.getTagGenerationRemovePrefixes;
import static io.micronaut.openapi.visitor.ConfigUtils.isTagGenerationByClassEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isTagGenerationByPackageEnabled;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCOPES;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.toValue;
import static io.micronaut.openapi.visitor.StringUtil.DOT;
import static io.micronaut.openapi.visitor.StringUtil.UNDERSCORE;
import static io.micronaut.openapi.visitor.Utils.resolveTags;

/**
 * OpenAPI tag utilities.
 *
 * @since 6.15.1
 */
@Internal
public final class TagUtils {

    /**
     * Converts annotation to model.
     *
     * @param <T> The model type.
     * @param <A> The annotation type.
     * @param element The element to process.
     * @param context The context.
     * @param annotationType The annotation type.
     * @param modelType The model type.
     * @param tagList The initial list of models.
     *
     * @return A list of model objects.
     */
    public static <T, A extends Annotation> List<T> processOpenApiAnnotation(Element element, VisitorContext context, Class<A> annotationType, Class<T> modelType, List<T> tagList) {
        List<AnnotationValue<A>> annotations = element.getAnnotationValuesByType(annotationType);
        if (CollectionUtils.isEmpty(tagList)) {
            tagList = new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(annotations)) {
            return tagList;
        }
        for (AnnotationValue<A> tag : annotations) {
            Map<CharSequence, Object> values;
            var tagValues = tag.getValues();
            if (tag.getAnnotationName().equals(io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName())
                && !tagValues.isEmpty()) {
                Object name = tagValues.get(PROP_NAME);
                Object scopes = tagValues.computeIfAbsent(PROP_SCOPES, (key) -> new ArrayList<String>());
                values = Collections.singletonMap((CharSequence) name, scopes);
            } else {
                values = tagValues;
            }
            T tagObj = toValue(tag.getAnnotationName(), values, context, modelType, null);
            if (tagObj != null) {
                // skip all existed tags
                boolean alreadyExists = false;
                if (CollectionUtils.isNotEmpty(tagList) && tag.getAnnotationName().equals(io.swagger.v3.oas.annotations.tags.Tag.class.getName())) {
                    var newTagName = ((Tag) tagObj).getName();
                    for (T existedTag : tagList) {
                        if (((Tag) existedTag).getName().equals(newTagName)) {
                            alreadyExists = true;
                            break;
                        }
                    }
                }
                if (!alreadyExists) {
                    tagList.add(tagObj);
                }
            }
        }
        return tagList;
    }

    public static void readTags(MethodElement element, VisitorContext context, Operation swaggerOperation, List<Tag> classTags, OpenAPI openApi) {
        element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.tags.Tag.class)
            .forEach(av -> av.stringValue(PROP_NAME)
                .ifPresent(swaggerOperation::addTagsItem));

        var copyTags = openApi.getTags() != null ? new ArrayList<>(openApi.getTags()) : null;
        var operationTags = processOpenApiAnnotation(element, context, io.swagger.v3.oas.annotations.tags.Tag.class, Tag.class, copyTags);
        // find not simple tags (tags with description or other information), such fields need to be described at the openAPI level.
        List<Tag> complexTags = null;
        if (CollectionUtils.isNotEmpty(operationTags)) {
            complexTags = new ArrayList<>();
            for (Tag operationTag : operationTags) {
                if (StringUtils.hasText(operationTag.getDescription())
                    || CollectionUtils.isNotEmpty(operationTag.getExtensions())
                    || operationTag.getExternalDocs() != null) {
                    complexTags.add(operationTag);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(complexTags)) {
            if (CollectionUtils.isEmpty(openApi.getTags())) {
                openApi.setTags(complexTags);
            } else {
                for (Tag complexTag : complexTags) {
                    // skip all existed tags
                    boolean alreadyExists = false;
                    for (Tag apiTag : openApi.getTags()) {
                        if (apiTag.getName().equals(complexTag.getName())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        openApi.getTags().add(complexTag);
                    }
                }
            }
        }

        // only way to get inherited tags
        element.getValues(Tags.class, AnnotationValue.class)
            .forEach((k, v) -> v.stringValue(PROP_NAME).ifPresent(name -> addTagIfNotPresent((String) name, swaggerOperation)));

        classTags.forEach(tag -> addTagIfNotPresent(tag.getName(), swaggerOperation));
        if (CollectionUtils.isNotEmpty(swaggerOperation.getTags())) {
            swaggerOperation.getTags().sort(Comparator.naturalOrder());
        }
    }

    public static void addTagIfNotPresent(String tag, Operation swaggerOperation) {
        List<String> tags = swaggerOperation.getTags();
        if (tags == null || !tags.contains(tag)) {
            swaggerOperation.addTagsItem(tag);
        }
    }

    public static List<Tag> readTags(ClassElement element, VisitorContext context) {
        return readTags(element.getAnnotationValuesByType(io.swagger.v3.oas.annotations.tags.Tag.class), context);
    }

    public static List<Tag> readTags(List<AnnotationValue<io.swagger.v3.oas.annotations.tags.Tag>> tagAnns, VisitorContext context) {
        var tags = new ArrayList<Tag>();
        for (var tagAnn : tagAnns) {
            var tag = toValue(tagAnn.getAnnotationName(), tagAnn.getValues(), context, Tag.class, null);
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * Generating tags by class name or/and by package name.
     *
     * @param element element
     * @param classDescription custom class description
     * @param context visitor context
     * @return generated tags by controller name or/and by package class name
     */
    public static List<Tag> generationTags(ClassElement element, String classDescription, VisitorContext context) {

        Tag tagByClass = null;
        if (isTagGenerationByClassEnabled(context)) {
            tagByClass = generateTag(element.getSimpleName(), StringUtils.isNotEmpty(classDescription) ? classDescription : element.getDocumentation().orElse(null), false, context);
        }
        Tag tagByPackage = null;
        if (isTagGenerationByPackageEnabled(context)) {
            tagByPackage = generateTag(element.getPackageName(), element.getPackage().getDocumentation().orElse(null), true, context);
        }
        if (tagByClass == null && tagByPackage == null) {
            return Collections.emptyList();
        }

        var globalTags = resolveTags(Utils.resolveOpenApi(context));

        var operationTags = new ArrayList<Tag>();
        addIfDoesntExist(tagByClass, operationTags, globalTags);
        addIfDoesntExist(tagByPackage, operationTags, globalTags);

        return operationTags;
    }

    private static void addIfDoesntExist(Tag tag, List<Tag> operationTags, List<Tag> globalTags) {
        if (tag == null) {
            return;
        }
        var opTagAlreadyExists = false;
        for (var opTag : operationTags) {
            if (opTag.getName().equals(tag.getName())) {
                opTagAlreadyExists = true;
                break;
            }
        }
        if (!opTagAlreadyExists) {
            operationTags.add(new Tag().name(tag.getName()));
        }

        // Don't need to create global tag, if it doesn't have description
        if (StringUtils.isEmpty(tag.getDescription())) {
            return;
        }

        var globalTagAlreadyExists = false;
        for (var globalTag : globalTags) {
            if (globalTag.getName().equals(tag.getName())) {
                globalTagAlreadyExists = true;
                break;
            }
        }
        if (!globalTagAlreadyExists) {
            globalTags.add(tag);
        }
    }

    private static Tag generateTag(String elementName, String javadocStr, boolean isPackageName, VisitorContext context) {

        var tagName = elementName;
        var tagNameLower = tagName.toLowerCase(Locale.ENGLISH);

        var prefixes = getTagGenerationRemovePrefixes(context);
        if (CollectionUtils.isNotEmpty(prefixes)) {
            for (var prefix : prefixes) {
                if (prefix.isEmpty()) {
                    continue;
                }
                if (tagNameLower.startsWith(prefix)) {
                    tagName = tagName.substring(prefix.length());
                    break;
                }
            }
        }
        tagNameLower = tagName.toLowerCase(Locale.ENGLISH);

        var postfixes = getTagGenerationRemovePostfixes(context);
        if (CollectionUtils.isNotEmpty(postfixes)) {
            for (var postfix : postfixes) {
                if (postfix.isEmpty()) {
                    continue;
                }
                if (tagNameLower.endsWith(postfix)) {
                    tagName = tagName.substring(0, tagNameLower.indexOf(postfix));
                    break;
                }
            }
        }

        var namingStrategy = getTagGenerationNamingStrategy(context);
        if (namingStrategy != null) {
            // need to camelize package name. Example: "user.operations" -> "userOperations"
            if (isPackageName) {
                tagName = camelCase(tagName.replace(DOT, UNDERSCORE));
            }
            tagName = namingStrategy.nameForField(null, null, tagName);
        }

        var tag = new Tag()
            .name(tagName);
        JavadocDescription javadoc = Utils.getJavadocParser().parse(javadocStr);
        if (javadoc != null) {
            var descFromJavaDoc = StringUtils.isNotEmpty(javadoc.getMethodSummary()) ? javadoc.getMethodSummary() : javadoc.getMethodDescription();
            if (StringUtils.isNotEmpty(descFromJavaDoc)) {
                var descriptionLength = getTagGenerationDescriptionMaxLength(context);
                if (descriptionLength > 0) {
                    descFromJavaDoc = StringUtil.left(descFromJavaDoc, descriptionLength);
                }
                tag.description(descFromJavaDoc);
            }
        }

        return tag;
    }
}
