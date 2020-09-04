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
package io.micronaut.openapi.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.TypedAnnotationTransformer;
import io.micronaut.inject.visitor.VisitorContext;

/**
 * Changes the Retention Policy of the annotation to SOURCE.
 *
 * @param <T> The annotation type.
 * @since 2.1
 * @author croudet
 */
abstract class AbstractRetentionPolicyAnnotationTransformer<T extends Annotation> implements TypedAnnotationTransformer<T> {
    private final Class<T> type;

    /**
     * Changes the Retention Policy of the annotation to SOURCE.
     *
     * @param type The annotation Type.
     */
    AbstractRetentionPolicyAnnotationTransformer(Class<T> type) {
        this.type = type;
    }

    @Override
    public List<AnnotationValue<?>> transform(AnnotationValue<T> annotation, VisitorContext visitorContext) {
        return Collections.singletonList(
                              AnnotationValue.builder(annotation, RetentionPolicy.SOURCE).build());
    }

    @Override
    public Class<T> annotationType() {
        return type;
    }

}
