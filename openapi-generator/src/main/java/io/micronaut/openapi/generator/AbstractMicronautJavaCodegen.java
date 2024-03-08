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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import io.micronaut.openapi.generator.Formatting.ReplaceDotsWithUnderscoreLambda;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;

import static io.micronaut.openapi.generator.Utils.DEFAULT_BODY_PARAM_NAME;
import static io.micronaut.openapi.generator.Utils.addStrValueToEnum;
import static io.micronaut.openapi.generator.Utils.processGenericAnnotations;
import static org.openapitools.codegen.CodegenConstants.INVOKER_PACKAGE;

/**
 * Base generator for Micronaut.
 *
 * @param <T> The generator options builder.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public abstract class AbstractMicronautJavaCodegen<T extends GeneratorOptionsBuilder> extends AbstractJavaCodegen implements BeanValidationFeatures, OptionalFeatures, MicronautCodeGenerator<T> {

    public static final String OPT_TITLE = "title";
    public static final String OPT_TEST = "test";
    public static final String OPT_TEST_JUNIT = "junit";
    public static final String OPT_TEST_SPOCK = "spock";
    public static final String OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR = "requiredPropertiesInConstructor";
    public static final String OPT_USE_AUTH = "useAuth";
    public static final String OPT_USE_LOMBOK = "lombok";
    public static final String OPT_USE_PLURAL = "plural";
    public static final String OPT_FLUX_FOR_ARRAYS = "fluxForArrays";
    public static final String OPT_GENERATED_ANNOTATION = "generatedAnnotation";
    public static final String OPT_VISITABLE = "visitable";
    public static final String OPT_DATE_LIBRARY_ZONED_DATETIME = "ZONED_DATETIME";
    public static final String OPT_DATE_LIBRARY_OFFSET_DATETIME = "OFFSET_DATETIME";
    public static final String OPT_DATE_LIBRARY_LOCAL_DATETIME = "LOCAL_DATETIME";
    public static final String OPT_DATE_FORMAT = "dateFormat";
    public static final String OPT_DATETIME_FORMAT = "datetimeFormat";
    public static final String OPT_REACTIVE = "reactive";
    public static final String OPT_GENERATE_HTTP_RESPONSE_ALWAYS = "generateHttpResponseAlways";
    public static final String OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED = "generateHttpResponseWhereRequired";
    public static final String OPT_APPLICATION_NAME = "applicationName";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS = "generateSwaggerAnnotations";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1 = "swagger1";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2 = "swagger2";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE = "true";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE = "false";
    public static final String OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG = "generateOperationOnlyForFirstTag";
    public static final String CONTENT_TYPE_APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String CONTENT_TYPE_ANY = "*/*";

    private static final String MONO_CLASS_NAME = "reactor.core.publisher.Mono";
    private static final String FLUX_CLASS_NAME = "reactor.core.publisher.Flux";

    protected String title;
    protected boolean useBeanValidation;
    protected boolean useOptional;
    protected boolean visitable;
    protected boolean lombok;
    protected boolean fluxForArrays;
    protected boolean plural = true;
    protected boolean generatedAnnotation = true;
    protected String testTool;
    protected boolean requiredPropertiesInConstructor = true;
    protected boolean reactive;
    protected boolean generateHttpResponseAlways;
    protected boolean generateHttpResponseWhereRequired = true;
    protected String appName;
    protected String generateSwaggerAnnotations;
    protected boolean generateOperationOnlyForFirstTag;
    protected String serializationLibrary = SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name();
    protected List<ParameterMapping> parameterMappings = new ArrayList<>();
    protected List<ResponseBodyMapping> responseBodyMappings = new ArrayList<>();
    protected Map<String, CodegenModel> allModels = new HashMap<>();

    protected AbstractMicronautJavaCodegen() {

        // CHECKSTYLE:OFF
        // Set all the fields
        useBeanValidation = true;
        useJakartaEe = true;
        useOptional = false;
        visitable = false;
        testTool = OPT_TEST_JUNIT;
        outputFolder = this instanceof JavaMicronautClientCodegen ?
                "generated-code/java-micronaut-client" : "generated-code/java-micronaut";
        apiPackage = "org.openapitools.api";
        modelPackage = "org.openapitools.model";
        invokerPackage = "org.openapitools";
        artifactId = this instanceof JavaMicronautClientCodegen ?
                "openapi-micronaut-client" : "openapi-micronaut";
        embeddedTemplateDir = templateDir = "templates/java-micronaut";
        apiDocPath = "docs/apis";
        modelDocPath = "docs/models";
        dateLibrary = OPT_DATE_LIBRARY_ZONED_DATETIME;
        reactive = true;
        appName = artifactId;
        generateSwaggerAnnotations = this instanceof JavaMicronautClientCodegen ? OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE : OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2;
        generateOperationOnlyForFirstTag = this instanceof JavaMicronautServerCodegen;
        openApiNullable = false;
        inlineSchemaOption.put("RESOLVE_INLINE_ENUMS", "true");
        // CHECKSTYLE:ON

        // Set implemented features for user information
        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(
                        DocumentationFeature.Readme
                )
                .securityFeatures(EnumSet.of(
                        SecurityFeature.ApiKey,
                        SecurityFeature.BearerToken,
                        SecurityFeature.BasicAuth,
                        SecurityFeature.OAuth2_Implicit,
                        SecurityFeature.OAuth2_AuthorizationCode,
                        SecurityFeature.OAuth2_ClientCredentials,
                        SecurityFeature.OAuth2_Password,
                        SecurityFeature.OpenIDConnect
                ))
        );

        // Set additional properties
        additionalProperties.put("openbrace", "{");
        additionalProperties.put("closebrace", "}");

        // Set client options that will be presented to user
        updateOption(INVOKER_PACKAGE, getInvokerPackage());
        updateOption(CodegenConstants.ARTIFACT_ID, getArtifactId());
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);

        cliOptions.add(new CliOption(OPT_TITLE, "Client service name").defaultValue(title));
        cliOptions.add(new CliOption(OPT_APPLICATION_NAME, "Micronaut application name (Defaults to the " + CodegenConstants.ARTIFACT_ID + " value)").defaultValue(appName));
        cliOptions.add(CliOption.newBoolean(OPT_USE_LOMBOK, "Whether or not to use lombok annotations in generated code", lombok));
        cliOptions.add(CliOption.newBoolean(OPT_USE_PLURAL, "Whether or not to use plural for request body parameter name", plural));
        cliOptions.add(CliOption.newBoolean(OPT_FLUX_FOR_ARRAYS, "Whether or not to use Flux<?> instead Mono<List<?>> for arrays in generated code", fluxForArrays));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATED_ANNOTATION, "Generate code with \"@Generated\" annotation", generatedAnnotation));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations", useBeanValidation));
        cliOptions.add(CliOption.newBoolean(USE_OPTIONAL, "Use Optional container for optional parameters", useOptional));
        cliOptions.add(CliOption.newBoolean(OPT_VISITABLE, "Generate visitor for subtypes with a discriminator", visitable));
        cliOptions.add(CliOption.newBoolean(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "Allow only to create models with all the required properties provided in constructor", requiredPropertiesInConstructor));
        cliOptions.add(CliOption.newBoolean(OPT_REACTIVE, "Make the responses use Reactor Mono as wrapper", reactive));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "Always wrap the operations response in HttpResponse object", generateHttpResponseAlways));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, "Wrap the operations response in HttpResponse object where non-200 HTTP status codes or additional headers are defined", generateHttpResponseWhereRequired));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "When false, the operation method will be duplicated in each of the tags if multiple tags are assigned to this operation. " +
                "If true, each operation will be generated only once in the first assigned tag.", generateOperationOnlyForFirstTag));
        CliOption testToolOption = new CliOption(OPT_TEST, "Specify which test tool to generate files for").defaultValue(testTool);
        Map<String, String> testToolOptionMap = new HashMap<>();
        testToolOptionMap.put(OPT_TEST_JUNIT, "Use JUnit as test tool");
        testToolOptionMap.put(OPT_TEST_SPOCK, "Use Spock as test tool");
        testToolOption.setEnum(testToolOptionMap);
        cliOptions.add(testToolOption);

        CliOption generateSwaggerAnnotationsOption = new CliOption(OPT_GENERATE_SWAGGER_ANNOTATIONS, "Specify if you want to generate swagger annotations and which version").defaultValue(generateSwaggerAnnotations);
        Map<String, String> generateSwaggerAnnotationsOptionMap = new HashMap<>();
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1, "Use io.swagger:swagger-annotations for annotating operations and schemas");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2, "Use io.swagger.core.v3:swagger-annotations for annotating operations and schemas");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE, "Equivalent to \"" + OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2 + "\"");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE, "Do not generate swagger annotations");
        generateSwaggerAnnotationsOption.setEnum(generateSwaggerAnnotationsOptionMap);
        cliOptions.add(generateSwaggerAnnotationsOption);

        cliOptions.add(new CliOption(OPT_DATE_FORMAT, "Specify the format pattern of date as a string"));
        cliOptions.add(new CliOption(OPT_DATETIME_FORMAT, "Specify the format pattern of date-time as a string"));

        // Modify the DATE_LIBRARY option to only have supported values
        cliOptions.stream()
                .filter(o -> o.getOpt().equals(DATE_LIBRARY))
                .findFirst()
                .ifPresent(opt -> {
                    Map<String, String> valuesEnum = new HashMap<>();
                    valuesEnum.put(OPT_DATE_LIBRARY_OFFSET_DATETIME, opt.getEnum().get(OPT_DATE_LIBRARY_OFFSET_DATETIME));
                    valuesEnum.put(OPT_DATE_LIBRARY_LOCAL_DATETIME, opt.getEnum().get(OPT_DATE_LIBRARY_LOCAL_DATETIME));
                    opt.setEnum(valuesEnum);
                });

        final CliOption serializationLibraryOpt = CliOption.newString(CodegenConstants.SERIALIZATION_LIBRARY, "Serialization library for model");
        serializationLibraryOpt.defaultValue(SerializationLibraryKind.JACKSON.name());
        Map<String, String> serializationLibraryOptions = new HashMap<>();
        serializationLibraryOptions.put(SerializationLibraryKind.JACKSON.name(), "Jackson as serialization library");
        serializationLibraryOptions.put(SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name(), "Use micronaut-serialization with Jackson annotations");
        serializationLibraryOpt.setEnum(serializationLibraryOptions);
        cliOptions.add(serializationLibraryOpt);

        // Add reserved words
        var micronautReservedWords = List.of(
            // special words
            "Object", "List", "File", "OffsetDateTime", "LocalDate", "LocalTime",
                "Client", "Format", "QueryValue", "QueryParam", "PathVariable", "Header", "Cookie",
                "Authorization", "Body", "application"
        );
        reservedWords.addAll(micronautReservedWords);
        List.of(
            "object",
            "list",
            "file",
            "offsetdatetime",
            "localdate",
            "localtime"
        ).forEach(reservedWords::remove);

        importMapping.put("DateTime", "java.time.Instant");
        importMapping.put("LocalDateTime", "java.time.LocalDateTime");
        importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        importMapping.put("ZonedDateTime", "java.time.ZonedDateTime");
        importMapping.put("LocalDate", "java.time.LocalDate");
        importMapping.put("LocalTime", "java.time.LocalTime");
        importMapping.put("Function", "java.util.function.Function");
    }

    public void setGenerateHttpResponseAlways(boolean generateHttpResponseAlways) {
        this.generateHttpResponseAlways = generateHttpResponseAlways;
    }

    public void setGenerateHttpResponseWhereRequired(boolean generateHttpResponseWhereRequired) {
        this.generateHttpResponseWhereRequired = generateHttpResponseWhereRequired;
    }

    public void setReactive(boolean reactive) {
        this.reactive = reactive;
    }

    public void setTestTool(String testTool) {
        this.testTool = testTool;
    }

    @Override
    public void setArtifactId(String artifactId) {
        super.setArtifactId(artifactId);
        updateOption(CodegenConstants.ARTIFACT_ID, artifactId);
    }

    @Override
    public void setModelPackage(String modelPackage) {
        super.setModelPackage(modelPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);
    }

    @Override
    public void setApiPackage(String apiPackage) {
        super.setApiPackage(apiPackage);
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
    }

    @Override
    public void setApiNamePrefix(String apiNamePrefix) {
        super.setApiNamePrefix(apiNamePrefix);
        updateOption(CodegenConstants.API_NAME_PREFIX, apiNamePrefix);
    }

    @Override
    public void setApiNameSuffix(String apiNameSuffix) {
        super.setApiNameSuffix(apiNameSuffix);
        updateOption(CodegenConstants.API_NAME_SUFFIX, apiNameSuffix);
    }

    @Override
    public void setModelNamePrefix(String modelNamePrefix) {
        super.setModelNamePrefix(modelNamePrefix);
        updateOption(CodegenConstants.MODEL_NAME_PREFIX, modelNamePrefix);
    }

    @Override
    public void setModelNameSuffix(String modelNameSuffix) {
        super.setModelNameSuffix(modelNameSuffix);
        updateOption(CodegenConstants.MODEL_NAME_SUFFIX, modelNameSuffix);
    }

    @Override
    public void setInvokerPackage(String invokerPackage) {
        super.setInvokerPackage(invokerPackage);
        updateOption(INVOKER_PACKAGE, getInvokerPackage());
    }

    public void setLombok(boolean lombok) {
        this.lombok = lombok;
    }

    public void setPlural(boolean plural) {
        this.plural = plural;
    }

    public void setFluxForArrays(boolean fluxForArrays) {
        this.fluxForArrays = fluxForArrays;
    }

    public void setGeneratedAnnotation(boolean generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // Get properties
        if (additionalProperties.containsKey(OPT_TITLE)) {
            title = (String) additionalProperties.get(OPT_TITLE);
        }

        if (additionalProperties.containsKey(INVOKER_PACKAGE)) {
            invokerPackage = (String) additionalProperties.get(INVOKER_PACKAGE);
        } else {
            additionalProperties.put(INVOKER_PACKAGE, invokerPackage);
        }

        if (additionalProperties.containsKey(OPT_APPLICATION_NAME)) {
            appName = (String) additionalProperties.get(OPT_APPLICATION_NAME);
        } else {
            additionalProperties.put(OPT_APPLICATION_NAME, artifactId);
        }

        // Get boolean properties
        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            useBeanValidation = convertPropertyToBoolean(USE_BEANVALIDATION);
        }
        writePropertyBack(USE_BEANVALIDATION, useBeanValidation);

        if (additionalProperties.containsKey(OPT_USE_LOMBOK)) {
            lombok = convertPropertyToBoolean(OPT_USE_LOMBOK);
        }
        writePropertyBack(OPT_USE_LOMBOK, lombok);

        if (additionalProperties.containsKey(OPT_USE_PLURAL)) {
            plural = convertPropertyToBoolean(OPT_USE_PLURAL);
        }
        writePropertyBack(OPT_USE_PLURAL, plural);

        if (additionalProperties.containsKey(OPT_FLUX_FOR_ARRAYS)) {
            fluxForArrays = convertPropertyToBoolean(OPT_FLUX_FOR_ARRAYS);
        }
        writePropertyBack(OPT_FLUX_FOR_ARRAYS, fluxForArrays);

        if (additionalProperties.containsKey(OPT_GENERATED_ANNOTATION)) {
            generatedAnnotation = convertPropertyToBoolean(OPT_GENERATED_ANNOTATION);
        }
        writePropertyBack(OPT_GENERATED_ANNOTATION, generatedAnnotation);

        if (additionalProperties.containsKey(USE_OPTIONAL)) {
            useOptional = convertPropertyToBoolean(USE_OPTIONAL);
        }
        writePropertyBack(USE_OPTIONAL, useOptional);

        if (additionalProperties.containsKey(OPT_VISITABLE)) {
            visitable = convertPropertyToBoolean(OPT_VISITABLE);
        }
        writePropertyBack(OPT_VISITABLE, visitable);

        if (additionalProperties.containsKey(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR)) {
            requiredPropertiesInConstructor = convertPropertyToBoolean(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR);
        }
        writePropertyBack(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, requiredPropertiesInConstructor);

        if (additionalProperties.containsKey(OPT_REACTIVE)) {
            reactive = convertPropertyToBoolean(OPT_REACTIVE);
        }
        writePropertyBack(OPT_REACTIVE, reactive);

        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_ALWAYS)) {
            generateHttpResponseAlways = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, generateHttpResponseAlways);
        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED)) {
            generateHttpResponseWhereRequired = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, generateHttpResponseWhereRequired);

        if (additionalProperties.containsKey(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG)) {
            generateOperationOnlyForFirstTag = convertPropertyToBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG);
        }
        writePropertyBack(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, generateOperationOnlyForFirstTag);

        maybeSetTestTool();
        writePropertyBack(OPT_TEST, testTool);
        if (testTool.equals(OPT_TEST_JUNIT)) {
            additionalProperties.put("isTestJunit", true);
        } else if (testTool.equals(OPT_TEST_SPOCK)) {
            additionalProperties.put("isTestSpock", true);
        }

        maybeSetSwagger();
        if (OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1.equals(generateSwaggerAnnotations)) {
            additionalProperties.put("generateSwagger1Annotations", true);
        } else if (OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2.equals(generateSwaggerAnnotations)) {
            additionalProperties.put("generateSwagger2Annotations", true);
        }

        if (additionalProperties.containsKey(CodegenConstants.SERIALIZATION_LIBRARY)) {
            setSerializationLibrary((String) additionalProperties.get(CodegenConstants.SERIALIZATION_LIBRARY));
        }
        additionalProperties.put(serializationLibrary.toLowerCase(Locale.US), true);
        if (SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name().equals(serializationLibrary)) {
            additionalProperties.put(SerializationLibraryKind.JACKSON.name().toLowerCase(Locale.US), true);
        }

        // Add all the supporting files
        String resourceFolder = projectFolder + "/resources";
        supportingFiles.add(new SupportingFile("common/configuration/application.yml.mustache", resourceFolder, "application.yml").doNotOverwrite());
        supportingFiles.add(new SupportingFile("common/configuration/logback.xml.mustache", resourceFolder, "logback.xml").doNotOverwrite());

        // Use jakarta instead of javax
        additionalProperties.put("javaxPackage", "jakarta");

        // Use the default java time
        switch (dateLibrary) {
            case OPT_DATE_LIBRARY_OFFSET_DATETIME -> {
                typeMapping.put("DateTime", "OffsetDateTime");
                typeMapping.put("date", "LocalDate");
            }
            case OPT_DATE_LIBRARY_ZONED_DATETIME -> {
                typeMapping.put("DateTime", "ZonedDateTime");
                typeMapping.put("date", "LocalDate");
            }
            case OPT_DATE_LIBRARY_LOCAL_DATETIME -> {
                typeMapping.put("DateTime", "LocalDateTime");
                typeMapping.put("date", "LocalDate");
            }
            default -> {
            }
        }

        // Add documentation files
        modelDocTemplateFiles.clear();
        modelDocTemplateFiles.put("common/doc/model_doc.mustache", ".md");

        // Add model files
        modelTemplateFiles.clear();
        modelTemplateFiles.put("common/model/model.mustache", ".java");

        // Add test files
        modelTestTemplateFiles.clear();
        if (testTool.equals(OPT_TEST_JUNIT)) {
            modelTestTemplateFiles.put("common/test/model_test.mustache", ".java");
        } else if (testTool.equals(OPT_TEST_SPOCK)) {
            modelTestTemplateFiles.put("common/test/model_test.groovy.mustache", ".groovy");
        }

        // Set properties for documentation
        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
        final String apiFolder = (sourceFolder + '/' + apiPackage()).replace('.', '/');
        final String modelFolder = (sourceFolder + '/' + modelPackage()).replace('.', '/');
        additionalProperties.put("invokerFolder", invokerFolder);
        additionalProperties.put("resourceFolder", resourceFolder);
        additionalProperties.put("apiFolder", apiFolder);
        additionalProperties.put("modelFolder", modelFolder);

        additionalProperties.put("formatNoEmptyLines", new Formatting.LineFormatter(0));
        additionalProperties.put("formatOneEmptyLine", new Formatting.LineFormatter(1));
        additionalProperties.put("formatSingleLine", new Formatting.SingleLineFormatter());
        additionalProperties.put("indent", new Formatting.IndentFormatter(4));
    }

    public void addParameterMappings(List<ParameterMapping> parameterMappings) {
        this.parameterMappings.addAll(parameterMappings);
    }

    public void addResponseBodyMappings(List<ResponseBodyMapping> responseBodyMappings) {
        this.responseBodyMappings.addAll(responseBodyMappings);
    }

    public void addSchemaMapping(Map<String, String> schemaMapping) {
        this.schemaMapping.putAll(schemaMapping);
    }

    public void addImportMapping(Map<String, String> importMapping) {
        this.importMapping.putAll(importMapping);
    }

    public void addNameMapping(Map<String, String> nameMapping) {
        this.nameMapping.putAll(nameMapping);
    }

    public void addTypeMapping(Map<String, String> typeMapping) {
        this.typeMapping.putAll(typeMapping);
    }

    public void addEnumNameMapping(Map<String, String> enumNameMapping) {
        this.enumNameMapping.putAll(enumNameMapping);
    }

    public void addModelNameMapping(Map<String, String> modelNameMapping) {
        this.modelNameMapping.putAll(modelNameMapping);
    }

    public void addInlineSchemaNameMapping(Map<String, String> inlineSchemaNameMapping) {
        this.inlineSchemaNameMapping.putAll(inlineSchemaNameMapping);
    }

    public void addInlineSchemaOption(Map<String, String> inlineSchemaOption) {
        this.inlineSchemaOption.putAll(inlineSchemaOption);
    }

    public void addOpenapiNormalizer(Map<String, String> openapiNormalizer) {
        this.openapiNormalizer.putAll(openapiNormalizer);
    }

    // CHECKSTYLE:OFF
    private void maybeSetSwagger() {
        if (additionalProperties.containsKey(OPT_GENERATE_SWAGGER_ANNOTATIONS)) {
            String value = String.valueOf(additionalProperties.get(OPT_GENERATE_SWAGGER_ANNOTATIONS));
            switch (value) {
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1 ->
                    generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1;
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2, OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE ->
                    generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2;
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE ->
                    generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE;
                default ->
                    throw new RuntimeException("Value \"" + value + "\" for the " + OPT_GENERATE_SWAGGER_ANNOTATIONS + " parameter is unsupported or misspelled");
            }
        }
    }

    private void maybeSetTestTool() {
        if (additionalProperties.containsKey(OPT_TEST)) {
            switch ((String) additionalProperties.get(OPT_TEST)) {
                case OPT_TEST_JUNIT, OPT_TEST_SPOCK ->
                    testTool = (String) additionalProperties.get(OPT_TEST);
                default ->
                    throw new RuntimeException("Test tool \"" + additionalProperties.get(OPT_TEST) + "\" is not supported or misspelled.");
            }
        }
    }
    // CHECKSTYLE:ON

    public String testFileFolder() {
        if (testTool.equals(OPT_TEST_SPOCK)) {
            return (getOutputDir() + "/src/test/groovy/").replace('/', File.separatorChar);
        }
        return (getOutputDir() + "/src/test/java/").replace('/', File.separatorChar);
    }

    public abstract boolean isServer();

    @Override
    public String apiTestFileFolder() {
        return testFileFolder() + apiPackage().replaceAll("\\.", "/");
    }

    @Override
    public String modelTestFileFolder() {
        if (testTool.equals(OPT_TEST_SPOCK)) {
            return getOutputDir() + "/src/test/groovy/" + modelPackage().replace('.', File.separatorChar);
        }
        return getOutputDir() + "/src/test/java/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String toApiTestFilename(String name) {
        if (testTool.equals(OPT_TEST_SPOCK)) {
            return toApiName(name) + "Spec";
        }
        return toApiName(name) + "Test";
    }

    @Override
    public String toModelTestFilename(String name) {
        if (testTool.equals(OPT_TEST_SPOCK)) {
            return toModelName(name) + "Spec";
        }
        return toModelName(name) + "Test";
    }

    @Override
    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    @Override
    public void setUseOptional(boolean useOptional) {
        this.useOptional = useOptional;
    }

    public void setVisitable(boolean visitable) {
        this.visitable = visitable;
    }

    @Override
    public String toApiVarName(String name) {
        String apiVarName = super.toApiVarName(name);
        if (reservedWords.contains(apiVarName)) {
            apiVarName = escapeReservedWord(apiVarName);
        }
        return apiVarName;
    }

    @Override
    protected boolean isReservedWord(String word) {
        return word != null && reservedWords.contains(word);
    }

    public boolean isUseBeanValidation() {
        return useBeanValidation;
    }

    public boolean isUseOptional() {
        return useOptional;
    }

    public boolean isVisitable() {
        return visitable;
    }

    @Override
    public String sanitizeTag(String tag) {
        // Skip sanitization to get the original tag name in the addOperationToGroup() method.
        // Inside that method tag is manually sanitized.
        return tag;
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
                                    Map<String, List<CodegenOperation>> operations) {
        if (generateOperationOnlyForFirstTag && !co.tags.get(0).getName().equals(tag)) {
            // This is not the first assigned to this operation tag;
            return;
        }

        super.addOperationToGroup(super.sanitizeTag(tag), resourcePath, operation, co, operations);
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {

        if (openAPI.getPaths() != null) {
            for (var path : openAPI.getPaths().values()) {
                if (path.getParameters() == null || path.getParameters().isEmpty()) {
                    continue;
                }

                for (var op : path.readOperations()) {
                    if (op.getParameters() == null) {
                        op.setParameters(new ArrayList<>());
                    }
                    for (var param : path.getParameters()) {
                        var found = false;
                        for (var opParam : op.getParameters()) {
                            if (Objects.equals(opParam.getName(), param.getName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            op.getParameters().add(param);
                        }
                    }
                }
            }
        }

        var inlineModelResolver = new MicronautInlineModelResolver(openAPI);
        inlineModelResolver.flattenPaths();

        super.preprocessOpenAPI(openAPI);
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        Map<String, CodegenModel> models = allModels.stream()
                .map(ModelMap::getModel)
                .collect(Collectors.toMap(v -> v.classname, v -> v));
        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();

        for (CodegenOperation op : operationList) {
            // Set whether body is supported in request
            op.vendorExtensions.put("methodAllowsBody", op.httpMethod.equals("PUT")
                    || op.httpMethod.equals("POST")
                    || op.httpMethod.equals("PATCH")
                    || op.httpMethod.equals("OPTIONS")
                    || op.httpMethod.equals("DELETE")
            );

            // Set response example
            if (op.returnType != null) {
                String example;
                String groovyExample;
                if (models.containsKey(op.returnType)) {
                    CodegenModel m = models.get(op.returnType);
                    List<Object> allowableValues = null;
                    if (m.allowableValues != null && m.allowableValues.containsKey("values")) {
                        allowableValues = (List<Object>) m.allowableValues.get("values");
                    }
                    example = getExampleValue(m.defaultValue, null, m.classname, true,
                            allowableValues, null, null, m.requiredVars, false, false);
                    groovyExample = getExampleValue(m.defaultValue, null, m.classname, true,
                            allowableValues, null, null, m.requiredVars, true, false);
                } else {
                    example = getExampleValue(null, null, op.returnType, false, null,
                            op.returnBaseType, null, null, false, false);
                    groovyExample = getExampleValue(null, null, op.returnType, false, null,
                            op.returnBaseType, null, null, true, false);
                }
                op.vendorExtensions.put("example", example);
                op.vendorExtensions.put("groovyExample", groovyExample);
            }

            // Remove the "*/*" contentType from operations as it is ambiguous
            if (CONTENT_TYPE_ANY.equals(op.vendorExtensions.get("x-contentType"))) {
                op.vendorExtensions.put("x-contentType", CONTENT_TYPE_APPLICATION_JSON);
            }
            op.consumes = op.consumes == null ? null : op.consumes.stream()
                    .filter(contentType -> !CONTENT_TYPE_ANY.equals(contentType.get("mediaType")))
                    .toList();
            op.produces = op.produces == null ? null : op.produces.stream()
                    .filter(contentType -> !CONTENT_TYPE_ANY.equals(contentType.get("mediaType")))
                    .toList();

            // is only default "application/json" media type
            if (op.consumes == null
                    || op.consumes.isEmpty()
                    || op.consumes.size() == 1 && "application/json".equals(op.consumes.get(0).get("mediaType"))) {
                op.vendorExtensions.put("onlyDefaultConsumeOrEmpty", true);
            }
            // is only default "application/json" media type
            if (op.produces == null
                    || op.produces.isEmpty()
                    || op.produces.size() == 1 && "application/json".equals(op.produces.get(0).get("mediaType"))) {
                op.vendorExtensions.put("onlyDefaultProduceOrEmpty", true);
            }

            // Force form parameters are only set if the content-type is according
            // formParams correspond to urlencoded type
            // bodyParams correspond to multipart body
            if (CONTENT_TYPE_APPLICATION_FORM_URLENCODED.equals(op.vendorExtensions.get("x-contentType"))) {
                op.formParams.addAll(op.bodyParams);
                op.bodyParams.forEach(p -> {
                    p.isBodyParam = false;
                    p.isFormParam = true;
                });
                op.bodyParams.clear();
            } else if (CONTENT_TYPE_MULTIPART_FORM_DATA.equals(op.vendorExtensions.get("x-contentType"))) {
                op.bodyParams.addAll(op.formParams);
                op.formParams.forEach(p -> {
                    p.isBodyParam = true;
                    p.isFormParam = false;
                });
                op.formParams.clear();
            }

            for (var param : op.allParams) {
                processGenericAnnotations(param, useBeanValidation, isGenerateHardNullable(), false, false, false, false);
                if (useBeanValidation && !param.isContainer && param.isModel) {
                    param.vendorExtensions.put("withValid", true);
                }
            }
            if (op.returnProperty != null) {
                processGenericAnnotations(op.returnProperty, useBeanValidation, isGenerateHardNullable(), false, false, false, false);
                op.returnType = op.returnProperty.vendorExtensions.get("typeWithEnumWithGenericAnnotations").toString();
            }
        }

        return objs;
    }

    @Override
    public CodegenModel fromModel(String name, Schema model) {
        CodegenModel codegenModel = super.fromModel(name, model);
        codegenModel.imports.remove("ApiModel");
        codegenModel.imports.remove("ApiModelProperty");
        allModels.put(name, codegenModel);
        return codegenModel;
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("Integer".equals(datatype) || "Double".equals(datatype)) {
            return value;
        } else if ("Long".equals(datatype)) {
            // add l to number, e.g. 2048 => 2048L
            return value + "L";
        } else if ("Float".equals(datatype)) {
            // add f to number, e.g. 3.14 => 3.14F
            return value + "F";
        } else if ("BigDecimal".equals(datatype)) {
            // use BigDecimal String constructor
            return "new BigDecimal(\"" + value + "\")";
        } else if ("URI".equals(datatype)) {
            return "URI.create(\"" + escapeText(value) + "\")";
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, servers);

        if (op.isResponseFile) {
            op.returnType = typeMapping.get("responseFile");
            op.returnProperty.dataType = op.returnType;
            op.returnProperty.datatypeWithEnum = op.returnType;
            op.imports.add(op.returnType);
        }

        op.vendorExtensions.put("originalParams", new ArrayList<>(op.allParams));
        var hasMultipleParams = false;
        var notBodyParamsSize = 0;
        for (var param : op.allParams) {
            if (param.isBodyParam) {
                continue;
            }
            notBodyParamsSize++;
            if (notBodyParamsSize > 1) {
                hasMultipleParams = true;
                break;
            }
        }
        for (var param : op.allParams) {
            param.vendorExtensions.put("hasNotBodyParam", notBodyParamsSize > 0);
            param.vendorExtensions.put("hasMultipleParams", hasMultipleParams);
        }
        op.vendorExtensions.put("originReturnProperty", op.returnProperty);
        if (op.responses != null && !op.responses.isEmpty()) {
            for (var resp : op.responses) {
                if (resp.isDefault) {
                    resp.code = "default";
                }
            }
        }

        processParametersWithAdditionalMappings(op.allParams, op.imports);
        processWithResponseBodyMapping(op);
        processOperationWithResponseWrappers(op);

        return op;
    }

    /**
     * Method that maps parameters if a corresponding mapping is specified.
     *
     * @param params The parameters to modify.
     * @param imports The operation imports.
     */
    private void processParametersWithAdditionalMappings(List<CodegenParameter> params, Set<String> imports) {
        Map<String, ParameterMapping> additionalMappings = new LinkedHashMap<>();
        Iterator<CodegenParameter> iter = params.iterator();
        while (iter.hasNext()) {
            CodegenParameter param = iter.next();
            boolean paramWasMapped = false;
            for (ParameterMapping mapping : parameterMappings) {
                if (mapping.doesMatch(param)) {
                    additionalMappings.put(mapping.mappedName(), mapping);
                    paramWasMapped = true;
                }
            }
            if (paramWasMapped) {
                iter.remove();
            } else {
                if (plural && param.isArray && param.isBodyParam
                    && StringUtils.isEmpty(param.getRef())
                    && !DEFAULT_BODY_PARAM_NAME.equals(param.paramName)) {
                    param.paramName = English.plural(param.paramName);
                }
            }
        }

        for (ParameterMapping mapping : additionalMappings.values()) {
            if (mapping.mappedType() != null) {
                CodegenParameter newParam = new CodegenParameter();
                newParam.paramName = mapping.mappedName();
                newParam.required = true;
                newParam.isModel = mapping.isValidated();

                String typeName = makeSureImported(mapping.mappedType(), imports);
                newParam.dataType = typeName;

                // Set the paramName if required
                if (newParam.paramName == null) {
                    newParam.paramName = toParamName(typeName);
                }

                params.add(newParam);
            }
        }
    }

    /**
     * Method that changes the return type if the corresponding header is specified.
     *
     * @param op The operation to modify.
     */
    private void processWithResponseBodyMapping(CodegenOperation op) {
        ResponseBodyMapping bodyMapping = null;

        Iterator<CodegenProperty> iter = op.responseHeaders.iterator();
        while (iter.hasNext()) {
            CodegenProperty header = iter.next();
            boolean headerWasMapped = false;
            for (ResponseBodyMapping mapping : responseBodyMappings) {
                if (mapping.doesMatch(header.baseName, op.isArray)) {
                    if (mapping.mappedBodyType() != null) {
                        bodyMapping = mapping;
                    }
                    headerWasMapped = true;
                }
            }
            if (headerWasMapped) {
                iter.remove();
            }
        }

        if (bodyMapping != null) {
            wrapOperationReturnType(op, bodyMapping.mappedBodyType(), bodyMapping.isValidated(), bodyMapping.isListWrapper());
        }
    }

    /**
     * Wrap the return type of operation in the provided type.
     *
     * @param op The operation to modify.
     * @param wrapperType The wrapper type.
     * @param isValidated Whether the wrapper requires validation.
     * @param isListWrapper Whether the wrapper should be around list items.
     */
    private void wrapOperationReturnType(CodegenOperation op, String wrapperType, boolean isValidated, boolean isListWrapper) {
        CodegenProperty newReturnType = new CodegenProperty();
        newReturnType.required = true;
        newReturnType.isModel = isValidated;

        String typeName = makeSureImported(wrapperType, op.imports);

        String originalReturnType;
        if ((isListWrapper || fluxForArrays) && op.isArray && op.returnProperty.items != null) {
            if (fluxForArrays && wrapperType.equals(MONO_CLASS_NAME)) {
                typeName = makeSureImported(FLUX_CLASS_NAME, op.imports);
                op.vendorExtensions.put("isReturnFlux", true);
            }
            originalReturnType = op.returnBaseType;
            newReturnType.dataType = typeName + '<' + op.returnBaseType + '>';
            newReturnType.items = op.returnProperty.items;
        } else {
            originalReturnType = op.returnType;
            if (originalReturnType == null) {
                originalReturnType = "Void";
                op.returnProperty = new CodegenProperty();
                op.returnProperty.dataType = "Void";
                op.returnProperty.openApiType = "";
            }
            newReturnType.dataType = typeName + '<' + originalReturnType + '>';
            newReturnType.items = op.returnProperty;
        }
        newReturnType.containerTypeMapped = typeName;
        newReturnType.containerType = typeName;
        op.vendorExtensions.put("originalReturnType", originalReturnType);

        op.returnType = newReturnType.dataType;
        op.returnContainer = newReturnType.containerTypeMapped;
        op.returnProperty = newReturnType;
        op.isArray = op.returnProperty.isArray;
    }

    private void processOperationWithResponseWrappers(CodegenOperation op) {
        boolean hasNon200StatusCodes = op.responses.stream().anyMatch(
                response -> !"200".equals(response.code) && response.code.startsWith("2")
        );
        boolean hasNonMappedHeaders = !op.responseHeaders.isEmpty();
        boolean requiresHttpResponse = hasNon200StatusCodes || hasNonMappedHeaders;
        if (generateHttpResponseAlways || (generateHttpResponseWhereRequired && requiresHttpResponse)) {
            wrapOperationReturnType(op, "io.micronaut.http.HttpResponse", false, false);
        }

        if (reactive) {
            wrapOperationReturnType(op, MONO_CLASS_NAME, false, false);
        }
    }

    private String makeSureImported(String typeName, Set<String> imports) {
        // Find the index of the first capital letter
        int firstCapitalIndex = 0;
        for (int i = 0; i < typeName.length(); i++) {
            if (Character.isUpperCase(typeName.charAt(i))) {
                firstCapitalIndex = i;
                break;
            }
        }

        // Add import if the name is fully-qualified
        if (firstCapitalIndex != 0) {
            // Add import if fully-qualified name is used
            String dataType = typeName.substring(firstCapitalIndex);
            importMapping.put(dataType, typeName);
            typeName = dataType;
        }
        imports.add(typeName);
        return typeName;
    }

    @Override
    public String toVarName(String name) {
        var varName = super.toVarName(name);

        if (varName.chars().allMatch(Character::isUpperCase)) {
            return varName;
        }

        // Micronaut can't process correctly properties like `eTemperature`, when first symbol in lower case
        // and second symbol in upper case.
        // See this: https://github.com/micronaut-projects/micronaut-core/pull/10130
        if (varName.length() >= 2 && Character.isLowerCase(varName.charAt(0)) && Character.isUpperCase(varName.charAt(1))) {
            varName = "" + varName.charAt(0) + Character.toLowerCase(varName.charAt(1)) + varName.substring(2);
        }

        // this fix for properties started with underscores and named by reserved words
        // For example, _____default
        var firstNameChar = varName.toCharArray()[0];
        var underscorePrefix = getUnderscorePrefix(name);
        varName = getUnderscorePrefix(name)
            + (firstNameChar == '_' && !underscorePrefix.isEmpty() ? "" : Character.toLowerCase(firstNameChar))
            + varName.substring(1);

        return varName;
    }

    @Override
    public String getterAndSetterCapitalize(String name) {
        var newName = super.getterAndSetterCapitalize(name);
        if (name.startsWith("_")) {
            newName = getUnderscorePrefix(name)
                    + Character.toLowerCase(newName.toCharArray()[0])
                    + newName.substring(1);
        }
        return newName;
    }

    private String getUnderscorePrefix(String name) {
        var nameChars = name.toCharArray();
        var newNameBuilder = new StringBuilder();
        for (char nameChar : nameChars) {
            if (nameChar != '_') {
                break;
            }
            newNameBuilder.append('_');
        }
        return newNameBuilder.toString();
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        objs = super.postProcessAllModels(objs);

        var isServer = isServer();
        var random = new Random();

        for (ModelsMap models : objs.values()) {
            CodegenModel model = models.getModels().get(0).getModel();

            var hasParent = model.getParentModel() != null;
            var requiredVarsWithoutDiscriminator = new ArrayList<CodegenProperty>();
            var requiredParentVarsWithoutDiscriminator = new ArrayList<CodegenProperty>();
            var allVars = new ArrayList<CodegenProperty>();

            processParentModel(model, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars);

            var optionalVars = new ArrayList<CodegenProperty>();
            var requiredVars = new ArrayList<CodegenProperty>();
            for (var v : model.vars) {
                if (v.required) {
                    requiredVars.add(v);
                } else {
                    optionalVars.add(v);
                }
            }

            model.vendorExtensions.put("withMultipleVars", model.vars.size() > 1);
            if (!requiredParentVarsWithoutDiscriminator.isEmpty()) {
                model.vendorExtensions.put("requiredParentVarsWithoutDiscriminator", requiredParentVarsWithoutDiscriminator);
            }
            if (!requiredVarsWithoutDiscriminator.isEmpty()) {
                model.vendorExtensions.put("requiredVarsWithoutDiscriminator", requiredVarsWithoutDiscriminator);
            }
            model.allVars = allVars;
            model.vendorExtensions.put("requiredVars", requiredVars);
            model.vendorExtensions.put("optionalVars", optionalVars);
            model.vendorExtensions.put("areRequiredVarsAndReadOnlyVars", !requiredVarsWithoutDiscriminator.isEmpty() && !model.readOnlyVars.isEmpty());
            model.vendorExtensions.put("serialId", random.nextLong());
            model.vendorExtensions.put("withRequiredVars", !model.requiredVars.isEmpty());
            if (model.discriminator != null) {
                model.vendorExtensions.put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
                model.vendorExtensions.put("hasMultipleMappedModels", model.discriminator.getMappedModels().size() > 1);
                model.discriminator.getVendorExtensions().put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
                model.discriminator.getVendorExtensions().put("hasMultipleMappedModels", model.discriminator.getMappedModels().size() > 1);
            }
            model.vendorExtensions.put("isServer", isServer);
            for (var property : model.vars) {
                processProperty(property, isServer, objs);
            }
            for (var property : model.requiredVars) {
                processProperty(property, isServer, objs);
            }
            if (model.isEnum) {
                addImport(model, "Function");
            }
            addStrValueToEnum(model);
        }

        return objs;
    }

    private void processProperty(CodegenProperty property, boolean isServer, Map<String, ModelsMap> models) {

        property.vendorExtensions.put("inRequiredArgsConstructor", !property.isReadOnly || isServer);
        property.vendorExtensions.put("isServer", isServer);
        property.vendorExtensions.put("lombok", lombok);
        property.vendorExtensions.put("defaultValueIsNotNull", property.defaultValue != null && !property.defaultValue.equals("null"));
        if (useBeanValidation && (
            (!property.isContainer && property.isModel)
                || (property.getIsArray() && property.getComplexType() != null && models.containsKey(property.getComplexType()))
        )) {
            property.vendorExtensions.put("withValid", true);
        }

        processGenericAnnotations(property, useBeanValidation, isGenerateHardNullable(), false, false, false, false);
    }

    public boolean isGenerateHardNullable() {
        return false;
    }

    private void processParentModel(CodegenModel model, List<CodegenProperty> requiredVarsWithoutDiscriminator,
                                    List<CodegenProperty> requiredParentVarsWithoutDiscriminator,
                                    List<CodegenProperty> allVars) {
        var parent = model.getParentModel();
        var hasParent = parent != null;

        allVars.addAll(model.vars);

        for (var v : model.requiredVars) {
            boolean isDiscriminator = isDiscriminator(v, model);
            if (!isDiscriminator(v, model) && !containsProp(v, requiredVarsWithoutDiscriminator)) {
                requiredVarsWithoutDiscriminator.add(v);
            }
        }

        requiredParentVarsWithoutDiscriminator(model, requiredParentVarsWithoutDiscriminator);
        if (hasParent) {
            model.parentVars = parent.allVars;
        }
        if (hasParent) {
            processParentModel(parent, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars);
        }
    }

    private void requiredParentVarsWithoutDiscriminator(CodegenModel model, List<CodegenProperty> requiredParentVarsWithoutDiscriminator) {

        var parent = model.parentModel;
        if (parent == null) {
            return;
        }

        for (var v : parent.vars) {
            boolean isDiscriminator = isDiscriminator(v, model);
            if (v.required && !isDiscriminator) {
                v.vendorExtensions.put("isServerOrNotReadOnly", !v.isReadOnly || isServer());
                if (!containsProp(v, requiredParentVarsWithoutDiscriminator)) {
                    requiredParentVarsWithoutDiscriminator.add(v);
                }
            }
        }
    }

    private boolean containsProp(CodegenProperty prop, List<CodegenProperty> props) {
        for (var p : props) {
            if (prop.name.equals(p.name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDiscriminator(CodegenProperty prop, CodegenModel model) {
        var isDiscriminator = prop.isDiscriminator;
        if (isDiscriminator) {
            return true;
        }
        if (model.parentModel == null) {
            return false;
        }
        CodegenProperty parentProp = null;
        for (var pv : model.parentModel.allVars) {
            if (pv.required && pv.name.equals(prop.name)) {
                isDiscriminator = pv.isDiscriminator;
                parentProp = pv;
                break;
            }
        }
        if (isDiscriminator) {
            return true;
        }
        return parentProp != null && isDiscriminator(parentProp, model.parentModel);
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        p.vendorExtensions.put("groovyExample", getParameterExampleValue(p, true));
        p.example = getParameterExampleValue(p, false);
    }

    protected String getParameterExampleValue(CodegenParameter p, boolean groovy) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            p.requiredVars, groovy, false);
    }

    protected String getPropertyExampleValue(CodegenProperty p, boolean groovy) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");
        var model = allModels.get(p.getDataType());

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            model != null ? model.requiredVars : null, groovy, true);
    }

    public String getExampleValue(
            String defaultValue, String example, String dataType, Boolean isModel, List<Object> allowableValues,
            String itemsType, String itemsExample, List<CodegenProperty> requiredVars, boolean groovy, boolean isProperty
    ) {
        example = defaultValue != null ? defaultValue : example;
        String containerType = dataType == null ? null : dataType.split("<")[0];

        if ("String".equals(dataType)) {
            if (groovy) {
                example = example != null ? "'" + escapeTextGroovy(example) + "'" : "'example'";
            } else {
                example = example != null ? "\"" + escapeText(example) + "\"" : "\"example\"";
            }
        } else if ("Integer".equals(dataType) || "Short".equals(dataType)) {
            example = example != null ? example : "56";
        } else if ("Long".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(example != null ? example : "56", "L");
        } else if ("Float".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(example != null ? example : "3.4", "F");
        } else if ("Double".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(example != null ? example : "3.4", "D");
        } else if ("Boolean".equals(dataType)) {
            example = example != null ? example : "false";
        } else if ("File".equals(dataType)) {
            example = null;
        } else if ("OffsetDateTime".equals(dataType)) {
            example = "OffsetDateTime.of(2001, 2, 3, 12, 0, 0, 0, java.time.ZoneOffset.of(\"+02:00\"))";
        } else if ("LocalDate".equals(dataType)) {
            example = "LocalDate.of(2001, 2, 3)";
        } else if ("LocalDateTime".equals(dataType)) {
            example = "LocalDateTime.of(2001, 2, 3, 4, 5)";
        } else if ("BigDecimal".equals(dataType)) {
            example = "new BigDecimal(\"78\")";
        } else if (allowableValues != null && !allowableValues.isEmpty()) {
            // This is an enum
            Object value = example;
            if (value == null || !allowableValues.contains(value)) {
                value = allowableValues.get(0);
            }
            if (isProperty) {
                dataType = importMapping.getOrDefault(dataType, modelPackage + '.' + dataType);
            }
            example = dataType + ".fromValue(\"" + value + "\")";
        } else if ((isModel != null && isModel) || (isModel == null && !languageSpecificPrimitives.contains(dataType))) {
            if (requiredVars == null) {
                example = null;
            } else {
                if (requiredPropertiesInConstructor) {
                    StringBuilder builder = new StringBuilder();
                    if (isProperty) {
                        dataType = importMapping.getOrDefault(dataType, modelPackage + '.' + dataType);
                    }
                    builder.append("new ").append(dataType).append("(");
                    for (int i = 0; i < requiredVars.size(); ++i) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(getPropertyExampleValue(requiredVars.get(i), groovy));
                    }
                    builder.append(")");
                    example = builder.toString();
                } else {
                    example = "new " + dataType + "()";
                }
            }
        }

        if ("List".equals(containerType)) {
            String innerExample;
            if ("String".equals(itemsType)) {
                itemsExample = itemsExample != null ? itemsExample : "example";
                if (groovy) {
                    innerExample = "'" + escapeTextGroovy(itemsExample) + "'";
                } else {
                    innerExample = "\"" + escapeText(itemsExample) + "\"";
                }
            } else {
                innerExample = itemsExample != null ? itemsExample : "";
            }

            if (groovy) {
                example = "[" + innerExample + "]";
            } else {
                example = "List.of(" + innerExample + ")";
            }
        } else if ("Set".equals(containerType)) {
            if (groovy) {
                example = "[].asSet()";
            } else {
                example = "new HashSet<>()";
            }
        } else if ("Map".equals(containerType)) {
            if (groovy) {
                example = "[:]";
            } else {
                example = "new HashMap<>()";
            }
        } else if (example == null) {
            example = "null";
        }

        return example;
    }

    public String escapeTextGroovy(String text) {
        if (text == null) {
            return null;
        }
        return escapeText(text).replaceAll("'", "'");
    }

    @Override
    protected ImmutableMap.Builder<String, Mustache.Lambda> addMustacheLambdas() {
        return super.addMustacheLambdas()
                .put("replaceDotsWithUnderscore", new ReplaceDotsWithUnderscoreLambda());
    }

    public void setSerializationLibrary(final String serializationLibrary) {
        try {
            this.serializationLibrary = SerializationLibraryKind.valueOf(serializationLibrary).name();
        } catch (IllegalArgumentException ex) {
            StringBuilder sb = new StringBuilder(serializationLibrary + " is an invalid enum property naming option. Please choose from:");
            for (SerializationLibraryKind availableSerializationLibrary : SerializationLibraryKind.values()) {
                sb.append("\n  ").append(availableSerializationLibrary.name());
            }
            throw new RuntimeException(sb.toString());
        }
    }

    public void setDateTimeLibrary(String name) {
        setDateLibrary(name);
    }

}
