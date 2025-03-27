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
import io.micronaut.core.util.StringUtils;

import static io.micronaut.core.util.StringUtils.EMPTY_STRING;

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
    public static final char SLASH_CHAR = '/';
    public static final String DOLLAR = "$";
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String UNDERSCORE = "_";
    public static final String MINUS = "-";
    public static final String WILDCARD = "*";
    public static final String QUOTE = "\"";
    public static final String KEY_VALUE_SEPARATOR = ": ";
    public static final String COMMA_NEW_LINE = ",\n";

    private StringUtil() {
    }

    /**
     * Return string with first capitalized letter and without braces.
     *
     * @param value path variable name with braces
     * @return capitalized path var name without braces
     */
    public static String capitalizedPathVar(final String value) {
        return StringUtils.capitalize(value.replaceAll("[{}]", EMPTY_STRING));
    }

    /**
     * Gets the leftmost {@code len} characters of a String.
     *
     * <p>If {@code len} characters are not available, or the
     * String is {@code null}, the String will be returned without
     * an exception. An empty String is returned if len is negative.</p>
     *
     * <pre>
     * StringUtil.left(null, *)    = null
     * StringUtil.left(*, -ve)     = ""
     * StringUtil.left("", *)      = ""
     * StringUtil.left("abc", 0)   = ""
     * StringUtil.left("abc", 2)   = "ab"
     * StringUtil.left("abc", 4)   = "abc"
     * </pre>
     *
     * @param str  the String to get the leftmost characters from, may be null
     * @param len  the length of the required String
     * @return the leftmost characters, {@code null} if null String input
     */
    public static String left(final String str, final int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return EMPTY_STRING;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }
}
