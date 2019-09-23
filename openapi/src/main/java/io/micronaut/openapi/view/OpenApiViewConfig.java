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
package io.micronaut.openapi.view;

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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import io.micronaut.inject.visitor.VisitorContext;

/**
 * OpenApi view configuration for Swagger-ui, ReDoc and RapiDoc.
 * By default no views are enabled.
 *
 * Here are the properties available to configure the views.
 * <ul>
 *   <li>{@code mapping.path=[String]}: The path from where the swagger specification will be served by the http server. Default is 'swagger'.</li>
 *   <li>{@code redoc.enabled=[boolean]}: When 'true' the redoc view is generated.</li>
 *   <li>{@code rapidoc.enabled=[boolean]}: When 'true' the rapidoc view is generated.</li>
 *   <li>{@code swagger-ui.enabled=[boolean]}: When 'true' the swagger-ui view is generated.</li>
 *   <li>{@code redoc.version=[String]}: The version of redoc to use.</li>
 *   <li>{@code rapidoc.version=[String]}: The version of rapidoc to use.</li>
 *   <li>{@code rapidoc.layout=[row | column]}: The layout of rapidoc to use.</li>
 *   <li>{@code rapidoc.theme=[dark | light]}: The theme of rapidoc to use.</li>
 *   <li>{@code swagger-ui.version=[String]}: The version of swagger-ui to use.</li>
 *   <li>{@code swagger-ui.layout=[String]}: The layout of swagger-ui to use. Defaults is 'StandaloneLayout'.</li>
 *   <li>{@code swagger-ui.theme=[DEFAULT | MATERIAL | FEELING_BLUE | FLATTOP | MONOKAI | MUTED | NEWSPAPER | OUTLINE]}: The theme of swagger-ui to use. These are case insensitive.</li>
 *   <li>{@code swagger-ui.deep-linking=[true | false]}: The deep-linking flag of swagger-ui to use. Default is 'true'.</li>
 * </ul>
 * You need to set a System property to specify them:
 * {@code -Dmicronaut.openapi.views.spec=swagger-ui.enabled=true,swagger-ui.theme=flattop}
 *
 * @see <a href="https://github.com/swagger-api/swagger-ui">Swagger-ui</a>
 * @see <a href="https://github.com/Rebilly/ReDoc">ReDoc</a>
 * @see <a href="https://github.com/mrin9/RapiDoc">RapiDoc</a>
 *
 * @author croudet
 */
