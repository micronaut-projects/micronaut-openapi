package io.micronaut.openapi.generator;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenConstants;

import java.net.URLConnection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicronautCodegenTest extends AbstractMicronautCodegenTest {

    @Test
    void testDisableUrlConnectionCache() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateWithOpts(codegen, Map.of(), "src/test/resources/3_0/oas.yml", CodegenConstants.MODELS, CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFalse(URLConnection.getDefaultUseCaches("jar"));
        assertFalse(URLConnection.getDefaultUseCaches("file"));

        assertFileExists(path + "model/CategoryObject.java");
    }

    @Test
    void testEnableUrlConnectionCache() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateWithOpts(codegen, Map.of("useUrlConnectionCache", true), "src/test/resources/3_0/oas.yml", CodegenConstants.MODELS, CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertTrue(URLConnection.getDefaultUseCaches("jar"));
        assertTrue(URLConnection.getDefaultUseCaches("file"));

        assertFileExists(path + "model/CategoryObject.java");
    }

    @Test
    void testEnableUrlConnectionCacheByAdditionalProperties() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateWithOpts(codegen, Map.of("additionalProperties", Map.of("useUrlConnectionCache", "true")), "src/test/resources/3_0/oas.yml", CodegenConstants.MODELS, CodegenConstants.APIS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertTrue(URLConnection.getDefaultUseCaches("jar"));
        assertTrue(URLConnection.getDefaultUseCaches("file"));

        assertFileExists(path + "model/CategoryObject.kt");
    }
}
