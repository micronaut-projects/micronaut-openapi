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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;
import io.micronaut.openapi.visitor.Pair;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static io.micronaut.openapi.visitor.StringUtil.SLASH;

/**
 * OpenAPI Explorer configuration.
 */
public final class OpenApiExplorerConfig extends AbstractViewConfig {

    public static final String OPENAPI_EXPLORER_PREFIX = "openapi-explorer.";
    private static final String DEFAULT_OPENAPI_EXPLORER_JS_PATH = OpenApiViewConfig.RESOURCE_DIR + SLASH;

    private static final List<String> RESOURCE_FILES = List.of(
        DEFAULT_OPENAPI_EXPLORER_JS_PATH + "default.min.css",
        DEFAULT_OPENAPI_EXPLORER_JS_PATH + "bootstrap.min.css",
        DEFAULT_OPENAPI_EXPLORER_JS_PATH + "font-awesome.min.css",
        DEFAULT_OPENAPI_EXPLORER_JS_PATH + "highlight.min.js",
        DEFAULT_OPENAPI_EXPLORER_JS_PATH + "openapi-explorer.min.js"
    );

    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>();

    // https://github.com/Authress-Engineering/openapi-explorer/blob/release/2.1/docs/documentation.md
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(61);

    static {
        // Setup
        VALID_OPTIONS.put("explorer-location", AbstractViewConfig::asString);
        VALID_OPTIONS.put("spec-url", AbstractViewConfig::asString);
        VALID_OPTIONS.put("server-url", AbstractViewConfig::asString);

        // Disable configuration
        VALID_OPTIONS.put("display-nulls", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-defaults", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("collapse", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("table", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("schema-expand-level", AbstractViewConfig::asInt);

        // Hide/Show Sections
        VALID_OPTIONS.put("hide-console", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-authentication", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-server-selection", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-components", AbstractViewConfig::asBoolean);

        // Custom configuration
        VALID_OPTIONS.put("default-schema-tab", new EnumConverter<>(DefaultSchemaTab.class));
        VALID_OPTIONS.put("use-path-in-nav-bar", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("nav-item-spacing", new EnumConverter<>(NavItemSpacing.class));

        // Colors
        VALID_OPTIONS.put("bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("header-bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("primary-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("secondary-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-bg-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-hover-text-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("nav-text-color", AbstractViewConfig::asString);
    }

    /**
     * OpenAPI Explorer nav-item-spacing.
     */
    enum NavItemSpacing {
        DEFAULT, COMPACT, RELAXED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    /**
     * OpenAPI Explorer default schema tab styles.
     */
    enum DefaultSchemaTab {
        MODEL, BODY;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    RapiPDFConfig rapiPDFConfig;

    private OpenApiExplorerConfig(Map<Pair<String, String>, OpenApiInfo> openApiInfos) {
        super(OPENAPI_EXPLORER_PREFIX, openApiInfos);
        jsUrl = DEFAULT_OPENAPI_EXPLORER_JS_PATH;
    }

    /**
     * Builds a OpenAPI ExplorerConfig given a set of properties.
     *
     * @param properties A set of properties.
     * @param openApiInfos Open API info objects.
     * @param context Visitor context.
     *
     * @return A OpenApiExplorerConfig.
     */
    static OpenApiExplorerConfig fromProperties(Map<String, String> properties, Map<Pair<String, String>, OpenApiInfo> openApiInfos, VisitorContext context) {
        return AbstractViewConfig.fromProperties(new OpenApiExplorerConfig(openApiInfos), DEFAULT_OPTIONS, properties, RendererType.OPENAPI_EXPLORER, context);
    }

    @Override
    public String render(String template, @Nullable VisitorContext context) {
        template = rapiPDFConfig.render(template, RendererType.OPENAPI_EXPLORER, context);
        template = OpenApiViewConfig.replacePlaceHolder(template, "openapi-explorer.js.url.prefix", isDefaultJsUrl ? getFinalUrlPrefix(RendererType.OPENAPI_EXPLORER, context) : jsUrl, StringUtils.EMPTY_STRING);
        return OpenApiViewConfig.replacePlaceHolder(template, "openapi-explorer.attributes", toHtmlAttributes(), StringUtils.EMPTY_STRING);
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
