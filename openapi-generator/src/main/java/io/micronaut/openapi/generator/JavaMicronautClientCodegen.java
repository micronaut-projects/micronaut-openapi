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

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.config.GlobalSettings;
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

import static io.micronaut.openapi.generator.Utils.normalizeJavaClass;
import static io.micronaut.openapi.generator.Utils.processMultipartBody;
import static io.micronaut.openapi.generator.Utils.readBooleanProperty;
import static io.micronaut.openapi.generator.Utils.readIntProperty;
import static io.micronaut.openapi.generator.Utils.readListOfStringsProperty;
import static io.micronaut.openapi.generator.Utils.readProperty;

/**
 * The generator for creating Micronaut clients.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class JavaMicronautClientCodegen extends AbstractMicronautJavaCodegen<JavaMicronautClientOptionsBuilder> {

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
    public static final String RETRYABLE_ANNOTATION = "retryableAnnotation";
    public static final String OPT_RETRYABLE = "retryable";
    public static final String OPT_RETRYABLE_INCLUDES = "retryableIncludes";
    public static final String OPT_RETRYABLE_EXCLUDES = "retryableExcludes";
    public static final String OPT_RETRYABLE_ATTEMPTS = "retryableAttempts";
    public static final String OPT_RETRYABLE_DELAY = "retryableDelay";
    public static final String OPT_RETRYABLE_MAX_DELAY = "retryableMaxDelay";
    public static final String OPT_RETRYABLE_MULTIPLIER = "retryableMultiplier";
    public static final String OPT_RETRYABLE_JITTER = "retryableJitter";
    public static final String OPT_RETRYABLE_PREDICATE = "retryablePredicate";
    public static final String OPT_RETRYABLE_CAPTURED_EXCEPTION = "retryableCapturedException";

    public static final String NAME = "java-micronaut-client";

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

    protected boolean retryable;
    protected List<String> retryableIncludes;
    protected List<String> retryableExcludes;
    protected int retryableAttempts;
    protected String retryableDelay;
    protected String retryableMaxDelay;
    protected String retryableMultiplier;
    protected String retryableJitter;
    protected String retryablePredicate;
    protected String retryableCapturedException;

    JavaMicronautClientCodegen() {

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

        cliOptions.add(CliOption.newString(OPT_RETRYABLE, "Add or not @Retryable annotation to client class"));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_INCLUDES, "Set includes parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_EXCLUDES, "Set excludes parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_ATTEMPTS, "Set attempts parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_DELAY, "Set delay parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_MAX_DELAY, "Set maxDelay parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_MULTIPLIER, "Set multiplier parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_JITTER, "Set jitter parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_PREDICATE, "Set predicate parameter for Retryable annotation."));
        cliOptions.add(CliOption.newString(OPT_RETRYABLE_CAPTURED_EXCEPTION, "Set capturedException parameter for Retryable annotation."));

        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "false");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "false");

        final CliOption authorizationFilterPatternStyleOpt = CliOption.newString(AUTHORIZATION_FILTER_PATTERN_STYLE, "Configure the authorization filter pattern style for the client");
        var authorizationFilterPatternStyleOptions = new HashMap<String, String>();
        authorizationFilterPatternStyleOptions.put(AuthFilterPatternStyle.ANT.name(), "Ant-style pattern matching.");
        authorizationFilterPatternStyleOptions.put(AuthFilterPatternStyle.REGEX.name(), "Regex-style pattern matching.");
        authorizationFilterPatternStyleOpt.setEnum(authorizationFilterPatternStyleOptions);
        cliOptions.add(authorizationFilterPatternStyleOpt);

        typeMapping.put("file", "byte[]");
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
        return "Generates a Java Micronaut Client.";
    }

    private void postProcessMultipartParam(CodegenOperation op, List<CodegenParameter> params, Collection<String> removedParams) {
        var pair = processMultipartBody(op, params, false);
        var multipartParam = pair.getLeft();
        if (multipartParam != null) {
            setParameterExampleValue(multipartParam);
        }
        removedParams.addAll(pair.getRight());
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        if (retryable) {
            objs.getImports().add(Map.of("import", "io.micronaut.retry.annotation.Retryable", "classname", "Retryable"));
        }

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

        var enumParams = (List<String>) additionalProperties.get("enumParams");
        if (generateEnumConverters && !enumParams.isEmpty()) {
            var enumConfigName = "Client";
            if (clientId != null) {
                enumConfigName = StringUtils.capitalize(clientId.replace("-", ""));
            }
            var enumConfigClassName = "EnumConverter" + enumConfigName + "Config";
            additionalProperties.put("enumConfigClassName", enumConfigClassName);
            final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
            supportingFiles.add(new SupportingFile("common/EnumConverterConfig.mustache", invokerFolder + "/config", enumConfigClassName + ".java"));
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

        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

        // Retryable annotation
        retryable = readBooleanProperty(OPT_RETRYABLE, additionalProperties, retryable);
        if (retryable) {
            retryableIncludes = readListOfStringsProperty(OPT_RETRYABLE_INCLUDES, additionalProperties, retryableIncludes);
            retryableExcludes = readListOfStringsProperty(OPT_RETRYABLE_EXCLUDES, additionalProperties, retryableExcludes);
            retryableAttempts = readIntProperty(OPT_RETRYABLE_ATTEMPTS, additionalProperties, retryableAttempts);
            retryableDelay = readProperty(OPT_RETRYABLE_DELAY, additionalProperties, retryableDelay);
            retryableMaxDelay = readProperty(OPT_RETRYABLE_MAX_DELAY, additionalProperties, retryableMaxDelay);
            retryableMultiplier = readProperty(OPT_RETRYABLE_MULTIPLIER, additionalProperties, retryableMultiplier);
            retryableJitter = readProperty(OPT_RETRYABLE_JITTER, additionalProperties, retryableJitter);
            retryablePredicate = readProperty(OPT_RETRYABLE_PREDICATE, additionalProperties, retryablePredicate);
            retryableCapturedException = readProperty(OPT_RETRYABLE_CAPTURED_EXCEPTION, additionalProperties, retryableCapturedException);

            writePropertyBack(RETRYABLE_ANNOTATION, calcRetryableAnnotation());
        }

        // Authorization files
        if (configureAuthorization) {

            generateAuthClasses = readBooleanProperty(OPT_GENERATE_AUTH_CLASSES, additionalProperties, generateAuthClasses);
            authFilter = readBooleanProperty(OPT_AUTH_FILTER, additionalProperties, authFilter);
            useOauth = readBooleanProperty(OPT_USE_OAUTH, additionalProperties, useOauth);
            useBasicAuth = readBooleanProperty(OPT_USE_BASIC_AUTH, additionalProperties, useBasicAuth);
            useApiKeyAuth = readBooleanProperty(OPT_USE_API_KEY_AUTH, additionalProperties, useApiKeyAuth);

            if (generateAuthClasses) {
                final String authFolder = invokerFolder + "/auth";
                supportingFiles.add(new SupportingFile("client/auth/Authorization.mustache", authFolder, "Authorization.java"));
                supportingFiles.add(new SupportingFile("client/auth/AuthorizationBinder.mustache", authFolder, "AuthorizationBinder.java"));
                supportingFiles.add(new SupportingFile("client/auth/Authorizations.mustache", authFolder, "Authorizations.java"));
                if (authFilter) {
                    supportingFiles.add(new SupportingFile("client/auth/AuthorizationFilter.mustache", authFolder, "AuthorizationFilter.java"));
                }
                final String authConfigurationFolder = authFolder + "/config";
                if (useApiKeyAuth) {
                    supportingFiles.add(new SupportingFile("client/auth/config/ApiKeyAuthConfig.mustache", authConfigurationFolder, "ApiKeyAuthConfig.java"));
                }
                if (useBasicAuth) {
                    supportingFiles.add(new SupportingFile("client/auth/config/HttpBasicAuthConfig.mustache", authConfigurationFolder, "HttpBasicAuthConfig.java"));
                }
                supportingFiles.add(new SupportingFile("client/auth/config/ConfigurableAuthorization.mustache", authConfigurationFolder, "ConfigurableAuthorization.java"));
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
        apiTemplateFiles.put("client/api.mustache", ".java");

        // Add test files
        apiTestTemplateFiles.clear();
        if (testTool.equals(OPT_TEST_JUNIT)) {
            apiTestTemplateFiles.put("client/test/api_test.mustache", ".java");
        } else if (testTool.equals(OPT_TEST_SPOCK)) {
            apiTestTemplateFiles.put("client/test/api_test.groovy.mustache", ".groovy");
        }
    }

    public String calcRetryableAnnotation() {

        if (!retryable) {
            return null;
        }

        var retryable = new StringBuilder("@Retryable");
        var retryableParams = new StringBuilder();
        var isFirst = true;
        if (retryableIncludes != null && !retryableIncludes.isEmpty()) {
            var normalizedRetryableIncludes = retryableIncludes.stream()
                .map(Utils::normalizeJavaClass)
                .toList();
            retryableParams.append('(');
            retryableParams.append("\n    includes = {").append(String.join(", ", normalizedRetryableIncludes)).append('}');
            isFirst = false;
        }
        if (retryableExcludes != null && !retryableExcludes.isEmpty()) {
            var normalizedRetryableExcludes = retryableExcludes.stream()
                .map(Utils::normalizeJavaClass)
                .toList();
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    excludes = {").append(String.join(", ", normalizedRetryableExcludes)).append('}');
            isFirst = false;
        }
        if (retryableAttempts > 0) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    attempts = \"").append(retryableAttempts).append('"');
            isFirst = false;
        }
        if (retryableDelay != null && !retryableDelay.isBlank()) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    delay = \"").append(retryableDelay).append('"');
            isFirst = false;
        }
        if (retryableMaxDelay != null && !retryableMaxDelay.isBlank()) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    maxDelay = \"").append(retryableMaxDelay).append('"');
            isFirst = false;
        }
        if (retryableMultiplier != null && !retryableMultiplier.isBlank()) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    multiplier = \"").append(retryableMultiplier).append('"');
            isFirst = false;
        }
        if (retryableJitter != null && !retryableJitter.isBlank()) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    jitter = \"").append(retryableJitter).append('"');
            isFirst = false;
        }
        if (retryablePredicate != null && !retryablePredicate.isBlank()) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    predicate = ").append(normalizeJavaClass(retryablePredicate));
            isFirst = false;
        }
        if (retryableCapturedException != null && !retryableCapturedException.isBlank()) {
            retryableParams.append(isFirst ? '(' : ',');
            retryableParams.append("\n    capturedException = ").append(normalizeJavaClass(retryableCapturedException));
        }
        if (!retryableParams.isEmpty()) {
            retryableParams.append("\n)");
            retryable.append(retryableParams);
        }

        return retryable.toString();
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

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public void setRetryableIncludes(List<String> retryableIncludes) {
        this.retryableIncludes = retryableIncludes;
    }

    public void setRetryableExcludes(List<String> retryableExcludes) {
        this.retryableExcludes = retryableExcludes;
    }

    public void setRetryableAttempts(int retryableAttempts) {
        this.retryableAttempts = retryableAttempts;
    }

    public void setRetryableDelay(String retryableDelay) {
        this.retryableDelay = retryableDelay;
    }

    public void setRetryableMaxDelay(String retryableMaxDelay) {
        this.retryableMaxDelay = retryableMaxDelay;
    }

    public void setRetryableMultiplier(String retryableMultiplier) {
        this.retryableMultiplier = retryableMultiplier;
    }

    public void setRetryableJitter(String retryableJitter) {
        this.retryableJitter = retryableJitter;
    }

    public void setRetryablePredicate(String retryablePredicate) {
        this.retryablePredicate = retryablePredicate;
    }

    public void setRetryableCapturedException(String retryableCapturedException) {
        this.retryableCapturedException = retryableCapturedException;
    }

    @Override
    public JavaMicronautClientOptionsBuilder optionsBuilder() {
        return new DefaultClientOptionsBuilder();
    }

    static class DefaultClientOptionsBuilder implements JavaMicronautClientOptionsBuilder {

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
        private boolean lombok;
        private boolean noArgsConstructor;

        private boolean retryable;
        private List<String> retryableIncludes;
        private List<String> retryableExcludes;
        private int retryableAttempts;
        private String retryableDelay;
        private String retryableMaxDelay;
        private String retryableMultiplier;
        private String retryableJitter;
        private String retryablePredicate;
        private String retryableCapturedException;

        @Override
        public JavaMicronautClientOptionsBuilder withAuthorization(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withGenerateAuthClasses(boolean generateAuthClasses) {
            this.generateAuthClasses = generateAuthClasses;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthFilter(boolean authFilter) {
            this.authFilter = authFilter;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withUseOauth(boolean useOauth) {
            this.useOauth = useOauth;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withUseBasicAuth(boolean useBasicAuth) {
            this.useBasicAuth = useBasicAuth;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withUseApiKeyAuth(boolean useApiKeyAuth) {
            this.useApiKeyAuth = useApiKeyAuth;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthorizationFilterPattern(String authorizationFilterPattern) {
            this.authorizationFilterPattern = authorizationFilterPattern;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthorizationFilterPatternStyle(String authorizationFilterPatternStyle) {
            this.authorizationFilterPatternStyle = authorizationFilterPatternStyle;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthFilterClientIds(List<String> authFilterClientIds) {
            this.authFilterClientIds = authFilterClientIds;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthFilterExcludedClientIds(List<String> authFilterExcludedClientIds) {
            this.authFilterExcludedClientIds = authFilterExcludedClientIds;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withAuthConfigName(String authConfigName) {
            this.authConfigName = authConfigName;
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

        @Override
        public JavaMicronautClientOptionsBuilder withFluxForArrays(boolean fluxForArrays) {
            this.fluxForArrays = fluxForArrays;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withPlural(boolean plural) {
            this.plural = plural;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation) {
            this.generatedAnnotation = generatedAnnotation;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withClientPath(boolean clientPath) {
            this.clientPath = clientPath;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withLombok(boolean lombok) {
            this.lombok = lombok;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withNoArgsConstructor(boolean noArgsConstructor) {
            this.noArgsConstructor = noArgsConstructor;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableIncludes(List<String> retryableIncludes) {
            this.retryableIncludes = retryableIncludes;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableExcludes(List<String> retryableExcludes) {
            this.retryableExcludes = retryableExcludes;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableAttempts(int retryableAttempts) {
            this.retryableAttempts = retryableAttempts;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableDelay(String retryableDelay) {
            this.retryableDelay = retryableDelay;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableMaxDelay(String retryableMaxDelay) {
            this.retryableMaxDelay = retryableMaxDelay;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableMultiplier(String retryableMultiplier) {
            this.retryableMultiplier = retryableMultiplier;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableJitter(String retryableJitter) {
            this.retryableJitter = retryableJitter;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryablePredicate(String retryablePredicate) {
            this.retryablePredicate = retryablePredicate;
            return this;
        }

        @Override
        public JavaMicronautClientOptionsBuilder withRetryableCapturedException(String retryableCapturedException) {
            this.retryableCapturedException = retryableCapturedException;
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
                lombok,
                noArgsConstructor,
                retryable,
                retryableIncludes,
                retryableExcludes,
                retryableAttempts,
                retryableDelay,
                retryableMaxDelay,
                retryableMultiplier,
                retryableJitter,
                retryablePredicate,
                retryableCapturedException
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
        boolean lombok,
        boolean noArgsConstructor,
        boolean retryable,
        List<String> retryableIncludes,
        List<String> retryableExcludes,
        int retryableAttempts,
        String retryableDelay,
        String retryableMaxDelay,
        String retryableMultiplier,
        String retryableJitter,
        String retryablePredicate,
        String retryableCapturedException
    ) {
    }
}
