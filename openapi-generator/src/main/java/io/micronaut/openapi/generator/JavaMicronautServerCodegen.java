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
import org.openapitools.codegen.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.openapi.generator.Utils.addUserParameter;
import static org.openapitools.codegen.CodegenConstants.API_PACKAGE;

/**
 * The generator for creating Micronaut servers.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class JavaMicronautServerCodegen extends AbstractMicronautJavaCodegen<JavaMicronautServerOptionsBuilder> {

    public static final String OPT_CONTROLLER_PACKAGE = "controllerPackage";
    public static final String OPT_GENERATE_CONTROLLER_FROM_EXAMPLES = "generateControllerFromExamples";
    public static final String OPT_GENERATE_IMPLEMENTATION_FILES = "generateImplementationFiles";
    public static final String OPT_GENERATE_OPERATIONS_TO_RETURN_NOT_IMPLEMENTED = "generateOperationsToReturnNotImplemented";
    public static final String OPT_GENERATE_HARD_NULLABLE = "generateHardNullable";
    public static final String OPT_GENERATE_STREAMING_FILE_UPLOAD = "generateStreamingFileUpload";
    public static final String OPT_AOT = "aot";
    public static final String OPT_USER_PARAMETER_MODE = "userParameterMode";
    public static final String OPT_USER_PARAMETER_CLASS = "userParameterClass";

    public static final String EXTENSION_ROLES = "x-roles";
    public static final String ANONYMOUS_ROLE_KEY = "isAnonymous()";
    public static final String ANONYMOUS_ROLE = "SecurityRule.IS_ANONYMOUS";
    public static final String AUTHORIZED_ROLE_KEY = "isAuthorized()";
    public static final String AUTHORIZED_ROLE = "SecurityRule.IS_AUTHENTICATED";
    public static final String DENY_ALL_ROLE_KEY = "denyAll()";
    public static final String DENY_ALL_ROLE = "SecurityRule.DENY_ALL";

    public static final String NAME = "java-micronaut-server";

    protected static final String CONTROLLER_PREFIX = "";
    protected static final String CONTROLLER_SUFFIX = "Controller";
    protected static final String API_PREFIX = "";
    protected static final String API_SUFFIX = "Api";

    protected String apiPackage = "org.openapitools.api";
    protected String controllerPackage = "org.openapitools.controller";
    protected boolean generateImplementationFiles = true;
    protected boolean generateOperationsToReturnNotImplemented = true;
    protected boolean generateControllerFromExamples;
    protected boolean useAuth = true;
    protected boolean generateHardNullable = true;
    protected boolean generateStreamingFileUpload;
    protected boolean aot;
    protected UserParameterMode userParameterMode = UserParameterMode.NONE;
    protected String userParameterClass;

    JavaMicronautServerCodegen() {

        title = "OpenAPI Micronaut Server";
        apiDocPath = "docs/controllers";

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
            .stability(Stability.STABLE)
            .build();
        additionalProperties.put("server", "true");

        cliOptions.add(new CliOption(OPT_CONTROLLER_PACKAGE, "The package in which api implementations (controllers) will be generated.").defaultValue(apiPackage));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_CONTROLLER_FROM_EXAMPLES,
            "Generate the implementation of controller and tests from parameter and return examples that will verify that the api works as desired (for testing).",
            generateControllerFromExamples));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_IMPLEMENTATION_FILES,
            "Whether to generate controller implementations that need to be filled in.",
            generateImplementationFiles));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_OPERATIONS_TO_RETURN_NOT_IMPLEMENTED,
            "Return HTTP 501 Not Implemented instead of an empty response in the generated controller methods.",
            generateOperationsToReturnNotImplemented));

        cliOptions.add(CliOption.newBoolean(OPT_USE_AUTH, "Whether to import authorization and to annotate controller methods accordingly", useAuth));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HARD_NULLABLE, "Whether to generate and use an inherited nullable annotation", generateHardNullable));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_STREAMING_FILE_UPLOAD, "Whether to generate StreamingFileUpload type for file request body", generateStreamingFileUpload));
        cliOptions.add(CliOption.newBoolean(OPT_AOT, "Generate compatible code with micronaut-aot", aot));

        var userModeOption = new CliOption(OPT_USER_PARAMETER_MODE, "Add or not principal / authentication parameter to controller methods").defaultValue(userParameterMode.name());
        var userModeOptionsMap = new HashMap<String, String>();
        userModeOptionsMap.put(UserParameterMode.NONE.name(), "Don't add principal / authentication parameter to controller methods");
        userModeOptionsMap.put(UserParameterMode.PRINCIPAL.name(), "Add java.security.Principal principal parameter to each controller method");
        userModeOptionsMap.put(UserParameterMode.AUTHENTICATION.name(), "Add io.micronaut.security.authentication.Authentication authentication parameter to each controller method");
        userModeOptionsMap.put(UserParameterMode.CUSTOM.name(), "Add a user-defined authentication parameter to each controller method");
        userModeOption.setEnum(userModeOptionsMap);
        cliOptions.add(userModeOption);

        cliOptions.add(CliOption.newString(OPT_USER_PARAMETER_CLASS, "User-defined authentication parameter class name"));

        setApiNamePrefix(API_PREFIX);
        setApiNameSuffix(API_SUFFIX);

        // Set the type mappings
        // It could be also StreamingFileUpload
        typeMapping.put("file", "CompletedFileUpload");
        importMapping.put("CompletedFileUpload", "io.micronaut.http.multipart.CompletedFileUpload");
        importMapping.put("StreamingFileUpload", "io.micronaut.http.multipart.StreamingFileUpload");

        typeMapping.put("responseFile", "FileCustomizableResponseType");
        importMapping.put("FileCustomizableResponseType", "io.micronaut.http.server.types.files.FileCustomizableResponseType");

        importMapping.put(UserParameterMode.PRINCIPAL.getClassName(), UserParameterMode.PRINCIPAL.getClassFullName());
        importMapping.put(UserParameterMode.AUTHENTICATION.getClassName(), UserParameterMode.AUTHENTICATION.getClassFullName());

        // Add all the supporting files
        String resourceFolder = projectFolder + "/resources";
        supportingFiles.add(new SupportingFile("common/configuration/application.yml.mustache", resourceFolder, "application.yml").doNotOverwrite());
        supportingFiles.add(new SupportingFile("common/configuration/logback.xml.mustache", resourceFolder, "logback.xml").doNotOverwrite());
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // Get all the properties that require to know if user specified them directly
        if (additionalProperties.containsKey(OPT_GENERATE_IMPLEMENTATION_FILES)) {
            generateImplementationFiles = convertPropertyToBoolean(OPT_GENERATE_IMPLEMENTATION_FILES);
        }
        writePropertyBack(OPT_GENERATE_IMPLEMENTATION_FILES, generateImplementationFiles);

        if (additionalProperties.containsKey(OPT_GENERATE_OPERATIONS_TO_RETURN_NOT_IMPLEMENTED)) {
            generateOperationsToReturnNotImplemented = convertPropertyToBoolean(OPT_GENERATE_OPERATIONS_TO_RETURN_NOT_IMPLEMENTED);
        }
        writePropertyBack(OPT_GENERATE_OPERATIONS_TO_RETURN_NOT_IMPLEMENTED, generateOperationsToReturnNotImplemented);

        if (additionalProperties.containsKey(API_PACKAGE)) {
            apiPackage = (String) additionalProperties.get(API_PACKAGE);
        }
        writePropertyBack(API_PACKAGE, apiPackage);

        if (additionalProperties.containsKey(OPT_CONTROLLER_PACKAGE)) {
            controllerPackage = (String) additionalProperties.get(OPT_CONTROLLER_PACKAGE);
        }
        writePropertyBack(OPT_CONTROLLER_PACKAGE, controllerPackage);

        // Get all the other properties after superclass processed everything
        if (additionalProperties.containsKey(OPT_GENERATE_CONTROLLER_FROM_EXAMPLES)) {
            generateControllerFromExamples = convertPropertyToBoolean(OPT_GENERATE_CONTROLLER_FROM_EXAMPLES);
        }
        writePropertyBack(OPT_GENERATE_CONTROLLER_FROM_EXAMPLES, generateControllerFromExamples);

        if (additionalProperties.containsKey(OPT_USE_AUTH)) {
            useAuth = convertPropertyToBoolean(OPT_USE_AUTH);
        }
        writePropertyBack(OPT_USE_AUTH, useAuth);

        if (additionalProperties.containsKey(OPT_AOT)) {
            aot = convertPropertyToBoolean(OPT_AOT);
        }
        writePropertyBack(OPT_AOT, aot);

        if (additionalProperties.containsKey(OPT_GENERATE_HARD_NULLABLE)) {
            generateHardNullable = convertPropertyToBoolean(OPT_GENERATE_HARD_NULLABLE);
        }
        writePropertyBack(OPT_GENERATE_HARD_NULLABLE, generateHardNullable);

        if (additionalProperties.containsKey(OPT_GENERATE_STREAMING_FILE_UPLOAD)) {
            generateStreamingFileUpload = convertPropertyToBoolean(OPT_GENERATE_STREAMING_FILE_UPLOAD);
        }
        writePropertyBack(OPT_GENERATE_STREAMING_FILE_UPLOAD, generateStreamingFileUpload);

        if (additionalProperties.containsKey(OPT_USER_PARAMETER_MODE)) {
            var userModeOpt = (String) additionalProperties.get(OPT_USER_PARAMETER_MODE);
            setUserParameterMode(userModeOpt);

            if (userModeOpt.toUpperCase().equals(UserParameterMode.CUSTOM.name())) {
                if (!additionalProperties.containsKey(OPT_USER_PARAMETER_CLASS)) {
                    throw InvalidUserParameterException.javaNoClass();
                }

                userParameterClass = (String) additionalProperties.get(OPT_USER_PARAMETER_CLASS);
                var className = userParameterClass.substring(userParameterClass.lastIndexOf('.') + 1);
                importMapping.put(className, userParameterClass);
            } else if (additionalProperties.containsKey(OPT_USER_PARAMETER_CLASS)) {
                throw InvalidUserParameterException.javaModeNotCustomButClassSet();
            }
        }
        writePropertyBack(OPT_USER_PARAMETER_MODE, userParameterMode.name());
        writePropertyBack(OPT_USER_PARAMETER_CLASS, userParameterClass);

        // Api file
        apiTemplateFiles.clear();
        apiTemplateFiles.put("server/controller-interface.mustache", ".java");

        apiTestTemplateFiles.clear();
        if (generateImplementationFiles) {
            // Add documentation files
            supportingFiles.add(new SupportingFile("server/doc/README.mustache", "", "README.md").doNotOverwrite());
            apiDocTemplateFiles.clear();
            apiDocTemplateFiles.put("server/doc/controller_doc.mustache", ".md");

            // Add Application.java file
            String invokerFolder = (sourceFolder + '/' + invokerPackage).replace('.', '/');
            supportingFiles.add(new SupportingFile("common/configuration/Application.mustache", invokerFolder, "Application.java").doNotOverwrite());

            // Controller Implementation is generated as a test file - so that it is not overwritten
            apiTestTemplateFiles.put("server/controller-implementation.mustache", ".java");

            // Add test files
            if (testTool.equals(OPT_TEST_JUNIT)) {
                apiTestTemplateFiles.put("server/test/controller_test.mustache", ".java");
            } else if (testTool.equals(OPT_TEST_SPOCK)) {
                apiTestTemplateFiles.put("server/test/controller_test.groovy.mustache", ".groovy");
            }
        }

        if (generateStreamingFileUpload) {
            typeMapping.put("file", "StreamingFileUpload");
        }
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public String apiTestFileFolder() {
        // Set it to the whole output dir, so that validation always passes
        return super.getOutputDir();
    }

    @Override
    public String apiTestFilename(String templateName, String tag) {
        String controllerName = StringUtils.camelize(CONTROLLER_PREFIX + "_" + tag + "_" + CONTROLLER_SUFFIX);

        // For controller implementation
        if (generateImplementationFiles && templateName.contains("controller-implementation")) {
            String implementationFolder = outputFolder + File.separator +
                sourceFolder + File.separator +
                controllerPackage.replace('.', File.separatorChar);
            return (implementationFolder + File.separator + controllerName + ".java"
            ).replace('/', File.separatorChar);
        }

        // For api tests
        String suffix = apiTestTemplateFiles().get(templateName);
        return super.apiTestFileFolder() + File.separator + toApiTestFilename(tag) + suffix;
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        super.setParameterExampleValue(p);

        if (p.isFile) {
            // The CompletedFileUpload cannot be initialized
            p.example = null;
        }
    }

    @Override
    public boolean isGenerateHardNullable() {
        return generateHardNullable;
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {

        // Add the controller classname to operations
        OperationMap operations = objs.getOperations();
        String controllerClassname = StringUtils.camelize(CONTROLLER_PREFIX + "_" + operations.getPathPrefix() + "_" + CONTROLLER_SUFFIX);
        objs.put("controllerClassname", controllerClassname);
        var allOperations = (List<CodegenOperation>) operations.get("operation");

        if (useAuth) {

            var alreadyAddedPrincipalImport = false;
            final String importClassFullName;
            final String importClassName;

            if (userParameterMode.equals(UserParameterMode.CUSTOM)) {
                importClassFullName = userParameterClass;
                importClassName = userParameterClass.substring(userParameterClass.lastIndexOf('.') + 1);
            } else {
                importClassFullName = userParameterMode.getClassFullName();
                importClassName = userParameterMode.getClassName();
            }

            for (CodegenOperation operation : allOperations) {

                List<String> roles = new ArrayList<>();

                if (!operation.vendorExtensions.containsKey(EXTENSION_ROLES)) {
                    var authMethods = operation.authMethods;
                    if (authMethods != null && !authMethods.isEmpty()) {
                        var scopes = authMethods.get(0).scopes;
                        if (scopes != null && !scopes.isEmpty()) {
                            for (var scope : scopes) {
                                roles.add("\"" + escapeText(scope.get("scope").toString()) + "\"");
                            }
                        } else {
                            roles.add(AUTHORIZED_ROLE);
                        }
                    } else {
                        roles.add(ANONYMOUS_ROLE);
                    }
                } else {
                    roles = (List<String>) operation.vendorExtensions.get(EXTENSION_ROLES);
                    roles = roles.stream()
                        .map(role -> switch (role) {
                            case ANONYMOUS_ROLE_KEY -> ANONYMOUS_ROLE;
                            case AUTHORIZED_ROLE_KEY -> AUTHORIZED_ROLE;
                            case DENY_ALL_ROLE_KEY -> DENY_ALL_ROLE;
                            default -> "\"" + escapeText(role) + "\"";
                        }).toList();
                }

                operation.vendorExtensions.put(EXTENSION_ROLES, roles);

                if (userParameterMode != UserParameterMode.NONE) {
                    var isAnonymous = roles.contains(ANONYMOUS_ROLE);
                    var isDenyAll = roles.contains(DENY_ALL_ROLE);
                    addUserParameter(operation, userParameterMode, userParameterClass, isAnonymous, isDenyAll);

                    if (importClassFullName != null && !alreadyAddedPrincipalImport) {
                        objs.getImports().add(Map.of("import", importClassFullName, "classname", importClassName));
                        alreadyAddedPrincipalImport = true;
                    }
                }
            }
        }

        objs = super.postProcessOperationsWithModels(objs, allModels);

        var enumParams = (List<String>) additionalProperties.get("enumParams");
        if (generateEnumConverters && !enumParams.isEmpty()) {
            var enumConfigName = "Server";
            var enumConfigClassName = "EnumConverter" + enumConfigName + "Config";
            additionalProperties.put("enumConfigClassName", enumConfigClassName);
            final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
            supportingFiles.add(new SupportingFile("common/EnumConverterConfig.mustache", invokerFolder + "/config", enumConfigClassName + ".java"));
        }

        return objs;
    }

    @Override
    public JavaMicronautServerOptionsBuilder optionsBuilder() {
        return new DefaultServerOptionsBuilder();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getHelp() {
        return "Generates a Java Micronaut Server.";
    }

    public void setControllerPackage(String controllerPackage) {
        this.controllerPackage = controllerPackage;
    }

    public void setGenerateImplementationFiles(boolean generateImplementationFiles) {
        this.generateImplementationFiles = generateImplementationFiles;
    }

    public void setGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented) {
        this.generateOperationsToReturnNotImplemented = generateOperationsToReturnNotImplemented;
    }

    public void setGenerateControllerFromExamples(boolean generateControllerFromExamples) {
        this.generateControllerFromExamples = generateControllerFromExamples;
    }

    public void setUseAuth(boolean useAuth) {
        this.useAuth = useAuth;
    }

    public void setAot(boolean aot) {
        this.aot = aot;
    }

    public void setGenerateStreamingFileUpload(boolean generateStreamingFileUpload) {
        this.generateStreamingFileUpload = generateStreamingFileUpload;
    }

    public void setGenerateHardNullable(boolean generateHardNullable) {
        this.generateHardNullable = generateHardNullable;
    }

    public void setUserParameterMode(String userParameterMode) {
        if (userParameterMode == null) {
            this.userParameterMode = UserParameterMode.NONE;
            return;
        }
        try {
            this.userParameterMode = UserParameterMode.valueOf(userParameterMode.toUpperCase());
        } catch (IllegalArgumentException ex) {
            var sb = new StringBuilder(userParameterMode + " is an invalid enum property naming option. Please choose from:");
            for (var availableOpt : UserParameterMode.values()) {
                sb.append("\n  ").append(availableOpt.name());
            }
            throw new RuntimeException(sb.toString());
        }
    }

    public void setUserParameterClass(String userParameterClass) {
        this.userParameterClass = userParameterClass;
    }

    static class DefaultServerOptionsBuilder implements JavaMicronautServerOptionsBuilder {

        private String controllerPackage;
        private boolean generateImplementationFiles;
        private boolean generateControllerFromExamples;
        private boolean generateOperationsToReturnNotImplemented = true;
        private boolean useAuth = true;
        private String userParameterMode = UserParameterMode.NONE.name();
        private String userParameterClass;
        private boolean lombok;
        private boolean plural = true;
        private boolean fluxForArrays;
        private boolean generatedAnnotation = true;
        private boolean generateHardNullable = true;
        private boolean generateStreamingFileUpload;
        private boolean aot;
        private boolean noArgsConstructor;

        @Override
        public JavaMicronautServerOptionsBuilder withNoArgsConstructor(boolean noArgsConstructor) {
            this.noArgsConstructor = noArgsConstructor;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withControllerPackage(String controllerPackage) {
            this.controllerPackage = controllerPackage;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withGenerateImplementationFiles(boolean generateImplementationFiles) {
            this.generateImplementationFiles = generateImplementationFiles;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented) {
            this.generateOperationsToReturnNotImplemented = generateOperationsToReturnNotImplemented;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withGenerateControllerFromExamples(boolean generateControllerFromExamples) {
            this.generateControllerFromExamples = generateControllerFromExamples;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withAuthentication(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withUserParameterMode(String userParameterMode) {
            this.userParameterMode = userParameterMode;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withUserParameterClass(String userParameterClass) {
            this.userParameterClass = userParameterClass;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withLombok(boolean lombok) {
            this.lombok = lombok;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withPlural(boolean plural) {
            this.plural = plural;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withFluxForArrays(boolean fluxForArrays) {
            this.fluxForArrays = fluxForArrays;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation) {
            this.generatedAnnotation = generatedAnnotation;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withGenerateHardNullable(boolean generateHardNullable) {
            this.generateHardNullable = generateHardNullable;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withGenerateStreamingFileUpload(boolean generateStreamingFileUpload) {
            this.generateStreamingFileUpload = generateStreamingFileUpload;
            return this;
        }

        @Override
        public JavaMicronautServerOptionsBuilder withAot(boolean aot) {
            this.aot = aot;
            return this;
        }

        ServerOptions build() {
            return new ServerOptions(
                controllerPackage,
                generateImplementationFiles,
                generateOperationsToReturnNotImplemented,
                generateControllerFromExamples,
                useAuth,
                userParameterMode,
                userParameterClass,
                lombok,
                plural,
                fluxForArrays,
                generatedAnnotation,
                generateHardNullable,
                generateStreamingFileUpload,
                aot,
                noArgsConstructor
            );
        }
    }

    record ServerOptions(
        String controllerPackage,
        boolean generateImplementationFiles,
        boolean generateOperationsToReturnNotImplemented,
        boolean generateControllerFromExamples,
        boolean useAuth,
        String userParameterMode,
        String userParameterClass,
        boolean lombok,
        boolean plural,
        boolean fluxForArrays,
        boolean generatedAnnotation,
        boolean generateHardNullable,
        boolean generateStreamingFileUpload,
        boolean aot,
        boolean noArgsConstructor
    ) {
    }
}
