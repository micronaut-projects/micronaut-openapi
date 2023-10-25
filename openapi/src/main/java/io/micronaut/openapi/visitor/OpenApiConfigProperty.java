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

import java.util.Set;

/**
 * Supported configuration properties.
 *
 * @since 4.10.0
 */
public interface OpenApiConfigProperty {

    /**
     * System property that enables or disables open api annotation processing.
     * <br>
     * Default: true
     */
    String MICRONAUT_OPENAPI_ENABLED = "micronaut.openapi.enabled";
    /**
     * System property that enables setting the open api config file.
     */
    String MICRONAUT_OPENAPI_CONFIG_FILE = "micronaut.openapi.config.file";
    /**
     * Prefix for expandable properties.
     */
    String MICRONAUT_OPENAPI_EXPAND_PREFIX = "micronaut.openapi.expand.";
    /**
     * System property for server context path.
     */
    String MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH = "micronaut.openapi.server.context.path";
    /**
     * System property for naming strategy. One jackson PropertyNamingStrategy.
     */
    String MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY = "micronaut.openapi.property.naming.strategy";
    /**
     * System property for views specification.
     */
    String MICRONAUT_OPENAPI_VIEWS_SPEC = "micronaut.openapi.views.spec";
    /**
     * System property that enables setting the target file to write to.
     */
    String MICRONAUT_OPENAPI_TARGET_FILE = "micronaut.openapi.target.file";
    /**
     * System property that specifies the path where the generated UI elements will be located.
     */
    String MICRONAUT_OPENAPI_VIEWS_DEST_DIR = "micronaut.openapi.views.dest.dir";
    /**
     * System property that specifies the location of additional swagger YAML and JSON files to read from.
     */
    String MICRONAUT_OPENAPI_ADDITIONAL_FILES = "micronaut.openapi.additional.files";
    /**
     * System property that specifies the location of current project.
     */
    String MICRONAUT_OPENAPI_PROJECT_DIR = "micronaut.openapi.project.dir";
    /**
     * System property that specifies the default security schema name, if it's not specified by annotation SecurityScheme.
     */
    String MICRONAUT_OPENAPI_SECURITY_DEFAULT_SCHEMA_NAME = "micronaut.openapi.security.default-schema-name";
    /**
     * System property that specifies the schema classes fields visibility level. By default, only public fields visible.
     * <p>
     * Available values:
     * </p>
     * PRIVATE
     * PACKAGE
     * PROTECTED
     * PUBLIC
     */
    String MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL = "micronaut.openapi.field.visibility.level";
    /**
     * Is this property true, output file format will be JSON, otherwise YAML.
     */
    String MICRONAUT_OPENAPI_JSON_FORMAT = "micronaut.openapi.json.format";
    /**
     * The name of the result swagger file.
     * <p>
     * Default filename is &lt;info.title&gt;-&lt;info.version&gt;.yml.
     * If info annotation not set, filename will be swagger.yml.
     */
    String MICRONAUT_OPENAPI_FILENAME = "micronaut.openapi.filename";
    /**
     * Active micronaut environments which will be used for @Requires annotations.
     */
    String MICRONAUT_OPENAPI_ENVIRONMENTS = "micronaut.openapi.environments";
    /**
     * Is this property true, properties wll be loaded in the standard way from application.yml.
     * Also, environments from "micronaut.openapi.environments" property will set as additional environments,
     * if you want to set specific environment name for openAPI generator.
     * <br>
     * Default value is "true".
     */
    String MICRONAUT_ENVIRONMENT_ENABLED = "micronaut.environment.enabled";
    /**
     * Is this property true, micronaut-openapi will process micronaut-security properties and annotations
     * to construct openapi security schema.
     * <br>
     * Default value is "true".
     */
    String MICRONAUT_OPENAPI_SECURITY_ENABLED = "micronaut.openapi.security.enabled";
    /**
     * Is this property true, micronaut-openapi will process micronaut-router versioning properties and annotations.
     * <br>
     * Default value is "true".
     */
    String MICRONAUT_OPENAPI_VERSIONING_ENABLED = "micronaut.openapi.versioning.enabled";
    /**
     * Config file locations. By default, micronaut-openapi search config in standard path:
     * &lt;project_path&gt;/src/main/resources/
     * <p>
     * You can set your custom paths separated by ','. To set absolute paths use prefix 'file:',
     * classpath paths use prefix 'classpath:' or use prefix 'project:' to set paths from project
     * directory.
     */
    String MICRONAUT_CONFIG_FILE_LOCATIONS = "micronaut.openapi.config.file.locations";
    /**
     * Property that determines whether properties that have no view annotations are included in JSON serialization views.
     * If enabled, non-annotated properties will be included; when disabled, they will be excluded.
     * <br>
     * Default value is "true".
     */
    String MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION = "micronaut.openapi.json.view.default.inclusion";
    /**
     * Loaded micronaut-http server context path property.
     */
    String MICRONAUT_SERVER_CONTEXT_PATH = "micronaut.server.context-path";
    /**
     * Loaded micronaut-http-server-netty property (json-view.enabled).
     */
    String MICRONAUT_JACKSON_JSON_VIEW_ENABLED = "jackson.json-view.enabled";

