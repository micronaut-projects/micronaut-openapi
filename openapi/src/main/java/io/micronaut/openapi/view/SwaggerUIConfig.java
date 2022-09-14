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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;

/**
 * Swagger-ui configuration.
 *
 * @author croudet
 */
final class SwaggerUIConfig extends AbstractViewConfig implements Renderer {

    private static final String DEFAULT_SWAGGER_JS_PATH = OpenApiViewConfig.RESOURCE_DIR + "/";

    private static final List<String> RESOURCE_FILES = Arrays.asList(
        DEFAULT_SWAGGER_JS_PATH + "swagger-ui.css",
        DEFAULT_SWAGGER_JS_PATH + "favicon-16x16.png",
        DEFAULT_SWAGGER_JS_PATH + "favicon-32x32.png",
        DEFAULT_SWAGGER_JS_PATH + "swagger-ui-bundle.js",
        DEFAULT_SWAGGER_JS_PATH + "swagger-ui-standalone-preset.js"
    );

    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>(4);
    private static final String OPTION_OAUTH2 = "oauth2";
    private static final String DOT = ".";
    private static final String PREFIX_SWAGGER_UI = "swagger-ui";
    private static final String KEY_VALUE_SEPARATOR = ": ";
    private static final String COMMNA_NEW_LINE = ",\n";

    // https://github.com/swagger-api/swagger-ui/blob/HEAD/docs/usage/configuration.md
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(29);
    // https://github.com/swagger-api/swagger-ui/blob/master/docs/usage/oauth2.md
    private static final Map<String, Function<String, Object>> VALID_OAUTH2_OPTIONS = new HashMap<>(9);

