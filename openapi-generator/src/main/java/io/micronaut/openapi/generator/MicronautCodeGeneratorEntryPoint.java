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
    private final AbstractMicronautJavaCodegen codeGenerator;
    private final EnumSet<OutputKind> outputs;
    private final Options options;
    private final ServerOptions serverOptions;
    private final ClientOptions clientOptions;

    private MicronautCodeGeneratorEntryPoint(URI definitionFile,
                                             File outputDirectory,
                                             AbstractMicronautJavaCodegen codeGenerator,
                                             EnumSet<OutputKind> outputs,
                                             Options options,
                                             ServerOptions serverOptions,
                                             ClientOptions clientOptions) {
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
        codeGenerator.setReactive(options.reactive);
        codeGenerator.setWrapInHttpResponse(options.wrapInHttpResponse);
        codeGenerator.setUseOptional(options.optional);
        codeGenerator.setUseBeanValidation(options.beanValidation);
        codeGenerator.setTestTool(options.testFramework.value);
        configureServerOptions();
        configureClientOptions();
    }

    private void configureServerOptions() {
        if (serverOptions != null && codeGenerator instanceof JavaMicronautServerCodegen serverCodegen) {
            if (serverOptions.controllerPackage != null) {
                serverCodegen.setControllerPackage(serverOptions.controllerPackage);
            }
            serverCodegen.setGenerateControllerAsAbstract(serverOptions.generateAbstractClasses);
            serverCodegen.setGenerateOperationsToReturnNotImplemented(serverOptions.generateOperationsToReturnNotImplemented);
            serverCodegen.setGenerateControllerFromExamples(serverOptions.generateControllerFromExamples);
            serverCodegen.setUseAuth(serverCodegen.useAuth);
        }
    }

    public void configureClientOptions() {
        if (clientOptions != null && codeGenerator instanceof JavaMicronautClientCodegen clientCodegen) {
            if (clientOptions.additionalClientTypeAnnotations != null) {
                clientCodegen.setAdditionalClientTypeAnnotations(clientOptions.additionalClientTypeAnnotations);
            }
            if (clientOptions.clientId != null) {
                clientCodegen.setClientId(clientCodegen.clientId);
            }
            if (clientOptions.authorizationFilterPattern != null) {
                clientCodegen.setAuthorizationFilterPattern(clientCodegen.authorizationFilterPattern);
            }
            if (clientOptions.basePathSeparator != null) {
                clientCodegen.setBasePathSeparator(clientCodegen.basePathSeparator);
            }
            clientCodegen.setConfigureAuthorization(clientOptions.useAuth);
        }
    }

    /**
     * Returns a code generator builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * A code generator builder.
     */
    @SuppressWarnings("UnusedReturnValue")
    public interface Builder {
        /**
         * Sets the code generator which should be used (e.g server or client).
         *
         * @param generator the generator
         * @return this builder
         */
        Builder forCodeGenerator(MicronautCodeGenerator generator);

        /**
         * Configures the code generator to create a client.
         *
         * @return this builder
         */
        default Builder forClient() {
            return forClient(o -> {
            });
        }

        /**
         * Configures the code generator to create a client.
         *
         * @param clientOptionsSpec the client options
         * @return this builder
         */
        Builder forClient(Consumer<? super ClientOptionsBuilder> clientOptionsSpec);

        /**
         * Configures the code generator to create a server.
         *
         * @return this builder
         */
        default Builder forServer() {
            return forServer(o -> {
            });
        }

        /**
         * Configures the code generator to create a server.
         *
         * @param serverOptionsSpec the server options
         * @return this builder
         */
        Builder forServer(Consumer<? super ServerOptionsBuilder> serverOptionsSpec);

        /**
         * Sets the URI to the OpenAPI definition file.
         * It is recommended to use a local path instead of using
         * a remote URI.
         *
         * @param definitionFile the definition file
         * @return this builder
         */
        Builder withDefinitionFile(URI definitionFile);

        /**
         * Sets the directory where to output the generated sources.
         *
         * @param outputDirectory the generated sources output directory
         * @return this builder
         */
        Builder withOutputDirectory(File outputDirectory);

        /**
         * Sets which output files should be generated.
         *
         * @param elements the different elements to generate
         * @return this builder
         */
        Builder withOutputs(OutputKind... elements);

        /**
         * Configures the code generation options.
         *
         * @param optionsConfigurer the configuration
         * @return this builder
         */
        Builder withOptions(Consumer<? super OptionsBuilder> optionsConfigurer);

        /**
         * Returns a configured code generator.
         *
         * @return the configured code generator
         */
        MicronautCodeGeneratorEntryPoint build();
    }

    /**
     * Builder for generic options that the Micronaut code generator supports.
     */
    @SuppressWarnings("UnusedReturnValue")
    public interface OptionsBuilder {
        /**
         * Sets the package of the generated API classes.
         *
         * @param apiPackage the package name
         * @return this builder
         */
        OptionsBuilder withApiPackage(String apiPackage);

        /**
         * Sets the package of the generated invoker classes.
         *
         * @param invokerPackage the package name
         * @return this builder
         */
        OptionsBuilder withInvokerPackage(String invokerPackage);

        /**
         * Sets the package of the generated model classes.
         *
         * @param modelPackage the package name
         * @return this builder
         */
        OptionsBuilder withModelPackage(String modelPackage);

        /**
         * Sets the artifact id of the project.
         *
         * @param artifactId the artifact id
         * @return this builder
         */
        OptionsBuilder withArtifactId(String artifactId);

        /**
         * If set to true, the generator will use reactive types.
         *
         * @param reactive the reactive flag
         * @return this builder
         */
        OptionsBuilder withReactive(boolean reactive);

        /**
         * If true, the generated client will use responses wrapped in HttpResponse.
         *
         * @param wrapInHttpResponse the wrapping flag
         * @return this builder
         */
        OptionsBuilder withWrapInHttpResponse(boolean wrapInHttpResponse);

        /**
         * If set to true, the generated code will use bean validation.
         *
         * @param beanValidation the bean validation flag
         * @return this builder
         */
        OptionsBuilder withBeanValidation(boolean beanValidation);

        /**
         * If set to true, the generated code will make use of {@link java.util.Optional}.
         *
         * @param optional the optional flag
         * @return this builder
         */
        OptionsBuilder withOptional(boolean optional);

        /**
         * Configures the test framework to use for generated tests.
         *
         * @param testFramework the test framework
         * @return this builder
         */
        OptionsBuilder withTestFramework(TestFramework testFramework);
    }

    /**
     * Configures options which are specific to the server code.
     */
    @SuppressWarnings("UnusedReturnValue")
    public interface ServerOptionsBuilder {
        /**
         * Sets the package name of generated controller classes.
         *
         * @param controllerPackage the package name
         * @return this builder
         */
        ServerOptionsBuilder withControllerPackage(String controllerPackage);

        /**
         * If set to true, the code generator will output abstract classes
         * for the controller, instead of concrete implementations.
         *
         * @param abstractClasses the abstract classes flag
         * @return this builder
         */
        ServerOptionsBuilder withGenerateAbstractClasses(boolean abstractClasses);

        /**
         * If set to true, controller operations will return not implemented status.
         *
         * @param generateOperationsToReturnNotImplemented the not implemented flag
         * @return this builder
         */
        ServerOptionsBuilder withGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented);

        /**
         * If set to true, controllers will be generated using examples.
         *
         * @param generateControllerFromExamples the examples flag
         * @return this builder
         */
        ServerOptionsBuilder withGenerateControllerFromExamples(boolean generateControllerFromExamples);

        /**
         * If set to true, generated code will add support for authentication.
         *
         * @param useAuth the authentication flag
         * @return this builder
         */
        ServerOptionsBuilder withAuthentication(boolean useAuth);
    }

    /**
     * The client generator options builder.
     */
    @SuppressWarnings("UnusedReturnValue")
    public interface ClientOptionsBuilder {
        /**
         * If set to true the client will be configured for authorization.
         *
         * @param useAuth the authorization flag
         * @return this builder
         */
        ClientOptionsBuilder withAuthorization(boolean useAuth);

        /**
         * Sets the authorization filter pattern.
         *
         * @param authorizationFilterPattern the filter pattern
         * @return this builder
         */
        ClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern);

        /**
         * Sets the client id.
         *
         * @param clientId the client id
         * @return this builder
         */
        ClientOptionsBuilder withClientId(String clientId);

        /**
         * Sets annotations for client type (class level annotations).
         *
         * @param additionalClientTypeAnnotations the type annotations
         * @return this builder
         */
        ClientOptionsBuilder withAdditionalClientTypeAnnotations(List<String> additionalClientTypeAnnotations);

        /**
         * Sets the separator to use between the application name and base path when referencing the property.
         *
         * @param basePathSeparator the base path separator
         * @return this builder
         */
        ClientOptionsBuilder withBasePathSeparator(String basePathSeparator);
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

    private static class DefaultBuilder implements Builder {
        private static final Consumer<DefaultBuilder> HAS_OUTPUT = b -> Objects.requireNonNull(b.outputDirectory, "Sources directory must not be null");
        private Options options;
        private AbstractMicronautJavaCodegen codeGenerator;
        private URI definitionFile;
        private File outputDirectory;
        private final EnumSet<OutputKind> outputs = EnumSet.noneOf(OutputKind.class);
        private ServerOptions serverOptions;
        private ClientOptions clientOptions;

        @Override
        public Builder forCodeGenerator(MicronautCodeGenerator generator) {
            this.codeGenerator = (AbstractMicronautJavaCodegen) generator;
            return this;
        }

        @Override
        public Builder forClient(Consumer<? super ClientOptionsBuilder> clientOptionsSpec) {
            this.codeGenerator = new JavaMicronautClientCodegen();
            var clientOptionsBuilder = new DefaultClientOptionsBuilder();
            clientOptionsSpec.accept(clientOptionsBuilder);
            this.clientOptions = clientOptionsBuilder.build();
            return this;
        }

        @Override
        public Builder forServer(Consumer<? super ServerOptionsBuilder> serverOptionsSpec) {
            this.codeGenerator = new JavaMicronautServerCodegen();
            var serverOptionsBuilder = new DefaultServerOptionsBuilder();
            serverOptionsSpec.accept(serverOptionsBuilder);
            this.serverOptions = serverOptionsBuilder.build();
            return this;
        }

        @Override
        public Builder withDefinitionFile(URI definitionFile) {
            this.definitionFile = definitionFile;
            return this;
        }

        @Override
        public Builder withOutputDirectory(File outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        @Override
        public Builder withOutputs(OutputKind... elements) {
            this.outputs.addAll(Arrays.asList(elements));
            return this;
        }

        @Override
        public Builder withOptions(Consumer<? super OptionsBuilder> optionsConfigurer) {
            DefaultOptionsBuilder builder = new DefaultOptionsBuilder();
            optionsConfigurer.accept(builder);
            this.options = builder.build();
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

        private static class DefaultOptionsBuilder implements OptionsBuilder {
            private String apiPackage;
            private String artifactId;
            private boolean beanValidation = true;
            private String invokerPackage;
            private String modelPackage;
            private boolean optional = false;
            private boolean reactive = true;
            private boolean wrapInHttpResponse;
            private TestFramework testFramework = TestFramework.JUNIT5;

            @Override
            public OptionsBuilder withApiPackage(String apiPackage) {
                this.apiPackage = apiPackage;
                return this;
            }

            @Override
            public OptionsBuilder withInvokerPackage(String invokerPackage) {
                this.invokerPackage = invokerPackage;
                return this;
            }

            @Override
            public OptionsBuilder withModelPackage(String modelPackage) {
                this.modelPackage = modelPackage;
                return this;
            }

            @Override
            public OptionsBuilder withArtifactId(String artifactId) {
                this.artifactId = artifactId;
                return this;
            }

            @Override
            public OptionsBuilder withReactive(boolean reactive) {
                this.reactive = reactive;
                return this;
            }

            @Override
            public OptionsBuilder withWrapInHttpResponse(boolean wrapInHttpResponse) {
                this.wrapInHttpResponse = wrapInHttpResponse;
                return this;
            }

            @Override
            public OptionsBuilder withBeanValidation(boolean beanValidation) {
                this.beanValidation = beanValidation;
                return this;
            }

            @Override
            public OptionsBuilder withOptional(boolean optional) {
                this.optional = optional;
                return this;
            }

            @Override
            public OptionsBuilder withTestFramework(TestFramework testFramework) {
                this.testFramework = testFramework;
                return this;
            }

            private Options build() {
                return new Options(apiPackage, modelPackage, invokerPackage, artifactId, beanValidation, optional, reactive, wrapInHttpResponse, testFramework);
            }
        }
    }

    private static class DefaultServerOptionsBuilder implements ServerOptionsBuilder {
        private String controllerPackage;
        private boolean generateAbstractClasses;
        private boolean generateControllerFromExamples;
        private boolean generateOperationsToReturnNotImplemented = true;
        private boolean useAuth = true;

        @Override
        public ServerOptionsBuilder withControllerPackage(String controllerPackage) {
            this.controllerPackage = controllerPackage;
            return this;
        }

        @Override
        public ServerOptionsBuilder withGenerateAbstractClasses(boolean abstractClasses) {
            this.generateAbstractClasses = abstractClasses;
            return this;
        }

        @Override
        public ServerOptionsBuilder withGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented) {
            this.generateOperationsToReturnNotImplemented = generateOperationsToReturnNotImplemented;
            return this;
        }

        @Override
        public ServerOptionsBuilder withGenerateControllerFromExamples(boolean generateControllerFromExamples) {
            this.generateControllerFromExamples = generateControllerFromExamples;
            return this;
        }

        @Override
        public ServerOptionsBuilder withAuthentication(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        ServerOptions build() {
            return new ServerOptions(controllerPackage, generateAbstractClasses, generateOperationsToReturnNotImplemented, generateControllerFromExamples, useAuth);
        }
    }

    private static class DefaultClientOptionsBuilder implements ClientOptionsBuilder {
        private List<String> additionalClientTypeAnnotations;
        private String authorizationFilterPattern;
        private String basePathSeparator;
        private String clientId;
        private boolean useAuth;

        @Override
        public ClientOptionsBuilder withAuthorization(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        @Override
        public ClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern) {
            this.authorizationFilterPattern = authorizationFilterPattern;
            return this;
        }

        @Override
        public ClientOptionsBuilder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public ClientOptionsBuilder withAdditionalClientTypeAnnotations(List<String> additionalClientTypeAnnotations) {
            this.additionalClientTypeAnnotations = additionalClientTypeAnnotations;
            return this;
        }

        @Override
        public ClientOptionsBuilder withBasePathSeparator(String basePathSeparator) {
            this.basePathSeparator = basePathSeparator;
            return this;
        }

        private ClientOptions build() {
            return new ClientOptions(
                additionalClientTypeAnnotations,
                authorizationFilterPattern,
                basePathSeparator,
                clientId,
                useAuth);
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
        boolean beanValidation,
        boolean optional,
        boolean reactive,
        boolean wrapInHttpResponse,
        TestFramework testFramework) {
    }

    private record ServerOptions(
        String controllerPackage,
        boolean generateAbstractClasses,
        boolean generateOperationsToReturnNotImplemented,
        boolean generateControllerFromExamples,
        boolean useAuth
    ) {
    }

    private record ClientOptions(
        List<String> additionalClientTypeAnnotations,
        String authorizationFilterPattern,
        String basePathSeparator,
        String clientId,
        boolean useAuth
    ) {
    }
}
