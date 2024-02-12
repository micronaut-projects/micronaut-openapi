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
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;

/**
 * Utilities methods to generators.
 *
 * @since 5.2.0
 */
public final class Utils {

    public static final String DEFAULT_BODY_PARAM_NAME = "requestBody";

    private Utils() {
    }

    public static void processGenericAnnotations(CodegenParameter parameter, boolean useBeanValidation, boolean isGenerateHardNullable,
                                           boolean isNullable, boolean isRequired, boolean isReadonly, boolean withNullablePostfix) {
        CodegenProperty items = parameter.isMap ? parameter.additionalProperties : parameter.items;
        String datatypeWithEnum = parameter.datatypeWithEnum == null ? parameter.dataType : parameter.datatypeWithEnum;
        processGenericAnnotations(parameter.dataType, datatypeWithEnum, parameter.isArray, parameter.isMap, parameter.containerTypeMapped,
            items, parameter.vendorExtensions, useBeanValidation, isGenerateHardNullable, isNullable, isRequired, isReadonly, withNullablePostfix);
    }

    public static void processGenericAnnotations(CodegenProperty property, boolean useBeanValidation, boolean isGenerateHardNullable,
                                           boolean isNullable, boolean isRequired, boolean isReadonly, boolean withNullablePostfix) {
        CodegenProperty items = property.isMap ? property.additionalProperties : property.items;
        String datatypeWithEnum = property.datatypeWithEnum == null ? property.dataType : property.datatypeWithEnum;
        processGenericAnnotations(property.dataType, datatypeWithEnum, property.isArray, property.isMap, property.containerTypeMapped,
            items, property.vendorExtensions, useBeanValidation, isGenerateHardNullable, isNullable, isRequired, isReadonly, withNullablePostfix);
    }

    public static void processGenericAnnotations(String dataType, String dataTypeWithEnum, boolean isArray, boolean isMap, String containerType, CodegenProperty itemsProp, Map<String, Object> ext,
                                                 boolean useBeanValidation, boolean isGenerateHardNullable, boolean isNullable,
                                                 boolean isRequired, boolean isReadonly,
                                                 boolean withNullablePostfix) {
        var typeWithGenericAnnotations = dataType;
        var typeWithEnumWithGenericAnnotations = dataTypeWithEnum;
        var addedGenericAnnotations = false;
        if (useBeanValidation && itemsProp != null && dataType.contains("<")) {
            if (isMap) {
                var genericAnnotations = genericAnnotations(itemsProp, isGenerateHardNullable);
                processGenericAnnotations(itemsProp, useBeanValidation, isGenerateHardNullable, itemsProp.isNullable, itemsProp.required, itemsProp.isReadOnly, withNullablePostfix);
                typeWithGenericAnnotations = "Map<String, " + genericAnnotations + itemsProp.vendorExtensions.get("typeWithGenericAnnotations") + ">";
                typeWithEnumWithGenericAnnotations = "Map<String, " + genericAnnotations + itemsProp.vendorExtensions.get("typeWithEnumWithGenericAnnotations") + ">";
            } else if (containerType != null) {
                var genericAnnotations = genericAnnotations(itemsProp, isGenerateHardNullable);
                processGenericAnnotations(itemsProp, useBeanValidation, isGenerateHardNullable, itemsProp.isNullable, itemsProp.required, itemsProp.isReadOnly, withNullablePostfix);
                typeWithGenericAnnotations = containerType + "<" + genericAnnotations + itemsProp.vendorExtensions.get("typeWithGenericAnnotations") + ">";
                typeWithEnumWithGenericAnnotations = containerType + "<" + genericAnnotations + itemsProp.vendorExtensions.get("typeWithEnumWithGenericAnnotations") + ">";
            }
        }

        ext.put("typeWithGenericAnnotations", typeWithGenericAnnotations + (withNullablePostfix && (isNullable || isRequired && isReadonly) ? "?" : ""));
        ext.put("typeWithEnumWithGenericAnnotations", typeWithEnumWithGenericAnnotations + (withNullablePostfix && (isNullable || isRequired && isReadonly) ? "?" : ""));
    }

    private static String genericAnnotations(CodegenProperty prop, boolean isGenerateHardNullable) {

        var type = prop.openApiType == null ? null : prop.openApiType.toLowerCase();

        var result = new StringBuilder();

        if (prop.isModel) {
            result.append("@Valid ");
        }
        if (!isPrimitive(type)) {
            return result.toString();
        }

        if (StringUtils.isNotEmpty(prop.pattern)) {
            if ("email".equals(type)) {
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
                result.append("@Nullable(inherited = true) ");
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

    private static boolean isPrimitive(String type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case "array", "string", "boolean", "byte", "uri", "url", "uuid", "email", "integer", "long", "float", "double",
                "number", "partial-time", "date", "date-time", "bigdecimal", "biginteger" -> true;
            default -> false;
        };
    }
}
