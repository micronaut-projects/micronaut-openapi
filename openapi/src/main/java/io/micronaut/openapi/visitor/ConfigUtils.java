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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

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
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.micronaut.openapi.visitor.group.RouterVersioningProperties;
import io.micronaut.openapi.visitor.security.InterceptUrlMapConverter;
import io.micronaut.openapi.visitor.security.InterceptUrlMapPattern;
import io.micronaut.openapi.visitor.security.SecurityProperties;

import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CUSTOM_SCHEMAS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENVIRONMENT;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENVIRONMENT_CREATED;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_GROUPS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_DEFAULT_INCLUSION;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_ENABLED;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_SCHEMA_DECORATORS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_SECURITY_PROPERTIES;
import static io.micronaut.openapi.visitor.ContextUtils.ARGUMENT_CUSTOM_SCHEMA_MAP;
import static io.micronaut.openapi.visitor.ContextUtils.ARGUMENT_GROUP_PROPERTIES_MAP;
import static io.micronaut.openapi.visitor.ContextUtils.ARGUMENT_SCHEMA_DECORATORS_MAP;
import static io.micronaut.openapi.visitor.ContextUtils.EXPANDABLE_PROPERTIES_ARGUMENT;
import static io.micronaut.openapi.visitor.FileUtils.calcFinalFilename;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ALL;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_ENVIRONMENT_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_JACKSON_JSON_VIEW_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENVIRONMENTS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_EXPAND_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_PROJECT_DIR;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_POSTFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_PREFIX;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SECURITY_DEFAULT_SCHEMA_NAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_SECURITY_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VERSIONING_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.OPENAPI_CONFIG_FILE;
import static io.micronaut.openapi.visitor.group.RouterVersioningProperties.DEFAULT_HEADER_NAME;
import static io.micronaut.openapi.visitor.group.RouterVersioningProperties.DEFAULT_PARAMETER_NAME;

/**
 * Configuration utilities methods.
 *
 * @since 4.10.0
 */
@Internal
public final class ConfigUtils {

    private static final String LOADED_POSTFIX = ".loaded";
    private static final String VALUE_POSTFIX = ".value";

    /**
     * Default autogenerated security schema name.
     */
    private static final String DEFAULT_SECURITY_SCHEMA_NAME = "Authorization";

    private ConfigUtils() {
    }

    public static SchemaDecorator getSchemaDecoration(String packageName, VisitorContext context) {

        Map<String, SchemaDecorator> schemaDecorators = context.get(MICRONAUT_INTERNAL_SCHEMA_DECORATORS, ARGUMENT_SCHEMA_DECORATORS_MAP).orElse(null);
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
            for (Map.Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_PREFIX, StringConvention.RAW).entrySet()) {
                SchemaDecorator decorator = schemaDecorators.get(entry.getKey());
                if (decorator == null) {
                    decorator = new SchemaDecorator();
                    schemaDecorators.put(entry.getKey(), decorator);
                }
                decorator.setPrefix((String) entry.getValue());
            }

