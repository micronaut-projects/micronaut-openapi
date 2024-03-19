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
import java.io.Serial;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Nullable;
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
import io.micronaut.openapi.visitor.group.EndpointInfo;
import io.micronaut.openapi.visitor.group.EndpointGroupInfo;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static io.micronaut.openapi.visitor.ConfigUtils.endpointsConfiguration;
import static io.micronaut.openapi.visitor.ConfigUtils.getAdocProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getBooleanProperty;
import static io.micronaut.openapi.visitor.ConfigUtils.getConfigProperty;
import static io.micronaut.openapi.visitor.ConfigUtils.getEnv;
import static io.micronaut.openapi.visitor.ConfigUtils.getExpandableProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.getGroupProperties;
import static io.micronaut.openapi.visitor.ConfigUtils.isOpenApiEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.isSpecGenerationEnabled;
import static io.micronaut.openapi.visitor.ConfigUtils.readOpenApiConfigFile;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS;
import static io.micronaut.openapi.visitor.ContextUtils.addGeneratedResource;
import static io.micronaut.openapi.visitor.ContextUtils.info;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.FileUtils.EXT_JSON;
import static io.micronaut.openapi.visitor.FileUtils.EXT_YML;
import static io.micronaut.openapi.visitor.FileUtils.calcFinalFilename;
import static io.micronaut.openapi.visitor.FileUtils.getDefaultFilePath;
import static io.micronaut.openapi.visitor.FileUtils.getViewsDestDir;
import static io.micronaut.openapi.visitor.FileUtils.openApiSpecFile;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.ALL;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_JSON_FORMAT;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VIEWS_SPEC;
import static io.micronaut.openapi.visitor.OpenApiNormalizeUtils.findAndRemoveDuplicates;
import static io.micronaut.openapi.visitor.OpenApiNormalizeUtils.normalizeOpenApi;
import static io.micronaut.openapi.visitor.OpenApiNormalizeUtils.removeEmtpyComponents;
import static io.micronaut.openapi.visitor.SchemaUtils.copyOpenApi;
import static io.micronaut.openapi.visitor.SchemaUtils.getOperationOnPathItem;
import static io.micronaut.openapi.visitor.SchemaUtils.setOperationOnPathItem;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Visits the application class.
 *
 * @author graemerocher
 * @since 1.0
 */
public class OpenApiApplicationVisitor extends AbstractOpenApiVisitor implements TypeElementVisitor<Object, Object> {

    private ClassElement classElement;
    private int visitedElements = -1;

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
            if (ignore(element, context)) {
                return;
            }
            incrementVisitedElements(context);

            info("Generating OpenAPI Documentation", context);
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

            OpenAPI existing = ContextUtils.get(Utils.ATTR_OPENAPI, OpenAPI.class, context);
            if (existing != null) {
                if (openApi.getInfo() != null) {
                    existing.setInfo(openApi.getInfo());
                }
                copyOpenApi(existing, openApi);
            } else {
                ContextUtils.put(Utils.ATTR_OPENAPI, openApi, context);
            }

            if (Utils.isTestMode()) {
                Utils.resolveOpenApi(context);
            }

