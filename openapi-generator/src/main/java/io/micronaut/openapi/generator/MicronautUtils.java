/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.openapi.generator;

import org.jspecify.annotations.Nullable;

/**
 * Utilities methods for micronaut generator.
 *
 * @since 6.20.0
 */
public final class MicronautUtils {

    public static final String MONO_CLASS_NAME = "reactor.core.publisher.Mono";
    public static final String FLUX_CLASS_NAME = "reactor.core.publisher.Flux";
    public static final String HTTP_STATUS_CLASS_NAME = "io.micronaut.http.HttpStatus";
    public static final String STATUS_ANNOTATION_CLASS_NAME = "io.micronaut.http.annotation.Status";

    @Nullable
    public static String httpStatusConstName(String code) {
        return switch (code) {
            case "200" -> "OK";
            case "201" -> "CREATED";
            case "202" -> "ACCEPTED";
            case "203" -> "NON_AUTHORITATIVE_INFORMATION";
            case "204" -> "NO_CONTENT";
            case "205" -> "RESET_CONTENT";
            case "206" -> "PARTIAL_CONTENT";
            case "207" -> "MULTI_STATUS";
            case "208" -> "ALREADY_IMPORTED";
            case "226" -> "IM_USED";
            default -> null;
        };
    }
}
