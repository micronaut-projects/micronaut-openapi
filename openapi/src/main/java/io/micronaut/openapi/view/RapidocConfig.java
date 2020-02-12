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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * RapiDoc configuration.
 *
 * Currently only the version, layout and theme can be set.
 *
 * @author croudet
 */
public final class RapidocConfig extends AbstractViewConfig implements Renderer {
    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>();

    // https://mrin9.github.io/RapiDoc/api.html
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(45);

    static {
        VALID_OPTIONS.put("style", AbstractViewConfig::asString);
        VALID_OPTIONS.put("sort-tags", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("sort-endpoints-by", new EnumConverter<>(EndPoint.class));
        VALID_OPTIONS.put("heading-text", AbstractViewConfig::asString);
        VALID_OPTIONS.put("goto-path", AbstractViewConfig::asString);
        VALID_OPTIONS.put("theme",  new EnumConverter<>(Theme.class));
        VALID_OPTIONS.put("bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("header-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("primary-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("regular-font", AbstractViewConfig::asString);
        VALID_OPTIONS.put("mono-font", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-hover-bg-color", RapidocConfig::asString);
        VALID_OPTIONS.put("nav-hover-text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-accent-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("layout",  new EnumConverter<>(Layout.class));
        VALID_OPTIONS.put("render-style",  new EnumConverter<>(RenderStyle.class));
        VALID_OPTIONS.put("schema-style",  new EnumConverter<>(SchemaStyle.class));
        VALID_OPTIONS.put("schema-expand-level", AbstractViewConfig::asString);
        VALID_OPTIONS.put("schema-description-expanded", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("default-schema-tab", new EnumConverter<>(DefaultSchemaTab.class));
        VALID_OPTIONS.put("response-area-height", AbstractViewConfig::asString);
        VALID_OPTIONS.put("show-info", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("show-header", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-authentication", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-spec-url-load", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-spec-file-load", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-search", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-try", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-server-selection", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("allow-api-list-style-selection", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("api-key-name", AbstractViewConfig::asString);
        VALID_OPTIONS.put("api-key-value", AbstractViewConfig::asString);
        VALID_OPTIONS.put("api-key-location",  new EnumConverter<>(ApiKeyLocation.class));
        VALID_OPTIONS.put("server-url", AbstractViewConfig::asString);
        VALID_OPTIONS.put("default-api-server", AbstractViewConfig::asString);
        VALID_OPTIONS.put("match-paths", AbstractViewConfig::asString);

        DEFAULT_OPTIONS.put("show-header", Boolean.FALSE);
        DEFAULT_OPTIONS.put("theme", Theme.DARK);
        DEFAULT_OPTIONS.put("layout", Layout.ROW);
        DEFAULT_OPTIONS.put("sort-tags", Boolean.TRUE);
        DEFAULT_OPTIONS.put("sort-endpoints-by", EndPoint.METHOD);
    }

    /**
     * Rapidoc api key location.
     */
    enum ApiKeyLocation {
        HEADER, QUERY;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc default schema tab styles.
     */
    enum DefaultSchemaTab {
        MODEL, EXAMPLE;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc schema styles.
     */
    enum SchemaStyle {
        TREE, TABLE;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc render styles.
     */
    enum RenderStyle {
        READ, VIEW;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc end point sorting.
     */
    enum EndPoint {
        PATH, METHOD;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }

    /**
     * Rapidoc themes.
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
     */
    enum Layout {
        COLUMN, ROW;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }

    private RapiPDFConfig rapiPDFConfig;

    private RapidocConfig() {
        super("rapidoc.");
    }

    /**
     * Builds a RapidocConfig given a set of properties.
     * @param properties A set of properties.
     * @return A RapidocConfig.
     */
    static RapidocConfig fromProperties(Map<String, String> properties) {
        RapidocConfig cfg = new RapidocConfig();
        cfg.rapiPDFConfig = RapiPDFConfig.fromProperties(properties);
        return AbstractViewConfig.fromProperties(cfg, DEFAULT_OPTIONS, properties);
    }

    @Override
    public String render(String template) {
        template = rapiPDFConfig.render(template);
        template = OpenApiViewConfig.replacePlaceHolder(template, "rapidoc.version", version, "@");
        return OpenApiViewConfig.replacePlaceHolder(template, "rapidoc.attributes", toHtmlAttributes(), "");
    }

    @Override
    protected Function<String, Object> getConverter(String key) {
        return VALID_OPTIONS.get(key);
    }
}
