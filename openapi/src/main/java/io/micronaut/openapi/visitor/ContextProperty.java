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

import io.micronaut.core.annotation.Internal;

/**
 * Visitor context properties. Usually these are loaded configuration objects.
 *
 * @since 4.10.0
 */
@Internal
public interface ContextProperty {

    /**
     * Loaded micronaut environment.
     */
    String MICRONAUT_INTERNAL_ENVIRONMENT = "micronaut.internal.environment";
    /**
     * Loaded spring openapi endpoints settings.
     */
    String MICRONAUT_INTERNAL_SPRING_OPENAPI_ENDPOINTS = "micronaut.internal.spring.openapi.endpoints";
    /**
     * Loaded micronaut openapi endpoints settings.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINTS = "micronaut.internal.openapi.endpoints";
    /**
     * Flag that shows that the environment properties are already loaded into the context.
     */
    String MICRONAUT_INTERNAL_ENVIRONMENT_CREATED = "micronaut.internal.environment.created";
    /**
     * Loaded micronaut openapi custom schema settings.
     */
    String MICRONAUT_INTERNAL_CUSTOM_SCHEMAS = "micronaut.internal.custom.schemas";
    /**
     * Loaded openapi properties from file.
     */
    String MICRONAUT_INTERNAL_OPENAPI_PROPERTIES = "micronaut.internal.openapi.properties";
    /**
     * The name of the entry for Endpoint class tags in the context.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_PROPS = "micronaut.internal.openapi.endpoint.props";
    /**
     * The name of the entry for Endpoint class tags in the context.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS = "micronaut.internal.openapi.endpoint.class.tags";
    /**
     * The name of the entry for Endpoint servers in the context.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS = "micronaut.internal.openapi.endpoint.servers";
    /**
     * The name of the entry for Endpoint description in the context.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_DESCRIPTION = "micronaut.internal.openapi.endpoint.description";
    /**
     * The name of the entry for Endpoint extensions in the context.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_EXTENSIONS = "micronaut.internal.openapi.endpoint.extensions";
    /**
     * The name of the entry for Endpoint security requirements in the context.
     */
    String MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS = "micronaut.internal.openapi.endpoint.security.requirements";
    /**
     * Loaded project directory from system properties.
     */
    String MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR = "micronaut.internal.openapi.project.dir";
    /**
     * Loaded into context jackson.json-view.enabled property value.
     */
    String MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_ENABLED = "micronaut.internal.jackson.json-view.enabled";
    /**
     * Loaded schema decorators settings into context.
     */
    String MICRONAUT_INTERNAL_SCHEMA_DECORATORS = "micronaut.internal.schema-decorators";
    /**
     * Loaded group settings into context.
     */
    String MICRONAUT_INTERNAL_GROUPS = "micronaut.internal.groups";
    /**
     * Loaded expandable properties. Need to save them to reuse in different places.
     */
    String MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES = "micronaut.internal.expandable.props";
    /**
     * Loaded tag generation remove prefixes list.
     */
    String MICRONAUT_INTERNAL_TAG_GENERATION_REMOVE_PREFIXES = "micronaut.internal.tag.generation.remove.prefixes";
    /**
     * Loaded tag generation remove postfixes list.
     */
    String MICRONAUT_INTERNAL_TAG_GENERATION_REMOVE_POSTFIXES = "micronaut.internal.tag.generation.remove.postfixes";
    /**
     * Flag that shows that the expandable properties are already loaded into the context.
     */
    String MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED = "micronaut.internal.expandable.props.loaded";
    /**
     * Loaded micronaut-security and micronaut-openapi security properties.
     */
    String MICRONAUT_INTERNAL_SECURITY_PROPERTIES = "micronaut.internal.security.properties";
    /**
     * Loaded micronaut-router and micronaut-openapi router versioning properties.
     */
    String MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES = "micronaut.internal.router.versioning.properties";
    /**
     * Saved generated files.
     */
    String MICRONAUT_INTERNAL_GENERATED_FILE = "micronaut.internal.generated.file";
    /**
     * Calculated class output directory path.
     */
    String MICRONAUT_INTERNAL_CLASSPATH_OUTPUT = "micronaut.internal.classpath.output";

    String MICRONAUT_INTERNAL_CHILD_PATH = "micronaut.internal.child.path";
    String MICRONAUT_INTERNAL_CHILD_OP_ID_PREFIX = "micronaut.internal.opId.prefix";
    String MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX = "micronaut.internal.opId.suffix";
    String MICRONAUT_INTERNAL_CHILD_OP_ID_SUFFIX_ADD_ALWAYS = "micronaut.internal.opId.suffixes.add.always";
    String MICRONAUT_INTERNAL_IS_PROCESS_PARENT_CLASS = "micronaut.internal.is.process.parent";
    /**
     * Is it spring-actuator prometheus endpoint.
     */
    String MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS = "micronaut.internal.endpoint.is-prometheus";
}
