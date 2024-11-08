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
import io.micronaut.core.util.Toggleable;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.ConfigUtils;
import io.micronaut.openapi.visitor.Pair;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.micronaut.openapi.visitor.ConfigUtils.getGroupProperties;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH;
import static io.micronaut.openapi.visitor.StringUtil.PLACEHOLDER_POSTFIX;
import static io.micronaut.openapi.visitor.StringUtil.PLACEHOLDER_PREFIX;
import static io.micronaut.openapi.visitor.StringUtil.QUOTE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;

/**
 * Abstract View Config.
 *
 * @author croudet
 */
abstract class AbstractViewConfig implements Toggleable {

    protected String prefix;
    protected String jsUrl = StringUtils.EMPTY_STRING;
    protected String specUrl;
    /**
     * URL prefix from config properties.
     */
    protected String urlPrefix;
    /**
     * URL prefix for templates and resources.
     */
    protected String fullUrlPrefix;
    protected String resourcesContextPath = "/res";
    protected String templatePath;
    protected boolean isDefaultJsUrl = true;
    protected boolean copyResources = true;
    protected boolean withFinalUrlPrefixCache = true;
    protected String primaryName;
    protected Boolean withUrls;
    protected List<OpenApiUrl> urls = new ArrayList<>();
    protected Map<String, Object> options = new HashMap<>();
    @Nullable
    protected Map<Pair<String, String>, OpenApiInfo> openApiInfos;

    /**
     * An AbstractViewConfig.
     *
     * @param prefix The configuration key prefix.
     * @param openApiInfos Information about all generated openAPI files.
     */
    protected AbstractViewConfig(String prefix, Map<Pair<String, String>, OpenApiInfo> openApiInfos) {
        this.prefix = prefix;
        this.openApiInfos = openApiInfos;
    }

    /**
     * Returns the converter associated with the key.
     *
     * @param key A key.
     *
     * @return A converter or null.
     */
    protected abstract Function<String, Object> getConverter(String key);

    protected abstract List<String> getResources();

    public String getTemplatePath() {
        return templatePath;
    }

    public abstract String render(String template, @Nullable VisitorContext context);

    /**
     * Adds an option.
     *
     * @param entry The user specified entry.
     */
    protected void addAttribute(Map.Entry<String, String> entry) {
        String key = entry.getKey().substring(prefix.length());
        Function<String, Object> converter = getConverter(key);
        if (converter != null) {
            options.put(key, converter.apply(entry.getValue()));
        }
    }

    /**
     * Converts to html attributes.
     *
     * @return A String.
     */
    protected String toHtmlAttributes() {
        return options.entrySet().stream()
            .map(e -> e.getKey() + "=\"" + e.getValue() + '"')
            .collect(Collectors.joining(" "));
    }

    protected String getFinalUrlPrefix(OpenApiViewConfig.RendererType rendererType, VisitorContext context) {
        if (fullUrlPrefix != null && withFinalUrlPrefixCache) {
            return fullUrlPrefix;
        }

        // process micronaut.openapi.server.context.path
        String serverContextPath = ConfigUtils.getConfigProperty(MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, context);
        if (serverContextPath == null) {
            serverContextPath = StringUtils.EMPTY_STRING;
        }
        String finalUrl = serverContextPath.startsWith(SLASH) ? serverContextPath : SLASH + serverContextPath;
        if (!finalUrl.endsWith(SLASH)) {
            finalUrl += SLASH;
        }

        // process micronaut.server.context-path
        String contextPath = ConfigUtils.getServerContextPath(context);
        finalUrl += contextPath.startsWith(SLASH) ? contextPath.substring(1) : contextPath;
        if (!finalUrl.endsWith(SLASH)) {
            finalUrl += SLASH;
        }

        urlPrefix = finalUrl;

        // standard path
        finalUrl += rendererType.getTemplatePath();
        finalUrl += finalUrl.endsWith(SLASH) ? resourcesContextPath.substring(1) : resourcesContextPath;
        if (!finalUrl.endsWith(SLASH)) {
            finalUrl += SLASH;
        }

        fullUrlPrefix = finalUrl;

        return finalUrl;
    }

