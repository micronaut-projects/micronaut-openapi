/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.openapi.visitor;

import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_CSV;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_DEEP_OBJECT;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_MULTI;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_PIPES;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_SSV;

/**
 * Micronaut format possible values.
 *
 * @see io.micronaut.core.convert.converters.MultiValuesConverterFactory
 * @since 6.16.0
 */
public enum MnParamFormat {

    CSV(FORMAT_CSV, StyleEnum.SIMPLE),
    SSV(FORMAT_SSV, StyleEnum.SPACEDELIMITED),
    PIPES(FORMAT_PIPES, StyleEnum.PIPEDELIMITED),
    MULTI(FORMAT_MULTI, StyleEnum.FORM),
    DEEP_OBJECT(FORMAT_DEEP_OBJECT, StyleEnum.DEEPOBJECT),
    ;

    public static final Map<String, MnParamFormat> BY_NAME = Map.copyOf(Arrays.stream(values())
        .collect(Collectors.toMap(v -> v.name, Function.identity())));

    private final String name;
    private final StyleEnum style;

    MnParamFormat(String name, StyleEnum style) {
        this.name = name;
        this.style = style;
    }

    public String getName() {
        return name;
    }

    public StyleEnum getStyle() {
        return style;
    }

    public static StyleEnum getStyleByFormatName(String name, String in) {
        var format = BY_NAME.get(name);
        // See https://github.com/OAI/OpenAPI-Specification/blob/3.0.4/versions/3.0.4.md#style-values
        if ("query".equalsIgnoreCase(in) && format != MULTI && format != SSV && format != PIPES && format != DEEP_OBJECT) {
            return null;
        } else if ("cookie".equalsIgnoreCase(in) && format != MULTI) {
            return null;
        } else if ("header".equalsIgnoreCase(in) && format != CSV) {
            return null;
        } else if ("path".equalsIgnoreCase(in) && format != CSV) {
            return null;
        }
        // ignore default values
        // https://github.com/OAI/OpenAPI-Specification/blob/3.0.4/versions/3.0.4.md#fixed-fields-for-use-with-schema
        if (format != null) {
            // default value for query is form
            if ("query".equalsIgnoreCase(in) && format == MULTI) {
                return null;
                // default value for cookie is form
            } else if ("cookie".equalsIgnoreCase(in) && format == MULTI) {
                return null;
                // default value for cookie is simple
            } else if ("header".equalsIgnoreCase(in) && format == CSV) {
                return null;
                // default value for cookie is simple
            } else if ("path".equalsIgnoreCase(in) && format == CSV) {
                return null;
            }
        }
        return format != null ? format.getStyle() : null;
    }

    public static MnParamFormat getByName(String name) {
        return BY_NAME.get(name);
    }
}
