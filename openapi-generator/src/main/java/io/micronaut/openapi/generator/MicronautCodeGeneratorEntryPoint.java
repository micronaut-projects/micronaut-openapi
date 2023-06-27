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
package io.micronaut.openapi.generator;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Main entry point for Micronaut OpenAPI code generation.
 */
public final class MicronautCodeGeneratorEntryPoint {

    private final URI definitionFile;
    private final File outputDirectory;
    private final AbstractMicronautJavaCodegen<?> codeGenerator;
    private final EnumSet<OutputKind> outputs;
    private final Options options;
    private final JavaMicronautServerCodegen.ServerOptions serverOptions;
    private final JavaMicronautClientCodegen.ClientOptions clientOptions;

    private MicronautCodeGeneratorEntryPoint(URI definitionFile,
                                             File outputDirectory,
                                             AbstractMicronautJavaCodegen<?> codeGenerator,
                                             EnumSet<OutputKind> outputs,
                                             Options options,
                                             JavaMicronautServerCodegen.ServerOptions serverOptions,
                                             JavaMicronautClientCodegen.ClientOptions clientOptions) {
        this.definitionFile = definitionFile;
        this.outputDirectory = outputDirectory;
        this.codeGenerator = codeGenerator;
        this.outputs = outputs;
        this.options = options;
        this.serverOptions = serverOptions;
        this.clientOptions = clientOptions;
    }

