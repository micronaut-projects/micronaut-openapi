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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.processing.SupportedOptions;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Set;

import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DISCRIMINATOR_MAPPING;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DISCRIMINATOR_PROPERTY;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ONE_OF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_VALUE;

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
    public void start(VisitorContext context) {
        Utils.init(context);
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
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
            return;
        }
        var jsonSubTypesDecAnn = element.getDeclaredAnnotation(JsonSubTypes.class);
        var jsonTypeInfoDecAnn = element.getDeclaredAnnotation(JsonTypeInfo.class);
        var schemaAnn = element.getDeclaredAnnotation(Schema.class);

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
                discriminatorMappingAnn = {
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

            var discriminatorClasses = new ArrayList<AnnotationClassValue<?>>();
            var discriminatorMappingAnns = new ArrayList<AnnotationValue<DiscriminatorMapping>>();
            for (AnnotationValue<Annotation> av : jsonSubTypesDecAnn.getAnnotations(PROP_VALUE)) {
                AnnotationClassValue<?> mappingClass = av.annotationClassValue(PROP_VALUE).orElse(null);
                String subTypeName = av.stringValue(PROP_NAME).orElse(null);
                if (mappingClass != null && subTypeName != null) {
                    var discriminatorMappingAnn = AnnotationValue.builder(DiscriminatorMapping.class)
                        .member(PROP_VALUE, subTypeName)
                        .member(PROP_SCHEMA, mappingClass)
                        .build();
                    discriminatorMappingAnns.add(discriminatorMappingAnn);
                    discriminatorClasses.add(mappingClass);
                }
            }

            element.annotate(Schema.class, builder -> {
                builder.member(PROP_ONE_OF, discriminatorClasses.toArray(new AnnotationClassValue[0]));
                builder.member(PROP_DISCRIMINATOR_PROPERTY, discriminatorProp);
                builder.member(PROP_DISCRIMINATOR_MAPPING, discriminatorMappingAnns.toArray(new AnnotationValue<?>[0]));
            });
        }
    }
}
