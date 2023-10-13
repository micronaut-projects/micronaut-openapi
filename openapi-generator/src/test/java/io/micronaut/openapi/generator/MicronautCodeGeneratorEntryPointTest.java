package io.micronaut.openapi.generator;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
