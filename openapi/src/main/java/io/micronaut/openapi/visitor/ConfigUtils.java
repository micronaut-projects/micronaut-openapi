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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.DefaultMutableConversionService;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.micronaut.openapi.visitor.group.RouterVersioningProperties;
import io.micronaut.openapi.visitor.management.EndpointProperties;
import io.micronaut.openapi.visitor.management.EndpointsConfig;
import io.micronaut.openapi.visitor.security.InterceptUrlMapConverter;
import io.micronaut.openapi.visitor.security.InterceptUrlMapPattern;
import io.micronaut.openapi.visitor.security.SecurityProperties;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import static io.micronaut.core.type.Argument.LIST_OF_STRING;
import static io.micronaut.core.util.StringUtils.EMPTY_STRING;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CLASSPATH_OUTPUT;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CUSTOM_SCHEMAS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENVIRONMENT;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENVIRONMENT_CREATED;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_GROUPS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINTS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_SCHEMA_DECORATORS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_SECURITY_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_TAG_GENERATION_REMOVE_POSTFIXES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_TAG_GENERATION_REMOVE_PREFIXES;
import static io.micronaut.openapi.visitor.ContextUtils.ARGUMENT_CUSTOM_SCHEMA_MAP;
import static io.micronaut.openapi.visitor.ContextUtils.ARGUMENT_GROUP_PROPERTIES_MAP;
import static io.micronaut.openapi.visitor.ContextUtils.ARGUMENT_SCHEMA_DECORATORS_MAP;
import static io.micronaut.openapi.visitor.ContextUtils.EXPANDABLE_PROPERTIES_ARGUMENT;
import static io.micronaut.openapi.visitor.ContextUtils.getProjectDir;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.FileUtils.calcFinalFilename;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ALL;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_EXTENSIONS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_GROUPS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_GROUPS_EXCLUDED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_SERVERS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ENDPOINTS_TAGS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_ENDPOINTS_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_ENDPOINTS_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_ENVIRONMENT_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_JACKSON_JSON_VIEW_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_31_JSON_SCHEMA_DIALECT;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONSTRUCTOR_ARGUMENTS_AS_REQUIRED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENVIRONMENTS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_EXPAND_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_GENERATOR_EXTENSIONS_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_PROJECT_DIR;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_INCLUDE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_RESPONSE_READ_SUCCESSFUL_FROM_CODE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DECORATOR_POSTFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DECORATOR_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DUPLICATE_RESOLUTION;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_EXTRA_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_MAPPING;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_EMPTY;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_GENERIC;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_INNER_CLASS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_POSTFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SECURITY_DEFAULT_SCHEMA_NAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SECURITY_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SWAGGER_FILE_GENERATION_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_BY_PACKAGE_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_DESCRIPTION_MAX_LENGTH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_POSTFIXES;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_PREFIXES;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VERSIONING_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_SERVER_CONTEXT_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.OPENAPI_CONFIG_FILE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.SPRING_SERVER_CONTEXT_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.SPRING_WEBFLUX_BASE_PATH;
import static io.micronaut.openapi.visitor.SchemaUtils.PREFIX_X;
import static io.micronaut.openapi.visitor.SchemaUtils.prependIfMissing;
import static io.micronaut.openapi.visitor.StringUtil.COMMA;
import static io.micronaut.openapi.visitor.StringUtil.DOT;
import static io.micronaut.openapi.visitor.StringUtil.UNDERSCORE;
import static io.micronaut.openapi.visitor.StringUtil.WILDCARD;
import static io.micronaut.openapi.visitor.UrlUtils.parsePath;
import static io.micronaut.openapi.visitor.Utils.isKsp;
import static io.micronaut.openapi.visitor.group.RouterVersioningProperties.DEFAULT_HEADER_NAME;
import static io.micronaut.openapi.visitor.group.RouterVersioningProperties.DEFAULT_PARAMETER_NAME;
import static io.micronaut.openapi.visitor.management.EndpointUtils.ALL_MICRONAUT_MANAGEMENT_ENDPOINTS;
import static io.micronaut.openapi.visitor.management.SpringActuatorConfigUtils.mergeWithActuatorProperties;

/**
 * Configuration utilities methods.
 *
 * @since 4.10.0
 */
@Internal
public final class ConfigUtils {

    public static final String ALL_ENDPOINTS_NAME = "all";
    public static final String ALL_SPRING_ACTUATOR_ENDPOINTS_NAME = "*";

    private static final String LOADED_POSTFIX = ".loaded";
    private static final String VALUE_POSTFIX = ".value";

    private static final List<String> DEFAULT_PREFIXES = List.of("");
    private static final List<String> DEFAULT_POSTFIXES = List.of("controller", "api", "endpoints", "endpoint");

    /**
     * Default autogenerated security schema name.
     */
    private static final String DEFAULT_SECURITY_SCHEMA_NAME = "Authorization";
    private static final TypeReference<Map<String, Object>> TYPE_EXTENSIONS = new TypeReference<>() {
    };
    private static final TypeReference<List<Server>> TYPE_SERVERS_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<SecurityRequirement>> TYPE_SECURITY_REQUIREMENTS_LIST = new TypeReference<>() {
    };

    private ConfigUtils() {
    }

