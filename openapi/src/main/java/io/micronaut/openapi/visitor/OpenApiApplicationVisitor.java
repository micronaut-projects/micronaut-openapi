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
import java.io.Serial;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.DefaultMutableConversionService;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.GenericArgument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.openapi.postprocessors.JacksonDiscriminatorPostProcessor;
import io.micronaut.openapi.postprocessors.OpenApiOperationsPostProcessor;
import io.micronaut.openapi.view.OpenApiViewConfig;
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static io.micronaut.openapi.visitor.ConfigProperty.ALL;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_FILENAME;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_JSON_FORMAT;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_TARGET_FILE;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_VIEWS_DEST_DIR;
import static io.micronaut.openapi.visitor.ConfigProperty.MICRONAUT_OPENAPI_VIEWS_SPEC;
import static io.micronaut.openapi.visitor.ConfigUtils.endpointsConfiguration;
import static io.micronaut.openapi.visitor.ConfigUtils.getConfigProperty;
import static io.micronaut.openapi.visitor.ConfigUtils.getEnv;
import static io.micronaut.openapi.visitor.ConfigUtils.getExpandableProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getGroupProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.readOpenApiConfigFile;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS;
import static io.micronaut.openapi.visitor.FileUtils.EXT_JSON;
import static io.micronaut.openapi.visitor.FileUtils.EXT_YML;
import static io.micronaut.openapi.visitor.FileUtils.createDirectories;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.SchemaUtils.EMPTY_SIMPLE_SCHEMA;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.getOperationOnPathItem;
import static io.micronaut.openapi.visitor.SchemaUtils.setOperationOnPathItem;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Visits the application class.
 *
 * @author graemerocher
 * @since 1.0
 */
public class OpenApiApplicationVisitor extends AbstractOpenApiVisitor implements TypeElementVisitor<OpenAPIDefinition, Object> {

    private ClassElement classElement;
    private int visitedElements = -1;

