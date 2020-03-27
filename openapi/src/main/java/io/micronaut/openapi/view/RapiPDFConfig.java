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

import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;

/**
 * RapiPDF configuration.
 *
 * @author croudet
 */
final class RapiPDFConfig extends AbstractViewConfig {
    private static final String LINK = "<script src='https://unpkg.com/rapipdf{{rapipdf.version}}/dist/rapipdf-min.js'></script>";
    private static final String TAG = "<rapi-pdf id='rapi-pdf' {{rapipdf.attributes}}></rapi-pdf>";
    private static final String SPEC = "document.getElementById('rapi-pdf').setAttribute('spec-url', '{{specURL}}');";
    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>(6);

    private static final String DEFAULT_RAPIDOC_STYLE = "width: 122px;height: 26px;font-size: 15px;padding-bottom: 0px;padding-top: 5px;padding-left: 12px;margin-left: 12px";
    private static final String DEFAULT_REDOC_STYLE = "width: 122px;height: 26px;font-size: 15px;padding-bottom: 5px;padding-top: 5px;margin-left: 2px";
    private static final String DEFAULT_SWAGGER_UI_STYLE = DEFAULT_REDOC_STYLE;

    // https://mrin9.github.io/RapiPdf/
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(20);

    static {
        VALID_OPTIONS.put("style", AbstractViewConfig::asString);
        VALID_OPTIONS.put("button-label", AbstractViewConfig::asString);
        VALID_OPTIONS.put("button-bg", AbstractViewConfig::asString);
        VALID_OPTIONS.put("button-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("input-bg",  AbstractViewConfig::asString);
        VALID_OPTIONS.put("input-color",  AbstractViewConfig::asString);
        VALID_OPTIONS.put("hide-input", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("pdf-primary-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("pdf-alternate-color", AbstractViewConfig::asString);
        VALID_OPTIONS.put("pdf-title", AbstractViewConfig::asString);
        VALID_OPTIONS.put("pdf-footer-text", AbstractViewConfig::asString);
        VALID_OPTIONS.put("pdf-schema-style", new EnumConverter<>(SchemaStyle.class));
        VALID_OPTIONS.put("include-info", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("include-toc", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("include-security", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("include-api-details", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("include-api-list", AbstractViewConfig::asBoolean);
        
        DEFAULT_OPTIONS.put("hide-input", Boolean.TRUE);
        DEFAULT_OPTIONS.put("button-bg", "#b44646");
        DEFAULT_OPTIONS.put("pdf-title", "{{title}}");
    }

    /**
     * Rapipdf schema styles.
     */
    enum SchemaStyle {
        OBJECT, TABLE;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.US);
        }
    }
    
    private boolean enabled; //false

    private RapiPDFConfig() {
        super("rapipdf.");
    }

    /**
     * Builds a RapiPDFConfig given a set of properties.
     * @param properties A set of properties.
     * @return A RapipdfConfig.
     */
    static RapiPDFConfig fromProperties(Map<String, String> properties) {
        RapiPDFConfig cfg = new RapiPDFConfig();
        cfg.enabled = "true".equals(properties.getOrDefault("rapipdf.enabled", Boolean.FALSE.toString()));
        return AbstractViewConfig.fromProperties(cfg, DEFAULT_OPTIONS, properties);
    }

    /**
     * Substitute placeholders in the template.
     *
     * @param template A template.
     * @param rendererType The renderer type.
     * @return The template with placeholders replaced.
     */
    String render(String template, RendererType rendererType) {
        if (enabled) {
            String style = (String) options.get("style");
            boolean styleUpdated = false;
            if (style == null || style.trim().isEmpty()) {
                styleUpdated = true;
                // set default style
                if (RendererType.REDOC.equals(rendererType)) {
                    options.put("style", DEFAULT_REDOC_STYLE);
                } else if (RendererType.SWAGGER_UI.equals(rendererType)) {
                    options.put("style", DEFAULT_SWAGGER_UI_STYLE);
                } else {
                    options.put("style", DEFAULT_RAPIDOC_STYLE);
                }
            }
            String script = OpenApiViewConfig.replacePlaceHolder(LINK, "rapipdf.version", version, "@");
            String rapipdfTag = OpenApiViewConfig.replacePlaceHolder(TAG, "rapipdf.attributes", toHtmlAttributes(), "");
            if (styleUpdated) {
                options.remove("style");
            }
            template = OpenApiViewConfig.replacePlaceHolder(template, "rapipdf.script", script, "");
            template = OpenApiViewConfig.replacePlaceHolder(template, "rapipdf.specurl", SPEC, "");
            return OpenApiViewConfig.replacePlaceHolder(template, "rapipdf.tag", rapipdfTag, "");
        } else {
            template = OpenApiViewConfig.replacePlaceHolder(template, "rapipdf.script", "", "");
            template = OpenApiViewConfig.replacePlaceHolder(template, "rapipdf.specurl", "", "");
            return OpenApiViewConfig.replacePlaceHolder(template, "rapipdf.tag", "", "");
        }
    }

    @Override
    protected Function<String, Object> getConverter(String key) {
        return VALID_OPTIONS.get(key);
    }
}
