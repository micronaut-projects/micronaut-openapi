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

import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenModelFactory;
import org.openapitools.codegen.CodegenModelType;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Utilities methods to generators.
 *
 * @since 5.2.0
 */
public final class Utils {

    public static final List<String> DATE_TIME_TYPES = List.of(
        "date",
        "Date",
        "DateTime",
        "LocalDateTime",
        "OffsetDateTime",
        "ZonedDateTime",
        "LocalDate",
        "LocalTime"
    );
    public static final String DIVIDE_OPERATIONS_BY_CONTENT_TYPE = "divideOperationsByContentType";

    public static final String DEFAULT_BODY_PARAM_NAME = "requestBody";
    public static final String EXT_ANNOTATIONS_OPERATION = "x-operation-extra-annotation";
    public static final String EXT_ANNOTATIONS_CLASS = "x-class-extra-annotation";
    public static final String EXT_ANNOTATIONS_FIELD = "x-field-extra-annotation";
    public static final String EXT_ANNOTATIONS_SETTER = "x-setter-extra-annotation";

    public static final String NULL_STRING = "null";

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    /**
     * Replace multipart data parameters, marked {@literal @}Part annotation to single MultipartBody parameter.
     *
     * @param op operation
     * @param params parameters
     * @param isKotlin is kotlin generator
     *
     * @return Pair with MultipartBody parameter and set of removed parameter names
     */
    public static Pair<CodegenParameter, Set<String>> processMultipartBody(CodegenOperation op, Collection<CodegenParameter> params, boolean isKotlin) {

        var removedParams = new HashSet<String>();
        CodegenParameter multipartBodyParam = null;
        var i = params.iterator();
        while (i.hasNext()) {
            var param = i.next();
            if (param.isBodyParam && param.vendorExtensions.containsKey("isPart")) {
                if (multipartBodyParam == null) {
                    multipartBodyParam = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
                    multipartBodyParam.isBodyParam = true;
                    multipartBodyParam.baseName = "multipartBody";
                    multipartBodyParam.paramName = "multipartBody";
                    multipartBodyParam.dataType = "MultipartBody";
                    multipartBodyParam.baseType = "MultipartBody";
                    multipartBodyParam.nameInCamelCase = "multipartBody";
                    multipartBodyParam.nameInLowerCase = "multipartbody";
                    multipartBodyParam.nameInPascalCase = "MultipartBody";
                    multipartBodyParam.nameInSnakeCase = "MULTIPART_BODY";
                    multipartBodyParam.vendorExtensions.put("typeWithGenericAnnotations", "MultipartBody" + (isKotlin ? "?" : ""));
                    multipartBodyParam.vendorExtensions.put("typeWithEnumWithGenericAnnotations", "MultipartBody" + (isKotlin ? "?" : ""));
                    op.imports.add("MultipartBody");
                }
                removedParams.add(param.paramName);
                i.remove();
            }
        }
        if (multipartBodyParam != null) {
            params.add(multipartBodyParam);
        }

        return Pair.of(multipartBodyParam, removedParams);
    }

    public static void processGenericAnnotations(CodegenParameter parameter, boolean useBeanValidation, boolean isGenerateHardNullable,
                                                 boolean isNullable, boolean isRequired, boolean isReadonly, boolean withNullablePostfix,
                                                 boolean isKotlin, boolean ksp) {
        var ext = parameter.vendorExtensions;
        if (!ext.containsKey("isPrimitiveArray")) {
            ext.put("isPrimitiveArray", isJavaPrimitiveArray(parameter.dataType) || isKotlinPrimitiveArray(parameter.dataType));
        }
        CodegenProperty items = parameter.isMap ? parameter.additionalProperties : parameter.items;
        String datatypeWithEnum = parameter.datatypeWithEnum == null ? parameter.dataType : parameter.datatypeWithEnum;
        processGenericAnnotations(parameter.dataType, datatypeWithEnum, parameter.isMap, parameter.containerTypeMapped,
            items, parameter.vendorExtensions, useBeanValidation, isGenerateHardNullable, isNullable, isRequired, isReadonly, withNullablePostfix, isKotlin, ksp);

        if (parameter.items != null) {
            processGenericAnnotations(parameter.items, useBeanValidation, isGenerateHardNullable, isNullable, isRequired, isReadonly, withNullablePostfix, isKotlin, ksp);
        }
    }

