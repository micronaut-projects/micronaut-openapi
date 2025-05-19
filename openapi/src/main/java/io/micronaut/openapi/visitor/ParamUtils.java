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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.annotation.CookieValue;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.List;
import java.util.Set;

import static io.micronaut.openapi.visitor.ElementUtils.isIgnoredParameter;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.SchemaUtils.isIgnoredHeader;
import static io.micronaut.openapi.visitor.SchemaUtils.setSpecVersion;
import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;

/**
 * Parameter util methods.
 *
 * @since 6.16.0
 */
@Internal
public final class ParamUtils {

    private static final String DEFAULT_PARAM_VERSION_DESCRIPTION = "API version";

    private ParamUtils() {
    }

    public static String calcIn(String path, HttpMethod httpMethod, ParameterElement methodParam) {
        String paramName = methodParam.getName();
        Set<String> paramAnnNames = methodParam.getAnnotationNames();
        if (CollectionUtils.isNotEmpty(paramAnnNames)) {
            if (paramAnnNames.contains(QueryValue.class.getName())) {
                return ParameterIn.QUERY.toString();
            } else if (paramAnnNames.contains(PathVariable.class.getName())) {
                return ParameterIn.PATH.toString();
            } else if (paramAnnNames.contains(Header.class.getName())) {
                return ParameterIn.HEADER.toString();
            } else if (paramAnnNames.contains(CookieValue.class.getName())) {
                return ParameterIn.COOKIE.toString();
            }
        }
        if (httpMethod == HttpMethod.GET) {
            if (path.contains(OPEN_BRACE + paramName + CLOSE_BRACE)) {
                return ParameterIn.PATH.toString();
            } else {
                return ParameterIn.QUERY.toString();
            }
        } else {
            if (path.contains(OPEN_BRACE + paramName + CLOSE_BRACE)) {
                return ParameterIn.PATH.toString();
            }
        }

        return null;
    }

    public static Parameter.StyleEnum paramStyle(ParameterStyle paramAnnStyle, String in) {
        if (paramAnnStyle == null) {
            return null;
        }
        if (in != null) {
            if (in.equalsIgnoreCase(ParameterIn.HEADER.toString())) {
                return switch (paramAnnStyle) {
                    case SIMPLE -> Parameter.StyleEnum.SIMPLE;
                    case DEFAULT, DEEPOBJECT, PIPEDELIMITED, SPACEDELIMITED, FORM, LABEL, MATRIX -> null;
                };
            } else if (in.equalsIgnoreCase(ParameterIn.PATH.toString())) {
                return switch (paramAnnStyle) {
                    case MATRIX -> Parameter.StyleEnum.MATRIX;
                    case LABEL -> Parameter.StyleEnum.LABEL;
                    case SIMPLE -> Parameter.StyleEnum.SIMPLE;
                    case DEFAULT, DEEPOBJECT, PIPEDELIMITED, SPACEDELIMITED, FORM -> null;
                };
            } else if (in.equalsIgnoreCase(ParameterIn.QUERY.toString())) {
                return switch (paramAnnStyle) {
                    case FORM -> Parameter.StyleEnum.FORM;
                    case SPACEDELIMITED -> Parameter.StyleEnum.SPACEDELIMITED;
                    case PIPEDELIMITED -> Parameter.StyleEnum.PIPEDELIMITED;
                    case DEEPOBJECT -> Parameter.StyleEnum.DEEPOBJECT;
                    case DEFAULT, SIMPLE, LABEL, MATRIX -> null;
                };
            } else if (in.equalsIgnoreCase(ParameterIn.COOKIE.toString())) {
                return switch (paramAnnStyle) {
                    case FORM -> Parameter.StyleEnum.FORM;
                    case DEFAULT, DEEPOBJECT, PIPEDELIMITED, SPACEDELIMITED, SIMPLE, LABEL, MATRIX -> null;
                };
            }
        }
        return switch (paramAnnStyle) {
            case MATRIX -> Parameter.StyleEnum.MATRIX;
            case LABEL -> Parameter.StyleEnum.LABEL;
            case FORM -> Parameter.StyleEnum.FORM;
            case SPACEDELIMITED -> Parameter.StyleEnum.SPACEDELIMITED;
            case PIPEDELIMITED -> Parameter.StyleEnum.PIPEDELIMITED;
            case DEEPOBJECT -> Parameter.StyleEnum.DEEPOBJECT;
            case SIMPLE -> Parameter.StyleEnum.SIMPLE;
            case DEFAULT -> null;
        };
    }

    public static Parameter.StyleEnum paramStyleByFormat(String format, String in) {
        if (format == null) {
            return null;
        }
        return MnParamFormat.getStyleByFormatName(format, in);
    }

    public static String getHeaderName(TypedElement parameter, String parameterName) {
        // skip params like this: @Header Map<String, String>
        if (isIgnoredParameter(parameter)) {
            return null;
        }
        String headerName = parameter.stringValue(Header.class, PROP_NAME)
            .orElse(parameter.stringValue(Header.class)
                .orElseGet(() -> NameUtils.hyphenate(parameterName)));

        if (isIgnoredHeader(headerName)) {
            return null;
        }

        return headerName;
    }

    public static void addVersionParameters(Operation swaggerOperation, List<String> names, boolean isHeader) {

        String in = isHeader ? ParameterIn.HEADER.toString() : ParameterIn.QUERY.toString();

        for (String parameterName : names) {
            var parameter = new Parameter()
                .in(in)
                .description(DEFAULT_PARAM_VERSION_DESCRIPTION)
                .name(parameterName)
                .schema(setSpecVersion(PrimitiveType.STRING.createProperty()));

            swaggerOperation.addParametersItem(parameter);
        }
    }
}
