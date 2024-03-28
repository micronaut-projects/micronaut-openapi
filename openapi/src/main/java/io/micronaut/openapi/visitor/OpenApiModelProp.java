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
    String PROP_$REF = "$ref";
    String PROP_HIDDEN = "hidden";
    String PROP_EXAMPLE = "example";
    String PROP_EXAMPLES = "examples";
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
}
