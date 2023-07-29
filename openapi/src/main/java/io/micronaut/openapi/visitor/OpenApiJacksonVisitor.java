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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;

/**
 * A {@link TypeElementVisitor} that builds appropriate {@link Schema} annotation for the parent class of a hierarchy
 * when using Jackson {@link JsonTypeInfo} and {@link JsonSubTypes}.
 *
 * @author Iván López
 * @since 3.0.0
 */
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiJacksonVisitor implements TypeElementVisitor<Object, Object> {

    @Override
    @NonNull
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return CollectionUtils.setOf(
                "com.fasterxml.jackson.annotation.JsonSubTypes",
                "com.fasterxml.jackson.annotation.JsonTypeInfo"
        );
    }

    @Override
    public int getOrder() {
        return -10;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context)) {
            return;
        }
        AnnotationValue<JsonSubTypes> jsonSubTypesDecAnn = element.getDeclaredAnnotation(JsonSubTypes.class);
        AnnotationValue<JsonTypeInfo> jsonTypeInfoDecAnn = element.getDeclaredAnnotation(JsonTypeInfo.class);
        AnnotationValue<Schema> schemaAnn = element.getDeclaredAnnotation(Schema.class);

        /*
        Given the following annotations:
            @JsonSubTypes({
                @JsonSubTypes.Type(name = "CAT", value = Cat.class),
                @JsonSubTypes.Type(name = "DOG", value = Dog.class)
            })
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)

        This visitor will add the following @Schema (if no previous @Schema already exists):
            @Schema(
                oneOf = {Cat.class, Dog.class},
                discriminatorMapping = {
                    @DiscriminatorMapping(value = "CAT", schema = Cat.class),
                    @DiscriminatorMapping(value = "DOG", schema = Dog.class)
                },
                discriminatorProperty = "type"
            )
         */
        if (jsonTypeInfoDecAnn != null && jsonSubTypesDecAnn != null && schemaAnn == null) {
            JsonTypeInfo.Id use = jsonTypeInfoDecAnn.enumValue("use", JsonTypeInfo.Id.class).orElse(null);
            String discriminatorProp = jsonTypeInfoDecAnn.stringValue("property").orElse(null);
            if (use != JsonTypeInfo.Id.NAME || discriminatorProp == null) {
                return;
            }

            List<AnnotationClassValue<?>> discriminatorClasses = new ArrayList<>();
            List<AnnotationValue<DiscriminatorMapping>> discriminatorMappings = new ArrayList<>();
            for (AnnotationValue<Annotation> av : jsonSubTypesDecAnn.getAnnotations("value")) {
                AnnotationClassValue<?> mappingClass = av.annotationClassValue("value").orElse(null);
                String subTypeName = av.stringValue("name").orElse(null);
                if (mappingClass != null && subTypeName != null) {
                    AnnotationValue<DiscriminatorMapping> discriminatorMapping = AnnotationValue.builder(DiscriminatorMapping.class)
                            .member("value", subTypeName)
                            .member("schema", mappingClass)
                            .build();
                    discriminatorMappings.add(discriminatorMapping);
                    discriminatorClasses.add(mappingClass);
                }
            }

            element.annotate(Schema.class, builder -> {
                builder.member("oneOf", discriminatorClasses.toArray(new AnnotationClassValue[0]));
                builder.member("discriminatorProperty", discriminatorProp);
                builder.member("discriminatorMapping", discriminatorMappings.toArray(new AnnotationValue<?>[0]));
            });
        }
    }
}
