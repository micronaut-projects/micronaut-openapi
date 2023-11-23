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
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.micronaut.openapi.generator.Formatting.ReplaceDotsWithUnderscoreLambda;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractKotlinCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.meta.features.ClientModificationFeature;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.GlobalFeature;
import org.openapitools.codegen.meta.features.SchemaSupportFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.meta.features.WireFormatFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;

import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;

import static io.micronaut.openapi.generator.Utils.processGenericAnnotations;
import static org.openapitools.codegen.CodegenConstants.INVOKER_PACKAGE;
import static org.openapitools.codegen.languages.KotlinClientCodegen.DATE_LIBRARY;
import static org.openapitools.codegen.utils.StringUtils.camelize;

/**
 * Base generator for Micronaut.
 *
 * @param <T> The generator options builder.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public abstract class AbstractMicronautKotlinCodegen<T extends GeneratorOptionsBuilder> extends AbstractKotlinCodegen implements BeanValidationFeatures, MicronautCodeGenerator<T> {

    public static final String OPT_TITLE = "title";
    public static final String OPT_TEST = "test";
    public static final String OPT_TEST_JUNIT = "junit";
    public static final String OPT_TEST_SPOCK = "spock";
    public static final String OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR = "requiredPropertiesInConstructor";
    public static final String OPT_USE_AUTH = "useAuth";
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

    protected String dateLibrary;
    protected String title;
    protected boolean useBeanValidation;
    protected boolean visitable;
    protected boolean fluxForArrays;
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

    private final Map<String, String> schemaKeyToModelNameCache = new HashMap<>();

    protected AbstractMicronautKotlinCodegen() {

        languageSpecificPrimitives = Set.of(
            "Byte",
            "ByteArray",
            "Short",
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "String",
            "Array",
            "List",
            "MutableList",
            "Map",
            "MutableMap",
            "Set",
            "MutableSet",
            "Any"
        );

        defaultIncludes = Set.of(
            "Byte",
            "ByteArray",
            "Short",
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "Array",
            "List",
            "MutableList",
            "Set",
            "MutableSet",
            "Map",
            "MutableMap",
            "Any"
        );

        // CHECKSTYLE:OFF
        // Set all the fields
        useBeanValidation = true;
        visitable = false;
        hideGenerationTimestamp = false;
        testTool = OPT_TEST_JUNIT;
        outputFolder = this instanceof KotlinMicronautClientCodegen ?
            "generated-code/kotlin-micronaut-client" : "generated-code/kotlin-micronaut";
        apiPackage = "org.openapitools.api";
        modelPackage = "org.openapitools.model";
        packageName = "org.openapitools";
        artifactId = this instanceof KotlinMicronautClientCodegen ?
            "openapi-micronaut-client" : "openapi-micronaut";
        embeddedTemplateDir = templateDir = "templates/kotlin-micronaut";
        apiDocPath = "docs/apis";
        modelDocPath = "docs/models";
        dateLibrary = OPT_DATE_LIBRARY_ZONED_DATETIME;
        reactive = true;
        appName = artifactId;
        generateSwaggerAnnotations = this instanceof KotlinMicronautClientCodegen ? OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE : OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2;
        generateOperationOnlyForFirstTag = this instanceof KotlinMicronautServerCodegen;
        enumPropertyNaming = CodegenConstants.ENUM_PROPERTY_NAMING_TYPE.UPPERCASE;
        // CHECKSTYLE:ON

        // Set implemented features for user information
        modifyFeatureSet(features -> features
            .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML))
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
            .excludeGlobalFeatures(GlobalFeature.XMLStructureDefinitions, GlobalFeature.Callbacks, GlobalFeature.LinkObjects, GlobalFeature.ParameterStyling)
            .excludeSchemaSupportFeatures(SchemaSupportFeature.Polymorphism)
            .includeClientModificationFeatures(ClientModificationFeature.BasePath)
        );

        // Set additional properties
        additionalProperties.put("openbrace", "{");
        additionalProperties.put("closebrace", "}");

        // Set client options that will be presented to user
        updateOption(INVOKER_PACKAGE, packageName);
        updateOption(CodegenConstants.ARTIFACT_ID, artifactId);
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);

        cliOptions.add(new CliOption(OPT_TITLE, "Client service name").defaultValue(title));
        cliOptions.add(new CliOption(OPT_APPLICATION_NAME, "Micronaut application name (Defaults to the " + CodegenConstants.ARTIFACT_ID + " value)").defaultValue(appName));
        cliOptions.add(CliOption.newBoolean(OPT_FLUX_FOR_ARRAYS, "Whether or not to use Flux<?> instead Mono<List<?>> for arrays in generated code", fluxForArrays));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATED_ANNOTATION, "Generate code with \"@Generated\" annotation", generatedAnnotation));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations", useBeanValidation));
        cliOptions.add(CliOption.newBoolean(OPT_VISITABLE, "Generate visitor for subtypes with a discriminator", visitable));
        cliOptions.add(CliOption.newBoolean(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "Allow only to create models with all the required properties provided in constructor", requiredPropertiesInConstructor));
        cliOptions.add(CliOption.newBoolean(OPT_REACTIVE, "Make the responses use Reactor Mono as wrapper", reactive));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "Always wrap the operations response in HttpResponse object", generateHttpResponseAlways));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, "Wrap the operations response in HttpResponse object where non-200 HTTP status codes or additional headers are defined", generateHttpResponseWhereRequired));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC, isHideGenerationTimestamp()));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "When false, the operation method will be duplicated in each of the tags if multiple tags are assigned to this operation. " +
            "If true, each operation will be generated only once in the first assigned tag.", generateOperationOnlyForFirstTag));
        CliOption testToolOption = new CliOption(OPT_TEST, "Specify which test tool to generate files for").defaultValue(testTool);
        Map<String, String> testToolOptionMap = new HashMap<>();
        testToolOptionMap.put(OPT_TEST_JUNIT, "Use JUnit as test tool");
        testToolOption.setEnum(testToolOptionMap);
        cliOptions.add(testToolOption);

        CliOption generateSwaggerAnnotationsOption = new CliOption(OPT_GENERATE_SWAGGER_ANNOTATIONS, "Specify if you want to generate swagger annotations and which version").defaultValue(generateSwaggerAnnotations);
        Map<String, String> generateSwaggerAnnotationsOptionMap = new HashMap<>();
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
        cliOptions.removeIf(opt -> opt.getOpt().equals(CodegenConstants.SERIALIZATION_LIBRARY));
        cliOptions.add(serializationLibraryOpt);

        // Add reserved words
        reservedWords.addAll(List.of(
            "Client",
            "Format",
            "QueryValue",
            "QueryParam",
            "PathVariable",
            "Header",
            "Cookie",
            "Authorization",
            "Body",
            "application"
        ));

//        typeMapping = new HashMap<>();
        typeMapping.put("string", "String");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("integer", "Int");
        typeMapping.put("float", "Float");
        typeMapping.put("long", "Long");
        typeMapping.put("double", "Double");
        typeMapping.put("ByteArray", "ByteArray");
        typeMapping.put("number", "BigDecimal");
        typeMapping.put("decimal", "BigDecimal");
        typeMapping.put("file", "File");
        typeMapping.put("array", "List");
        typeMapping.put("list", "List");
        typeMapping.put("set", "Set");
        typeMapping.put("map", "Map");
        typeMapping.put("object", "Any");
        typeMapping.put("binary", "ByteArray");
        typeMapping.put("AnyType", "Any");
        typeMapping.put("DateTime", "Instant");
        typeMapping.put("date-time", "OffsetDateTime");
        typeMapping.put("date", "LocalDate");
        typeMapping.put("Date", "LocalDate");
        typeMapping.put("LocalDateTime", "LocalDateTime");
        typeMapping.put("OffsetDateTime", "OffsetDateTime");
        typeMapping.put("ZonedDateTime", "ZonedDateTime");
        typeMapping.put("LocalDate", "LocalDate");
        typeMapping.put("LocalTime", "LocalTime");

        instantiationTypes.put("array", "ArrayList");
        instantiationTypes.put("list", "ArrayList");
        instantiationTypes.put("map", "HashMap");

        importMapping.put("File", "java.io.File");
        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("DateTime", "java.time.Instant");
        importMapping.put("LocalDateTime", "java.time.LocalDateTime");
        importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        importMapping.put("ZonedDateTime", "java.time.ZonedDateTime");
        importMapping.put("LocalDate", "java.time.LocalDate");
        importMapping.put("LocalTime", "java.time.LocalTime");
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

    public void setInvokerPackage(String packageName) {
        updateOption(INVOKER_PACKAGE, packageName);
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
            packageName = (String) additionalProperties.get(INVOKER_PACKAGE);
        } else {
            additionalProperties.put(INVOKER_PACKAGE, packageName);
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

        if (additionalProperties.containsKey(OPT_FLUX_FOR_ARRAYS)) {
            fluxForArrays = convertPropertyToBoolean(OPT_FLUX_FOR_ARRAYS);
        }
        writePropertyBack(OPT_FLUX_FOR_ARRAYS, fluxForArrays);

        if (additionalProperties.containsKey(OPT_GENERATED_ANNOTATION)) {
            generatedAnnotation = convertPropertyToBoolean(OPT_GENERATED_ANNOTATION);
        }
        writePropertyBack(OPT_GENERATED_ANNOTATION, generatedAnnotation);

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
        additionalProperties.put(OPT_TEST, testTool);
        if (testTool.equals(OPT_TEST_JUNIT)) {
            additionalProperties.put("isTestJunit", true);
        }

        maybeSetSwagger();
        if (OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2.equals(generateSwaggerAnnotations)) {
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
        supportingFiles.add(new SupportingFile("common/configuration/application.yml.mustache", resourcesFolder, "application.yml").doNotOverwrite());
        supportingFiles.add(new SupportingFile("common/configuration/logback.xml.mustache", resourcesFolder, "logback.xml").doNotOverwrite());

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
        modelTemplateFiles.put("common/model/model.mustache", ".kt");

        // Add test files
        modelTestTemplateFiles.clear();
        if (testTool.equals(OPT_TEST_JUNIT)) {
            modelTestTemplateFiles.put("common/test/model_test.mustache", ".kt");
        }

        // Set properties for documentation
        final String invokerFolder = (sourceFolder + '/' + packageName).replace(".", "/");
        final String apiFolder = (sourceFolder + '/' + apiPackage()).replace('.', '/');
        final String modelFolder = (sourceFolder + '/' + modelPackage()).replace('.', '/');
        additionalProperties.put("invokerFolder", invokerFolder);
        additionalProperties.put("resourceFolder", resourcesFolder);
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

    // CHECKSTYLE:OFF
    private void maybeSetSwagger() {
        if (additionalProperties.containsKey(OPT_GENERATE_SWAGGER_ANNOTATIONS)) {
            String value = String.valueOf(additionalProperties.get(OPT_GENERATE_SWAGGER_ANNOTATIONS));
            switch (value) {
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
        return (getOutputDir() + "/src/test/kotlin/").replace('/', File.separatorChar);
    }

    public abstract boolean isServer();

    @Override
    public String apiTestFileFolder() {
        return testFileFolder() + apiPackage().replaceAll("\\.", "/");
    }

    @Override
    public String modelTestFileFolder() {
        return getOutputDir() + "/src/test/kotlin/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelName(name) + "Test";
    }

    @Override
    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
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

    public boolean isUseBeanValidation() {
        return useBeanValidation;
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
                if (models.containsKey(op.returnType)) {
                    CodegenModel m = models.get(op.returnType);
                    List<Object> allowableValues = null;
                    if (m.allowableValues != null && m.allowableValues.containsKey("values")) {
                        allowableValues = (List<Object>) m.allowableValues.get("values");
                    }
                    example = getExampleValue(m.defaultValue, null, m.classname, true,
                        allowableValues, null, null, m.requiredVars,  false);
                } else {
                    example = getExampleValue(null, null, op.returnType, false, null,
                        op.returnBaseType, null, null,  false);
                }
                op.vendorExtensions.put("example", example);
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
                processGenericAnnotations(param, useBeanValidation, false, param.isNullable || !param.required,
                    param.required, false, true);
                param.vendorExtensions.put("isString", "string".equalsIgnoreCase(param.dataType));
                param.vendorExtensions.put("withoutExample", param.example == null || param.example.equals("null"));
                if (useBeanValidation && ((!param.isContainer && param.isModel)
                    || (param.getIsArray() && param.getComplexType() != null && models.containsKey(param.getComplexType())))) {
                    param.vendorExtensions.put("withValid", true);
                }
            }

            if (op.returnProperty != null) {
                processGenericAnnotations(op.returnProperty, useBeanValidation, false, false, false, false, false);
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

    /**
     * Output the type declaration of the property.
     *
     * @param p OpenAPI Property object
     * @return a string presentation of the property type
     */
    @Override
    public String getTypeDeclaration(Schema p) {
        Schema<?> schema = unaliasSchema(p);
        Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? p : schema;
        if (ModelUtils.isArraySchema(target)) {
            Schema<?> items = getSchemaItems((ArraySchema) schema);
            return getSchemaType(target) + "<" + getTypeDeclaration(items) + ">";
        }

        if (ModelUtils.isMapSchema(target)) {
            // Note: ModelUtils.isMapSchema(p) returns true when p is a composed schema that also defines
            // additionalproperties: true
            Schema<?> inner = ModelUtils.getAdditionalProperties(target);
            if (inner == null) {
                System.err.println("`" + p.getName() + "` (map property) does not have a proper inner type defined. Default to type:string");
                inner = new StringSchema().description("TODO default missing map inner type to string");
                p.setAdditionalProperties(inner);
            }
            return getSchemaType(target) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(target);
    }

    @Override
    public String toModelName(final String name) {
        // memoization
        if (schemaKeyToModelNameCache.containsKey(name)) {
            return schemaKeyToModelNameCache.get(name);
        }

        // Allow for explicitly configured kotlin.* and java.* types
        if (name.startsWith("kotlin.") || name.startsWith("java.")) {
            return name;
        }

        // If schemaMapping contains name, assume this is a legitimate model name.
        if (schemaMapping.containsKey(name)) {
            return schemaMapping.get(name);
        }

        String modifiedName = name.replaceAll("\\.", "").replaceAll("-", "_");

        String nameWithPrefixSuffix = sanitizeKotlinSpecificNames(modifiedName);
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = modelNamePrefix + "_" + nameWithPrefixSuffix;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = nameWithPrefixSuffix + "_" + modelNameSuffix;
        }

        // Camelize name of nested properties
        modifiedName = camelize(nameWithPrefixSuffix);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(modifiedName)) {
            final String modelName = "Model" + modifiedName;
            System.out.println("WARN: " + modifiedName + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (modifiedName.matches("^\\d.*")) {
            final String modelName = "Model" + modifiedName; // e.g. 200Response => Model200Response (after camelize)
            System.out.println("WARN: " + name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        schemaKeyToModelNameCache.put(name, titleCase(modifiedName));
        return schemaKeyToModelNameCache.get(name);
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, servers);

        if (op.isResponseFile) {
            op.returnType = typeMapping.get("responseFile");
            op.imports.add(op.returnType);
        }

        op.vendorExtensions.put("originalParams", new ArrayList<>(op.allParams));
        for (var param : op.allParams) {
            param.vendorExtensions.put("hasMultipleParams", op.allParams.size() > 1);
        }
        op.vendorExtensions.put("originReturnProperty", op.returnProperty);
        processParametersWithAdditionalMappings(op.allParams, op.imports);
        processWithResponseBodyMapping(op);
        processOperationWithResponseWrappers(op);

        return op;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        String modified;
        if (value.isEmpty()) {
            modified = "EMPTY";
            return sanitizeKotlinSpecificNames(modified);
        }
        value = value.replaceAll("[^a-zA-Z0-9_]", "_");
        if (isNumeric(value)) {
            value = "_" + value;
        }
        return super.toEnumVarName(value, datatype);
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
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
            }
            newReturnType.dataType = typeName + '<' + originalReturnType + '>';
            newReturnType.items = op.returnProperty;
        }

        op.returnType = newReturnType.dataType;
        op.returnContainer = null;
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
        // Micronaut can't process correctly properties like `eTemperature`, when first symbol in lower case
        // and second symbol in upper case.
        // See this: https://github.com/micronaut-projects/micronaut-core/pull/10130
        if (varName.length() >= 2 && Character.isLowerCase(varName.charAt(0)) && Character.isUpperCase(varName.charAt(1))) {
            varName = "" + varName.charAt(0) + Character.toLowerCase(varName.charAt(1)) + varName.substring(2);
        }

        return varName;
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

            processParentModel(model, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars, false);

            var withInheritance = model.hasChildren || model.parent != null;
            model.vendorExtensions.put("withInheritance", withInheritance);

            var optionalVars = new ArrayList<CodegenProperty>();
            var requiredVars = new ArrayList<CodegenProperty>();
            for (var v : model.vars) {
                v.vendorExtensions.put("hasChildren", model.hasChildren);
                if (containsProp(v, requiredVarsWithoutDiscriminator)
                    || (!model.hasChildren || (v.required && !v.isDiscriminator))) {
                    if (!containsProp(v, requiredVars)) {
                        requiredVars.add(v);
                    }
                } else {
                    if (!containsProp(v, optionalVars)) {
                        optionalVars.add(v);
                    }
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
            model.vendorExtensions.put("withRequiredOrOptionalVars", !requiredVarsWithoutDiscriminator.isEmpty() || !optionalVars.isEmpty());
            model.vendorExtensions.put("optionalVars", optionalVars);
            model.vendorExtensions.put("serialId", random.nextLong());
            model.vendorExtensions.put("withRequiredVars", !model.requiredVars.isEmpty());
            if (model.discriminator != null) {
                model.vendorExtensions.put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
                model.discriminator.getVendorExtensions().put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
            }
            for (var property : model.vars) {
                processProperty(property, isServer, model, objs);
            }
            for (var property : model.requiredVars) {
                processProperty(property, isServer, model, objs);
            }
        }

        return objs;
    }

    private void processProperty(CodegenProperty property, boolean isServer, CodegenModel model, Map<String, ModelsMap> models) {

        property.vendorExtensions.put("withRequiredAndOptionalVars", model.vendorExtensions.get("withRequiredAndOptionalVars"));
        property.vendorExtensions.put("inRequiredArgsConstructor", !property.isReadOnly || isServer);
        property.vendorExtensions.put("isServer", isServer);
        property.vendorExtensions.put("defaultValueIsNotNull", property.defaultValue != null && !property.defaultValue.equals("null"));
        if ("null".equals(property.example)) {
            property.example = null;
        }
        if (useBeanValidation && (
            (!property.isContainer && property.isModel)
                || (property.getIsArray() && property.getComplexType() != null && models.containsKey(property.getComplexType()))
        )) {
            property.vendorExtensions.put("withValid", true);
        }

        processGenericAnnotations(property, useBeanValidation, false, property.isNullable || property.isDiscriminator,
            property.required, property.isReadOnly, true);
    }

    private void processParentModel(CodegenModel model, List<CodegenProperty> requiredVarsWithoutDiscriminator,
                                    List<CodegenProperty> requiredParentVarsWithoutDiscriminator,
                                    List<CodegenProperty> allVars,
                                    boolean processParentModel) {
        var parent = model.getParentModel();
        var hasParent = parent != null;

        allVars.addAll(model.vars);

        if (!processParentModel) {
            processVar(model, model.vars, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, processParentModel);
        }
        processVar(model, model.requiredVars, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, processParentModel);

        requiredParentVarsWithoutDiscriminator(model, requiredParentVarsWithoutDiscriminator);

        if (hasParent) {
            model.parentVars = parent.allVars;
        }
        if (hasParent) {
            processParentModel(parent, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars, true);
        }
    }

    private void processVar(CodegenModel model, List<CodegenProperty> vars, List<CodegenProperty> requiredVarsWithoutDiscriminator, List<CodegenProperty> requiredParentVarsWithoutDiscriminator, boolean processParentModel) {
        for (var v : vars) {
            boolean isDiscriminator = isDiscriminator(v, model);
            if (!isDiscriminator(v, model) && !containsProp(v, requiredVarsWithoutDiscriminator)) {
                if (v.isOverridden != null && !v.isOverridden && !v.vendorExtensions.containsKey("overriden") && !containsProp(v, vars)) {
                    v.isOverridden = true;
                    v.vendorExtensions.put("overridden", true);
                }
                v.vendorExtensions.put("hasChildren", model.hasChildren);
                if (model.parentModel == null || !containsProp(v, (List<CodegenProperty>) model.parentModel.vendorExtensions.get("requiredVarsWithoutDiscriminator"))) {
                    requiredVarsWithoutDiscriminator.add(v);
                }
                if (processParentModel && containsProp(v, (List<CodegenProperty>) model.vendorExtensions.get("requiredVarsWithoutDiscriminator"))) {
                    v.isOverridden = true;
                    v.vendorExtensions.put("overridden", true);
                }
            }
            v.isNullable = v.isNullable || (v.required && v.isReadOnly && isServer()) || !v.required;
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
        if (props == null) {
            return false;
        }
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
    public void setParameterExampleValue(CodegenParameter codegenParameter, Parameter parameter) {
        if (parameter.getExample() != null) {
            codegenParameter.example = parameter.getExample().toString();
            return;
        }

        if (parameter.getExamples() != null && !parameter.getExamples().isEmpty()) {
            Example example = parameter.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                codegenParameter.example = example.getValue().toString();
            }
        } else {

            Schema<?> schema = parameter.getSchema();
            if (schema != null && schema.getExample() != null) {
                codegenParameter.example = schema.getExample().toString();
            }
        }

        setParameterExampleValue(codegenParameter);
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        p.example = getParameterExampleValue(p);
    }

    protected String getParameterExampleValue(CodegenParameter p) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            p.requiredVars, false);
    }

    protected String getPropertyExampleValue(CodegenProperty p) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");
        var model = allModels.get(p.getDataType());

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            model != null ? model.requiredVars : null, true);
    }

    private boolean withExample(String example) {
        return example != null && !example.equals("null");
    }

    public String getExampleValue(
        String defaultValue, String example, String dataType, Boolean isModel, List<Object> allowableValues,
        String itemsType, String itemsExample, List<CodegenProperty> requiredVars, boolean isProperty
    ) {
        example = defaultValue != null ? defaultValue : example;
        String containerType = dataType == null ? null : dataType.split("<")[0];

        if ("String".equals(dataType)) {
            example = withExample(example) ? "\"" + escapeText(example) + "\"" : "\"example\"";
        } else if ("Int".equals(dataType) || "Short".equals(dataType)) {
            example = withExample(example) ? example : "56";
        } else if ("Long".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(withExample(example) ? example : "56", "L");
        } else if ("Float".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(withExample(example) ? example : "3.4", "F");
        } else if ("Double".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(withExample(example) ? example : "3.4", "D");
        } else if ("Boolean".equals(dataType)) {
            example = withExample(example) ? example : "false";
        } else if ("File".equals(dataType) || "java.io.File".equals(dataType)) {
            example = null;
        } else if ("OffsetDateTime".equals(dataType)) {
            example = "OffsetDateTime.of(2001, 2, 3, 12, 0, 0, 0, java.time.ZoneOffset.of(\"+02:00\"))";
        } else if ("LocalDate".equals(dataType)) {
            example = "LocalDate.of(2001, 2, 3)";
        } else if ("LocalDateTime".equals(dataType)) {
            example = "LocalDateTime.of(2001, 2, 3, 4, 5)";
        } else if ("ByteArray".equals(dataType)) {
            example = "ByteArray(10)";
        } else if ("BigDecimal".equals(dataType)) {
            example = "BigDecimal(78)";
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
                    var builder = new StringBuilder();
                    if (isProperty) {
                        dataType = importMapping.getOrDefault(dataType, modelPackage + '.' + dataType);
                    }
                    builder.append(dataType).append("(");
                    for (int i = 0; i < requiredVars.size(); ++i) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(getPropertyExampleValue(requiredVars.get(i)));
                    }
                    builder.append(")");
                    example = builder.toString();
                } else {
                    example = dataType + "()";
                }
            }
        }

        if ("List".equals(containerType)) {
            String innerExample;
            if ("String".equals(itemsType)) {
                itemsExample = itemsExample != null ? itemsExample : "example";
                innerExample = "\"" + escapeText(itemsExample) + "\"";
            } else {
                innerExample = itemsExample != null ? itemsExample : "";
            }

            if (StringUtils.isNotEmpty(innerExample)) {
                example = "listOf(" + innerExample + ")";
            } else {
                example = "listOf<" + itemsType + ">()";
            }
        } else if ("Set".equals(containerType)) {
            example = "HashSet<Any>()";
        } else if ("Map".equals(containerType)) {
            example = "HashMap<Any, Any>()";
        } else if (example == null) {
            example = "null";
        }

        return example;
    }

    @Override
    protected ImmutableMap.Builder<String, Mustache.Lambda> addMustacheLambdas() {
        return super.addMustacheLambdas()
            .put("replaceDotsWithUnderscore", new ReplaceDotsWithUnderscoreLambda());
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
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
        dateLibrary = name;
    }

    /**
     * Sanitize against Kotlin specific naming conventions, which may differ from those required by {@link DefaultCodegen#sanitizeName}.
     *
     * @param name string to be sanitize
     * @return sanitized string
     */
    private String sanitizeKotlinSpecificNames(final String name) {
        if (typeMapping.containsValue(name)) {
            return name;
        }
        String word = name;
        for (Map.Entry<String, String> specialCharacters : specialCharReplacements.entrySet()) {
            word = replaceSpecialCharacters(word, specialCharacters);
        }

        // Fallback, replace unknowns with underscore.
        word = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS).matcher(word).replaceAll("_");
        if (word.matches("\\d.*")) {
            word = "_" + word;
        }

        // _, __, and ___ are reserved in Kotlin. Treat all names with only underscores consistently, regardless of count.
        if (word.matches("^_*$")) {
            word = word.replaceAll("\\Q_\\E", "Underscore");
        }

        return word;
    }

    private String replaceSpecialCharacters(String word, Map.Entry<String, String> specialCharacters) {
        String specialChar = specialCharacters.getKey();
        String replacementChar = specialCharacters.getValue();
        // Underscore is the only special character we'll allow
        if (!specialChar.equals("_") && word.contains(specialChar)) {
            return replaceCharacters(word, specialChar, replacementChar);
        }
        return word;
    }

    private String replaceCharacters(String word, String oldValue, String newValue) {
        if (!word.contains(oldValue)) {
            return word;
        }
        if (word.equals(oldValue)) {
            return newValue;
        }
        int i = word.indexOf(oldValue);
        String start = word.substring(0, i);
        String end = recurseOnEndOfWord(word, oldValue, newValue, i);
        return start + newValue + end;
    }

    private String recurseOnEndOfWord(String word, String oldValue, String newValue, int lastReplacedValue) {
        String end = word.substring(lastReplacedValue + 1);
        if (!end.isEmpty()) {
            end = titleCase(end);
            end = replaceCharacters(end, oldValue, newValue);
        }
        return end;
    }

    private String titleCase(final String input) {
        return input.substring(0, 1).toUpperCase(Locale.ROOT) + input.substring(1);
    }
}