public final class OpenApiViewConfig {
    private String mappingPath;
    private String title;
    private String specFile;
    private SwaggerUIConfig swaggerUIConfig;
    private RedocConfig redocConfig;
    private RapidocConfig rapidocConfig;

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
                    keyValue[0] = keyValue[0].trim().toLowerCase(Locale.US);
                    keyValue[1] = keyValue[1].trim();
                }).filter(keyValue -> !keyValue[0].isEmpty() && !keyValue[1].isEmpty())
                .collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1]));
    }

    /**
     * Creates an OpenApiViewConfig form a String representation.
     * @param specification A String representation of an OpenApiViewConfig.
     * @return An OpenApiViewConfig.
     */
    public static OpenApiViewConfig fromSpecification(String specification) {
        Map<String, String> properties = parse(specification);
        OpenApiViewConfig cfg = new OpenApiViewConfig();
        if ("true".equals(properties.getOrDefault("redoc.enabled", Boolean.FALSE.toString()))) {
            cfg.redocConfig = RedocConfig.fromProperties(properties);
        }
        if ("true".equals(properties.getOrDefault("rapidoc.enabled", Boolean.FALSE.toString()))) {
            cfg.rapidocConfig = RapidocConfig.fromProperties(properties);
        }
        if ("true".equals(properties.getOrDefault("swagger-ui.enabled", Boolean.FALSE.toString()))) {
            cfg.swaggerUIConfig = SwaggerUIConfig.fromProperties(properties);
        }
        cfg.mappingPath = properties.getOrDefault("mapping.path", "swagger");
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
     * @param outputDir The destination directory of the generated views.
     * @param visitorContext 
     * @throws IOException When the generation fails.
     */
    public void render(Path outputDir, VisitorContext visitorContext) throws IOException {
        if (redocConfig != null) {
            render(redocConfig, outputDir.resolve("redoc"), "templates/redoc/index.html", visitorContext);
        }
        if (rapidocConfig != null) {
            render(rapidocConfig, outputDir.resolve("rapidoc"), "templates/rapidoc/index.html", visitorContext);
        }
        if (swaggerUIConfig != null) {
            render(swaggerUIConfig, outputDir.resolve("swagger-ui"), "templates/swagger-ui/index.html", visitorContext);
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
        template = replacePlaceHolder(template, "specURL", getSpecURL(), "");
        template = replacePlaceHolder(template, "title", getTitle(), "");
        template = renderer.render(template);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        Path file = outputDir.resolve("index.html");
        if (visitorContext != null) {
          visitorContext.info("Writing OpenAPI View to destination: " + file);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(template);
        }
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
        return '/' + mappingPath + '/' + specFile;
    }

    /**
     * Sets the generated openApi specification file name.
     * @param specFile The openApi specification file name.
     */
    public void setSpecFile(String specFile) {
        this.specFile = specFile;
    }

    /**
     * Returns the rapidoc config.
     * @return A RapidocConfig.
     */
    public RapidocConfig getRapidoc() {
        return rapidocConfig;
    }

    /**
     * Returns the redoc config.
     * @return A RedocConfig.
     */
    public RedocConfig getRedoc() {
        return redocConfig;
    }

    /**
     * Returns the swagger-ui config.
     * @return A SwaggerUIConfig.
     */
    public SwaggerUIConfig getSwaggerUi() {
        return swaggerUIConfig;
    }

    @Override
    public String toString() {
        return new StringBuilder(100).append("OpenApiConfig [swaggerUIConfig=").append(swaggerUIConfig)
                .append(", reDocConfig=").append(redocConfig).append(", rapiDocConfig=").append(rapidocConfig)
                .append(']').toString();
    }

    private static String replacePlaceHolder(String template, String placeHolder, String value, String valuePrefix) {
        if (value != null && ! value.isEmpty()) {
            return template.replace("{{" + placeHolder + "}}", valuePrefix + value);
        } else {
            return template.replace("{{" + placeHolder + "}}", "");
        }
    }

    /**
     * Basic interface to replace placeHolder with values.
     * @author croudet
     */
    private static interface Renderer {

        /**
         * Replaces placeHolder in template with values.
         * @param template The template to process.
         * @return A template.
         */
        String render(String template);
    }

    /**
     * RapiDoc configuration.
     *
     * Currently only the version, layout and theme can be set.
     *
     * @author croudet
     */
    public static final class RapidocConfig implements Renderer {
        private String version = "";
        private Theme theme = Theme.DARK;
        private Layout layout = Layout.ROW;

        /**
         * Rapidoc themes.
         * @author croudet
         */
        enum Theme {
            LIGHT, DARK;

            @Override
            public String toString() {
                return this.name().toLowerCase(Locale.US);
            }
        }

        /**
         * Rapidoc layouts.
         * @author croudet
         */
        enum Layout {
            COLUMN, ROW;

            @Override
            public String toString() {
                return this.name().toLowerCase(Locale.US);
            }
        }

        private RapidocConfig() {
        }

        /**
         * Builds a RapidocConfig given a set of properties.
         * @param properties A set of properties.
         * @return A RapidocConfig.
         */
        static RapidocConfig fromProperties(Map<String, String> properties) {
            RapidocConfig cfg = new RapidocConfig();
            cfg.version = properties.getOrDefault("rapidoc.version", cfg.version);
            cfg.layout = Layout
                    .valueOf(properties.getOrDefault("rapidoc.layout", cfg.layout.name()).toUpperCase(Locale.US));
            cfg.theme = Theme
                    .valueOf(properties.getOrDefault("rapidoc.theme", cfg.theme.name()).toUpperCase(Locale.US));
            return cfg;
        }

        @Override
        public String render(String template) {
            template = replacePlaceHolder(template, "rapidoc.version", version, "@");
            template = replacePlaceHolder(template, "rapidoc.layout", getLayout().toString(), "");
            template = replacePlaceHolder(template, "rapidoc.theme", getTheme().toString(), "");
            return template;
        }

        /**
         * Returns the theme.
         * @return A Theme.
         */
        public Theme getTheme() {
            return theme;
        }

        /**
         * Returns the layout.
         * @return A Layout.
         */
        public Layout getLayout() {
            return layout;
        }

        /**
         * Returns the version of rapidoc to use.
         * @return The version.
         */
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return new StringBuilder(50).append("RapidocConfig [version=").append(version).append(", theme=")
                    .append(theme).append(", layout=").append(layout).append(']').toString();
        }

    }

    /**
     * ReDoc configuration.
     *
     * Currently only the version can be set.
     *
     * @author croudet
     */
    public static final class RedocConfig implements Renderer {
        private String version = "";

        private RedocConfig() {
        }

        /**
         * Builds a RedocConfig given a set of properties.
         * @param properties A set of properties.
         * @return A RedocConfig.
         */
        static RedocConfig fromProperties(Map<String, String> properties) {
            RedocConfig cfg = new RedocConfig();
            cfg.version = properties.getOrDefault("redoc.version", cfg.version);
            return cfg;
        }

        @Override
        public String render(String template) {
            return replacePlaceHolder(template, "redoc.version", version, "@");
        }

        /**
         * Returns the version of redoc to use.
         * @return The version.
         */
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return new StringBuilder(30).append("[version=").append(version).append(']').toString();
        }

    }

    /**
     * Swagger-ui configuration.
     * https://github.com/ostranme/swagger-ui-themes/tree/develop/themes/3.x
     * @author croudet
     */
    public static final class SwaggerUIConfig implements Renderer {
        private String version = "";
        private String layout = "StandaloneLayout";
        private boolean deepLinking = true;
        private Theme theme = Theme.DEFAULT;

        /**
         * Swagger-ui themes.
         * @author croudet
         */
        enum Theme {
            DEFAULT(null), MATERIAL("theme-material"), FEELING_BLUE("theme-feeling-blue"), FLATTOP("theme-flattop"),
            MONOKAI("theme-monokai"), MUTED("theme-muted"), NEWSPAPER("theme-newspaper"), OUTLINE("theme-outline");
            private String css;

            /**
             * Creates a Theme with the given css.
             * @param css A css.
             */
            Theme(String css) {
                this.css = css;
            }

            /**
             * Return the css of the theme.
             * @return A css name.
             */
            public String getCss() {
                return css;
            }
        }

        private SwaggerUIConfig() {
        }

        /**
         * Builds a SwaggerUIConfig given a set of properties.
         * @param properties A set of properties.
         * @return A SwaggerUIConfig.
         */
        static SwaggerUIConfig fromProperties(Map<String, String> properties) {
            SwaggerUIConfig cfg = new SwaggerUIConfig();
            cfg.version = properties.getOrDefault("swagger-ui.version", cfg.version);
            cfg.layout = properties.getOrDefault("swagger-ui.layout", cfg.layout);
            cfg.deepLinking = Boolean
                    .valueOf(properties.getOrDefault("swagger-ui.deep-linking", Boolean.toString(cfg.deepLinking)));
            cfg.theme = Theme
                    .valueOf(properties.getOrDefault("swagger-ui.theme", cfg.theme.name()).toUpperCase(Locale.US));
            return cfg;
        }

        @Override
        public String render(String template) {
            template = replacePlaceHolder(template, "swagger-ui.version", version, "@");
            template = replacePlaceHolder(template, "swagger-ui.layout", getLayout(), "");
            if (getTheme() != null && !Theme.DEFAULT.equals(getTheme())) {
                template = template.replace("{{swagger-ui.theme}}", "<link rel='stylesheet' type='text/css' href='https://unpkg.com/swagger-ui-themes@3.0.0/themes/3.x/" + getTheme().css + ".css' />");
            } else {
                template = template.replace("{{swagger-ui.theme}}", "");
            }
            template = replacePlaceHolder(template, "swagger-ui.deep-linking", Boolean.toString(deepLinking), "");
            return template;
        }

        /**
         * Returns the version of swagger-ui to use.
         * @return The version.
         */
        public String getVersion() {
            return version;
        }

        /**
         * Returns the layout. Default is 'StandaloneLayout'.
         * @return The layout.
         */
        public String getLayout() {
            return layout;
        }

        /**
         * Returns the deep linking flag.
         * @return The deep linking flag.
         */
        public boolean isDeepLinking() {
            return deepLinking;
        }

        /**
         * Returns the theme.
         * @return A Theme.
         */
        public Theme getTheme() {
            return theme;
        }

        @Override
        public String toString() {
            return new StringBuilder(100).append("[version=").append(version).append(", layout=").append(layout)
                    .append(", theme=").append(theme).append(", deepLinking=").append(deepLinking).append(']')
                    .toString();
        }

    }
}
