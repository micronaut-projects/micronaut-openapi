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
package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;

import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

/**
 * OpenApi introspection configuration for Swagger-model.
 * Adds introspection of the io.swagger.v3.oas.models.parameters package
 *
 * @author Henrique Mota
 */
@Introspected(classes = {
    CookieParameter.class,
    HeaderParameter.class,
    Parameter.class,
    PathParameter.class,
    QueryParameter.class,
    RequestBody.class,
})
public class ParametersConfiguration {
}
