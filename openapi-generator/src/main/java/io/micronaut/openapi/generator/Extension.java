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

/**
 * OpenAPI extensions, supported by Micronaut OpenAPI Generator.
 *
 * @since 6.20.0
 */
@SuppressWarnings("checkstyle:MissingJavadocType")
public interface Extension {

    String EXT_HTTP_RESPONSE_WRAPPER = "x-http-response-wrapper";
    String EXT_NOT_NULL = "x-not-null";
    String EXT_DEPRECATED_MESSAGE = "x-deprecated-message";
    String EXT_DEPRECATED = "x-deprecated";
    String EXT_FORMAT = "x-format";
    String EXT_TYPE = "x-type";
    String EXT_ROLES = "x-roles";
    String EXT_ENUM_DESCRIPTIONS = "x-enum-descriptions";
    String EXT_ENUM_DEPRECATED_MESSAGES = "x-enum-deprecated-messages";
    String EXT_ENUM_VAR_NAMES = "x-enum-varnames";

    String EXT_ANNOTATIONS_OPERATION = "x-operation-extra-annotation";
    String EXT_ANNOTATIONS_CLASS = "x-class-extra-annotation";
    String EXT_ANNOTATIONS_FIELD = "x-field-extra-annotation";
    String EXT_ANNOTATIONS_SETTER = "x-setter-extra-annotation";

    String EXT_PATTERN_MESSAGE = "x-pattern-message";
    String EXT_SIZE_MESSAGE = "x-size-message";
    String EXT_NOT_NULL_MESSAGE = "x-not-null-message";
    String EXT_MINIMUM_MESSAGE = "x-minimum-message";
    String EXT_MAXIMUM_MESSAGE = "x-maximum-message";

}
