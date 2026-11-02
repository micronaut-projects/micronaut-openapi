/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.openapi.generator;

import org.jspecify.annotations.Nullable;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.model.ApiInfoMap;
import org.openapitools.codegen.model.OperationsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves the user parameter of an operation, taking into account the per-operation class overrides.
 */
final class UserParameterResolver {

    static final String CLI_DESCRIPTION = "Map from spec operationId to fully qualified class name of the user parameter, as a Map or as 'op1=fqn1,op2=fqn2'. Overrides userParameterMode and userParameterClass for the listed operations.";
    static final String OPT_NAME = "perOperationUserParameterClass";

    private static final Pattern QUALIFIED_NAME = Pattern.compile("([\\p{L}_][\\p{L}\\p{N}_]*\\.)+[\\p{L}_][\\p{L}\\p{N}_]*");
    private static final Logger LOG = LoggerFactory.getLogger(UserParameterResolver.class);

    private final boolean useAuth;
    private final UserParameterMode mode;
    private final @Nullable String userParameterClass;
    private final Map<String, String> perOperationUserParameterClass;

    private UserParameterResolver(boolean useAuth, UserParameterMode mode, @Nullable String userParameterClass, Map<String, String> perOperationUserParameterClass) {
        this.useAuth = useAuth;
        this.mode = mode;
        this.userParameterClass = userParameterClass;
        this.perOperationUserParameterClass = new LinkedHashMap<>(perOperationUserParameterClass);
    }

    /**
     * Creates a resolver for the user parameter options of a server generator.
     *
     * @param useAuth whether authentication is enabled
     * @param mode the global user parameter mode
     * @param userParameterClass the global user parameter class, used with {@link UserParameterMode#CUSTOM}
     * @param perOperationUserParameterClass the map from spec operationId to fully qualified class name
     * @return the resolver
     */
    static UserParameterResolver of(boolean useAuth, UserParameterMode mode, @Nullable String userParameterClass, Map<String, String> perOperationUserParameterClass) {
        return new UserParameterResolver(useAuth, mode, userParameterClass, perOperationUserParameterClass);
    }

