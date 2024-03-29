/*
 * Copyright 2017-2024 original authors
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

import io.micronaut.core.annotation.Internal;

/**
 * String utilities.
 *
 * @since 6.7.0
 */
@Internal
public final class StringUtil {

    public static final String PLACEHOLDER_PREFIX = "${";
    public static final String PLACEHOLDER_POSTFIX = "}";

    public static final String THREE_DOTS = "...";

    public static final String OPEN_BRACE = "{";
    public static final String CLOSE_BRACE = "}";
    public static final String SLASH = "/";
    public static final String DOLLAR = "$";
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String UNDERSCORE = "_";
    public static final String MINUS = "-";

    private StringUtil() {
    }
}