    @Override
    public Set<String> getSupportedOptions() {
        return ALL;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        try {
            incrementVisitedElements(context);
            if (!isOpenApiEnabled(context)) {
                return;
            }
            context.info("Generating OpenAPI Documentation");
            OpenAPI openApi = readOpenApi(element, context);

            // Handle Application securityRequirements schemes
            processSecuritySchemes(element, context);

            mergeAdditionalSwaggerFiles(element, context, openApi);
            // handle type level tags
            List<io.swagger.v3.oas.models.tags.Tag> tagList = processOpenApiAnnotation(
                    element,
                    context,
                    Tag.class,
                    io.swagger.v3.oas.models.tags.Tag.class,
                    openApi.getTags()
            );
            openApi.setTags(tagList);

            // handle type level security requirements
            List<io.swagger.v3.oas.models.security.SecurityRequirement> securityRequirements = readSecurityRequirements(element);
            if (openApi.getSecurity() != null) {
                securityRequirements.addAll(openApi.getSecurity());
            }

            openApi.setSecurity(securityRequirements);

            // handle type level servers
            List<io.swagger.v3.oas.models.servers.Server> servers = processOpenApiAnnotation(
                    element,
                    context,
                    Server.class,
                    io.swagger.v3.oas.models.servers.Server.class,
                    openApi.getServers()
            );
            openApi.setServers(servers);

            Optional<OpenAPI> attr = context.get(Utils.ATTR_OPENAPI, OpenAPI.class);
            if (attr.isPresent()) {
                OpenAPI existing = attr.get();
                Optional.ofNullable(openApi.getInfo())
                        .ifPresent(existing::setInfo);
                copyOpenApi(existing, openApi);
            } else {
                context.put(Utils.ATTR_OPENAPI, openApi);
            }

            if (Utils.isTestMode()) {
                Utils.resolveOpenApi(context);
            }

            classElement = element;
        } catch (Throwable t) {
            context.warn("Error with processing class:\n" + Utils.printStackTrace(t), classElement);
        }
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
        String additionalSwaggerFiles = getConfigProperty(MICRONAUT_OPENAPI_ADDITIONAL_FILES, context);
        if (StringUtils.isNotEmpty(additionalSwaggerFiles)) {
            Path directory = resolve(context, Paths.get(additionalSwaggerFiles));
            if (Files.isDirectory(directory)) {
                context.info("Merging Swagger OpenAPI YAML and JSON files from location: " + directory);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> FileUtils.isYaml(path.toString().toLowerCase()) || path.toString().toLowerCase().endsWith(EXT_JSON))) {
                    stream.forEach(path -> {
                        boolean isYaml = FileUtils.isYaml(path.toString().toLowerCase());
                        context.info("Reading Swagger OpenAPI " + (isYaml ? "YAML" : "JSON") + " file " + path.getFileName());
                        OpenAPI parsedOpenApi = null;
                        try {
                            parsedOpenApi = (isYaml ? ConvertUtils.getYamlMapper() : ConvertUtils.getJsonMapper()).readValue(path.toFile(), OpenAPI.class);
                        } catch (IOException e) {
                            context.warn("Unable to read file " + path.getFileName() + ": " + e.getMessage(), element);
                        }
                        copyOpenApi(openAPI, parsedOpenApi);
                    });
                } catch (IOException e) {
                    context.warn("Unable to read  file from " + directory + ": " + e.getMessage(), element);
                }
            } else {
                context.warn(directory + " does not exist or is not a directory", element);
            }
        }
    }

    /**
     * Copy information from one {@link OpenAPI} object to another.
     *
     * @param to The {@link OpenAPI} object to copy to
     * @param from The {@link OpenAPI} object to copy from
     */
    private void copyOpenApi(OpenAPI to, OpenAPI from) {
        if (to != null && from != null) {
            Optional.ofNullable(from.getTags()).ifPresent(tags -> tags.forEach(to::addTagsItem));
            Optional.ofNullable(from.getServers()).ifPresent(servers -> servers.forEach(to::addServersItem));
            Optional.ofNullable(from.getSecurity()).ifPresent(securityRequirements -> securityRequirements.forEach(to::addSecurityItem));
            Optional.ofNullable(from.getPaths()).ifPresent(paths -> paths.forEach(to::path));
            Optional.ofNullable(from.getComponents()).ifPresent(components -> {
                Map<String, Schema> schemas = components.getSchemas();

                if (CollectionUtils.isNotEmpty(schemas)) {
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

    private OpenAPI readOpenApi(ClassElement element, VisitorContext context) {
        return element.findAnnotation(OpenAPIDefinition.class).flatMap(o -> {
            Optional<OpenAPI> result = toValue(o.getValues(), context, OpenAPI.class, null);
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

    private void renderViews(String title, Map<Pair<String, String>, OpenApiInfo> openApiInfos, Path destinationDir, VisitorContext context) throws IOException {
        String viewSpecification = getConfigProperty(MICRONAUT_OPENAPI_VIEWS_SPEC, context);
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(viewSpecification, openApiInfos, readOpenApiConfigFile(context), context);
        if (cfg.isEnabled()) {
            cfg.setTitle(title);
            cfg.setSpecFile(openApiInfos.values().iterator().next().getSpecFilePath());
            cfg.setServerContextPath(getConfigProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, context));
            cfg.render(destinationDir, context);
        }
    }

    private static PropertyNamingStrategies.NamingBase fromName(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.toUpperCase(Locale.US)) {
            case "LOWER_CAMEL_CASE" -> new LowerCamelCasePropertyNamingStrategy();
            case "UPPER_CAMEL_CASE" -> (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.UPPER_CAMEL_CASE;
            case "SNAKE_CASE" -> (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.SNAKE_CASE;
            case "UPPER_SNAKE_CASE" -> (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.UPPER_SNAKE_CASE;
            case "LOWER_CASE" -> (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.LOWER_CASE;
            case "KEBAB_CASE" -> (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.KEBAB_CASE;
            case "LOWER_DOT_CASE" -> (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.LOWER_DOT_CASE;
            default -> null;
        };
    }

    private Optional<Path> getDefaultFilePath(String fileName, VisitorContext context) {
        // default location
        Optional<GeneratedFile> generatedFile = context.visitMetaInfFile("swagger/" + fileName, Element.EMPTY_ELEMENT_ARRAY);
        if (generatedFile.isPresent()) {
            URI uri = generatedFile.get().toURI();
            // happens in tests 'mem:///CLASS_OUTPUT/META-INF/swagger/swagger.yml'
            if (uri.getScheme() != null && !uri.getScheme().equals("mem")) {
                Path specPath = Paths.get(uri);
                createDirectories(specPath, context);
                return Optional.of(specPath);
            }
        }
        context.warn("Unable to get swagger/" + fileName + " file.", null);
        return Optional.empty();
    }

    private Optional<Path> openApiSpecFile(String fileName, VisitorContext context) {
        Optional<Path> path = userDefinedSpecFile(context);
        if (path.isPresent()) {
            return path;
        }
        return getDefaultFilePath(fileName, context);
    }

    private Optional<Path> userDefinedSpecFile(VisitorContext context) {
        String targetFile = getConfigProperty(MICRONAUT_OPENAPI_TARGET_FILE, context);
        if (StringUtils.isEmpty(targetFile)) {
            return Optional.empty();
        }
        Path specFile = resolve(context, Paths.get(targetFile));
        createDirectories(specFile, context);
        return Optional.of(specFile);
    }

    private Path getViewsDestDir(Path defaultSwaggerFilePath, VisitorContext context) {
        String destDir = getConfigProperty(MICRONAUT_OPENAPI_VIEWS_DEST_DIR, context);
        if (StringUtils.isNotEmpty(destDir)) {
            Path destPath = resolve(context, Paths.get(destDir));
            createDirectories(destPath, context);
            return destPath;
        }
        return defaultSwaggerFilePath.getParent().resolve("views");
    }

    private void applyPropertyNamingStrategy(OpenAPI openAPI, VisitorContext context) {
        final String namingStrategyName = getConfigProperty(MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, context);
        final PropertyNamingStrategies.NamingBase propertyNamingStrategy = fromName(namingStrategyName);
        if (propertyNamingStrategy != null) {
            context.info("Using " + namingStrategyName + " property naming strategy.");
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
                        List<String> updatedRequired = required.stream().map(propertyNamingStrategy::translate).toList();
                        required.clear();
                        required.addAll(updatedRequired);
                    }
                });
            }
        }
    }

    private void applyPropertyServerContextPath(OpenAPI openAPI, VisitorContext context) {
        final String serverContextPath = getConfigProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, context);
        if (serverContextPath == null || serverContextPath.isEmpty()) {
            return;
        }
        context.info("Applying server context path: " + serverContextPath + " to Paths.");
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
            if (text == null || text.isBlank()) {
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

    public static String expandProperties(String s, List<Pair<String, String>> properties, VisitorContext context) {
        if (StringUtils.isEmpty(s) || !s.contains(Utils.PLACEHOLDER_PREFIX)) {
            return s;
        }

        // form openapi file (expandable properties)
        if (CollectionUtils.isNotEmpty(properties)) {
            for (Pair<String, String> entry : properties) {
                s = s.replaceAll(entry.getFirst(), entry.getSecond());
            }
        }

        return replacePlaceholders(s, context);
    }

    public static String replacePlaceholders(String value, VisitorContext context) {
        if (StringUtils.isEmpty(value) || !value.contains(Utils.PLACEHOLDER_PREFIX)) {
            return value;
        }
        // system properties
        if (CollectionUtils.isNotEmpty(System.getProperties())) {
            for (Map.Entry<Object, Object> sysProp : System.getProperties().entrySet()) {
                value = value.replace(Utils.PLACEHOLDER_PREFIX + sysProp.getKey().toString() + Utils.PLACEHOLDER_POSTFIX, sysProp.getValue().toString());
            }
        }

        // form openapi file
        for (Map.Entry<Object, Object> fileProp : readOpenApiConfigFile(context).entrySet()) {
            value = value.replace(Utils.PLACEHOLDER_PREFIX + fileProp.getKey().toString() + Utils.PLACEHOLDER_POSTFIX, fileProp.getValue().toString());
        }

        // from environments
        Environment environment = getEnv(context);
        if (environment != null) {
            value = environment.getPlaceholderResolver().resolvePlaceholders(value).orElse(value);
        }

        return value;
    }

    private static OpenAPI resolvePropertyPlaceHolders(OpenAPI openAPI, VisitorContext context) {
        List<Pair<String, String>> expandableProperties = getExpandableProperties(context);
        if (CollectionUtils.isNotEmpty(expandableProperties)) {
            context.info("Expanding properties: " + expandableProperties);
        }
        JsonNode root = resolvePlaceholders(ConvertUtils.getYamlMapper().convertValue(openAPI, ObjectNode.class), s -> expandProperties(s, expandableProperties, context));
        return ConvertUtils.getYamlMapper().convertValue(root, OpenAPI.class);
    }

    @Override
    public void finish(VisitorContext context) {
        try {
            if (!isOpenApiEnabled(context)) {
                return;
            }
            if (visitedElements == visitedElements(context)) {
                // nothing new visited, avoid rewriting the files.
                return;
            }
            Optional<OpenAPI> attr = context.get(Utils.ATTR_OPENAPI, OpenAPI.class);
            if (attr.isEmpty()) {
                return;
            }
            OpenAPI openApi = attr.get();
            processEndpoints(context);

            mergeMicronautEndpointInfos(openApi, context);
            Map<Pair<String, String>, OpenApiInfo> openApiInfos = divideOpenapiByGroupsAndVersions(openApi, context);
            if (Utils.isTestMode()) {
                Utils.setTestReferences(openApiInfos);
            }

            String isJson = getConfigProperty(MICRONAUT_OPENAPI_JSON_FORMAT, context);
            boolean isYaml = !(StringUtils.isNotEmpty(isJson) && isJson.equalsIgnoreCase(StringUtils.TRUE));
            String documentTitle = "OpenAPI";

            for (Map.Entry<Pair<String, String>, OpenApiInfo> entry : openApiInfos.entrySet()) {

                OpenApiInfo openApiInfo = entry.getValue();

                openApi = openApiInfo.getOpenApi();

                openApi = postProcessOpenApi(openApi, context);
                openApiInfo.setOpenApi(openApi);
                // need to set test reference to openApi after post-processing
                if (Utils.isTestMode()) {
                    Utils.setTestReference(openApi);
                }

                String ext = isYaml ? EXT_YML : EXT_JSON;
                String fileName = "swagger" + ext;

                Info info = openApi.getInfo();
                if (info != null) {
                    documentTitle = Optional.ofNullable(info.getTitle()).orElse(Environment.DEFAULT_NAME);
                    documentTitle = documentTitle.toLowerCase(Locale.US).replace(' ', '-');
                    String version = info.getVersion();
                    if (version != null) {
                        documentTitle = documentTitle + '-' + version;
                    }
                    fileName = documentTitle + ext;
                }

                String versionFromInfo = info != null && info.getVersion() != null ? info.getVersion() : StringUtils.EMPTY_STRING;

                String fileNameFromConfig = getConfigProperty(MICRONAUT_OPENAPI_FILENAME, context);
                if (StringUtils.isNotEmpty(fileNameFromConfig)) {
                    fileName = replacePlaceholders(fileNameFromConfig, context) + ext;
                    if (fileName.contains("${version}")) {
                        fileName = fileName.replaceAll("\\$\\{version}", versionFromInfo);
                    }
                }

                // contruct filename for group
                if (openApiInfos.size() > 1) {
                    if (StringUtils.isNotEmpty(openApiInfo.getFilename())) {
                        fileName = openApiInfo.getFilename();
                    } else {

                        // default name: swagger-<version>-<groupName>-<apiVersion>

                        fileName = fileName.substring(0, fileName.length() - ext.length())
                                + (openApiInfo.getGroupName() != null ? "-" + openApiInfo.getGroupName() : StringUtils.EMPTY_STRING)
                                + (openApiInfo.getVersion() != null ? "-" + openApiInfo.getVersion() : StringUtils.EMPTY_STRING);
                    }

                    fileName = replacePlaceholders(fileName, context) + ext;
                    if (fileName.contains("${apiVersion}")) {
                        fileName = fileName.replaceAll("\\$\\{apiVersion}", openApiInfo.getVersion() != null ? openApiInfo.getVersion() : versionFromInfo);
                    }
                    if (fileName.contains("${version}")) {
                        fileName = fileName.replaceAll("\\$\\{version}", versionFromInfo);
                    }
                    if (fileName.contains("${group}")) {
                        fileName = fileName.replaceAll("\\$\\{group}", openApiInfo.getGroupName() != null ? openApiInfo.getGroupName() : StringUtils.EMPTY_STRING);
                    }
                }
                if (fileName.contains(Utils.PLACEHOLDER_PREFIX)) {
                    context.warn("Can't set some placeholders in fileName: " + fileName, null);
                }

                openApiInfo.setFilename(fileName);
            }

            writeYamlToFile(openApiInfos, documentTitle, context, isYaml);

            visitedElements = visitedElements(context);
        } catch (Throwable t) {
            context.warn("Error:\n" + Utils.printStackTrace(t), null);
            throw t;
        }
    }

    private Map<Pair<String, String>, OpenApiInfo> divideOpenapiByGroupsAndVersions(OpenAPI openApi, VisitorContext context) {
        Map<String, List<EndpointInfo>> endpointInfosMap = Utils.getEndpointInfos();
        Set<String> allVersions = Utils.getAllKnownVersions();
        Set<String> allGroups = Utils.getAllKnownGroups();
        if (CollectionUtils.isEmpty(endpointInfosMap)
                || (CollectionUtils.isEmpty(allVersions) && CollectionUtils.isEmpty(allGroups))) {
            return Collections.singletonMap(Pair.NULL_STRING_PAIR, new OpenApiInfo(openApi));
        }

        List<EndpointInfo> commonEndpoints = new ArrayList<>();

        // key version, groupName
        Map<Pair<String, String>, OpenApiInfo> result = new HashMap<>();

        for (List<EndpointInfo> endpointInfos : endpointInfosMap.values()) {
            for (EndpointInfo endpointInfo : endpointInfos) {
                if (CollectionUtils.isEmpty(endpointInfo.getGroups()) && endpointInfo.getVersion() == null) {
                    commonEndpoints.add(endpointInfo);
                    continue;
                }
                for (String group : endpointInfo.getGroups()) {
                    if (CollectionUtils.isNotEmpty(endpointInfo.getExcludedGroups()) && endpointInfo.getExcludedGroups().contains(group)) {
                        continue;
                    }
                    OpenAPI newOpenApi = addOpenApiInfo(group, endpointInfo.getVersion(), openApi, result, context);
                    addOperation(endpointInfo, newOpenApi);
                }

                // if we have only versions without groups
                if (CollectionUtils.isEmpty(endpointInfo.getGroups())) {
                    OpenAPI newOpenApi = addOpenApiInfo(null, endpointInfo.getVersion(), openApi, result, context);
                    addOperation(endpointInfo, newOpenApi);
                }
            }
        }

        // add common endpoints (without group name)
        for (Map.Entry<Pair<String, String>, OpenApiInfo> entry : result.entrySet()) {

            String group = entry.getKey().getFirst();
            GroupProperties groupProperties = getGroupProperties(group, context);
            if (groupProperties != null && groupProperties.getCommonExclude() != null && groupProperties.getCommonExclude()) {
                continue;
            }

            OpenAPI openAPI = entry.getValue().getOpenApi();

            for (EndpointInfo commonEndpoint : commonEndpoints) {
                if (CollectionUtils.isNotEmpty(commonEndpoint.getExcludedGroups()) && commonEndpoint.getExcludedGroups().contains(group)) {
                    continue;
                }
                addOperation(commonEndpoint, openAPI);
            }
        }

        return result;
    }

    private void addOperation(EndpointInfo endpointInfo, OpenAPI openApi) {
        if (openApi == null) {
            return;
        }
        io.swagger.v3.oas.models.Paths paths = openApi.getPaths();
        if (paths == null) {
            paths = new io.swagger.v3.oas.models.Paths();
            openApi.setPaths(paths);
        }
        PathItem pathItem = paths.computeIfAbsent(endpointInfo.getUrl(), (pathurl) -> new PathItem());
        Operation operation = getOperationOnPathItem(pathItem, endpointInfo.getHttpMethod());
        if (operation == null) {
            setOperationOnPathItem(pathItem, endpointInfo.getHttpMethod(), endpointInfo.getOperation());
            return;
        }
        setOperationOnPathItem(pathItem, endpointInfo.getHttpMethod(), SchemaUtils.mergeOperations(operation, endpointInfo.getOperation()));
    }

    private OpenAPI addOpenApiInfo(String group, String version, OpenAPI openApi, Map<Pair<String, String>, OpenApiInfo> openApiInfoMap, VisitorContext context) {
        GroupProperties groupProperties = getGroupProperties(group, context);
        boolean hasGroupProperties = groupProperties != null;

        Pair<String, String> key = Pair.of(group, version);
        OpenApiInfo openApiInfo = openApiInfoMap.get(key);
        OpenAPI newOpenApi;
        if (openApiInfo == null) {

            Map<String, OpenAPI> knownOpenApis = Utils.getOpenApis();
            if (CollectionUtils.isNotEmpty(knownOpenApis) && knownOpenApis.containsKey(group)) {
                newOpenApi = knownOpenApis.get(group);
            } else {
                newOpenApi = new OpenAPI();
            }

            openApiInfo = new OpenApiInfo(
                    version,
                    group,
                    hasGroupProperties ? groupProperties.getDisplayName() : null,
                    hasGroupProperties ? groupProperties.getFilename() : null,
                    newOpenApi
            );

            openApiInfoMap.put(key, openApiInfo);

            OpenAPI openApiCopy;
            try {
                openApiCopy = ConvertUtils.getJsonMapper().treeToValue(ConvertUtils.getJsonMapper().valueToTree(openApi), OpenAPI.class);
            } catch (JsonProcessingException e) {
                context.warn("Error\n" + Utils.printStackTrace(e), null);
                return null;
            }

            if (CollectionUtils.isEmpty(knownOpenApis) || !knownOpenApis.containsKey(group)) {
                newOpenApi.setTags(openApiCopy.getTags());
                newOpenApi.setServers(openApiCopy.getServers());
                newOpenApi.setInfo(openApiCopy.getInfo());
                newOpenApi.setSecurity(openApiCopy.getSecurity());
                newOpenApi.setExternalDocs(openApiCopy.getExternalDocs());
            }

            newOpenApi.setComponents(openApiCopy.getComponents());

        } else {
            newOpenApi = openApiInfo.getOpenApi();
        }

        return newOpenApi;
    }

    private void mergeMicronautEndpointInfos(OpenAPI openApi, VisitorContext context) {

        Map<String, List<EndpointInfo>> endpointInfosMap = Utils.getEndpointInfos();
        if (CollectionUtils.isEmpty(endpointInfosMap)) {
            return;
        }
        // we need to merge operations for single path without versions
        for (List<EndpointInfo> endpointInfos : endpointInfosMap.values()) {
            for (EndpointInfo endpointInfo : endpointInfos) {
                PathItem pathItem = openApi.getPaths().get(endpointInfo.getUrl());
                Operation operation = getOperationOnPathItem(pathItem, endpointInfo.getHttpMethod());
                if (operation == null) {
                    setOperationOnPathItem(pathItem, endpointInfo.getHttpMethod(), endpointInfo.getOperation());
                    continue;
                }
                if (endpointInfo.getVersion() == null) {
                    setOperationOnPathItem(pathItem, endpointInfo.getHttpMethod(), SchemaUtils.mergeOperations(operation, endpointInfo.getOperation()));
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private OpenAPI postProcessOpenApi(OpenAPI openApi, VisitorContext context) {

        applyPropertyNamingStrategy(openApi, context);
        applyPropertyServerContextPath(openApi, context);

        normalizeOpenApi(openApi);
        // Process after sorting so order is stable
        new JacksonDiscriminatorPostProcessor().addMissingDiscriminatorType(openApi);
        new OpenApiOperationsPostProcessor().processOperations(openApi);

        // remove unused schemas
        try {
            if (openApi.getComponents() != null) {
                Map<String, Schema> schemas = openApi.getComponents().getSchemas();
                if (CollectionUtils.isNotEmpty(schemas)) {
                    String openApiJson = ConvertUtils.getJsonMapper().writeValueAsString(openApi);
                    // Create a copy of the keySet so that we can modify the map while in a foreach
                    Set<String> keySet = new HashSet<>(schemas.keySet());
                    for (String schemaName : keySet) {
                        if (!openApiJson.contains("\"" + COMPONENTS_SCHEMAS_REF + schemaName + '"')) {
                            schemas.remove(schemaName);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // do nothing
        }

        removeEmtpyComponents(openApi);

        openApi = resolvePropertyPlaceHolders(openApi, context);

        return openApi;
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

    private void normalizeOpenApi(OpenAPI openAPI) {
        // Sort paths
        if (openAPI.getPaths() != null) {
            io.swagger.v3.oas.models.Paths sortedPaths = new io.swagger.v3.oas.models.Paths();
            new TreeMap<>(openAPI.getPaths()).forEach(sortedPaths::addPathItem);
            if (openAPI.getPaths().getExtensions() != null) {
                sortedPaths.setExtensions(new TreeMap<>(openAPI.getPaths().getExtensions()));
            }
            openAPI.setPaths(sortedPaths);
            for (PathItem pathItem : sortedPaths.values()) {
                normalizeOperation(pathItem.getGet());
                normalizeOperation(pathItem.getPut());
                normalizeOperation(pathItem.getPost());
                normalizeOperation(pathItem.getDelete());
                normalizeOperation(pathItem.getOptions());
                normalizeOperation(pathItem.getHead());
                normalizeOperation(pathItem.getPatch());
                normalizeOperation(pathItem.getTrace());
            }
        }

        // Sort all reusable Components
        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }

        normalizeSchemas(components.getSchemas());

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

    private void normalizeOperation(Operation operation) {
        if (operation == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(operation.getParameters())) {
            for (Parameter parameter : operation.getParameters()) {
                if (parameter == null) {
                    continue;
                }
                Schema<?> paramSchema = parameter.getSchema();
                if (paramSchema == null) {
                    continue;
                }
                Schema<?> normalizedSchema = normalizeSchema(paramSchema);
                if (normalizedSchema != null) {
                    parameter.setSchema(normalizedSchema);
                } else if (paramSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                    paramSchema.setType(TYPE_OBJECT);
                }
            }
        }
        if (operation.getRequestBody() != null) {
            normalizeContent(operation.getRequestBody().getContent());
        }
        if (CollectionUtils.isNotEmpty(operation.getResponses())) {
            for (ApiResponse apiResponse : operation.getResponses().values()) {
                normalizeContent(apiResponse.getContent());
            }
        }
    }

    private void normalizeContent(Content content) {
        if (CollectionUtils.isEmpty(content)) {
            return;
        }
        for (MediaType mediaType : content.values()) {
            Schema<?> mediaTypeSchema = mediaType.getSchema();
            if (mediaTypeSchema == null) {
                continue;
            }
            Schema<?> normalizedSchema = normalizeSchema(mediaTypeSchema);
            if (normalizedSchema != null) {
                mediaType.setSchema(normalizedSchema);
            } else if (mediaTypeSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                mediaTypeSchema.setType(TYPE_OBJECT);
            }
            Map<String, Schema> paramSchemas = mediaTypeSchema.getProperties();
            if (CollectionUtils.isNotEmpty(paramSchemas)) {
                Map<String, Schema> paramNormalizedSchemas = new HashMap<>();
                for (Map.Entry<String, Schema> paramEntry : paramSchemas.entrySet()) {
                    Schema<?> paramSchema = paramEntry.getValue();
                    Schema<?> paramNormalizedSchema = normalizeSchema(paramSchema);
                    if (paramNormalizedSchema != null) {
                        paramNormalizedSchemas.put(paramEntry.getKey(), paramNormalizedSchema);
                    }
                }
                if (CollectionUtils.isNotEmpty(paramNormalizedSchemas)) {
                    paramSchemas.putAll(paramNormalizedSchemas);
                }
            }
        }
    }

    private <T> void sortComponent(Components components, Function<Components, Map<String, T>> getter, BiConsumer<Components, Map<String, T>> setter) {
        if (components != null && getter.apply(components) != null) {
            Map<String, T> component = getter.apply(components);
            setter.accept(components, new TreeMap<>(component));
        }
    }

    private Schema<?> normalizeSchema(Schema<?> schema) {
        List<Schema> allOf = schema.getAllOf();
        if (CollectionUtils.isEmpty(allOf)) {
            return null;
        }

        if (allOf.size() == 1) {

            Schema<?> allOfSchema = allOf.get(0);

            schema.setAllOf(null);
            // if schema has only allOf block with one item or only defaultValue property or only type
            Object defaultValue = schema.getDefault();
            String type = schema.getType();
            String serializedDefaultValue;
            try {
                serializedDefaultValue = defaultValue != null ? ConvertUtils.getJsonMapper().writeValueAsString(defaultValue) : null;
            } catch (JsonProcessingException e) {
                return null;
            }
            schema.setDefault(null);
            schema.setType(null);
            Schema<?> normalizedSchema = null;

            Object allOfDefaultValue = allOfSchema.getDefault();
            String serializedAllOfDefaultValue;
            try {
                serializedAllOfDefaultValue = allOfDefaultValue != null ? ConvertUtils.getJsonMapper().writeValueAsString(allOfDefaultValue) : null;
            } catch (JsonProcessingException e) {
                return null;
            }
            boolean isSameType = allOfSchema.getType() == null || allOfSchema.getType().equals(type);

            if (SchemaUtils.isEmptySchema(schema)
                    && (serializedDefaultValue == null || serializedDefaultValue.equals(serializedAllOfDefaultValue))
                    && (type == null || allOfSchema.getType() == null || allOfSchema.getType().equals(type))) {
                normalizedSchema = allOfSchema;
            }
            schema.setType(type);
            schema.setAllOf(allOf);
            schema.setDefault(defaultValue);
            return normalizedSchema;
        }
        List<Schema> finalList = new ArrayList<>(allOf.size());
        List<Schema> schemasWithoutRef = new ArrayList<>(allOf.size() - 1);
        for (Schema<?> schemaAllOf : allOf) {
            Schema<?> normalizedSchema = normalizeSchema(schemaAllOf);
            if (normalizedSchema != null) {
                schemaAllOf = normalizedSchema;
            }
            Map<String, Schema> paramSchemas = schemaAllOf.getProperties();
            if (CollectionUtils.isNotEmpty(paramSchemas)) {
                Map<String, Schema> paramNormalizedSchemas = new HashMap<>();
                for (Map.Entry<String, Schema> paramEntry : paramSchemas.entrySet()) {
                    Schema<?> paramSchema = paramEntry.getValue();
                    Schema<?> paramNormalizedSchema = normalizeSchema(paramSchema);
                    if (paramNormalizedSchema != null) {
                        paramNormalizedSchemas.put(paramEntry.getKey(), paramNormalizedSchema);
                    }
                }
                if (CollectionUtils.isNotEmpty(paramNormalizedSchemas)) {
                    paramSchemas.putAll(paramNormalizedSchemas);
                }
            }

            if (StringUtils.isEmpty(schemaAllOf.get$ref())) {
                schemasWithoutRef.add(schemaAllOf);
                // remove all description fields, if it's already set in main schema
                if (StringUtils.isNotEmpty(schema.getDescription())
                        && StringUtils.isNotEmpty(schemaAllOf.getDescription())) {
                    schemaAllOf.setDescription(null);
                }
                // remove deplicate default field
                if (schema.getDefault() != null
                        && schemaAllOf.getDefault() != null && schema.getDefault().equals(schemaAllOf.getDefault())) {
                    schema.setDefault(null);
                }
                continue;
            }
            finalList.add(schemaAllOf);
        }
        finalList.addAll(schemasWithoutRef);
        schema.setAllOf(finalList);
        return null;
    }

    /**
     * Sort schemas list in allOf block: schemas with ref must be first, next other schemas.
     *
     * @param schemas all schema components
     */
    private void normalizeSchemas(Map<String, Schema> schemas) {

        if (CollectionUtils.isEmpty(schemas)) {
            return;
        }

        Map<String, Schema> normalizedSchemas = new HashMap<>();

        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            Schema<?> schema = entry.getValue();
            Schema<?> normalizedSchema = normalizeSchema(schema);
            if (normalizedSchema != null) {
                normalizedSchemas.put(entry.getKey(), normalizedSchema);
            } else if (schema.equals(EMPTY_SIMPLE_SCHEMA)) {
                schema.setType(TYPE_OBJECT);
            }

            Map<String, Schema> paramSchemas = schema.getProperties();
            if (CollectionUtils.isNotEmpty(paramSchemas)) {
                Map<String, Schema> paramNormalizedSchemas = new HashMap<>();
                for (Map.Entry<String, Schema> paramEntry : paramSchemas.entrySet()) {
                    Schema<?> paramSchema = paramEntry.getValue();
                    Schema<?> paramNormalizedSchema = normalizeSchema(paramSchema);
                    if (paramNormalizedSchema != null) {
                        paramNormalizedSchemas.put(paramEntry.getKey(), paramNormalizedSchema);
                    } else if (paramSchema.equals(EMPTY_SIMPLE_SCHEMA)) {
                        paramSchema.setType(TYPE_OBJECT);
                    }
                }
                if (CollectionUtils.isNotEmpty(paramNormalizedSchemas)) {
                    paramSchemas.putAll(paramNormalizedSchemas);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(normalizedSchemas)) {
            schemas.putAll(normalizedSchemas);
        }
    }

    private void writeYamlToFile(Map<Pair<String, String>, OpenApiInfo> openApiInfos, String documentTitle, VisitorContext context, boolean isYaml) {

        Path viewsDestDirs = null;

        for (OpenApiInfo openApiInfo : openApiInfos.values()) {
            Optional<Path> specFile = openApiSpecFile(openApiInfo.getFilename(), context);
            try (Writer writer = getFileWriter(specFile.orElse(null))) {
                (isYaml ? ConvertUtils.getYamlMapper() : ConvertUtils.getJsonMapper()).writeValue(writer, openApiInfo.getOpenApi());
                if (Utils.isTestMode()) {
                    Utils.setTestFileName(openApiInfo.getFilename());
                    if (isYaml) {
                        Utils.setTestYamlReference(writer.toString());
                    } else {
                        Utils.setTestJsonReference(writer.toString());
                    }
                } else {
                    @SuppressWarnings("OptionalGetWithoutIsPresent")
                    Path specPath = specFile.get();
                    context.info("Writing OpenAPI file to destination: " + specPath);
                    viewsDestDirs = getViewsDestDir(getDefaultFilePath(openApiInfo.getFilename(), context).get(), context);
                    context.info("Writing OpenAPI views to destination: " + viewsDestDirs);
                    final Path viewsDestDirsFinal = viewsDestDirs;
                    context.getClassesOutputPath().ifPresent(path -> {
                        // add relative paths for the specPath, and its parent META-INF/swagger
                        // so that micronaut-graal visitor knows about them
                        context.addGeneratedResource(path.relativize(specPath).toString());
                        context.addGeneratedResource(path.relativize(specPath.getParent()).toString());
                        context.addGeneratedResource(path.relativize(viewsDestDirsFinal).toString());
                    });
                    openApiInfo.setSpecFilePath(specPath.getFileName().toString());
                }
            } catch (Exception e) {
                context.warn("Unable to generate swagger" + (isYaml ? EXT_YML : EXT_JSON) + ": " + specFile.orElse(null) + " - " + e.getMessage() + ".\n" + Utils.printStackTrace(e), classElement);
            }
        }
        if (!Utils.isTestMode() && viewsDestDirs != null) {
            try {
                renderViews(documentTitle, openApiInfos, viewsDestDirs, context);
            } catch (Exception e) {
                context.warn("Unable to render swagger" + (isYaml ? EXT_YML : EXT_JSON) + ": " + openApiInfos.values()
                        .stream()
                        .map(OpenApiInfo::getSpecFilePath)
                        .collect(Collectors.joining(", ", "files ", "")) + " - " + e.getMessage() + ".\n" + Utils.printStackTrace(e), classElement);
            }
        }
    }

    private Writer getFileWriter(Path specFile) throws IOException {
        if (Utils.isTestMode()) {
            return new StringWriter();
        } else if (specFile != null) {
            return Files.newBufferedWriter(specFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } else {
            throw new IOException("Swagger spec file location is not present");
        }
    }

    private void processEndpoints(VisitorContext context) {
        EndpointsConfiguration endpointsCfg = endpointsConfiguration(context);
        if (endpointsCfg.isEnabled() && CollectionUtils.isNotEmpty(endpointsCfg.getEndpoints())) {
            OpenApiEndpointVisitor visitor = new OpenApiEndpointVisitor(true);
            endpointsCfg.getEndpoints().values().stream()
                    .filter(endpoint -> endpoint.getClassElement().isPresent())
                    .forEach(endpoint -> {
                        ClassElement classEl = endpoint.getClassElement().get();
                        context.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS, endpoint.getTags());
                        context.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS, endpoint.getServers());
                        context.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, endpoint.getSecurityRequirements());
                        visitor.visitClass(classEl, context);
                        for (MethodElement methodEl : classEl.getEnclosedElements(ElementQuery.ALL_METHODS
                                .modifiers(mods -> !mods.contains(ElementModifier.STATIC) && !mods.contains(ElementModifier.PRIVATE))
                                .named(name -> !name.contains("$")))) {
                            visitor.visitMethod(methodEl, context);
                        }
                    });
        }
    }

    static class LowerCamelCasePropertyNamingStrategy extends PropertyNamingStrategies.NamingBase {

        @Serial
        private static final long serialVersionUID = -2750503285679998670L;

        @Override
        public String translate(String propertyName) {
            return propertyName;
        }

    }

}
