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
import java.util.stream.Collectors;

/**
 * Abstract View Config.
 *
 * @author croudet
 */
abstract class AbstractViewConfig {
    protected String prefix;
    protected String version = "";
    protected Map<String, Object> options = new HashMap<>();

    /**
     * An AbstractViewConfig.
     * @param prefix The configuration key prefix.
     */
    protected AbstractViewConfig(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the converter associated with the key.
     * @param key A key.
     * @return A converter or null.
     */
    protected abstract Function<String, Object> getConverter(String key);

    /**
     * Adds an option.
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
     * @return A String.
     */
    protected String toHtmlAttributes() {
        return options.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + '"')
                .collect(Collectors.joining(" "));
    }

    /**
     * Builds and parse a View Config.
     * @param <T> A View config type.
     * @param cfg A View config.
     * @param defaultOptions The default options.
     * @param properties The options to parse.
     * @return A View config.
     */
    static <T extends AbstractViewConfig> T fromProperties(T cfg, Map<String, Object> defaultOptions, Map<String, String> properties) {
        cfg.version = properties.getOrDefault(cfg.prefix + "version", cfg.version);
        cfg.options.putAll(defaultOptions);
        properties.entrySet().stream().filter(entry -> entry.getKey().startsWith(cfg.prefix))
            .forEach(cfg::addAttribute);
        return cfg;
    }

    /**
     * Converts to a Boolean.
     * @param v The input.
     * @return A Boolean.
     */
    static Object asBoolean(String v) {
        return Boolean.valueOf(v);
    }

    /**
     * Converts to a String.
     * @param v The input.
     * @return A String.
     */
    static Object asString(String v) {
        return v;
    }

    /**
     * Converts to a quoted String.
     * @param v The input.
     * @return A quoted String.
     */
    static Object asQuotedString(String v) {
        return v == null ? null : '"' + v + '"';
    }

    /**
     * Converts to an enum.
     *
     * @author croudet
     *
     * @param <T> An Enum class.
     */
    static class EnumConverter<T extends Enum<T>> implements Function<String, Object> {
        private final Class<T> type;

        /**
         * EnumConverter.
         * @param type An Enum type.
         */
        public EnumConverter(Class<T> type) {
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
}
