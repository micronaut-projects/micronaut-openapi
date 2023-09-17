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
 * The client generator options builder.
 */
@SuppressWarnings("UnusedReturnValue")
public interface JavaMicronautClientOptionsBuilder extends GeneratorOptionsBuilder {

    /**
     * If set to true the client will be configured for authorization.
     *
     * @param useAuth the authorization flag
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withAuthorization(boolean useAuth);

    /**
     * Sets the authorization filter pattern.
     *
     * @param authorizationFilterPattern the filter pattern
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern);

    /**
     * Sets the client id.
     *
     * @param clientId the client id
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withClientId(String clientId);

    /**
     * Sets annotations for client type (class level annotations).
     *
     * @param additionalClientTypeAnnotations the type annotations
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withAdditionalClientTypeAnnotations(List<String> additionalClientTypeAnnotations);

    /**
     * Sets the separator to use between the application name and base path when referencing the property.
     *
     * @param basePathSeparator the base path separator
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withBasePathSeparator(String basePathSeparator);

    /**
     * If set to true, generated code will be with lombok annotations.
     *
     * @param lombok generate code with lombok annotations or not
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withLombok(boolean lombok);

    /**
     * If set to true, generated code will be with Flux{@literal <}?> instead Mono{@literal <}List{@literal <}?>>.
     *
     * @param fluxForArrays generate code with Flux{@literal <}?> instead Mono{@literal <}List{@literal <}?>> or not
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withFluxForArrays(boolean fluxForArrays);

    /**
     * If set to true, generated code will be with jakarta.annotation.Generated annotation.
     *
     * @param generatedAnnotation generate code with jakarta.annotation.Generated annotation or not
     * @return this builder
     */
    JavaMicronautClientOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation);
}