    public static void processGenericAnnotations(CodegenProperty property, boolean useBeanValidation, boolean isGenerateHardNullable,
                                                 boolean isNullable, boolean isRequired, boolean isReadonly, boolean withNullablePostfix,
                                                 boolean isKotlin, boolean ksp) {
        var ext = property.vendorExtensions;
        if (!ext.containsKey("isPrimitiveArray")) {
            ext.put("isPrimitiveArray", isJavaPrimitiveArray(property.dataType) || isKotlinPrimitiveArray(property.dataType));
        }
        CodegenProperty items = property.isMap ? property.additionalProperties : property.items;
        String datatypeWithEnum = property.datatypeWithEnum == null ? property.dataType : property.datatypeWithEnum;
        processGenericAnnotations(property.dataType, datatypeWithEnum, property.isMap, property.containerTypeMapped,
            items, ext, useBeanValidation, isGenerateHardNullable, isNullable, isRequired, isReadonly, withNullablePostfix, isKotlin, ksp);

        if (property.items != null) {
            processGenericAnnotations(property.items, useBeanValidation, isGenerateHardNullable, isNullable, isRequired, isReadonly, withNullablePostfix, isKotlin, ksp);
        }
    }

    public static void processGenericAnnotations(String dataType, String dataTypeWithEnum, boolean isMap, String containerType, CodegenProperty itemsProp, Map<String, Object> ext,
                                                 boolean useBeanValidation, boolean isGenerateHardNullable, boolean isNullable,
                                                 boolean isRequired, boolean isReadonly,
                                                 boolean withNullablePostfix, boolean isKotlin, boolean ksp) {
        String genericAnnotations = null;
        dataType = addAnnotationsToPrimitiveArray(dataType, isNullable, isRequired, isReadonly, isGenerateHardNullable);
        dataTypeWithEnum = addAnnotationsToPrimitiveArray(dataTypeWithEnum, isNullable, isRequired, isReadonly, isGenerateHardNullable);
        var typeWithGenericAnnotations = dataType;
        var typeWithEnumWithGenericAnnotations = dataTypeWithEnum;
        if (useBeanValidation && itemsProp != null && dataType.contains("<")) {
            genericAnnotations = genericAnnotations(itemsProp, isGenerateHardNullable, isKotlin, ksp);
            processGenericAnnotations(itemsProp, useBeanValidation, isGenerateHardNullable, itemsProp.isNullable, itemsProp.required, itemsProp.isReadOnly, withNullablePostfix, isKotlin, ksp);
            if (isMap) {
                typeWithGenericAnnotations = "Map<String, " + genericAnnotations + itemsProp.vendorExtensions.get("typeWithGenericAnnotations") + ">";
                typeWithEnumWithGenericAnnotations = "Map<String, " + genericAnnotations + itemsProp.vendorExtensions.get("typeWithEnumWithGenericAnnotations") + ">";
            } else if (containerType != null) {
                typeWithGenericAnnotations = containerType + "<" + genericAnnotations + itemsProp.vendorExtensions.get("typeWithGenericAnnotations") + ">";
                typeWithEnumWithGenericAnnotations = containerType + "<" + genericAnnotations + itemsProp.vendorExtensions.get("typeWithEnumWithGenericAnnotations") + ">";
            }
        }

        var isNullableType = withNullablePostfix && (isNullable || isRequired && isReadonly || (genericAnnotations != null && !genericAnnotations.contains("NotNull") && !isRequired));
        // properties with default value are optional, but it works fine only with KSP
        if (ksp) {
            var hasDefaultValue = (Boolean) ext.get("defaultValueIsNotNull");
            isNullableType = isNullableType && (hasDefaultValue == null || !hasDefaultValue);
        }

        ext.put("typeWithGenericAnnotations", typeWithGenericAnnotations + (isNullableType ? "?" : ""));
        ext.put("typeWithEnumWithGenericAnnotations", typeWithEnumWithGenericAnnotations + (isNullableType ? "?" : ""));
    }

