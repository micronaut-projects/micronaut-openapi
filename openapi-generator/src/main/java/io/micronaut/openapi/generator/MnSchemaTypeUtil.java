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
package io.micronaut.openapi.generator;

/**
 * Schema type utils and constants.
 *
 * @since 6.12.4
 */
public final class MnSchemaTypeUtil {

    public static final String TYPE_CHAR = "char";
    public static final String TYPE_CHARACTER = "character";
    public static final String TYPE_BYTE = "byte";
    public static final String TYPE_SHORT = "short";
    public static final String TYPE_INT = "int";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_DOUBLE = "double";

    public static final String FORMAT_INT8 = "int8";
    public static final String FORMAT_INT16 = "int16";
    public static final String FORMAT_SHORT = "short";

    private MnSchemaTypeUtil() {
    }
}
