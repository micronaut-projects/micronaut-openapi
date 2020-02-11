/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;

import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.openapi.view.OpenApiViewConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.processing.SupportedOptions;

/**
 * Visits the application class.
 *
 * @author graemerocher
 * @since 1.0
 */
@Experimental
@SupportedOptions({
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

    private ClassElement classElement;

    private Path projectDirectory;
    private Properties openApiProperties = new Properties();

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        context.info("Generating OpenAPI Documentation");
        try {
            readOpenApiConfigFile(context);
        } catch (IOException e) {
            context.warn("Fail to read OpenAPI configuration file: " + e.getMessage(), null);
        }
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
        List<io.swagger.v3.oas.models.security.SecurityRequirement> securityRequirements = element.getAnnotationValuesByType(SecurityRequirement.class)
                .stream()
                .map(this::mapToSecurityRequirement)
                .collect(Collectors.toList());
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

        if (Boolean.getBoolean(ATTR_TEST_MODE)) {
            testReference = resolveOpenAPI(context);
        }

        this.classElement = element;
    }

    private String getConfigurationProperty(String key) {
        return System.getProperty(key, openApiProperties.getProperty(key));
    }

    /**
     * Merge the OpenAPI YAML files into one single file.
     *
     * @param element The element
     * @param context The visitor context
     * @param openAPI The {@link OpenAPI} object for the application
     */
    private void mergeAdditionalSwaggerFiles(ClassElement element, VisitorContext context, OpenAPI openAPI) {
        String additionalSwaggerFiles = getConfigurationProperty(MICRONAUT_OPENAPI_ADDITIONAL_FILES);
        if (StringUtils.isNotEmpty(additionalSwaggerFiles)) {
            Path directory = resolve(context, Paths.get(additionalSwaggerFiles));
            if (Files.isDirectory(directory)) {
                context.info("Merging Swagger OpenAPI YAML files from location :" + directory);
                try {
                    Files.newDirectoryStream(directory,
                            path -> path.toString().endsWith(".yml"))
                            .forEach(path -> {
                                context.info("Reading Swagger OpenAPI YAML file " + path.getFileName());
                                OpenAPI parsedOpenApi = null;
                                try {
                                    parsedOpenApi = yamlMapper.readValue(path.toFile(), OpenAPI.class);
                                } catch (IOException e) {
                                    context.warn("Unable to read file " + path.getFileName() + ": " + e.getMessage() , classElement);
                                }
                                copyOpenAPI(openAPI, parsedOpenApi);
                            });
                } catch (IOException e) {
                    context.warn("Unable to read  file from " + directory + ": " + e.getMessage() , classElement);
                }
            } else {
                context.warn(directory + " does not exist or is not a directory", classElement);
            }
        }
    }

    private Path resolve(VisitorContext context, Path path) {
        if (!path.isAbsolute()) {
            Path projectDir = projectDir(context);
            if (projectDir != null) {
                path = projectDir.resolve(path);
            }
        }
        return path;
    }

    private void readOpenApiConfigFile(VisitorContext context) throws IOException {
        String cfgFile = System.getProperty(MICRONAUT_OPENAPI_CONFIG_FILE, OPENAPI_CONFIG_FILE);
        if (StringUtils.isNotEmpty(cfgFile)) {
            Path cfg = resolve(context, Paths.get(cfgFile));
            if (Files.isReadable(cfg)) {
                try (Reader reader = Files.newBufferedReader(cfg)) {
                    openApiProperties.load(reader);
                }
            } else if (Files.exists(cfg)) {
                context.warn("Can not read configuration file: " + cfg, null);
            }
        }
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

    private <T, A extends Annotation> List<T> processOpenApiAnnotation(ClassElement element, VisitorContext context, Class<A> annotationType, Class<T> modelType, List<T> tagList) {
        List<AnnotationValue<A>> annotations = element.getAnnotationValuesByType(annotationType);
        if (CollectionUtils.isNotEmpty(annotations)) {
            if (CollectionUtils.isEmpty(tagList)) {
                tagList = new ArrayList<>();

            }
            for (AnnotationValue<A> tag : annotations) {
                JsonNode jsonNode;
                if (tag.getAnnotationName().equals(SecurityRequirement.class.getName()) && tag.getValues().size() > 0) {
                    Object name = tag.getValues().get("name");
                    Object scopes = Optional.ofNullable(tag.getValues().get("scopes")).orElse(new ArrayList<String>());
                    jsonNode = toJson(Collections.singletonMap((CharSequence) name, scopes), context);
                } else {
                    jsonNode = toJson(tag.getValues(), context);
                }
                try {
                    T t = treeToValue(jsonNode, modelType);
                    if (t != null) {
                        tagList.add(t);
                    }
                } catch (JsonProcessingException e) {
                    context.warn("Error reading OpenAPI" + annotationType + " annotation", element);
                }
            }
        }
        return tagList;
    }

    private OpenAPI readOpenAPI(ClassElement element, VisitorContext context) {
        return element.findAnnotation(OpenAPIDefinition.class).flatMap(o -> {
                    JsonNode jsonNode = toJson(o.getValues(), context);

                    try {
                        Optional<OpenAPI> result = Optional.of(treeToValue(jsonNode, OpenAPI.class));
                        result.ifPresent(openAPI -> {
                            List<io.swagger.v3.oas.models.security.SecurityRequirement> securityRequirements =
                                    o.getAnnotations("security", io.swagger.v3.oas.annotations.security.SecurityRequirement.class)
                                    .stream()
                                    .map(this::mapToSecurityRequirement)
                                    .collect(Collectors.toList());
                            openAPI.setSecurity(securityRequirements);
                        });
                        return result;
                    } catch (JsonProcessingException e) {
                        context.warn("Error reading Swagger OpenAPI for element [" + element + "]: " + e.getMessage(), element);
                        return Optional.empty();
                    }
                }).orElse(new OpenAPI());
    }

    private void renderViews(String title, String specFile, Path destinationDir, VisitorContext visitorContext) throws IOException {
        String viewSpecification = System.getProperty(MICRONAUT_OPENAPI_VIEWS_SPEC);
        if (viewSpecification != null) {
            OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(viewSpecification, openApiProperties);
            if (cfg.isEnabled()) {
                cfg.setTitle(title);
                cfg.setSpecFile(specFile);
                cfg.render(destinationDir.resolve("views"), visitorContext);
            }
        }
    }

    private static PropertyNamingStrategyBase fromName(String name) {
        if (name == null) {
            return null;
        }
        switch (name.toUpperCase()) {
        case "SNAKE_CASE": return (PropertyNamingStrategyBase) PropertyNamingStrategy.SNAKE_CASE;
        case "UPPER_CAMEL_CASE":  return (PropertyNamingStrategyBase) PropertyNamingStrategy.UPPER_CAMEL_CASE;
        case "LOWER_CAMEL_CASE":  return (PropertyNamingStrategyBase) PropertyNamingStrategy.LOWER_CAMEL_CASE;
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

    private Path projectDir(VisitorContext visitorContext) {
        if (projectDirectory != null) {
            return projectDirectory;
        }
        // let's find the projectDir
        Optional<GeneratedFile> dummyFile = visitorContext.visitGeneratedFile("dummy");
        if (dummyFile.isPresent()) {
            URI uri = dummyFile.get().toURI();
            // happens in tests 'mem:///CLASS_OUTPUT/META-INF/swagger/swagger.yml'
            if (uri.getScheme() != null && !uri.getScheme().equals("mem")) {
                // assume files are generated in 'build' dir
                Path dummy = Paths.get(uri).normalize();
                while (dummy != null) {
                    Path dummyFileName = dummy.getFileName();
                    if (dummyFileName != null && "build".equals(dummyFileName.toString())) {
                        projectDirectory = dummy.getParent();
                        break;
                    }
                    dummy = dummy.getParent();
                }
            }
        }
        return projectDirectory;
    }

    private Optional<Path> userDefinedSpecFile(VisitorContext visitorContext) {
        String targetFile = getConfigurationProperty(MICRONAUT_OPENAPI_TARGET_FILE);
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

    @Override
    public void finish(VisitorContext visitorContext) {
        if (classElement == null) {
            return;
        }

        Optional<OpenAPI> attr = visitorContext.get(ATTR_OPENAPI, OpenAPI.class);

        attr.ifPresent(openAPI -> {
            final String namingStrategyName = getConfigurationProperty(MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY);
            final PropertyNamingStrategyBase propertyNamingStrategy = fromName(namingStrategyName);
            if (propertyNamingStrategy != null) {
                visitorContext.info("Using " + namingStrategyName + " property naming strategy.");
                openAPI.getComponents().getSchemas().values().forEach(model -> {
                    Map<String, Schema> properties = model.getProperties();
                    if (properties == null) {
                        return;
                    }
                    Map<String, Schema> newProperties = properties.entrySet().stream()
                            .collect(Collectors.toMap(entry -> propertyNamingStrategy.translate(entry.getKey()),
                                    Map.Entry::getValue, (prop1, prop2) -> prop1, LinkedHashMap::new));
                    model.getProperties().clear();
                    model.setProperties(newProperties);
                });
            }
            String fileName = "swagger.yml";
            String documentTitle = "OpenAPI";

            Info info = openAPI.getInfo();
            if (info != null) {
                documentTitle = Optional.ofNullable(info.getTitle()).orElse(Environment.DEFAULT_NAME);
                documentTitle = documentTitle.toLowerCase().replace(' ', '-');
                String version = info.getVersion();
                if (version != null) {
                    documentTitle = documentTitle + '-' + version;
                }
                fileName = documentTitle + ".yml";
            }
            Optional<Path> specFile = openApiSpecFile(fileName, visitorContext);
            if (specFile.isPresent()) {
                Path specPath = specFile.get();
                try (Writer writer = Files.newBufferedWriter(specPath, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE)) {
                    visitorContext.info("Writing OpenAPI YAML to destination: " + specPath);
                    yamlMapper.writeValue(writer, openAPI);
                    renderViews(documentTitle, fileName, specPath.getParent(), visitorContext);
                } catch (Exception e) {
                    visitorContext.warn("Unable to generate swagger.yml: " + specPath + " - " + e.getMessage(), classElement);
                }
            }
        });
    }
}