    static {
        VALID_OPTIONS.put("layout", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("deepLinking", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("displayOperationId", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("defaultModelsExpandDepth", AbstractViewConfig::asString);
        VALID_OPTIONS.put("defaultModelExpandDepth", AbstractViewConfig::asString);
        VALID_OPTIONS.put("defaultModelRendering", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("displayRequestDuration", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("docExpansion", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("filter", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("maxDisplayedTags", AbstractViewConfig::asString);
        VALID_OPTIONS.put("operationsSorter", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("showExtensions", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("showCommonExtensions", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("tagsSorter", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("onComplete", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("syntaxHighlight", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("syntaxHighlight.activate", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("syntaxHighlight.theme", SyntaxHighlightTheme::byCode);
        VALID_OPTIONS.put("tryItOutEnabled", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("requestSnippetsEnabled", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("requestSnippets", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("oauth2RedirectUrl", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("requestInterceptor", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("request.curlOptions", AbstractViewConfig::asString);
        VALID_OPTIONS.put("responseInterceptor", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("showMutatedRequest", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("supportedSubmitMethods", AbstractViewConfig::asString);
        VALID_OPTIONS.put("validatorUrl", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("withCredentials", AbstractViewConfig::asBoolean);

        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".clientId", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".clientSecret", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".realm", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".appName", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".scopeSeparator", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".scopes", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".additionalQueryStringParams", AbstractViewConfig::asString);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".useBasicAuthenticationWithAccessCodeGrant", AbstractViewConfig::asBoolean);
        VALID_OAUTH2_OPTIONS.put(OPTION_OAUTH2 + ".usePkceWithAuthorizationCodeGrant", AbstractViewConfig::asBoolean);

        DEFAULT_OPTIONS.put("layout", "\"StandaloneLayout\"");
        DEFAULT_OPTIONS.put("deepLinking", Boolean.TRUE);
        DEFAULT_OPTIONS.put("validatorUrl", null);
    }

    RapiPDFConfig rapiPDFConfig;
    SwaggerUIConfig.Theme theme = Theme.CLASSIC;

    enum SyntaxHighlightTheme {
        AGATE("agate"),
        ARTA("arta"),
        MONOKAI("monokai"),
        NORD("nord"),
        OBSIDIAN("obsidian"),
        TOMORROW_NIGHT("tomorrow-night"),
        ;

        private static final Map<String, SyntaxHighlightTheme> BY_CODE;

        static {
            Map<String, SyntaxHighlightTheme> byCode = new HashMap<>(SyntaxHighlightTheme.values().length);
            for (SyntaxHighlightTheme navTagClick : values()) {
                byCode.put(navTagClick.code, navTagClick);
            }
            BY_CODE = Collections.unmodifiableMap(byCode);
        }

        private final String code;

        SyntaxHighlightTheme(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }

        public static SyntaxHighlightTheme byCode(String code) {
            SyntaxHighlightTheme value = BY_CODE.get(code.toLowerCase());
            if (value == null) {
                throw new IllegalArgumentException("Unknown value " + code);
            }
            return value;
        }
    }

    /**
     * Swagger-ui themes. <a href="https://github.com/ilyamixaltik/swagger-themes">link</a>
     */
    enum Theme {
        CLASSIC("classic"),
        DARK("dark"),
        FEELING_BLUE("feeling-blue"),
        FLATTOP("flattop"),
        MATERIAL("material"),
        MONOKAI("monokai"),
        MUTED("muted"),
        NEWSPAPER("newspaper"),
        OUTLINE("toutline"),
        ;

        private final String css;

        /**
         * Creates a Theme with the given css.
         *
         * @param css A css.
         */
        Theme(String css) {
            this.css = css;
        }

        /**
         * Return the css of the theme.
         *
         * @return A css name.
         */
        public String getCss() {
            return css;
        }
    }

    private SwaggerUIConfig() {
        super(PREFIX_SWAGGER_UI + DOT);
        jsUrl = DEFAULT_SWAGGER_JS_PATH;
    }

    @NonNull
    private String toOptions() {
        return toOptions(VALID_OPTIONS, null);
    }

    private String toOptions(@NonNull Map<String, Function<String, Object>> validOptions,
                             @Nullable String keyPrefix) {
        return options
                .entrySet()
                .stream()
                .filter(e -> validOptions.containsKey(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ((keyPrefix != null && e.getKey().startsWith(keyPrefix)) ? e.getKey().substring(keyPrefix.length()) : e.getKey())
                    + KEY_VALUE_SEPARATOR + e.getValue())
                .collect(Collectors.joining(COMMNA_NEW_LINE));
    }

    @NonNull
    private String toOauth2Options() {
        String properties = toOptions(VALID_OAUTH2_OPTIONS, OPTION_OAUTH2 + DOT);
        if (StringUtils.hasText(properties)) {
            return "ui.initOAuth({\n" + properties + "\n});";
        } else {
            return "";
        }
    }

    static boolean hasOauth2Option(Map<String, Object> options) {
        return options.containsKey("oauth2RedirectUrl") || VALID_OAUTH2_OPTIONS.keySet().stream().anyMatch(options::containsKey);
    }

    /**
     * Builds a SwaggerUIConfig given a set of properties.
     *
     * @param properties A set of properties.
     *
     * @return A SwaggerUIConfig.
     */
    static SwaggerUIConfig fromProperties(Map<String, String> properties) {
        SwaggerUIConfig cfg = new SwaggerUIConfig();
        cfg.theme = Theme.valueOf(properties.getOrDefault(PREFIX_SWAGGER_UI + ".theme", cfg.theme.name()).toUpperCase(Locale.US));
        AbstractViewConfig.fromProperties(cfg, DEFAULT_OPTIONS, properties);
        if (DEFAULT_SWAGGER_JS_PATH.equals(cfg.jsUrl)) {
            cfg.isDefaultJsUrl = true;
        }
        return cfg;
    }

    @Override
    public String render(String template) {
        template = rapiPDFConfig.render(template, RendererType.SWAGGER_UI);
        template = OpenApiViewConfig.replacePlaceHolder(template, PREFIX_SWAGGER_UI + ".js.url", jsUrl, "");
        template = OpenApiViewConfig.replacePlaceHolder(template, PREFIX_SWAGGER_UI + ".attributes", toOptions(), "");
        template = template.replace("{{" + PREFIX_SWAGGER_UI + ".theme}}", theme == null || Theme.CLASSIC == theme ? "" :
            "<link rel='stylesheet' type='text/css' href='" + OpenApiViewConfig.THEMES_DIR + '/' + theme.getCss() + ".css' />");
        template = template.replace("{{" + PREFIX_SWAGGER_UI + DOT + OPTION_OAUTH2 + "}}", hasOauth2Option(options) ? toOauth2Options() : "");
        return template;
    }

    @Override
    protected Function<String, Object> getConverter(String key) {
        return (VALID_OPTIONS.containsKey(key) ? VALID_OPTIONS : VALID_OAUTH2_OPTIONS).get(key);
    }

    @Override
    protected List<String> getResources() {
        return RESOURCE_FILES;
    }
}
