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
package io.micronaut.openapi.mapper;

/**
 * The kinds of mapper the OpenAPI visitor needs.
 *
 * @since 8.0.0
 */
public enum OpenApiMapperType {

    /**
     * Strict JSON mapper used to read and write OpenAPI documents in JSON.
     */
    JSON,

    /**
     * YAML mapper used to read and write OpenAPI documents in YAML.
     */
    YAML,

    /**
     * Lenient JSON mapper used to convert annotation values and partial trees into OpenAPI model objects.
     */
    CONVERT
}
