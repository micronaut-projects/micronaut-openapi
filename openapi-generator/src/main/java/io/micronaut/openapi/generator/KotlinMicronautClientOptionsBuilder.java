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
public interface KotlinMicronautClientOptionsBuilder extends GeneratorOptionsBuilder {

    /**
     * If set to true the client will be configured for authorization.
     *
     * @param useAuth the authorization flag
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthorization(boolean useAuth);

    /**
     * Generate authorization classes or not.
     *
     * @param generateAuthorizationClasses Generate authorization classes or not.
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withGenerateAuthClasses(boolean generateAuthorizationClasses);

    /**
     * Generate AuthorizationFilter or not.
     *
     * @param authFilter Generate AuthorizationFilter or not.
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthFilter(boolean authFilter);

    /**
     * Generate AuthorizationFilter with support OAuth2.0 or not.
     *
     * @param useOauth if true, then AuthorizationFilter will be created with support OAuth2.0
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withUseOauth(boolean useOauth);

    /**
     * Generate HttpBasicAuthConfig class or not.
     *
     * @param useBasicAuth if true, then HttpBasicAuthConfig class will be generated
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withUseBasicAuth(boolean useBasicAuth);

    /**
     * Generate ApiKeyAuthConfig config or not.
     *
     * @param useApiKeyAuth if true, then ApiKeyAuthConfig class will be generated
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withUseApiKeyAuth(boolean useApiKeyAuth);

    /**
     * Sets the authorization filter pattern. Can be a list of strings, or single string with `;` separator
     *
     * @param authorizationFilterPattern the filter pattern
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern);

    /**
     * Sets the authorization filter pattern style. Available values: ANT, REGEX.
     * <p>
     * Default: ANT
     *
     * @param authorizationFilterPatternStyle the filter pattern style. Default: ANT
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthorizationFilterPatternStyle(String authorizationFilterPatternStyle);

    /**
     * Sets serviceId annotation property for AuthorizationFilter. Usefully, when use one filter for several clients.
     * Can be a list of strings, or single string with `;` separator.
     * <p>
     * By default, if you don't set any authFilterClientIds, value will be a list with one element - `clientId` property value.
     * If you don't want to add any serviceId annotation property for AuthorizationFilter, just set empty list (not null!).
     *
     * @param authFilterClientIds the list of client IDs, for which this filter will be used
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthFilterClientIds(List<String> authFilterClientIds);

    /**
     * The list of client IDs, for which this filter will NOT be used. Can be a list of strings,
     * or single string with `;` separator (if you set this property, by additionalProperties)
     *
     * @param authFilterExcludedClientIds the list of client IDs, for which this filter will NOT be used
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthFilterExcludedClientIds(List<String> authFilterExcludedClientIds);

    /**
     * Authorization config name. Using in config properties for HttpBasicAuthConfig and ApiKeyAuthConfig.
     * If not set, clientId value will be used.
     *
     * @param authConfigName the client id
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAuthConfigName(String authConfigName);

    /**
     * Sets the client id.
     *
     * @param clientId the client id
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withClientId(String clientId);

    /**
     * Sets annotations for client type (class level annotations). Can be a list of strings, or single string with `;` separator
     *
     * @param additionalClientTypeAnnotations the type annotations
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withAdditionalClientTypeAnnotations(List<String> additionalClientTypeAnnotations);

    /**
     * Sets the separator to use between the application name and base path when referencing the property.
     *
     * @param basePathSeparator the base path separator
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withBasePathSeparator(String basePathSeparator);

    /**
     * If set to true, the generated code will pluralize parameters and properties for arrays.
     *
     * @param plural generate pluralized parameters and properties for arrays
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withPlural(boolean plural);

    /**
     * If set to true, generated code will be with Flux{@literal <}?> instead Mono{@literal <}List{@literal <}?>>.
     *
     * @param fluxForArrays generate code with Flux{@literal <}?> instead Mono{@literal <}List{@literal <}?>> or not
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withFluxForArrays(boolean fluxForArrays);

    /**
     * If set to true, generated code will be with jakarta.annotation.Generated annotation.
     *
     * @param generatedAnnotation generate code with jakarta.annotation.Generated annotation or not
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation);

    /**
     * If set to true, Api annotation {@literal @}Client will be with `path` attribute.
     *
     * @param clientPath do we need add path attribute to {@literal @}Client annotation
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withClientPath(boolean clientPath);

    /**
     * If set to true, generated code will be fully compatible with KSP, but not 100% with KAPT.
     *
     * @param ksp do we need to generate code compatible only with KSP
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withKsp(boolean ksp);

    /**
     * If set to true, generated code will be with suspend methods.
     *
     * @param coroutines do we need to generate suspend methods
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withCoroutines(boolean coroutines);

    /**
     * Add or not @JvmOverloads annotation for classes with properties with default values. Default: false
     *
     * @param jvmOverloads if true, then @JvmOverload annotation will be added to classes with properties with default values
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withJvmOverloads(boolean jvmOverloads);

    /**
     * Add or not @JvmRecord annotation to data classes. Default: false
     *
     * @param jvmRecord if true, then @JvmRecord annotation will be added to data classes
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withJvmRecord(boolean jvmRecord);

    /**
     * Add or not @JvmField, @JvmStatic and @JvmRepeatable annotations to improve java compatibility. Default: true
     *
     * @param javaCompatibility if true, then @JvmField, @JvmStatic and @JvmRepeatable annotations wil be added where it needed to improve java compatibility
     *
     * @return this builder
     */
    KotlinMicronautClientOptionsBuilder withJavaCompatibility(boolean javaCompatibility);
}
