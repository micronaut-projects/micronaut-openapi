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

import java.io.File;
import java.net.URI;
import java.util.function.Consumer;

/**
 * A code generator builder.
 */
@SuppressWarnings("UnusedReturnValue")
public interface MicronautCodeGeneratorBuilder {

    /**
     * Sets the code generator which should be used (e.g server or client).
     *
     * @param generator the generator
     * @param configuration the configuration block for this generator
     * @param <B> the type of the builder used to configure the options specific to this generator
     * @param <G> the type of the generator
     * @return this builder
     */
    <B extends GeneratorOptionsBuilder, G extends MicronautCodeGenerator<B>> MicronautCodeGeneratorBuilder forCodeGenerator(G generator, Consumer<? super B> configuration);

    /**
     * Configures the code generator to create a client.
     *
     * @return this builder
     */
    default MicronautCodeGeneratorBuilder forClient() {
        return forClient(o -> {
        });
    }

    /**
     * Configures the code generator to create a client.
     *
     * @param clientOptionsSpec the client options
     * @return this builder
     */
    MicronautCodeGeneratorBuilder forClient(Consumer<? super JavaMicronautClientOptionsBuilder> clientOptionsSpec);

    /**
     * Configures the code generator to create a server.
     *
     * @return this builder
     */
    default MicronautCodeGeneratorBuilder forServer() {
        return forServer(o -> {
        });
    }

    /**
     * Configures the code generator to create a server.
     *
     * @param serverOptionsSpec the server options
     * @return this builder
     */
    MicronautCodeGeneratorBuilder forServer(Consumer<? super JavaMicronautServerOptionsBuilder> serverOptionsSpec);

    /**
     * Sets the URI to the OpenAPI definition file.
     * It is recommended to use a local path instead of using
     * a remote URI.
     *
     * @param definitionFile the definition file
     * @return this builder
     */
    MicronautCodeGeneratorBuilder withDefinitionFile(URI definitionFile);

    /**
     * Sets the directory where to output the generated sources.
     *
     * @param outputDirectory the generated sources output directory
     * @return this builder
     */
    MicronautCodeGeneratorBuilder withOutputDirectory(File outputDirectory);

    /**
     * Sets which output files should be generated.
     *
     * @param elements the different elements to generate
     * @return this builder
     */
    MicronautCodeGeneratorBuilder withOutputs(MicronautCodeGeneratorEntryPoint.OutputKind... elements);

    /**
     * Configures the code generation options.
     *
     * @param optionsConfigurer the configuration
     * @return this builder
     */
    MicronautCodeGeneratorBuilder withOptions(Consumer<? super MicronautCodeGeneratorOptionsBuilder> optionsConfigurer);

    /**
     * Returns a configured code generator.
     *
     * @return the configured code generator
     */
    MicronautCodeGeneratorEntryPoint build();
}
