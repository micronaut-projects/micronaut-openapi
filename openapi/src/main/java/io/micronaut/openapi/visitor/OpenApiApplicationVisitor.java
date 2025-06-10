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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.annotation.OpenAPIGroupInfo;
import io.micronaut.openapi.annotation.OpenAPIGroupInfos;
import io.micronaut.openapi.postprocessors.JacksonDiscriminatorPostProcessor;
import io.micronaut.openapi.postprocessors.OpenApiOperationsPostProcessor;
import io.micronaut.openapi.view.OpenApiViewConfig;
import io.micronaut.openapi.visitor.group.EndpointGroupInfo;
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static io.micronaut.openapi.adoc.utils.FileUtils.CLASSPATH_SCHEME;
import static io.micronaut.openapi.adoc.utils.FileUtils.FILE_SCHEME;
import static io.micronaut.openapi.adoc.utils.FileUtils.PROJECT_SCHEME;
import static io.micronaut.openapi.visitor.ConfigUtils.ALL_ENDPOINTS_NAME;
import static io.micronaut.openapi.visitor.ConfigUtils.MergeMode.REPLACE;
import static io.micronaut.openapi.visitor.ConfigUtils.getAdditionalFiles;
import static io.micronaut.openapi.visitor.ConfigUtils.getAdditionalFilesMergeMode;
import static io.micronaut.openapi.visitor.ConfigUtils.getAdocProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getConfigProperty;
import static io.micronaut.openapi.visitor.ConfigUtils.getEndpointsConfig;
import static io.micronaut.openapi.visitor.ConfigUtils.getEnv;
import static io.micronaut.openapi.visitor.ConfigUtils.getExpandableProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getGroupProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getProjectPath;
import static io.micronaut.openapi.visitor.ConfigUtils.getPropertyNamingStrategy;
import static io.micronaut.openapi.visitor.ConfigUtils.isEndpointsEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.readOpenApiConfigFile;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_DESCRIPTION;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_EXTENSIONS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_PROPS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS;
import static io.micronaut.openapi.visitor.ContextUtils.addGeneratedResource;
import static io.micronaut.openapi.visitor.ContextUtils.info;
import static io.micronaut.openapi.visitor.ContextUtils.put;
import static io.micronaut.openapi.visitor.ContextUtils.remove;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.FileUtils.EXT_JSON;
import static io.micronaut.openapi.visitor.FileUtils.EXT_YML;
import static io.micronaut.openapi.visitor.FileUtils.calcFinalFilename;
import static io.micronaut.openapi.visitor.FileUtils.getViewsDestDir;
import static io.micronaut.openapi.visitor.FileUtils.openApiSpecFile;
import static io.micronaut.openapi.visitor.FileUtils.readFile;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ALL;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_APPLICATION_NAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_JSON_FORMAT;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VIEWS_SPEC;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.SPRING_APPLICATION_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SECURITY;
import static io.micronaut.openapi.visitor.OpenApiNormalizeUtils.findAndRemoveDuplicates;
import static io.micronaut.openapi.visitor.OpenApiNormalizeUtils.normalizeOpenApi;
import static io.micronaut.openapi.visitor.OpenApiNormalizeUtils.removeEmptyComponents;
import static io.micronaut.openapi.visitor.SchemaDefinitionUtils.toValue;
import static io.micronaut.openapi.visitor.SchemaUtils.copyOpenApi;
import static io.micronaut.openapi.visitor.SchemaUtils.getOperationOnPathItem;
import static io.micronaut.openapi.visitor.SchemaUtils.resolveSchemas;
import static io.micronaut.openapi.visitor.SchemaUtils.setOperationOnPathItem;
import static io.micronaut.openapi.visitor.SecurityUtils.processSecuritySchemes;
import static io.micronaut.openapi.visitor.SecurityUtils.readSecurityRequirements;
import static io.micronaut.openapi.visitor.StringUtil.PLACEHOLDER_POSTFIX;
import static io.micronaut.openapi.visitor.StringUtil.PLACEHOLDER_PREFIX;
import static io.micronaut.openapi.visitor.StringUtil.QUOTE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;
import static io.micronaut.openapi.visitor.TagUtils.processOpenApiAnnotation;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static io.micronaut.openapi.visitor.management.EndpointUtils.SPECIFIC_ENDPOINTS;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Visits the application class.
 *
 * @author graemerocher
 * @since 1.0
 */
public class OpenApiApplicationVisitor extends AbstractOpenApiVisitor implements TypeElementVisitor<Object, Object> {

