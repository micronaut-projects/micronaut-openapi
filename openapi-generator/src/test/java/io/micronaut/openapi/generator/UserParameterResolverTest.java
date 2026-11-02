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

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.model.ApiInfoMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserParameterResolverTest {

    private static final String SAVE_USER = "com.example.SaveUser";
    private static final String AUTHENTICATION = "io.micronaut.security.authentication.Authentication";

    private static CodegenOperation op(String operationIdOriginal) {
        var op = new CodegenOperation();
        op.operationIdOriginal = operationIdOriginal;
        return op;
    }

    private static OperationsMap operations() {
        var objs = new OperationsMap();
        objs.setImports(new ArrayList<>());
        return objs;
    }

    private static Map<String, String> importOf(String fqn) {
        return Map.of("import", fqn, "classname", fqn.substring(fqn.lastIndexOf('.') + 1));
    }

    @Test
    void parsesMap() {
        var map = Map.of("save", SAVE_USER);
        assertEquals(map, UserParameterResolver.validatePerOperation(map));
    }

    @Test
    void parsesString() {
        var expected = new LinkedHashMap<String, String>();
        expected.put("save", SAVE_USER);
        expected.put("get", "com.example.GetUser");
        assertEquals(expected, UserParameterResolver.parsePerOperation(" save = com.example.SaveUser , get=com.example.GetUser "));
    }

    @Test
    void parsesNullAsEmpty() {
        assertTrue(UserParameterResolver.parsePerOperationProperty(null).isEmpty());
        assertTrue(UserParameterResolver.validatePerOperation(null).isEmpty());
    }

    @Test
    void rejectsMalformedString() {
        var e1 = assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperation("save"));
        assertTrue(e1.getMessage().contains("save"));
        assertTrue(e1.getMessage().contains("expected operationId=fully.qualified.ClassName"));
        var e2 = assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperation("save="));
        assertTrue(e2.getMessage().contains("save"));
        assertTrue(e2.getMessage().contains("expected operationId=fully.qualified.ClassName"));
    }

    @Test
    void rejectsOtherTypes() {
        assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperationProperty(42));
    }

    @Test
    void parsesPropertyMapAndString() {
        assertEquals(Map.of("save", SAVE_USER), UserParameterResolver.parsePerOperationProperty(Map.of("save", SAVE_USER)));
        assertEquals(Map.of("save", SAVE_USER), UserParameterResolver.parsePerOperationProperty("save=" + SAVE_USER));
    }

    @Test
    void rejectsNonStringPropertyMapEntries() {
        var e1 = assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperationProperty(Map.of(1, SAVE_USER)));
        assertTrue(e1.getMessage().contains("expected string keys and values"));
        assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperationProperty(Map.of("save", 1)));
    }

    @Test
    void rejectsDuplicateKeys() {
        var e1 = assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperation("save=a.A,save=b.B"));
        assertTrue(e1.getMessage().contains("save"));
        var map = new LinkedHashMap<String, String>();
        map.put(" save", "a.A");
        map.put("save", "b.B");
        var e2 = assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.validatePerOperation(map));
        assertTrue(e2.getMessage().contains("save"));
    }

    @Test
    void rejectsUnqualifiedClassNames() {
        var e1 = assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperation("save=SaveUser"));
        assertTrue(e1.getMessage().contains("save=SaveUser"));
        assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.validatePerOperation(Map.of("save", "com..SaveUser")));
        assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.validatePerOperation(Map.of("save", "com.example.")));
    }

    @Test
    void rejectsBinaryNestedClassNames() {
        assertThrows(IllegalArgumentException.class, () -> UserParameterResolver.parsePerOperation("save=com.example.Outer$Inner"));
        assertEquals(Map.of("save", "com.example.Outer.Inner"), UserParameterResolver.parsePerOperation("save=com.example.Outer.Inner"));
    }

    @Test
    void overrideWinsOverGlobal() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.AUTHENTICATION, null, Map.of("save", SAVE_USER));
        var save = op("save");
        assertEquals(SAVE_USER, resolver.apply(save, false, false));
        var param = save.allParams.get(save.allParams.size() - 1);
        assertEquals("SaveUser", param.dataType);
        assertEquals("authentication", param.paramName);
        var get = op("get");
        assertEquals(AUTHENTICATION, resolver.apply(get, false, false));
        assertEquals("Authentication", get.allParams.get(get.allParams.size() - 1).dataType);
    }

    @Test
    void overrideWithGlobalNone() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.NONE, null, Map.of("save", SAVE_USER));
        var save = op("save");
        assertEquals(SAVE_USER, resolver.apply(save, false, false));
        assertEquals(1, save.allParams.size());
        assertEquals("SaveUser", save.allParams.get(0).dataType);
        assertEquals("authentication", save.allParams.get(0).paramName);
        var get = op("get");
        assertNull(resolver.apply(get, false, false));
        assertTrue(get.allParams.isEmpty());
    }

    @Test
    void operationWithoutIdUsesGlobal() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.PRINCIPAL, null, Map.of("save", SAVE_USER));
        assertEquals("java.security.Principal", resolver.apply(op(null), false, false));
    }

    @Test
    void addsDeduplicatedImports() {
        var objs = operations();
        UserParameterResolver.addImports(objs, List.of(SAVE_USER, AUTHENTICATION, SAVE_USER));
        assertEquals(List.of(importOf(SAVE_USER), importOf(AUTHENTICATION)), objs.getImports());
    }

    @Test
    void matchesOnlySpecOperationId() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.NONE, null, Map.of("savePublic", SAVE_USER));
        var operation = op("save-public");
        operation.operationId = "savePublic";
        assertNull(resolver.apply(operation, false, false));
        assertEquals(Set.of("savePublic"), resolver.unmatchedOperationIds(Set.of("save-public")));
    }

    @Test
    void denyAllAddsNoParameter() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.NONE, null, Map.of("denyAllOp", SAVE_USER));
        var operation = op("denyAllOp");
        assertNull(resolver.apply(operation, false, true));
        assertTrue(operation.allParams.isEmpty());
        assertTrue(resolver.unmatchedOperationIds(Set.of("denyAllOp")).isEmpty());
    }

    @Test
    void noWarningsWithoutGeneratedOperations() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.NONE, null, Map.of("save", SAVE_USER));
        assertTrue(resolver.unmatchedOperationIds(Set.of()).isEmpty());
        assertTrue(resolver.warnings(Set.of()).isEmpty());
    }

    @Test
    void warnsWithoutUseAuth() {
        var resolver = UserParameterResolver.of(false, UserParameterMode.NONE, null, Map.of("save", SAVE_USER));
        var warnings = resolver.warnings(Set.of("save"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("no effect when useAuth is false"));
        assertTrue(UserParameterResolver.of(false, UserParameterMode.NONE, null, Map.of()).warnings(Set.of("save")).isEmpty());
    }

    @Test
    void warnsAboutUnmatchedKey() {
        var resolver = UserParameterResolver.of(true, UserParameterMode.NONE, null, Map.of("missing", SAVE_USER));
        var warnings = resolver.warnings(Set.of("save"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("missing"));
        assertTrue(warnings.get(0).contains("no generated operation"));
    }

    @Test
    void readsGeneratedOperationsFromSupportingFileData() {
        var objs = operations();
        var operationMap = new OperationMap();
        operationMap.setOperation(List.of(op("save")));
        objs.setOperation(operationMap);
        var apiInfo = new ApiInfoMap();
        apiInfo.setApis(List.of(objs));
        assertTrue(UserParameterResolver.generatedOperationIds(Map.of("apiInfo", apiInfo)).contains("save"));
        assertTrue(UserParameterResolver.generatedOperationIds(Map.of()).isEmpty());
    }
}
