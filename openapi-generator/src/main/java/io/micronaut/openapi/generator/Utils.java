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
package io.micronaut.openapi.generator;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenProperty;

/**
 * Utilities methods to generators.
 *
 * @since 5.2.0
 */
public final class Utils {

    private Utils() {
    }

    public static void processGenericAnnotations(String dataType, String dataTypeWithEnum, boolean isArray, CodegenProperty itemsProp, Map<String, Object> ext,
                                                 boolean useBeanValidation, boolean isGenerateHardNullable, boolean isNullable,
                                                 boolean isRequired, boolean isReadonly,
                                                 boolean withNullablePostfix) {
        var typeWithGenericAnnotations = dataType;
        var typeWithEnumWithGenericAnnotations = dataTypeWithEnum;
        var addedGenericAnnotations = false;
        if (useBeanValidation && isArray && itemsProp != null && isPrimitive(itemsProp.openApiType) && dataType.contains("<")) {
            var genericAnnotations = genericAnnotations(itemsProp, isGenerateHardNullable);
            if (itemsProp.isArray) {
                processGenericAnnotations(itemsProp.dataType, itemsProp.datatypeWithEnum, true, itemsProp.items, itemsProp.vendorExtensions,
                    useBeanValidation, isGenerateHardNullable, itemsProp.isNullable, itemsProp.required, itemsProp.isReadOnly, withNullablePostfix);
                typeWithGenericAnnotations = addGenericAnnotations((String) itemsProp.vendorExtensions.get("typeWithGenericAnnotations"), dataType, genericAnnotations);
                typeWithEnumWithGenericAnnotations = addGenericAnnotations((String) itemsProp.vendorExtensions.get("typeWithEnumWithGenericAnnotations"), dataTypeWithEnum, genericAnnotations);
            } else {
                typeWithGenericAnnotations = addGenericAnnotations(dataType, null, genericAnnotations);
                typeWithEnumWithGenericAnnotations = addGenericAnnotations(dataTypeWithEnum, null, genericAnnotations);
            }
        }

        ext.put("typeWithGenericAnnotations", typeWithGenericAnnotations + (withNullablePostfix && (isNullable || isRequired && isReadonly) ? "?" : ""));
        ext.put("typeWithEnumWithGenericAnnotations", typeWithEnumWithGenericAnnotations + (withNullablePostfix && (isNullable || isRequired && isReadonly) ? "?" : ""));
    }

    private static String genericAnnotations(CodegenProperty prop, boolean isGenerateHardNullable) {

        var type = prop.openApiType.toLowerCase();

        var result = new StringBuilder();
        if (StringUtils.isNotEmpty(prop.pattern)) {
            if (type.equals("email")) {
                result.append("@Email(regexp = \"");
            } else {
                result.append("@Pattern(regexp = \"");
            }
            result.append(prop.pattern).append("\") ");
        }

        var containsNotEmpty = false;

        if (prop.minLength != null || prop.maxLength != null) {
            if (prop.minLength != null && prop.minLength == 1 && prop.maxLength == null && !prop.isNullable) {
                result.append("@NotEmpty ");
                containsNotEmpty = true;
            } else {
                result.append("@Size(");
                if (prop.minLength != null) {
                    result.append("min = ").append(prop.minLength);
                }
                if (prop.maxLength != null) {
                    if (prop.minLength != null) {
                        result.append(", ");
                    }
                    result.append("max = ").append(prop.maxLength);
                }
                result.append(") ");
            }
        }

        if (prop.minItems != null || prop.maxItems != null) {
            if (prop.minItems != null && prop.minItems == 1 && prop.maxItems == null && !prop.isNullable) {
                result.append("@NotEmpty ");
                containsNotEmpty = true;
            } else {
                result.append("@Size(");
                if (prop.minItems != null) {
                    result.append("min = ").append(prop.minItems);
                }
                if (prop.maxItems != null) {
                    if (prop.minItems != null) {
                        result.append(", ");
                    }
                    result.append("max = ").append(prop.maxItems);
                }
                result.append(") ");
            }
        }
        if (prop.isNullable) {
            if (isGenerateHardNullable) {
                result.append("@HardNullable ");
            } else {
                result.append("@Nullable ");
            }
        } else if (!containsNotEmpty) {
            result.append("@NotNull ");
        }
        if (StringUtils.isNotEmpty(prop.minimum)) {
            try {
                var longNumber = Long.parseLong(prop.minimum);
                if (prop.exclusiveMinimum) {
                    longNumber++;
                }
                if (longNumber == 0 && StringUtils.isEmpty(prop.maximum)) {
                    result.append("@PositiveOrZero ");
                } else if (longNumber == 1 && StringUtils.isEmpty(prop.maximum)) {
                    result.append("@Positive ");
                } else {
                    result.append("@Min(").append(longNumber).append(") ");
                }
            } catch (Exception e) {
                result.append("@DecimalMin(");
                if (prop.exclusiveMinimum) {
                    result.append("value = ");
                }
                result.append('"').append(prop.minimum).append('"');
                if (prop.exclusiveMinimum) {
                    result.append(", inclusive = false");
                }
                result.append(") ");
            }
        }
        if (StringUtils.isNotEmpty(prop.maximum)) {
            try {
                var longNumber = Long.parseLong(prop.maximum);
                if (prop.exclusiveMaximum) {
                    longNumber--;
                }
                if (longNumber == 0 && StringUtils.isEmpty(prop.minimum)) {
                    result.append("@NegativeOrZero ");
                } else if (longNumber == -1 && StringUtils.isEmpty(prop.minimum)) {
                    result.append("@Negative ");
                } else {
                    result.append("@Max(").append(longNumber).append(") ");
                }
            } catch (Exception e) {
                result.append("@DecimalMax(");
                if (prop.exclusiveMaximum) {
                    result.append("value = ");
                }
                result.append('"').append(prop.maximum).append('"');
                if (prop.exclusiveMaximum) {
                    result.append(", inclusive = false");
                }
                result.append(") ");
            }
        }
        return result.toString();
    }

    public static String addGenericAnnotations(String type, String wrapType, String genericAnnotations) {
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(genericAnnotations)) {
            return type;
        }
        var t = wrapType != null ? wrapType : type;
        var diamondOpen = t.indexOf('<');
        var diamondClose = t.lastIndexOf('>');
        var containerType = t.substring(0, diamondOpen);
        var elementType = t.substring(diamondOpen + 1, diamondClose);
        return containerType + '<' + genericAnnotations + (wrapType != null ? type : elementType) + '>';
    }

    private static boolean isPrimitive(String type) {
        return switch (type.toLowerCase()) {
            case "array", "string", "boolean", "byte", "uri", "url", "uuid", "email", "integer", "long", "float", "double",
                "number", "partial-time", "date", "date-time", "bigdecimal", "biginteger" -> true;
            default -> false;
        };
    }
}