    /**
     * Parses the untyped option value from the additional properties: a map or a string like {@code op1=fqn1,op2=fqn2}.
     *
     * @param value the option value
     * @return the operation id to class name map
     */
    static Map<String, String> parsePerOperationProperty(@Nullable Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof String str) {
            return parsePerOperation(str);
        }
        if (value instanceof Map<?, ?> map) {
            var typed = new LinkedHashMap<String, String>();
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String className)) {
                    throw new IllegalArgumentException("Invalid " + OPT_NAME + " entry '" + entry + "', expected string keys and values");
                }
                typed.put(key, className);
            }
            return validatePerOperation(typed);
        }
        throw new IllegalArgumentException("Invalid " + OPT_NAME + " value '" + value + "' of type " + value.getClass().getName() + ", expected a map or a string like operationId=fully.qualified.ClassName,...");
    }

    /**
     * Validates and trims a map from operation id to class name.
     *
     * @param map the operation id to class name map, null for none
     * @return the trimmed operation id to class name map
     */
    static Map<String, String> validatePerOperation(@Nullable Map<String, String> map) {
        var result = new LinkedHashMap<String, String>();
        if (map == null) {
            return result;
        }
        for (var entry : map.entrySet()) {
            put(result, entry.getKey(), entry.getValue(), String.valueOf(entry));
        }
        return result;
    }

    /**
     * Parses a string like {@code op1=fqn1,op2=fqn2}.
     *
     * @param value the option value
     * @return the operation id to class name map
     */
    static Map<String, String> parsePerOperation(String value) {
        var result = new LinkedHashMap<String, String>();
        for (var item : value.split(",")) {
            if (item.isBlank()) {
                continue;
            }
            int idx = item.indexOf('=');
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid " + OPT_NAME + " entry '" + item.trim() + "', expected operationId=fully.qualified.ClassName");
            }
            put(result, item.substring(0, idx), item.substring(idx + 1), item.trim());
        }
        return result;
    }

    private static void put(Map<String, String> result, @Nullable String key, @Nullable String value, String entry) {
        var operationId = requireNotBlank(key, entry);
        var className = requireNotBlank(value, entry);
        if (!QUALIFIED_NAME.matcher(className).matches()) {
            throw new IllegalArgumentException("Invalid " + OPT_NAME + " entry '" + entry + "', '" + className + "' is not a fully qualified class name");
        }
        if (result.put(operationId, className) != null) {
            throw new IllegalArgumentException("Invalid " + OPT_NAME + " entry '" + entry + "', duplicate operationId '" + operationId + "'");
        }
    }

    private static String requireNotBlank(@Nullable String part, String entry) {
        if (part == null || part.isBlank()) {
            throw new IllegalArgumentException("Invalid " + OPT_NAME + " entry '" + entry + "', expected operationId=fully.qualified.ClassName");
        }
        return part.trim();
    }

    /**
     * Adds the user parameter to the operation.
     *
     * @param op the operation
     * @param isAnonymous whether the operation is anonymous
     * @param isDenyAll whether the operation denies all
     * @return the fully qualified class name of the added parameter, or null if no parameter was added
     */
    @Nullable String apply(CodegenOperation op, boolean isAnonymous, boolean isDenyAll) {
        var override = op.operationIdOriginal != null ? perOperationUserParameterClass.get(op.operationIdOriginal) : null;
        UserParameterMode effectiveMode;
        String effectiveClass;
        if (override != null) {
            effectiveMode = UserParameterMode.CUSTOM;
            effectiveClass = override;
        } else if (mode == UserParameterMode.NONE) {
            return null;
        } else {
            effectiveMode = mode;
            effectiveClass = mode == UserParameterMode.CUSTOM ? userParameterClass : mode.getClassFullName();
        }
        return Utils.addUserParameter(op, effectiveMode, effectiveClass, isAnonymous, isDenyAll) ? effectiveClass : null;
    }

    /**
     * Adds the imports of the user parameter classes to the operations of a controller.
     *
     * @param objs the operations of the controller
     * @param classes the fully qualified class names returned by {@link #apply(CodegenOperation, boolean, boolean)}
     */
    static void addImports(OperationsMap objs, Collection<String> classes) {
        for (var fqn : new LinkedHashSet<>(classes)) {
            objs.getImports().add(Map.of("import", fqn, "classname", fqn.substring(fqn.lastIndexOf('.') + 1)));
        }
    }

    /**
     * Logs the configuration problems of the per-operation option.
     *
     * @param supportingFileData the supporting file data, with the generated operations under {@code apiInfo}
     */
    void logWarnings(Map<String, Object> supportingFileData) {
        warnings(generatedOperationIds(supportingFileData)).forEach(LOG::warn);
    }

    /**
     * Collects the spec operation ids of the operations in the supporting file data.
     *
     * @param supportingFileData the supporting file data, with the generated operations under {@code apiInfo}
     * @return the spec operation ids of the generated operations
     */
    static Set<String> generatedOperationIds(Map<String, Object> supportingFileData) {
        var operationIds = new HashSet<String>();
        if (supportingFileData.get("apiInfo") instanceof ApiInfoMap apiInfo && apiInfo.getApis() != null) {
            for (var api : apiInfo.getApis()) {
                for (var op : api.getOperations().getOperation()) {
                    if (op.operationIdOriginal != null) {
                        operationIds.add(op.operationIdOriginal);
                    }
                }
            }
        }
        return operationIds;
    }

    /**
     * Builds the warning messages for the configuration problems of the per-operation option.
     *
     * @param operationIds the spec operation ids of the generated operations
     * @return the configuration problems of the per-operation option
     */
    List<String> warnings(Set<String> operationIds) {
        if (perOperationUserParameterClass.isEmpty()) {
            return List.of();
        }
        if (!useAuth) {
            return List.of("Option " + OPT_NAME + " has no effect when useAuth is false.");
        }
        return unmatchedOperationIds(operationIds).stream()
            .map(operationId -> "Option " + OPT_NAME + " contains the key '" + operationId + "' which matches no generated operation. Keys must equal the operationId written in the spec.")
            .toList();
    }

    /**
     * Finds the configured operation ids that match no generated operation.
     *
     * @param operationIds the spec operation ids of the generated operations
     * @return the configured operation ids that match no generated operation, empty when no operation was generated
     */
    Set<String> unmatchedOperationIds(Set<String> operationIds) {
        if (operationIds.isEmpty()) {
            return Set.of();
        }
        var result = new LinkedHashSet<>(perOperationUserParameterClass.keySet());
        result.removeAll(operationIds);
        return result;
    }
}
