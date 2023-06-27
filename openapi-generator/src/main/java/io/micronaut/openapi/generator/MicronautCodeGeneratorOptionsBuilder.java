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

import java.util.List;

/**
 * Builder for generic options that the Micronaut code generator supports.
 */
@SuppressWarnings("UnusedReturnValue")
public interface MicronautCodeGeneratorOptionsBuilder {

    /**
     * Sets the package of the generated API classes.
     *
     * @param apiPackage the package name
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withApiPackage(String apiPackage);

    /**
     * Sets the package of the generated invoker classes.
     *
     * @param invokerPackage the package name
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withInvokerPackage(String invokerPackage);

    /**
     * Sets the package of the generated model classes.
     *
     * @param modelPackage the package name
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withModelPackage(String modelPackage);

    /**
     * Sets the artifact id of the project.
     *
     * @param artifactId the artifact id
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withArtifactId(String artifactId);

    /**
     * Add the parameter mappings.
     *
     * @param parameterMappings the parameter mappings specified by a {@link AbstractMicronautJavaCodegen.ParameterMapping} objects
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withParameterMappings(List<AbstractMicronautJavaCodegen.ParameterMapping> parameterMappings);

    /**
     * If set to true, the generator will use reactive types.
     *
     * @param reactive the reactive flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withReactive(boolean reactive);

    /**
     * If true, the generated client will use responses wrapped in HttpResponse.
     *
     * @param wrapInHttpResponse the wrapping flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withWrapInHttpResponse(boolean wrapInHttpResponse);

    /**
     * If set to true, the generated code will use bean validation.
     *
     * @param beanValidation the bean validation flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withBeanValidation(boolean beanValidation);

    /**
     * If set to true, the generated code will make use of {@link java.util.Optional}.
     *
     * @param optional the optional flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withOptional(boolean optional);

    /**
     * Configures the test framework to use for generated tests.
     *
     * @param testFramework the test framework
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withTestFramework(MicronautCodeGeneratorEntryPoint.TestFramework testFramework);

    MicronautCodeGeneratorOptionsBuilder withSerializationLibrary(SerializationLibraryKind library);
}
