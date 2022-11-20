/*
 * Copyright 2017-2020 original authors
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
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.GenericArgument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.openapi.postprocessors.JacksonDiscriminatorPostProcessor;
import io.micronaut.openapi.postprocessors.OpenApiOperationsPostProcessor;
import io.micronaut.openapi.view.OpenApiViewConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Visits the application class.
 *
 * @author graemerocher
 * @since 1.0
 */
@SupportedOptions({
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_VIEWS_SPEC,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_JSON_FORMAT,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENVIRONMENTS,
    OpenApiApplicationVisitor.MICRONAUT_ENVIRONMENT_ENABLED,
    OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_TARGET_FILE,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ADDITIONAL_FILES,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE,
})
public class OpenApiApplicationVisitor extends AbstractOpenApiVisitor implements TypeElementVisitor<OpenAPIDefinition, Object> {

    /**
     * System property that enables setting the open api config file.
     */
    public static final String MICRONAUT_OPENAPI_CONFIG_FILE = "micronaut.openapi.config.file";
    /**
     * Prefix for expandable properties.
     */
    public static final String MICRONAUT_OPENAPI_EXPAND_PREFIX = "micronaut.openapi.expand.";
    /**
     * System property for server context path.
     */
    public static final String MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH = "micronaut.openapi.server.context.path";
    /**
     * System property for naming strategy. One jackson PropertyNamingStrategy.
     */
    public static final String MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY = "micronaut.openapi.property.naming.strategy";
    /**
     * System property for views specification.
     */
    public static final String MICRONAUT_OPENAPI_VIEWS_SPEC = "micronaut.openapi.views.spec";
    /**
     * System property that enables setting the target file to write to.
     */
    public static final String MICRONAUT_OPENAPI_TARGET_FILE = "micronaut.openapi.target.file";
    /**
     * System property that specifies the location of additional swagger YAML and JSON files to read from.
     */
    public static final String MICRONAUT_OPENAPI_ADDITIONAL_FILES = "micronaut.openapi.additional.files";
    /**
     * Default openapi config file.
     */
    public static final String OPENAPI_CONFIG_FILE = "openapi.properties";
    /**
     * The name of the entry for Endpoint class tags in the context.
     */
    public static final String MICRONAUT_OPENAPI_ENDPOINT_CLASS_TAGS = "micronaut.openapi.endpoint.class.tags";
    /**
     * The name of the entry for Endpoint servers in the context.
     */
    public static final String MICRONAUT_OPENAPI_ENDPOINT_SERVERS = "micronaut.openapi.endpoint.servers";
    /**
     * The name of the entry for Endpoint security requirements in the context.
     */
    public static final String MICRONAUT_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS = "micronaut.openapi.endpoint.security.requirements";
    /**
     * Is this property true, output file format will be JSON, otherwise YAML.
     */
    public static final String MICRONAUT_OPENAPI_JSON_FORMAT = "micronaut.openapi.json.format";
    /**
     * Active micronaut environments which will be used for @Requires annotations.
     */
    public static final String MICRONAUT_OPENAPI_ENVIRONMENTS = "micronaut.openapi.environments";
    /**
     * Is this property true, properties wll be loaded in the standard way from application.yml.
     * Also, environments from "micronaut.openapi.environments" property will set as additional environments,
     * if you want to set specific environment name for openAPI generator.
     * <br>
     * Default value is "true".
     */
    public static final String MICRONAUT_ENVIRONMENT_ENABLED = "micronaut.environment.enabled";
    /**
     * Config file locations. By default, micronaut-openapi search config in standard path:
     * &lt;project_path&gt;/src/main/resources/
     * <p>
     * You can set your custom paths separated by ','. To set absolute paths use prefix 'file:',
     * classpath paths use prefix 'classpath:' or use prefix 'project:' to set paths from project
     * directory.
     */
    public static final String MICRONAUT_CONFIG_FILE_LOCATIONS = "micronaut.openapi.config.file.locations";

    /**
     * Loaded micronaut environment.
     */
    private static final String MICRONAUT_ENVIRONMENT = "micronaut.environment";
    private static final String MICRONAUT_ENVIRONMENT_CREATED = "micronaut.environment.created";
    private static final String MICRONAUT_OPENAPI_PROPERTIES = "micronaut.openapi.properties";
    private static final String MICRONAUT_OPENAPI_ENDPOINTS = "micronaut.openapi.endpoints";

