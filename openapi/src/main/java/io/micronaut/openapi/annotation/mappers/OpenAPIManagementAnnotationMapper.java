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
package io.micronaut.openapi.annotation.mappers;

import java.util.Collections;
import java.util.List;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.TypedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.openapi.annotation.OpenAPIManagement;

import static io.micronaut.openapi.visitor.ElementUtils.EMPTY_ANNOTATION_VALUES_ARRAY;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SECURITY;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_TAGS;

/**
 * Mapper for management endpoints.
 */
public class OpenAPIManagementAnnotationMapper implements TypedAnnotationMapper<OpenAPIManagement> {

    @Override
    public Class<OpenAPIManagement> annotationType() {
        return OpenAPIManagement.class;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<OpenAPIManagement> annotation, VisitorContext context) {
        return Collections.singletonList(
            AnnotationValue.builder(OpenAPIInclude.class)
                .values(
                    "io.micronaut.management.endpoint.beans.BeansEndpoint",
                    "io.micronaut.management.endpoint.env.EnvironmentEndpoint",
                    "io.micronaut.management.endpoint.health.HealthEndpoint",
                    "io.micronaut.management.endpoint.info.InfoEndpoint",
                    "io.micronaut.management.endpoint.loggers.LoggersEndpoint",
                    "io.micronaut.management.endpoint.refresh.RefreshEndpoint",
                    "io.micronaut.management.endpoint.routes.RoutesEndpoint",
                    "io.micronaut.management.endpoint.stop.ServerStopEndpoint",
                    "io.micronaut.management.endpoint.threads.ThreadDumpEndpoint"
                )
                .member(PROP_TAGS, annotation.getAnnotations(PROP_TAGS).toArray(EMPTY_ANNOTATION_VALUES_ARRAY))
                .member(PROP_SECURITY, annotation.getAnnotations(PROP_SECURITY).toArray(EMPTY_ANNOTATION_VALUES_ARRAY))
                .build()
        );
    }
}
