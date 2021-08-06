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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micronaut.annotation.processing.visitor.JavaClassElementExt;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.openapi.postprocessors.JacksonDiscriminatorPostProcessor;
import io.micronaut.openapi.postprocessors.OpenApiOperationsPostProcessor;
import io.micronaut.openapi.view.OpenApiViewConfig;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;

import javax.annotation.processing.SupportedOptions;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Visits the application class.
 *
 * @author graemerocher
 * @since 1.0
 */
@Experimental
@SupportedOptions({
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY,
    OpenApiApplicationVisitor.MICRONAUT_OPENAPI_VIEWS_SPEC,
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
     * System property that specifies the location of additional swagger YAML files to read from.
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

    private static final String MICRONAUT_OPENAPI_PROPERTIES = "micronaut.openapi.properties";
    private static final String MICRONAUT_OPENAPI_ENDPOINTS = "micronaut.openapi.endpoints";

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

        Optional<OpenAPI> attr = context.get(ATTR_OPENAPI, OpenAPI.class);
        if (attr.isPresent()) {
            OpenAPI existing = attr.get();
            Optional.ofNullable(openAPI.getInfo())
                    .ifPresent(existing::setInfo);
            copyOpenAPI(existing, openAPI);
        } else {
            context.put(ATTR_OPENAPI, openAPI);
        }

        if (isTestMode()) {
            resolveOpenAPI(context);
        }

        this.classElement = element;
    }

    private String getConfigurationProperty(String key, VisitorContext context) {
        return System.getProperty(key, readOpenApiConfigFile(context).getProperty(key));
    }

    /**
     * Merge the OpenAPI YAML files into one single file.
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
                context.info("Merging Swagger OpenAPI YAML files from location: " + directory);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> path.toString().endsWith(".yml"))) {
                    stream.forEach(path -> {
                        context.info("Reading Swagger OpenAPI YAML file " + path.getFileName());
                        OpenAPI parsedOpenApi = null;
                        try {
                            parsedOpenApi = yamlMapper.readValue(path.toFile(), OpenAPI.class);
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
        if (!path.isAbsolute()) {
            Optional<Path> projectDir = context.getProjectDir();
            if (projectDir.isPresent()) {
                path = projectDir.get().resolve(path);
            }
        }
        return path.toAbsolutePath();
    }

    /**
     * Returns the EndpointsConfiguration.
     * @param context The context.
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

    private static Properties readOpenApiConfigFile(VisitorContext context) {
        Optional<Properties> props = context.get(MICRONAUT_OPENAPI_PROPERTIES, Properties.class);
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
                    context.warn("Fail to read OpenAPI configuration file: " + e.getMessage(), null);
                }
            } else if (Files.exists(cfg)) {
                context.warn("Can not read configuration file: " + cfg, null);
            }
        }
        context.put(MICRONAUT_OPENAPI_PROPERTIES, openApiProperties);
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
                                o.getAnnotations("security", io.swagger.v3.oas.annotations.security.SecurityRequirement.class)
                                .stream()
                                .map(this::mapToSecurityRequirement)
                                .collect(Collectors.toList());
                        openAPI.setSecurity(securityRequirements);
                    });
                    return result;
                }).orElse(new OpenAPI());
    }

    private void renderViews(String title, String specFile, Path destinationDir, VisitorContext visitorContext) throws IOException {
        String viewSpecification = System.getProperty(MICRONAUT_OPENAPI_VIEWS_SPEC);
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(viewSpecification, readOpenApiConfigFile(visitorContext));
        if (cfg.isEnabled()) {
            cfg.setTitle(title);
            cfg.setSpecFile(specFile);
            cfg.setServerContextPath(getConfigurationProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, visitorContext));
            cfg.render(destinationDir.resolve("views"), visitorContext);
        }
    }

    private static PropertyNamingStrategyBase fromName(String name) {
        if (name == null) {
            return null;
        }
        switch (name.toUpperCase(Locale.US)) {
        case "SNAKE_CASE": return (PropertyNamingStrategyBase) PropertyNamingStrategy.SNAKE_CASE;
        case "UPPER_CAMEL_CASE":  return (PropertyNamingStrategyBase) PropertyNamingStrategy.UPPER_CAMEL_CASE;
        case "LOWER_CAMEL_CASE":  return new LowerCamelCasePropertyNamingStrategy();
        case "LOWER_CASE":  return (PropertyNamingStrategyBase) PropertyNamingStrategy.LOWER_CASE;
        case "KEBAB_CASE":  return (PropertyNamingStrategyBase) PropertyNamingStrategy.KEBAB_CASE;
        default: return  null;
        }
    }

    private Optional<Path> openApiSpecFile(String fileName, VisitorContext visitorContext) {
        Optional<Path> path = userDefinedSpecFile(visitorContext);
        if (path.isPresent()) {
            return path;
        }
        // default location
        Optional<GeneratedFile> generatedFile = visitorContext.visitMetaInfFile("swagger/" + fileName);
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
        final PropertyNamingStrategyBase propertyNamingStrategy = fromName(namingStrategyName);
        if (propertyNamingStrategy != null) {
            visitorContext.info("Using " + namingStrategyName + " property naming strategy.");
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
        for (Map.Entry<String, PathItem> path: paths.entrySet()) {
            final String mapping = path.getKey();
            newPaths.addPathItem(mapping.startsWith(serverContextPath) ? mapping : StringUtils.prependUri(serverContextPath, mapping), path.getValue());
        }
        openAPI.setPaths(newPaths);
    }

    private JsonNode resolvePlaceholders(ArrayNode anode, UnaryOperator<String> propertyExpander) {
        for (int i = 0 ; i < anode.size(); ++i) {
            anode.set(i, resolvePlaceholders(anode.get(i), propertyExpander));
        }
        return anode;
    }

    private JsonNode resolvePlaceholders(ObjectNode onode, UnaryOperator<String> propertyExpander) {
        if (onode.size() == 0) {
            return onode;
        }
        final ObjectNode newNode = onode.objectNode();
        for (Iterator<Map.Entry<String, JsonNode>> i = onode.fields(); i.hasNext();) {
            final Map.Entry<String, JsonNode> entry = i.next();
            newNode.set(propertyExpander.apply(entry.getKey()), resolvePlaceholders(entry.getValue(), propertyExpander));
        }
        return newNode;
    }

    private JsonNode resolvePlaceholders(JsonNode node, UnaryOperator<String> propertyExpander) {
        if  (node.isTextual()) {
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

    private String expandProperties(String s, List<Map.Entry<String, String>> properties) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        for (Map.Entry<String, String> entry: properties) {
            s = s.replace(entry.getKey(), entry.getValue());
        }
        return s;
    }

    private OpenAPI resolvePropertyPlaceHolders(OpenAPI openAPI, VisitorContext visitorContext) {
        List<Map.Entry<String, String>> expandableProperties = readOpenApiConfigFile(visitorContext).entrySet()
            .stream()
            .filter(entry -> entry.getKey().toString().startsWith(MICRONAUT_OPENAPI_EXPAND_PREFIX))
            .map(entry -> new AbstractMap.SimpleImmutableEntry<>("${" + entry.getKey().toString().substring(MICRONAUT_OPENAPI_EXPAND_PREFIX.length()) + '}', entry.getValue().toString())).collect(Collectors.toList());
        if (expandableProperties.isEmpty()) {
            return openAPI;
        }
        visitorContext.info("Expanding properties: " + expandableProperties);
        JsonNode root = resolvePlaceholders(Yaml.mapper().convertValue(openAPI, ObjectNode.class), s -> expandProperties(s, expandableProperties));
        return Yaml.mapper().convertValue(root, OpenAPI.class);
    }

    @Override
    public void finish(VisitorContext visitorContext) {
        if (visitedElements == visitedElements(visitorContext)) {
            // nothing new visited, avoid rewriting the files.
            return;
        }
        Optional<OpenAPI> attr = visitorContext.get(ATTR_OPENAPI, OpenAPI.class);
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
        String fileName = "swagger.yml";
        String documentTitle = "OpenAPI";

        Info info = openAPI.getInfo();
        if (info != null) {
            documentTitle = Optional.ofNullable(info.getTitle()).orElse(Environment.DEFAULT_NAME);
            documentTitle = documentTitle.toLowerCase(Locale.US).replace(' ', '-');
            String version = info.getVersion();
            if (version != null) {
                documentTitle = documentTitle + '-' + version;
            }
            fileName = documentTitle + ".yml";
        }
        writeYamlToFile(openAPI, fileName, documentTitle, visitorContext);
        visitedElements = visitedElements(visitorContext);
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

    private void writeYamlToFile(OpenAPI openAPI, String fileName, String documentTitle, VisitorContext visitorContext) {
        Optional<Path> specFile = openApiSpecFile(fileName, visitorContext);
        try (Writer writer = getYamlWriter(specFile)) {
            yamlMapper.writeValue(writer, openAPI);
            if (isTestMode()) {
                AbstractOpenApiVisitor.testYamlReference = writer.toString();
            } else {
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                Path specPath = specFile.get();
                visitorContext.info("Writing OpenAPI YAML to destination: " + specPath);
                visitorContext.getClassesOutputPath().ifPresent(path -> {
                    // add relative paths for the specPath, and its parent META-INF/swagger
                    // so that micronaut-graal visitor knows about them
                    visitorContext.addGeneratedResource(path.relativize(specPath).toString());
                    visitorContext.addGeneratedResource(path.relativize(specPath.getParent()).toString());
                });
                renderViews(documentTitle, specPath.getFileName().toString(), specPath.getParent(), visitorContext);
            }
        } catch (Exception e) {
            visitorContext.warn("Unable to generate swagger.yml: " + specFile.orElse(null) + " - " + e.getMessage(), classElement);
        }
    }

    private Writer getYamlWriter(Optional<Path> specFile) throws IOException {
        if (isTestMode()) {
            return new StringWriter();
        } else if (specFile.isPresent()) {
            return Files.newBufferedWriter(specFile.get(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } else {
            throw new IOException("Swagger spec file location is not present");
        }
    }

    private void processEndpoints(VisitorContext visitorContext) {
        EndpointsConfiguration endpointsCfg = endPointsConfiguration(visitorContext);
        if ("io.micronaut.annotation.processing.visitor.JavaVisitorContext".equals(visitorContext.getClass().getName())
                && endpointsCfg.isEnabled()
                && !endpointsCfg.getEndpoints().isEmpty()) {
            OpenApiEndpointVisitor visitor = new OpenApiEndpointVisitor(true);
            endpointsCfg.getEndpoints().values().stream()
            .filter(endpoint -> endpoint.getClassElement().isPresent()
                    && isJavaElement(endpoint.getClassElement().get(), visitorContext))
            .forEach(endpoint -> {
                ClassElement element = endpoint.getClassElement().get();
                visitorContext.put(MICRONAUT_OPENAPI_ENDPOINT_CLASS_TAGS, endpoint.getTags());
                visitorContext.put(MICRONAUT_OPENAPI_ENDPOINT_SERVERS, endpoint.getServers());
                visitorContext.put(MICRONAUT_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, endpoint.getSecurityRequirements());
                visitor.visitClass(element, visitorContext);
                JavaClassElementExt javaClassElement = new JavaClassElementExt(element, visitorContext);
                javaClassElement.getCandidateMethods().forEach(method -> visitor.visitMethod(method, visitorContext));
            });
        }
    }

    static class LowerCamelCasePropertyNamingStrategy extends PropertyNamingStrategyBase {
        private static final long serialVersionUID = -2750503285679998670L;

        @Override
        public String translate(String propertyName) {
            return propertyName;
        }

    }

}