    /**
     * Properties prefix to set custom schema implementations for selected clases.
     * For example, if you want to set simple 'java.lang.String' class to some complex 'org.somepackage.MyComplexType' class you need to write:
     * <p>
     * micronaut.openapi.schema.org.somepackage.MyComplexType=java.lang.String
     * <p>
     * Also, you can set it in your application.yml file like this:
     * <p>
     * micronaut:
     *   openapi:
     *     schema:
     *       org.somepackage.MyComplexType: java.lang.String
     *       org.somepackage.MyComplexType2: java.lang.Integer
     *       ...
     */
    private static final String MICRONAUT_OPENAPI_SCHEMA = "micronaut.openapi.schema";
    private static final String MICRONAUT_CUSTOM_SCHEMAS = "micronaut.internal.custom.schemas";
    /**
     * Loaded expandable properties. Need to save them to reuse in diffferent places.
     */
    private static final String MICRONAUT_INTERNAL_EXPANDBLE_PROPERTIES = "micronaut.internal.expandable.props";
    /**
     * Flag that shows that the expandable properties are already loaded into the context.
     */
    private static final String MICRONAUT_INTERNAL_EXPANDBLE_PROPERTIES_LOADED = "micronaut.internal.expandable.props.loaded";

    private static final Argument<List<Map.Entry<String, String>>> EXPANDABLE_PROPERTIES_ARGUMENT = new GenericArgument<List<Map.Entry<String, String>>>() { };

    private ClassElement classElement;
    private int visitedElements = -1;

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        incrementVisitedElements(context);
        context.info("Generating OpenAPI Documentation");
        OpenAPI openAPI = readOpenAPI(element, context);
        mergeAdditionalSwaggerFiles(element, context, openAPI);
        // handle type level tags
        List<io.swagger.v3.oas.models.tags.Tag> tagList = processOpenApiAnnotation(
                element,
                context,
                Tag.class,
                io.swagger.v3.oas.models.tags.Tag.class,
                openAPI.getTags()
        );
        openAPI.setTags(tagList);

        // handle type level security requirements
        List<io.swagger.v3.oas.models.security.SecurityRequirement> securityRequirements = readSecurityRequirements(element);
        if (openAPI.getSecurity() != null) {
            securityRequirements.addAll(openAPI.getSecurity());
        }

        openAPI.setSecurity(securityRequirements);

        // handle type level servers
        List<io.swagger.v3.oas.models.servers.Server> servers = processOpenApiAnnotation(
                element,
                context,
                Server.class,
                io.swagger.v3.oas.models.servers.Server.class,
                openAPI.getServers()
        );
        openAPI.setServers(servers);

        // Handle Application securityRequirements schemes
        processSecuritySchemes(element, context);

        Optional<OpenAPI> attr = context.get(Utils.ATTR_OPENAPI, OpenAPI.class);
        if (attr.isPresent()) {
            OpenAPI existing = attr.get();
            Optional.ofNullable(openAPI.getInfo())
                    .ifPresent(existing::setInfo);
            copyOpenAPI(existing, openAPI);
        } else {
            context.put(Utils.ATTR_OPENAPI, openAPI);
        }

        if (Utils.isTestMode()) {
            Utils.resolveOpenAPI(context);
        }

