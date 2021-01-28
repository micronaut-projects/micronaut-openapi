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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micronaut.core.util.StringUtils;
import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;

/**
 * Swagger-ui configuration.
 *
 * @author croudet
 */
final class SwaggerUIConfig extends AbstractViewConfig implements Renderer {
    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>(4);

    // https://github.com/swagger-api/swagger-ui/blob/HEAD/docs/usage/configuration.md
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(16);

    // https://github.com/swagger-api/swagger-ui/blob/master/docs/usage/oauth2.md
    private static final Map<String, Function<String, Object>> VALID_OAUTH2_OPTIONS = new HashMap<>(9);

    static {
        VALID_OPTIONS.put("layout", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("deepLinking", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("displayOperationId", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("defaultModelsExpandDepth", AbstractViewConfig::asString);
        VALID_OPTIONS.put("defaultModelRendering", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("displayRequestDuration", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("docExpansion", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("filter", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("maxDisplayedTags", AbstractViewConfig::asString);
        VALID_OPTIONS.put("operationsSorter", AbstractViewConfig::asString);
        VALID_OPTIONS.put("showExtensions", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("showCommonExtensions", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("tagsSorter", AbstractViewConfig::asString);
        VALID_OPTIONS.put("oauth2RedirectUrl", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("showMutatedRequest", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("supportedSubmitMethods", AbstractViewConfig::asString);
        VALID_OPTIONS.put("validatorUrl", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("withCredentials", AbstractViewConfig::asBoolean);

        VALID_OAUTH2_OPTIONS.put("oauth2.clientId", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put("oauth2.clientSecret", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put("oauth2.realm", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put("oauth2.appName", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put("oauth2.scopeSeparator", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put("oauth2.scopes", AbstractViewConfig::asQuotedString);
        VALID_OAUTH2_OPTIONS.put("oauth2.additionalQueryStringParams", AbstractViewConfig::asString);
        VALID_OAUTH2_OPTIONS.put("oauth2.useBasicAuthenticationWithAccessCodeGrant", AbstractViewConfig::asBoolean);
        VALID_OAUTH2_OPTIONS.put("oauth2.usePkceWithAuthorizationCodeGrant", AbstractViewConfig::asBoolean);

        DEFAULT_OPTIONS.put("layout", "\"StandaloneLayout\"");
        DEFAULT_OPTIONS.put("deepLinking", Boolean.TRUE);
        DEFAULT_OPTIONS.put("validatorUrl", null);
    }

    RapiPDFConfig rapiPDFConfig;
    private SwaggerUIConfig.Theme theme = Theme.DEFAULT;

    /**
     * Swagger-ui themes.
     * https://github.com/ostranme/swagger-ui-themes
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
        super("swagger-ui.");
    }

    private String toOptions() {
        return options
                .entrySet()
                .stream()
                .filter(e -> VALID_OPTIONS.containsKey(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(",\n"));
    }

    private String toOauth2Options() {
        String properties = options
                .entrySet()
                .stream()
                .filter(e -> VALID_OAUTH2_OPTIONS.containsKey(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey().substring("oauth2.".length()) + ": " + e.getValue())
                .collect(Collectors.joining(",\n"));
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
     * @param properties A set of properties.
     * @return A SwaggerUIConfig.
     */
    static SwaggerUIConfig fromProperties(Map<String, String> properties) {
        SwaggerUIConfig cfg = new SwaggerUIConfig();
        cfg.theme = Theme
                .valueOf(properties.getOrDefault("swagger-ui.theme", cfg.theme.name()).toUpperCase(Locale.US));
        return AbstractViewConfig.fromProperties(cfg, DEFAULT_OPTIONS, properties);
    }

    @Override
    public String render(String template) {
        template = rapiPDFConfig.render(template, RendererType.SWAGGER_UI);
        template = OpenApiViewConfig.replacePlaceHolder(template, "swagger-ui.version", version, "@");
        template = OpenApiViewConfig.replacePlaceHolder(template, "swagger-ui.attributes", toOptions(), "");
        if (theme == null || Theme.DEFAULT.equals(theme)) {
            template = template.replace("{{swagger-ui.theme}}", "");
        } else {
            template = template.replace("{{swagger-ui.theme}}", "<link rel='stylesheet' type='text/css' href='https://unpkg.com/swagger-ui-themes@3.0.0/themes/3.x/" + theme.getCss() + ".css' />");
        }
        if (hasOauth2Option(options)) {
            template = template.replace("{{swagger-ui.oauth2}}", toOauth2Options());
        } else {
            template = template.replace("{{swagger-ui.oauth2}}", "");
        }
        return template;
    }

    @Override
    protected Function<String, Object> getConverter(String key) {
        if (VALID_OPTIONS.containsKey(key)) {
            return VALID_OPTIONS.get(key);
        } else {
            return VALID_OAUTH2_OPTIONS.get(key);
        }
    }

}
