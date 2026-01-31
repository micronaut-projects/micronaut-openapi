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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.ast.MethodElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Methods to construct OpenPI schema definition.
 *
 * @since 6.19.2
 */
@Internal
public final class OperationUtils {

    /**
     * Stores the current in progress type.
     */
    private static Map<String, List<String>> methodsByOperationId = new HashMap<>();

    private OperationUtils() {
    }

    /**
     * Cleanup context.
     */
    public static void clean() {
        methodsByOperationId = new HashMap<>();
    }

    @Nullable
    public static List<String> getMethodsByOperationId(String operationId) {
        return methodsByOperationId.get(operationId);
    }

    public static void addOperation(@Nullable String operationId, MethodElement methodEl) {
        if (operationId == null) {
            return;
        }
        var existedList = methodsByOperationId.computeIfAbsent(operationId, k -> new ArrayList<>());
        existedList.add(methodEl.getOwningType().getName() + '.' + methodEl.getName());
    }

}
