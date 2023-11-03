package io.micronaut.openapi.generator;

import java.util.stream.Stream;

import io.micronaut.openapi.generator.MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultCodegen;

import static io.micronaut.openapi.generator.MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.JAVA;
import static io.micronaut.openapi.generator.MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.KOTLIN;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JavaMicronautClientCodegenSerializationLibraryTest extends AbstractMicronautCodegenTest {

    static Stream<Arguments> langs() {
        return Stream.of(
            arguments(JAVA),
            arguments(KOTLIN)
        );
    }

    @MethodSource("langs")
    @ParameterizedTest
    void testSerializationLibraryJackson(GeneratorLanguage lang) {
        var codegen = lang == JAVA ? new JavaMicronautClientCodegen() : new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(CodegenConstants.SERIALIZATION_LIBRARY, SerializationLibraryKind.JACKSON.name());
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS);

        var ext = lang == JAVA ? "java" : "kt";

        // Model does not contain micronaut serde annotation
        String micronautSerDeAnnotation = "@Serdeable";
        String modelPath = outputPath + "src/main/" + (lang == JAVA ? "java" : "kotlin") + "/org/openapitools/model/";
        assertFileNotContains(modelPath + "Pet." + ext, micronautSerDeAnnotation);
        assertFileNotContains(modelPath + "User." + ext, micronautSerDeAnnotation);
        assertFileNotContains(modelPath + "Order." + ext, micronautSerDeAnnotation);
        assertFileNotContains(modelPath + "Tag." + ext, micronautSerDeAnnotation);
        assertFileNotContains(modelPath + "Category." + ext, micronautSerDeAnnotation);
    }

    /**
     * Checks micronaut-serde-jackson limitation.
     *
     * @see <a href="https://micronaut-projects.github.io/micronaut-serialization/latest/guide/index.html#jacksonAnnotations"></a>
     */
    @MethodSource("langs")
    @ParameterizedTest
    void testSerializationLibraryMicronautSerdeJackson(GeneratorLanguage lang) {
        var codegen = lang == JAVA ? new JavaMicronautClientCodegen() : new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(CodegenConstants.SERIALIZATION_LIBRARY, SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name());
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS);

        var ext = lang == JAVA ? "java" : "kt";

        // Model contains micronaut serde annotation
        String micronautSerdeAnnotation = "@Serdeable";
        String modelPath = outputPath + "src/main/" + (lang == JAVA ? "java" : "kotlin") + "/org/openapitools/model/";
        assertFileContains(modelPath + "Pet." + ext, "import io.micronaut.serde.annotation.Serdeable");
        assertFileContains(modelPath + "Pet." + ext, micronautSerdeAnnotation);
        assertFileContains(modelPath + "User." + ext, micronautSerdeAnnotation);
        assertFileContains(modelPath + "Order." + ext, micronautSerdeAnnotation);
        assertFileContains(modelPath + "Tag." + ext, micronautSerdeAnnotation);
        assertFileContains(modelPath + "Category." + ext, micronautSerdeAnnotation);
    }
}
