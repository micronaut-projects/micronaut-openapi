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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;
import io.micronaut.openapi.visitor.Pair;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

/**
 * RapiDoc configuration.
 *
 * @author croudet
 */
final class RapidocConfig extends AbstractViewConfig {

    public static final String RAPIDOC_PREFIX = "rapidoc.";
    private static final String DEFAULT_RAPIDOC_JS_PATH = OpenApiViewConfig.RESOURCE_DIR + "/";

    private static final List<String> RESOURCE_FILES = Collections.singletonList(
        DEFAULT_RAPIDOC_JS_PATH + "rapidoc-min.js"
    );

    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>();

    // https://rapidocweb.com/api.html
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(61);

    static {
        VALID_OPTIONS.put("style", AbstractViewConfig::asString);

        // General
        VALID_OPTIONS.put("spec-url", AbstractViewConfig::asString);
        VALID_OPTIONS.put("update-route", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("route-prefix", AbstractViewConfig::asString);
        VALID_OPTIONS.put("sort-tags", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("sort-endpoints-by", new EnumConverter<>(EndPoint.class));
        VALID_OPTIONS.put("heading-text", AbstractViewConfig::asString);
        VALID_OPTIONS.put("goto-path", AbstractViewConfig::asString);
        VALID_OPTIONS.put("fill-request-fields-with-example", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("persist-auth", AbstractViewConfig::asBoolean);

        // UI Colors and Fonts
        VALID_OPTIONS.put("theme", new EnumConverter<>(Theme.class));
        VALID_OPTIONS.put("bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("header-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("primary-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("load-fonts", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("regular-font", AbstractViewConfig::asString);
        VALID_OPTIONS.put("mono-font", AbstractViewConfig::asString);
        VALID_OPTIONS.put("font-size", new EnumConverter<>(FontSize.class));
        VALID_OPTIONS.put("css-file", AbstractViewConfig::asString);
        VALID_OPTIONS.put("css-classes", AbstractViewConfig::asString);

        // Navigation bar settings (only applicable in read and focused render style)
        VALID_OPTIONS.put("show-method-in-nav-bar", ShowMethodInNavBar::byCode);
        VALID_OPTIONS.put("use-path-in-nav-bar", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("nav-bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-hover-bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-hover-text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-accent-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-accent-text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-active-item-marker", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-item-spacing", new EnumConverter<>(NavItemSpacing.class));
        VALID_OPTIONS.put("on-nav-tag-click", NavTagClick::byCode);

        // UI Layout & Placement
        VALID_OPTIONS.put("layout", new EnumConverter<>(Layout.class));
        VALID_OPTIONS.put("render-style", new EnumConverter<>(RenderStyle.class));
        VALID_OPTIONS.put("response-area-height", AbstractViewConfig::asString);

        // Hide/Show Sections
        VALID_OPTIONS.put("show-info", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("info-description-headings-in-navbar", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("show-components", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("show-header", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-authentication", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-spec-url-load", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-spec-file-load", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-spec-file-download", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-search", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-advanced-search", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-try", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("show-curl-before-try", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-server-selection", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-schema-description-expand-toggle", AbstractViewConfig::asBoolean);

        // Schema Section Settings
        VALID_OPTIONS.put("schema-style", new EnumConverter<>(SchemaStyle.class));
        VALID_OPTIONS.put("schema-expand-level", AbstractViewConfig::asString);
        VALID_OPTIONS.put("schema-description-expanded", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("schema-hide-read-only", new EnumConverter<>(SchemaHideReadOnly.class));
        VALID_OPTIONS.put("schema-hide-write-only", new EnumConverter<>(SchemaHideWriteOnly.class));
        VALID_OPTIONS.put("default-schema-tab", new EnumConverter<>(DefaultSchemaTab.class));

        // API Server & calls
        VALID_OPTIONS.put("server-url", AbstractViewConfig::asString);
        VALID_OPTIONS.put("default-api-server", AbstractViewConfig::asString);
        VALID_OPTIONS.put("api-key-name", AbstractViewConfig::asString);
        VALID_OPTIONS.put("api-key-location", new EnumConverter<>(ApiKeyLocation.class));
        VALID_OPTIONS.put("api-key-value", AbstractViewConfig::asString);
        VALID_OPTIONS.put("fetch-credentials", FetchCredentials::byCode);

        DEFAULT_OPTIONS.put("show-header", Boolean.FALSE);
        DEFAULT_OPTIONS.put("theme", Theme.DARK);
        DEFAULT_OPTIONS.put("layout", Layout.ROW);
        DEFAULT_OPTIONS.put("sort-tags", Boolean.TRUE);
        DEFAULT_OPTIONS.put("sort-endpoints-by", EndPoint.METHOD);
    }

    /**
     * Rapidoc font-size.
     */
    enum FontSize {
        DEFAULT, LARGE, LARGEST;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc nav-item-spacing.
     */
    enum NavItemSpacing {
        DEFAULT, COMPACT, RELAXED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc on-nav-tag-click.
     */
    enum NavTagClick {
        EXPAND_COLLAPSE("expand-collapse"),
        SHOW_DESCRIPTION("show-description");

        private static final Map<String, NavTagClick> BY_CODE;

        static {
            Map<String, NavTagClick> byCode = new HashMap<>(NavTagClick.values().length);
            for (NavTagClick navTagClick : values()) {
                byCode.put(navTagClick.code, navTagClick);
            }
            BY_CODE = Collections.unmodifiableMap(byCode);
        }

        private final String code;

        NavTagClick(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }

        public static NavTagClick byCode(String code) {
            NavTagClick value = BY_CODE.get(code.toLowerCase());
            if (value == null) {
                throw new IllegalArgumentException("Unknown value " + code);
            }
            return value;
        }
    }

    /**
     * Rapidoc fetch-credentials.
     */
    enum FetchCredentials {
        OMIT("omit"),
        SAME_ORIGIN("same-origin"),
        INCLUDE("include"),
        ;

        private static final Map<String, FetchCredentials> BY_CODE;

        static {
            Map<String, FetchCredentials> byCode = new HashMap<>(FetchCredentials.values().length);
            for (FetchCredentials navTagClick : values()) {
                byCode.put(navTagClick.code, navTagClick);
            }
            BY_CODE = Collections.unmodifiableMap(byCode);
        }

        private final String code;

        FetchCredentials(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }

        public static FetchCredentials byCode(String code) {
            FetchCredentials value = BY_CODE.get(code.toLowerCase());
            if (value == null) {
                throw new IllegalArgumentException("Unknown value " + code);
            }
            return value;
        }
    }

    /**
     * Rapidoc show-method-in-nav-bar.
     */
    enum ShowMethodInNavBar {
        FALSE("false"),
        AS_PLAIN_TEXT("as-plain-text"),
        AS_COLORED_TEXT("as-colored-text"),
        AS_COLORED_BLOCK("as-colored-block"),
        ;

        private static final Map<String, ShowMethodInNavBar> BY_CODE;

        static {
            Map<String, ShowMethodInNavBar> byCode = new HashMap<>(ShowMethodInNavBar.values().length);
            for (ShowMethodInNavBar showMethodInNavBar : values()) {
                byCode.put(showMethodInNavBar.code, showMethodInNavBar);
            }
            BY_CODE = Collections.unmodifiableMap(byCode);
        }

        private final String code;

        ShowMethodInNavBar(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }

        public static ShowMethodInNavBar byCode(String code) {
            ShowMethodInNavBar value = BY_CODE.get(code.toLowerCase());
            if (value == null) {
                throw new IllegalArgumentException("Unknown value " + code);
            }
            return value;
        }
    }

    /**
     * Rapidoc api key location.
     */
    enum ApiKeyLocation {
        HEADER, QUERY;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc schema-hide-read-only.
     */
    enum SchemaHideReadOnly {
        DEFAULT, NEVER;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc schema-hide-write-only.
     */
    enum SchemaHideWriteOnly {
        DEFAULT, NEVER;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc default schema tab styles.
     */
    enum DefaultSchemaTab {
        SCHEMA, EXAMPLE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc schema styles.
     */
    enum SchemaStyle {
        TREE, TABLE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc render styles.
     */
    enum RenderStyle {
        READ, VIEW, FOCUSED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc end point sorting.
     */
    enum EndPoint {
        PATH, METHOD, SUMMARY, NONE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc themes.
     */
    enum Theme {
        LIGHT, DARK;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc layouts.
     */
    enum Layout {
        ROW, COLUMN;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    RapiPDFConfig rapiPDFConfig;

    private RapidocConfig(Map<Pair<String, String>, OpenApiInfo> openApiInfos) {
        super(RAPIDOC_PREFIX, openApiInfos);
        jsUrl = DEFAULT_RAPIDOC_JS_PATH;
    }

    /**
     * Builds a RapidocConfig given a set of properties.
     *
     * @param properties A set of properties.
     * @param openApiInfos Open API info objects.
     * @param context Visitor context.
     *
     * @return A RapidocConfig.
     */
    static RapidocConfig fromProperties(Map<String, String> properties, Map<Pair<String, String>, OpenApiInfo> openApiInfos, VisitorContext context) {
        return AbstractViewConfig.fromProperties(new RapidocConfig(openApiInfos), DEFAULT_OPTIONS, properties, RendererType.RAPIDOC, context);
    }

    @Override
    public String render(String template, VisitorContext context) {
        template = rapiPDFConfig.render(template, RendererType.RAPIDOC, context);
        template = OpenApiViewConfig.replacePlaceHolder(template, "rapidoc.js.url.prefix", isDefaultJsUrl ? getFinalUrlPrefix(RendererType.RAPIDOC, context) : jsUrl, "");
        return OpenApiViewConfig.replacePlaceHolder(template, "rapidoc.attributes", toHtmlAttributes(), "");
    }

    @Override
    protected Function<String, Object> getConverter(String key) {
        return VALID_OPTIONS.get(key);
    }

    @Override
    protected List<String> getResources() {
        return RESOURCE_FILES;
    }
}
