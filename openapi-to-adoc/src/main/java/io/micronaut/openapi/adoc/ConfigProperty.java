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
package io.micronaut.openapi.adoc;

/**
 * Configuration properties for Openapi-to-adoc converter.
 *
 * @since 5.2.0
 */
public interface ConfigProperty {

    /**
     * Prefix for custom sub-template names.
     */
    String OPENAPI_ADOC_TEMPLATE_PREFIX = "io.micronaut.openapi.adoc.template.";

    /**
     * Custom template directory.
     */
    String OPENAPI_ADOC_TEMPLATES_DIR_PATH = "io.micronaut.openapi.adoc.template-dir";
    /**
     * Custom final template filename.
     */
    String OPENAPI_ADOC_TEMPLATE_FILENAME = "io.micronaut.openapi.adoc.template-filename";
    /**
     * Result adoc file output directory.
     */
    String OPENAPI_ADOC_OUTPUT_DIR_PATH = "io.micronaut.openapi.adoc.output-dir";
    /**
     * Result adoc filename.
     */
    String OPENAPI_ADOC_OUTPUT_FILENAME = "io.micronaut.openapi.adoc.output-filename";
    /**
     * OpenAPI file path.
     */
    String OPENAPI_ADOC_OPENAPI_PATH = "io.micronaut.openapi.adoc.openapi-path";
}
