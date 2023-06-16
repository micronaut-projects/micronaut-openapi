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
package io.micronaut.openapi.view;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.OpenApiApplicationVisitor;
import io.micronaut.openapi.visitor.Pair;
import io.micronaut.openapi.visitor.Utils;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.MICRONAUT_SERVER_CONTEXT_PATH;

/**
 * OpenApi view configuration for Swagger-ui, ReDoc and RapiDoc.
 * By default, no views are enabled.
 *
 * @author croudet
 * @see <a href="https://github.com/swagger-api/swagger-ui">Swagger-ui</a>
 * @see <a href="https://github.com/Rebilly/ReDoc">ReDoc</a>
 * @see <a href="https://github.com/mrin9/RapiDoc">RapiDoc</a>
 */
public final class OpenApiViewConfig {

    public static final String RESOURCE_DIR = "res";
    public static final String THEMES_DIR = "theme";
    public static final String TEMPLATES = "templates";
    public static final String TEMPLATES_RAPIPDF = "rapipdf";
    public static final String TEMPLATES_SWAGGER_UI = "swagger-ui";
    public static final String TEMPLATES_REDOC = "redoc";
    public static final String TEMPLATES_RAPIDOC = "rapidoc";
    public static final String SLASH = "/";

    private static final String TEMPLATE_INDEX_HTML = "index.html";
    private static final String REDOC = "redoc";
    private static final String RAPIDOC = "rapidoc";
    private static final String SWAGGER_UI = "swagger-ui";
    private static final String TEMPLATE_OAUTH_2_REDIRECT_HTML = "oauth2-redirect.html";

    private String mappingPath;
    private String title;
    private String specFile;
    private String serverContextPath = "";
    private SwaggerUIConfig swaggerUIConfig;
    private RedocConfig redocConfig;
    private RapidocConfig rapidocConfig;
    private final Map<Pair<String, String>, OpenApiInfo> openApiInfos;

    /**
     * The Renderer types.
     */
    enum RendererType {

        SWAGGER_UI(TEMPLATES_SWAGGER_UI),
        REDOC(TEMPLATES_REDOC),
        RAPIDOC(TEMPLATES_RAPIDOC);

        private final String templatePath;

        RendererType(String templatePath) {
            this.templatePath = templatePath;
        }

        public String getTemplatePath() {
            return templatePath;
        }
    }

    private OpenApiViewConfig(Map<Pair<String, String>, OpenApiInfo> openApiInfos) {
        this.openApiInfos = openApiInfos;
    }

