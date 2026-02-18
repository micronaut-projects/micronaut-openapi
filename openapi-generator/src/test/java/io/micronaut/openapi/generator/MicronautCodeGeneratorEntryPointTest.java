package io.micronaut.openapi.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

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
