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
import java.util.Collections;
import java.util.List;

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

import static org.openapitools.codegen.CodegenConstants.API_PACKAGE;

/**
 * The generator for creating Micronaut servers.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class KotlinMicronautServerCodegen extends AbstractMicronautKotlinCodegen<KotlinMicronautServerOptionsBuilder> {

    public static final String OPT_CONTROLLER_PACKAGE = "controllerPackage";
    public static final String OPT_GENERATE_CONTROLLER_FROM_EXAMPLES = "generateControllerFromExamples";
    public static final String OPT_GENERATE_IMPLEMENTATION_FILES = "generateImplementationFiles";
    public static final String OPT_GENERATE_OPERATIONS_TO_RETURN_NOT_IMPLEMENTED = "generateOperationsToReturnNotImplemented";
    public static final String OPT_GENERATE_STREAMING_FILE_UPLOAD = "generateStreamingFileUpload";
    public static final String OPT_AOT = "aot";

    public static final String EXTENSION_ROLES = "x-roles";
    public static final String ANONYMOUS_ROLE_KEY = "isAnonymous()";
    public static final String ANONYMOUS_ROLE = "SecurityRule.IS_ANONYMOUS";
    public static final String AUTHORIZED_ROLE_KEY = "isAuthorized()";
    public static final String AUTHORIZED_ROLE = "SecurityRule.IS_AUTHENTICATED";
    public static final String DENY_ALL_ROLE_KEY = "denyAll()";
    public static final String DENY_ALL_ROLE = "SecurityRule.DENY_ALL";

    public static final String NAME = "kotlin-micronaut-server";

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
    protected boolean generateStreamingFileUpload;
    protected boolean aot;

    KotlinMicronautServerCodegen() {

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
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_STREAMING_FILE_UPLOAD, "Whether to generate StreamingFileUpload type for file request body", generateStreamingFileUpload));
        cliOptions.add(CliOption.newBoolean(OPT_AOT, "Generate compatible code with micronaut-aot", aot));

        setApiNamePrefix(API_PREFIX);
        setApiNameSuffix(API_SUFFIX);

        // Set the type mappings
        // It could be also StreamingFileUpload
        typeMapping.put("file", "CompletedFileUpload");
        importMapping.put("CompletedFileUpload", "io.micronaut.http.multipart.CompletedFileUpload");
        importMapping.put("StreamingFileUpload", "io.micronaut.http.multipart.StreamingFileUpload");

        typeMapping.put("responseFile", "FileCustomizableResponseType");
        importMapping.put("FileCustomizableResponseType", "io.micronaut.http.server.types.files.FileCustomizableResponseType");
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
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

        if (additionalProperties.containsKey(OPT_GENERATE_STREAMING_FILE_UPLOAD)) {
            generateStreamingFileUpload = convertPropertyToBoolean(OPT_GENERATE_STREAMING_FILE_UPLOAD);
        }
        writePropertyBack(OPT_GENERATE_STREAMING_FILE_UPLOAD, generateStreamingFileUpload);

        // Api file
        apiTemplateFiles.clear();
        apiTemplateFiles.put("server/controller-interface.mustache", ".kt");

        apiTestTemplateFiles.clear();
        if (generateImplementationFiles) {
            // Add documentation files
            supportingFiles.add(new SupportingFile("server/doc/README.mustache", "", "README.md").doNotOverwrite());
            apiDocTemplateFiles.clear();
            apiDocTemplateFiles.put("server/doc/controller_doc.mustache", ".md");

            // Add Application.kt file
            String invokerFolder = (sourceFolder + '/' + packageName).replace('.', '/');
            supportingFiles.add(new SupportingFile("common/configuration/Application.mustache", invokerFolder, "Application.kt").doNotOverwrite());

            // Controller Implementation is generated as a test file - so that it is not overwritten
            apiTestTemplateFiles.put("server/controller-implementation.mustache", ".kt");

            // Add test files
            if (testTool.equals(OPT_TEST_JUNIT)) {
                apiTestTemplateFiles.put("server/test/controller_test.mustache", ".kt");
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
            return (implementationFolder + File.separator + controllerName + ".kt"
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
            p.example = "null";
        }
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        // Add the controller classname to operations
        OperationMap operations = objs.getOperations();
        String controllerClassname = StringUtils.camelize(CONTROLLER_PREFIX + "_" + operations.getPathPrefix() + "_" + CONTROLLER_SUFFIX);
        objs.put("controllerClassname", controllerClassname);

        List<CodegenOperation> allOperations = (List<CodegenOperation>) operations.get("operation");
        if (useAuth) {
            for (CodegenOperation operation : allOperations) {
                if (!operation.vendorExtensions.containsKey(EXTENSION_ROLES)) {
                    String role = operation.hasAuthMethods ? AUTHORIZED_ROLE : ANONYMOUS_ROLE;
                    operation.vendorExtensions.put(EXTENSION_ROLES, Collections.singletonList(role));
                } else {
                    List<String> roles = (List<String>) operation.vendorExtensions.get(EXTENSION_ROLES);
                    roles = roles.stream().map(role -> switch (role) {
                        case ANONYMOUS_ROLE_KEY -> ANONYMOUS_ROLE;
                        case AUTHORIZED_ROLE_KEY -> AUTHORIZED_ROLE;
                        case DENY_ALL_ROLE_KEY -> DENY_ALL_ROLE;
                        default -> "\"" + escapeText(role) + "\"";
                    }).toList();
                    operation.vendorExtensions.put(EXTENSION_ROLES, roles);
                }
            }
        }

        return objs;
    }

    @Override
    public KotlinMicronautServerOptionsBuilder optionsBuilder() {
        return new DefaultServerOptionsBuilder();
    }

    static class DefaultServerOptionsBuilder implements KotlinMicronautServerOptionsBuilder {

        private String controllerPackage;
        private boolean generateImplementationFiles;
        private boolean generateControllerFromExamples;
        private boolean generateOperationsToReturnNotImplemented = true;
        private boolean plural = true;
        private boolean useAuth = true;
        private boolean fluxForArrays;
        private boolean generatedAnnotation = true;
        private boolean aot;
        private boolean ksp;

        @Override
        public KotlinMicronautServerOptionsBuilder withControllerPackage(String controllerPackage) {
            this.controllerPackage = controllerPackage;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withGenerateImplementationFiles(boolean generateImplementationFiles) {
            this.generateImplementationFiles = generateImplementationFiles;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withGenerateOperationsToReturnNotImplemented(boolean generateOperationsToReturnNotImplemented) {
            this.generateOperationsToReturnNotImplemented = generateOperationsToReturnNotImplemented;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withGenerateControllerFromExamples(boolean generateControllerFromExamples) {
            this.generateControllerFromExamples = generateControllerFromExamples;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withAuthentication(boolean useAuth) {
            this.useAuth = useAuth;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withFluxForArrays(boolean fluxForArrays) {
            this.fluxForArrays = fluxForArrays;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withPlural(boolean plural) {
            this.plural = plural;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withGeneratedAnnotation(boolean generatedAnnotation) {
            this.generatedAnnotation = generatedAnnotation;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withAot(boolean aot) {
            this.aot = aot;
            return this;
        }

        @Override
        public KotlinMicronautServerOptionsBuilder withKsp(boolean ksp) {
            this.ksp = ksp;
            return this;
        }

        ServerOptions build() {
            return new ServerOptions(
                controllerPackage,
                generateImplementationFiles,
                generateOperationsToReturnNotImplemented,
                generateControllerFromExamples,
                useAuth,
                plural,
                fluxForArrays,
                generatedAnnotation,
                aot,
                ksp
            );
        }
    }

    record ServerOptions(
        String controllerPackage,
        boolean generateImplementationFiles,
        boolean generateOperationsToReturnNotImplemented,
        boolean generateControllerFromExamples,
        boolean useAuth,
        boolean plural,
        boolean fluxForArrays,
        boolean generatedAnnotation,
        boolean aot,
        boolean ksp
    ) {
    }
}
