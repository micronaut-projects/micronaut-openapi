/*
 * Copyright 2017-2024 original authors
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

import io.micronaut.core.annotation.Internal;

/**
 * Supported openapi generator extensions.
 *
 * @since 4.13.0
 */
@Internal
public interface GeneratorExt {

    String TYPE = "x-type";
    String FORMAT = "x-format";
    String ENUM_VAR_NAMES = "x-enum-varnames";
    String ENUM_DESCRIPTIONS = "x-enum-descriptions";
    String ENUM_DEPRECATED = "x-deprecated";
    String DEPRECATED_MESSAGE = "x-deprecated-message";
    String MIN_MESSAGE = "x-min-message";
    String MAX_MESSAGE = "x-max-message";
    String SIZE_MESSAGE = "x-size-message";
    String PATTERN_MESSAGE = "x-pattern-message";
    String NOT_NULL_MESSAGE = "x-not-null-message";
}
