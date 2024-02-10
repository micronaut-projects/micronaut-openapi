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
public interface KotlinMicronautServerOptionsBuilder extends GeneratorOptionsBuilder {

    /**
     * Sets the package name of generated controller classes.
     *
     * @param controllerPackage the package name
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withControllerPackage(String controllerPackage);

    /**
     * Whether to generate controller implementations that need to be filled in.
     *
     * @param generateImplementationFiles the implementation files flag
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withGenerateImplementationFiles(boolean generateImplementationFiles);

    /**
     * If set to true, controller operations will return not implemented status.
     *
     * @param generateOperationsToReturnNotImplemented the not implemented flag
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented);

    /**
     * If set to true, controllers will be generated using examples.
     *
     * @param generateControllerFromExamples the examples flag
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withGenerateControllerFromExamples(boolean generateControllerFromExamples);

    /**
     * If set to true, generated code will add support for authentication.
     *
     * @param useAuth the authentication flag
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withAuthentication(boolean useAuth);

    /**
     * If set to true, generated code will be with Flux{@literal <}?> instead Mono{@literal <}List{@literal <}?>>.
     *
     * @param fluxForArrays generate code with Flux{@literal <}?> instead Mono{@literal <}List{@literal <}?>> or not
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withFluxForArrays(boolean fluxForArrays);

    /**
     * If set to true, the generated code will pluralize parameters and properties for arrays.
     *
     * @param plural generate pluralized parameters and properties for arrays
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withPlural(boolean plural);

    /**
     * If set to true, generated code will be with jakarta.annotation.Generated annotation.
     *
     * @param generatedAnnotation generate code with jakarta.annotation.Generated annotation or not
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation);

    /**
     * If set to true, generated compatible code with micronaut-aot.
     *
     * @param aot generate compatible code with micronaut-aot or not
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withAot(boolean aot);

    /**
     * If set to true, generated code will be fully compatible with KSP, but not 100% with KAPT.
     *
     * @param ksp do we need to generate code compatible only with KSP
     *
     * @return this builder
     */
    KotlinMicronautServerOptionsBuilder withKsp(boolean ksp);
}
