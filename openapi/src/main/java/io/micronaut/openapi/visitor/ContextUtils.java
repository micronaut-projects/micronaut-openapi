/*
 * Copyright 2017-2022 original authors
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
import io.micronaut.inject.visitor.VisitorContext;

/**
 * Convert utilities methods.
 *
 * @since 4.5.0
 */
@Internal
public final class ContextUtils {

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