    /**
     * Parse the string representation.
     */
    private static Map<String, String> parse(String specification) {
        if (specification == null) {
            return Collections.emptyMap();
        }
        return Arrays.stream(specification.split(",")).map(String::trim).filter(s -> !s.isEmpty())
            .map(s -> s.split("=")).filter(keyValue -> keyValue.length == 2).peek(keyValue -> {
                keyValue[0] = keyValue[0].trim();
                keyValue[1] = keyValue[1].trim();
            }).filter(keyValue -> !keyValue[0].isEmpty() && !keyValue[1].isEmpty())
            .collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1]));
    }

    /**
     * Creates an OpenApiViewConfig form a String representation.
     *
     * @param specification A String representation of an OpenApiViewConfig.
     * @param openApiProperties The open api properties.
     * @param context Visitor context.
     *
     * @return An OpenApiViewConfig.
     */
    public static OpenApiViewConfig fromSpecification(String specification, Map<Pair<String, String>, OpenApiInfo> openApiInfos, Properties openApiProperties, VisitorContext context) {
        Map<String, String> openApiMap = new HashMap<>(openApiProperties.size());
        openApiProperties.forEach((key, value) -> openApiMap.put((String) key, (String) value));
        openApiMap.putAll(parse(specification));
        OpenApiViewConfig cfg = new OpenApiViewConfig(openApiInfos);
        RapiPDFConfig rapiPDFConfig = RapiPDFConfig.fromProperties(openApiMap, openApiInfos, context);
        if ("true".equals(openApiMap.getOrDefault("redoc.enabled", Boolean.FALSE.toString()))) {
            cfg.redocConfig = RedocConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.redocConfig.rapiPDFConfig = rapiPDFConfig;
        }
        if ("true".equals(openApiMap.getOrDefault("rapidoc.enabled", Boolean.FALSE.toString()))) {
            cfg.rapidocConfig = RapidocConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.rapidocConfig.rapiPDFConfig = rapiPDFConfig;
        }
//        if ("true".equals(openApiMap.getOrDefault("swagger-ui.enabled", Boolean.FALSE.toString()))) {
            cfg.swaggerUIConfig = SwaggerUIConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.swaggerUIConfig.rapiPDFConfig = rapiPDFConfig;
//        }
        cfg.mappingPath = openApiMap.getOrDefault("mapping.path", "swagger");
        return cfg;
    }

    /**
     * Returns true when the generation of views is enabled.
     *
     * @return true when the generation of views is enabled.
     */
    public boolean isEnabled() {
        return redocConfig != null || rapidocConfig != null || swaggerUIConfig != null;
    }

    /**
     * Generates the views given this configuration.
     *
     * @param outputDir The destination directory of the generated views.
     * @param visitorContext The visitor context
     *
     * @throws IOException When the generation fails.
     */
    public void render(Path outputDir, VisitorContext visitorContext) throws IOException {
        if (redocConfig != null) {
            Path redocDir = outputDir.resolve(REDOC);
            render(redocConfig, redocDir, TEMPLATES + SLASH + TEMPLATES_REDOC + SLASH + TEMPLATE_INDEX_HTML, visitorContext);
            copyResources(redocConfig, redocDir, TEMPLATES_REDOC, redocConfig.getResources(), visitorContext);
            if (redocConfig.rapiPDFConfig.enabled) {
                copyResources(redocConfig.rapiPDFConfig, redocDir, TEMPLATES_RAPIPDF, redocConfig.rapiPDFConfig.getResources(), visitorContext);
            }
        }
        if (rapidocConfig != null) {
            Path rapidocDir = outputDir.resolve(RAPIDOC);
            render(rapidocConfig, rapidocDir, TEMPLATES + SLASH + TEMPLATES_RAPIDOC + SLASH + TEMPLATE_INDEX_HTML, visitorContext);
            copyResources(rapidocConfig, rapidocDir, TEMPLATES_RAPIDOC, rapidocConfig.getResources(), visitorContext);
            if (rapidocConfig.rapiPDFConfig.enabled) {
                copyResources(rapidocConfig.rapiPDFConfig, rapidocDir, TEMPLATES_RAPIPDF, rapidocConfig.rapiPDFConfig.getResources(), visitorContext);
            }
        }
        if (swaggerUIConfig != null) {
            Path swaggerUiDir = outputDir.resolve(SWAGGER_UI);
            render(swaggerUIConfig, swaggerUiDir, TEMPLATES + SLASH + TEMPLATES_SWAGGER_UI + SLASH + TEMPLATE_INDEX_HTML, visitorContext);
            if (SwaggerUIConfig.hasOauth2Option(swaggerUIConfig.options)) {
                render(swaggerUIConfig, swaggerUiDir, TEMPLATES + SLASH + TEMPLATES_SWAGGER_UI + SLASH + TEMPLATE_OAUTH_2_REDIRECT_HTML, visitorContext);
            }
            copyResources(swaggerUIConfig, swaggerUiDir, TEMPLATES_SWAGGER_UI, swaggerUIConfig.getResources(), visitorContext);
            if (swaggerUIConfig.rapiPDFConfig.enabled) {
                copyResources(swaggerUIConfig.rapiPDFConfig, swaggerUiDir, TEMPLATES_RAPIPDF, swaggerUIConfig.rapiPDFConfig.getResources(), visitorContext);
            }
            copySwaggerUiTheme(swaggerUIConfig, swaggerUiDir, TEMPLATES_SWAGGER_UI, visitorContext);
        }
    }

    private void copySwaggerUiTheme(SwaggerUIConfig cfg, Path outputDir, String templatesDir, VisitorContext context) throws IOException {

        if (!cfg.copyTheme) {
            return;
        }

        String themeFileName = cfg.theme.getCss() + ".css";

        Path resDir = outputDir.resolve(RESOURCE_DIR);
        if (!Files.exists(resDir)) {
            Files.createDirectories(resDir);
        }

        ClassLoader classLoader = getClass().getClassLoader();
        try {
            InputStream is = classLoader.getResourceAsStream(TEMPLATES + SLASH + templatesDir + SLASH + THEMES_DIR + SLASH + themeFileName);

            Files.copy(is, Paths.get(resDir.toString(), themeFileName), StandardCopyOption.REPLACE_EXISTING);
            Path file = resDir.resolve(themeFileName);
            if (context != null) {
                context.info("Writing OpenAPI View Resources to destination: " + file);
                context.getClassesOutputPath().ifPresent(path -> {
                    // add relative path for the file, so that the micronaut-graal visitor knows about it
                    context.addGeneratedResource(path.relativize(file).toString());
                });
            }
        } catch (Exception e) {
            if (context != null) {
                context.warn("Can't copy resource: " + themeFileName, null);
            }
            throw new RuntimeException(e);
        }
    }

    private void copyResources(AbstractViewConfig cfg, Path outputDir, String templateDir, List<String> resources, VisitorContext context) throws IOException {
        if (!cfg.copyResources) {
            return;
        }

        ClassLoader classLoader = getClass().getClassLoader();

        Path outputResDir = outputDir.resolve(RESOURCE_DIR);
        if (!Files.exists(outputResDir)) {
            Files.createDirectories(outputResDir);
        }

        if (CollectionUtils.isNotEmpty(resources)) {
            for (String resource : resources) {
                try {
                    InputStream is = classLoader.getResourceAsStream(TEMPLATES + SLASH + templateDir + SLASH + resource);
                    Files.copy(is, Paths.get(outputDir.toString(), resource), StandardCopyOption.REPLACE_EXISTING);
                    Path file = outputResDir.resolve(resource);

                    if (context != null) {
                        context.info("Writing OpenAPI View Resources to destination: " + file);
                        context.getClassesOutputPath().ifPresent(path -> {
                            // add relative path for the file, so that the micronaut-graal visitor knows about it
                            context.addGeneratedResource(path.relativize(file).toString());
                        });
                    }
                } catch (Exception e) {
                    if (context != null) {
                        context.warn("Can't copy resource: " + resource, null);
                    }
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String readTemplateFromClasspath(String templateName) throws IOException {
        StringBuilder buf = new StringBuilder(1024);
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(templateName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
        ) {
            return readFile(reader);
        } catch (Exception e) {
            throw new IOException("Fail to load " + templateName, e);
        }
    }

    private String readTemplateFromCustomPath(String customPathStr, VisitorContext context) throws IOException {
        String projectDir = StringUtils.EMPTY_STRING;
        Path projectPath = Utils.getProjectPath(context);
        if (projectPath != null) {
            projectDir = projectPath.toString().replaceAll("\\\\", "/");
        }
        if (customPathStr.startsWith("project:")) {
            customPathStr = customPathStr.replace("project:", projectDir);
        } else if (!customPathStr.startsWith("file:") && !customPathStr.startsWith("classpath:")) {
            if (!projectDir.endsWith(SLASH)) {
                projectDir += SLASH;
            }
            if (customPathStr.startsWith(SLASH)) {
                customPathStr = customPathStr.substring(1);
            }
            customPathStr = projectDir + customPathStr;
        } else if (customPathStr.startsWith("file:")) {
            customPathStr = customPathStr.substring(5);
        } else if (customPathStr.startsWith("classpath:")) {
            ClassPathResourceLoader resourceLoader = new DefaultClassPathResourceLoader(getClass().getClassLoader());
            Optional<InputStream> inOpt = resourceLoader.getResourceAsStream(customPathStr);
            if (!inOpt.isPresent()) {
                throw new IOException("Fail to load " + customPathStr);
            }
            try (InputStream in = inOpt.get();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
            ) {
                return readFile(reader);
            } catch (IOException e) {
                throw new IOException("Fail to load " + customPathStr, e);
            }
        }

        Path templatePath = OpenApiApplicationVisitor.resolve(context, Paths.get(customPathStr));
        if (!Files.isReadable(templatePath)) {
            throw new IOException("Can't read file " + customPathStr);
        }
        try (BufferedReader reader = Files.newBufferedReader(templatePath)) {
            return readFile(reader);
        } catch (IOException e) {
            throw new IOException("Fail to load " + customPathStr, e);
        }
    }

    private String readFile(BufferedReader reader) throws IOException {
        StringBuilder buf = new StringBuilder(1024);
        String line;
        while ((line = reader.readLine()) != null) {
            buf.append(line).append('\n');
        }
        return buf.toString();
    }

    private void render(AbstractViewConfig cfg, Path outputDir, String templateName, VisitorContext context) throws IOException {

        String template;
        if (StringUtils.isEmpty(cfg.templatePath)) {
            template = readTemplateFromClasspath(templateName);
        } else {
            template = readTemplateFromCustomPath(cfg.templatePath, context);
        }

        template = cfg.render(template, context);
        template = replacePlaceHolder(template, "specURL", getSpecURL(cfg, context), "");
        template = replacePlaceHolder(template, "title", getTitle(), "");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        String fileName = templateName.substring(templateName.lastIndexOf(SLASH) + 1);
        Path file = outputDir.resolve(fileName);
        if (context != null) {
            context.info("Writing OpenAPI View to destination: " + file);
            context.getClassesOutputPath().ifPresent(path -> {
                // add relative path for the file, so that the micronaut-graal visitor knows about it
                context.addGeneratedResource(path.relativize(file).toString());
            });
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            writer.write(template);
        }
    }

    /**
     * Sets the server context path.
     *
     * @param contextPath The server context path.
     */
    public void setServerContextPath(String contextPath) {
        serverContextPath = contextPath == null ? StringUtils.EMPTY_STRING : contextPath;
    }

    /**
     * Returns the title for the generated views.
     *
     * @return A title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title for the generated views.
     *
     * @param title A title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the relative openApi specification url path.
     *
     * @param cfg view config.
     * @param context Visitor context.
     *
     * @return A path.
     */
    public String getSpecURL(AbstractViewConfig cfg, VisitorContext context) {

        if (cfg.specUrl != null) {
            return cfg.specUrl;
        }

        String specUrl = StringUtils.prependUri(serverContextPath, StringUtils.prependUri(mappingPath, specFile));
        if (StringUtils.isEmpty(serverContextPath)) {
            String contextPath = OpenApiApplicationVisitor.getConfigurationProperty(MICRONAUT_SERVER_CONTEXT_PATH, context);
            if (contextPath == null) {
                contextPath = StringUtils.EMPTY_STRING;
            }
            if (!contextPath.startsWith("/") && !contextPath.startsWith("$")) {
                contextPath = "/" + contextPath;
            }
            if (!contextPath.endsWith("/")) {
                contextPath += "/";
            }
            if (specUrl.startsWith("/")) {
                specUrl = specUrl.substring(1);
            }

            specUrl = contextPath + specUrl;
        }

        return specUrl;
    }

    /**
     * Sets the generated openApi specification file name.
     *
     * @param specFile The openApi specification file name.
     */
    public void setSpecFile(String specFile) {
        this.specFile = specFile;
    }

    /**
     * Replaces placeholders in the template.
     *
     * @param template A template.
     * @param placeHolder The placeholder to replace.
     * @param value The value that will replace the placeholder.
     * @param valuePrefix A prefix.
     *
     * @return The updated template.
     */
    static String replacePlaceHolder(String template, String placeHolder, String value, String valuePrefix) {
        if (StringUtils.isEmpty(value)) {
            return template.replace("{{" + placeHolder + "}}", "");
        } else {
            return template.replace("{{" + placeHolder + "}}", valuePrefix + value);
        }
    }
}
