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
import java.util.Map;
import java.util.function.Function;

import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;

/**
 * ReDoc configuration.
 *
 * @author croudet
 */
final class RedocConfig extends AbstractViewConfig implements Renderer {

    private static final String DEFAULT_REDOC_JS_PATH = OpenApiViewConfig.RESOURCE_DIR + "/redoc.standalone.js";

    private static final List<String> RESOURCE_FILES = Collections.singletonList(
        DEFAULT_REDOC_JS_PATH
    );

    private static final Map<String, Object> DEFAULT_OPTIONS = Collections.emptyMap();

    // https://github.com/Redocly/redoc#redoc-options-object
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(38);

    static {
        VALID_OPTIONS.put("disable-search", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("min-character-length-to-init-search", AbstractViewConfig::asInt);
        VALID_OPTIONS.put("expand-default-server-variables", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("expand-responses", AbstractViewConfig::asString);
        VALID_OPTIONS.put("generated-payload-samples-max-depth", AbstractViewConfig::asInt);
        VALID_OPTIONS.put("max-displayed-enum-values", AbstractViewConfig::asInt);
        VALID_OPTIONS.put("hide-download-button", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("download-file-name", AbstractViewConfig::asString);
        VALID_OPTIONS.put("download-definition-url", AbstractViewConfig::asString);
        VALID_OPTIONS.put("hide-hostname", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-loading", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-fab", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-schema-pattern", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-single-request-sample-tab", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("show-object-schema-examples", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("expand-single-schema-field", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("schema-expansion-level", AbstractViewConfig::asString);
        VALID_OPTIONS.put("json-sample-expand-level", AbstractViewConfig::asString);
        VALID_OPTIONS.put("hide-schema-titles", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("simple-one-of-type-label", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("sort-enum-values-alphabetically", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("sort-operations-alphabetically", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("sort-tags-alphabetically", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("lazy-rendering", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("menu-toggle", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("native-scrollbars", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("only-required-in-samples", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("path-in-middle-panel", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("required-props-first", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("scroll-y-offset", AbstractViewConfig::asString);
        VALID_OPTIONS.put("show-extensions", AbstractViewConfig::asString);
        VALID_OPTIONS.put("sort-props-alphabetically", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("payload-sample-idx", AbstractViewConfig::asString);
        VALID_OPTIONS.put("theme", AbstractViewConfig::asString);
        VALID_OPTIONS.put("untrusted-spec", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("nonce", AbstractViewConfig::asString);
        VALID_OPTIONS.put("side-nav-style", SideNavStyle::byCode);
        VALID_OPTIONS.put("show-webhook-verb", AbstractViewConfig::asBoolean);
    }

    /**
     * Redoc side-nav-style.
     */
    enum SideNavStyle {
        SUMMARY_ONLY("summary-only"),
        PATH_ONLY("path-only"),
        ID_ONLY("id-only"),
        ;

        private static final Map<String, SideNavStyle> BY_CODE;

        static {
            Map<String, SideNavStyle> byCode = new HashMap<>(SideNavStyle.values().length);
            for (SideNavStyle navTagClick : values()) {
                byCode.put(navTagClick.code, navTagClick);
            }
            BY_CODE = Collections.unmodifiableMap(byCode);
        }

        private final String code;

        SideNavStyle(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }

        public static SideNavStyle byCode(String code) {
            SideNavStyle value = BY_CODE.get(code.toLowerCase());
            if (value == null) {
                throw new IllegalArgumentException("Unknown value " + code);
            }
            return value;
        }
    }

    RapiPDFConfig rapiPDFConfig;

    private RedocConfig() {
        super("redoc.");
        jsUrl = DEFAULT_REDOC_JS_PATH;
    }

    /**
     * Builds a RedocConfig given a set of properties.
     *
     * @param properties A set of properties.
     *
     * @return A RedocConfig.
     */
    static RedocConfig fromProperties(Map<String, String> properties) {
        RedocConfig cfg = AbstractViewConfig.fromProperties(new RedocConfig(), DEFAULT_OPTIONS, properties);
        if (DEFAULT_REDOC_JS_PATH.equals(cfg.jsUrl)) {
            cfg.isDefaultJsUrl = true;
        }
        return cfg;
    }

    @Override
    public String render(String template) {
        template = rapiPDFConfig.render(template, RendererType.REDOC);
        template = OpenApiViewConfig.replacePlaceHolder(template, "redoc.js.url", jsUrl, "");
        return OpenApiViewConfig.replacePlaceHolder(template, "redoc.attributes", toHtmlAttributes(), "");
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
