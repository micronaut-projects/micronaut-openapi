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
package io.micronaut.openapi.visitor;

import java.util.List;
import java.util.Map;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.GenericArgument;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.group.GroupProperties;

/**
 * Convert utilities methods.
 *
 * @since 4.5.0
 */
@Internal
public final class ContextUtils {

    public static final Argument<List<Pair<String, String>>> EXPANDABLE_PROPERTIES_ARGUMENT = new GenericArgument<>() { };
    public static final Argument<Map<String, ConfigUtils.SchemaDecorator>> ARGUMENT_SCHEMA_DECORATORS_MAP = new GenericArgument<>() { };
    public static final Argument<Map<String, ConfigUtils.CustomSchema>> ARGUMENT_CUSTOM_SCHEMA_MAP = new GenericArgument<>() { };
    public static final Argument<Map<String, GroupProperties>> ARGUMENT_GROUP_PROPERTIES_MAP = new GenericArgument<>() { };

    private ContextUtils() {
    }

    public static Integer getVisitedElements(VisitorContext context) {
        Integer visitedElements = context.get(Utils.ATTR_VISITED_ELEMENTS, Integer.class).orElse(null);
        if (visitedElements == null) {
            visitedElements = 0;
            context.put(Utils.ATTR_VISITED_ELEMENTS, visitedElements);
        }
        return visitedElements;
    }
}