    public static final String DEFAULT_OPENAPI_TITLE = "Service";
    public static final String DEFAULT_OPENAPI_VERSION = "1.0.0";
    public static final String DEFAULT_DOCUMENT_TITLE = "OpenAPI";
    public static final String PREFIX_DUMMY_FILE = "dummy";

    private static final int MAX_ITERATIONS = 100;
    private ClassElement classElement;
    private int visitedElements = -1;

    @Override
    public void start(VisitorContext context) {
        Utils.init(context);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return ALL;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        try {
            if (!isOpenApiEnabled(context) || !isSpecGenerationEnabled(context)) {
                return;
            }
            if (ignore(element)) {
                return;
            }
            incrementVisitedElements(context);

            info("Generating OpenAPI Documentation", context);
            OpenAPI openApi = readOpenApi(element, context);

            // Handle Application securityRequirements schemes
            processSecuritySchemes(element, context);

            // handle type level tags
            List<Tag> tagList = processOpenApiAnnotation(
                element,
                context,
                io.swagger.v3.oas.annotations.tags.Tag.class,
                Tag.class,
                openApi.getTags()
            );
            openApi.setTags(tagList);

            // handle type level security requirements
            List<SecurityRequirement> securityRequirements = readSecurityRequirements(element);
            if (openApi.getSecurity() != null) {
                securityRequirements.addAll(openApi.getSecurity());
            }

            openApi.setSecurity(securityRequirements);

            // handle type level servers
            List<Server> servers = processOpenApiAnnotation(
                element,
                context,
                io.swagger.v3.oas.annotations.servers.Server.class,
                Server.class,
                openApi.getServers()
            );
            openApi.setServers(servers);

            OpenAPI existing = ContextUtils.get(Utils.ATTR_OPENAPI, OpenAPI.class, context);
            if (existing != null) {
                if (openApi.getInfo() != null) {
                    existing.setInfo(openApi.getInfo());
                }
                copyOpenApi(existing, openApi, false);
            } else {
                ContextUtils.put(Utils.ATTR_OPENAPI, openApi, context);
            }

            if (Utils.isTestMode()) {
                Utils.resolveOpenApi(context);
            }

            classElement = element;
        } catch (Exception e) {
            warn("Error with processing class:\n" + Utils.printStackTrace(e), context, classElement);
        }
    }

    private boolean ignore(ClassElement element) {
        return !element.isAnnotationPresent(OpenAPIDefinition.class)
            && !element.isAnnotationPresent(OpenAPIGroupInfo.class)
            && !element.isAnnotationPresent(OpenAPIGroupInfos.class);
    }

