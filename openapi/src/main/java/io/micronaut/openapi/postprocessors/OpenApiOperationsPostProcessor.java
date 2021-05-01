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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

/**
 * A helper class that post process OpenApi operations.
 */
public class OpenApiOperationsPostProcessor {

    /**
     * Auto generated operation prefix.
     */
    public static final String AUTO_GENERATED_OPERATION_PREFIX = "micronautGenerated::";

    /**
     * Process operations, making operation ids unique.
     *
     * @param openAPI OpenApi object with all definitions
     * @param visitorContext visitor context of Annotation processor
     * @param classElement visited class element
     */
    public void processOperations(OpenAPI openAPI, VisitorContext visitorContext, ClassElement classElement) {
        if (openAPI.getPaths() == null) {
            return;
        }
        List<Operation> allOperations = openAPI.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .collect(Collectors.toList());
        Set<String> userOperationIds = collectUserOperationIds(allOperations, visitorContext, classElement);
        updateOperationIds(allOperations, userOperationIds);
    }

    private Set<String> collectUserOperationIds(List<Operation> allOperations, VisitorContext visitorContext, ClassElement classElement) {
        Set<String> operations = new HashSet<>();
        allOperations.stream()
                .filter(operation -> !hasGeneratedOperationId(operation))
                .map(Operation::getOperationId)
                .forEach(operationId -> {
                    if (operations.contains(operationId)) {
                        visitorContext.warn("Found duplicate operation id: '" + operationId + "'", classElement);
                    }
                    operations.add(operationId);
                });
        return operations;
    }

    private void updateOperationIds(List<Operation> allOperations, Set<String> userOperationIds) {
        Set<String> generatedOperationIds = new HashSet<>();
        Set<String> duplicatedOperationIds = allOperations.stream()
                .filter(this::hasGeneratedOperationId)
                .map(operation -> operation.getOperationId().replaceFirst(AUTO_GENERATED_OPERATION_PREFIX, ""))
                .filter(operationId -> !generatedOperationIds.add(operationId) || userOperationIds.contains(operationId))
                .collect(Collectors.toSet());

        Map<String, Integer> counters = new HashMap<>();
        allOperations.stream()
                .filter(this::hasGeneratedOperationId)
                .forEach(operation -> {
                    String originalOperationId = operation.getOperationId();
                    String newOperationId = originalOperationId.replace(AUTO_GENERATED_OPERATION_PREFIX, "");
                    if (duplicatedOperationIds.contains(newOperationId)) {
                        newOperationId = findNextOperationId(newOperationId, counters, userOperationIds, generatedOperationIds);
                        generatedOperationIds.add(newOperationId);
                    }
                    operation.setOperationId(newOperationId);
                    fixResponseDescription(operation, originalOperationId, newOperationId);
                });
    }

    private static void fixResponseDescription(Operation operation, String originalId, String newOperationId) {
        if (operation.getResponses() != null) {
            operation.getResponses().values().stream()
                    .filter(apiResponse -> apiResponse != null && apiResponse.getDescription() != null)
                    .forEach(apiResponse -> apiResponse.setDescription(apiResponse.getDescription().replaceFirst(originalId, newOperationId)));
        }
    }

    private boolean hasGeneratedOperationId(Operation operation) {
        return operation.getOperationId().startsWith(AUTO_GENERATED_OPERATION_PREFIX);
    }

    private String findNextOperationId(String operationId, Map<String, Integer> counters, Set<String> userOperationIds, Set<String> generatedOperationIds) {
        String newOperationId;
        do {
            // There is no specific reason, but lets start with 1
            int newIndex = counters.compute(operationId, (key, previousValue) -> previousValue == null ? 1 : previousValue + 1);
            newOperationId = operationId + newIndex;
        } while (generatedOperationIds.contains(newOperationId) || userOperationIds.contains(newOperationId));
        return newOperationId;
    }

}
