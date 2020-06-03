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
import java.util.Map;
import java.util.function.Function;

import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;

/**
 * ReDoc configuration.
 *
 * @author croudet
 */
final class RedocConfig extends AbstractViewConfig implements Renderer {
    private static final Map<String, Object> DEFAULT_OPTIONS = Collections.emptyMap();

    // https://github.com/Redocly/redoc#redoc-options-object
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(22);

    static {
        VALID_OPTIONS.put("disable-search", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("expand-default-server-variables", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("expand-responses", AbstractViewConfig::asString);
        VALID_OPTIONS.put("hide-download-button", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-hostname", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-loading", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hide-single-request-sample-tab", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("expand-single-schema-field", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("json-sample-expand-level", AbstractViewConfig::asString);
        VALID_OPTIONS.put("menu-toggle", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("native-scrollbars", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("no-auto-auth", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("only-required-in-samples", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("path-in-middle-panel", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("required-props-first", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("scroll-y-offset", AbstractViewConfig::asString);
        VALID_OPTIONS.put("show-extensions", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("sort-props-alphabetically", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("suppress-warnings", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("payload-sample-idx", AbstractViewConfig::asString);
        VALID_OPTIONS.put("theme", AbstractViewConfig::asString);
        VALID_OPTIONS.put("untrusted-spec", AbstractViewConfig::asBoolean);
    }

    RapiPDFConfig rapiPDFConfig;

    private RedocConfig() {
        super("redoc.");
    }

    /**
     * Builds a RedocConfig given a set of properties.
     * 
     * @param properties A set of properties.
     * @return A RedocConfig.
     */
    static RedocConfig fromProperties(Map<String, String> properties) {
        return AbstractViewConfig.fromProperties(new RedocConfig(), DEFAULT_OPTIONS, properties);
    }

    @Override
    public String render(String template) {
        template = rapiPDFConfig.render(template, RendererType.REDOC);
        template = OpenApiViewConfig.replacePlaceHolder(template, "redoc.version", version, "@");
        return OpenApiViewConfig.replacePlaceHolder(template, "redoc.attributes", toHtmlAttributes(), "");
    }

    @Override
    protected Function<String, Object> getConverter(String key) {
        return VALID_OPTIONS.get(key);
    }
}