        classElement = element;
    }

    public static ClassElement getCustomSchema(String className, Map<String, ClassElement> typeArgs, VisitorContext context) {

        Map<String, CustomSchema> customSchemas = (Map<String, CustomSchema>) context.get(MICRONAUT_CUSTOM_SCHEMAS, Map.class).orElse(null);
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

        context.put(MICRONAUT_CUSTOM_SCHEMAS, customSchemas);

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

    private static void readCustomSchemas(Properties props, Map<String, CustomSchema> customSchemas, VisitorContext context) {

        for (String prop : props.stringPropertyNames()) {
            if (!prop.startsWith(MICRONAUT_OPENAPI_SCHEMA)) {
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

    public static String getConfigurationProperty(String key, VisitorContext context) {
        String value = System.getProperty(key, readOpenApiConfigFile(context).getProperty(key));
        if (value != null) {
            return value;
        }
        Environment environment = getEnv(context);
        return environment != null ? environment.get(key, String.class).orElse(null) : null;
    }

    @Nullable
    public static Environment getEnv(VisitorContext context) {
        Environment existedEnvironment = context != null ? context.get(MICRONAUT_ENVIRONMENT, Environment.class).orElse(null) : null;
        Boolean envCreated = context != null ? context.get(MICRONAUT_ENVIRONMENT_CREATED, Boolean.class).orElse(null) : null;
        if (envCreated != null && envCreated) {
            return existedEnvironment;
        }

        Environment environment = createEnv(context);
        if (context != null) {
            context.put(MICRONAUT_ENVIRONMENT, environment);
            context.put(MICRONAUT_ENVIRONMENT_CREATED, true);
        }

        return environment;
    }

    public static List<String> getActiveEnvs(VisitorContext context) {

        String isEnabledStr = System.getProperty(MICRONAUT_ENVIRONMENT_ENABLED, readOpenApiConfigFile(context).getProperty(MICRONAUT_ENVIRONMENT_ENABLED));
        boolean isEnabled = true;
        if (StringUtils.isNotEmpty(isEnabledStr)) {
            isEnabled = Boolean.parseBoolean(isEnabledStr);
        }
        if (context != null) {
            context.put(MICRONAUT_ENVIRONMENT_ENABLED, isEnabled);
        }
        if (!isEnabled) {
            return Collections.emptyList();
        }

        String activeEnvStr = System.getProperty(MICRONAUT_OPENAPI_ENVIRONMENTS, readOpenApiConfigFile(context).getProperty(MICRONAUT_OPENAPI_ENVIRONMENTS));
        List<String> activeEnvs;
        if (StringUtils.isNotEmpty(activeEnvStr)) {
            activeEnvs = Stream.of(activeEnvStr)
                .filter(StringUtils::isNotEmpty)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .collect(Collectors.toList());
        } else {
            activeEnvs = new ArrayList<>();
        }
        return activeEnvs;
    }

    private static Environment createEnv(VisitorContext context) {

        ApplicationContextConfiguration configuration = new ApplicationContextConfiguration() {
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
                context.warn("Can't create environment: " + e.getMessage(), null);
            }
        }
        return environment;
    }

    private ClassLoader resolveClassLoader() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return DefaultApplicationContextBuilder.class.getClassLoader();
    }

    /**
     * Merge the OpenAPI YAML and JSON files into one single file.
     *
     * @param element The element
     * @param context The visitor context
     * @param openAPI The {@link OpenAPI} object for the application
     */
    private void mergeAdditionalSwaggerFiles(ClassElement element, VisitorContext context, OpenAPI openAPI) {
        String additionalSwaggerFiles = getConfigurationProperty(MICRONAUT_OPENAPI_ADDITIONAL_FILES, context);
        if (StringUtils.isNotEmpty(additionalSwaggerFiles)) {
            Path directory = resolve(context, Paths.get(additionalSwaggerFiles));
            if (Files.isDirectory(directory)) {
                context.info("Merging Swagger OpenAPI YAML and JSON files from location: " + directory);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> path.toString().endsWith(".yml") || path.toString().endsWith(".json"))) {
                    stream.forEach(path -> {
                        boolean isYaml = path.toString().endsWith(".yml");
                        context.info("Reading Swagger OpenAPI " + (isYaml ? "YAML" : "JSON") + " file " + path.getFileName());
                        OpenAPI parsedOpenApi = null;
                        try {
                            parsedOpenApi = (isYaml ? ConvertUtils.getYamlMapper() : ConvertUtils.getJsonMapper()).readValue(path.toFile(), OpenAPI.class);
                        } catch (IOException e) {
                            context.warn("Unable to read file " + path.getFileName() + ": " + e.getMessage(), element);
                        }
                        copyOpenAPI(openAPI, parsedOpenApi);
                    });
                } catch (IOException e) {
                    context.warn("Unable to read  file from " + directory + ": " + e.getMessage(), element);
                }
            } else {
                context.warn(directory + " does not exist or is not a directory", element);
            }
        }
    }

    private static Path resolve(VisitorContext context, Path path) {
        if (!path.isAbsolute() && context != null) {
            Optional<Path> projectDir = context.getProjectDir();
            if (projectDir.isPresent()) {
                path = projectDir.get().resolve(path);
            }
        }
        return path.toAbsolutePath();
    }

    /**
     * Returns the EndpointsConfiguration.
     *
     * @param context The context.
     *
     * @return The EndpointsConfiguration.
     */
    static EndpointsConfiguration endPointsConfiguration(VisitorContext context) {
        Optional<EndpointsConfiguration> cfg = context.get(MICRONAUT_OPENAPI_ENDPOINTS, EndpointsConfiguration.class);
        if (cfg.isPresent()) {
            return cfg.get();
        }
        EndpointsConfiguration conf = new EndpointsConfiguration(context, readOpenApiConfigFile(context));
        context.put(MICRONAUT_OPENAPI_ENDPOINTS, conf);
        return conf;
    }

    public static Properties readOpenApiConfigFile(VisitorContext context) {
        Optional<Properties> props = context != null ? context.get(MICRONAUT_OPENAPI_PROPERTIES, Properties.class) : Optional.empty();
        if (props.isPresent()) {
            return props.get();
        }
        Properties openApiProperties = new Properties();
        String cfgFile = System.getProperty(MICRONAUT_OPENAPI_CONFIG_FILE, OPENAPI_CONFIG_FILE);
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
            context.put(MICRONAUT_OPENAPI_PROPERTIES, openApiProperties);
        }
        return openApiProperties;
    }

    /**
     * Copy information from one {@link OpenAPI} object to another.
     *
     * @param to The {@link OpenAPI} object to copy to
     * @param from The {@link OpenAPI} object to copy from
     */
    private void copyOpenAPI(OpenAPI to, OpenAPI from) {
        if (to != null && from != null) {
            Optional.ofNullable(from.getTags()).ifPresent(tags -> tags.forEach(to::addTagsItem));
            Optional.ofNullable(from.getServers()).ifPresent(servers -> servers.forEach(to::addServersItem));
            Optional.ofNullable(from.getSecurity()).ifPresent(securityRequirements -> securityRequirements.forEach(to::addSecurityItem));
            Optional.ofNullable(from.getPaths()).ifPresent(paths -> paths.forEach(to::path));
            Optional.ofNullable(from.getComponents()).ifPresent(components -> {
                Map<String, Schema> schemas = components.getSchemas();

                if (schemas != null && !schemas.isEmpty()) {
                    schemas.forEach((k, v) -> {
                        if (v.getName() == null) {
                            v.setName(k);
                        }
                    });
                    schemas.forEach(to::schema);
                }
                Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
                if (securitySchemes != null && !securitySchemes.isEmpty()) {
                    securitySchemes.forEach(to::schemaRequirement);
                }
            });
            Optional.ofNullable(from.getExternalDocs()).ifPresent(to::externalDocs);
            Optional.ofNullable(from.getExtensions()).ifPresent(extensions -> extensions.forEach(to::addExtension));
        }
    }

    private OpenAPI readOpenAPI(ClassElement element, VisitorContext context) {
        return element.findAnnotation(OpenAPIDefinition.class).flatMap(o -> {
                    Optional<OpenAPI> result = toValue(o.getValues(), context, OpenAPI.class);
                    result.ifPresent(openAPI -> {
                        List<io.swagger.v3.oas.models.security.SecurityRequirement> securityRequirements =
                                o.getAnnotations("security", SecurityRequirement.class)
                                .stream()
                                .map(ConvertUtils::mapToSecurityRequirement)
                                .collect(Collectors.toList());
                        openAPI.setSecurity(securityRequirements);
                    });
                    return result;
                }).orElse(new OpenAPI());
    }

    private void renderViews(String title, String specFile, Path destinationDir, VisitorContext context) throws IOException {
        String viewSpecification = OpenApiApplicationVisitor.getConfigurationProperty(MICRONAUT_OPENAPI_VIEWS_SPEC, context);
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(viewSpecification, readOpenApiConfigFile(context));
        if (cfg.isEnabled()) {
            cfg.setTitle(title);
            cfg.setSpecFile(specFile);
            cfg.setServerContextPath(getConfigurationProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, context));
            cfg.render(destinationDir.resolve("views"), context);
        }
    }

    private static PropertyNamingStrategies.NamingBase fromName(String name) {
        if (name == null) {
            return null;
        }
        switch (name.toUpperCase(Locale.US)) {
            case "LOWER_CAMEL_CASE":  return new LowerCamelCasePropertyNamingStrategy();
            case "UPPER_CAMEL_CASE":  return (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.UPPER_CAMEL_CASE;
            case "SNAKE_CASE": return (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.SNAKE_CASE;
            case "UPPER_SNAKE_CASE": return (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.UPPER_SNAKE_CASE;
            case "LOWER_CASE":  return (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.LOWER_CASE;
            case "KEBAB_CASE":  return (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.KEBAB_CASE;
            case "LOWER_DOT_CASE":  return (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.LOWER_DOT_CASE;
            default: return  null;
        }
    }

    private Optional<Path> openApiSpecFile(String fileName, VisitorContext visitorContext) {
        Optional<Path> path = userDefinedSpecFile(visitorContext);
        if (path.isPresent()) {
            return path;
        }
        // default location
        Optional<GeneratedFile> generatedFile = visitorContext.visitMetaInfFile("swagger/" + fileName, Element.EMPTY_ELEMENT_ARRAY);
        if (generatedFile.isPresent()) {
            URI uri = generatedFile.get().toURI();
            // happens in tests 'mem:///CLASS_OUTPUT/META-INF/swagger/swagger.yml'
            if (uri.getScheme() != null && !uri.getScheme().equals("mem")) {
                Path specPath = Paths.get(uri);
                createDirectories(specPath, visitorContext);
                return Optional.of(specPath);
            }
        }
        visitorContext.warn("Unable to get swagger/" + fileName + " file.", null);
        return Optional.empty();
    }

    private Optional<Path> userDefinedSpecFile(VisitorContext visitorContext) {
        String targetFile = getConfigurationProperty(MICRONAUT_OPENAPI_TARGET_FILE, visitorContext);
        if (StringUtils.isEmpty(targetFile)) {
            return Optional.empty();
        }
        Path specFile = resolve(visitorContext, Paths.get(targetFile));
        createDirectories(specFile, visitorContext);
        return Optional.of(specFile);
    }

    private static void createDirectories(Path f, VisitorContext visitorContext) {
        if (f.getParent() != null) {
            try {
                Files.createDirectories(f.getParent());
            } catch (IOException e) {
                visitorContext.warn("Fail to create directories for" + f + ": " + e.getMessage(), null);
            }
        }
    }

    private void applyPropertyNamingStrategy(OpenAPI openAPI, VisitorContext visitorContext) {
        final String namingStrategyName = getConfigurationProperty(MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, visitorContext);
        final PropertyNamingStrategies.NamingBase propertyNamingStrategy = fromName(namingStrategyName);
        if (propertyNamingStrategy != null) {
            visitorContext.info("Using " + namingStrategyName + " property naming strategy.");
            if (openAPI.getComponents() != null && CollectionUtils.isNotEmpty(openAPI.getComponents().getSchemas())) {
                openAPI.getComponents().getSchemas().values().forEach(model -> {
                    Map<String, Schema> properties = model.getProperties();
                    if (properties != null) {
                        Map<String, Schema> newProperties = properties.entrySet().stream()
                            .collect(Collectors.toMap(entry -> propertyNamingStrategy.translate(entry.getKey()),
                                Map.Entry::getValue, (prop1, prop2) -> prop1, LinkedHashMap::new));
                        model.getProperties().clear();
                        model.setProperties(newProperties);
                    }
                    List<String> required = model.getRequired();
                    if (required != null) {
                        List<String> updatedRequired = required.stream().map(propertyNamingStrategy::translate).collect(Collectors.toList());
                        required.clear();
                        required.addAll(updatedRequired);
                    }
                });
            }
        }
    }

    private void applyPropertyServerContextPath(OpenAPI openAPI, VisitorContext visitorContext) {
        final String serverContextPath = getConfigurationProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, visitorContext);
        if (serverContextPath == null || serverContextPath.isEmpty()) {
            return;
        }
        visitorContext.info("Applying server context path: " + serverContextPath + " to Paths.");
        io.swagger.v3.oas.models.Paths paths = openAPI.getPaths();
        if (paths == null || paths.isEmpty()) {
            return;
        }
        final io.swagger.v3.oas.models.Paths newPaths = new io.swagger.v3.oas.models.Paths();
        for (Map.Entry<String, PathItem> path : paths.entrySet()) {
            final String mapping = path.getKey();
            String newPath = mapping.startsWith(serverContextPath) ? mapping : StringUtils.prependUri(serverContextPath, mapping);
            if (!newPath.startsWith("/") && !newPath.startsWith("$")) {
                newPath = "/" + newPath;
            }
            newPaths.addPathItem(newPath, path.getValue());
        }
        openAPI.setPaths(newPaths);
    }

    public static JsonNode resolvePlaceholders(ArrayNode anode, UnaryOperator<String> propertyExpander) {
        for (int i = 0; i < anode.size(); ++i) {
            anode.set(i, resolvePlaceholders(anode.get(i), propertyExpander));
        }
        return anode;
    }

    public static JsonNode resolvePlaceholders(ObjectNode onode, UnaryOperator<String> propertyExpander) {
        if (onode.isEmpty()) {
            return onode;
        }
        final ObjectNode newNode = onode.objectNode();
        for (Iterator<Map.Entry<String, JsonNode>> i = onode.fields(); i.hasNext(); ) {
            final Map.Entry<String, JsonNode> entry = i.next();
            newNode.set(propertyExpander.apply(entry.getKey()), resolvePlaceholders(entry.getValue(), propertyExpander));
        }
        return newNode;
    }

    public static JsonNode resolvePlaceholders(JsonNode node, UnaryOperator<String> propertyExpander) {
        if (node.isTextual()) {
            final String text = node.textValue();
            if (text == null || text.trim().isEmpty()) {
                return node;
            }
            final String newText = propertyExpander.apply(text);
            return text.equals(newText) ? node : TextNode.valueOf(newText);
        } else if (node.isArray()) {
            return resolvePlaceholders((ArrayNode) node, propertyExpander);
        } else if (node.isObject()) {
            return resolvePlaceholders((ObjectNode) node, propertyExpander);
        } else {
            return node;
        }
    }

    public static String expandProperties(String s, List<Map.Entry<String, String>> properties, VisitorContext context) {
        if (StringUtils.isEmpty(s)) {
            return s;
        }
        if (CollectionUtils.isNotEmpty(properties)) {
            for (Map.Entry<String, String> entry : properties) {
                s = s.replace(entry.getKey(), entry.getValue());
            }
        }
        Environment environment = getEnv(context);
        return environment != null ? environment.getPlaceholderResolver().resolvePlaceholders(s).orElse(s) : s;
    }

    public static List<Map.Entry<String, String>> getExpandableProperties(VisitorContext context) {
        List<Map.Entry<String, String>> expandableProperties;
        Optional<Boolean> propertiesLoaded = context.get(MICRONAUT_INTERNAL_EXPANDBLE_PROPERTIES_LOADED, Boolean.class);
        if (!propertiesLoaded.orElse(false)) {

            expandableProperties = readOpenApiConfigFile(context).entrySet()
                .stream()
                .filter(entry -> entry.getKey().toString().startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX))
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>("${" + entry.getKey().toString().substring(MICRONAUT_OPENAPI_EXPAND_PREFIX.length()) + '}', entry.getValue().toString()))
                .collect(Collectors.toList());

            context.put(MICRONAUT_INTERNAL_EXPANDBLE_PROPERTIES, expandableProperties);
            context.put(MICRONAUT_INTERNAL_EXPANDBLE_PROPERTIES_LOADED, true);
        } else {
            expandableProperties = context.get(MICRONAUT_INTERNAL_EXPANDBLE_PROPERTIES, EXPANDABLE_PROPERTIES_ARGUMENT).orElse(null);
        }

        return expandableProperties;
    }

    private static OpenAPI resolvePropertyPlaceHolders(OpenAPI openAPI, VisitorContext visitorContext) {
        List<Map.Entry<String, String>> expandableProperties = getExpandableProperties(visitorContext);
        if (CollectionUtils.isNotEmpty(expandableProperties)) {
            visitorContext.info("Expanding properties: " + expandableProperties);
        }
        JsonNode root = resolvePlaceholders(ConvertUtils.getYamlMapper().convertValue(openAPI, ObjectNode.class), s -> expandProperties(s, expandableProperties, visitorContext));
        return ConvertUtils.getYamlMapper().convertValue(root, OpenAPI.class);
    }

    @Override
    public void finish(VisitorContext visitorContext) {
        if (visitedElements == visitedElements(visitorContext)) {
            // nothing new visited, avoid rewriting the files.
            return;
        }
        Optional<OpenAPI> attr = visitorContext.get(Utils.ATTR_OPENAPI, OpenAPI.class);
        if (!attr.isPresent()) {
            return;
        }
        OpenAPI openAPI = attr.get();
        processEndpoints(visitorContext);
        applyPropertyNamingStrategy(openAPI, visitorContext);
        applyPropertyServerContextPath(openAPI, visitorContext);
        openAPI = resolvePropertyPlaceHolders(openAPI, visitorContext);
        sortOpenAPI(openAPI);
        // Process after sorting so order is stable
        new JacksonDiscriminatorPostProcessor().addMissingDiscriminatorType(openAPI);
        new OpenApiOperationsPostProcessor().processOperations(openAPI);
        // need to replace openAPI after property placeholders resolved
        if (Utils.isTestMode()) {
            Utils.setTestReferenceAfterPlaceholders(openAPI);
        }

        // remove unused schemas
        try {
            if (openAPI.getComponents() != null) {
                Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
                if (CollectionUtils.isNotEmpty(schemas)) {
                    String openApiJson = ConvertUtils.getJsonMapper().writeValueAsString(openAPI);
                    for (String schemaName : schemas.keySet()) {
                        if (!openApiJson.contains(COMPONENTS_SCHEMAS_REF + schemaName)) {
                            schemas.remove(schemaName);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // do nothing
        }

        removeEmtpyComponents(openAPI);

        String isJson = getConfigurationProperty(MICRONAUT_OPENAPI_JSON_FORMAT, visitorContext);
        boolean isYaml = !(StringUtils.isNotEmpty(isJson) && isJson.equals(StringUtils.TRUE));

        String ext = isYaml ? ".yml" : ".json";
        String fileName = "swagger" + ext;
        String documentTitle = "OpenAPI";

        Info info = openAPI.getInfo();
        if (info != null) {
            documentTitle = Optional.ofNullable(info.getTitle()).orElse(Environment.DEFAULT_NAME);
            documentTitle = documentTitle.toLowerCase(Locale.US).replace(' ', '-');
            String version = info.getVersion();
            if (version != null) {
                documentTitle = documentTitle + '-' + version;
            }
            fileName = documentTitle + ext;
        }
        writeYamlToFile(openAPI, fileName, documentTitle, visitorContext, isYaml);
        visitedElements = visitedElements(visitorContext);
    }

    private void removeEmtpyComponents(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }
        if (CollectionUtils.isEmpty(components.getSchemas())) {
            components.setSchemas(null);
        }
        if (CollectionUtils.isEmpty(components.getResponses())) {
            components.setResponses(null);
        }
        if (CollectionUtils.isEmpty(components.getParameters())) {
            components.setParameters(null);
        }
        if (CollectionUtils.isEmpty(components.getExamples())) {
            components.setExamples(null);
        }
        if (CollectionUtils.isEmpty(components.getRequestBodies())) {
            components.setRequestBodies(null);
        }
        if (CollectionUtils.isEmpty(components.getHeaders())) {
            components.setHeaders(null);
        }
        if (CollectionUtils.isEmpty(components.getSecuritySchemes())) {
            components.setSecuritySchemes(null);
        }
        if (CollectionUtils.isEmpty(components.getLinks())) {
            components.setLinks(null);
        }
        if (CollectionUtils.isEmpty(components.getCallbacks())) {
            components.setCallbacks(null);
        }
        if (CollectionUtils.isEmpty(components.getExtensions())) {
            components.setExtensions(null);
        }

        if (CollectionUtils.isEmpty(components.getSchemas())
            && CollectionUtils.isEmpty(components.getResponses())
            && CollectionUtils.isEmpty(components.getParameters())
            && CollectionUtils.isEmpty(components.getExamples())
            && CollectionUtils.isEmpty(components.getRequestBodies())
            && CollectionUtils.isEmpty(components.getHeaders())
            && CollectionUtils.isEmpty(components.getSecuritySchemes())
            && CollectionUtils.isEmpty(components.getLinks())
            && CollectionUtils.isEmpty(components.getCallbacks())
            && CollectionUtils.isEmpty(components.getExtensions())
        ) {
            openAPI.setComponents(null);
        }
    }

    private void sortOpenAPI(OpenAPI openAPI) {
        // Sort paths
        if (openAPI.getPaths() != null) {
            io.swagger.v3.oas.models.Paths sortedPaths = new io.swagger.v3.oas.models.Paths();
            new TreeMap<>(openAPI.getPaths()).forEach(sortedPaths::addPathItem);
            if (openAPI.getPaths().getExtensions() != null) {
                sortedPaths.setExtensions(new TreeMap<>(openAPI.getPaths().getExtensions()));
            }
            openAPI.setPaths(sortedPaths);
        }

        // Sort all reusable Components
        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }

        sortComponent(components, Components::getSchemas, Components::setSchemas);
        sortComponent(components, Components::getResponses, Components::setResponses);
        sortComponent(components, Components::getParameters, Components::setParameters);
        sortComponent(components, Components::getExamples, Components::setExamples);
        sortComponent(components, Components::getRequestBodies, Components::setRequestBodies);
        sortComponent(components, Components::getHeaders, Components::setHeaders);
        sortComponent(components, Components::getSecuritySchemes, Components::setSecuritySchemes);
        sortComponent(components, Components::getLinks, Components::setLinks);
        sortComponent(components, Components::getCallbacks, Components::setCallbacks);
    }

    private <T> void sortComponent(Components components, Function<Components, Map<String, T>> getter, BiConsumer<Components, Map<String, T>> setter) {
        if (components != null && getter.apply(components) != null) {
            Map<String, T> component = getter.apply(components);
            setter.accept(components, new TreeMap<>(component));
        }
    }

    private void writeYamlToFile(OpenAPI openAPI, String fileName, String documentTitle, VisitorContext visitorContext, boolean isYaml) {
        Optional<Path> specFile = openApiSpecFile(fileName, visitorContext);
        try (Writer writer = getFileWriter(specFile)) {
            (isYaml ? ConvertUtils.getYamlMapper() : ConvertUtils.getJsonMapper()).writeValue(writer, openAPI);
            if (Utils.isTestMode()) {
                if (isYaml) {
                    Utils.setTestYamlReference(writer.toString());
                } else {
                    Utils.setTestJsonReference(writer.toString());
                }
            } else {
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                Path specPath = specFile.get();
                visitorContext.info("Writing OpenAPI file to destination: " + specPath);
                visitorContext.getClassesOutputPath().ifPresent(path -> {
                    // add relative paths for the specPath, and its parent META-INF/swagger
                    // so that micronaut-graal visitor knows about them
                    visitorContext.addGeneratedResource(path.relativize(specPath).toString());
                    visitorContext.addGeneratedResource(path.relativize(specPath.getParent()).toString());
                });
                renderViews(documentTitle, specPath.getFileName().toString(), specPath.getParent(), visitorContext);
            }
        } catch (Exception e) {
            visitorContext.warn("Unable to generate swagger." + (isYaml ? "yml" : "json") + ": " + specFile.orElse(null) + " - " + e.getMessage(), classElement);
        }
    }

    private Writer getFileWriter(Optional<Path> specFile) throws IOException {
        if (Utils.isTestMode()) {
            return new StringWriter();
        } else if (specFile.isPresent()) {
            return Files.newBufferedWriter(specFile.get(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } else {
            throw new IOException("Swagger spec file location is not present");
        }
    }

    private void processEndpoints(VisitorContext visitorContext) {
        EndpointsConfiguration endpointsCfg = endPointsConfiguration(visitorContext);
        if (endpointsCfg.isEnabled() && !endpointsCfg.getEndpoints().isEmpty()) {
            OpenApiEndpointVisitor visitor = new OpenApiEndpointVisitor(true);
            endpointsCfg.getEndpoints().values().stream()
            .filter(endpoint -> endpoint.getClassElement().isPresent())
            .forEach(endpoint -> {
                ClassElement element = endpoint.getClassElement().get();
                visitorContext.put(MICRONAUT_OPENAPI_ENDPOINT_CLASS_TAGS, endpoint.getTags());
                visitorContext.put(MICRONAUT_OPENAPI_ENDPOINT_SERVERS, endpoint.getServers());
                visitorContext.put(MICRONAUT_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, endpoint.getSecurityRequirements());
                visitor.visitClass(element, visitorContext);
                element.getEnclosedElements(ElementQuery.ALL_METHODS
                                .modifiers(mods -> !mods.contains(ElementModifier.STATIC) && !mods.contains(ElementModifier.PRIVATE))
                                .named(name -> !name.contains("$"))
                        )
                        .forEach(method -> visitor.visitMethod(method, visitorContext));
            });
        }
    }

    static class LowerCamelCasePropertyNamingStrategy extends PropertyNamingStrategies.NamingBase {

        private static final long serialVersionUID = -2750503285679998670L;

        @Override
        public String translate(String propertyName) {
            return propertyName;
        }

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
}
