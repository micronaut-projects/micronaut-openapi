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
package io.micronaut.openapi.postprocessors;

import io.micronaut.core.util.CollectionUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

import java.util.HashMap;

import static io.micronaut.openapi.visitor.StringUtil.UNDERSCORE;

/**
 * A helper class that post process OpenApi operations.
 */
public class OpenApiOperationsPostProcessor {

    /**
     * Process operations, making operation ids unique.
     *
     * @param openApi OpenApi object with all definitions
     */
    public void processOperations(OpenAPI openApi) {
        if (CollectionUtils.isEmpty(openApi.getPaths())) {
            return;
        }

        var operationIdsIndex = new HashMap<String, Integer>();

        for (var pathItem : openApi.getPaths().values()) {
            for (var operation : pathItem.readOperations()) {
                String operationId = operation.getOperationId();

                if (!operationIdsIndex.containsKey(operationId)) {
                    operationIdsIndex.put(operationId, 1);
                    continue;
                }
                int nextValue = operationIdsIndex.get(operationId);

                String newOperationId = operationId + UNDERSCORE + nextValue;
                operation.setOperationId(newOperationId);
                updateResponseDescription(operation, operationId, newOperationId);

                operationIdsIndex.put(operationId, ++nextValue);
            }
        }
    }

    private static void updateResponseDescription(Operation operation, String originalId, String newOperationId) {
        if (CollectionUtils.isEmpty(operation.getResponses())) {
            return;
        }
        for (var apiResponse : operation.getResponses().values()) {
            if (apiResponse == null || apiResponse.getDescription() == null) {
                continue;
            }
            apiResponse.setDescription(apiResponse.getDescription().replaceFirst(originalId, newOperationId));
        }
    }

}
