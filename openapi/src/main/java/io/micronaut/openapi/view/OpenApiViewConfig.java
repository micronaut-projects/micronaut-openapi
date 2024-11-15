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
package io.micronaut.openapi.view;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.ConfigUtils;
import io.micronaut.openapi.visitor.ContextUtils;
import io.micronaut.openapi.visitor.Pair;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static io.micronaut.openapi.visitor.ConfigUtils.getProjectPath;
import static io.micronaut.openapi.visitor.ContextUtils.addGeneratedResource;
import static io.micronaut.openapi.visitor.ContextUtils.info;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.FileUtils.readFile;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH;
import static io.micronaut.openapi.visitor.StringUtil.COMMA;
import static io.micronaut.openapi.visitor.StringUtil.DOLLAR;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;

/**
 * OpenApi view configuration for Swagger UI, ReDoc, OpenAPI Explorer and RapiDoc.
 * By default, no views are enabled.
 *
 * @author croudet
 * @see <a href="https://github.com/swagger-api/swagger-ui">Swagger UI</a>
 * @see <a href="https://github.com/Redocly/ReDoc">ReDoc</a>
 * @see <a href="https://github.com/rapi-doc/RapiDoc">RapiDoc</a>
 * @see <a href="https://github.com/Authress-Engineering/openapi-explorer">OpenAPI Explorer</a>
 */
public final class OpenApiViewConfig {

    public static final String DEFAULT_SPEC_MAPPING_PATH = "swagger";

    public static final String RESOURCE_DIR = "res";
    public static final String THEMES_DIR = "theme";
    public static final String TEMPLATES = "templates";
    public static final String TEMPLATES_RAPIPDF = "rapipdf";
    public static final String TEMPLATES_SWAGGER_UI = "swagger-ui";
    public static final String TEMPLATES_REDOC = "redoc";
    public static final String TEMPLATES_RAPIDOC = "rapidoc";
    public static final String TEMPLATES_OPENAPI_EXPLORER = "openapi-explorer";

    private static final String TEMPLATE_INDEX_HTML = "index.html";
    private static final String REDOC = "redoc";
    private static final String RAPIDOC = "rapidoc";
    private static final String SWAGGER_UI = "swagger-ui";
    private static final String OPENAPI_EXPLORER = "openapi-explorer";
    private static final String TEMPLATE_OAUTH_2_REDIRECT_HTML = "oauth2-redirect.html";

    private String mappingPath;
    private String title;
    private String specFile;
    private SwaggerUIConfig swaggerUIConfig;
    private RedocConfig redocConfig;
    private RapidocConfig rapidocConfig;
    private OpenApiExplorerConfig openApiExplorerConfig;
    private final Map<Pair<String, String>, OpenApiInfo> openApiInfos;

    /**
     * The Renderer types.
     */
    public enum RendererType {

