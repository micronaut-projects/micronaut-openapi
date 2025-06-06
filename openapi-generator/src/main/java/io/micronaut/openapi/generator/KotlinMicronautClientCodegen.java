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
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static io.micronaut.openapi.generator.Utils.processMultipartBody;
import static io.micronaut.openapi.generator.Utils.readListOfStringsProperty;

/**
 * The generator for creating Micronaut clients.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class KotlinMicronautClientCodegen extends AbstractMicronautKotlinCodegen<KotlinMicronautClientOptionsBuilder> {

    public static final String OPT_CONFIGURE_AUTH = "configureAuth";
    public static final String OPT_CONFIGURE_AUTH_FILTER_PATTERN = "configureAuthFilterPattern";
    public static final String OPT_CONFIGURE_CLIENT_ID = "configureClientId";
    public static final String OPT_CLIENT_PATH = "clientPath";
    public static final String OPT_USE_OAUTH = "useOauth";
    public static final String OPT_USE_BASIC_AUTH = "useBasicAuth";
    public static final String OPT_USE_API_KEY_AUTH = "useApiKeyAuth";
    public static final String OPT_AUTH_FILTER = "authFilter";
    public static final String OPT_GENERATE_AUTH_CLASSES = "generateAuthClasses";
    public static final String OPT_AUTH_CONFIG_NAME = "authConfigName";
    public static final String OPT_AUTH_FILTER_CLIENT_IDS = "authFilterClientIds";
    public static final String OPT_AUTH_FILTER_EXCLUDED_CLIENT_IDS = "authFilterExcludedClientIds";
    public static final String ADDITIONAL_CLIENT_TYPE_ANNOTATIONS = "additionalClientTypeAnnotations";
    public static final String AUTHORIZATION_FILTER_PATTERN = "authorizationFilterPattern";
    public static final String AUTHORIZATION_FILTER_PATTERN_STYLE = "authorizationFilterPatternStyle";
    public static final String BASE_PATH_SEPARATOR = "basePathSeparator";
    public static final String CLIENT_ID = "clientId";

    public static final String NAME = "kotlin-micronaut-client";

    protected Object additionalClientTypeAnnotations;
    protected Object authorizationFilterPattern;
    protected String authorizationFilterPatternStyle;
    protected String basePathSeparator = ".";
    protected String clientId;
    protected String authConfigName;
    protected Object authFilterClientIds;
    protected Object authFilterExcludedClientIds;
    protected boolean configureAuthorization;
    protected boolean clientPath;
    protected boolean useOauth = true;
    protected boolean useBasicAuth = true;
    protected boolean useApiKeyAuth = true;
    protected boolean authFilter = true;
    protected boolean generateAuthClasses = true;

    KotlinMicronautClientCodegen() {

        title = "OpenAPI Micronaut Client";

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
            .stability(Stability.STABLE)
            .build();
        additionalProperties.put("client", "true");

        cliOptions.add(CliOption.newString(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, "Additional annotations for client type(class level annotations). List separated by semicolon(;) or new line (Linux or Windows)"));
        cliOptions.add(CliOption.newString(AUTHORIZATION_FILTER_PATTERN, "Configure the authorization filter pattern for the client. Generally defined when generating clients from multiple specification files"));
        cliOptions.add(CliOption.newString(BASE_PATH_SEPARATOR, "Configure the separator to use between the application name and base path when referencing the property").defaultValue(basePathSeparator));
        cliOptions.add(CliOption.newString(CLIENT_ID, "Configure the service ID for the Client"));
        cliOptions.add(CliOption.newString(OPT_AUTH_CONFIG_NAME, "Authorization config name. Using in config properties for HttpBasicAuthConfig and ApiKeyAuthConfig"));
        cliOptions.add(CliOption.newString(OPT_AUTH_FILTER_CLIENT_IDS, "Client IDs for AuthorizationFilter. If you set `clientId`, then authFilterClientIds will be set only this clientId"));
        cliOptions.add(CliOption.newString(OPT_AUTH_FILTER_EXCLUDED_CLIENT_IDS, "Excluded client IDs for AuthorizationFilter"));
        cliOptions.add(CliOption.newBoolean(OPT_CONFIGURE_AUTH, "Configure all the authorization methods as specified in the file", configureAuthorization));
        cliOptions.add(CliOption.newBoolean(OPT_CLIENT_PATH, "Generate code with @Client annotation path attribute", clientPath));
        cliOptions.add(CliOption.newBoolean(OPT_USE_OAUTH, "Generate AuthorizationFilter with support OAuth2.0 or not"));
        cliOptions.add(CliOption.newBoolean(OPT_USE_BASIC_AUTH, "Generate HttpBasicAuthConfig class or not"));
        cliOptions.add(CliOption.newBoolean(OPT_USE_API_KEY_AUTH, "Generate ApiKeyAuthConfig config or not"));
        cliOptions.add(CliOption.newBoolean(OPT_AUTH_FILTER, "Generate AuthorizationFilter or not"));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_AUTH_CLASSES, "Generate authorization classes or not"));

        final CliOption authorizationFilterPatternStyleOpt = CliOption.newString(AUTHORIZATION_FILTER_PATTERN_STYLE, "Configure the authorization filter pattern style for the client");
        var authorizationFilterPatternStyleOptions = new HashMap<String, String>();
        authorizationFilterPatternStyleOptions.put(AuthFilterPatternStyle.ANT.name(), "Ant-style pattern matching.");
        authorizationFilterPatternStyleOptions.put(AuthFilterPatternStyle.REGEX.name(), "Regex-style pattern matching.");
        authorizationFilterPatternStyleOpt.setEnum(authorizationFilterPatternStyleOptions);
        cliOptions.add(authorizationFilterPatternStyleOpt);

        typeMapping.put("file", "ByteArray");
        typeMapping.put("responseFile", "ByteBuffer<?>");

        importMapping.put("ByteBuffer<?>", "io.micronaut.core.io.buffer.ByteBuffer");
        importMapping.put("MultipartBody", "io.micronaut.http.client.multipart.MultipartBody");
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
        return "Generates a Kotlin Micronaut Client.";
    }

    private void postProcessMultipartParam(CodegenOperation op, List<CodegenParameter> params, Collection<String> removedParams) {
        var pair = processMultipartBody(op, params, true);
        var multipartParam = pair.getLeft();
        if (multipartParam != null) {
            setParameterExampleValue(multipartParam);
        }
        removedParams.addAll(pair.getRight());
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();

        var removedParams = new HashSet<String>();
        var alreadyAddedMultipartBodyImport = false;

        for (CodegenOperation op : operationList) {
            postProcessMultipartParam(op, op.bodyParams, removedParams);
            postProcessMultipartParam(op, op.allParams, removedParams);

            op.notNullableParams.removeIf(p -> removedParams.contains(p.paramName));
            op.requiredParams.removeIf(p -> removedParams.contains(p.paramName));
            op.optionalParams.removeIf(p -> removedParams.contains(p.paramName));
            op.requiredAndNotNullableParams.removeIf(p -> removedParams.contains(p.paramName));
            if (op.vendorExtensions.containsKey("originalParams")) {
                ((List<CodegenParameter>) op.vendorExtensions.get("originalParams")).removeIf(p -> removedParams.contains(p.paramName));
            }

            if (!removedParams.isEmpty() && !alreadyAddedMultipartBodyImport) {
                objs.getImports().add(Map.of("import", "io.micronaut.http.client.multipart.MultipartBody", "classname", "MultipartBody"));
                alreadyAddedMultipartBodyImport = true;
            }

            var hasMultipleParams = !op.allParams.isEmpty();
            var hasNotBodyParam = hasMultipleParams;

            for (var param : op.allParams) {
                param.vendorExtensions.put("hasNotBodyParam", hasNotBodyParam);
                param.vendorExtensions.put("hasMultipleParams", hasMultipleParams);
            }
        }

        return objs;
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

        final String invokerFolder = (sourceFolder + '/' + packageName).replace(".", "/");

        // Authorization files
        if (configureAuthorization) {

            if (additionalProperties.containsKey(OPT_GENERATE_AUTH_CLASSES)) {
                generateAuthClasses = convertPropertyToBoolean(OPT_GENERATE_AUTH_CLASSES);
            }
            writePropertyBack(OPT_GENERATE_AUTH_CLASSES, generateAuthClasses);

            if (additionalProperties.containsKey(OPT_AUTH_FILTER)) {
                authFilter = convertPropertyToBoolean(OPT_AUTH_FILTER);
            }
            writePropertyBack(OPT_AUTH_FILTER, authFilter);

            if (additionalProperties.containsKey(OPT_USE_OAUTH)) {
                useOauth = convertPropertyToBoolean(OPT_USE_OAUTH);
            }
            writePropertyBack(OPT_USE_OAUTH, useOauth);

            if (additionalProperties.containsKey(OPT_USE_BASIC_AUTH)) {
                useBasicAuth = convertPropertyToBoolean(OPT_USE_BASIC_AUTH);
            }
            writePropertyBack(OPT_USE_BASIC_AUTH, useBasicAuth);

            if (additionalProperties.containsKey(OPT_USE_API_KEY_AUTH)) {
                useApiKeyAuth = convertPropertyToBoolean(OPT_USE_API_KEY_AUTH);
            }
            writePropertyBack(OPT_USE_API_KEY_AUTH, useApiKeyAuth);

            if (generateAuthClasses) {
                final String authFolder = invokerFolder + "/auth";
                supportingFiles.add(new SupportingFile("client/auth/Authorization.mustache", authFolder, "Authorization.kt"));
                supportingFiles.add(new SupportingFile("client/auth/AuthorizationBinder.mustache", authFolder, "AuthorizationBinder.kt"));
                supportingFiles.add(new SupportingFile("client/auth/Authorizations.mustache", authFolder, "Authorizations.kt"));
                if (authFilter) {
                    supportingFiles.add(new SupportingFile("client/auth/AuthorizationFilter.mustache", authFolder, "AuthorizationFilter.kt"));
                }
                final String authConfigurationFolder = authFolder + "/config";
                if (useApiKeyAuth) {
                    supportingFiles.add(new SupportingFile("client/auth/config/ApiKeyAuthConfig.mustache", authConfigurationFolder, "ApiKeyAuthConfig.kt"));
                }
                if (useBasicAuth) {
                    supportingFiles.add(new SupportingFile("client/auth/config/HttpBasicAuthConfig.mustache", authConfigurationFolder, "HttpBasicAuthConfig.kt"));
                }
                supportingFiles.add(new SupportingFile("client/auth/config/ConfigurableAuthorization.mustache", authConfigurationFolder, "ConfigurableAuthorization.kt"));
            }

            if (additionalProperties.containsKey(AUTHORIZATION_FILTER_PATTERN)) {
                authorizationFilterPattern = additionalProperties.get(AUTHORIZATION_FILTER_PATTERN);
            }
            var parsedPatterns = readListOfStringsProperty(authorizationFilterPattern);
            writePropertyBack(AUTHORIZATION_FILTER_PATTERN, parsedPatterns);
            if (!parsedPatterns.isEmpty()) {
                writePropertyBack(OPT_CONFIGURE_AUTH_FILTER_PATTERN, true);
            }

            if (additionalProperties.containsKey(OPT_AUTH_FILTER_CLIENT_IDS)) {
                authFilterClientIds = additionalProperties.get(OPT_AUTH_FILTER_CLIENT_IDS);
            }
            // this case for create filter without any clientID
            if (authFilterClientIds != null
                && (
                authFilterClientIds instanceof String str && str.isEmpty()
                    || authFilterClientIds instanceof List<?> list && list.isEmpty()
            )) {
                authFilterClientIds = Collections.emptyList();
            } else {
                var parsedAuthFilterClientIds = readListOfStringsProperty(authFilterClientIds);
                writePropertyBack(OPT_AUTH_FILTER_CLIENT_IDS, parsedAuthFilterClientIds);
                if (parsedAuthFilterClientIds.isEmpty() && clientId != null) {
                    authFilterClientIds = List.of(clientId);
                    writePropertyBack(OPT_AUTH_FILTER_CLIENT_IDS, authFilterClientIds);
                }
            }

            if (additionalProperties.containsKey(OPT_AUTH_FILTER_EXCLUDED_CLIENT_IDS)) {
                authFilterExcludedClientIds = additionalProperties.get(OPT_AUTH_FILTER_EXCLUDED_CLIENT_IDS);
            }
            writePropertyBack(OPT_AUTH_FILTER_EXCLUDED_CLIENT_IDS, readListOfStringsProperty(authFilterExcludedClientIds));

            if (additionalProperties.containsKey(AUTHORIZATION_FILTER_PATTERN_STYLE)) {
                var additionalPropertiesOpt = (String) additionalProperties.get(AUTHORIZATION_FILTER_PATTERN_STYLE);
                setAuthorizationFilterPatternStyle(additionalPropertiesOpt);
            }
            writePropertyBack(AUTHORIZATION_FILTER_PATTERN_STYLE, authorizationFilterPatternStyle);

            if (AuthFilterPatternStyle.ANT.name().equals(authorizationFilterPatternStyle)) {
                authorizationFilterPatternStyle = null;
                additionalProperties.remove(AUTHORIZATION_FILTER_PATTERN_STYLE);
            }

            if (additionalProperties.containsKey(OPT_AUTH_CONFIG_NAME)) {
                authConfigName = (String) additionalProperties.get(OPT_AUTH_CONFIG_NAME);
            }
            if (authConfigName == null) {
                authConfigName = clientId;
            }
            writePropertyBack(OPT_AUTH_CONFIG_NAME, authConfigName);
        }

        if (additionalProperties.containsKey(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS)) {
            additionalClientTypeAnnotations = additionalProperties.get(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS);
        }
        var parsedAnnotations = readListOfStringsProperty(additionalClientTypeAnnotations);
        writePropertyBack(ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, parsedAnnotations);

        var clientId = additionalProperties.get(CLIENT_ID);
        if (clientId != null) {
            this.clientId = clientId.toString();
        }
        if (this.clientId != null) {
            writePropertyBack(OPT_CONFIGURE_CLIENT_ID, true);
            writePropertyBack(CLIENT_ID, this.clientId);
        }

        if (additionalProperties.containsKey(OPT_CLIENT_PATH)) {
            clientPath = convertPropertyToBoolean(OPT_CLIENT_PATH);
        }
        writePropertyBack(OPT_CLIENT_PATH, clientPath);

        var basePathSeparator = additionalProperties.get(BASE_PATH_SEPARATOR);
        if (basePathSeparator != null) {
            this.basePathSeparator = basePathSeparator.toString();
        }
        writePropertyBack(BASE_PATH_SEPARATOR, this.basePathSeparator);

        // Api file
        apiTemplateFiles.clear();
        apiTemplateFiles.put("client/api.mustache", ".kt");

        // Add test files
        apiTestTemplateFiles.clear();
        if (testTool.equals(OPT_TEST_JUNIT)) {
            apiTestTemplateFiles.put("client/test/api_test.mustache", ".kt");
        }

        // Add documentation files
        supportingFiles.add(new SupportingFile("client/doc/README.mustache", "", "README.md").doNotOverwrite());
        supportingFiles.add(new SupportingFile("client/doc/auth.mustache", apiDocPath, "auth.md"));
        apiDocTemplateFiles.clear();
        apiDocTemplateFiles.put("client/doc/api_doc.mustache", ".md");
    }

    @Override
    public boolean isServer() {
        return false;
    }

    public void setAdditionalClientTypeAnnotations(final Object additionalClientTypeAnnotations) {
        this.additionalClientTypeAnnotations = additionalClientTypeAnnotations;
    }

    public void setAuthorizationFilterPattern(final Object authorizationFilterPattern) {
        this.authorizationFilterPattern = authorizationFilterPattern;
    }

    public void setAuthorizationFilterPatternStyle(String authorizationFilterPatternStyle) {
        if (authorizationFilterPatternStyle == null) {
            this.authorizationFilterPatternStyle = null;
            return;
        }
        try {
            this.authorizationFilterPatternStyle = AuthFilterPatternStyle.valueOf(authorizationFilterPatternStyle.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            var sb = new StringBuilder(authorizationFilterPatternStyle + " is an invalid enum property naming option. Please choose from:");
            for (var availableOpt : AuthFilterPatternStyle.values()) {
                sb.append("\n  ").append(availableOpt.name());
            }
            throw new RuntimeException(sb.toString());
        }
    }

    public void setAuthFilterClientIds(Object authFilterClientIds) {
        this.authFilterClientIds = authFilterClientIds;
    }

    public void setAuthFilterExcludedClientIds(Object authFilterExcludedClientIds) {
        this.authFilterExcludedClientIds = authFilterExcludedClientIds;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public void setAuthConfigName(String authConfigName) {
        this.authConfigName = authConfigName;
    }

    public void setClientPath(boolean clientPath) {
        this.clientPath = clientPath;
    }

    public void setUseOauth(boolean useOauth) {
        this.useOauth = useOauth;
    }

    public void setUseBasicAuth(boolean useBasicAuth) {
        this.useBasicAuth = useBasicAuth;
    }

    public void setUseApiKeyAuth(boolean useApiKeyAuth) {
        this.useApiKeyAuth = useApiKeyAuth;
    }

    public void setAuthFilter(boolean authFilter) {
        this.authFilter = authFilter;
    }

    public void setGenerateAuthClasses(boolean generateAuthClasses) {
        this.generateAuthClasses = generateAuthClasses;
    }

    public void setBasePathSeparator(final String basePathSeparator) {
        this.basePathSeparator = basePathSeparator;
    }

    public void setConfigureAuthorization(boolean configureAuthorization) {
        this.configureAuthorization = configureAuthorization;
    }

    @Override
    public KotlinMicronautClientOptionsBuilder optionsBuilder() {
        return new DefaultClientOptionsBuilder();
    }

    static class DefaultClientOptionsBuilder implements KotlinMicronautClientOptionsBuilder {

        private List<String> additionalClientTypeAnnotations;
        private String authorizationFilterPattern;
        private String authorizationFilterPatternStyle;
        private List<String> authFilterClientIds;
        private List<String> authFilterExcludedClientIds;
        private String basePathSeparator;
        private String clientId;
        private String authConfigName;
        private boolean clientPath;
        private boolean useAuth;
        private boolean generateAuthClasses = true;
        private boolean useOauth = true;
        private boolean useBasicAuth = true;
        private boolean useApiKeyAuth = true;
        private boolean authFilter = true;
        private boolean plural;
        private boolean fluxForArrays;
        private boolean generatedAnnotation = true;
        private boolean ksp;
        private boolean coroutines;
        private boolean jvmOverloads;
        private boolean jvmRecord;
        private boolean javaCompatibility = true;

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthorization(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withGenerateAuthClasses(boolean generateAuthClasses) {
            this.generateAuthClasses = generateAuthClasses;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthFilter(boolean authFilter) {
            this.authFilter = authFilter;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withUseOauth(boolean useOauth) {
            this.useOauth = useOauth;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withUseBasicAuth(boolean useBasicAuth) {
            this.useBasicAuth = useBasicAuth;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withUseApiKeyAuth(boolean useApiKeyAuth) {
            this.useApiKeyAuth = useApiKeyAuth;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern) {
            this.authorizationFilterPattern = authorizationFilterPattern;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthorizationFilterPatternStyle(String authorizationFilterPatternStyle) {
            this.authorizationFilterPatternStyle = authorizationFilterPatternStyle;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthFilterClientIds(List<String> authFilterClientIds) {
            this.authFilterClientIds = authFilterClientIds;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthFilterExcludedClientIds(List<String> authFilterExcludedClientIds) {
            this.authFilterExcludedClientIds = authFilterExcludedClientIds;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAuthConfigName(String authConfigName) {
            this.authConfigName = authConfigName;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withAdditionalClientTypeAnnotations(List<String> additionalClientTypeAnnotations) {
            this.additionalClientTypeAnnotations = additionalClientTypeAnnotations;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withBasePathSeparator(String basePathSeparator) {
            this.basePathSeparator = basePathSeparator;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withFluxForArrays(boolean fluxForArrays) {
            this.fluxForArrays = fluxForArrays;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withPlural(boolean plural) {
            this.plural = plural;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation) {
            this.generatedAnnotation = generatedAnnotation;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withClientPath(boolean clientPath) {
            this.clientPath = clientPath;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withKsp(boolean ksp) {
            this.ksp = ksp;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withCoroutines(boolean coroutines) {
            this.coroutines = coroutines;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withJvmOverloads(boolean jvmOverloads) {
            this.jvmOverloads = jvmOverloads;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withJvmRecord(boolean jvmRecord) {
            this.jvmRecord = jvmRecord;
            return this;
        }

        @Override
        public KotlinMicronautClientOptionsBuilder withJavaCompatibility(boolean javaCompatibility) {
            this.javaCompatibility = javaCompatibility;
            return this;
        }

        ClientOptions build() {
            return new ClientOptions(
                additionalClientTypeAnnotations,
                authorizationFilterPattern,
                authorizationFilterPatternStyle,
                authFilterClientIds,
                authFilterExcludedClientIds,
                basePathSeparator,
                clientId,
                authConfigName,
                clientPath,
                useAuth,
                generateAuthClasses,
                authFilter,
                useOauth,
                useBasicAuth,
                useApiKeyAuth,
                plural,
                fluxForArrays,
                generatedAnnotation,
                ksp,
                coroutines,
                jvmOverloads,
                jvmRecord,
                javaCompatibility
            );
        }
    }

    record ClientOptions(
        List<String> additionalClientTypeAnnotations,
        String authorizationFilterPattern,
        String authorizationFilterPatternStyle,
        List<String> authFilterClientIds,
        List<String> authFilterExcludedClientIds,
        String basePathSeparator,
        String clientId,
        String authConfigName,
        boolean clientPath,
        boolean useAuth,
        boolean generateAuthClasses,
        boolean authFilter,
        boolean useOauth,
        boolean useBasicAuth,
        boolean useApiKeyAuth,
        boolean plural,
        boolean fluxForArrays,
        boolean generatedAnnotation,
        boolean ksp,
        boolean coroutines,
        boolean jvmOverloads,
        boolean jvmRecord,
        boolean javaCompatibility
    ) {
    }
}