            classElement = element;
        } catch (Throwable t) {
            warn("Error with processing class:\n" + Utils.printStackTrace(t), context, classElement);
        }
    }

    private boolean ignore(ClassElement element, VisitorContext context) {

        return !element.isAnnotationPresent(OpenAPIDefinition.class)
            && !element.isAnnotationPresent(OpenAPIGroupInfo.class)
            && !element.isAnnotationPresent(OpenAPIGroupInfos.class);
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
                info("Merging Swagger OpenAPI YAML and JSON files from location: " + directory, context);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> FileUtils.isYaml(path.toString().toLowerCase()) || path.toString().toLowerCase().endsWith(EXT_JSON))) {
                    stream.forEach(path -> {
                        boolean isYaml = FileUtils.isYaml(path.toString().toLowerCase());
                        info("Reading Swagger OpenAPI " + (isYaml ? "YAML" : "JSON") + " file " + path.getFileName(), context);
                        OpenAPI parsedOpenApi = null;
                        try {
                            parsedOpenApi = (isYaml ? OpenApiUtils.getYamlMapper() : OpenApiUtils.getJsonMapper()).readValue(path.toFile(), OpenAPI.class);
                        } catch (IOException e) {
                            warn("Unable to read file " + path.getFileName() + ": " + e.getMessage(), context, element);
                        }
                        copyOpenApi(openAPI, parsedOpenApi);
                    });
                } catch (IOException e) {
                    warn("Unable to read  file from " + directory + ": " + e.getMessage(), context, element);
                }
            } else {
                warn(directory + " does not exist or is not a directory", context, element);
            }
        }
    }

    private OpenAPI readOpenApi(ClassElement element, VisitorContext context) {
        return element.findAnnotation(OpenAPIDefinition.class).flatMap(o -> {
            Optional<OpenAPI> result = toValue(o.getValues(), context, OpenAPI.class, null);
            result.ifPresent(openAPI -> {
                var securityRequirements = new ArrayList<io.swagger.v3.oas.models.security.SecurityRequirement>();
                for (var secRequirementAnn : o.getAnnotations("security", SecurityRequirement.class)) {
                    securityRequirements.add(ConvertUtils.mapToSecurityRequirement(secRequirementAnn));
                }
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
            if (CollectionUtils.isNotEmpty(openApiInfos)) {
                cfg.setSpecFile(openApiInfos.values().iterator().next().getSpecFilePath());
            }
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
            case "UPPER_CAMEL_CASE" ->
                (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.UPPER_CAMEL_CASE;
            case "SNAKE_CASE" ->
                (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.SNAKE_CASE;
            case "UPPER_SNAKE_CASE" ->
                (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.UPPER_SNAKE_CASE;
            case "LOWER_CASE" ->
                (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.LOWER_CASE;
            case "KEBAB_CASE" ->
                (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.KEBAB_CASE;
            case "LOWER_DOT_CASE" ->
                (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.LOWER_DOT_CASE;
            default -> null;
        };
    }

    private void applyPropertyNamingStrategy(OpenAPI openAPI, VisitorContext context) {
        final String namingStrategyName = getConfigProperty(MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, context);
        final PropertyNamingStrategies.NamingBase propertyNamingStrategy = fromName(namingStrategyName);
        if (propertyNamingStrategy != null) {
            info("Using " + namingStrategyName + " property naming strategy.", context);
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
        info("Applying server context path: " + serverContextPath + " to Paths.", context);
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
            info("Expanding properties: " + expandableProperties, context);
        }
        JsonNode root = resolvePlaceholders(OpenApiUtils.getYamlMapper().convertValue(openAPI, ObjectNode.class), s -> expandProperties(s, expandableProperties, context));
        return OpenApiUtils.getYamlMapper().convertValue(root, OpenAPI.class);
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
            String documentTitle = "OpenAPI";

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
                boolean isYaml = !(StringUtils.isNotEmpty(isJson) && isJson.equalsIgnoreCase(StringUtils.TRUE));

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

                    var titleAndFilename = calcFinalFilename(openApiInfo.getFilename(), openApiInfo, openApiInfos.size() == 1, ext, context);
                    documentTitle = titleAndFilename.getFirst();
                    openApiInfo.setFilename(titleAndFilename.getSecond());
                }

                writeYamlToFile(openApiInfos, documentTitle, context, isYaml);
            }

            generateViews(documentTitle, openApiInfos, context);

            visitedElements = visitedElements(context);
        } catch (Throwable t) {
            warn("Error:\n" + Utils.printStackTrace(t), context);
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
                    if (CollectionUtils.isNotEmpty(endpointInfo.getExcludedGroups()) && endpointInfo.getExcludedGroups().contains(endpointGroupInfo)) {
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
        io.swagger.v3.oas.models.Paths paths = openApi.getPaths();
        if (paths == null) {
            paths = new io.swagger.v3.oas.models.Paths();
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
                openApiCopy = OpenApiUtils.getJsonMapper().treeToValue(OpenApiUtils.getJsonMapper().valueToTree(openApi), OpenAPI.class);
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
                    String openApiJson = OpenApiUtils.getJsonMapper().writeValueAsString(openApi);
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
        findAndRemoveDuplicates(openApi);

        openApi = resolvePropertyPlaceHolders(openApi, context);

        return openApi;
    }

    private void generateViews(@Nullable String documentTitle, @Nullable Map<Pair<String, String>, OpenApiInfo> openApiInfos, VisitorContext context) {
        Path viewsDestDirs = getViewsDestDir(getDefaultFilePath("dummy" + System.nanoTime(), context), context);
        if (viewsDestDirs == null) {
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

            String swaggerFiles = "";
            if (openApiInfos != null) {
                swaggerFiles = openApiInfos.values().stream()
                    .map(OpenApiInfo::getSpecFilePath)
                    .collect(Collectors.joining(", ", "files ", ""));
            }

            warn("Unable to render swagger view: " + swaggerFiles + " - " + e.getMessage() + ".\n" + Utils.printStackTrace(e), context, classElement);
        }
    }

    private void writeYamlToFile(Map<Pair<String, String>, OpenApiInfo> openApiInfos, String documentTitle, VisitorContext context, boolean isYaml) {

        Path viewsDestDirs = null;
        var isAdocModuleInClassPath = false;
        var isGlobalAdocEnabled = getBooleanProperty(MICRONAUT_OPENAPI_ADOC_ENABLED, true, context);

        if (!Utils.isTestMode()) {
            try {
                var converterClass = Class.forName("io.micronaut.openapi.adoc.OpenApiToAdocConverter");
                isAdocModuleInClassPath = true;
            } catch (ClassNotFoundException e) {
                //
            }
        }

        for (OpenApiInfo openApiInfo : openApiInfos.values()) {
            Path specFile = openApiSpecFile(openApiInfo.getFilename(), context);
            try (Writer writer = getFileWriter(specFile)) {
                (isYaml ? OpenApiUtils.getYamlMapper() : OpenApiUtils.getJsonMapper()).writeValue(writer, openApiInfo.getOpenApi());
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

                    if (isAdocModuleInClassPath && isGlobalAdocEnabled && openApiInfo.isAdocEnabled()) {
                        var adocProperties = getAdocProperties(openApiInfo, openApiInfos.size() == 1, context);
                        AdocModule.convert(openApiInfo, adocProperties, context);
                    }
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
            throw new IOException("Swagger spec file location is not present");
        }
    }

    private void processEndpoints(VisitorContext context) {
        EndpointsConfiguration endpointsCfg = endpointsConfiguration(context);
        if (endpointsCfg.isEnabled() && CollectionUtils.isNotEmpty(endpointsCfg.getEndpoints())) {
            var visitor = new OpenApiEndpointVisitor(true);
            for (var endpoint : endpointsCfg.getEndpoints().values()) {
                ClassElement classEl = endpoint.getClassElement();
                if (classEl == null) {
                    continue;
                }
                ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_CLASS_TAGS, endpoint.getTags(), context);
                ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SERVERS, endpoint.getServers(), context);
                ContextUtils.put(MICRONAUT_INTERNAL_OPENAPI_ENDPOINT_SECURITY_REQUIREMENTS, endpoint.getSecurityRequirements(), context);
                visitor.visitClass(classEl, context);
                for (MethodElement methodEl : classEl.getEnclosedElements(ElementQuery.ALL_METHODS
                    .modifiers(mods -> !mods.contains(ElementModifier.STATIC) && !mods.contains(ElementModifier.PRIVATE))
                    .named(name -> !name.contains("$")))) {
                    visitor.visitMethod(methodEl, context);
                }

            }
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