        SWAGGER_UI(TEMPLATES_SWAGGER_UI),
        REDOC(TEMPLATES_REDOC),
        RAPIDOC(TEMPLATES_RAPIDOC),
        OPENAPI_EXPLORER(TEMPLATES_OPENAPI_EXPLORER),
        ;

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
        var result = new HashMap<String, String>();
        for (var prop : specification.split(COMMA)) {
            prop = prop.strip();
            if (prop.isEmpty()) {
                continue;
            }
            var keyValue = prop.split("=", 2);
            var key = keyValue[0].strip();
            var value = keyValue.length == 1 ? StringUtils.EMPTY_STRING : keyValue[1].strip();
            if (key.isEmpty()) {
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * Creates an OpenApiViewConfig form a String representation.
     *
     * @param specification A String representation of an OpenApiViewConfig.
     * @param openApiInfos Open API info objects.
     * @param openApiProperties The open api properties.
     * @param context Visitor context.
     *
     * @return An OpenApiViewConfig.
     */
    public static OpenApiViewConfig fromSpecification(String specification, Map<Pair<String, String>, OpenApiInfo> openApiInfos, Properties openApiProperties, VisitorContext context) {
        var openApiMap = new HashMap<String, String>(openApiProperties.size());
        openApiProperties.forEach((key, value) -> openApiMap.put((String) key, (String) value));
        openApiMap.putAll(parse(specification));
        var cfg = new OpenApiViewConfig(openApiInfos);
        RapiPDFConfig rapiPDFConfig = RapiPDFConfig.fromProperties(openApiMap, openApiInfos, context);
        if ("true".equals(openApiMap.getOrDefault("redoc.enabled", Boolean.FALSE.toString()))) {
            cfg.redocConfig = RedocConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.redocConfig.rapiPDFConfig = rapiPDFConfig;
        }
        if ("true".equals(openApiMap.getOrDefault("rapidoc.enabled", Boolean.FALSE.toString()))) {
            cfg.rapidocConfig = RapidocConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.rapidocConfig.rapiPDFConfig = rapiPDFConfig;
        }
        if ("true".equals(openApiMap.getOrDefault("openapi-explorer.enabled", Boolean.FALSE.toString()))) {
            cfg.openApiExplorerConfig = OpenApiExplorerConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.openApiExplorerConfig.rapiPDFConfig = rapiPDFConfig;
        }
        if ("true".equals(openApiMap.getOrDefault("swagger-ui.enabled", Boolean.FALSE.toString()))) {
            cfg.swaggerUIConfig = SwaggerUIConfig.fromProperties(openApiMap, openApiInfos, context);
            cfg.swaggerUIConfig.rapiPDFConfig = rapiPDFConfig;
        }
        cfg.mappingPath = openApiMap.getOrDefault("mapping.path", DEFAULT_SPEC_MAPPING_PATH);
        return cfg;
    }

    /**
     * Returns true when the generation of views is enabled.
     *
     * @return true when the generation of views is enabled.
     */
    public boolean isEnabled() {
        return redocConfig != null || rapidocConfig != null || swaggerUIConfig != null || openApiExplorerConfig != null;
    }

    /**
     * Generates the views given this configuration.
     *
     * @param outputDir The destination directory of the generated views.
     * @param context The visitor context
     *
     * @throws IOException When the generation fails.
     */
    public void render(Path outputDir, VisitorContext context) throws IOException {
        if (redocConfig != null) {
            copyResources(outputDir, context, REDOC, TEMPLATES_REDOC, redocConfig, redocConfig.rapiPDFConfig);
        }
        if (rapidocConfig != null) {
            copyResources(outputDir, context, RAPIDOC, TEMPLATES_RAPIDOC, rapidocConfig, rapidocConfig.rapiPDFConfig);
        }
        if (openApiExplorerConfig != null) {
            Path openapiExplorerDir = outputDir.resolve(OPENAPI_EXPLORER);
            render(openApiExplorerConfig, openapiExplorerDir, TEMPLATES + SLASH + TEMPLATES_OPENAPI_EXPLORER + SLASH + TEMPLATE_INDEX_HTML, context);
            copyResources(openApiExplorerConfig, openapiExplorerDir, TEMPLATES_OPENAPI_EXPLORER, openApiExplorerConfig.getResources(), context);
            if (openApiExplorerConfig.rapiPDFConfig.enabled) {
                copyResources(openApiExplorerConfig.rapiPDFConfig, openapiExplorerDir, TEMPLATES_RAPIPDF, openApiExplorerConfig.rapiPDFConfig.getResources(), context);
            }
        }
        if (swaggerUIConfig != null) {
            Path swaggerUiDir = copyResources(outputDir, context, SWAGGER_UI, TEMPLATES_SWAGGER_UI, swaggerUIConfig, swaggerUIConfig.rapiPDFConfig);
            if (SwaggerUIConfig.hasOauth2Option(swaggerUIConfig.options)) {
                render(swaggerUIConfig, swaggerUiDir, TEMPLATES + SLASH + TEMPLATES_SWAGGER_UI + SLASH + TEMPLATE_OAUTH_2_REDIRECT_HTML, context);
            }
            copySwaggerUiTheme(swaggerUIConfig, swaggerUiDir, TEMPLATES_SWAGGER_UI, context);
        }
    }

    private Path copyResources(@NonNull Path outputDir,
                               @NonNull VisitorContext context,
                               @NonNull String otherDir,
                               @NonNull String templates,
                               AbstractViewConfig viewConfig,
                               AbstractViewConfig rapidPDFConfig) throws IOException {
        Path dir = outputDir.resolve(otherDir);
        render(viewConfig, dir, TEMPLATES + SLASH + templates + SLASH + TEMPLATE_INDEX_HTML, context);
        copyResources(viewConfig, dir, templates, viewConfig.getResources(), context);
        if (rapidPDFConfig.isEnabled()) {
            copyResources(rapidPDFConfig, dir, TEMPLATES_RAPIPDF, rapidPDFConfig.getResources(), context);
        }
        return dir;
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
        try (var is = classLoader.getResourceAsStream(TEMPLATES + SLASH + templatesDir + SLASH + THEMES_DIR + SLASH + themeFileName)) {

            Files.copy(is, Paths.get(resDir.toString(), themeFileName), StandardCopyOption.REPLACE_EXISTING);
            Path file = resDir.resolve(themeFileName);
            if (context != null) {
                info("Writing OpenAPI View Resources to destination: " + file, context);
                var classesOutputPath = ContextUtils.getClassesOutputPath(context);
                if (classesOutputPath != null) {
                    // add relative path for the file, so that the micronaut-graal visitor knows about it
                    addGeneratedResource(classesOutputPath.relativize(file).toString(), context);
                }
            }
        } catch (Exception e) {
            warn("Can't copy resource: " + themeFileName, context);
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
                try (var is = classLoader.getResourceAsStream(TEMPLATES + SLASH + templateDir + SLASH + resource)) {

                    Files.copy(is, Paths.get(outputDir.toString(), resource), StandardCopyOption.REPLACE_EXISTING);
                    Path file = outputResDir.resolve(resource);

                    if (context != null) {
                        info("Writing OpenAPI View Resources to destination: " + file, context);
                        var classesOutputPath = ContextUtils.getClassesOutputPath(context);
                        if (classesOutputPath != null) {
                            // add relative path for the file, so that the micronaut-graal visitor knows about it
                            addGeneratedResource(classesOutputPath.relativize(file).toString(), context);
                        }
                    }
                } catch (Exception e) {
                    warn("Can't copy resource: " + resource, context);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String readTemplateFromClasspath(String templateName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (var in = classLoader.getResourceAsStream(templateName);
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
        ) {
            return readFile(reader);
        } catch (Exception e) {
            throw new IOException("Fail to load " + templateName, e);
        }
    }

    private String readTemplateFromCustomPath(String customPathStr, @Nullable VisitorContext context) throws IOException {
        String projectDir = StringUtils.EMPTY_STRING;
        Path projectPath = context != null ? getProjectPath(context) : null;
        if (projectPath != null) {
            projectDir = projectPath.toString().replace("\\\\", SLASH);
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
            var resourceLoader = new DefaultClassPathResourceLoader(getClass().getClassLoader());
            Optional<InputStream> inOpt = resourceLoader.getResourceAsStream(customPathStr);
            if (inOpt.isEmpty()) {
                throw new IOException("Fail to load " + customPathStr);
            }
            try (InputStream in = inOpt.get();
                 var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
            ) {
                return readFile(reader);
            } catch (IOException e) {
                throw new IOException("Fail to load " + customPathStr, e);
            }
        }

        Path templatePath = resolve(context, Paths.get(customPathStr));
        if (!Files.isReadable(templatePath)) {
            throw new IOException("Can't read file " + customPathStr);
        }
        try (BufferedReader reader = Files.newBufferedReader(templatePath)) {
            return readFile(reader);
        } catch (IOException e) {
            throw new IOException("Fail to load " + customPathStr, e);
        }
    }

    private void render(AbstractViewConfig cfg, Path outputDir, String templateName, @Nullable VisitorContext context) throws IOException {

        String template;
        if (StringUtils.isEmpty(cfg.templatePath)) {
            template = readTemplateFromClasspath(templateName);
        } else {
            template = readTemplateFromCustomPath(cfg.templatePath, context);
        }

        template = cfg.render(template, context);
        template = replacePlaceHolder(template, "specURL", getSpecURL(cfg, context), StringUtils.EMPTY_STRING);
        template = replacePlaceHolder(template, "title", title, StringUtils.EMPTY_STRING);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        String fileName = templateName.substring(templateName.lastIndexOf(SLASH) + 1);
        Path file = outputDir.resolve(fileName);
        info("Writing OpenAPI View to destination: " + file, context);
        var classesOutputPath = ContextUtils.getClassesOutputPath(context);
        if (classesOutputPath != null) {
            // add relative path for the file, so that the micronaut-graal visitor knows about it
            addGeneratedResource(classesOutputPath.relativize(file).toString(), context);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            writer.write(template);
        }
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
    public String getSpecURL(AbstractViewConfig cfg, @Nullable VisitorContext context) {

        if (cfg.specUrl != null) {
            return cfg.specUrl;
        }
        if (specFile == null) {
            return StringUtils.EMPTY_STRING;
        }

        // process micronaut.openapi.server.context.path
        String serverContextPath = ConfigUtils.getConfigProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, context);
        if (serverContextPath == null) {
            serverContextPath = StringUtils.EMPTY_STRING;
        }
        String finalUrl = serverContextPath.startsWith(SLASH) ? serverContextPath : SLASH + serverContextPath;
        if (!finalUrl.endsWith(SLASH)) {
            finalUrl += SLASH;
        }

        // process micronaut.server.context-path
        String contextPath = ConfigUtils.getServerContextPath(context);
        finalUrl += contextPath.startsWith(SLASH) ? contextPath.substring(1) : contextPath;
        if (!finalUrl.endsWith(SLASH)) {
            finalUrl += SLASH;
        }

        finalUrl = StringUtils.prependUri(finalUrl, StringUtils.prependUri(mappingPath, specFile));
        if (!finalUrl.startsWith(SLASH) && !finalUrl.startsWith(DOLLAR)) {
            finalUrl = SLASH + finalUrl;
        }
        return finalUrl;
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
        return template.replace("{{" + placeHolder + "}}", StringUtils.isNotEmpty(value) ? valuePrefix + value : StringUtils.EMPTY_STRING);
    }

    public SwaggerUIConfig getSwaggerUIConfig() {
        return swaggerUIConfig;
    }

    public RedocConfig getRedocConfig() {
        return redocConfig;
    }

    public RapidocConfig getRapidocConfig() {
        return rapidocConfig;
    }

    public OpenApiExplorerConfig getOpenApiExplorerConfig() {
        return openApiExplorerConfig;
    }
}