    /**
     * Merge the OpenAPI YAML and JSON files into one single file.
     *
     * @param openApi The {@link OpenAPI} object for the application
     * @param context The visitor context
     */
    private void mergeAdditionalOpenApiFiles(OpenAPI openApi, VisitorContext context) {
        List<String> additionalSwaggerFiles = getAdditionalFiles(context);
        if (CollectionUtils.isEmpty(additionalSwaggerFiles)) {
            return;
        }

        for (var additionalSwaggerFile : additionalSwaggerFiles) {
            additionalSwaggerFile = additionalSwaggerFile.trim();

            if (additionalSwaggerFile.startsWith(CLASSPATH_SCHEME)) {
                var resourceLoader = new DefaultClassPathResourceLoader(getClass().getClassLoader());
                additionalSwaggerFile = additionalSwaggerFile.substring(CLASSPATH_SCHEME.length());
                if (additionalSwaggerFile.startsWith(SLASH)) {
                    additionalSwaggerFile = additionalSwaggerFile.substring(SLASH.length());
                }
                Optional<InputStream> inOpt = resourceLoader.getResourceAsStream(additionalSwaggerFile);
                if (inOpt.isEmpty()) {
                    warn("Fail to load " + additionalSwaggerFile, context);
                    continue;
                }
                try (InputStream in = inOpt.get();
                     var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                ) {
                    var openapiFile = readFile(reader);
                    boolean isYaml = FileUtils.isYaml(additionalSwaggerFile.toLowerCase());
                    info("Reading OpenAPI " + (isYaml ? "YAML" : "JSON") + " file " + additionalSwaggerFile, context);
                    OpenAPI parsedOpenApi = null;
                    try {
                        parsedOpenApi = (isYaml ? Utils.getYamlMapper() : Utils.getJsonMapper()).readValue(openapiFile, OpenAPI.class);
                    } catch (IOException e) {
                        warn("Unable to read file " + additionalSwaggerFile + ": " + e.getMessage(), context);
                    }
                    copyOpenApi(openApi, parsedOpenApi, getAdditionalFilesMergeMode(context) == REPLACE);

                } catch (IOException e) {
                    warn("Fail to load " + additionalSwaggerFile + "\n" + Utils.printStackTrace(e), context);
                }
            } else if (additionalSwaggerFile.startsWith(PROJECT_SCHEME)) {
                additionalSwaggerFile = additionalSwaggerFile.substring(PROJECT_SCHEME.length());
                String projectDir = StringUtils.EMPTY_STRING;
                Path projectPath = getProjectPath(context);
                if (projectPath != null) {
                    projectDir = projectPath.toString().replace("\\\\", SLASH).replace("\\", SLASH);
                }
                if (!projectDir.endsWith(SLASH)) {
                    projectDir += SLASH;
                }
                if (additionalSwaggerFile.startsWith(SLASH)) {
                    additionalSwaggerFile = additionalSwaggerFile.substring(SLASH.length());
                }
                additionalSwaggerFile = projectDir + additionalSwaggerFile;
            } else {
                if (additionalSwaggerFile.startsWith(FILE_SCHEME)) {
                    additionalSwaggerFile = additionalSwaggerFile.substring(FILE_SCHEME.length());
                }
            }

            Path path = resolve(context, java.nio.file.Path.of(additionalSwaggerFile));
            if (!Files.exists(path)) {
                warn(path + " does not exist", context);
                continue;
            }

            if (Files.isDirectory(path)) {
                info("Merging OpenAPI YAML and JSON files from location: " + path, context);
                var foundAnyFile = new AtomicBoolean(false);
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(path, p -> {
                    var pathStr = p.toString().toLowerCase();
                    return FileUtils.isYaml(pathStr) || FileUtils.isJson(pathStr);
                })) {
                    foundAnyFile.set(true);
                    for (var pathInDir : paths) {
                        readAndMergeAdditionalFile(pathInDir, openApi, context);
                    }
                } catch (IOException e) {
                    warn("Unable to read  file from " + path + ": " + e.getMessage(), context);
                }
                if (!foundAnyFile.get()) {
                    warn("Not found any OpenAPI YAML or JSON files in directory " + path, context);
                }
            } else {
                var pathStr = path.toString().toLowerCase();
                if (!FileUtils.isYaml(pathStr) && !FileUtils.isJson(pathStr)) {
                    warn("Unknown file type: " + path + ". Must be json, yaml or yml", context);
                    return;
                }

                readAndMergeAdditionalFile(path, openApi, context);
            }
        }
    }

    private void readAndMergeAdditionalFile(Path path, OpenAPI openApi, VisitorContext context) {
        boolean isYaml = FileUtils.isYaml(path.toString().toLowerCase());
        info("Reading OpenAPI " + (isYaml ? "YAML" : "JSON") + " file " + path.getFileName(), context);
        OpenAPI parsedOpenApi = null;
        try {
            parsedOpenApi = (isYaml ? Utils.getYamlMapper() : Utils.getJsonMapper()).readValue(path.toFile(), OpenAPI.class);
        } catch (IOException e) {
            warn("Unable to read file " + path.getFileName() + ": " + e.getMessage(), context);
        }
        copyOpenApi(openApi, parsedOpenApi, getAdditionalFilesMergeMode(context) == REPLACE);
    }

    private OpenAPI readOpenApi(ClassElement element, VisitorContext context) {
        var openApiDefAnn = element.findAnnotation(OpenAPIDefinition.class).orElse(null);
        if (openApiDefAnn == null) {
            var openApi = new OpenAPI();
            if (Utils.isOpenapi31()) {
                openApi.openapi(OpenApiUtils.OPENAPI_31_VERSION)
                    .jsonSchemaDialect(ConfigUtils.getJsonSchemaDialect(context))
                    .specVersion(SpecVersion.V31);
            }
            return openApi;
        }

        var openApi = toValue(openApiDefAnn.getAnnotationName(), openApiDefAnn.getValues(), context, OpenAPI.class, null);
        if (openApi == null) {
            openApi = new OpenAPI();
        }
        if (Utils.isOpenapi31()) {
            openApi.openapi(OpenApiUtils.OPENAPI_31_VERSION)
                .jsonSchemaDialect(ConfigUtils.getJsonSchemaDialect(context))
                .specVersion(SpecVersion.V31);
        }
        var secRequirementAnns = openApiDefAnn.getAnnotations(PROP_SECURITY, io.swagger.v3.oas.annotations.security.SecurityRequirement.class);
        if (CollectionUtils.isNotEmpty(secRequirementAnns)) {
            var securityRequirements = new ArrayList<SecurityRequirement>();
            for (var secRequirementAnn : secRequirementAnns) {
                securityRequirements.add(ConvertUtils.mapToSecurityRequirement(secRequirementAnn));
            }
            openApi.setSecurity(securityRequirements);
        }
        return openApi;
    }

    private void renderViews(String title, Map<Pair<String, String>, OpenApiInfo> openApiInfos, Path destinationDir, VisitorContext context) throws IOException {
        String viewSpecification = getConfigProperty(MICRONAUT_OPENAPI_VIEWS_SPEC, context);
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(viewSpecification, openApiInfos, readOpenApiConfigFile(context), context);
        if (cfg.isEnabled()) {
            cfg.setTitle(title);
            if (CollectionUtils.isNotEmpty(openApiInfos)) {
                cfg.setSpecFile(openApiInfos.values().iterator().next().getSpecFilePath());
            }
            cfg.render(destinationDir, context);
        }
    }

    private void applyPropertyNamingStrategy(OpenAPI openAPI, VisitorContext context) {
        var propertyNamingStrategy = getPropertyNamingStrategy(context);
        if (propertyNamingStrategy == null) {
            return;
        }
        info("Using " + propertyNamingStrategy.getClass().getSimpleName() + " property naming strategy.", context);
        if (openAPI.getComponents() == null || CollectionUtils.isEmpty(openAPI.getComponents().getSchemas())) {
            return;
        }
        for (var schema : openAPI.getComponents().getSchemas().values()) {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                var newProps = new LinkedHashMap<String, Schema>(properties.size());
                for (var entry : properties.entrySet()) {
                    newProps.put(propertyNamingStrategy.translate(entry.getKey()), entry.getValue());
                }
                schema.setProperties(newProps);
            }
            List<String> required = schema.getRequired();
            if (CollectionUtils.isNotEmpty(required)) {
                var newRequired = new ArrayList<String>(required.size());
                for (var req : required) {
                    newRequired.add(propertyNamingStrategy.translate(req));
                }
                schema.setRequired(newRequired);
            }
        }
    }

    private void applyPropertyServerContextPath(OpenAPI openAPI, VisitorContext context) {
        final String serverContextPath = getConfigProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, context);
        if (StringUtils.isEmpty(serverContextPath)) {
            return;
        }
        info("Applying server context path: " + serverContextPath + " to Paths.", context);
        Paths paths = openAPI.getPaths();
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        var newPaths = new Paths();
        for (Map.Entry<String, PathItem> path : paths.entrySet()) {
            final String mapping = path.getKey();
            String newPath = mapping.startsWith(serverContextPath) ? mapping : StringUtils.prependUri(serverContextPath, mapping);
            if (!newPath.startsWith(SLASH) && !newPath.startsWith(StringUtil.DOLLAR)) {
                newPath = SLASH + newPath;
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
        var i = onode.fields();
        while (i.hasNext()) {
            var entry = i.next();
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
        if (StringUtils.isEmpty(s) || !s.contains(PLACEHOLDER_PREFIX)) {
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
        if (StringUtils.isEmpty(value) || !value.contains(PLACEHOLDER_PREFIX)) {
            return value;
        }
        // system properties
        if (CollectionUtils.isNotEmpty(System.getProperties())) {
            for (Map.Entry<Object, Object> sysProp : System.getProperties().entrySet()) {
                value = value.replace(PLACEHOLDER_PREFIX + sysProp.getKey().toString() + PLACEHOLDER_POSTFIX, sysProp.getValue().toString());
            }
        }

        // form openapi file
        for (Map.Entry<Object, Object> fileProp : readOpenApiConfigFile(context).entrySet()) {
            value = value.replace(PLACEHOLDER_PREFIX + fileProp.getKey().toString() + PLACEHOLDER_POSTFIX, fileProp.getValue().toString());
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
            info("Expanding properties: " + expandableProperties, context);
        }
        JsonNode root = resolvePlaceholders(Utils.getYamlMapper().convertValue(openAPI, ObjectNode.class), s -> expandProperties(s, expandableProperties, context));
        return Utils.getYamlMapper().convertValue(root, OpenAPI.class);
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

            Map<Pair<String, String>, OpenApiInfo> openApiInfos = null;
            String documentTitle = DEFAULT_DOCUMENT_TITLE;

            if (isSpecGenerationEnabled(context)) {
                OpenAPI openApi = ContextUtils.get(Utils.ATTR_OPENAPI, OpenAPI.class, context);
                if (openApi == null) {
                    return;
                }
                processEndpoints(context);

                mergeMicronautEndpointInfos(openApi, context);
                openApiInfos = divideOpenapiByGroupsAndVersions(openApi, context);
                if (Utils.isTestMode()) {
                    Utils.setTestReferences(openApiInfos);
                }

                String isJson = getConfigProperty(MICRONAUT_OPENAPI_JSON_FORMAT, context);
                boolean isYaml = StringUtils.isEmpty(isJson) || !isJson.equalsIgnoreCase(StringUtils.TRUE);
                String ext = isYaml ? EXT_YML : EXT_JSON;

                for (Map.Entry<Pair<String, String>, OpenApiInfo> entry : openApiInfos.entrySet()) {

                    OpenApiInfo openApiInfo = entry.getValue();

                    openApi = openApiInfo.getOpenApi();

                    openApi = postProcessOpenApi(openApi, context);
                    openApiInfo.setOpenApi(openApi);
                    // need to set test reference to openApi after post-processing
                    if (Utils.isTestMode()) {
                        Utils.setTestReference(openApi);
                    }

                    var titleAndFilename = calcFinalFilename(openApiInfo.getFilename(), openApiInfo, openApiInfos.size() == 1, ext, context);
                    documentTitle = titleAndFilename.getFirst();
                    openApiInfo.setFilename(titleAndFilename.getSecond());
                }

                writeYamlToFile(openApiInfos, documentTitle, context, isYaml);
            }

            generateViews(documentTitle, openApiInfos, context);

            visitedElements = visitedElements(context);
        } catch (Exception e) {
            warn("Error:\n" + Utils.printStackTrace(e), context);
            throw e;
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

        var commonEndpoints = new ArrayList<EndpointInfo>();

        // key version, groupName
        var result = new HashMap<Pair<String, String>, OpenApiInfo>();

        for (List<EndpointInfo> endpointInfos : endpointInfosMap.values()) {
            for (EndpointInfo endpointInfo : endpointInfos) {
                if (CollectionUtils.isEmpty(endpointInfo.getGroups()) && endpointInfo.getVersion() == null) {
                    commonEndpoints.add(endpointInfo);
                    continue;
                }
                for (EndpointGroupInfo endpointGroupInfo : endpointInfo.getGroups().values()) {
                    if (CollectionUtils.isNotEmpty(endpointInfo.getExcludedGroups())
                        && endpointInfo.getExcludedGroups().contains(endpointGroupInfo.getName())) {
                        continue;
                    }
                    OpenAPI newOpenApi = addOpenApiInfo(endpointGroupInfo.getName(), endpointInfo.getVersion(), openApi, result, context);
                    addOperation(endpointInfo, newOpenApi, endpointGroupInfo, context);
                }

                // if we have only versions without groups
                if (CollectionUtils.isEmpty(endpointInfo.getGroups())) {
                    OpenAPI newOpenApi = addOpenApiInfo(null, endpointInfo.getVersion(), openApi, result, context);
                    addOperation(endpointInfo, newOpenApi, null, context);
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

            OpenAPI groupOpenApi = entry.getValue().getOpenApi();

            for (EndpointInfo commonEndpoint : commonEndpoints) {
                if (CollectionUtils.isNotEmpty(commonEndpoint.getExcludedGroups()) && commonEndpoint.getExcludedGroups().contains(group)) {
                    continue;
                }
                addOperation(commonEndpoint, groupOpenApi, null, context);
            }
        }

        return result;
    }

    private void addOperation(EndpointInfo endpointInfo, OpenAPI openApi, @Nullable EndpointGroupInfo endpointGroupInfo, VisitorContext context) {
        if (openApi == null) {
            return;
        }
        Paths paths = openApi.getPaths();
        if (paths == null) {
            paths = new Paths();
            openApi.setPaths(paths);
        }
        PathItem pathItem = paths.computeIfAbsent(endpointInfo.getUrl(), (pathUrl) -> new PathItem());
        Operation operation = getOperationOnPathItem(pathItem, endpointInfo.getHttpMethod());
        if (operation == null) {
            Operation opCopy = null;
            try {
                opCopy = OpenApiUtils.getJsonMapper().treeToValue(OpenApiUtils.getJsonMapper().valueToTree(endpointInfo.getOperation()), Operation.class);
                if (endpointGroupInfo != null) {
                    addExtensions(opCopy, endpointGroupInfo.getExtensions());
                }
            } catch (JsonProcessingException e) {
                warn("Error\n" + Utils.printStackTrace(e), context);
            }
            setOperationOnPathItem(pathItem, endpointInfo.getHttpMethod(), opCopy != null ? opCopy : endpointInfo.getOperation());
            return;
        }
        var mergedOp = SchemaUtils.mergeOperations(operation, endpointInfo.getOperation());
        if (endpointGroupInfo != null) {
            addExtensions(mergedOp, endpointGroupInfo.getExtensions());
        }
        setOperationOnPathItem(pathItem, endpointInfo.getHttpMethod(), mergedOp);
    }

    private void addExtensions(Operation operation, Map<CharSequence, Object> extensions) {
        if (CollectionUtils.isEmpty(extensions)) {
            return;
        }
        for (var ext : extensions.entrySet()) {
            operation.addExtension(ext.getKey().toString(), ext.getValue());
        }
    }

    private OpenAPI addOpenApiInfo(String groupName, String version, OpenAPI openApi,
                                   Map<Pair<String, String>, OpenApiInfo> openApiInfoMap,
                                   VisitorContext context) {
        GroupProperties groupProperties = getGroupProperties(groupName, context);
        boolean hasGroupProperties = groupProperties != null;

        var key = Pair.of(groupName, version);
        OpenApiInfo openApiInfo = openApiInfoMap.get(key);
        OpenAPI newOpenApi;
        if (openApiInfo == null) {

            Map<String, OpenAPI> knownOpenApis = Utils.getOpenApis();
            if (CollectionUtils.isNotEmpty(knownOpenApis) && knownOpenApis.containsKey(groupName)) {
                newOpenApi = knownOpenApis.get(groupName);
            } else {
                newOpenApi = new OpenAPI();
            }

            openApiInfo = new OpenApiInfo(
                version,
                groupName,
                hasGroupProperties ? groupProperties.getDisplayName() : null,
                hasGroupProperties ? groupProperties.getFilename() : null,
                !hasGroupProperties || groupProperties.getAdocEnabled() == null || groupProperties.getAdocEnabled(),
                hasGroupProperties ? groupProperties.getAdocFilename() : null,
                newOpenApi
            );

            openApiInfoMap.put(key, openApiInfo);

            OpenAPI openApiCopy;
            try {
                openApiCopy = Utils.getJsonMapper().treeToValue(Utils.getJsonMapper().valueToTree(openApi), OpenAPI.class);
            } catch (JsonProcessingException e) {
                warn("Error\n" + Utils.printStackTrace(e), context);
                return null;
            }

            if (CollectionUtils.isEmpty(knownOpenApis) || !knownOpenApis.containsKey(groupName)) {
                newOpenApi.setTags(openApiCopy.getTags());
                newOpenApi.setServers(openApiCopy.getServers());
                newOpenApi.setInfo(openApiCopy.getInfo());
                newOpenApi.setSecurity(openApiCopy.getSecurity());
                newOpenApi.setExternalDocs(openApiCopy.getExternalDocs());
                newOpenApi.setExtensions(openApiCopy.getExtensions());
            }

            // if we have SecuritySchemes specified only for group
            var groupSecuritySchemes = newOpenApi.getComponents() != null ? newOpenApi.getComponents().getSecuritySchemes() : null;
            if (CollectionUtils.isNotEmpty(groupSecuritySchemes)
                && openApiCopy.getComponents() != null
                && CollectionUtils.isNotEmpty(openApiCopy.getComponents().getSecuritySchemes())) {

                for (var entry : openApiCopy.getComponents().getSecuritySchemes().entrySet()) {
                    if (!groupSecuritySchemes.containsKey(entry.getKey())) {
                        groupSecuritySchemes.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            newOpenApi.setComponents(openApiCopy.getComponents());
            if (CollectionUtils.isNotEmpty(groupSecuritySchemes)) {
                resolveComponents(newOpenApi).setSecuritySchemes(groupSecuritySchemes);
            }
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
                if (endpointInfo.getVersion() == null && CollectionUtils.isEmpty(endpointInfo.getGroups())) {
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

        fixInfoBlockIfNeeded(openApi, context);
        applyPropertyNamingStrategy(openApi, context);
        applyPropertyServerContextPath(openApi, context);

        normalizeOpenApi(openApi, context);
        normalizeOpenApi(openApi, context);
        // Process after sorting so order is stable
        new JacksonDiscriminatorPostProcessor().addMissingDiscriminatorType(openApi);
        new OpenApiOperationsPostProcessor().processOperations(openApi);

        removeUnusedSchemas(openApi);

        mergeAdditionalOpenApiFiles(openApi, context);

        removeEmptyComponents(openApi);
        findAndRemoveDuplicates(openApi);

        addExtraSchemas(openApi, context);

        openApi = resolvePropertyPlaceHolders(openApi, context);

        return openApi;
    }

    private void fixInfoBlockIfNeeded(OpenAPI openApi, VisitorContext context) {

        if (openApi.getInfo() == null) {
            openApi.setInfo(new Info());
        }
        var info = openApi.getInfo();
        if (info.getTitle() == null) {
            String applicationName = ConfigUtils.getConfigProperty(MICRONAUT_APPLICATION_NAME, context);
            if (applicationName == null) {
                applicationName = ConfigUtils.getConfigProperty(SPRING_APPLICATION_NAME, context);
            }
            info.setTitle(applicationName != null ? applicationName : DEFAULT_OPENAPI_TITLE);
        }
        if (info.getVersion() == null) {
            info.setVersion(DEFAULT_OPENAPI_VERSION);
        }
    }

    public static void removeUnusedSchemas(OpenAPI openApi) {
        int i = 0;
        // remove unused schemas
        while (removeUnusedSchemasIter(openApi) && i < MAX_ITERATIONS) {
            i++;
        }
    }

    public static boolean removeUnusedSchemasIter(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            return false;
        }
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        if (CollectionUtils.isEmpty(schemas)) {
            return false;
        }

        var extraSchemas = OpenApiExtraSchemaVisitor.getExtraSchemas();
        var removed = false;

        try {
            String openApiJson = Utils.getJsonMapper().writeValueAsString(openApi);
            // Create a copy of the keySet so that we can modify the map while in a foreach
            var keySet = new HashSet<>(schemas.keySet());
            for (String schemaName : keySet) {
                if (!openApiJson.contains(QUOTE + COMPONENTS_SCHEMAS_REF + schemaName + QUOTE)
                    && !extraSchemas.containsKey(schemaName)
                ) {
                    schemas.remove(schemaName);
                    removed = true;
                }
            }
            // check excluded extra schemas also
            for (String schemaName : OpenApiExtraSchemaVisitor.getExcludedExtraSchemas()) {
                schemas.remove(schemaName);
            }
        } catch (JsonProcessingException e) {
            // do nothing
        }
        return removed;
    }

    private void addExtraSchemas(OpenAPI openApi, VisitorContext context) {
        var extraSchemas = OpenApiExtraSchemaVisitor.getExtraSchemas();
        if (CollectionUtils.isEmpty(extraSchemas)) {
            return;
        }
        var schemas = resolveSchemas(openApi);
        for (var entry : extraSchemas.entrySet()) {
            if (schemas.containsKey(entry.getKey())) {
                continue;
            }
            schemas.put(entry.getKey(), entry.getValue());
        }
    }

    private void generateViews(@Nullable String documentTitle, @Nullable Map<Pair<String, String>, OpenApiInfo> openApiInfos, VisitorContext context) {
        Path viewsDestDirs = getViewsDestDir(context);
        if (viewsDestDirs == null) {
            return;
        }
        String viewSpecification = getConfigProperty(MICRONAUT_OPENAPI_VIEWS_SPEC, context);
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(viewSpecification, openApiInfos, readOpenApiConfigFile(context), context);
        if (!cfg.isEnabled()) {
            return;
        }
        if (context != null) {
            info("Writing OpenAPI views to destination: " + viewsDestDirs, context);
            var classesOutputPath = ContextUtils.getClassesOutputPath(context);
            if (classesOutputPath != null) {
                addGeneratedResource(classesOutputPath.relativize(viewsDestDirs).toString(), context);
                addGeneratedResource(classesOutputPath.relativize(viewsDestDirs.getParent()).toString(), context);
            }
        }
        try {
            renderViews(documentTitle, openApiInfos, viewsDestDirs, context);
        } catch (Exception e) {

            String swaggerFiles = StringUtils.EMPTY_STRING;
            if (openApiInfos != null) {
                swaggerFiles = openApiInfos.values().stream()
                    .map(OpenApiInfo::getSpecFilePath)
                    .collect(Collectors.joining(", ", "files ", StringUtils.EMPTY_STRING));
            }

            warn("Unable to render OpenAPI view: " + swaggerFiles + " - " + e.getMessage() + ".\n" + Utils.printStackTrace(e), context, classElement);
        }
    }

    private void writeYamlToFile(Map<Pair<String, String>, OpenApiInfo> openApiInfos, String documentTitle, VisitorContext context, boolean isYaml) {

        var isAdocModuleInClassPath = false;
        var isGlobalAdocEnabled = ConfigUtils.isAdocEnabled(context);

        try {
            Class.forName("io.micronaut.openapi.adoc.OpenApiToAdocConverter");
            isAdocModuleInClassPath = true;
        } catch (ClassNotFoundException e) {
            // do nothing
        }

        var objectMapper = isYaml ? Utils.getYamlMapper() : Utils.getJsonMapper();

        for (OpenApiInfo openApiInfo : openApiInfos.values()) {
            Path specFile = openApiSpecFile(openApiInfo.getFilename(), context);
            try (Writer writer = getFileWriter(specFile)) {
                objectMapper.writeValue(writer, openApiInfo.getOpenApi());
                if (Utils.isTestMode()) {
                    Utils.setTestFileName(openApiInfo.getFilename());
                    if (isYaml) {
                        Utils.setTestYamlReference(writer.toString());
                    } else {
                        Utils.setTestJsonReference(writer.toString());
                    }
                } else {
                    info("Writing OpenAPI file to destination: " + specFile, context);
                    var classesOutputPath = ContextUtils.getClassesOutputPath(context);
                    if (classesOutputPath != null) {
                        // add relative paths for the specFile, and its parent META-INF/swagger
                        // so that micronaut-graal visitor knows about them
                        addGeneratedResource(classesOutputPath.relativize(specFile).toString(), context);
                        addGeneratedResource(classesOutputPath.relativize(specFile.getParent()).toString(), context);
                    }
                    openApiInfo.setSpecFilePath(specFile.getFileName().toString());
                }
                if (isAdocModuleInClassPath && isGlobalAdocEnabled && openApiInfo.isAdocEnabled()) {
                    var adocProperties = getAdocProperties(openApiInfo, openApiInfos.size() == 1, context);
                    AdocModule.convert(openApiInfo, adocProperties, context);
                }
            } catch (Exception e) {
                warn("Unable to generate swagger" + (isYaml ? EXT_YML : EXT_JSON) + ": " + specFile + " - " + e.getMessage() + ".\n" + Utils.printStackTrace(e), context, classElement);
            }
        }
    }

    private Writer getFileWriter(Path specFile) throws IOException {
        if (Utils.isTestMode()) {
            return new StringWriter();
        } else if (specFile != null) {
            return Files.newBufferedWriter(specFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } else {
            throw new IOException("OpenAPI spec file location is not present");
        }
    }

    private void processEndpoints(VisitorContext context) {
        if (!isEndpointsEnabled(context)) {
            return;
        }
        var endpointsConfig = getEndpointsConfig(context);
        if (CollectionUtils.isEmpty(endpointsConfig.getEndpoints())) {
            return;
        }
        var allEndpointsProps = endpointsConfig.getEndpoints().get(ALL_ENDPOINTS_NAME);
        var isAllEnabled = allEndpointsProps != null && allEndpointsProps.getEnabled() != null ? allEndpointsProps.getEnabled() : true;
        var visitor = new OpenApiEndpointVisitor(true);
        var groupVisitor = new OpenApiGroupInfoVisitor(endpointsConfig.getGroups(), endpointsConfig.getGroupsExcluded());

        for (var endpointProps : endpointsConfig.getEndpoints().values()) {
            if (!isAllEnabled || (endpointProps.getEnabled() != null && !endpointProps.getEnabled())) {
                continue;
            }
            ClassElement classEl = endpointProps.getElement();
            if (classEl == null) {
                continue;
            }
            if (!canProcessEndpoint(classEl, context)) {
                continue;
            }

            var tags = new ArrayList<>(endpointsConfig.getTags());
            tags.addAll(endpointProps.getTags());
            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS, tags, context);

            var servers = new ArrayList<>(endpointsConfig.getServers());
            servers.addAll(endpointProps.getServers());
            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS, servers, context);

            var securityRequirements = new ArrayList<>(endpointsConfig.getSecurityRequirements());
            securityRequirements.addAll(endpointProps.getSecurityRequirements());
            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, securityRequirements, context);

            var extensions = new HashMap<>(endpointsConfig.getExtensions());
            extensions.putAll(endpointProps.getExtensions());
            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_EXTENSIONS, extensions, context);

            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_DESCRIPTION, endpointProps.getDescription(), context);
            ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_PROPS, endpointProps, context);

            if ("prometheus".equals(endpointProps.getId())) {
                put(MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS, true, context);
            }

            groupVisitor.visitClass(classEl, context);

            try {
                visitor.visitClass(classEl, context);
                for (MethodElement methodEl : classEl.getEnclosedElements(ElementQuery.ALL_METHODS
                    .modifiers(mods -> !mods.contains(ElementModifier.STATIC) && !mods.contains(ElementModifier.PRIVATE))
                    .named(name -> !name.contains(StringUtil.DOLLAR)))) {

                    visitor.visitMethod(methodEl, context);
                }
            } catch (Exception e) {
                warn("Can't process endpoint " + endpointProps.getId() + ". Skip it", context);
            } finally {
                remove(MICRONAUT_INTERNAL_ENDPOINT_IS_PROMETHEUS, context);
            }
        }
    }

    private boolean canProcessEndpoint(ClassElement classEl, VisitorContext context) {
        var classToCheck = SPECIFIC_ENDPOINTS.get(classEl.getName());
        if (classToCheck == null) {
            return true;
        }
        var checkedClassEl = ContextUtils.getClassElement(classToCheck, context);
        return checkedClassEl != null;
    }
}
