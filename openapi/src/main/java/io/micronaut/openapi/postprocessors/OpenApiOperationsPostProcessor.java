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

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.ConfigUtils;
import io.micronaut.openapi.visitor.OperationUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.HashMap;

import static io.micronaut.openapi.visitor.ConfigUtils.getOperationDuplicateResolution;
import static io.micronaut.openapi.visitor.StringUtil.UNDERSCORE;

/**
 * A helper class that post process OpenApi operations.
 */
public class OpenApiOperationsPostProcessor {

    /**
     * Process operations, making operation ids unique.
     *
     * @param openApi OpenApi object with all definitions
     * @param context The visitor context
     */
    public void processOperations(OpenAPI openApi, VisitorContext context) {
        if (CollectionUtils.isEmpty(openApi.getPaths())) {
            return;
        }

        var operationPathsById = new HashMap<String, String>();
        var operationIdsIndex = new HashMap<String, Integer>();

        for (var pathItemEntry : openApi.getPaths().entrySet()) {
            var pathItem = pathItemEntry.getValue();
            var path = pathItemEntry.getKey();
            var operations = pathItem.readOperationsMap();
            for (var opEntry : operations.entrySet()) {
                var method = opEntry.getKey();
                var operation = opEntry.getValue();
                String operationId = operation.getOperationId();
                if (operationId == null) {
                    continue;
                }
                var operationPath = operationPath(method, path);

                if (!operationIdsIndex.containsKey(operationId)) {
                    operationIdsIndex.put(operationId, 1);
                    operationPathsById.put(operationId, operationPath);
                    continue;
                }
                int nextValue = operationIdsIndex.get(operationId);
                if (getOperationDuplicateResolution(context) == ConfigUtils.DuplicateResolution.ERROR) {
                    var existedOperationPath = operationPathsById.get(operationId);
                    var methods = OperationUtils.getMethodsByOperationId(operationId);
                    var methodsMessage = "";
                    if (CollectionUtils.isNotEmpty(methods)) {
                        methodsMessage = "\nMethods: " + String.join(", ", methods);
                    }
                    throw new ConfigurationException("Found 2 operations with same ID \"" + operationId + "\" for paths " + existedOperationPath + " and " + operationPath + methodsMessage);
                }

                String newOperationId = operationId + UNDERSCORE + nextValue;
                operation.setOperationId(newOperationId);
                updateResponseDescription(operation, operationId, newOperationId);

                operationIdsIndex.put(operationId, ++nextValue);
                operationPathsById.put(newOperationId, operationPath);
            }
        }
    }

    private String operationPath(PathItem.HttpMethod method, String path) {
        return method + " " + path;
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