    private static void withPath(File file, Consumer<? super String> action) {
        if (file == null) {
            return;
        }
        try {
            String path = file.getCanonicalPath();
            action.accept(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs code generation.
     */
    public void generate() {
        var openAPI = new OpenAPIParser()
            .readLocation(definitionFile.toString(), null, new ParseOptions()).getOpenAPI();

        // Configure codegen
        withPath(outputDirectory, codeGenerator::setOutputDir);

        // Disable timestamps are it makes builds non preproducible
        codeGenerator.setHideGenerationTimestamp(true);

        configureOptions();

        // Create input
        ClientOptInput input = new ClientOptInput();
        input.openAPI(openAPI);
        input.config(codeGenerator);

        // Generate
        DefaultGenerator generator = new DefaultGenerator();
        for (OutputKind outputKind : OutputKind.values()) {
            generator.setGeneratorPropertyDefault(outputKind.generatorProperty, "false");
        }
        for (OutputKind outputKind : outputs) {
            generator.setGeneratorPropertyDefault(outputKind.generatorProperty, "true");
        }

        generator.opts(input).generate();
    }

    private void configureOptions() {
        if (options == null) {
            return;
        }
        if (options.modelPackage != null) {
            codeGenerator.setModelPackage(options.modelPackage);
        }
        if (options.apiPackage != null) {
            codeGenerator.setApiPackage(options.apiPackage);
        }
        if (options.invokerPackage != null) {
            codeGenerator.setInvokerPackage(options.invokerPackage);
        }
        if (options.artifactId != null) {
            codeGenerator.setArtifactId(options.artifactId);
        }
        if (options.parameterMappings != null) {
            codeGenerator.addParameterMappings(options.parameterMappings);
        }
        if (options.responseBodyMappings != null) {
            codeGenerator.addResponseBodyMappings(options.responseBodyMappings);
        }
        codeGenerator.setReactive(options.reactive);
        codeGenerator.setWrapInHttpResponse(options.wrapInHttpResponse);
        codeGenerator.setUseOptional(options.optional);
        codeGenerator.setUseBeanValidation(options.beanValidation);
        codeGenerator.setTestTool(options.testFramework.value);
        codeGenerator.setSerializationLibrary(options.serializationLibraryKind().name());
        codeGenerator.setDateTimeLibrary(options.dateTimeFormat().name());
        configureServerOptions();
        configureClientOptions();
        codeGenerator.processOpts();
    }

    private void configureServerOptions() {
        if (serverOptions != null && codeGenerator instanceof JavaMicronautServerCodegen serverCodegen) {
            if (serverOptions.controllerPackage() != null) {
                serverCodegen.setControllerPackage(serverOptions.controllerPackage());
            }
            serverCodegen.setGenerateImplementationFiles(serverOptions.generateImplementationFiles());
            serverCodegen.setGenerateOperationsToReturnNotImplemented(serverOptions.generateOperationsToReturnNotImplemented());
            serverCodegen.setGenerateControllerFromExamples(serverOptions.generateControllerFromExamples());
            serverCodegen.setUseAuth(serverOptions.useAuth());
        }
    }

    public void configureClientOptions() {
        if (clientOptions != null && codeGenerator instanceof JavaMicronautClientCodegen clientCodegen) {
            if (clientOptions.additionalClientTypeAnnotations() != null) {
                clientCodegen.setAdditionalClientTypeAnnotations(clientOptions.additionalClientTypeAnnotations());
            }
            if (clientOptions.clientId() != null) {
                clientCodegen.setClientId(clientCodegen.clientId);
            }
            if (clientOptions.authorizationFilterPattern() != null) {
                clientCodegen.setAuthorizationFilterPattern(clientCodegen.authorizationFilterPattern);
            }
            if (clientOptions.basePathSeparator() != null) {
                clientCodegen.setBasePathSeparator(clientCodegen.basePathSeparator);
            }
            clientCodegen.setConfigureAuthorization(clientOptions.useAuth());
        }
    }

    /**
     * Returns a code generator builder.
     *
     * @return the builder
     */
    public static MicronautCodeGeneratorBuilder builder() {
        return new DefaultBuilder();
    }

    /**
     * The different output kinds that the generator supports.
     */
    public enum OutputKind {
        MODELS(CodegenConstants.MODELS, DefaultBuilder.HAS_OUTPUT),
        MODEL_TESTS(CodegenConstants.MODEL_TESTS, DefaultBuilder.HAS_OUTPUT),
        MODEL_DOCS(CodegenConstants.MODEL_DOCS, DefaultBuilder.HAS_OUTPUT),
        APIS(CodegenConstants.APIS, DefaultBuilder.HAS_OUTPUT),
        API_TESTS(CodegenConstants.API_TESTS, DefaultBuilder.HAS_OUTPUT),
        API_DOCS(CodegenConstants.API_DOCS, DefaultBuilder.HAS_OUTPUT),
        SUPPORTING_FILES(CodegenConstants.SUPPORTING_FILES, DefaultBuilder.HAS_OUTPUT);

        private final String generatorProperty;
        private final Consumer<DefaultBuilder> validationAction;

        OutputKind(String generatorProperty, Consumer<DefaultBuilder> validationAction) {
            this.generatorProperty = generatorProperty;
            this.validationAction = validationAction;
        }

        public static OutputKind of(String name) {
            for (OutputKind kind : values()) {
                if (kind.name().equals(name) || kind.generatorProperty.equals(name)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("Unknown output kind '" + name + "'");
        }
    }

    private static class DefaultBuilder implements MicronautCodeGeneratorBuilder {

        private static final Consumer<DefaultBuilder> HAS_OUTPUT = b -> Objects.requireNonNull(b.outputDirectory, "Sources directory must not be null");

        private Options options;
        private AbstractMicronautJavaCodegen<?> codeGenerator;
        private URI definitionFile;
        private File outputDirectory;
        private final EnumSet<OutputKind> outputs = EnumSet.noneOf(OutputKind.class);
        private JavaMicronautServerCodegen.ServerOptions serverOptions;
        private JavaMicronautClientCodegen.ClientOptions clientOptions;

        @Override
        public <B extends GeneratorOptionsBuilder, G extends MicronautCodeGenerator<B>> MicronautCodeGeneratorBuilder forCodeGenerator(G generator, Consumer<? super B> configuration) {
            codeGenerator = (AbstractMicronautJavaCodegen<?>) generator;
            var builder = generator.optionsBuilder();
            configuration.accept(builder);
            return this;
        }

        @Override
        public MicronautCodeGeneratorBuilder forClient(Consumer<? super JavaMicronautClientOptionsBuilder> clientOptionsSpec) {
            codeGenerator = new JavaMicronautClientCodegen();
            var clientOptionsBuilder = new JavaMicronautClientCodegen.DefaultClientOptionsBuilder();
            clientOptionsSpec.accept(clientOptionsBuilder);
            clientOptions = clientOptionsBuilder.build();
            return this;
        }

        @Override
        public MicronautCodeGeneratorBuilder forServer(Consumer<? super JavaMicronautServerOptionsBuilder> serverOptionsSpec) {
            codeGenerator = new JavaMicronautServerCodegen();
            var serverOptionsBuilder = new JavaMicronautServerCodegen.DefaultServerOptionsBuilder();
            serverOptionsSpec.accept(serverOptionsBuilder);
            serverOptions = serverOptionsBuilder.build();
            return this;
        }

        @Override
        public MicronautCodeGeneratorBuilder withDefinitionFile(URI definitionFile) {
            this.definitionFile = definitionFile;
            return this;
        }

        @Override
        public MicronautCodeGeneratorBuilder withOutputDirectory(File outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        @Override
        public MicronautCodeGeneratorBuilder withOutputs(OutputKind... elements) {
            outputs.addAll(Arrays.asList(elements));
            return this;
        }

        @Override
        public MicronautCodeGeneratorBuilder withOptions(Consumer<? super MicronautCodeGeneratorOptionsBuilder> optionsConfigurer) {
            DefaultOptionsBuilder builder = new DefaultOptionsBuilder();
            optionsConfigurer.accept(builder);
            options = builder.build();
            return this;
        }

        private void validate() {
            Objects.requireNonNull(definitionFile, "OpenAPI definition file must not be null");
            Objects.requireNonNull(codeGenerator, "You must select either server or client generation");
            for (OutputKind output : outputs) {
                output.validationAction.accept(this);
            }
        }

        @Override
        public MicronautCodeGeneratorEntryPoint build() {
            validate();
            return new MicronautCodeGeneratorEntryPoint(definitionFile,
                outputDirectory,
                codeGenerator,
                outputs,
                options,
                serverOptions,
                clientOptions);
        }

        private static class DefaultOptionsBuilder implements MicronautCodeGeneratorOptionsBuilder {

            private String apiPackage;
            private String artifactId;
            private boolean beanValidation = true;
            private String invokerPackage;
            private String modelPackage;
            private List<AbstractMicronautJavaCodegen.ParameterMapping> parameterMappings;
            private List<AbstractMicronautJavaCodegen.ResponseBodyMapping> responseBodyMappings;
            private boolean optional = false;
            private boolean reactive = true;
            private boolean wrapInHttpResponse;
            private TestFramework testFramework = TestFramework.JUNIT5;
            private SerializationLibraryKind serializationLibraryKind = SerializationLibraryKind.MICRONAUT_SERDE_JACKSON;
            private DateTimeFormat dateTimeFormat = DateTimeFormat.ZONED_DATETIME;

            @Override
            public MicronautCodeGeneratorOptionsBuilder withApiPackage(String apiPackage) {
                this.apiPackage = apiPackage;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withInvokerPackage(String invokerPackage) {
                this.invokerPackage = invokerPackage;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withModelPackage(String modelPackage) {
                this.modelPackage = modelPackage;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withArtifactId(String artifactId) {
                this.artifactId = artifactId;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withParameterMappings(List<AbstractMicronautJavaCodegen.ParameterMapping> parameterMappings) {
                this.parameterMappings = parameterMappings;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withResponseBodyMappings(List<AbstractMicronautJavaCodegen.ResponseBodyMapping> responseBodyMappings) {
                this.responseBodyMappings = responseBodyMappings;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withReactive(boolean reactive) {
                this.reactive = reactive;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withWrapInHttpResponse(boolean wrapInHttpResponse) {
                this.wrapInHttpResponse = wrapInHttpResponse;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withBeanValidation(boolean beanValidation) {
                this.beanValidation = beanValidation;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withOptional(boolean optional) {
                this.optional = optional;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withTestFramework(TestFramework testFramework) {
                this.testFramework = testFramework;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withSerializationLibrary(SerializationLibraryKind library) {
                serializationLibraryKind = library;
                return this;
            }

            @Override
            public MicronautCodeGeneratorOptionsBuilder withDateTimeLibrary(DateTimeFormat library) {
                dateTimeFormat = library;
                return this;
            }

            private Options build() {
                return new Options(apiPackage, modelPackage, invokerPackage, artifactId, parameterMappings, responseBodyMappings, beanValidation, optional, reactive, wrapInHttpResponse, testFramework, serializationLibraryKind, dateTimeFormat);
            }
        }
    }

    /**
     * The different test frameworks which are supported
     * by this generator.
     */
    public enum TestFramework {
        JUNIT5(AbstractMicronautJavaCodegen.OPT_TEST_JUNIT),
        SPOCK(AbstractMicronautJavaCodegen.OPT_TEST_SPOCK);

        private final String value;

        TestFramework(String value) {
            this.value = value;
        }
    }

    private record Options(
        String apiPackage,
        String modelPackage,
        String invokerPackage,
        String artifactId,
        List<AbstractMicronautJavaCodegen.ParameterMapping> parameterMappings,
        List<AbstractMicronautJavaCodegen.ResponseBodyMapping> responseBodyMappings,
        boolean beanValidation,
        boolean optional,
        boolean reactive,
        boolean wrapInHttpResponse,
        TestFramework testFramework,
        SerializationLibraryKind serializationLibraryKind,
        MicronautCodeGeneratorOptionsBuilder.DateTimeFormat dateTimeFormat
    ) {
    }

}
