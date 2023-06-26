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

/**
 * Configures options which are specific to the server code.
 */
@SuppressWarnings("UnusedReturnValue")
public interface JavaMicronautServerOptionsBuilder extends GeneratorOptionsBuilder {

    /**
     * Sets the package name of generated controller classes.
     *
     * @param controllerPackage the package name
     * @return this builder
     */
    JavaMicronautServerOptionsBuilder withControllerPackage(String controllerPackage);

    /**
     * Whether to generate controller implementations that need to be filled in.
     *
     * @param generateImplementationFiles the implementation files flag
     * @return this builder
     */
    JavaMicronautServerOptionsBuilder withGenerateImplementationFiles(boolean generateImplementationFiles);

    /**
     * If set to true, controller operations will return not implemented status.
     *
     * @param generateOperationsToReturnNotImplemented the not implemented flag
     * @return this builder
     */
    JavaMicronautServerOptionsBuilder withGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented);

    /**
     * If set to true, controllers will be generated using examples.
     *
     * @param generateControllerFromExamples the examples flag
     * @return this builder
     */
    JavaMicronautServerOptionsBuilder withGenerateControllerFromExamples(boolean generateControllerFromExamples);

    /**
     * If set to true, generated code will add support for authentication.
     *
     * @param useAuth the authentication flag
     * @return this builder
     */
    JavaMicronautServerOptionsBuilder withAuthentication(boolean useAuth);
}