    public static SchemaDecorator getSchemaDecoration(String packageName, VisitorContext context) {

        Map<String, SchemaDecorator> schemaDecorators = ContextUtils.get(MICRONAUT_INTERNAL_SCHEMA_DECORATORS, ARGUMENT_SCHEMA_DECORATORS_MAP, context);
        if (schemaDecorators != null) {
            return schemaDecorators.get(packageName);
        }

        schemaDecorators = new HashMap<>();

        // first read system properties
        Properties sysProps = System.getProperties();
        readSchemaDecorators(sysProps, schemaDecorators, context);

        // second read openapi.properties file
        Properties fileProps = readOpenApiConfigFile(context);
        readSchemaDecorators(fileProps, schemaDecorators, context);

        // third read environments properties
        Environment environment = getEnv(context);
        if (environment != null) {
            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_PREFIX, StringConvention.RAW).entrySet()) {
                SchemaDecorator decorator = schemaDecorators.computeIfAbsent(entry.getKey(), k -> new SchemaDecorator());
                decorator.setPrefix((String) entry.getValue());
            }

            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_POSTFIX, StringConvention.RAW).entrySet()) {
                SchemaDecorator decorator = schemaDecorators.computeIfAbsent(entry.getKey(), k -> new SchemaDecorator());
                decorator.setPostfix((String) entry.getValue());
            }

            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_PREFIX, StringConvention.RAW).entrySet()) {
                SchemaDecorator decorator = schemaDecorators.computeIfAbsent(entry.getKey(), k -> new SchemaDecorator());
                decorator.setPrefix((String) entry.getValue());
            }

            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_POSTFIX, StringConvention.RAW).entrySet()) {
                SchemaDecorator decorator = schemaDecorators.computeIfAbsent(entry.getKey(), k -> new SchemaDecorator());
                decorator.setPostfix((String) entry.getValue());
            }
        }

        ContextUtils.put(MICRONAUT_INTERNAL_SCHEMA_DECORATORS, schemaDecorators, context);

        return schemaDecorators.get(packageName);
    }

    public static ClassElement getCustomSchema(String className, Map<String, ClassElement> typeArgs, VisitorContext context) {

        Map<String, CustomSchema> customSchemas = ContextUtils.get(MICRONAUT_INTERNAL_CUSTOM_SCHEMAS, ARGUMENT_CUSTOM_SCHEMA_MAP, context);
        if (customSchemas != null) {
            String key = getClassNameWithGenerics(className, typeArgs);

            CustomSchema customSchema = customSchemas.get(key);
            if (customSchema != null) {
                return customSchema.classElement;
            }
            customSchema = customSchemas.get(className);

            return customSchema != null ? customSchema.classElement : null;
        }

        customSchemas = new HashMap<>();

        // first read system properties
        Properties sysProps = System.getProperties();
        readCustomSchemas(sysProps, customSchemas, context);

        // second read openapi.properties file
        Properties fileProps = readOpenApiConfigFile(context);
        readCustomSchemas(fileProps, customSchemas, context);

        // third read environments properties
        Environment environment = getEnv(context);
        if (environment != null) {
            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA, StringConvention.RAW).entrySet()) {

                String configuredClassName = entry.getKey();
                // Remove this check, after we remove MICRONAUT_OPENAPI_SCHEMA property
                String prop = MICRONAUT_OPENAPI_SCHEMA + StringUtil.DOT + configuredClassName;
                if (isMicronautProperty(prop)) {
                    continue;
                }

                String targetClassName = (String) entry.getValue();
                readCustomSchema(configuredClassName, targetClassName, customSchemas, context);
            }
            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_MAPPING, StringConvention.RAW).entrySet()) {
                String configuredClassName = entry.getKey();

                // Remove this check, after we remove MICRONAUT_OPENAPI_SCHEMA property
                String prop = MICRONAUT_OPENAPI_SCHEMA + StringUtil.DOT + configuredClassName;
                if (isMicronautProperty(prop)) {
                    continue;
                }
                String targetClassName = (String) entry.getValue();
                readCustomSchema(configuredClassName, targetClassName, customSchemas, context);
            }
        }

        ContextUtils.put(MICRONAUT_INTERNAL_CUSTOM_SCHEMAS, customSchemas, context);

        if (customSchemas.isEmpty()) {
            return null;
        }

        String key = getClassNameWithGenerics(className, typeArgs);

        CustomSchema customSchema = customSchemas.get(key);
        if (customSchema != null) {
            return customSchema.classElement;
        }
        customSchema = customSchemas.get(className);

        return customSchema != null ? customSchema.classElement : null;
    }

    private static boolean isMicronautProperty(String prop) {
        return prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_PREFIX)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_POSTFIX)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_PREFIX)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_POSTFIX)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_EMPTY)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_GENERIC)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_INNER_CLASS)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_DUPLICATE_RESOLUTION)
            || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_EXTRA_ENABLED);
    }

    private static String getClassNameWithGenerics(String className, Map<String, ClassElement> typeArgs) {
        var key = new StringBuilder(className);
        if (!typeArgs.isEmpty()) {
            key.append('<');
            boolean isFirst = true;
            for (ClassElement typeArg : typeArgs.values()) {
                if (!isFirst) {
                    key.append(',');
                }
                key.append(typeArg.getName());
                isFirst = false;
            }
            key.append('>');
        }
        return key.toString();
    }

    @NonNull
    public static String getServerContextPath(VisitorContext context) {
        var contextPath = getConfigProperty(MICRONAUT_SERVER_CONTEXT_PATH, context);
        if (contextPath == null) {
            contextPath = getConfigProperty(SPRING_SERVER_CONTEXT_PATH, context);
        }
        if (contextPath == null) {
            contextPath = getConfigProperty(SPRING_WEBFLUX_BASE_PATH, context);
        }
        if (contextPath == null) {
            contextPath = StringUtils.EMPTY_STRING;
        }

        return contextPath;
    }

    public static DuplicateResolution getSchemaDuplicateResolution(VisitorContext context) {
        var value = getConfigProperty(MICRONAUT_OPENAPI_SCHEMA_DUPLICATE_RESOLUTION, context);
        if (StringUtils.isNotEmpty(value) && DuplicateResolution.ERROR.name().equalsIgnoreCase(value)) {
            return DuplicateResolution.ERROR;
        }
        return DuplicateResolution.AUTO;
    }

    public static boolean isConstructorArgumentsAsRequired(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_CONSTRUCTOR_ARGUMENTS_AS_REQUIRED, true, context);
    }

    public static boolean isResponseReadSuccessfulFromCode(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_RESPONSE_READ_SUCCESSFUL_FROM_CODE, true, context);
    }

    public static boolean isOpenApiEnabled(VisitorContext context) {
        boolean value = getBooleanProperty(MICRONAUT_OPENAPI_ENABLED, true, context);
        System.setProperty(MICRONAUT_OPENAPI_ENABLED, Boolean.toString(value));
        return value;
    }

    public static boolean isSchemaNameSeparatorEmpty(VisitorContext context) {
        boolean value = getBooleanProperty(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_EMPTY, false, context);
        System.setProperty(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_EMPTY, Boolean.toString(value));
        return value;
    }

    public static String getGenericSeparator(VisitorContext context) {
        if (isSchemaNameSeparatorEmpty(context)) {
            return EMPTY_STRING;
        }

        var value = getConfigProperty(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_GENERIC, context);
        return StringUtils.isNotEmpty(value) ? value : UNDERSCORE;
    }

    public static String getInnerClassSeparator(VisitorContext context) {
        if (isSchemaNameSeparatorEmpty(context)) {
            return EMPTY_STRING;
        }
        var value = getConfigProperty(MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_INNER_CLASS, context);
        return StringUtils.isNotEmpty(value) ? value : DOT;
    }

    public static String getJsonSchemaDialect(VisitorContext context) {
        var value = getConfigProperty(MICRONAUT_OPENAPI_31_JSON_SCHEMA_DIALECT, context);
        return StringUtils.isNotEmpty(value) ? value : null;
    }

    public static boolean isSpecGenerationEnabled(VisitorContext context) {
        boolean value = getBooleanProperty(MICRONAUT_OPENAPI_SWAGGER_FILE_GENERATION_ENABLED, true, context);
        System.setProperty(MICRONAUT_OPENAPI_SWAGGER_FILE_GENERATION_ENABLED, Boolean.toString(value));
        return value;
    }

    public static boolean isTagGenerationByClassEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, false, context);
    }

    public static boolean isTagGenerationByPackageEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_PACKAGE_ENABLED, false, context);
    }

    public static int getTagGenerationDescriptionMaxLength(VisitorContext context) {
        var descriptionLengthStr = getConfigProperty(MICRONAUT_OPENAPI_TAG_GENERATION_DESCRIPTION_MAX_LENGTH, context);
        try {
            return Integer.parseInt(descriptionLengthStr);
        } catch (Exception e) {
            return -1;
        }
    }

    public static PropertyNamingStrategies.NamingBase getTagGenerationNamingStrategy(VisitorContext context) {
        var value = getConfigProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, context);
        if (value == null) {
            return null;
        }
        try {
            return toJacksonStrategy(value.toUpperCase(Locale.ENGLISH), context);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> getTagGenerationRemovePrefixes(VisitorContext context) {
        return readTagGenerationProperty(
            MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_PREFIXES,
            MICRONAUT_INTERNAL_TAG_GENERATION_REMOVE_PREFIXES,
            DEFAULT_PREFIXES,
            context
        );
    }

    public static List<String> getTagGenerationRemovePostfixes(VisitorContext context) {
        return readTagGenerationProperty(
            MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_POSTFIXES,
            MICRONAUT_INTERNAL_TAG_GENERATION_REMOVE_POSTFIXES,
            DEFAULT_POSTFIXES,
            context
        );
    }

    private static List<String> readTagGenerationProperty(String configProp, String cacheProp, List<String> defaultVal, VisitorContext context) {
        List<String> loadedValues = ContextUtils.get(cacheProp, LIST_OF_STRING, context);
        if (loadedValues != null) {
            return loadedValues;
        }

        List<String> result;
        var configPropStr = getConfigProperty(configProp, context);
        if (EMPTY_STRING.equals(configPropStr)) {
            result = Collections.emptyList();
        } else if (configPropStr == null) {
            // if not set, get default values
            result = defaultVal;
        } else {
            result = new ArrayList<>();
            var values = configPropStr.split(COMMA);
            for (var value : values) {
                result.add(value.trim().toLowerCase(Locale.ENGLISH));
            }
        }
        ContextUtils.put(cacheProp, result, context);

        return result;
    }

    public static boolean isExtraSchemasEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_SCHEMA_EXTRA_ENABLED, true, context);
    }

    public static boolean isJsonViewDefaultInclusion(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION, true, context);
    }

    public static boolean isGeneratorExtensionsEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_GENERATOR_EXTENSIONS_ENABLED, true, context);
    }

    public static boolean isAdocEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_OPENAPI_ADOC_ENABLED, true, context);
    }

    public static boolean isJsonViewEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_JACKSON_JSON_VIEW_ENABLED, false, context);
    }

    public static boolean isEndpointsEnabled(VisitorContext context) {
        return getBooleanProperty(MICRONAUT_ENDPOINTS_ENABLED, false, context);
    }

    public static List<Pair<String, String>> getExpandableProperties(VisitorContext context) {

        Boolean propertiesLoaded = ContextUtils.get(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED, Boolean.class, context);
        if (propertiesLoaded != null) {
            return ContextUtils.get(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES, EXPANDABLE_PROPERTIES_ARGUMENT, context);
        }

        var expandableProperties = new ArrayList<Pair<String, String>>();
        var expandPrefix = MICRONAUT_OPENAPI_EXPAND_PREFIX + DOT;

        // first, check system properties and environments config files
        var env = (AnnProcessorEnvironment) getEnv(context);
        Map<String, Object> propertiesFromEnv = null;
        if (env != null) {
            try {
                propertiesFromEnv = env.getProperties(expandPrefix.substring(0, expandPrefix.length() - 1), null);
            } catch (Exception e) {
                warn("Error:\n" + Utils.printStackTrace(e), context);
            }
        }

        var expandedPropsMap = new HashMap<String, String>();
        if (CollectionUtils.isNotEmpty(propertiesFromEnv)) {
            for (Entry<String, Object> entry : propertiesFromEnv.entrySet()) {
                expandedPropsMap.put(entry.getKey(), entry.getValue().toString());
            }
        }

        // next, read openapi.properties file
        Properties openapiProps = readOpenApiConfigFile(context);
        for (Entry<Object, Object> entry : openapiProps.entrySet()) {
            String key = entry.getKey().toString();
            if (!key.startsWith(expandPrefix)) {
                continue;
            }
            expandedPropsMap.put(key, entry.getValue().toString());
        }

        // next, read system properties
        if (CollectionUtils.isNotEmpty(System.getProperties())) {
            for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (!key.startsWith(expandPrefix)) {
                    continue;
                }
                expandedPropsMap.put(key, entry.getValue().toString());
            }
        }

        for (Entry<String, String> entry : expandedPropsMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(expandPrefix)) {
                key = key.substring(expandPrefix.length());
            }
            var prop = Pair.of("\\$\\{" + key + '}', entry.getValue());
            if (!expandableProperties.contains(prop)) {
                expandableProperties.add(prop);
            }
        }

        ContextUtils.put(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES, expandableProperties, context);
        ContextUtils.put(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED, true, context);

        return expandableProperties;
    }

    public static Map<String, String> getAdocProperties(OpenApiInfo openApiInfo, boolean isSingleGroup, VisitorContext context) {

        var adocProperties = new HashMap<String, String>();
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH, getConfigProperty(MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME, getConfigProperty(MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, getConfigProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, getConfigProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH, getConfigProperty(MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH, context));

        var expandPrefix = MICRONAUT_OPENAPI_EXPAND_PREFIX + DOT;

        // first, check system properties and environments config files
        var env = (AnnProcessorEnvironment) getEnv(context);
        Map<String, Object> propertiesFromEnv = null;
        if (env != null) {
            try {
                propertiesFromEnv = env.getProperties(MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX.substring(0, MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX.length() - 1), null);
            } catch (Exception e) {
                warn("Error:\n" + Utils.printStackTrace(e), context);
            }
        }

        if (CollectionUtils.isNotEmpty(propertiesFromEnv)) {
            for (var entry : propertiesFromEnv.entrySet()) {
                adocProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }

        // next, read openapi.properties file
        Properties openapiProps = readOpenApiConfigFile(context);
        for (Entry<Object, Object> entry : openapiProps.entrySet()) {
            String key = entry.getKey().toString();
            if (!key.startsWith(expandPrefix)) {
                continue;
            }
            adocProperties.put(key, entry.getValue().toString());
        }

        // next, read system properties
        if (CollectionUtils.isNotEmpty(System.getProperties())) {
            for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (!key.startsWith(expandPrefix)) {
                    continue;
                }
                adocProperties.put(key, entry.getValue().toString());
            }
        }

        var fileName = StringUtils.isNotEmpty(openApiInfo.getAdocFilename()) ? openApiInfo.getAdocFilename() : adocProperties.get(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME);
        var titleAndFilename = calcFinalFilename(openApiInfo.getAdocFilename(), openApiInfo, isSingleGroup, "adoc", context);

        return adocProperties;
    }

    public static SecurityProperties getSecurityProperties(VisitorContext context) {

        SecurityProperties securityProperties = ContextUtils.get(MICRONAUT_INTERNAL_SECURITY_PROPERTIES, SecurityProperties.class, context);
        if (securityProperties != null) {
            return securityProperties;
        }

        // load micronaut security properties
        Environment environment = getEnv(context);
        List<InterceptUrlMapPattern> interceptUrlMapPatterns;
        if (environment != null) {
            interceptUrlMapPatterns = environment.get("micronaut.security.intercept-url-map", Argument.listOf(InterceptUrlMapPattern.class)).orElse(Collections.emptyList());
        } else {
            interceptUrlMapPatterns = Collections.emptyList();
        }

        String defaultSchemaName = getConfigProperty(MICRONAUT_OPENAPI_SECURITY_DEFAULT_SCHEMA_NAME, context);
        if (StringUtils.isEmpty(defaultSchemaName)) {
            defaultSchemaName = DEFAULT_SECURITY_SCHEMA_NAME;
        }

        boolean tokenEnabled = getBooleanProperty("micronaut.security.token.enabled", false, context);

        securityProperties = new SecurityProperties(
            getBooleanProperty(MICRONAUT_OPENAPI_SECURITY_ENABLED, true, context),
            getBooleanProperty("micronaut.security.enabled", false, context),
            defaultSchemaName,
            interceptUrlMapPatterns,
            tokenEnabled,
            getBooleanProperty("micronaut.security.token.jwt.enabled", tokenEnabled, context),
            getBooleanProperty("micronaut.security.token.jwt.bearer", tokenEnabled, context),
            getBooleanProperty("micronaut.security.token.jwt.cookie.enabled", false, context),
            getBooleanProperty("micronaut.security.oauth2.enabled", false, context),
            getBooleanProperty("micronaut.security.basic-auth.enabled", false, context)
        );

        ContextUtils.put(MICRONAUT_INTERNAL_SECURITY_PROPERTIES, securityProperties, context);

        return securityProperties;
    }

    public static RouterVersioningProperties getRouterVersioningProperties(VisitorContext context) {

        RouterVersioningProperties routerVersioningProperties = ContextUtils.get(MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES, RouterVersioningProperties.class, context);
        if (routerVersioningProperties != null) {
            return routerVersioningProperties;
        }

        routerVersioningProperties = new RouterVersioningProperties(
            getBooleanProperty(MICRONAUT_OPENAPI_VERSIONING_ENABLED, true, context),
            getBooleanProperty("micronaut.router.versioning.enabled", false, context),
            getBooleanProperty("micronaut.router.versioning.header.enabled", false, context),
            getListStringsProperty("micronaut.router.versioning.header.names", Collections.singletonList(DEFAULT_HEADER_NAME), context),
            getBooleanProperty("micronaut.router.versioning.parameter.enabled", false, context),
            getListStringsProperty("micronaut.router.versioning.parameter.names", Collections.singletonList(DEFAULT_PARAMETER_NAME), context)
        );

        ContextUtils.put(MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES, routerVersioningProperties, context);

        return routerVersioningProperties;
    }

    public static List<String> getListStringsProperty(String property, List<String> defaultValue, VisitorContext context) {
        String strValue = System.getProperty(property);
        if (StringUtils.isEmpty(strValue)) {
            strValue = readOpenApiConfigFile(context).getProperty(property);
        }
        if (StringUtils.isNotEmpty(strValue)) {
            var result = new ArrayList<String>();
            for (String item : strValue.split(COMMA)) {
                result.add(item.strip());
            }
            return result;
        }

        Environment env = getEnv(context);
        if (env != null) {
            return env.get(property, LIST_OF_STRING).orElse(defaultValue);
        }
        return defaultValue;
    }

    public static GroupProperties getGroupProperties(String groupName, VisitorContext context) {
        if (groupName == null) {
            return null;
        }
        Map<String, GroupProperties> allGroupsProperties = getGroupsPropertiesMap(context);
        return CollectionUtils.isNotEmpty(allGroupsProperties) ? allGroupsProperties.get(groupName) : null;
    }

    public static Map<String, GroupProperties> getGroupsPropertiesMap(VisitorContext context) {

        if (context == null) {
            return Collections.emptyMap();
        }
        Map<String, GroupProperties> groupPropertiesMap = ContextUtils.get(MICRONAUT_INTERNAL_GROUPS, ARGUMENT_GROUP_PROPERTIES_MAP, context);
        if (groupPropertiesMap != null) {
            return groupPropertiesMap;
        }

        groupPropertiesMap = new HashMap<>();

        // first read system properties
        Properties sysProps = System.getProperties();
        readGroupsProperties(sysProps, groupPropertiesMap, context);

        // second read openapi.properties file
        Properties fileProps = readOpenApiConfigFile(context);
        readGroupsProperties(fileProps, groupPropertiesMap, context);

        // third read environments properties
        Environment environment = getEnv(context);
        if (environment != null) {
            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_GROUPS, StringConvention.RAW).entrySet()) {
                String entryKey = entry.getKey();
                String[] propParts = entryKey.split("\\.");
                String propName = propParts[propParts.length - 1];
                String groupName = entryKey.substring(0, entryKey.length() - propName.length() - 1);
                setGroupProperty(groupName, propName, entry.getValue(), groupPropertiesMap, context);
            }
        }

        Utils.getAllKnownGroups().addAll(groupPropertiesMap.keySet());

        ContextUtils.put(MICRONAUT_INTERNAL_GROUPS, groupPropertiesMap, context);

        return groupPropertiesMap;
    }

    private static void readGroupsProperties(Properties props, Map<String, GroupProperties> groupPropertiesMap, VisitorContext context) {

        for (String prop : props.stringPropertyNames()) {
            if (!prop.startsWith(MICRONAUT_OPENAPI_GROUPS)) {
                continue;
            }
            int groupNameIndexEnd = prop.indexOf('.', MICRONAUT_OPENAPI_GROUPS.length() + 1);
            if (groupNameIndexEnd < 0) {
                continue;
            }
            String groupName = prop.substring(MICRONAUT_OPENAPI_GROUPS.length() + 1, groupNameIndexEnd);
            String propertyName = prop.substring(groupNameIndexEnd + 1);
            String value = props.getProperty(prop);
            setGroupProperty(groupName, propertyName, value, groupPropertiesMap, context);
        }
    }

    private static void setGroupProperty(String groupName, String propertyName, Object value, Map<String, GroupProperties> groupPropertiesMap, VisitorContext context) {
        if (value == null) {
            return;
        }
        String valueStr = value.toString();
        GroupProperties groupProperties = groupPropertiesMap.computeIfAbsent(groupName, GroupProperties::new);
        switch (propertyName.toLowerCase(Locale.ENGLISH)) {
            case "display-name", "displayname":
                if (groupProperties.getDisplayName() == null) {
                    groupProperties.setDisplayName(valueStr);
                }
                break;
            case "file-name", "filename":
                if (groupProperties.getFilename() == null) {
                    groupProperties.setFilename(valueStr);
                }
                break;
            case "adoc-file-name", "adocfilename":
                if (groupProperties.getAdocFilename() == null) {
                    groupProperties.setAdocFilename(valueStr);
                }
                break;
            case "adoc-enabled", "adocenabled":
                if (groupProperties.getAdocEnabled() == null) {
                    groupProperties.setAdocEnabled(Boolean.valueOf(valueStr));
                }
                break;
            case "packages":
                if (groupProperties.getPackages() == null) {
                    var packages = new ArrayList<GroupProperties.PackageProperties>();
                    for (String groupPackage : valueStr.split(COMMA)) {
                        packages.add(getPackageProperties(groupPackage));
                    }
                    groupProperties.setPackages(packages);
                }
                break;
            case "primary":
                if (groupProperties.getPrimary() == null) {
                    groupProperties.setPrimary(Boolean.valueOf(valueStr));
                }
                break;
            case "common-exclude", "commonexclude":
                if (groupProperties.getCommonExclude() == null) {
                    groupProperties.setCommonExclude(Boolean.valueOf(valueStr));
                }
                break;
            case "packages-exclude", "packagesexclude":
                if (groupProperties.getPackagesExclude() == null) {
                    var packagesExclude = new ArrayList<GroupProperties.PackageProperties>();
                    for (String groupPackage : valueStr.split(COMMA)) {
                        packagesExclude.add(getPackageProperties(groupPackage));
                    }
                    groupProperties.setPackagesExclude(packagesExclude);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Returns the EndpointsConfiguration.
     *
     * @param context The context.
     * @return The EndpointsConfiguration.
     */
    public static EndpointsConfig getEndpointsConfig(VisitorContext context) {
        var endpointsConfig = ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_ENDPOINTS, EndpointsConfig.class, context);
        if (endpointsConfig != null) {
            return endpointsConfig;
        }

        endpointsConfig = new EndpointsConfig(isEndpointsEnabled(context));
        endpointsConfig.setPath(parsePath(getConfigProperty(ENDPOINTS_PATH, context)));
        endpointsConfig.setTags(parseTags(getConfigProperty(ENDPOINTS_TAGS, context)));
        endpointsConfig.setServers(parseServers(getConfigProperty(ENDPOINTS_SERVERS, context), context));
        endpointsConfig.setSecurityRequirements(parseSecurityRequirements(getConfigProperty(ENDPOINTS_SECURITY_REQUIREMENTS, context), context));
        endpointsConfig.setExtensions(parseExtensions(getConfigProperty(ENDPOINTS_EXTENSIONS, context), context));
        endpointsConfig.setGroups(getListStringsProperty(ENDPOINTS_GROUPS, Collections.emptyList(), context));
        endpointsConfig.setGroupsExcluded(getListStringsProperty(ENDPOINTS_GROUPS_EXCLUDED, Collections.emptyList(), context));
        endpointsConfig.setEndpoints(endpointsProperties(context));

        mergeWithActuatorProperties(endpointsConfig, context);

        ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINTS, endpointsConfig, context);
        return endpointsConfig;
    }

    public static Map<String, EndpointProperties> endpointsProperties(VisitorContext context) {

        var endpointPropertiesMap = new HashMap<String, EndpointProperties>();

        // first read system properties
        Properties sysProps = System.getProperties();
        readEndpointsProperties(sysProps, endpointPropertiesMap, context);

        // second read openapi.properties file
        Properties fileProps = readOpenApiConfigFile(context);
        readEndpointsProperties(fileProps, endpointPropertiesMap, context);

        // third read environments properties
        Environment environment = getEnv(context);
        if (environment != null) {
            for (Entry<String, Object> entry : environment.getProperties(MICRONAUT_ENDPOINTS_PREFIX, StringConvention.RAW).entrySet()) {
                String entryKey = entry.getKey();
                var dotIndex = entryKey.indexOf('.');
                if (dotIndex < 0) {
                    continue;
                }
                String endpointName = entryKey.substring(0, dotIndex);
                String propName = entryKey.substring(endpointName.length() + 1);
                setEndpointProperty(endpointName, propName, entry.getValue(), endpointPropertiesMap, context);
            }
        }

        // set standard endpoints implementations, if not set in config
        for (var entry : ALL_MICRONAUT_MANAGEMENT_ENDPOINTS.entrySet()) {
            var endpointName = entry.getKey();
            var endpointProperties = endpointPropertiesMap.get(endpointName);
            if (endpointProperties == null) {
                endpointProperties = new EndpointProperties(endpointName);
                endpointPropertiesMap.put(endpointName, endpointProperties);
            }
            if (endpointProperties.getElement() != null) {
                continue;
            }
            var className = entry.getValue();
            if (className == null) {
                continue;
            }
            var classEl = ContextUtils.getClassElement(className, context);
            endpointProperties.setElement(classEl);
        }

        return endpointPropertiesMap;
    }

    private static void readEndpointsProperties(Properties props, Map<String, EndpointProperties> endpointPropertiesMap, VisitorContext context) {
        for (String prop : props.stringPropertyNames()) {
            if (!prop.startsWith(MICRONAUT_ENDPOINTS_PREFIX)) {
                continue;
            }
            int endpointNameIndexEnd = prop.indexOf('.', MICRONAUT_ENDPOINTS_PREFIX.length() + 1);
            if (endpointNameIndexEnd < 0) {
                continue;
            }
            String endpointName = prop.substring(MICRONAUT_ENDPOINTS_PREFIX.length() + 1, endpointNameIndexEnd);
            String propertyName = prop.substring(endpointNameIndexEnd + 1);
            String value = props.getProperty(prop);
            setEndpointProperty(endpointName, propertyName, value, endpointPropertiesMap, context);
        }
    }

    private static void setEndpointProperty(String endpointName, String propertyName, Object value, Map<String, EndpointProperties> endpointPropertiesMap, VisitorContext context) {
        if (value == null) {
            return;
        }
        String valueStr = value.toString();
        var simplePropName = propertyName.toLowerCase(Locale.ENGLISH);
        if (propertyName.contains(DOT)) {
            simplePropName = propertyName.substring(0, propertyName.indexOf(DOT)).toLowerCase(Locale.ENGLISH);
        }
        EndpointProperties endpointProperties = endpointPropertiesMap.computeIfAbsent(endpointName, EndpointProperties::new);
        switch (simplePropName) {
            case "enabled":
                if (endpointProperties.getEnabled() == null) {
                    endpointProperties.setEnabled(Boolean.parseBoolean(valueStr));
                }
                break;
            case "path":
                if (endpointProperties.getPath() == null) {
                    endpointProperties.setPath(valueStr);
                }
                break;
            case "context-path", "contextpath":
                if (ALL_ENDPOINTS_NAME.equals(endpointName) && endpointProperties.getContextPath() == null) {
                    endpointProperties.setContextPath(valueStr);
                }
                break;
            case "sensitive":
                if (endpointProperties.getSensitive() == null) {
                    endpointProperties.setSensitive(Boolean.parseBoolean(valueStr));
                }
                break;
            case "description":
                if (endpointProperties.getDescription() == null) {
                    endpointProperties.setDescription(valueStr);
                }
                break;
            case "extensions":
                if (CollectionUtils.isEmpty(endpointProperties.getExtensions())) {
                    endpointProperties.setExtensions(new HashMap<>());
                }
                var extName = propertyName.substring("extensions".length() + 1);
                endpointProperties.getExtensions().put(prependIfMissing(extName, PREFIX_X), parseExtensions(valueStr, context));
                break;
            case "security-requirements", "securityrequirements":
                if (CollectionUtils.isEmpty(endpointProperties.getSecurityRequirements())) {
                    endpointProperties.setSecurityRequirements(new ArrayList<>());
                }
                endpointProperties.getSecurityRequirements().addAll(parseSecurityRequirements(valueStr, context));
                break;
            case "servers":
                if (CollectionUtils.isEmpty(endpointProperties.getServers())) {
                    endpointProperties.setServers(new ArrayList<>());
                }
                endpointProperties.getServers().addAll(parseServers(valueStr, context));
                break;
            case "tags":
                if (CollectionUtils.isEmpty(endpointProperties.getTags())) {
                    endpointProperties.setTags(new ArrayList<>());
                }
                endpointProperties.getTags().addAll(parseTags(valueStr));
                break;
            case "class":
                if (endpointProperties.getElement() == null) {
                    endpointProperties.setElement(ContextUtils.getClassElement(valueStr, context));
                }
                break;
            default:
                break;
        }
    }

    public static Map<String, Object> parseExtensions(String value, VisitorContext context) {
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyMap();
        }
        try {
            return OpenApiUtils.getConvertJsonMapper().readValue(value, TYPE_EXTENSIONS);
        } catch (JsonProcessingException e) {
            warn("Fail to parse " + TYPE_EXTENSIONS.getType().toString() + ": " + value + " - " + e.getMessage(), context);
        }
        return Collections.emptyMap();
    }

    public static List<Server> parseServers(String servers, VisitorContext context) {
        return parseModel(servers, context, TYPE_SERVERS_LIST);
    }

    public static List<SecurityRequirement> parseSecurityRequirements(String securityRequirements, VisitorContext context) {
        return parseModel(securityRequirements, context, TYPE_SECURITY_REQUIREMENTS_LIST);
    }

    private static <T> List<T> parseModel(String s, VisitorContext context, TypeReference<List<T>> typeReference) {
        if (StringUtils.isEmpty(s) || (!s.startsWith("[") && !s.endsWith("]"))) {
            return Collections.emptyList();
        }
        try {
            return OpenApiUtils.getConvertJsonMapper().readValue(s, typeReference);
        } catch (JsonProcessingException e) {
            warn("Fail to parse " + typeReference.getType().toString() + ": " + s + " - " + e.getMessage(), context);
        }
        return Collections.emptyList();
    }

    public static List<Tag> parseTags(String stringTagsStr) {

        if (StringUtils.isEmpty(stringTagsStr)) {
            return Collections.emptyList();
        }
        String[] stringTags = stringTagsStr.split(COMMA);
        if (stringTags.length == 0) {
            return Collections.emptyList();
        }
        var tags = new ArrayList<Tag>(stringTags.length);
        for (String name : stringTags) {
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            tags.add(new Tag().name(name));
        }
        return tags;
    }

    private static GroupProperties.PackageProperties getPackageProperties(String groupPackage) {
        groupPackage = groupPackage.strip();
        boolean includeSubpackages = groupPackage.endsWith(WILDCARD);
        if (includeSubpackages) {
            groupPackage = groupPackage.substring(0, groupPackage.length() - 2);
        }
        if (groupPackage.endsWith(DOT)) {
            groupPackage = groupPackage.substring(0, groupPackage.length() - 2);
        }
        return new GroupProperties.PackageProperties(groupPackage, includeSubpackages);
    }

    private static void readSchemaDecorators(Properties props, Map<String, SchemaDecorator> schemaDecorators, VisitorContext context) {

        for (String prop : props.stringPropertyNames()) {
            boolean isPrefix = false;
            String packageName = null;
            if (prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_PREFIX)) {
                packageName = prop.substring(MICRONAUT_OPENAPI_SCHEMA_PREFIX.length() + 1);
                isPrefix = true;
            } else if (prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_POSTFIX)) {
                packageName = prop.substring(MICRONAUT_OPENAPI_SCHEMA_POSTFIX.length() + 1);
            } else if (prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_PREFIX)) {
                packageName = prop.substring(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_PREFIX.length() + 1);
                isPrefix = true;
            } else if (prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_POSTFIX)) {
                packageName = prop.substring(MICRONAUT_OPENAPI_SCHEMA_DECORATOR_POSTFIX.length() + 1);
            }
            if (StringUtils.isEmpty(packageName)) {
                continue;
            }
            SchemaDecorator schemaDecorator = schemaDecorators.computeIfAbsent(packageName, k -> new SchemaDecorator());
            if (isPrefix) {
                schemaDecorator.setPrefix(props.getProperty(prop));
            } else {
                schemaDecorator.setPostfix(props.getProperty(prop));
            }
        }
    }

    private static void readCustomSchemas(Properties props, Map<String, CustomSchema> customSchemas, VisitorContext context) {

        for (String prop : props.stringPropertyNames()) {

            // Remove this check, after we remove MICRONAUT_OPENAPI_SCHEMA property
            if (isMicronautProperty(prop)) {
                continue;
            }

            String className;
            if (prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_MAPPING)) {
                className = prop.substring(MICRONAUT_OPENAPI_SCHEMA_MAPPING.length() + 1);
            } else if (prop.startsWith(MICRONAUT_OPENAPI_SCHEMA)) {
                className = prop.substring(MICRONAUT_OPENAPI_SCHEMA.length() + 1);
            } else {
                continue;
            }
            String targetClassName = props.getProperty(prop);
            readCustomSchema(className, targetClassName, customSchemas, context);
        }
    }

    private static void readCustomSchema(String className, String targetClassName, Map<String, CustomSchema> customSchemas, VisitorContext context) {
        if (customSchemas.containsKey(className) || context == null) {
            return;
        }
        ClassElement targetClassElement = ContextUtils.getClassElement(targetClassName, context);
        if (targetClassElement == null) {
            warn("Can't find class " + targetClassName + " for className " + className + " in classpath. Custom schemas: " + customSchemas + ". Skip it.", context);
            return;
        }

        List<String> configuredTypeArgs = null;
        int genericNameStart = className.indexOf('<');
        if (genericNameStart > 0) {
            String[] generics = className.substring(genericNameStart + 1, className.indexOf('>')).split(COMMA);
            configuredTypeArgs = new ArrayList<>();
            for (String generic : generics) {
                configuredTypeArgs.add(generic.strip());
            }
        }

        customSchemas.put(className, new CustomSchema(configuredTypeArgs, targetClassElement));
    }

    @Nullable
    public static Path getProjectPath(VisitorContext context) {

        Path projectPath = ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR, Path.class, context);
        if (projectPath != null) {
            return projectPath;
        }

        String projectDir = ContextUtils.getOptions(context).get(MICRONAUT_OPENAPI_PROJECT_DIR);
        if (projectDir != null) {
            projectPath = Path.of(projectDir);
            // calculating classes output path for KSP and gradle
            if (isKsp(context)) {
                var classesOutputDir = projectPath.toString().replace('\\', '/') + "/build/generated/ksp/main/resources";
                ContextUtils.put(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, classesOutputDir, context);
            }
        }
        if (projectPath == null) {
            try {
                if (context != null) {
                    projectPath = getProjectDir(context);
                }
                if (projectPath == null) {
                    projectPath = context.getProjectDir().orElse(null);
                }
                if (projectPath == null && Utils.isTestMode()) {
                    projectPath = Path.of(System.getProperty("user.dir"));
                }
            } catch (Exception e) {
                // Should never happen
                projectPath = Path.of(System.getProperty("user.dir"));
            }
        }

        ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR, projectPath, context);

        return projectPath;
    }

    public static PropertyNamingStrategies.NamingBase getPropertyNamingStrategy(VisitorContext context) {
        var value = getConfigProperty(MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, context);
        if (value == null) {
            return null;
        }
        return toJacksonStrategy(value.toUpperCase(Locale.ENGLISH), context);
    }

    private static PropertyNamingStrategies.NamingBase toJacksonStrategy(String namingStrategy, VisitorContext context) {
        if (namingStrategy == null) {
            return null;
        }
        return (PropertyNamingStrategies.NamingBase) switch (namingStrategy.toUpperCase(Locale.ENGLISH)) {
            case "LOWER_CAMEL_CASE" -> PropertyNamingStrategies.LOWER_CAMEL_CASE;
            case "UPPER_CAMEL_CASE" -> PropertyNamingStrategies.UPPER_CAMEL_CASE;
            case "SNAKE_CASE" -> PropertyNamingStrategies.SNAKE_CASE;
            case "UPPER_SNAKE_CASE" -> PropertyNamingStrategies.UPPER_SNAKE_CASE;
            case "LOWER_CASE" -> PropertyNamingStrategies.LOWER_CASE;
            case "KEBAB_CASE" -> PropertyNamingStrategies.KEBAB_CASE;
            case "LOWER_DOT_CASE" -> PropertyNamingStrategies.LOWER_DOT_CASE;
            default -> {
                warn("Unknown naming strategy value: " + namingStrategy, context);
                yield null;
            }
        };
    }

    public static boolean isIncludeAll(VisitorContext context) {
        var value = getConfigProperty(MICRONAUT_OPENAPI_PROPERTY_INCLUDE, context);
        if (value == null) {
            return false;
        }
        // ALWAYS is the only value that has any impact on API spec generation.
        if (value.toUpperCase(Locale.ENGLISH).equals("ALWAYS")) {
            return true;
        }
        return false;
    }

    public static String getConfigProperty(String key, VisitorContext context) {

        if (context != null) {
            Boolean isLoaded = ContextUtils.get(key + LOADED_POSTFIX, Boolean.class, context);
            if (isLoaded != null) {
                return ContextUtils.get(key + VALUE_POSTFIX, String.class, context);
            }
        }

        String value;
        // if this option, need to check context.options
        if (ALL.contains(key) && context != null) {
            value = ContextUtils.getOptions(context).get(key);
        } else {
            value = System.getProperty(key);
        }

        if (value == null) {
            value = readOpenApiConfigFile(context).getProperty(key);
        }
        if (value != null) {
            return value;
        }
        Environment environment = getEnv(context);
        value = environment != null ? environment.get(key, String.class).orElse(null) : null;

        if (context != null) {
            ContextUtils.put(key + LOADED_POSTFIX, true, context);
            if (value != null) {
                ContextUtils.put(key + VALUE_POSTFIX, value, context);
            }
        }

        return value;
    }

    public static List<String> getAdditionalFiles(VisitorContext context) {
        return getListStringsProperty(MICRONAUT_OPENAPI_ADDITIONAL_FILES, null, context);
    }

    public static MergeMode getAdditionalFilesMergeMode(VisitorContext context) {
        String str = getConfigProperty(MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, context);
        if (StringUtils.isEmpty(str)) {
            return MergeMode.REPLACE;
        }
        try {
            return MergeMode.valueOf(str.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            warn("Unknown additional files mergeMode value: " + str, context);
            return MergeMode.REPLACE;
        }
    }

    public static boolean getBooleanProperty(String property, boolean defaultValue, VisitorContext context) {
        String str = getConfigProperty(property, context);
        if (StringUtils.isEmpty(str)) {
            return defaultValue;
        }
        return !StringUtils.FALSE.equalsIgnoreCase(str);
    }

    public static Properties readOpenApiConfigFile(VisitorContext context) {
        Properties props = ContextUtils.get(MICRONAUT_INTERNAL_OPENAPI_PROPERTIES, Properties.class, context);
        if (props != null) {
            return props;
        }
        var openApiProperties = new Properties();
        String cfgFile = context != null
            ? ContextUtils.getOptions(context).getOrDefault(MICRONAUT_OPENAPI_CONFIG_FILE, System.getProperty(MICRONAUT_OPENAPI_CONFIG_FILE, OPENAPI_CONFIG_FILE))
            : System.getProperty(MICRONAUT_OPENAPI_CONFIG_FILE, OPENAPI_CONFIG_FILE);
        if (StringUtils.isNotEmpty(cfgFile)) {
            Path cfg = resolve(context, Path.of(cfgFile));
            if (Files.isReadable(cfg)) {
                try (Reader reader = Files.newBufferedReader(cfg)) {
                    openApiProperties.load(reader);
                } catch (IOException e) {
                    warn("Fail to read OpenAPI configuration file: " + e.getMessage(), null);
                }
            } else if (Files.exists(cfg)) {
                warn("Can not read configuration file: " + cfg, context);
            }
        }
        if (context != null) {
            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_PROPERTIES, openApiProperties, context);
        }
        return openApiProperties;
    }

    @Nullable
    public static Environment getEnv(VisitorContext context) {
        if (!isEnvEnabled(context)) {
            return null;
        }

        Boolean envCreated = ContextUtils.get(MICRONAUT_INTERNAL_ENVIRONMENT_CREATED, Boolean.class, context);
        if (envCreated != null && envCreated) {
            return ContextUtils.get(MICRONAUT_INTERNAL_ENVIRONMENT, Environment.class, context);
        }

        Environment environment = createEnv(context);
        ContextUtils.put(MICRONAUT_INTERNAL_ENVIRONMENT, environment, context);
        ContextUtils.put(MICRONAUT_INTERNAL_ENVIRONMENT_CREATED, true, context);

        return environment;
    }

    private static Environment createEnv(VisitorContext context) {

        var configuration = new ApplicationContextConfiguration() {
            @Override
            public Optional<MutableConversionService> getConversionService() {
                var conversionService = new DefaultMutableConversionService();
                conversionService.addConverter(Map.class, InterceptUrlMapPattern.class, new InterceptUrlMapConverter(conversionService));
                return Optional.of(conversionService);
            }

            @Override
            public ClassPathResourceLoader getResourceLoader() {
                var classLoader = ApplicationContextConfiguration.class.getClassLoader();
                if (classLoader == null) {
                    classLoader = Thread.currentThread().getContextClassLoader();
                }
                if (classLoader == null) {
                    classLoader = ClassPathResourceLoader.class.getClassLoader();
                }
                if (classLoader == null) {
                    classLoader = ClassLoader.getSystemClassLoader();
                }
                return new DefaultClassPathResourceLoader(classLoader, null, false, false);
            }

            @Override
            @NonNull
            public List<String> getEnvironments() {
                return getActiveEnvs(context);
            }
        };

        Environment environment = null;
        try {
            environment = new AnnProcessorEnvironment(configuration, context);
            environment.start();
            return environment;
        } catch (Exception e) {
            warn("Can't create environment: " + e.getMessage() + ".\n" + Utils.printStackTrace(e), context);
        }
        return environment;
    }

    public static List<String> getActiveEnvs(VisitorContext context) {

        if (!isEnvEnabled(context)) {
            return Collections.emptyList();
        }

        String activeEnvStr = System.getProperty(MICRONAUT_OPENAPI_ENVIRONMENTS, readOpenApiConfigFile(context).getProperty(MICRONAUT_OPENAPI_ENVIRONMENTS));
        var activeEnvs = new ArrayList<String>();
        if (StringUtils.isNotEmpty(activeEnvStr)) {
            for (var activeEnv : activeEnvStr.split(COMMA)) {
                activeEnvs.add(activeEnv.strip());
            }
        }
        return activeEnvs;
    }

    private static boolean isEnvEnabled(VisitorContext context) {

        if (context == null) {
            return true;
        }

        boolean isEnabled = true;
        String isEnabledStr = ContextUtils.getOptions(context).get(MICRONAUT_ENVIRONMENT_ENABLED);
        if (StringUtils.isEmpty(isEnabledStr)) {
            isEnabledStr = readOpenApiConfigFile(context).getProperty(MICRONAUT_ENVIRONMENT_ENABLED);
        }
        if (StringUtils.isNotEmpty(isEnabledStr)) {
            isEnabled = Boolean.parseBoolean(isEnabledStr);
        }
        ContextUtils.put(MICRONAUT_ENVIRONMENT_ENABLED, isEnabled, context);
        return isEnabled;
    }

    /**
     * Custom schema class.
     *
     * @param typeArgs type arguments
     * @param classElement class element
     */
    public record CustomSchema(
        List<String> typeArgs,
        ClassElement classElement
    ) {
    }

    /**
     * Information about decorator.
     */
    public static final class SchemaDecorator {

        private String prefix;
        private String postfix;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getPostfix() {
            return postfix;
        }

        public void setPostfix(String postfix) {
            this.postfix = postfix;
        }
    }

    /**
     * Duplicate schema resolution mode.
     */
    public enum DuplicateResolution {
        AUTO,
        ERROR,
    }

    /**
     * Merge mode for additional OpenAPI specification files.
     */
    public enum MergeMode {
        APPEND,
        REPLACE,
    }
}
