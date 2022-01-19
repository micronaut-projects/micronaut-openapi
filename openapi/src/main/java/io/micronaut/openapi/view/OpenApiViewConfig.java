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

import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * OpenApi view configuration for Swagger-ui, ReDoc and RapiDoc.
 * By default no views are enabled.
 *
 * @see <a href="https://github.com/swagger-api/swagger-ui">Swagger-ui</a>
 * @see <a href="https://github.com/Rebilly/ReDoc">ReDoc</a>
 * @see <a href="https://github.com/mrin9/RapiDoc">RapiDoc</a>
 *
 * @author croudet
 */
public final class OpenApiViewConfig {

    private static final String TEMPLATES = "templates";
    private static final String TEMPLATES_SWAGGER_UI = "swagger-ui";
    private static final String TEMPLATES_REDOC = "redoc";
    private static final String TEMPLATES_RAPIDOC = "rapidoc";
    private static final String TEMPLATE_INDEX_HTML = "index.html";
    private static final String REDOC = "redoc";
    private static final String RAPIDOC = "rapidoc";
    private static final String SWAGGER_UI = "swagger-ui";
    private static final String TEMPLATE_OAUTH_2_REDIRECT_HTML = "oauth2-redirect.html";
    private static final String SLASH = "/";

    private String mappingPath;
    private String title;
    private String specFile;
    private String serverContextPath = "";
    private SwaggerUIConfig swaggerUIConfig;
    private RedocConfig redocConfig;
    private RapidocConfig rapidocConfig;

    /**
     * The Renderer types.
     */
    enum RendererType {
        SWAGGER_UI, REDOC, RAPIDOC
    }

    private OpenApiViewConfig() {
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
     * @param specification     A String representation of an OpenApiViewConfig.
     * @param openApiProperties The open api properties.
     * @return An OpenApiViewConfig.
     */
    public static OpenApiViewConfig fromSpecification(String specification, Properties openApiProperties) {
        Map<String, String> openApiMap = new HashMap<>(openApiProperties.size());
        openApiProperties.forEach((key, value) -> openApiMap.put((String) key, (String) value));
        openApiMap.putAll(parse(specification));
        OpenApiViewConfig cfg = new OpenApiViewConfig();
        RapiPDFConfig rapiPDFConfig = RapiPDFConfig.fromProperties(openApiMap);
        if ("true".equals(openApiMap.getOrDefault("redoc.enabled", Boolean.FALSE.toString()))) {
            cfg.redocConfig = RedocConfig.fromProperties(openApiMap);
            cfg.redocConfig.rapiPDFConfig = rapiPDFConfig;
        }
        if ("true".equals(openApiMap.getOrDefault("rapidoc.enabled", Boolean.FALSE.toString()))) {
            cfg.rapidocConfig = RapidocConfig.fromProperties(openApiMap);
            cfg.rapidocConfig.rapiPDFConfig = rapiPDFConfig;
        }
        if ("true".equals(openApiMap.getOrDefault("swagger-ui.enabled", Boolean.FALSE.toString()))) {
            cfg.swaggerUIConfig = SwaggerUIConfig.fromProperties(openApiMap);
            cfg.swaggerUIConfig.rapiPDFConfig = rapiPDFConfig;
        }
        cfg.mappingPath = openApiMap.getOrDefault("mapping.path", "swagger");
        return cfg;
    }

    /**
     * Returns true when the generation of views is enabled.
     * @return true when the generation of views is enabled.
     */
    public boolean isEnabled() {
        return redocConfig != null || rapidocConfig != null || swaggerUIConfig != null;
    }

    /**
     * Generates the views given this configuration.
     *
     * @param outputDir      The destination directory of the generated views.
     * @param visitorContext The visitor context
     * @throws IOException When the generation fails.
     */
    public void render(Path outputDir, VisitorContext visitorContext) throws IOException {
        if (redocConfig != null) {
            render(redocConfig, outputDir.resolve(REDOC), TEMPLATES + SLASH + TEMPLATES_REDOC + SLASH + TEMPLATE_INDEX_HTML, visitorContext);
        }
        if (rapidocConfig != null) {
            render(rapidocConfig, outputDir.resolve(RAPIDOC), TEMPLATES + SLASH + TEMPLATES_RAPIDOC + SLASH + TEMPLATE_INDEX_HTML, visitorContext);
        }
        if (swaggerUIConfig != null) {
            Path dir = outputDir.resolve(SWAGGER_UI);
            render(swaggerUIConfig, dir, TEMPLATES + SLASH + TEMPLATES_SWAGGER_UI + SLASH + TEMPLATE_INDEX_HTML, visitorContext);
            if (SwaggerUIConfig.hasOauth2Option(swaggerUIConfig.options)) {
                render(swaggerUIConfig, dir, TEMPLATES + SLASH + TEMPLATES_SWAGGER_UI + SLASH + TEMPLATE_OAUTH_2_REDIRECT_HTML, visitorContext);
            }
        }
    }
    
    private String readTemplateFromClasspath(String templateName) throws IOException {
        StringBuilder buf = new StringBuilder(1024);
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(templateName);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                buf.append(line).append('\n');
            }
            return buf.toString();
        } catch (Exception e) {
            throw new IOException("Fail to load " + templateName, e);
        }
    }

    private void render(Renderer renderer, Path outputDir, String templateName, VisitorContext visitorContext) throws IOException {
        String template = readTemplateFromClasspath(templateName);
        template = renderer.render(template);
        template = replacePlaceHolder(template, "specURL", getSpecURL(), "");
        template = replacePlaceHolder(template, "title", getTitle(), "");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        String fileName = templateName.substring(templateName.lastIndexOf(SLASH) + 1);
        Path file = outputDir.resolve(fileName);
        if (visitorContext != null) {
            visitorContext.info("Writing OpenAPI View to destination: " + file);
            visitorContext.getClassesOutputPath().ifPresent(path ->
                // add relative path for the file, so that the micronaut-graal visitor knows about it
                visitorContext.addGeneratedResource(path.relativize(file).toString()));
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(template);
        }
    }

    /**
     * Sets the server context path.
     * @param contextPath The server context path.
     */
    public void setServerContextPath(String contextPath) {
        this.serverContextPath = contextPath == null ? "" : contextPath;
    }

    /**
     * Returns the title for the generated views.
     * @return A title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title for the generated views.
     * @param title A title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the relative openApi specification url path.
     * @return A path.
     */
    public String getSpecURL() {
        return StringUtils.prependUri(serverContextPath, StringUtils.prependUri(mappingPath, specFile));
    }

    /**
     * Sets the generated openApi specification file name.
     * @param specFile The openApi specification file name.
     */
    public void setSpecFile(String specFile) {
        this.specFile = specFile;
    }

    @Override
    public String toString() {
        return new StringBuilder(100).append("OpenApiConfig [swaggerUIConfig=").append(swaggerUIConfig)
                .append(", reDocConfig=").append(redocConfig).append(", rapiDocConfig=").append(rapidocConfig)
                .append(']').toString();
    }

    /**
     * Replaces placeholders in the template.
     * @param template A template.
     * @param placeHolder The placeholder to replace.
     * @param value The value that will replace the placeholder.
     * @param valuePrefix A prefix.
     * @return The updated template.
     */
    static String replacePlaceHolder(String template, String placeHolder, String value, String valuePrefix) {
        if (value == null || value.isEmpty()) {
            return template.replace("{{" + placeHolder + "}}", "");
        } else {
            return template.replace("{{" + placeHolder + "}}", valuePrefix + value);
        }
    }
}
