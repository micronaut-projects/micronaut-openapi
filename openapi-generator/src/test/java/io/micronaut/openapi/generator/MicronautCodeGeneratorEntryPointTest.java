package io.micronaut.openapi.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static io.micronaut.openapi.generator.AbstractMicronautCodegenTest.assertFileContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MicronautCodeGeneratorEntryPointTest {

    @Test
    @DisplayName("Can build with a custom code generator")
    void testCustomGenerator() throws URISyntaxException {
        var builder = MicronautCodeGeneratorEntryPoint.builder();
        var generator = new TestGenerator();
        builder.forCodeGenerator(generator, spec -> spec.withValue("Hello"))
            .withDefinitionFile(new URI("https://fake.uri"))
            .build();
        assertEquals("Hello", generator.value);
    }

    @Test
    @DisplayName("Cannot invoke Java server generator entrypoint with `userParameterMode` set to CUSTOM without `userParameterClass`")
    void testJavaServerCustomUserParameterModeThrowsWithoutUserParameterClass() throws URISyntaxException {
        var builder = MicronautCodeGeneratorEntryPoint.builder();
        var generator = new JavaMicronautServerCodegen();
        var entrypoint = builder.forCodeGenerator(generator, spec -> {})
            .withOptions(options -> {
                options.withLang(MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.JAVA);
                options.withAdditionalProperties(Map.of(
                    JavaMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.CUSTOM.name(),
                    AbstractMicronautJavaCodegen.OPT_USE_AUTH, true
                ));
            })
            .withDefinitionFile(new URI("src/test/resources/3_0/security.yml"))
            .build();

        assertThrows(IllegalArgumentException.class, entrypoint::generate);
    }

    @Test
    @DisplayName("Cannot invoke Java server generator entrypoint with `userParameterMode` not set to CUSTOM and `userParameterClass`")
    void testJavaServerNonCustomUserParameterModeThrowsWithUserParameterClass() throws URISyntaxException {
        var builder = MicronautCodeGeneratorEntryPoint.builder();
        var generator = new JavaMicronautServerCodegen();
        var entrypoint = builder.forCodeGenerator(generator, spec -> {})
            .withOptions(options -> {
                options.withLang(MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.JAVA);
                options.withAdditionalProperties(Map.of(
                    JavaMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.AUTHENTICATION.name(),
                    AbstractMicronautJavaCodegen.OPT_USE_AUTH, true,
                    JavaMicronautServerCodegen.OPT_USER_PARAMETER_CLASS, "com.example.CustomUserParameter"
                ));
            })
            .withDefinitionFile(new URI("src/test/resources/3_0/security.yml"))
            .build();

        assertThrows(IllegalArgumentException.class, entrypoint::generate);
    }

    @Test
    @DisplayName("Cannot invoke Kotlin server generator entrypoint with `userParameterMode` not set to CUSTOM and `userParameterClass`")
    void testKotlinServerNonCustomUserParameterModeThrowsWithUserParameterClass() throws URISyntaxException {
        var builder = MicronautCodeGeneratorEntryPoint.builder();
        var generator = new KotlinMicronautServerCodegen();
        var entrypoint = builder.forCodeGenerator(generator, spec -> {})
            .withOptions(options -> {
                options.withLang(MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.JAVA);
                options.withAdditionalProperties(Map.of(
                    KotlinMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.AUTHENTICATION.name(),
                    AbstractMicronautJavaCodegen.OPT_USE_AUTH, true,
                    KotlinMicronautServerCodegen.OPT_USER_PARAMETER_CLASS, "com.example.CustomUserParameter"
                ));
            })
            .withDefinitionFile(new URI("src/test/resources/3_0/security.yml"))
            .build();

        assertThrows(IllegalArgumentException.class, entrypoint::generate);
    }

    @Test
    @DisplayName("Cannot invoke Kotlin server generator entrypoint with `userParameterMode` set to CUSTOM without `userParameterClass`")
    void testKotlinServerCustomUserParameterModeThrowsWithoutUserParameterClass() throws URISyntaxException {
        var builder = MicronautCodeGeneratorEntryPoint.builder();
        var generator = new KotlinMicronautServerCodegen();
        var entrypoint = builder.forCodeGenerator(generator, spec -> {})
            .withOptions(options -> {
                options.withLang(MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.JAVA);
                options.withAdditionalProperties(Map.of(
                    KotlinMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.CUSTOM.name(),
                    AbstractMicronautJavaCodegen.OPT_USE_AUTH, true
                ));
            })
            .withDefinitionFile(new URI("src/test/resources/3_0/security.yml"))
            .build();

        assertThrows(IllegalArgumentException.class, entrypoint::generate);
    }

    @Test
    @DisplayName("Per-operation user parameter class is applied by the Java server generator")
    void testJavaServerPerOperationUserParameterClassIsApplied(@TempDir File outputDir) throws IOException {
        MicronautCodeGeneratorEntryPoint.builder()
            .forJavaServer(spec -> spec
                .withAuthentication(true)
                .withPerOperationUserParameterClass(Map.of("save", "com.example.SaveUser", "save-public", "com.example.PublicUser")))
            .withOptions(options -> options.withLang(MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.JAVA))
            .withDefinitionFile(new File("src/test/resources/3_0/security.yml").toURI())
            .withOutputDirectory(outputDir)
            .withOutputs(MicronautCodeGeneratorEntryPoint.OutputKind.APIS)
            .build()
            .generate();

        var api = outputDir.toPath().resolve("src/main/java/org/openapitools/api/DefaultApi.java").toString();
        assertFileContains(api,
            "import com.example.SaveUser;",
            "import com.example.PublicUser;",
            "SaveUser authentication",
            "@Nullable(inherited = true) PublicUser authentication");
    }

    @Test
    @DisplayName("Per-operation user parameter class is applied by the Kotlin server generator")
    void testKotlinServerPerOperationUserParameterClassIsApplied(@TempDir File outputDir) throws IOException {
        MicronautCodeGeneratorEntryPoint.builder()
            .forKotlinServer(spec -> spec
                .withAuthentication(true)
                .withPerOperationUserParameterClass(Map.of("save", "com.example.SaveUser", "save-public", "com.example.PublicUser")))
            .withOptions(options -> options.withLang(MicronautCodeGeneratorOptionsBuilder.GeneratorLanguage.KOTLIN))
            .withDefinitionFile(new File("src/test/resources/3_0/security.yml").toURI())
            .withOutputDirectory(outputDir)
            .withOutputs(MicronautCodeGeneratorEntryPoint.OutputKind.APIS)
            .build()
            .generate();

        var api = outputDir.toPath().resolve("src/main/kotlin/org/openapitools/api/DefaultApi.kt").toString();
        assertFileContains(api,
            "import com.example.SaveUser",
            "import com.example.PublicUser",
            "authentication: SaveUser",
            "@Nullable authentication: PublicUser? = null");
    }

    private static class TestGenerator extends AbstractMicronautJavaCodegen<TestBuilder> {

        private String value;

        @Override
        public boolean isServer() {
            return false;
        }

        @Override
        public TestBuilder optionsBuilder() {
            return new TestBuilder() {
                @Override
                public TestBuilder withValue(String v) {
                    value = v;
                    return this;
                }
            };
        }

    }

    public interface TestBuilder extends GeneratorOptionsBuilder {

        TestBuilder withValue(String value);
    }
}