    /**
     * Builds and parse a View Config.
     *
     * @param <T> A View config type.
     * @param cfg A View config.
     * @param defaultOptions The default options.
     * @param properties The options to parse.
     * @param rendererType The render type.
     * @param context Visitor context.
     *
     * @return A View config.
     */
    static <T extends AbstractViewConfig> T fromProperties(T cfg, Map<String, Object> defaultOptions, Map<String, String> properties, OpenApiViewConfig.RendererType rendererType, VisitorContext context) {

        String copyResources = properties.get(cfg.prefix + "copy-resources");
        if (StringUtils.isNotEmpty(copyResources) && "false".equalsIgnoreCase(copyResources)) {
            cfg.copyResources = false;
        }

        cfg.withUrls = cfg.openApiInfos != null && (cfg.openApiInfos.size() > 1 || cfg.openApiInfos.get(Pair.NULL_STRING_PAIR) == null);

        if (cfg.withUrls) {

            String primaryName = null;
            var urls = new ArrayList<OpenApiUrl>();
            for (OpenApiInfo openApiInfo : cfg.openApiInfos.values()) {
                String groupName = openApiInfo.getGroupName();
                String version = openApiInfo.getVersion();
                if (StringUtils.isEmpty(groupName) && StringUtils.isEmpty(version)) {
                    continue;
                }

                if (StringUtils.isEmpty(groupName)) {
                    groupName = version;
                }

                GroupProperties groupProperties = getGroupProperties(groupName, context);
                if (groupProperties != null) {
                    if (groupProperties.getDisplayName() != null) {
                        groupName = groupProperties.getDisplayName();
                    }
                    if (groupProperties.getPrimary() != null && groupProperties.getPrimary()) {
                        primaryName = groupName;
                    }
                }

                cfg.getFinalUrlPrefix(OpenApiViewConfig.RendererType.SWAGGER_UI, context);
                String groupUrl = cfg.urlPrefix + (!cfg.urlPrefix.endsWith(SLASH) ? "/swagger/" : "swagger/") + openApiInfo.getFilename();
                urls.add(new OpenApiUrl(groupUrl, groupName));
            }
            cfg.urls = urls;
            if (primaryName != null) {
                cfg.primaryName = primaryName;
            }
        } else {
            String specUrl = properties.get(cfg.prefix + "spec.url");
            if (specUrl != null) {

                String filenameFromContext = null;
                if (context != null && cfg.openApiInfos != null) {
                    filenameFromContext = cfg.openApiInfos.get(Pair.NULL_STRING_PAIR).getFilename();
                }

                cfg.specUrl = specUrl.replace(PLACEHOLDER_PREFIX + "filename" + PLACEHOLDER_POSTFIX,
                    filenameFromContext != null ? filenameFromContext : StringUtils.EMPTY_STRING);
            }
        }

        String jsUrl = properties.get(cfg.prefix + "js.url");
        if (StringUtils.isNotEmpty(jsUrl)) {
            cfg.jsUrl = jsUrl;
            cfg.isDefaultJsUrl = false;
        } else {
            String resourcesContextPath = properties.get(cfg.prefix + "resources.context.path");
            if (StringUtils.isNotEmpty(resourcesContextPath)) {
                cfg.resourcesContextPath = resourcesContextPath.startsWith(SLASH) ? resourcesContextPath : SLASH + resourcesContextPath;
            }
        }

        String templatePath = properties.get(cfg.prefix + "template.path");
        if (StringUtils.isNotEmpty(templatePath)) {
            cfg.templatePath = templatePath;
        }

        cfg.options.putAll(defaultOptions);
        for (var entry : properties.entrySet()) {
            if (entry.getKey().startsWith(cfg.prefix)) {
                cfg.addAttribute(entry);
            }
        }
        return cfg;
    }

    public List<OpenApiUrl> getUrls() {
        return urls;
    }

    /**
     * Converts to a Boolean.
     *
     * @param v The input.
     *
     * @return A Boolean.
     */
    static Object asBoolean(String v) {
        return Boolean.valueOf(v);
    }

    /**
     * Converts to an Integer.
     *
     * @param v The input.
     *
     * @return An Integer.
     */
    static Object asInt(String v) {
        return Integer.valueOf(v);
    }

    /**
     * Converts to a String.
     *
     * @param v The input.
     *
     * @return A String.
     */
    static Object asString(String v) {
        return v;
    }

    /**
     * Converts to a quoted String.
     *
     * @param v The input.
     *
     * @return A quoted String.
     */
    static Object asQuotedString(String v) {
        return v == null ? null : QUOTE + v + QUOTE;
    }

    /**
     * Converts to an enum.
     *
     * @param <T> An Enum class.
     *
     * @author croudet
     */
    static class EnumConverter<T extends Enum<T>> implements Function<String, Object> {

        private final Class<T> type;

        /**
         * EnumConverter.
         *
         * @param type An Enum type.
         */
        EnumConverter(Class<T> type) {
            this.type = type;
        }

        /**
         * Converts to an Enum.
         */
        @Override
        public Object apply(String v) {
            return v == null ? null : Enum.valueOf(type, v.toUpperCase(Locale.US));
        }
    }

    record OpenApiUrl(String url, String name) {
    }
}
