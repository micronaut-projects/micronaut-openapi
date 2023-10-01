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
 * Freemarker component template names.
 *
 * @since 5.2.0
 */
public interface TemplatePaths {

    String DEFINITIONS = "definitions";
    String OVERVIEW = "overview";
    String PATHS = "paths";

    String CONTENT = "content";
    String EXAMPLES = "examples";
    String EXTERNAL_DOCS = "externalDocs";
    String HEADERS = "headers";
    String LINKS = "links";
    String PARAMETERS = "parameters";
    String PROPERTIES = "properties";
    String PROPERTY_DESCRIPTION = "propertyDescription";
    String REQUEST_BODY = "requestBody";
    String RESPONSES = "responses";
    String SCHEMA_TYPE = "schemaType";
    String SECURITY_REQUIREMENTS = "securityRequirements";
    String SERVERS = "servers";
}
