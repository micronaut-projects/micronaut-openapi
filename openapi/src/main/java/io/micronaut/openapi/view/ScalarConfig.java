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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.view.OpenApiViewConfig.RendererType;
import io.micronaut.openapi.visitor.Pair;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.micronaut.core.util.StringUtils.EMPTY_STRING;
import static io.micronaut.openapi.view.OpenApiViewConfig.replacePlaceHolder;
import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.COMMA_NEW_LINE;
import static io.micronaut.openapi.visitor.StringUtil.KEY_VALUE_SEPARATOR;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;

/**
 * Scalar configuration.
 */
public final class ScalarConfig extends AbstractViewConfig {

    public static final String SCALAR_PREFIX = "scalar.";
    private static final String DEFAULT_SCALAR_JS_PATH = OpenApiViewConfig.RESOURCE_DIR + SLASH;

    private static final List<String> RESOURCE_FILES = List.of(
        DEFAULT_SCALAR_JS_PATH + "standalone.js"
    );

    private static final Map<String, Object> DEFAULT_OPTIONS = new HashMap<>();

    // https://github.com/scalar/scalar/blob/main/documentation/configuration.md
    private static final Map<String, Function<String, Object>> VALID_OPTIONS = new HashMap<>(61);

    static {
        VALID_OPTIONS.put("customDomain", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("theme", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("isEditable", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("spec", AbstractViewConfig::asString);
        VALID_OPTIONS.put("proxyUrl", AbstractViewConfig::asString);
        VALID_OPTIONS.put("showSidebar", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hideModels", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hideDownloadButton", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hideTestRequestButton", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hideSearch", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("darkMode", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("forceDarkModeState", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("hideDarkModeToggle", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("customCss", AbstractViewConfig::asString);
        VALID_OPTIONS.put("searchHotKey", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("baseServerURL", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("servers", AbstractViewConfig::asString);
        VALID_OPTIONS.put("metadata", AbstractViewConfig::asString);
        VALID_OPTIONS.put("favicon", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("defaultHttpClient", AbstractViewConfig::asString);
        VALID_OPTIONS.put("hiddenClients", AbstractViewConfig::asString);
        VALID_OPTIONS.put("onSpecUpdate", AbstractViewConfig::asString);
        VALID_OPTIONS.put("authentication", AbstractViewConfig::asString);
        VALID_OPTIONS.put("generateHeadingSlug", AbstractViewConfig::asString);
        VALID_OPTIONS.put("generateModelSlug", AbstractViewConfig::asString);
        VALID_OPTIONS.put("generateTagSlug", AbstractViewConfig::asString);
        VALID_OPTIONS.put("generateOperationSlug", AbstractViewConfig::asString);
        VALID_OPTIONS.put("generateWebhookSlug", AbstractViewConfig::asString);
        VALID_OPTIONS.put("onLoaded", AbstractViewConfig::asString);
        VALID_OPTIONS.put("withDefaultFonts", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("defaultOpenAllTags", AbstractViewConfig::asBoolean);
        VALID_OPTIONS.put("tagsSorter", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("operationsSorter", AbstractViewConfig::asQuotedString);
        VALID_OPTIONS.put("hideClientButton", AbstractViewConfig::asBoolean);

        DEFAULT_OPTIONS.put("defaultHttpClient", "{targetKey: 'shell', clientKey: 'curl'}");
        // by default, we have only shell curl (curl), Invoke-WebRequest Powershell (webrequest), java.net.http Java (nethttp), Fetch JavaScript (fetch), Axios JavaScript (axios)
        DEFAULT_OPTIONS.put("hiddenClients", "['libcurl', 'clj_http', 'httpclient', 'restsharp', 'native', 'http1.1', 'asynchttp', 'okhttp', 'unirest', 'xhr', 'jquery', 'native', 'request', 'unirest', 'ofetch', 'nsurlsession', 'cohttp', 'guzzle', 'http', 'http1', 'http2', 'restmethod', 'python3', 'requests', 'httr', 'native', 'curl', 'httpie', 'wget', 'nsurlsession', 'undici'],");
    }

    String style = StringUtils.EMPTY_STRING;
    RapiPDFConfig rapiPDFConfig;

    private ScalarConfig(Map<Pair<String, String>, OpenApiInfo> openApiInfos) {
        super(SCALAR_PREFIX, openApiInfos);
        jsUrl = DEFAULT_SCALAR_JS_PATH;
    }

    private String toOptions() {
        return options.entrySet().stream()
            .filter(e -> VALID_OPTIONS.containsKey(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + KEY_VALUE_SEPARATOR + e.getValue())
            .collect(Collectors.joining(COMMA_NEW_LINE));
    }

    /**
     * Builds ScalarConfig given a set of properties.
     *
     * @param properties A set of properties.
     * @param openApiInfos Open API info objects.
     * @param context Visitor context.
     *
     * @return A ScalarConfig.
     */
    static ScalarConfig fromProperties(Map<String, String> properties, Map<Pair<String, String>, OpenApiInfo> openApiInfos, VisitorContext context) {
        var cfg = new ScalarConfig(openApiInfos);
        cfg.style = properties.get(cfg.prefix + "style");
        return AbstractViewConfig.fromProperties(cfg, DEFAULT_OPTIONS, properties, RendererType.SCALAR, context);
    }

    @Override
    public String render(String template, @Nullable VisitorContext context) {
        template = rapiPDFConfig.render(template, RendererType.SCALAR, context);
        template = replacePlaceHolder(template, "scalar.js.url.prefix", isDefaultJsUrl ? getFinalUrlPrefix(RendererType.SCALAR, context) : jsUrl);
        template = replacePlaceHolder(template, "style", StringUtils.isNotEmpty(style) ? "<style>" + style + "</style>" : EMPTY_STRING);
        template = replacePlaceHolder(template, "scalar.sources", getSourcesStr());
        return replacePlaceHolder(template, "scalar.attributes", toOptions());
    }

    @NonNull
    private String getSourcesStr() {
        var result = new StringBuilder("sources: [");
        // case with single document
        if (CollectionUtils.isEmpty(urls) || (withUrls != null && !withUrls)) {
            result.append("{url: contextPath + \"").append(specUrl).append("\"}");
        } else {
            // case with multiple documents
            var isFirst = true;
            for (var url : urls) {
                if (!isFirst) {
                    result.append(',');
                }
                result.append(OPEN_BRACE)
                    .append("title: \"").append(url.name()).append("\",")
                    .append("url: contextPath + \"").append(url.url()).append("\",");
                if (StringUtils.isNotEmpty(primaryName) && primaryName.equals(url.name())) {
                    result.append("default: true");
                }
                result.append(CLOSE_BRACE);
                isFirst = false;
            }
        }
        result.append("],");
        return result.toString();
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