    /**
     * Properties prefix to set custom schema implementations for selected classes.
     * For example, if you want to set simple 'java.lang.String' class to some complex 'org.somepackage.MyComplexType' class you need to write:
     * <p>
     * micronaut.openapi.schema.org.somepackage.MyComplexType=java.lang.String
     * <p>
     * Also, you can set it in your application.yml file like this:
     * <p>
     * micronaut:
     * openapi:
     * schema:
     * org.somepackage.MyComplexType: java.lang.String
     * org.somepackage.MyComplexType2: java.lang.Integer
     * ...
     */
    String MICRONAUT_OPENAPI_SCHEMA = "micronaut.openapi.schema";
    /**
     * Properties prefix to set schema name prefix or postfix by package.
     * For example, if you have some classes with same names in different packages you can set postfix like this:
     * <p>
     * micronaut.openapi.schema-postfix.org.api.v1_0_0=1_0_0
     * micronaut.openapi.schema-postfix.org.api.v2_0_0=2_0_0
     * <p>
     * Also, you can set it in your application.yml file like this:
     * <p>
     * micronaut:
     * openapi:
     * schema-postfix:
     * org.api.v1_0_0: 1_0_0
     * org.api.v2_0_0: 2_0_0
     * ...
     */
    String MICRONAUT_OPENAPI_SCHEMA_PREFIX = "micronaut.openapi.schema-prefix";
    String MICRONAUT_OPENAPI_SCHEMA_POSTFIX = "micronaut.openapi.schema-postfix";
    /**
     * Properties prefix to set custom schema implementations for selected classes.
     * For example, if you want to set simple 'java.lang.String' class to some complex 'org.somepackage.MyComplexType' class you need to write:
     * <p>
     * -Dmicronaut.openapi.group.my-group1.title="Title 1"
     * <p>
     * Also, you can set it in your application.yml file like this:
     * <p>
     * micronaut:
     * openapi:
     * group:
     * my-group1:
     * title: Title 1
     * filename: swagger-${group}-${apiVersion}-${version}.yml
     * my-group2:
     * title: Title 2
     * ...
     */
    String MICRONAUT_OPENAPI_GROUPS = "micronaut.openapi.groups";

    /**
     * Prefix for custom sub-template names.
     */
    String MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX = "micronaut.openapi.adoc.templates.";
    /**
     * Is conversion to Asciidoc enabled.
     */
    String MICRONAUT_OPENAPI_ADOC_ENABLED = "micronaut.openapi.adoc.enabled";
    /**
     * Custom template directory.
     */
    String MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH = "micronaut.openapi.adoc.template.dir";
    /**
     * Custom final template filename.
     */
    String MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME = "micronaut.openapi.adoc.template.filename";
    /**
     * Result adoc file output directory.
     */
    String MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH = "micronaut.openapi.adoc.output.dir";
    /**
     * Result adoc filename.
     */
    String MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME = "micronaut.openapi.adoc.output.filename";
    /**
     * OpenAPI file path.
     */
    String MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH = "micronaut.openapi.adoc.openapi.path";
    /**
     * Default openapi config file.
     */
    String OPENAPI_CONFIG_FILE = "openapi.properties";

    /**
     * All supported annotation processor properties.
     */
    Set<String> ALL = Set.of(
        MICRONAUT_OPENAPI_ENABLED,
        MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH,
        MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY,
        MICRONAUT_OPENAPI_VIEWS_SPEC,
        MICRONAUT_OPENAPI_FILENAME,
        MICRONAUT_OPENAPI_JSON_FORMAT,
        MICRONAUT_OPENAPI_ENVIRONMENTS,
        MICRONAUT_ENVIRONMENT_ENABLED,
        MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL,
        MICRONAUT_CONFIG_FILE_LOCATIONS,
        MICRONAUT_OPENAPI_TARGET_FILE,
        MICRONAUT_OPENAPI_VIEWS_DEST_DIR,
        MICRONAUT_OPENAPI_ADDITIONAL_FILES,
        MICRONAUT_OPENAPI_CONFIG_FILE,
        MICRONAUT_OPENAPI_SECURITY_ENABLED,
        MICRONAUT_OPENAPI_VERSIONING_ENABLED,
        MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION,
        MICRONAUT_OPENAPI_PROJECT_DIR,
        MICRONAUT_OPENAPI_ADOC_ENABLED,
        MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH,
        MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME,
        MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH,
        MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME,
        MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH
    );
}
