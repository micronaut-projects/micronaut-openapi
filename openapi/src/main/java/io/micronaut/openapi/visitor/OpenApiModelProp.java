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
 * Common property names in OpenAPI and Micronaut objects and annotations.
 *
 * @since 6.7.0
 */
@Internal
public interface OpenApiModelProp {

    String PROP_EXTENSIONS = "extensions";
    String PROP_VALUE = "value";
    String PROP_NAME = "name";
    String PROP_DESCRIPTION = "description";
    String PROP_SCHEMA = "schema";
    String PROP_REF = "ref";
    String PROP_REF_DOLLAR = "$ref";
    String PROP_HIDDEN = "hidden";
    String PROP_EXAMPLE = "example";
    String PROP_EXAMPLES = "examples";
    String PROP_NOT = "not";
    String PROP_ALL_OF = "allOf";
    String PROP_ANY_OF = "anyOf";
    String PROP_ONE_OF = "oneOf";
    String PROP_TYPE = "type";
    String PROP_ONE_TYPES = "types";
    String PROP_ONE_FORMAT = "format";
    String PROP_ALLOWABLE_VALUES = "allowableValues";
    String PROP_DEFAULT_VALUE = "defaultValue";
    String PROP_DEFAULT = "default";
    String PROP_ENUM = "enum";
    String PROP_IN = "in";
    String PROP_IMPLEMENTATION = "implementation";
    String PROP_REQUIRED = "required";
    String PROP_REQUIRED_PROPERTIES = "requiredProperties";
    String PROP_ADDITIONAL_PROPERTIES = "additionalProperties";
    String PROP_DISCRIMINATOR_PROPERTY = "discriminatorProperty";
    String PROP_DISCRIMINATOR_MAPPING = "discriminatorMapping";
    String PROP_DEPRECATED = "deprecated";
    String PROP_STYLE = "style";
    String DISCRIMINATOR = "discriminator";
    String PROP_OPERATION = "operation";
    String PROP_ALLOW_RESERVED = "allowReserved";
    String PROP_ALLOW_EMPTY_VALUE = "allowEmptyValue";
    String PROP_MEDIA_TYPE = "mediaType";
    String PROP_CONTENT = "content";
    String PROP_ARRAY_SCHEMA = "arraySchema";
    String PROP_RESPONSE_CODE = "responseCode";
    String PROP_METHOD = "method";
    String PROP_EXPLODE = "explode";
    String PROP_ACCESS_MODE = "accessMode";
    String PROP_NULLABLE = "nullable";
    String PROP_SCOPES = "scopes";
    String PROP_PARAM_NAME = "paramName";
    String PROP_PROPERTY_NAME = "propertyName";
    String PROP_TAGS = "tags";
    String PROP_SECURITY = "security";
    String PROP_READ_ONLY = "readOnly";
    String PROP_WRITE_ONLY = "writeOnly";
    String PROP_EXTERNAL_DOCS = "externalDocs";
    String PROP_EXPRESSION = "expression";
    String PROP_REQUIRED_MODE = "requiredMode";
    String PROP_TITLE = "title";
    String PROP_CALLBACK_URL_EXPRESSION = "callbackUrlExpression";
    String PROP_EXCLUDE = "exclude";
    String PROP_OP_ID_SUFFIX = "opIdSuffix";
    String PROP_ADD_ALWAYS = "addAlways";
    String PROP_PROPERTIES = "properties";
    String PROP_PARSE_VALUE = "parseValue";
    String PROP_PARAMETERS = "parameters";
    String PROP_MAPPING = "mapping";
    String PROP_SCHEME = "scheme";
    String PROP_FLOWS = "flows";
    String PROP_OPEN_ID_CONNECT_URL = "openIdConnectUrl";
    String PROP_BEARER_FORMAT = "bearerFormat";
}