    private static String addAnnotationsToPrimitiveArray(String dataType, boolean isNullable, boolean isRequired, boolean isReadOnly, boolean isGenerateHardNullable) {
        if (!isJavaPrimitiveArray(dataType)) {
            return dataType;
        }

        var type = dataType.substring(0, dataType.indexOf('['));

        if (isNullable || !isRequired || isReadOnly) {
            if (isGenerateHardNullable) {
                return type + " @Nullable(inherited = true) []";
            } else {
                return type + " @Nullable []";
            }
        } else {
            return type + " @NotNull []";
        }
    }

    private static boolean isJavaPrimitiveArray(String dataType) {
        if (dataType == null) {
            return false;
        }
        return switch (dataType) {
            case "byte[]", "short[]", "int[]", "long[]", "boolean[]", "char[]", "float[]", "double[]" -> true;
            default -> false;
        };
    }

    private static boolean isKotlinPrimitiveArray(String dataType) {
        if (dataType == null) {
            return false;
        }
        return switch (dataType) {
            case "ByteArray", "ShortArray", "IntArray", "LongArray", "BooleanArray", "CharArray", "FloatArray", "DoubleArray" -> true;
            default -> false;
        };
    }

    private static String genericAnnotations(CodegenProperty prop, boolean isGenerateHardNullable, boolean isKotlin, boolean ksp) {

        var type = prop.openApiType == null ? null : prop.openApiType.toLowerCase();

        var isPrimitiveArray = isJavaPrimitiveArray(prop.dataType) || isKotlinPrimitiveArray(prop.dataType);

        var result = new StringBuilder();

        if (prop.isModel) {
            result.append("@Valid ");
        }
        if (!isPrimitive(type)) {
            return result.toString();
        }

        var patternMsg = (String) prop.vendorExtensions.get("x-pattern-message");
        var sizeMsg = (String) prop.vendorExtensions.get("x-size-message");
        var notNullMsg = (String) prop.vendorExtensions.get("x-not-null-message");
        var minMsg = (String) prop.vendorExtensions.get("x-minimum-message");
        var maxMsg = (String) prop.vendorExtensions.get("x-maximum-message");
        if (isKotlin) {
            patternMsg = normalizeStr(patternMsg);
            sizeMsg = normalizeStr(sizeMsg);
            notNullMsg = normalizeStr(notNullMsg);
            minMsg = normalizeStr(minMsg);
            maxMsg = normalizeStr(maxMsg);
        }

        if (StringUtils.isNotEmpty(prop.pattern) && !prop.isDate && !prop.isDateTime) {
            if ("email".equals(type) || "email".equalsIgnoreCase(prop.dataFormat) || prop.isEmail) {
                result.append("@Email(regexp = \"");
            } else {
                result.append("@Pattern(regexp = \"");
            }
            result.append(prop.pattern).append("\"");
            if (patternMsg != null) {
                result.append(", message = \"").append(patternMsg).append("\"");
            }
            result.append(") ");
        }

        var containsNotEmpty = false;

        if (prop.minLength != null || prop.maxLength != null) {
            if (prop.minLength != null && prop.minLength == 1 && prop.maxLength == null && !prop.isNullable) {
                result.append("@NotEmpty");
                if (sizeMsg != null) {
                    result.append("(message = \"").append(sizeMsg).append("\")");
                }
                result.append(' ');
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
                if (sizeMsg != null) {
                    result.append(", message = \"").append(sizeMsg).append('"');
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
                if (sizeMsg != null) {
                    result.append(", message = \"").append(sizeMsg).append('"');
                }
                result.append(") ");
            }
        }
        if (!isPrimitiveArray && !(boolean) prop.vendorExtensions.get("isPrimitive")) {
            if (prop.isNullable) {
                // properties with default value are optional, but it works fine only with KSP
                var hasDefaultValue = (Boolean) prop.vendorExtensions.get("defaultValueIsNotNull");
                if (!ksp || hasDefaultValue == null || !hasDefaultValue) {
                    if (isGenerateHardNullable) {
                        result.append("@Nullable(inherited = true) ");
                    } else {
                        result.append("@Nullable ");
                    }
                }
            } else if (!containsNotEmpty) {
                result.append("@NotNull");
                if (notNullMsg != null) {
                    result.append("(message = \"").append(notNullMsg).append("\")");
                }
                result.append(" ");
            }
        }
        if (StringUtils.isNotEmpty(prop.minimum)) {
            try {
                var longNumber = Long.parseLong(prop.minimum);
                if (prop.exclusiveMinimum) {
                    longNumber++;
                }
                if (longNumber == 0 && StringUtils.isEmpty(prop.maximum)) {
                    result.append("@PositiveOrZero");
                    if (minMsg != null) {
                        result.append("(message = \"").append(minMsg).append("\")");
                    }
                    result.append(" ");
                } else if (longNumber == 1 && StringUtils.isEmpty(prop.maximum)) {
                    result.append("@Positive");
                    if (minMsg != null) {
                        result.append("(message = \"").append(minMsg).append("\")");
                    }
                    result.append(" ");
                } else {
                    result.append("@Min(");
                    if (!isKotlin && minMsg != null) {
                        result.append("value = ");
                    }
                    result.append(longNumber);
                    if (prop.isLong) {
                        result.append("L");
                    }
                    if (minMsg != null) {
                        result.append(", message = \"").append(minMsg).append("\"");
                    }
                    result.append(") ");
                }
            } catch (Exception e) {
                result.append("@DecimalMin(");
                if (!isKotlin && (prop.exclusiveMinimum || minMsg != null)) {
                    result.append("value = ");
                }
                result.append('"').append(prop.minimum).append('"');
                if (prop.exclusiveMinimum) {
                    result.append(", inclusive = false");
                }
                if (minMsg != null) {
                    result.append(", message = \"").append(minMsg).append("\"");
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
                    result.append("@NegativeOrZero");
                    if (maxMsg != null) {
                        result.append("(message = \"").append(maxMsg).append("\")");
                    }
                    result.append(" ");
                } else if (longNumber == -1 && StringUtils.isEmpty(prop.minimum)) {
                    result.append("@Negative");
                    if (maxMsg != null) {
                        result.append("(message = \"").append(maxMsg).append("\")");
                    }
                    result.append(" ");
                } else {
                    result.append("@Max(");
                    if (!isKotlin && maxMsg != null) {
                        result.append("value = ");
                    }
                    result.append(longNumber);
                    if (prop.isLong) {
                        result.append("L");
                    }
                    if (maxMsg != null) {
                        result.append(", message = \"").append(maxMsg).append("\"");
                    }
                    result.append(") ");

                }
            } catch (Exception e) {
                result.append("@DecimalMax(");
                if (!isKotlin && (prop.exclusiveMaximum || maxMsg != null)) {
                    result.append("value = ");
                }
                result.append('"').append(prop.maximum).append('"');
                if (prop.exclusiveMaximum) {
                    result.append(", inclusive = false");
                }
                if (maxMsg != null) {
                    result.append(", message = \"").append(maxMsg).append("\"");
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
            case "array", "char", "character", "string", "boolean", "byte", "short", "int", "integer", "long", "uri", "url", "uuid", "email", "float",
                 "double", "number", "partial-time", "date", "date-time", "bigdecimal", "decimal", "biginteger" -> true;
            default -> false;
        };
    }

    public static void addStrValueToEnum(CodegenProperty property) {
        if (property == null || !property.isEnum || property.allowableValues == null) {
            return;
        }
        var enumVars = (List<Object>) property.allowableValues.get("enumVars");
        addStrValueToEnum(enumVars, property.isNumeric);
    }

    public static void addStrValueToEnum(CodegenModel model) {
        if (model == null || !model.isEnum || model.allowableValues == null) {
            return;
        }
        var enumVars = (List<Object>) model.allowableValues.get("enumVars");
        addStrValueToEnum(enumVars, model.isNumeric);
    }

    public static void addStrValueToEnum(List<Object> enumVars, boolean isNumeric) {
        for (var enumVar : enumVars) {
            var varMap = (Map<String, Object>) enumVar;
            var value = varMap.get("value").toString();
            if (value.startsWith("(short)")) {
                value = value.replace("(short) ", "");
            } else if (value.startsWith("(byte)")) {
                value = value.replace("(byte) ", "");
            }
            value = value.replace("'", "");
            if (isNumeric) {
                var argPos = value.indexOf('(');
                // case for BigDecimal
                if (argPos >= 0) {
                    value = value.substring(argPos + 1, value.indexOf(')'));
                }
                var upperValue = value.toUpperCase(Locale.ENGLISH);
                if (upperValue.endsWith("F")
                    || upperValue.endsWith("L")
                    || upperValue.endsWith("D")) {
                    value = value.substring(0, value.length() - 1);
                }
            }
            if (!value.contains("\"")) {
                value = "\"" + value + "\"";
            }
            varMap.put("strValue", value);
            varMap.put("strValueNormalized", normalizeStr(value));
        }
    }

    public static void normalizeStrValue(String key, Map<String, Object> exts) {
        if (exts.containsKey(key + "-alreadyNormalized")) {
            return;
        }
        var value = exts.get(key);
        if (value == null) {
            return;
        }
        exts.put(key, normalizeStr(value.toString()));
        exts.put(key + "-alreadyNormalized", true);
    }

    public static String normalizeStr(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("$", "\\$");
    }

    public static String toApiName(String name, String prefix, String suffix) {
        if (name.isEmpty()) {
            return "DefaultApi";
        }
        return prefix + name + suffix;
    }

    public static void normalizeExtraAnnotations(String extName, boolean isKotlin, Map<String, Object> vendorExtensions) {

        var ext = vendorExtensions.get(extName);
        if (ext == null) {
            return;
        }

        var prefix = "@";
        if (isKotlin) {
            if (extName.equals(EXT_ANNOTATIONS_FIELD)) {
                prefix = "@field:";
            } else if (extName.equals(EXT_ANNOTATIONS_SETTER)) {
                prefix = "@set:";
            }
        }

        if (ext instanceof Collection<?> annotations) {
            ext = normalizeExtraAnnotations(prefix, annotations);
        } else if (ext instanceof String annotationStr) {
            ext = normalizeExtraAnnotation(prefix, annotationStr);
        }

        vendorExtensions.put(extName, ext);
    }

    private static List<String> normalizeExtraAnnotations(String prefix, Collection<?> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return null;
        }
        var result = new ArrayList<String>(annotations.size());
        for (var annotation : annotations) {
            result.add(normalizeExtraAnnotation(prefix, annotation.toString()));
        }
        return result;
    }

    private static String normalizeExtraAnnotation(String prefix, String annotationStr) {
        if (annotationStr.startsWith(prefix)) {
            return annotationStr;
        }
        return prefix + (annotationStr.startsWith("@") ? annotationStr.substring(1) : annotationStr);
    }

    public static boolean isDateType(String type) {
        if (type == null) {
            return false;
        }
        return DATE_TIME_TYPES.contains(type);
    }

    public static List<String> readListOfStringsProperty(String property, Map<String, Object> additionalProperties) {
        var value = additionalProperties.get(property);
        if (value == null) {
            return Collections.emptyList();
        }
        return readListOfStringsProperty(value);
    }

    public static List<String> readListOfStringsProperty(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> parsedValues;
        if (value instanceof Collection<?> valueCol) {
            parsedValues = new ArrayList<>(valueCol.size());
            for (var el : valueCol) {
                var str = el.toString().strip();
                if (str.isEmpty()) {
                    continue;
                }
                parsedValues.add(str);
            }
        } else {
            var elements = value.toString().trim().split("\\s*(;|\\r?\\n)\\s*");
            parsedValues = new ArrayList<>(elements.length);
            for (var el : elements) {
                var str = el.strip();
                if (str.isEmpty()) {
                    continue;
                }
                parsedValues.add(str);
            }
        }
        return parsedValues;
    }

    public static boolean readBooleanProperty(String propertyKey, Map<String, Object> additionalProperties, boolean defaultValue) {
        var value = additionalProperties.get(propertyKey);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        } else if (value instanceof String strValue) {
            try {
                return Boolean.parseBoolean(strValue);
            } catch (NumberFormatException e) {
                LOG.warn("Can't parse generator's option {}. Value: {}", propertyKey, value);
                return defaultValue;
            }
        }
        LOG.warn("The value (generator's option {}) must be either boolean or string. Default to `{}`.", propertyKey, defaultValue);
        return defaultValue;
    }

    public static String calcQueryValueFormat(CodegenParameter param) {
        if (!param.isArray && !param.isMap && !param.isFreeFormObject) {
            return null;
        }
        if (!param.isQueryParam) {
            return null;
        }
        if (param.style == null || Parameter.StyleEnum.FORM.toString().equalsIgnoreCase(param.style)) {
            return param.isExplode ? "FORMAT_MULTI" : null; // Don't need to add FORMAT_CSV - it is default value
        } else if (Parameter.StyleEnum.SPACEDELIMITED.toString().equalsIgnoreCase(param.style)) {
            return "FORMAT_SSV";
        } else if (Parameter.StyleEnum.PIPEDELIMITED.toString().equalsIgnoreCase(param.style)) {
            return "FORMAT_PIPES";
        } else if (Parameter.StyleEnum.DEEPOBJECT.toString().equalsIgnoreCase(param.style)) {
            return "FORMAT_DEEP_OBJECT";
        }
        return null;
    }

    public static void processDuplicateVars(List<CodegenProperty> vars) {
        var names = new HashMap<String, Integer>();
        for (CodegenProperty var : vars) {
            var varName = var.getName();
            if (names.containsKey(varName)) {
                names.put(varName, names.get(varName) + 1);
                int index = names.get(varName);
                var.setName(varName + index);
                var.nameInSnakeCase = var.nameInSnakeCase + "_" + index;
            } else {
                names.put(varName, 1);
            }
        }
    }

    public static void addEnumParamsForConverters(
        String modelPackage,
        CodegenModel enumModel,
        CodegenParameter param,
        Map<String, Integer> converterCounters,
        List<CodegenParameter> enumParams,
        List<String> enumImports,
        boolean isKotlin
    ) {
        var converterName = param.dataType;
        var alreadyAdded = false;
        for (var enumParam : enumParams) {
            if (param.dataType.equals(enumParam.dataType)) {
                alreadyAdded = true;
                break;
            }
        }
        if (alreadyAdded) {
            return;
        }

        var counter = converterCounters.get(converterName);
        converterCounters.put(converterName, counter == null ? 0 : counter + 1);
        param.vendorExtensions.put("converterName", converterName + (counter != null ? counter : ""));

        var convertFun = (enumModel.isNullable ? "Optional.ofNullable(" : "Optional.of(")
            + param.dataType + ".fromValue(" + (isKotlin ? getKotlinEnumConvertFun(enumModel.dataType) : getJavaEnumConvertFun(enumModel.dataType, enumImports)) + "))";

        param.vendorExtensions.put("convertFun", convertFun);
        param.vendorExtensions.put("convertToStrFun", isKotlin ? getKotlinEnumConvertToStrFun(enumModel.dataType) : getJavaEnumConvertToStrFun((String) enumModel.vendorExtensions.get("baseType")));
        enumParams.add(param);
        enumImports.add(modelPackage + "." + param.dataType);
    }

    private static String getKotlinEnumConvertToStrFun(String type) {
        return "String".equals(type) ? "v.value" : "v.value.toString()";
    }

    private static String getJavaEnumConvertToStrFun(String type) {
        if ("String".equals(type)) {
            return "v.getValue()";
        } else if (
            "char".equals(type)
                || "byte".equals(type)
                || "short".equals(type)
                || "int".equals(type)
                || "long".equals(type)
                || "float".equals(type)
                || "double".equals(type)
                || "boolean".equals(type)
        ) {
            return "String.valueOf(v.getValue())";
        } else {
            return "v.getValue().toString()";
        }
    }

    private static String getKotlinEnumConvertFun(String type) {
        if ("Char".equals(type)) {
            return "v[0]";
        } else if ("Byte".equals(type)) {
            return "v.toByte()";
        } else if ("Short".equals(type)) {
            return "v.toShort()";
        } else if ("Int".equals(type)) {
            return "v.toInt()";
        } else if ("Long".equals(type)) {
            return "v.toLong()";
        } else if ("Float".equals(type)) {
            return "v.toFloat()";
        } else if ("Double".equals(type)) {
            return "v.toDouble()";
        } else if ("Boolean".equals(type)) {
            return "v.toBoolean()";
        } else if ("BigDecimal".equals(type)) {
            return "v.toBigDecimal()";
        } else if ("BigInteger".equals(type)) {
            return "v.toBigInteger()";
        }
        return "v";
    }

    private static String getJavaEnumConvertFun(String type, List<String> imports) {
        if ("Character".equals(type) || "char".equals(type)) {
            return "v.charAt(0)";
        } else if ("Byte".equals(type) || "byte".equals(type)) {
            return "Byte.valueOf(v)";
        } else if ("Short".equals(type) || "short".equals(type)) {
            return "Short.valueOf(v)";
        } else if ("Integer".equals(type) || "int".equals(type)) {
            return "Integer.valueOf(v)";
        } else if ("Long".equals(type) || "long".equals(type)) {
            return "Long.valueOf(v)";
        } else if ("Float".equals(type) || "float".equals(type)) {
            return "Float.valueOf(v)";
        } else if ("Double".equals(type) || "double".equals(type)) {
            return "Double.valueOf(v)";
        } else if ("Boolean".equals(type) || "boolean".equals(type)) {
            return "Boolean.valueOf(v)";
        } else if ("BigDecimal".equals(type)) {
            if (!imports.contains("java.math.BigDecimal")) {
                imports.add("java.math.BigDecimal");
            }
            return "new BigDecimal(v)";
        } else if ("BigInteger".equals(type)) {
            if (!imports.contains("java.math.BigInteger")) {
                imports.add("java.math.BigInteger");
            }
            return "new BigInteger(v)";
        }
        return "v";
    }

    public static boolean addUserParameter(CodegenOperation op, UserParameterMode userParameterMode, boolean isAnonymous, boolean isDenyAll) {

        if (userParameterMode == UserParameterMode.NONE || isDenyAll) {
            return false;
        }

        CodegenParameter userParam = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
        userParam.paramName = userParameterMode.getParamName();
        userParam.description = userParameterMode.getParamDescription();
        userParam.dataType = userParameterMode.getClassName();
        userParam.unescapedDescription = userParam.description;
        userParam.required = true;
        if (!isAnonymous) {
            userParam.vendorExtensions.put("withoutNullable", true);
        } else {
            userParam.isNullable = true;
            userParam.vendorExtensions.put("defaultValueInit", NULL_STRING);
        }

        op.allParams.add(userParam);

        return true;
    }
}
