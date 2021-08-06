/*
 * Copyright 2017-2021 original authors
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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class that post process OpenApi operations.
 */
public class OpenApiOperationsPostProcessor {

    /**
     * Process operations, making operation ids unique.
     *
     * @param openAPI OpenApi object with all definitions
     */
    public void processOperations(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }

        Map<String, Integer> operationIdsIndex = new HashMap<>();

        openAPI.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .forEach(operation -> {
                    String operationId = operation.getOperationId();

                    if (operationIdsIndex.containsKey(operationId)) {
                        int nextValue = operationIdsIndex.get(operationId);

                        String newOperationId = operationId + "_" + nextValue;
                        operation.setOperationId(newOperationId);
                        updateResponseDescription(operation, operationId, newOperationId);

                        operationIdsIndex.put(operationId, ++nextValue);
                    } else {
                        operationIdsIndex.put(operationId, 1);
                    }
                });
    }

    private static void updateResponseDescription(Operation operation, String originalId, String newOperationId) {
        if (operation.getResponses() != null) {
            operation.getResponses().values().stream()
                    .filter(apiResponse -> apiResponse != null && apiResponse.getDescription() != null)
                    .forEach(apiResponse -> apiResponse.setDescription(apiResponse.getDescription().replaceFirst(originalId, newOperationId)));
        }
    }

}
