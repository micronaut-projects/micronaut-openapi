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

import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;

import java.util.Arrays;
import java.util.List;

/**
 * The generator for creating Micronaut clients.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class JavaMicronautClientCodegen extends AbstractMicronautJavaCodegen<JavaMicronautClientOptionsBuilder> {

    public static final String OPT_CONFIGURE_AUTH = "configureAuth";
    public static final String OPT_CONFIGURE_AUTH_FILTER_PATTERN = "configureAuthFilterPattern";
    public static final String OPT_CONFIGURE_CLIENT_ID = "configureClientId";
    public static final String ADDITIONAL_CLIENT_TYPE_ANNOTATIONS = "additionalClientTypeAnnotations";
    public static final String AUTHORIZATION_FILTER_PATTERN = "authorizationFilterPattern";
    public static final String BASE_PATH_SEPARATOR = "basePathSeparator";
    public static final String CLIENT_ID = "clientId";

    public static final String NAME = "java-micronaut-client";

    protected boolean configureAuthorization;
    protected List<String> additionalClientTypeAnnotations;
    protected String authorizationFilterPattern;
    protected String basePathSeparator = "-";
    protected String clientId;

    JavaMicronautClientCodegen() {
        super();

        title = "OpenAPI Micronaut Client";
        configureAuthorization = false;

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
            .stability(Stability.BETA)
            .build();
        additionalProperties.put("client", "true");

        cliOptions.add(CliOption.newBoolean(OPT_CONFIGURE_AUTH, "Configure all the authorization methods as specified in the file", configureAuthorization));
        cliOptions.add(CliOption.newString(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, "Additional annotations for client type(class level annotations). List separated by semicolon(;) or new line (Linux or Windows)"));
        cliOptions.add(CliOption.newString(AUTHORIZATION_FILTER_PATTERN, "Configure the authorization filter pattern for the client. Generally defined when generating clients from multiple specification files"));
        cliOptions.add(CliOption.newString(BASE_PATH_SEPARATOR, "Configure the separator to use between the application name and base path when referencing the property").defaultValue(basePathSeparator));
        cliOptions.add(CliOption.newString(CLIENT_ID, "Configure the service ID for the Client"));

        typeMapping.put("file", "byte[]");
        typeMapping.put("responseFile", "InputStream");
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getHelp() {
        return "Generates a Java Micronaut Client.";
    }

    public boolean isConfigureAuthorization() {
        return configureAuthorization;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(OPT_CONFIGURE_AUTH)) {
            configureAuthorization = convertPropertyToBoolean(OPT_CONFIGURE_AUTH);
        }
        writePropertyBack(OPT_CONFIGURE_AUTH, configureAuthorization);

        // Write property that is present in server
        writePropertyBack(OPT_USE_AUTH, true);

        writePropertyBack(OPT_CONFIGURE_AUTH_FILTER_PATTERN, false);
        writePropertyBack(OPT_CONFIGURE_CLIENT_ID, false);

        if (additionalProperties.containsKey(BASE_PATH_SEPARATOR)) {
            basePathSeparator = additionalProperties.get(BASE_PATH_SEPARATOR).toString();
        }
        writePropertyBack(BASE_PATH_SEPARATOR, basePathSeparator);

        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

        // Authorization files
        if (configureAuthorization) {
            final String authFolder = invokerFolder + "/auth";
            supportingFiles.add(new SupportingFile("client/auth/Authorization.mustache", authFolder, "Authorization.java"));
            supportingFiles.add(new SupportingFile("client/auth/AuthorizationBinder.mustache", authFolder, "AuthorizationBinder.java"));
            supportingFiles.add(new SupportingFile("client/auth/Authorizations.mustache", authFolder, "Authorizations.java"));
            supportingFiles.add(new SupportingFile("client/auth/AuthorizationFilter.mustache", authFolder, "AuthorizationFilter.java"));
            final String authConfigurationFolder = authFolder + "/configuration";
            supportingFiles.add(new SupportingFile("client/auth/configuration/ApiKeyAuthConfiguration.mustache", authConfigurationFolder, "ApiKeyAuthConfiguration.java"));
            supportingFiles.add(new SupportingFile("client/auth/configuration/ConfigurableAuthorization.mustache", authConfigurationFolder, "ConfigurableAuthorization.java"));
            supportingFiles.add(new SupportingFile("client/auth/configuration/HttpBasicAuthConfiguration.mustache", authConfigurationFolder, "HttpBasicAuthConfiguration.java"));

            if (additionalProperties.containsKey(AUTHORIZATION_FILTER_PATTERN)) {
                String pattern = additionalProperties.get(AUTHORIZATION_FILTER_PATTERN).toString();
                setAuthorizationFilterPattern(pattern);
                additionalProperties.put(AUTHORIZATION_FILTER_PATTERN, authorizationFilterPattern);
            }
        }

        if (additionalProperties.containsKey(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS)) {
            String additionalClientAnnotationsList = additionalProperties.get(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS).toString();
            setAdditionalClientTypeAnnotations(Arrays.asList(additionalClientAnnotationsList.trim().split("\\s*(;|\\r?\\n)\\s*")));
            additionalProperties.put(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, additionalClientTypeAnnotations);
        }

        if (additionalProperties.containsKey(CLIENT_ID)) {
            String id = additionalProperties.get(CLIENT_ID).toString();
            setClientId(id);
            additionalProperties.put(CLIENT_ID, clientId);
        }

        if (additionalProperties.containsKey(BASE_PATH_SEPARATOR)) {
            String separator = additionalProperties.get(BASE_PATH_SEPARATOR).toString();
            setBasePathSeparator(separator);
            additionalProperties.put(BASE_PATH_SEPARATOR, basePathSeparator);
        }

        // Api file
        apiTemplateFiles.clear();
        apiTemplateFiles.put("client/api.mustache", ".java");

        // Add test files
        apiTestTemplateFiles.clear();
        if (testTool.equals(OPT_TEST_JUNIT)) {
            apiTestTemplateFiles.put("client/test/api_test.mustache", ".java");
        } else if (testTool.equals(OPT_TEST_SPOCK)) {
            apiTestTemplateFiles.put("client/test/api_test.groovy.mustache", ".groovy");
        }

        // Add documentation files
        supportingFiles.add(new SupportingFile("client/doc/README.mustache", "", "README.md").doNotOverwrite());
        supportingFiles.add(new SupportingFile("client/doc/auth.mustache", apiDocPath, "auth.md"));
        apiDocTemplateFiles.clear();
        apiDocTemplateFiles.put("client/doc/api_doc.mustache", ".md");
    }

    public void setAdditionalClientTypeAnnotations(final List<String> additionalClientTypeAnnotations) {
        this.additionalClientTypeAnnotations = additionalClientTypeAnnotations;
    }

    public void setAuthorizationFilterPattern(final String pattern) {
        writePropertyBack(OPT_CONFIGURE_AUTH_FILTER_PATTERN, true);
        authorizationFilterPattern = pattern;
    }

    public void setClientId(final String id) {
        writePropertyBack(OPT_CONFIGURE_CLIENT_ID, true);
        clientId = id;
    }

    public void setBasePathSeparator(final String separator) {
        basePathSeparator = separator;
    }

    public void setConfigureAuthorization(boolean configureAuthorization) {
        this.configureAuthorization = configureAuthorization;
    }

    @Override
    public JavaMicronautClientOptionsBuilder optionsBuilder() {
        return new DefaultClientOptionsBuilder();
    }

    static class DefaultClientOptionsBuilder implements JavaMicronautClientOptionsBuilder {
        private List<String> additionalClientTypeAnnotations;
        private String authorizationFilterPattern;
        private String basePathSeparator;
        private String clientId;
        private boolean useAuth;

        @Override
        public JavaMicronautClientOptionsBuilder withAuthorization(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern) {
            this.authorizationFilterPattern = authorizationFilterPattern;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAdditionalClientTypeAnnotations(List<String> additionalClientTypeAnnotations) {
            this.additionalClientTypeAnnotations = additionalClientTypeAnnotations;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withBasePathSeparator(String basePathSeparator) {
            this.basePathSeparator = basePathSeparator;
            return this;
        }

        ClientOptions build() {
            return new ClientOptions(
                additionalClientTypeAnnotations,
                authorizationFilterPattern,
                basePathSeparator,
                clientId,
                useAuth);
        }
    }

    record ClientOptions(
        List<String> additionalClientTypeAnnotations,
        String authorizationFilterPattern,
        String basePathSeparator,
        String clientId,
        boolean useAuth
    ) {
    }
}
