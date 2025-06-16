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
package io.micronaut.openapi.visitor.management;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.List;

import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS;
import static io.micronaut.openapi.visitor.ContextUtils.get;

/**
 * Utilities to interpret spring-actuator endpoints.
 *
 * @since 6.16.3
 */
@Internal
public final class SpringActuatorUtils {

    public static final List<String> PROMETHEUS_CONTENT_TYPES = List.of(
        "text/plain; version=0.0.4; charset=utf-8",
        "application/openmetrics-text; version=1.0.0; charset=utf-8",
        "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited"
    );

    public static List<String> getProducesFrom(VisitorContext context) {
        if (get(MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS, Boolean.class, false, context)) {
            return PROMETHEUS_CONTENT_TYPES;
        }
        return null;
    }
}