            for (Map.Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA_POSTFIX, StringConvention.RAW).entrySet()) {
                SchemaDecorator decorator = schemaDecorators.get(entry.getKey());
                if (decorator == null) {
                    decorator = new SchemaDecorator();
                    schemaDecorators.put(entry.getKey(), decorator);
                }
                decorator.setPostfix((String) entry.getValue());
            }
        }

        context.put(MICRONAUT_INTERNAL_SCHEMA_DECORATORS, schemaDecorators);

        return schemaDecorators.get(packageName);
    }

    public static ClassElement getCustomSchema(String className, Map<String, ClassElement> typeArgs, VisitorContext context) {

        Map<String, CustomSchema> customSchemas = context.get(MICRONAUT_INTERNAL_CUSTOM_SCHEMAS, ARGUMENT_CUSTOM_SCHEMA_MAP).orElse(null);
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
            for (Map.Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_SCHEMA, StringConvention.RAW).entrySet()) {
                String configuredClassName = entry.getKey();
                String targetClassName = (String) entry.getValue();
                readCustomSchema(configuredClassName, targetClassName, customSchemas, context);
            }
        }

        context.put(MICRONAUT_INTERNAL_CUSTOM_SCHEMAS, customSchemas);

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

    private static String getClassNameWithGenerics(String className, Map<String, ClassElement> typeArgs) {
        StringBuilder key = new StringBuilder(className);
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

    public static boolean isOpenApiEnabled(VisitorContext context) {
        Boolean loadedValue = context.get(MICRONAUT_INTERNAL_OPENAPI_ENABLED, Boolean.class).orElse(null);
        if (loadedValue != null) {
            return loadedValue;
        }
        boolean value = getBooleanProperty(MICRONAUT_OPENAPI_ENABLED, true, context);
        context.put(MICRONAUT_INTERNAL_OPENAPI_ENABLED, value);

        System.setProperty(MICRONAUT_OPENAPI_ENABLED, Boolean.toString(value));
        return value;
    }

    public static List<Pair<String, String>> getExpandableProperties(VisitorContext context) {

        boolean propertiesLoaded = context.get(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED, Boolean.class).orElse(false);
        if (propertiesLoaded) {
            return context.get(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES, EXPANDABLE_PROPERTIES_ARGUMENT).orElse(null);
        }

        List<Pair<String, String>> expandableProperties = new ArrayList<>();

        // first, check system properties and environments config files
        AnnProcessorEnvironment env = (AnnProcessorEnvironment) getEnv(context);
        Map<String, Object> propertiesFromEnv = null;
        if (env != null) {
            try {
                propertiesFromEnv = env.getProperties(MICRONAUT_OPENAPI_EXPAND_PREFIX.substring(0, MICRONAUT_OPENAPI_EXPAND_PREFIX.length() - 1), null);
            } catch (Exception e) {
                context.warn("Error:\n" + Utils.printStackTrace(e), null);
            }
        }

        Map<String, String> expandedPropsMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(propertiesFromEnv)) {
            for (Map.Entry<String, Object> entry : propertiesFromEnv.entrySet()) {
                expandedPropsMap.put(entry.getKey(), entry.getValue().toString());
            }
        }

        // next, read openapi.properties file
        Properties openapiProps = readOpenApiConfigFile(context);
        for (Map.Entry<Object, Object> entry : openapiProps.entrySet()) {
            String key = entry.getKey().toString();
            if (!key.startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX)) {
                continue;
            }
            expandedPropsMap.put(key, entry.getValue().toString());
        }

        // next, read system properties
        if (CollectionUtils.isNotEmpty(System.getProperties())) {
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (!key.startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX)) {
                    continue;
                }
                expandedPropsMap.put(key, entry.getValue().toString());
            }
        }

        for (Map.Entry<String, String> entry : expandedPropsMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX)) {
                key = key.substring(MICRONAUT_OPENAPI_EXPAND_PREFIX.length());
            }
            Pair<String, String> prop = Pair.of("\\$\\{" + key + '}', entry.getValue());
            if (!expandableProperties.contains(prop)) {
                expandableProperties.add(prop);
            }
        }

        context.put(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES, expandableProperties);
        context.put(MICRONAUT_INTERNAL_EXPANDABLE_PROPERTIES_LOADED, true);

        return expandableProperties;
    }

    public static Map<String, String> getAdocProperties(OpenApiInfo openApiInfo, boolean isSingleGroup, VisitorContext context) {

        var adocProperties = new HashMap<String, String>();
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH, getConfigProperty(MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME, getConfigProperty(MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, getConfigProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, getConfigProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, context));
        adocProperties.put(MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH, getConfigProperty(MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH, context));

        // first, check system properties and environments config files
        var env = (AnnProcessorEnvironment) getEnv(context);
        Map<String, Object> propertiesFromEnv = null;
        if (env != null) {
            try {
                propertiesFromEnv = env.getProperties(MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX.substring(0, MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX.length() - 1), null);
            } catch (Exception e) {
                context.warn("Error:\n" + Utils.printStackTrace(e), null);
            }
        }

        if (CollectionUtils.isNotEmpty(propertiesFromEnv)) {
            for (var entry : propertiesFromEnv.entrySet()) {
                adocProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }

        // next, read openapi.properties file
        Properties openapiProps = readOpenApiConfigFile(context);
        for (Map.Entry<Object, Object> entry : openapiProps.entrySet()) {
            String key = entry.getKey().toString();
            if (!key.startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX)) {
                continue;
            }
            adocProperties.put(key, entry.getValue().toString());
        }

        // next, read system properties
        if (CollectionUtils.isNotEmpty(System.getProperties())) {
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (!key.startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX)) {
                    continue;
                }
                adocProperties.put(key, entry.getValue().toString());
            }
        }

        var fileName = StringUtils.isNotEmpty(openApiInfo.getAdocFilename()) ? openApiInfo.getAdocFilename() : adocProperties.get(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME);
        var titleAndFilename = calcFinalFilename(openApiInfo.getAdocFilename(), openApiInfo, isSingleGroup, "adoc", context);

        return adocProperties;
    }

    public static boolean isJsonViewEnabled(VisitorContext context) {

        Boolean isJsonViewEnabled = context.get(MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_ENABLED, Boolean.class).orElse(null);
        if (isJsonViewEnabled != null) {
            return isJsonViewEnabled;
        }

        isJsonViewEnabled = getBooleanProperty(MICRONAUT_JACKSON_JSON_VIEW_ENABLED, false, context);
        context.put(MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_ENABLED, isJsonViewEnabled);

        return isJsonViewEnabled;
    }

    public static boolean isJsonViewDefaultInclusion(VisitorContext context) {

        Boolean isJsonViewDefaultInclusion = context.get(MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_DEFAULT_INCLUSION, Boolean.class).orElse(null);
        if (isJsonViewDefaultInclusion != null) {
            return isJsonViewDefaultInclusion;
        }

        isJsonViewDefaultInclusion = getBooleanProperty(MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION, true, context);
        context.put(MICRONAUT_INTERNAL_JACKSON_JSON_VIEW_DEFAULT_INCLUSION, isJsonViewDefaultInclusion);

        return isJsonViewDefaultInclusion;
    }

    public static SecurityProperties getSecurityProperties(VisitorContext context) {

        SecurityProperties securityProperties = context.get(MICRONAUT_INTERNAL_SECURITY_PROPERTIES, SecurityProperties.class).orElse(null);
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

        context.put(MICRONAUT_INTERNAL_SECURITY_PROPERTIES, securityProperties);

        return securityProperties;
    }

    public static RouterVersioningProperties getRouterVersioningProperties(VisitorContext context) {

        RouterVersioningProperties routerVersioningProperties = context.get(MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES, RouterVersioningProperties.class).orElse(null);
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

        context.put(MICRONAUT_INTERNAL_ROUTER_VERSIONING_PROPERTIES, routerVersioningProperties);

        return routerVersioningProperties;
    }

    public static List<String> getListStringsProperty(String property, List<String> defaultValue, VisitorContext context) {
        String strValue = System.getProperty(property);
        if (StringUtils.isEmpty(strValue)) {
            strValue = readOpenApiConfigFile(context).getProperty(property);
        }
        if (StringUtils.isNotEmpty(strValue)) {
            List<String> result = new ArrayList<>();
            for (String item : strValue.split(",")) {
                result.add(item.trim());
            }
            return result;
        }

        Environment env = getEnv(context);
        if (env != null) {
            return env.get(property, Argument.LIST_OF_STRING).orElse(defaultValue);
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
        Map<String, GroupProperties> groupPropertiesMap = context.get(MICRONAUT_INTERNAL_GROUPS, ARGUMENT_GROUP_PROPERTIES_MAP).orElse(null);
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
            for (Map.Entry<String, Object> entry : environment.getProperties(MICRONAUT_OPENAPI_GROUPS, StringConvention.RAW).entrySet()) {
                String entryKey = entry.getKey();
                String[] propParts = entryKey.split("\\.");
                String propName = propParts[propParts.length - 1];
                String groupName = entryKey.substring(0, entryKey.length() - propName.length() - 1);
                setGroupProperty(groupName, propName, entry.getValue(), groupPropertiesMap, context);
            }
        }

        Utils.getAllKnownGroups().addAll(groupPropertiesMap.keySet());

        context.put(MICRONAUT_INTERNAL_GROUPS, groupPropertiesMap);

        return groupPropertiesMap;
    }

    /**
     * Returns the EndpointsConfiguration.
     *
     * @param context The context.
     *
     * @return The EndpointsConfiguration.
     */
    public static EndpointsConfiguration endpointsConfiguration(VisitorContext context) {
        Optional<EndpointsConfiguration> cfg = context.get(MICRONAUT_INTERNAL_ENVIRONMENT_CREATED, EndpointsConfiguration.class);
        if (cfg.isPresent()) {
            return cfg.get();
        }
        var conf = new EndpointsConfiguration(context, readOpenApiConfigFile(context));
        context.put(MICRONAUT_INTERNAL_ENVIRONMENT_CREATED, conf);
        return conf;
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
        switch (propertyName.toLowerCase()) {
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
                    List<GroupProperties.PackageProperties> packages = new ArrayList<>();
                    for (String groupPackage : valueStr.split(",")) {
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
            case "commonexclude", "common-exclude":
                if (groupProperties.getCommonExclude() == null) {
                    groupProperties.setCommonExclude(Boolean.valueOf(valueStr));
                }
                break;
            case "packagesexclude", "packages-exclude":
                if (groupProperties.getPackagesExclude() == null) {
                    List<GroupProperties.PackageProperties> packagesExclude = new ArrayList<>();
                    for (String groupPackage : valueStr.split(",")) {
                        packagesExclude.add(getPackageProperties(groupPackage));
                    }
                    groupProperties.setPackagesExclude(packagesExclude);
                }
                break;
            default:
                break;
        }
    }

    private static GroupProperties.PackageProperties getPackageProperties(String groupPackage) {
        groupPackage = groupPackage.trim();
        boolean includeSubpackages = groupPackage.endsWith("*");
        if (includeSubpackages) {
            groupPackage = groupPackage.substring(0, groupPackage.length() - 2);
        }
        if (groupPackage.endsWith(".")) {
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
            }
            if (StringUtils.isEmpty(packageName)) {
                continue;
            }
            SchemaDecorator schemaDecorator = schemaDecorators.get(packageName);
            if (schemaDecorator == null) {
                schemaDecorator = new SchemaDecorator();
                schemaDecorators.put(packageName, schemaDecorator);
            }
            if (isPrefix) {
                schemaDecorator.setPrefix(props.getProperty(prop));
            } else {
                schemaDecorator.setPostfix(props.getProperty(prop));
            }
        }
    }

    private static void readCustomSchemas(Properties props, Map<String, CustomSchema> customSchemas, VisitorContext context) {

        for (String prop : props.stringPropertyNames()) {
            if (!prop.startsWith(MICRONAUT_OPENAPI_SCHEMA) || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_PREFIX) || prop.startsWith(MICRONAUT_OPENAPI_SCHEMA_POSTFIX)) {
                continue;
            }
            String className = prop.substring(MICRONAUT_OPENAPI_SCHEMA.length() + 1);
            String targetClassName = props.getProperty(prop);
            readCustomSchema(className, targetClassName, customSchemas, context);
        }
    }

    private static void readCustomSchema(String className, String targetClassName, Map<String, CustomSchema> customSchemas, VisitorContext context) {
        if (customSchemas.containsKey(className)) {
            return;
        }
        ClassElement targetClassElement = context.getClassElement(targetClassName).orElse(null);
        if (targetClassElement == null) {
            context.warn("Can't find class " + targetClassName + " in classpath. Skip it.", null);
            return;
        }

        List<String> configuredTypeArgs = null;
        int genericNameStart = className.indexOf('<');
        if (genericNameStart > 0) {
            String[] generics = className.substring(genericNameStart + 1, className.indexOf('>')).split(",");
            configuredTypeArgs = new ArrayList<>();
            for (String generic : generics) {
                configuredTypeArgs.add(generic.trim());
            }
        }

        customSchemas.put(className, new CustomSchema(configuredTypeArgs, targetClassElement));
    }

    @Nullable
    public static Path getProjectPath(VisitorContext context) {

        Path projectPath = context.get(MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR, Path.class).orElse(null);
        if (projectPath != null) {
            return projectPath;
        }

        String projectDir = context.getOptions().get(MICRONAUT_OPENAPI_PROJECT_DIR);
        if (projectDir != null) {
            projectPath = Paths.get(projectDir);
        }
        if (projectPath == null) {
            try {
                projectPath = context.getProjectDir().orElse(Utils.isTestMode() ? Paths.get(System.getProperty("user.dir")) : null);
            } catch (Exception e) {
                // Should never happen
                projectPath = Paths.get(System.getProperty("user.dir"));
            }
        }

        context.put(MICRONAUT_INTERNAL_OPENAPI_PROJECT_DIR, projectPath);

        return projectPath;
    }

    public static String getConfigProperty(String key, VisitorContext context) {

        if (context != null) {
            boolean isLoaded = context.get(key + LOADED_POSTFIX, Boolean.class).orElse(false);
            if (isLoaded) {
                return context.get(key + VALUE_POSTFIX, String.class).orElse(null);
            }
        }

        String value;
        // if this option, need to check context.options
        if (ALL.contains(key) && context != null) {
            value = context.getOptions().get(key);
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
            context.put(key + LOADED_POSTFIX, true);
            if (value != null) {
                context.put(key + VALUE_POSTFIX, value);
            }
        }

        return value;
    }

    public static boolean getBooleanProperty(String property, boolean defaultValue, VisitorContext context) {
        String str = getConfigProperty(property, context);
        if (StringUtils.isEmpty(str)) {
            return defaultValue;
        }
        return !StringUtils.FALSE.equalsIgnoreCase(str);
    }

    public static Properties readOpenApiConfigFile(VisitorContext context) {
        Optional<Properties> props = context != null ? context.get(MICRONAUT_INTERNAL_OPENAPI_PROPERTIES, Properties.class) : Optional.empty();
        if (props.isPresent()) {
            return props.get();
        }
        Properties openApiProperties = new Properties();
        String cfgFile = context != null ? context.getOptions().getOrDefault(MICRONAUT_OPENAPI_CONFIG_FILE, OPENAPI_CONFIG_FILE) : System.getProperty(MICRONAUT_OPENAPI_CONFIG_FILE, OPENAPI_CONFIG_FILE);
        if (StringUtils.isNotEmpty(cfgFile)) {
            Path cfg = resolve(context, Paths.get(cfgFile));
            if (Files.isReadable(cfg)) {
                try (Reader reader = Files.newBufferedReader(cfg)) {
                    openApiProperties.load(reader);
                } catch (IOException e) {
                    if (context != null) {
                        context.warn("Fail to read OpenAPI configuration file: " + e.getMessage(), null);
                    }
                }
            } else if (Files.exists(cfg)) {
                if (context != null) {
                    context.warn("Can not read configuration file: " + cfg, null);
                }
            }
        }
        if (context != null) {
            context.put(MICRONAUT_INTERNAL_OPENAPI_PROPERTIES, openApiProperties);
        }
        return openApiProperties;
    }

    @Nullable
    public static Environment getEnv(VisitorContext context) {
        if (!isEnvEnabled(context)) {
            return null;
        }

        Boolean envCreated = context != null ? context.get(MICRONAUT_INTERNAL_ENVIRONMENT_CREATED, Boolean.class).orElse(null) : null;
        if (envCreated != null && envCreated) {
            return context.get(MICRONAUT_INTERNAL_ENVIRONMENT, Environment.class).orElse(null);
        }

        Environment environment = createEnv(context);
        if (context != null) {
            context.put(MICRONAUT_INTERNAL_ENVIRONMENT, environment);
            context.put(MICRONAUT_INTERNAL_ENVIRONMENT_CREATED, true);
        }

        return environment;
    }

    private static Environment createEnv(VisitorContext context) {

        ApplicationContextConfiguration configuration = new ApplicationContextConfiguration() {
            @Override
            public Optional<MutableConversionService> getConversionService() {
                MutableConversionService conversionService = new DefaultMutableConversionService();
                conversionService.addConverter(Map.class, InterceptUrlMapPattern.class, new InterceptUrlMapConverter(conversionService));
                return Optional.of(conversionService);
            }

            @Override
            public ClassPathResourceLoader getResourceLoader() {
                ClassLoader classLoader = ApplicationContextConfiguration.class.getClassLoader();
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
            if (context != null) {
                context.warn("Can't create environment: " + e.getMessage() + ".\n" + Utils.printStackTrace(e), null);
            }
        }
        return environment;
    }

    public static List<String> getActiveEnvs(VisitorContext context) {

        if (!isEnvEnabled(context)) {
            return Collections.emptyList();
        }

        String activeEnvStr = System.getProperty(MICRONAUT_OPENAPI_ENVIRONMENTS, readOpenApiConfigFile(context).getProperty(MICRONAUT_OPENAPI_ENVIRONMENTS));
        List<String> activeEnvs;
        if (StringUtils.isNotEmpty(activeEnvStr)) {
            activeEnvs = Stream.of(activeEnvStr)
                .filter(StringUtils::isNotEmpty)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .toList();
        } else {
            activeEnvs = new ArrayList<>();
        }
        return activeEnvs;
    }

    private static boolean isEnvEnabled(VisitorContext context) {

        if (context == null) {
            return true;
        }

        boolean isEnabled = true;
        String isEnabledStr = context.getOptions().get(MICRONAUT_ENVIRONMENT_ENABLED);
        if (StringUtils.isEmpty(isEnabledStr)) {
            isEnabledStr = readOpenApiConfigFile(context).getProperty(MICRONAUT_ENVIRONMENT_ENABLED);
        }
        if (StringUtils.isNotEmpty(isEnabledStr)) {
            isEnabled = Boolean.parseBoolean(isEnabledStr);
        }
        context.put(MICRONAUT_ENVIRONMENT_ENABLED, isEnabled);
        return isEnabled;
    }

    static final class CustomSchema {

        private final List<String> typeArgs;
        private final ClassElement classElement;

        private CustomSchema(List<String> typeArgs, ClassElement classElement) {
            this.typeArgs = typeArgs;
            this.classElement = classElement;
        }

        public List<String> getTypeArgs() {
            return typeArgs;
        }

        public ClassElement getClassElement() {
            return classElement;
        }
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
}
