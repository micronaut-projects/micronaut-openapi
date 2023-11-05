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

import org.openapitools.codegen.CodegenParameter;

/**
 * A record that can be used to specify parameter mapping.
 * Parameter mapping would map a given parameter to a specific type and name.
 *
 * @param name The name of the parameter as described by the name field in specification.
 * @param location The location of parameter. Path parameters cannot be mapped, as this
 *                 behavior should not be used.
 * @param mappedType The type to which the parameter should be mapped. If multiple parameters
 *                   have the same mapping, only one parameter will be present. If set to null,
 *                   the original parameter will be deleted.
 * @param mappedName The unique name of the parameter to be used as method parameter name.
 * @param isValidated Whether the mapped parameter requires validation.
 */
public record ParameterMapping(
    String name,
    ParameterLocation location,
    String mappedType,
    String mappedName,
    boolean isValidated
) {

    public boolean doesMatch(CodegenParameter parameter) {
        if (name != null && !name.equals(parameter.baseName)) {
            return false;
        }
        if (location == null) {
            return true;
        }
        return switch (location) {
            case HEADER -> parameter.isHeaderParam;
            case QUERY -> parameter.isQueryParam;
            case FORM -> parameter.isFormParam;
            case COOKIE -> parameter.isCookieParam;
            case BODY -> parameter.isBodyParam;
        };
    }

    /**
     * The location of the parameter to be mapped.
     */
    public enum ParameterLocation {
        HEADER,
        QUERY,
        FORM,
        COOKIE,
        BODY
    }
}
