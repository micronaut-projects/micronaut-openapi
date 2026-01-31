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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;
import io.micronaut.openapi.generator.Formatting.ReplaceDotsWithUnderscoreLambda;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.evo.inflector.English;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenDiscriminator;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenModelFactory;
import org.openapitools.codegen.CodegenModelType;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.IJsonSchemaValidationProperties;
import org.openapitools.codegen.VendorExtension;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_INT16;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_INT8;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_LONG;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_SHORT;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_BYTE;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_CHAR;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_CHARACTER;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_DOUBLE;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_FLOAT;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_INT;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_LONG;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_SHORT;
import static io.micronaut.openapi.generator.Utils.DEFAULT_BODY_PARAM_NAME;
import static io.micronaut.openapi.generator.Utils.DIVIDE_OPERATIONS_BY_CONTENT_TYPE;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_CLASS;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_FIELD;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_OPERATION;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_SETTER;
import static io.micronaut.openapi.generator.Utils.NULL_STRING;
import static io.micronaut.openapi.generator.Utils.addEnumParamsForConverters;
import static io.micronaut.openapi.generator.Utils.addStrValueToEnum;
import static io.micronaut.openapi.generator.Utils.calcQueryValueFormat;
import static io.micronaut.openapi.generator.Utils.findEnumVar;
import static io.micronaut.openapi.generator.Utils.isDateType;
import static io.micronaut.openapi.generator.Utils.normalizeExtraAnnotations;
import static io.micronaut.openapi.generator.Utils.processDuplicateVars;
import static io.micronaut.openapi.generator.Utils.processEnumExt;
import static io.micronaut.openapi.generator.Utils.processGenericAnnotations;
import static io.micronaut.openapi.generator.Utils.readListOfStringsProperty;
import static io.swagger.v3.parser.util.SchemaTypeUtil.BYTE_FORMAT;
import static io.swagger.v3.parser.util.SchemaTypeUtil.DOUBLE_FORMAT;
import static io.swagger.v3.parser.util.SchemaTypeUtil.FLOAT_FORMAT;
import static io.swagger.v3.parser.util.SchemaTypeUtil.INTEGER64_FORMAT;
import static io.swagger.v3.parser.util.SchemaTypeUtil.INTEGER_TYPE;
import static org.openapitools.codegen.CodegenConstants.API_PACKAGE;
import static org.openapitools.codegen.CodegenConstants.INVOKER_PACKAGE;
import static org.openapitools.codegen.CodegenConstants.MODEL_PACKAGE;
import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.OnceLogger.once;
import static org.openapitools.codegen.utils.StringUtils.camelize;
import static org.openapitools.codegen.utils.StringUtils.underscore;

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
    public static final String OPT_GENERATE_ENUM_CONVERTERS = "generateEnumConverters";
    public static final String OPT_NO_ARGS_CONSTRUCTOR = "noArgsConstructor";
    public static final String OPT_USE_PLURAL = "plural";
    public static final String OPT_FLUX_FOR_ARRAYS = "fluxForArrays";
    public static final String OPT_GENERATED_ANNOTATION = "generatedAnnotation";
    public static final String OPT_VISITABLE = "visitable";
    public static final String OPT_DATE_LIBRARY_ZONED_DATETIME = "ZONED_DATETIME";
    public static final String OPT_DATE_LIBRARY_OFFSET_DATETIME = "OFFSET_DATETIME";
    public static final String OPT_DATE_LIBRARY_LOCAL_DATETIME = "LOCAL_DATETIME";
    public static final String OPT_DATE_FORMAT = "dateFormat";
    public static final String OPT_DATE_TIME_FORMAT = "dateTimeFormat";
    public static final String OPT_USE_ENUM_CASE_INSENSITIVE = "useEnumCaseInsensitive";
    public static final String OPT_REACTIVE = "reactive";
    public static final String OPT_USE_SEALED = "useSealed";
    public static final String OPT_USE_TAGS = "useTags";
    public static final String OPT_GENERATE_HTTP_RESPONSE_ALWAYS = "generateHttpResponseAlways";
    public static final String OPT_GENERATE_CONTROLLER_AS_ABSTRACT = "generateControllerAsAbstract";
    public static final String OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED = "generateHttpResponseWhereRequired";
    public static final String OPT_APPLICATION_NAME = "applicationName";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS = "generateSwaggerAnnotations";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1 = "swagger1";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2 = "swagger2";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE = "true";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE = "false";
    public static final String OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG = "generateOperationOnlyForFirstTag";
    public static final String OPT_SKIP_SORTING_OPERATIONS = "skipSortingOperations";
    public static final String OPT_JSON_INCLUDE_ALWAYS_FOR_REQUIRED_FIELDS = "jsonIncludeAlwaysForRequiredFields";
    public static final String CONTENT_TYPE_APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String CONTENT_TYPE_ANY = "*/*";

    private static final String MONO_CLASS_NAME = "reactor.core.publisher.Mono";
    private static final String FLUX_CLASS_NAME = "reactor.core.publisher.Flux";

    protected SecureRandom random = new SecureRandom();
    protected String title;
    protected boolean useOptional;
    protected boolean visitable;
    protected boolean lombok;
    protected boolean generateEnumConverters = true;
    protected boolean noArgsConstructor;
    protected boolean fluxForArrays;
    protected boolean useTags = true;
    protected boolean plural = true;
    protected boolean generatedAnnotation = true;
    protected String testTool;
    protected boolean requiredPropertiesInConstructor = true;
    protected boolean reactive;
    protected boolean useSealed;
    protected boolean useEnumCaseInsensitive;
    protected boolean generateHttpResponseAlways;
    protected boolean generateHttpResponseWhereRequired = true;
    protected boolean generateControllerAsAbstract;
    protected boolean jsonIncludeAlwaysForRequiredFields;
    protected String appName;
    protected String dateFormat;
    protected String dateTimeFormat;
    protected String generateSwaggerAnnotations;
    protected boolean generateOperationOnlyForFirstTag;
    protected String serializationLibrary = SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name();
    protected List<ParameterMapping> parameterMappings = new ArrayList<>();
    protected List<ResponseBodyMapping> responseBodyMappings = new ArrayList<>();
    protected Map<String, CodegenModel> allModels = new HashMap<>();
    // simple class name -> full qualified class name
    protected Map<String, String> importsToDrop = new HashMap<>();

    private final Logger log = LoggerFactory.getLogger(getClass());

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
        useOneOfInterfaces = true;
        inlineSchemaOption.put("RESOLVE_INLINE_ENUMS", "true");
        // CHECKSTYLE:ON

        languageSpecificPrimitives.addAll(Set.of(
            "char",
            "float",
            "double",
            "byte",
            "short",
            "int",
            "long",
            "Byte",
            "Short",
            "Character"
        ));

        typeMapping.put("char", "Character");
        typeMapping.put("byte", "Byte");

        GlobalSettings.setProperty(DIVIDE_OPERATIONS_BY_CONTENT_TYPE, "true");

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
                SecurityFeature.OpenIDConnect,
                SecurityFeature.SignatureAuth,
                SecurityFeature.AWSV4Signature
            ))
        );

        // Set additional properties
        additionalProperties.put("useOneOfInterfaces", useOneOfInterfaces);
        additionalProperties.put("openbrace", "{");
        additionalProperties.put("closebrace", "}");

        // Set client options that will be presented to user
        updateOption(INVOKER_PACKAGE, getInvokerPackage());
        updateOption(CodegenConstants.ARTIFACT_ID, getArtifactId());
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(MODEL_PACKAGE, modelPackage);

        cliOptions.add(new CliOption(OPT_TITLE, "Client service name").defaultValue(title));
        cliOptions.add(new CliOption(OPT_APPLICATION_NAME, "Micronaut application name (Defaults to the " + CodegenConstants.ARTIFACT_ID + " value)").defaultValue(appName));
        cliOptions.add(CliOption.newBoolean(OPT_USE_LOMBOK, "Whether or not to use lombok annotations in generated code", lombok));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_ENUM_CONVERTERS, "Generate or not custom enum converters for enum query values and path variables", generateEnumConverters));
        cliOptions.add(CliOption.newBoolean(OPT_NO_ARGS_CONSTRUCTOR, "Generate or not no-args constructor for each POJO", noArgsConstructor));
        cliOptions.add(CliOption.newBoolean(OPT_USE_PLURAL, "Whether or not to use plural for request body parameter name", plural));
        cliOptions.add(CliOption.newBoolean(OPT_FLUX_FOR_ARRAYS, "Whether or not to use Flux<?> instead Mono<List<?>> for arrays in generated code", fluxForArrays));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATED_ANNOTATION, "Generate code with \"@Generated\" annotation", generatedAnnotation));
        cliOptions.add(CliOption.newBoolean(OPT_USE_TAGS, "Whether to use tags for creating interface and controller class names", useTags));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations", useBeanValidation));
        cliOptions.add(CliOption.newBoolean(USE_OPTIONAL, "Use Optional container for optional parameters", useOptional));
        cliOptions.add(CliOption.newBoolean(OPT_VISITABLE, "Generate visitor for subtypes with a discriminator", visitable));
        cliOptions.add(CliOption.newBoolean(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "Allow only to create models with all the required properties provided in constructor", requiredPropertiesInConstructor));
        cliOptions.add(CliOption.newBoolean(OPT_REACTIVE, "Make the responses use Reactor Mono as wrapper", reactive));
        cliOptions.add(CliOption.newBoolean(OPT_USE_SEALED, "Whether to generate sealed model interfaces and classes", useSealed));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "Always wrap the operations response in HttpResponse object", generateHttpResponseAlways));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_CONTROLLER_AS_ABSTRACT, "If true, then controller interface will be without @Controller annotation", generateControllerAsAbstract));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, "Wrap the operations response in HttpResponse object where non-200 HTTP status codes or additional headers are defined", generateHttpResponseWhereRequired));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "When false, the operation method will be duplicated in each of the tags if multiple tags are assigned to this operation. " +
            "If true, each operation will be generated only once in the first assigned tag.", generateOperationOnlyForFirstTag));
        cliOptions.add(CliOption.newBoolean(OPT_USE_ENUM_CASE_INSENSITIVE, "Use `equalsIgnoreCase` when String for enum comparison", useEnumCaseInsensitive));
        cliOptions.add(CliOption.newBoolean(OPT_JSON_INCLUDE_ALWAYS_FOR_REQUIRED_FIELDS, "If set to true, @JsonInclude annotation will be with value ALWAYS for required properties in POJO's", jsonIncludeAlwaysForRequiredFields));

        var testToolOption = new CliOption(OPT_TEST, "Specify which test tool to generate files for").defaultValue(testTool);
        var testToolOptionMap = new HashMap<String, String>();
        testToolOptionMap.put(OPT_TEST_JUNIT, "Use JUnit as test tool");
        testToolOptionMap.put(OPT_TEST_SPOCK, "Use Spock as test tool");
        testToolOption.setEnum(testToolOptionMap);
        cliOptions.add(testToolOption);

        var generateSwaggerAnnotationsOption = new CliOption(OPT_GENERATE_SWAGGER_ANNOTATIONS, "Specify if you want to generate swagger annotations and which version").defaultValue(generateSwaggerAnnotations);
        var generateSwaggerAnnotationsOptionMap = new HashMap<String, String>();
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2, "Use io.swagger.core.v3:swagger-annotations for annotating operations and schemas");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE, "Equivalent to \"" + OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2 + "\"");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE, "Do not generate swagger annotations");
        generateSwaggerAnnotationsOption.setEnum(generateSwaggerAnnotationsOptionMap);
        cliOptions.add(generateSwaggerAnnotationsOption);

        cliOptions.add(new CliOption(OPT_DATE_FORMAT, "Specify the format pattern of date as a string"));
        cliOptions.add(new CliOption(OPT_DATE_TIME_FORMAT, "Specify the format pattern of date-time as a string"));

        // Modify the DATE_LIBRARY option to only have supported values
        cliOptions.stream()
            .filter(o -> o.getOpt().equals(DATE_LIBRARY))
            .findFirst()
            .ifPresent(opt -> {
                var valuesEnum = new HashMap<String, String>();
                valuesEnum.put(OPT_DATE_LIBRARY_OFFSET_DATETIME, opt.getEnum().get(OPT_DATE_LIBRARY_OFFSET_DATETIME));
                valuesEnum.put(OPT_DATE_LIBRARY_LOCAL_DATETIME, opt.getEnum().get(OPT_DATE_LIBRARY_LOCAL_DATETIME));
                opt.setEnum(valuesEnum);
            });

        final CliOption serializationLibraryOpt = CliOption.newString(CodegenConstants.SERIALIZATION_LIBRARY, "Serialization library for model");
        serializationLibraryOpt.defaultValue(SerializationLibraryKind.JACKSON.name());
        var serializationLibraryOptions = new HashMap<String, String>();
        serializationLibraryOptions.put(SerializationLibraryKind.JACKSON.name(), "Jackson as serialization library");
        serializationLibraryOptions.put(SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name(), "Use micronaut-serialization with Jackson annotations");
        serializationLibraryOpt.setEnum(serializationLibraryOptions);
        cliOptions.add(serializationLibraryOpt);

        // Add reserved words
        var micronautReservedWords = List.of(
            // lost words in openapi generator
            "true", "false",
            // special words
            "Object", "List", "File", "OffsetDateTime", "LocalDate", "LocalTime",
            "Client", "Format", "QueryValue", "QueryParam", "PathVariable", "Header", "Cookie",
            "Authorization", "Body", "Application"
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
        importMapping.put("MultiValuesConverterFactory", "static io.micronaut.core.convert.converters.MultiValuesConverterFactory.*");
    }

    public void setGenerateHttpResponseAlways(boolean generateHttpResponseAlways) {
        this.generateHttpResponseAlways = generateHttpResponseAlways;
    }

    public void setGenerateControllerAsAbstract(boolean generateControllerAsAbstract) {
        this.generateControllerAsAbstract = generateControllerAsAbstract;
    }

    public void setGenerateHttpResponseWhereRequired(boolean generateHttpResponseWhereRequired) {
        this.generateHttpResponseWhereRequired = generateHttpResponseWhereRequired;
    }

    public void setReactive(boolean reactive) {
        this.reactive = reactive;
    }

    public void setUseSealed(boolean useSealed) {
        this.useSealed = useSealed;
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
        updateOption(MODEL_PACKAGE, modelPackage);
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

    public void setGenerateEnumConverters(boolean generateEnumConverters) {
        this.generateEnumConverters = generateEnumConverters;
    }

    public void setUseTags(boolean useTags) {
        this.useTags = useTags;
    }

    public void setGenerateOperationOnlyForFirstTag(boolean generateOperationOnlyForFirstTag) {
        this.generateOperationOnlyForFirstTag = generateOperationOnlyForFirstTag;
    }

    public void setNoArgsConstructor(boolean noArgsConstructor) {
        this.noArgsConstructor = noArgsConstructor;
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

        // need it to add ability to set List<String> in `additionalModelTypeAnnotations` property
        if (additionalProperties.containsKey(ADDITIONAL_MODEL_TYPE_ANNOTATIONS)) {
            setAdditionalModelTypeAnnotations(readListOfStringsProperty(ADDITIONAL_MODEL_TYPE_ANNOTATIONS, additionalProperties));
            additionalProperties.remove(ADDITIONAL_MODEL_TYPE_ANNOTATIONS);
        }
        if (additionalProperties.containsKey(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS)) {
            setAdditionalOneOfTypeAnnotations(readListOfStringsProperty(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS, additionalProperties));
            additionalProperties.remove(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS);
        }
        if (additionalProperties.containsKey(ADDITIONAL_ENUM_TYPE_ANNOTATIONS)) {
            setAdditionalEnumTypeAnnotations(readListOfStringsProperty(ADDITIONAL_ENUM_TYPE_ANNOTATIONS, additionalProperties));
            additionalProperties.remove(ADDITIONAL_ENUM_TYPE_ANNOTATIONS);
        }

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

        if (additionalProperties.containsKey(API_PACKAGE)) {
            apiPackage = (String) additionalProperties.get(API_PACKAGE);
        } else {
            additionalProperties.put(API_PACKAGE, apiPackage);
        }

        if (additionalProperties.containsKey(MODEL_PACKAGE)) {
            modelPackage = (String) additionalProperties.get(MODEL_PACKAGE);
        } else {
            additionalProperties.put(MODEL_PACKAGE, modelPackage);
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

        if (additionalProperties.containsKey(OPT_USE_TAGS)) {
            useTags = convertPropertyToBoolean(OPT_USE_TAGS);
        }
        writePropertyBack(OPT_USE_TAGS, useTags);

        if (additionalProperties.containsKey(OPT_JSON_INCLUDE_ALWAYS_FOR_REQUIRED_FIELDS)) {
            jsonIncludeAlwaysForRequiredFields = convertPropertyToBoolean(OPT_JSON_INCLUDE_ALWAYS_FOR_REQUIRED_FIELDS);
        }
        writePropertyBack(OPT_JSON_INCLUDE_ALWAYS_FOR_REQUIRED_FIELDS, jsonIncludeAlwaysForRequiredFields);

        if (additionalProperties.containsKey(OPT_USE_LOMBOK)) {
            lombok = convertPropertyToBoolean(OPT_USE_LOMBOK);
        }
        writePropertyBack(OPT_USE_LOMBOK, lombok);

        if (additionalProperties.containsKey(OPT_GENERATE_ENUM_CONVERTERS)) {
            generateEnumConverters = convertPropertyToBoolean(OPT_GENERATE_ENUM_CONVERTERS);
        }
        writePropertyBack(OPT_GENERATE_ENUM_CONVERTERS, generateEnumConverters);

        if (additionalProperties.containsKey(OPT_NO_ARGS_CONSTRUCTOR)) {
            noArgsConstructor = convertPropertyToBoolean(OPT_NO_ARGS_CONSTRUCTOR);
        }
        writePropertyBack(OPT_NO_ARGS_CONSTRUCTOR, noArgsConstructor);

        if (additionalProperties.containsKey(OPT_USE_PLURAL)) {
            plural = convertPropertyToBoolean(OPT_USE_PLURAL);
        }
        writePropertyBack(OPT_USE_PLURAL, plural);

        if (additionalProperties.containsKey(OPT_FLUX_FOR_ARRAYS)) {
            fluxForArrays = convertPropertyToBoolean(OPT_FLUX_FOR_ARRAYS);
        }
        writePropertyBack(OPT_FLUX_FOR_ARRAYS, fluxForArrays);

        if (additionalProperties.containsKey(OPT_USE_ENUM_CASE_INSENSITIVE)) {
            useEnumCaseInsensitive = convertPropertyToBoolean(OPT_USE_ENUM_CASE_INSENSITIVE);
        }
        writePropertyBack(OPT_USE_ENUM_CASE_INSENSITIVE, useEnumCaseInsensitive);

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

        if (additionalProperties.containsKey(OPT_USE_SEALED)) {
            useSealed = convertPropertyToBoolean(OPT_USE_SEALED);
        }
        writePropertyBack(OPT_USE_SEALED, useSealed);

        if (additionalProperties.containsKey(OPT_DATE_FORMAT)) {
            dateFormat = (String) additionalProperties.get(OPT_DATE_FORMAT);
        }
        writePropertyBack(OPT_DATE_FORMAT, dateFormat);

        if (additionalProperties.containsKey(OPT_DATE_TIME_FORMAT)) {
            dateTimeFormat = (String) additionalProperties.get(OPT_DATE_TIME_FORMAT);
        }
        writePropertyBack(OPT_DATE_TIME_FORMAT, dateTimeFormat);

        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_ALWAYS)) {
            generateHttpResponseAlways = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, generateHttpResponseAlways);

        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_ALWAYS)) {
            generateHttpResponseAlways = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, generateHttpResponseAlways);

        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED)) {
            generateHttpResponseWhereRequired = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, generateHttpResponseWhereRequired);

        if (additionalProperties.containsKey(OPT_GENERATE_CONTROLLER_AS_ABSTRACT)) {
            generateControllerAsAbstract = convertPropertyToBoolean(OPT_GENERATE_CONTROLLER_AS_ABSTRACT);
        }
        writePropertyBack(OPT_GENERATE_CONTROLLER_AS_ABSTRACT, generateControllerAsAbstract);

        if (additionalProperties.containsKey(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG)) {
            generateOperationOnlyForFirstTag = convertPropertyToBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG);
        }
        writePropertyBack(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, generateOperationOnlyForFirstTag);

        if (additionalProperties.containsKey(USE_JAKARTA_EE)) {
            setUseJakartaEe(convertPropertyToBoolean(USE_JAKARTA_EE));
        }
        writePropertyBack(USE_JAKARTA_EE, useJakartaEe);
        writePropertyBack(JAVAX_PACKAGE, useJakartaEe ? "jakarta" : "javax");

        convertPropertyToBooleanAndWriteBack(OPT_SKIP_SORTING_OPERATIONS, this::setSkipSortingOperations);

        maybeSetTestTool();
        writePropertyBack(OPT_TEST, testTool);
        if (testTool.equals(OPT_TEST_JUNIT)) {
            additionalProperties.put("isTestJunit", true);
        } else if (testTool.equals(OPT_TEST_SPOCK)) {
            additionalProperties.put("isTestSpock", true);
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

        // Use the default java time
        switch (dateLibrary) {
            case OPT_DATE_LIBRARY_OFFSET_DATETIME -> {
                typeMapping.put("DateTime", "OffsetDateTime");
                typeMapping.put("date-time", "OffsetDateTime");
                typeMapping.put("date", "LocalDate");
                importMapping.put("DateTime", "java.time.OffsetDateTime");
                importMapping.put("date-time", "java.time.OffsetDateTime");
                importMapping.put("date", "java.time.LocalDate");
            }
            case OPT_DATE_LIBRARY_ZONED_DATETIME -> {
                typeMapping.put("DateTime", "ZonedDateTime");
                typeMapping.put("date-time", "ZonedDateTime");
                typeMapping.put("date", "LocalDate");
                importMapping.put("DateTime", "java.time.ZonedDateTime");
                importMapping.put("date-time", "java.time.ZonedDateTime");
                importMapping.put("date", "java.time.LocalDate");
            }
            case OPT_DATE_LIBRARY_LOCAL_DATETIME -> {
                typeMapping.put("DateTime", "LocalDateTime");
                typeMapping.put("date-time", "LocalDateTime");
                typeMapping.put("date", "LocalDate");
                importMapping.put("DateTime", "java.time.LocalDateTime");
                importMapping.put("date-time", "java.time.LocalDateTime");
                importMapping.put("date", "java.time.LocalDate");
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
        String resourceFolder = projectFolder + "/resources";
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
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_1, OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2, OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE -> generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2;
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE -> generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE;
                default -> throw new RuntimeException("Value \"" + value + "\" for the " + OPT_GENERATE_SWAGGER_ANNOTATIONS + " parameter is unsupported or misspelled");
            }
        }
    }

    private void maybeSetTestTool() {
        if (additionalProperties.containsKey(OPT_TEST)) {
            switch ((String) additionalProperties.get(OPT_TEST)) {
                case OPT_TEST_JUNIT, OPT_TEST_SPOCK -> testTool = (String) additionalProperties.get(OPT_TEST);
                default -> throw new RuntimeException("Test tool \"" + additionalProperties.get(OPT_TEST) + "\" is not supported or misspelled.");
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
        return testFileFolder() + apiPackage().replace(".", "/");
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
    public CodegenParameter fromRequestBody(RequestBody body, Set<String> imports, String bodyParameterName) {
        var rqBody = super.fromRequestBody(body, imports, bodyParameterName);

        var rqBodySchema = body.getContent() != null && !body.getContent().isEmpty() ? body.getContent().entrySet().iterator().next().getValue().getSchema() : null;
        CodegenProperty codegenProperty = fromProperty(bodyParameterName, rqBodySchema, false);

        if (rqBodySchema != null) {
            rqBodySchema = unaliasSchema(rqBodySchema);
            var isRequiredBody = body.getRequired() != null && body.getRequired();
            if (getUseInlineModelResolver()) {
                codegenProperty = fromProperty(bodyParameterName, getReferencedSchemaWhenNotEnum(rqBodySchema), isRequiredBody);
            } else {
                codegenProperty = fromProperty(bodyParameterName, rqBodySchema, isRequiredBody);
            }
            rqBody.setSchema(codegenProperty);
        }

        if (Boolean.TRUE.equals(codegenProperty.isModel)) {
            rqBody.isModel = true;
        }

        rqBody.dataFormat = codegenProperty.dataFormat;
        if (body.getRequired() != null) {
            rqBody.required = body.getRequired();
        }

        // set containerType
        rqBody.containerType = codegenProperty.containerType;
        rqBody.containerTypeMapped = codegenProperty.containerTypeMapped;

        // enum
        updateCodegenPropertyEnum(codegenProperty);
        rqBody.isEnum = codegenProperty.isEnum;
        rqBody.isEnumRef = codegenProperty.isEnumRef;
        rqBody._enum = codegenProperty._enum;
        rqBody.allowableValues = codegenProperty.allowableValues;

        if (codegenProperty.isEnum || codegenProperty.isEnumRef) {
            rqBody.datatypeWithEnum = codegenProperty.datatypeWithEnum;
            rqBody.enumName = codegenProperty.enumName;
            if (codegenProperty.defaultValue != null) {
                rqBody.enumDefaultValue = codegenProperty.defaultValue.replace(codegenProperty.enumName + ".", "");
            }
        }
        return rqBody;
    }

    private Schema getReferencedSchemaWhenNotEnum(Schema parameterSchema) {
        Schema referencedSchema = ModelUtils.getReferencedSchema(openAPI, parameterSchema);
        if (referencedSchema.getEnum() != null && !referencedSchema.getEnum().isEmpty()) {
            referencedSchema = parameterSchema;
        }
        return referencedSchema;
    }

    @Override
    public CodegenParameter fromParameter(Parameter p, Set<String> imports) {
        var parameter = super.fromParameter(p, imports);

        if (parameter.isPathParam && !parameter.required) {
            parameter.required = true;
        }

        checkPrimitives(parameter, unaliasSchema(p.getSchema()));
        // if name is escaped
        var realName = parameter.paramName;
        if (realName.startsWith("_") && !parameter.baseName.startsWith("_") && isReservedWord(parameter.baseName)) {
            realName = realName.replaceFirst("_", "");
        }
        parameter.vendorExtensions.put("realName", realName);

        Schema parameterSchema;
        if (p.getSchema() != null) {
            parameterSchema = unaliasSchema(p.getSchema());
        } else if (p.getContent() != null) {
            Content content = p.getContent();
            if (content.size() > 1) {
                once(log).warn("Multiple schemas found in content, returning only the first one");
            }
            Map.Entry<String, MediaType> entry = content.entrySet().iterator().next();
            parameterSchema = entry.getValue().getSchema();
        } else {
            parameterSchema = null;
        }
        if (parameterSchema != null && parameterSchema.get$ref() != null) {
            parameterSchema = openAPI.getComponents().getSchemas().get(parameterSchema.get$ref().substring("#/components/schemas/".length()));
        }

        String defaultValueInit;
        var items = parameter.items;
        if (items == null) {
            defaultValueInit = calcDefaultValues(null, null, false, false,
                false, false, false, false, false, parameterSchema).getLeft();
        } else {
            defaultValueInit = calcDefaultValues(items.datatypeWithEnum, items.dataType, items.getIsEnumOrRef(),
                parameter.isArray, items.isString, items.isNumeric, items.isFloat, items.isMap, items.isNullable,
                parameterSchema).getLeft();
        }
        if (parameterSchema != null && ModelUtils.isEnumSchema(parameterSchema)) {
            defaultValueInit = parameter.dataType + ".fromValue(" + defaultValueInit + ")";
        }
        if (defaultValueInit != null) {
            parameter.vendorExtensions.put("defaultValueInit", defaultValueInit);
        }

        addStrValueToEnum(parameter.items);

        return parameter;
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        var resp = super.fromResponse(responseCode, response);
        checkPrimitives(resp, (Schema) resp.schema);
        return resp;
    }

    @Override
    public CodegenProperty fromProperty(String name, Schema schema, boolean required, boolean schemaIsFromAdditionalProperties) {
        var property = super.fromProperty(name, schema, required, schemaIsFromAdditionalProperties);
        checkPrimitives(property, unaliasSchema(schema));

        // if name is escaped
        var realName = property.name;
        if (realName.startsWith("_") && !property.baseName.startsWith("_") && isReservedWord(property.baseName)) {
            realName = realName.replaceFirst("_", "");
            property.nameInPascalCase = camelize(realName);
            property.nameInCamelCase = camelize(realName, LOWERCASE_FIRST_LETTER);
            property.nameInSnakeCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, property.nameInCamelCase);
            // fix for getters and setters for escaped vars as reserved words and started with '_', like this: '_for'
            if (!property.getter.startsWith("get_")) {
                property.getter = "get" + getterAndSetterCapitalize(property.name);
            }
            if (!property.setter.startsWith("set_")) {
                property.setter = "set" + getterAndSetterCapitalize(property.name);
            }
        }
        property.vendorExtensions.put("realName", realName);

        if (schema != null && schema.get$ref() != null) {
            var refSchema = ModelUtils.getSchemaFromRefToSchemaWithProperties(openAPI, schema.get$ref());
            if (refSchema == null) {
                refSchema = ModelUtils.getReferencedSchema(openAPI, schema);
            }
            schema = refSchema;
        }

        String defaultValueInit;
        var items = property.items;
        if (items == null) {
            defaultValueInit = calcDefaultValues(null, null, false, false,
                false, false, false, false, false, schema).getLeft();
        } else {
            defaultValueInit = calcDefaultValues(items.datatypeWithEnum, items.dataType, items.getIsEnumOrRef(),
                property.isArray, items.isString, items.isNumeric, items.isFloat, items.isMap, items.isNullable,
                schema).getLeft();
        }
        if (schema != null && ModelUtils.isEnumSchema(schema)) {
            var enumVarName = toEnumVarName(property.defaultValue, property.dataType);
            if (enumVarName != null) {
                defaultValueInit = property.dataType + "." + enumVarName;
            } else {
                defaultValueInit = null;
            }
        }
        if (defaultValueInit != null) {
            property.vendorExtensions.put("defaultValueInit", defaultValueInit);
        }

        return property;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (value == null) {
            return null;
        }
        if (enumNameMapping.containsKey(value)) {
            return enumNameMapping.get(value);
        }

        if (value.isEmpty()) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(value) != null) {
            return getSymbolName(value).toUpperCase(Locale.ROOT);
        }

        if (" ".equals(value)) {
            return "SPACE";
        }

        // number
        if ("Int".equalsIgnoreCase(datatype)
            || "Byte".equalsIgnoreCase(datatype)
            || "Short".equalsIgnoreCase(datatype)
            || "Integer".equalsIgnoreCase(datatype)
            || "Long".equalsIgnoreCase(datatype)
            || "Float".equalsIgnoreCase(datatype)
            || "Double".equalsIgnoreCase(datatype)
            || "BigDecimal".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String var = underscore(value.replaceAll("\\W+", "_")).toUpperCase(Locale.ROOT);
        if (var.matches("\\d.*")) {
            var = "_" + var;
        }
        return this.toVarName(var);
    }

    @Override
    public String toDefaultValue(CodegenProperty cp, Schema schema) {

        if (cp.items != null) {
            return calcDefaultValues(cp.items.datatypeWithEnum, cp.items.dataType, cp.items.getIsEnumOrRef(),
                cp.isArray, cp.items.isString, cp.items.isNumeric, cp.items.isFloat, cp.items.isMap, cp.items.isNullable,
                schema).getRight();
        } else {
            return calcDefaultValues(null, null, false,
                false, false, false, false, false, false, schema).getRight();
        }
    }

    private Pair<String, String> calcDefaultValues(String itemsDatatypeWithEnum, String itemsDataType, boolean itemsIsEnumOrRef,
                                                   boolean isArray, boolean itemsIsString, boolean itemsIsNumeric,
                                                   boolean itemsIsFloat, boolean itemsIsMap, boolean isNullable, Schema schema) {

        String defaultValueInit = null;
        String defaultValueStr = null;

        schema = ModelUtils.getReferencedSchema(this.openAPI, schema);
        if (ModelUtils.isArraySchema(schema)) {
            if (schema.getDefault() == null) {
                // nullable or containerDefaultToNull set to true
                if (isNullable || containerDefaultToNull) {
                    return Pair.of(null, null);
                }
                return getDefaultCollectionType(schema);
            }
            return arrayDefaultValue(itemsDatatypeWithEnum, itemsDataType, itemsIsEnumOrRef,
                isArray, itemsIsString, itemsIsNumeric, itemsIsFloat, itemsIsMap, schema);
        } else if (ModelUtils.isMapSchema(schema) && !(ModelUtils.isComposedSchema(schema))) {
            if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                // object is complex object with free-form additional properties
                if (schema.getDefault() != null) {
                    defaultValueInit = super.toDefaultValue(schema);
                    defaultValueStr = super.toDefaultValue(schema);
                }
            }

            // nullable or containerDefaultToNull set to true
            if (isNullable || containerDefaultToNull) {
                return Pair.of(null, null);
            }

            if (ModelUtils.getAdditionalProperties(schema) == null) {
                return Pair.of(null, null);
            }

            defaultValueInit = String.format(Locale.ROOT, "new %s<>()", instantiationTypes().getOrDefault("map", "HashMap"));
            defaultValueStr = null;
        } else if (ModelUtils.isIntegerSchema(schema)) {
            if (schema.getDefault() != null) {
                if (INTEGER64_FORMAT.equals(schema.getFormat())) {
                    defaultValueInit = schema.getDefault().toString() + "L";
                } else {
                    defaultValueInit = schema.getDefault().toString();
                }
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isNumberSchema(schema)) {
            if (schema.getDefault() != null) {
                if (FLOAT_FORMAT.equals(schema.getFormat())) {
                    defaultValueInit = schema.getDefault().toString() + "F";
                } else if (SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat())) {
                    defaultValueInit = schema.getDefault().toString() + "D";
                } else {
                    defaultValueInit = "new BigDecimal(\"" + schema.getDefault().toString() + "\")";
                }
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isBooleanSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = schema.getDefault().toString();
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isDateSchema(schema)) {
            if (schema.getDefault() != null) {
                var date = (Date) schema.getDefault();
                LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                defaultValueInit = "LocalDate.parse(\"%s\")".formatted(localDate.toString());
                defaultValueStr = localDate.toString();
            }
        } else if (ModelUtils.isDateTimeSchema(schema)) {
            if (schema.getDefault() != null) {
                var offsetDateTime = (OffsetDateTime) schema.getDefault();
                switch (dateLibrary) {
                    case OPT_DATE_LIBRARY_OFFSET_DATETIME -> {
                        defaultValueInit = "OffsetDateTime.parse(\"%s\")".formatted(offsetDateTime);
                        defaultValueStr = offsetDateTime.toString();
                    }
                    case OPT_DATE_LIBRARY_ZONED_DATETIME -> {
                        var zonedDateTime = offsetDateTime.toZonedDateTime();
                        defaultValueInit = "ZonedDateTime.parse(\"%s\")".formatted(zonedDateTime);
                        defaultValueStr = zonedDateTime.toString();
                    }
                    case OPT_DATE_LIBRARY_LOCAL_DATETIME -> {
                        var localDateTime = offsetDateTime.toLocalDateTime();
                        defaultValueInit = "LocalDateTime.parse(\"%s\")".formatted(localDateTime);
                        defaultValueStr = localDateTime.toString();
                    }
                    default -> {
                    }
                }
            }
        } else if (ModelUtils.isURISchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = "URI.create(\"" + escapeText(String.valueOf(schema.getDefault())) + "\")";
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isUUIDSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = "UUID.fromString(\"" + schema.getDefault() + "\")";
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isStringSchema(schema)) {
            if (schema.getDefault() != null) {
                String def = schema.getDefault().toString();
                if (schema.getEnum() == null) {
                    defaultValueInit = "\"" + escapeText(def) + "\"";
                    defaultValueStr = escapeText(def);
                } else {
                    // convert to enum var name later in postProcessModels
                    defaultValueInit = "\"" + def + "\"";
                    defaultValueStr = def;
                }
            }
        } else if (ModelUtils.isObjectSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = super.toDefaultValue(schema);
                defaultValueStr = super.toDefaultValue(schema);
            }
        } else if (ModelUtils.isComposedSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = super.toDefaultValue(schema);
                defaultValueStr = super.toDefaultValue(schema);
            }
        }

        return Pair.of(defaultValueInit, defaultValueStr);
    }

    // left - initStr, right - defaultStr
    public Pair<String, String> arrayDefaultValue(String itemsDatatypeWithEnum, String itemsDataType, boolean itemsIsEnumOrRef,
                                                  boolean isArray, boolean itemsIsString, boolean itemsIsNumeric,
                                                  boolean itemsIsFloat, boolean itemsIsMap, Schema schema) {
        if (schema.getDefault() != null) { // has default value
            if (isArray) {
                List<String> values = new ArrayList<>();

                if (schema.getDefault() instanceof ArrayNode arrayNodeDefault) { // array of default values
                    if (arrayNodeDefault.isEmpty()) { // e.g. default: []
                        return getDefaultCollectionType(schema);
                    }
                    List<String> finalValues = values;
                    arrayNodeDefault.elements().forEachRemaining((element) -> finalValues.add(element.asText()));
                } else if (schema.getDefault() instanceof Collection<?> defCollection) {
                    List<String> finalValues = values;
                    defCollection.forEach((element) -> finalValues.add(String.valueOf(element)));
                } else { // single value
                    values = Collections.singletonList(String.valueOf(schema.getDefault()));
                }

                String defaultValue;
                String defaultValueInit;

                if (itemsIsEnumOrRef) { // inline or ref enum
                    var defaultValues = new ArrayList<String>();
                    for (String value : values) {
                        var enumVarName = toEnumVarName(value, itemsDataType);
                        if (enumVarName == null) {
                            defaultValues.add(null);
                        } else {
                            defaultValues.add(itemsDatatypeWithEnum + "." + enumVarName);
                        }
                    }
                    defaultValue = StringUtils.join(defaultValues, ", ");
                } else if (!values.isEmpty()) {
                    if (itemsIsString) { // array item is string
                        defaultValue = String.format(Locale.ROOT, "\"%s\"", StringUtils.join(values, "\", \""));
                        defaultValueInit = defaultValue;
                    } else if (itemsIsNumeric) {
                        defaultValue = String.join(", ", values);
                        defaultValueInit = values.stream()
                            .map(v -> {
                                if ("BigInteger".equals(itemsDataType)) {
                                    return "new BigInteger(\"" + v + "\")";
                                } else if ("BigDecimal".equals(itemsDataType)) {
                                    return "new BigDecimal(\"" + v + "\")";
                                } else if (itemsIsFloat) {
                                    return v + "F";
                                } else {
                                    return v;
                                }
                            })
                            .collect(Collectors.joining(", "));
                    } else { // array item is non-string, e.g. integer
                        defaultValue = StringUtils.join(values, ", ");
                    }
                } else {
                    return getDefaultCollectionType(schema);
                }

                return getDefaultCollectionType(schema, defaultValue);
            }
            if (itemsIsMap) { // map
                // TODO
                return Pair.of(null, null);
            } else {
                throw new RuntimeException("Error. Codegen Property must be array/set/map: " + schema);
            }
        }
        return Pair.of(null, null);
    }

    private Pair<String, String> getDefaultCollectionType(Schema schema) {
        return getDefaultCollectionType(schema, null);
    }

    private Pair<String, String> getDefaultCollectionType(Schema schema, String defaultValues) {
        String arrayFormat = "new %s<>(Arrays.asList(%s))";
        if (defaultValues == null || defaultValues.isEmpty()) {
            defaultValues = "";
            arrayFormat = "new %s<>()";
        }

        if (ModelUtils.isSet(schema)) {
            return Pair.of(String.format(Locale.ROOT, arrayFormat, instantiationTypes().getOrDefault("set", "LinkedHashSet"),
                defaultValues), defaultValues);
        }
        return Pair.of(String.format(Locale.ROOT, arrayFormat, instantiationTypes().getOrDefault("array", "ArrayList"), defaultValues), defaultValues);
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
    protected CodegenDiscriminator createDiscriminator(String schemaName, Schema schema) {

        var discriminator = super.createDiscriminator(schemaName, schema);
        if (discriminator == null) {
            return null;
        }

        if (discriminator.getMapping() != null) {
            for (var entry : discriminator.getMapping().entrySet()) {
                String name;
                if (entry.getValue().indexOf('/') < 0) {
                    continue;
                }
                name = ModelUtils.getSimpleRef(entry.getValue());
                var referencedSchema = ModelUtils.getSchema(openAPI, name);
                if (referencedSchema == null) {
                    once(log).error("Failed to lookup the schema '{}' when processing the discriminator mapping of oneOf/anyOf. Please check to ensure it's defined properly.", name);
                    continue;
                }
                if (referencedSchema.getProperties() == null || referencedSchema.getProperties().isEmpty()) {
                    continue;
                }
                boolean isDiscriminatorPropTypeFound = false;
                var props = (Map<String, Schema>) referencedSchema.getProperties();
                for (var propEntry : props.entrySet()) {
                    if (!propEntry.getKey().equals(discriminator.getPropertyName())) {
                        continue;
                    }
                    discriminator.setPropertyType(getTypeDeclaration(propEntry.getValue()));
                    isDiscriminatorPropTypeFound = true;
                    break;
                }
                if (isDiscriminatorPropTypeFound) {
                    break;
                }
            }
        }
        return discriminator;
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
    public String toApiName(String name) {
        return Utils.toApiName(name, apiNamePrefix, apiNameSuffix);
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
                                    Map<String, List<CodegenOperation>> operations) {

        if (!useTags) {
            String basePath = resourcePath;
            if (basePath.startsWith("/")) {
                basePath = basePath.substring(1);
            }
            int pos = basePath.indexOf("/");
            if (pos > 0) {
                basePath = basePath.substring(0, pos);
            }

            if (basePath.isEmpty()) {
                basePath = "default";
            } else {
                co.subresourceOperation = !co.path.isEmpty();
            }
            basePath = super.sanitizeTag(basePath);
            List<CodegenOperation> opList = operations.computeIfAbsent(basePath, k -> new ArrayList<>());
            opList.add(co);
            co.baseName = basePath;
            return;
        }

        if (generateOperationOnlyForFirstTag && !co.tags.get(0).getName().equals(tag)) {
            // This is not the first assigned to this operation tag;
            return;
        }

        super.addOperationToGroup(super.sanitizeTag(tag), resourcePath, operation, co, operations);

        var foundTag = co.tags.stream()
            .filter(t -> (t.getExtensions() == null || !t.getExtensions().containsKey("x-autogenerated")) && t.getName().equals(tag))
            .findFirst()
            .orElse(null);
        if (foundTag != null) {
            co.vendorExtensions.put("normalizedBaseTag", foundTag.getName());
            co.vendorExtensions.put("normalizedTagDesc", foundTag.getDescription());
        }
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openApi) {

        if (openApi.getPaths() != null) {
            for (var path : openApi.getPaths().values()) {
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
                            if (Objects.equals(opParam.getName(), param.getName())
                                && Objects.equals(opParam.get$ref(), param.get$ref())) {
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

        var inlineModelResolver = new MicronautInlineModelResolver(openApi);
        inlineModelResolver.setInlineSchemaNameMapping(inlineSchemaNameMapping);
        inlineModelResolver.setInlineSchemaOptions(inlineSchemaOption);
        inlineModelResolver.flatten();

        var pathItems = openApi.getPaths();
        // fix for ApiResponse with $ref
        if (pathItems != null) {
            for (var pathEntry : pathItems.entrySet()) {
                for (var opEntry : pathEntry.getValue().readOperationsMap().entrySet()) {
                    // process all response bodies
                    if (opEntry.getValue().getResponses() != null) {
                        for (var responseEntry : opEntry.getValue().getResponses().entrySet()) {
                            var response = responseEntry.getValue();
                            var refResponse = ModelUtils.getReferencedApiResponse(openApi, response);
                            if (response.getContent() == null) {
                                response.setContent(refResponse.getContent());
                            }
                            if (response.getDescription() == null) {
                                response.setDescription(refResponse.getDescription());
                            }
                            if (response.getHeaders() == null || response.getHeaders().isEmpty()) {
                                response.setHeaders(refResponse.getHeaders());
                            }
                            if (response.getExtensions() == null || response.getExtensions().isEmpty()) {
                                response.setExtensions(refResponse.getExtensions());
                            }
                            if (response.getLinks() == null || response.getLinks().isEmpty()) {
                                response.setLinks(refResponse.getLinks());
                            }
                        }
                    }
                }
            }
        }

        super.preprocessOpenAPI(openApi);
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        Map<String, CodegenModel> models = allModels.stream()
            .map(ModelMap::getModel)
            .collect(Collectors.toMap(v -> v.classname, v -> v));
        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();
        var needToAddImportFormat = false;

        var converterCounters = new HashMap<String, Integer>();
        var enumParams = new ArrayList<CodegenParameter>();
        var enumImports = new ArrayList<String>();

        for (CodegenOperation op : operationList) {

            op.imports.removeIf(importsToDrop::containsKey);

            objs.put("normalizedBaseTag", op.vendorExtensions.get("normalizedBaseTag"));
            objs.put("normalizedTagDesc", op.vendorExtensions.get("normalizedTagDesc"));

            // Set whether body is supported in request
            op.vendorExtensions.put("methodAllowsBody", op.httpMethod.equals("PUT")
                || op.httpMethod.equals("POST")
                || op.httpMethod.equals("PATCH")
                || op.httpMethod.equals("OPTIONS")
                || op.httpMethod.equals("DELETE")
            );
            if (StringUtils.isNotEmpty(op.notes)) {
                op.notes = op.notes.strip();
            }
            if (StringUtils.isNotEmpty(op.summary)) {
                op.summary = op.summary.strip();
            }

            normalizeExtraAnnotations(EXT_ANNOTATIONS_OPERATION, false, op.vendorExtensions);

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
            if (CONTENT_TYPE_ANY.equals(op.vendorExtensions.get(VendorExtension.X_CONTENT_TYPE.getName()))) {
                op.vendorExtensions.put(VendorExtension.X_CONTENT_TYPE.getName(), CONTENT_TYPE_APPLICATION_JSON);
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
                || op.consumes.size() == 1 && CONTENT_TYPE_APPLICATION_JSON.equals(op.consumes.get(0).get("mediaType"))) {
                op.vendorExtensions.put("onlyDefaultConsumeOrEmpty", true);
            }
            // is only default "application/json" media type
            if (op.produces == null
                || op.produces.isEmpty()
                || op.produces.size() == 1 && CONTENT_TYPE_APPLICATION_JSON.equals(op.produces.get(0).get("mediaType"))) {
                op.vendorExtensions.put("onlyDefaultProduceOrEmpty", true);
            }

            // Force form parameters are only set if the content-type is according
            // formParams correspond to urlencoded type
            // bodyParams correspond to multipart body
            if (CONTENT_TYPE_APPLICATION_FORM_URLENCODED.equals(op.vendorExtensions.get(VendorExtension.X_CONTENT_TYPE.getName()))) {
                op.formParams.addAll(op.bodyParams);
                op.bodyParams.forEach(p -> {
                    p.isBodyParam = false;
                    p.isFormParam = true;
                });
                op.bodyParams.clear();
            } else if (CONTENT_TYPE_MULTIPART_FORM_DATA.equals(op.vendorExtensions.get(VendorExtension.X_CONTENT_TYPE.getName()))) {
                op.bodyParams.addAll(op.formParams);
                for (var param : op.allParams) {
                    if (param.isFormParam) {
                        param.isBodyParam = true;
                        param.isFormParam = false;
                        param.vendorExtensions.put("isPart", true);
                        if (param.isEnumRef) {
                            param.isEnum = true;
                        }
                        if (param.isEnum) {
                            addEnumParamsForConverters(modelPackage, param, converterCounters, enumParams, enumImports, false);
                        }
                    }
                }
                op.formParams.forEach(p -> {
                    p.isBodyParam = true;
                    p.isFormParam = false;
                    p.vendorExtensions.put("isPart", true);
                    if (p.isEnumRef) {
                        p.isEnum = true;
                    }
                    if (p.isEnum) {
                        addEnumParamsForConverters(modelPackage, p, converterCounters, enumParams, enumImports, false);
                    }
                });
                op.formParams.clear();
            }

            for (var param : op.allParams) {
                if (param.isEnumRef) {
                    param.isEnum = true;
                }
                processGenericAnnotations(param, useBeanValidation, isGenerateHardNullable(), false, false, false, false, false, false);
                if (useBeanValidation && !param.isContainer && param.isModel) {
                    param.vendorExtensions.put("withValid", true);
                }

                // check pattern property for date types: if set, need use this pattern as `@Format` annotation value
                if (isDateType(param.dataType)) {
                    if (StringUtils.isNotEmpty(param.pattern)) {
                        param.vendorExtensions.put("formatPattern", param.pattern);
                        param.pattern = null;
                    }
                    param.minItems = null;
                    param.maxItems = null;
                    param.minLength = null;
                    param.maxLength = null;
                }
                var queryValueFormat = calcQueryValueFormat(param);
                if (queryValueFormat != null) {
                    needToAddImportFormat = true;
                    param.vendorExtensions.put("format", queryValueFormat);
                }

                if (param.isEnum && !param.isBodyParam) {
                    addEnumParamsForConverters(modelPackage, param, converterCounters, enumParams, enumImports, false);
                }
            }
            if (op.returnProperty != null) {
                processGenericAnnotations(op.returnProperty, useBeanValidation, isGenerateHardNullable(), false, false, false, false, false, false);
                op.returnType = op.returnProperty.vendorExtensions.get("typeWithEnumWithGenericAnnotations").toString();
            }

            var deprecatedMessage = op.vendorExtensions.get("x-deprecated-message");
            var xDeprecated = op.vendorExtensions.get("x-deprecated");
            if (deprecatedMessage == null && xDeprecated instanceof String xDeprecatedStr) {
                deprecatedMessage = xDeprecatedStr;
            }
            if (deprecatedMessage != null) {
                op.vendorExtensions.put("x-deprecated-message", deprecatedMessage.toString());
            }
        }

        additionalProperties.put("enumImports", enumImports);
        additionalProperties.put("enumParams", enumParams);

        if (needToAddImportFormat) {
            objs.getImports().add(Map.of("import", "static io.micronaut.core.convert.converters.MultiValuesConverterFactory.*", "classname", "MultiValuesConverterFactory"));
        }

        return objs;
    }

    private void checkPrimitives(IJsonSchemaValidationProperties obj, Schema schema) {

        if (obj == null) {
            return;
        }

        CodegenModel model = null;
        CodegenParameter param = null;
        CodegenProperty prop = null;
        CodegenResponse response = null;
        if (obj instanceof CodegenModel m) {
            model = m;
        } else if (obj instanceof CodegenProperty p) {
            prop = p;
        } else if (obj instanceof CodegenParameter p) {
            param = p;
        } else if (obj instanceof CodegenResponse r) {
            response = r;
        }
        var extMap = model != null ? model.vendorExtensions : param != null ? param.vendorExtensions : prop != null ? prop.vendorExtensions : response.vendorExtensions;
        var dataType = model != null ? model.dataType : param != null ? param.dataType : prop != null ? prop.dataType : response.dataType;
        var dataTypeWithEnum = param != null ? param.datatypeWithEnum : prop != null ? prop.datatypeWithEnum : null;

        if (schema == null) {
            extMap.put("baseType", dataType);
            return;
        }

        var isNumeric = model != null ? model.isNumeric : param != null ? param.isNumeric : prop != null ? prop.isNumeric : response.isNumeric;
        var isNumber = model != null ? model.isNumber : param != null ? param.isNumber : prop != null ? prop.isNumber : response.isNumber;
        var isShort = model != null ? model.isShort : param != null ? param.isShort : prop != null ? prop.isShort : response.isShort;
        var isInteger = model != null ? model.isInteger : param != null ? param.isInteger : prop != null ? prop.isInteger : response.isInteger;
        var isLong = model != null ? model.isLong : param != null ? param.isLong : prop != null ? prop.isLong : response.isLong;
        var isFloat = model != null ? model.isFloat : param != null ? param.isFloat : prop != null ? prop.isFloat : response.isFloat;
        var isDouble = model != null ? model.isDouble : param != null ? param.isDouble : prop != null ? prop.isDouble : response.isDouble;

        var extensions = schema.getExtensions();
        var format = extensions != null ? (String) extensions.get("x-format") : null;
        if (format == null) {
            format = schema.getFormat() == null ? "object" : schema.getFormat();
        }
        var schemaType = extensions != null ? (String) extensions.get("x-type") : null;
        if (schemaType == null) {
            schemaType = schema.getType() == null ? "object" : schema.getType();
        }
        var baseType = dataType;
        var isPrimitive = false;

        if (TYPE_CHAR.equals(schemaType) || TYPE_CHARACTER.equals(schemaType)) {
            baseType = "char";
            dataType = "Character";
            dataTypeWithEnum = "Character";
            isPrimitive = true;
        } else if (TYPE_BYTE.equals(schemaType)) {
            baseType = "byte";
            dataType = "Byte";
            dataTypeWithEnum = "Byte";
            isNumeric = true;
            isNumber = true;
            isInteger = true;
            isPrimitive = true;
        } else if (INTEGER_TYPE.equals(schemaType) && (FORMAT_INT8.equals(format) || BYTE_FORMAT.equals(format))) {
            baseType = "Byte";
            dataType = "Byte";
            dataTypeWithEnum = "Byte";
            isNumeric = true;
            isNumber = true;
            isInteger = true;
        } else if (TYPE_SHORT.equals(schemaType)) {
            baseType = "short";
            dataType = "Short";
            dataTypeWithEnum = "Short";
            isNumeric = true;
            isNumber = true;
            isShort = true;
            isPrimitive = true;
        } else if (INTEGER_TYPE.equals(schemaType) && (FORMAT_INT16.equals(format) || FORMAT_SHORT.equals(format))) {
            baseType = "Short";
            dataType = "Short";
            dataTypeWithEnum = "Short";
            isNumeric = true;
            isNumber = true;
            isShort = true;
        } else if (TYPE_INT.equals(schemaType)) {
            baseType = "int";
            dataType = "Integer";
            dataTypeWithEnum = "Integer";
            isNumeric = true;
            isNumber = true;
            isInteger = true;
            isPrimitive = true;
        } else if (TYPE_LONG.equals(schemaType)) {
            baseType = "long";
            dataType = "Long";
            dataTypeWithEnum = "Long";
            isNumeric = true;
            isNumber = true;
            isLong = true;
            isPrimitive = true;
        } else if (TYPE_FLOAT.equals(schemaType)) {
            baseType = "float";
            dataType = "Float";
            dataTypeWithEnum = "Float";
            isNumeric = true;
            isNumber = true;
            isFloat = true;
            isPrimitive = true;
        } else if (TYPE_DOUBLE.equals(schemaType)) {
            baseType = "double";
            dataType = "Double";
            dataTypeWithEnum = "Double";
            isNumeric = true;
            isNumber = true;
            isDouble = true;
            isPrimitive = true;
        }

        var enumSchema = schema;
        var isEnum = ModelUtils.isEnumSchema(schema);
        if (!isEnum) {
            var refSchema = ModelUtils.getReferencedSchema(openAPI, schema);
            if (refSchema != null) {
                isEnum = ModelUtils.isEnumSchema(refSchema);
                enumSchema = refSchema;
            }
        }

        if (isEnum) {
            var enumSchemaType = enumSchema.getExtensions() != null ? (String) enumSchema.getExtensions().get("x-type") : null;
            if (enumSchemaType == null) {
                enumSchemaType = enumSchema.getType();
            }
            var enumSchemaFormat = enumSchema.getExtensions() != null ? (String) enumSchema.getExtensions().get("x-format") : null;
            if (enumSchemaFormat == null) {
                enumSchemaFormat = enumSchema.getFormat();
            }
            var isEnumPrimitive = TYPE_CHAR.equals(enumSchemaType)
                || TYPE_CHARACTER.equals(enumSchemaType)
                || TYPE_BYTE.equals(enumSchemaType)
                || TYPE_SHORT.equals(enumSchemaType)
                || INTEGER_TYPE.equals(enumSchemaType) && (FORMAT_INT16.equals(enumSchemaFormat) || FORMAT_SHORT.equals(enumSchemaFormat))
                || TYPE_INT.equals(enumSchemaType)
                || TYPE_LONG.equals(enumSchemaType)
                || TYPE_FLOAT.equals(enumSchemaType)
                || TYPE_DOUBLE.equals(enumSchemaType);
            var enumValueType = "Object";
            if ("string".equalsIgnoreCase(enumSchemaType)) {
                enumValueType = "String";
            } else if ("char".equalsIgnoreCase(enumSchemaType)) {
                enumValueType = isEnumPrimitive ? "char" : "Character";
            } else if ("byte".equalsIgnoreCase(enumSchemaType)
                || ("integer".equalsIgnoreCase(enumSchemaType) && BYTE_FORMAT.equalsIgnoreCase(enumSchemaFormat))
                || ("integer".equalsIgnoreCase(enumSchemaType) && FORMAT_INT8.equalsIgnoreCase(enumSchemaFormat))
            ) {
                enumValueType = isEnumPrimitive ? "byte" : "Byte";
            } else if ("short".equalsIgnoreCase(enumSchemaType)
                || ("integer".equalsIgnoreCase(enumSchemaType) && FORMAT_SHORT.equalsIgnoreCase(enumSchemaFormat))
                || ("integer".equalsIgnoreCase(enumSchemaType) && FORMAT_INT16.equalsIgnoreCase(enumSchemaFormat))
            ) {
                enumValueType = isEnumPrimitive ? "short" : "Short";
            } else if ("long".equalsIgnoreCase(enumSchemaType)
                || ("integer".equalsIgnoreCase(enumSchemaType) && FORMAT_LONG.equalsIgnoreCase(enumSchemaFormat))
                || ("integer".equalsIgnoreCase(enumSchemaType) && INTEGER64_FORMAT.equalsIgnoreCase(enumSchemaFormat))
            ) {
                enumValueType = isEnumPrimitive ? "long" : "Long";
            } else if ("int".equalsIgnoreCase(enumSchemaType)
                || "integer".equalsIgnoreCase(enumSchemaType)) {
                enumValueType = isEnumPrimitive ? "int" : "Integer";
            } else if ("float".equalsIgnoreCase(enumSchemaType)
                || ("number".equalsIgnoreCase(enumSchemaType) && FLOAT_FORMAT.equalsIgnoreCase(enumSchemaFormat))
            ) {
                enumValueType = isEnumPrimitive ? "float" : "Float";
            } else if ("double".equalsIgnoreCase(enumSchemaType)
                || ("number".equalsIgnoreCase(enumSchemaType) && DOUBLE_FORMAT.equalsIgnoreCase(enumSchemaFormat))
            ) {
                enumValueType = isEnumPrimitive ? "double" : "Double";
            } else if ("boolean".equalsIgnoreCase(enumSchemaType)) {
                enumValueType = isEnumPrimitive ? "boolean" : "Boolean";
            } else if ("biginteger".equalsIgnoreCase(enumSchemaType)) {
                enumValueType = "BigInteger";
            } else if ("bigdecimal".equalsIgnoreCase(enumSchemaType)
                || "number".equalsIgnoreCase(enumSchemaType)
            ) {
                enumValueType = "BigDecimal";
            }
            extMap.put("enumValueType", enumValueType);
            if (enumSchema.getNullable() != null && enumSchema.getNullable()) {
                extMap.put("enumValueIsNullable", true);
            }
        }

        extMap.put("baseType", baseType);
        extMap.put("isPrimitive", isPrimitive);

        if (model != null) {
            model.dataType = dataType;
            model.isNumeric = isNumeric;
            model.isNumber = isNumber;
            model.isShort = isShort;
            model.isInteger = isInteger;
            model.isLong = isLong;
            model.isFloat = isFloat;
            model.isDouble = isDouble;
        } else if (prop != null) {
            prop.dataType = dataType;
            prop.datatypeWithEnum = dataTypeWithEnum;
            prop.isNumeric = isNumeric;
            prop.isNumber = isNumber;
            prop.isShort = isShort;
            prop.isInteger = isInteger;
            prop.isLong = isLong;
            prop.isFloat = isFloat;
            prop.isDouble = isDouble;
        } else if (param != null) {
            param.dataType = dataType;
            param.datatypeWithEnum = dataTypeWithEnum;
            param.isNumeric = isNumeric;
            param.isNumber = isNumber;
            param.isShort = isShort;
            param.isInteger = isInteger;
            param.isLong = isLong;
            param.isFloat = isFloat;
            param.isDouble = isDouble;
        } else {
            response.dataType = dataType;
            response.isNumeric = isNumeric;
            response.isNumber = isNumber;
            response.isShort = isShort;
            response.isInteger = isInteger;
            response.isLong = isLong;
            response.isFloat = isFloat;
            response.isDouble = isDouble;
        }
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema) {
        CodegenModel model = super.fromModel(name, schema);
        checkPrimitives(model, unaliasSchema(schema));

        if (!model.oneOf.isEmpty()) {
            if (useOneOfInterfaces) {
                model.vendorExtensions.put("x-is-one-of-interface", true);
            }
            if (ModelUtils.isTypeObjectSchema(schema)) {
                CodegenModel m = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
                updateModelForObject(m, schema);
                model.vars = m.vars;
                model.allVars = m.allVars;
                model.requiredVars = m.requiredVars;
                model.readWriteVars = m.readWriteVars;
                model.optionalVars = m.optionalVars;
                model.readOnlyVars = m.readOnlyVars;
                model.parentVars = m.parentVars;
                model.nonNullableVars = m.nonNullableVars;
                model.setRequiredVarsMap(m.getRequiredVarsMap());
                model.mandatory = m.mandatory;
                model.allMandatory = m.allMandatory;
            }
        }

        processDuplicateVars(model.getVars());
        model.imports.remove("ApiModel");
        model.imports.remove("ApiModelProperty");
        allModels.put(name, model);
        return model;
    }

    private boolean shouldBeImplicitHeader(CodegenParameter parameter) {
        return StringUtils.isNotBlank(implicitHeadersRegex) && parameter.baseName.matches(implicitHeadersRegex);
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
            var responseFileType = typeMapping.get("responseFile");
            op.returnType = responseFileType;
            op.returnBaseType = responseFileType;
            op.imports.add(op.returnType);
            if (op.returnProperty != null) {
                op.returnProperty.complexType = responseFileType;
                op.returnProperty.dataType = responseFileType;
                op.returnProperty.datatypeWithEnum = responseFileType;
                op.returnProperty.baseType = responseFileType;
            }
        }

        var paramsWithoutImplicitHeaders = new ArrayList<CodegenParameter>();
        var swaggerParams = new ArrayList<CodegenParameter>();
        var hasMultipleParams = false;
        var notBodyParamsSize = 0;
        for (var param : op.allParams) {
            if (!param.isHeaderParam || (!implicitHeaders && !shouldBeImplicitHeader(param))) {
                paramsWithoutImplicitHeaders.add(param);
            }
            if (param.isBodyParam || param.isFormParam) {
                continue;
            }
            swaggerParams.add(param);
            notBodyParamsSize++;
            if (notBodyParamsSize > 1) {
                hasMultipleParams = true;
            }
        }
        op.vendorExtensions.put("swaggerParams", swaggerParams);
        op.vendorExtensions.put("originalParams", paramsWithoutImplicitHeaders);
        op.vendorExtensions.put("hasNotBodyParam", notBodyParamsSize > 0);
        op.vendorExtensions.put("hasMultipleParams", hasMultipleParams);
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
        var additionalMappings = new LinkedHashMap<String, ParameterMapping>();
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
            if (mapping.mappedType() == null) {
                continue;
            }
            var newParam = new CodegenParameter();
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

        if (varName.chars().allMatch(c -> Character.isUpperCase(c) || c == '_')) {
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
        if (varName.startsWith("_")) {
            var firstNameChar = varName.toCharArray()[0];
            var underscorePrefix = getUnderscorePrefix(name);
            varName = underscorePrefix
                + (firstNameChar == '_' && !underscorePrefix.isEmpty() ? "" : Character.toLowerCase(firstNameChar))
                + varName.substring(1);
        }

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

        for (ModelsMap models : objs.values()) {
            CodegenModel model = models.getModels().get(0).getModel();
            var parentModel = model.getParentModel();
            if (parentModel != null
                && parentModel.isAlias
                && parentModel.getRef() != null
                && model.interfaces != null
                && model.interfaceModels != null
            ) {
                for (var interfaceModel : model.interfaceModels) {
                    if (!interfaceModel.name.equals(parentModel.dataType)) {
                        continue;
                    }
                    importsToDrop.put(model.parent, modelPackage + '.' + model.parent);
                    model.imports.removeIf(importsToDrop::containsKey);
                    model.parent = interfaceModel.name;
                    model.parentSchema = interfaceModel.schemaName;
                    model.parentModel = interfaceModel;
                    model.parentRequiredVars = interfaceModel.requiredVars;
                    model.parentVars = interfaceModel.vars;
                    model.interfaces = null;
                    model.interfaceModels = null;
                    break;
                }
                if (model.interfaces != null) {
                    model.interfaces.removeIf(name -> name.equals(parentModel.dataType));
                }
                if (model.interfaceModels != null) {
                    model.interfaceModels.removeIf(iModel -> iModel.name.equals(parentModel.dataType));
                }
            }
        }

        var isServer = isServer();

        for (ModelsMap models : objs.values()) {
            CodegenModel model = models.getModels().get(0).getModel();

            models.getImports().removeIf(impMap -> importsToDrop.containsValue(impMap.get("import")));
            model.imports.removeIf(importsToDrop::containsKey);

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

            model.vendorExtensions.put("hasOwnVars", !model.vars.isEmpty());
            model.vendorExtensions.put("withMultipleVars", model.vars.size() > 1);
            if (!requiredParentVarsWithoutDiscriminator.isEmpty()) {
                model.vendorExtensions.put("requiredParentVarsWithoutDiscriminator", requiredParentVarsWithoutDiscriminator);
            }
            var requiredVarsWithoutDiscriminatorAndReadOnly = new ArrayList<CodegenProperty>();
            if (!requiredVarsWithoutDiscriminator.isEmpty()) {
                model.vendorExtensions.put("requiredVarsWithoutDiscriminator", requiredVarsWithoutDiscriminator);
                for (var prop : requiredVarsWithoutDiscriminator) {
                    if (!isServer && prop.isReadOnly) {
                        continue;
                    }
                    requiredVarsWithoutDiscriminatorAndReadOnly.add(prop);
                }
                if (!requiredVarsWithoutDiscriminatorAndReadOnly.isEmpty()) {
                    model.vendorExtensions.put("requiredVarsWithoutDiscriminatorAndReadOnly", requiredVarsWithoutDiscriminatorAndReadOnly);
                }
            }
            model.allVars = allVars;

            var deprecatedMessage = model.vendorExtensions.get("x-deprecated-message");
            var xDeprecated = model.vendorExtensions.get("x-deprecated");
            if (deprecatedMessage == null && xDeprecated instanceof String xDeprecatedStr) {
                deprecatedMessage = xDeprecatedStr;
            }
            if (deprecatedMessage != null) {
                model.vendorExtensions.put("x-deprecated-message", deprecatedMessage.toString());
            }

            model.vendorExtensions.put("requiredVars", requiredVars);
            model.vendorExtensions.put("optionalVars", optionalVars);
            model.vendorExtensions.put("areRequiredVarsAndReadOnlyVars", !requiredVarsWithoutDiscriminator.isEmpty() && !model.readOnlyVars.isEmpty());
            model.vendorExtensions.put("serialId", random.nextLong());
            model.vendorExtensions.put("withRequiredVars", !requiredVarsWithoutDiscriminator.isEmpty() && !requiredVarsWithoutDiscriminatorAndReadOnly.isEmpty());
            normalizeExtraAnnotations(EXT_ANNOTATIONS_CLASS, false, model.vendorExtensions);
            if (model.discriminator != null) {
                model.vendorExtensions.put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
                model.vendorExtensions.put("hasMultipleMappedModels", model.discriminator.getMappedModels().size() > 1);
                model.discriminator.getVendorExtensions().put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
                model.discriminator.getVendorExtensions().put("hasMultipleMappedModels", model.discriminator.getMappedModels().size() > 1);
            }
            model.vendorExtensions.put("isServer", isServer);
            for (var property : model.vars) {
                processProperty(property, isServer, model, objs);
            }
            for (var property : model.requiredVars) {
                processProperty(property, isServer, model, objs);
            }
            if (model.parentVars != null) {
                for (var property : model.parentVars) {
                    processProperty(property, isServer, model, objs);
                }
            }
            if (model.isEnum) {
                addImport(model, "Function");
            }

            model.hasVars = !requiredVarsWithoutDiscriminator.isEmpty() || !model.vars.isEmpty();
            // just for tests
            if (System.getProperty("micronaut.test.no-vars") != null) {
                model.hasVars = false;
                model.vendorExtensions.put("hasOwnVars", false);
                model.vendorExtensions.put("requiredVarsWithoutDiscriminator", Collections.emptyList());
                model.vendorExtensions.put("optionalVars", Collections.emptyList());
                model.vendorExtensions.put("requiredVars", Collections.emptyList());
                model.vendorExtensions.put("withRequiredVars", false);
                model.vars = Collections.emptyList();
            }

            addStrValueToEnum(model);
        }

        for (ModelsMap models : objs.values()) {
            CodegenModel model = models.getModels().get(0).getModel();
            processOneOfModels(model, objs.values());
        }

        return objs;
    }

    @Override
    protected void updateEnumVarsWithExtensions(List<Map<String, Object>> enumVars, Map<String, Object> vendorExtensions, String dataType) {
        if (vendorExtensions == null) {
            return;
        }

        processEnumExt(enumVars, vendorExtensions, "enumDescription", List.of("x-enum-descriptions", "x-xl4-enum-doc"));
        processEnumExt(enumVars, vendorExtensions, "enumDeprecatedMessage", List.of("x-enum-deprecated-messages", "x-xl4-enum-deprecated"));

        var deprecatedExt = vendorExtensions.get("x-deprecated");
        List<?> xDeprecated = null;
        Map<?, ?> xDeprecatedMap = null;
        if (deprecatedExt instanceof List<?> deprecatedList) {
            xDeprecated = deprecatedList;
        } else if (deprecatedExt instanceof Map<?, ?> deprecatedMap) {
            xDeprecated = new ArrayList<>(deprecatedMap.keySet());
            xDeprecatedMap = deprecatedMap;
        }
        var deprecatedMessagesExt = vendorExtensions.get("x-enum-deprecated-messages");
        if (deprecatedMessagesExt == null) {
            deprecatedMessagesExt = vendorExtensions.get("x-xl4-enum-deprecated");
        }
        if (xDeprecated == null && deprecatedMessagesExt instanceof Map<?, ?> deprecatedMap) {
            xDeprecated = new ArrayList<>(deprecatedMap.keySet());
            xDeprecatedMap = deprecatedMap;
        }
        if (xDeprecated != null && !xDeprecated.isEmpty()) {
            for (var deprecatedItem : xDeprecated) {
                Map<String, Object> foundEnumVar = findEnumVar(deprecatedItem.toString(), enumVars);
                if (foundEnumVar == null) {
                    continue;
                }
                foundEnumVar.put("deprecated", true);
                if (xDeprecatedMap != null) {
                    var deprecatedMessage = xDeprecatedMap.get(deprecatedItem);
                    if (deprecatedMessage instanceof String deprecatedMessageStr) {
                        foundEnumVar.put("enumDeprecatedMessage", deprecatedMessageStr);
                    }
                }
            }
        }

        processEnumExt(enumVars, vendorExtensions, "name", List.of("x-enum-varnames"));

        var baseType = (String) vendorExtensions.get("baseType");
        for (var enumVar : enumVars) {
            if ((boolean) enumVar.get("isString")) {
                continue;
            }
            var value = (String) enumVar.get("value");
            value = value.replace("\"", "");
            if ("char".equals(baseType) && !value.startsWith("'")) {
                enumVar.put("value", "'" + value + "'");
            } else if ("short".equalsIgnoreCase(baseType) && !value.startsWith("(short)")) {
                enumVar.put("value", "(short) " + value);
            } else if ("byte".equalsIgnoreCase(baseType) && !value.startsWith("(byte)")) {
                enumVar.put("value", "(byte) " + value);
            }
        }
    }

    private void processOneOfModels(CodegenModel model, Collection<ModelsMap> models) {

        if (!model.vendorExtensions.containsKey("x-is-one-of-interface")
            || !Boolean.parseBoolean(model.vendorExtensions.get("x-is-one-of-interface").toString())) {
            return;
        }

        var discriminator = model.discriminator;
        if (discriminator == null) {
            return;
        }
        var oneOfInterfaceName = model.name;
        for (var modelMap : models) {
            var m = modelMap.getModels().get(0).getModel();

            if (!m.vendorExtensions.containsKey("x-implements")) {
                continue;
            }
            var xImplements = m.vendorExtensions.get("x-implements");
            if (!(xImplements instanceof List<?> xImplementsList)) {
                continue;
            }
            for (var implInterface : xImplementsList) {
                if (!oneOfInterfaceName.equalsIgnoreCase(implInterface.toString())) {
                    continue;
                }
                fixDiscriminatorProp(m.allVars, discriminator);
                fixDiscriminatorProp(m.requiredVars, discriminator);
                var requiredVarsWithoutDiscriminatorAndReadOnly = (List<CodegenProperty>) m.vendorExtensions.get("requiredVarsWithoutDiscriminatorAndReadOnly");
                if (fixDiscriminatorProp(requiredVarsWithoutDiscriminatorAndReadOnly, discriminator)) {
                    removePropIfContains(discriminator.getPropertyName(), requiredVarsWithoutDiscriminatorAndReadOnly);
                }
                var requiredVarsWithoutDiscriminator = (List<CodegenProperty>) m.vendorExtensions.get("requiredVarsWithoutDiscriminator");
                if (fixDiscriminatorProp(requiredVarsWithoutDiscriminator, discriminator)) {
                    removePropIfContains(discriminator.getPropertyName(), requiredVarsWithoutDiscriminator);
                    m.vendorExtensions.put("withRequiredVars", !requiredVarsWithoutDiscriminator.isEmpty() && !requiredVarsWithoutDiscriminatorAndReadOnly.isEmpty());
                }
                var discriminatorPropFound = fixDiscriminatorProp(m.vars, discriminator);
                if (!discriminatorPropFound) {
                    var discriminatorProp = new CodegenProperty();
                    discriminatorProp.name = discriminator.getPropertyName();
                    discriminatorProp.baseName = discriminator.getPropertyName();
                    discriminatorProp.dataType = discriminator.getPropertyType();
                    discriminatorProp.baseType = discriminator.getPropertyType();
                    discriminatorProp.vendorExtensions.put("typeWithEnumWithGenericAnnotations", discriminator.getPropertyType());
                    discriminatorProp.isDiscriminator = true;
                    discriminatorProp.isOverridden = true;
                    discriminatorProp.isNullable = true;
                    discriminatorProp.isOptional = false;
                    discriminatorProp.required = true;
                    discriminatorProp.isReadOnly = false;
                    discriminatorProp.vendorExtensions.put("overridden", true);
                    var realName = discriminatorProp.name;
                    realName = realName.replaceFirst("_", "");
                    discriminatorProp.nameInPascalCase = camelize(realName);
                    discriminatorProp.nameInCamelCase = camelize(realName, LOWERCASE_FIRST_LETTER);
                    discriminatorProp.nameInSnakeCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, discriminatorProp.nameInCamelCase);
                    // fix for getters and setters for escaped vars as reserved words and started with '_', like this: '_for'
                    discriminatorProp.getter = "get" + getterAndSetterCapitalize(discriminatorProp.name);
                    discriminatorProp.setter = "set" + getterAndSetterCapitalize(discriminatorProp.name);
                    discriminatorProp.vendorExtensions.put("realName", realName);
                    var parentVarDiscriminator = findProp(discriminatorProp, m.parentVars);
                    if (parentVarDiscriminator == null) {
                        m.vars.add(discriminatorProp);
                        m.allVars.add(discriminatorProp);
                        m.vendorExtensions.put("withMultipleVars", true);
                    }
                }
            }
        }
    }

    private boolean fixDiscriminatorProp(List<CodegenProperty> props, CodegenDiscriminator discriminator) {
        if (props == null || props.isEmpty()) {
            return false;
        }
        for (var prop : props) {
            if (prop.name.equals(discriminator.getPropertyName())) {
                processDiscriminatorProperty(prop, discriminator);
                return true;
            }
        }
        return false;
    }

    private void processDiscriminatorProperty(CodegenProperty prop, CodegenDiscriminator discriminator) {
        prop.isDiscriminator = true;
        prop.isOverridden = true;
        prop.isNullable = false;
        prop.isOptional = false;
        prop.required = true;
        prop.isReadOnly = false;
        prop.vendorExtensions.put("overridden", true);
        prop.openApiType = discriminator.getPropertyType();
        prop.dataType = discriminator.getPropertyType();
        prop.datatypeWithEnum = discriminator.getPropertyType();
        prop.baseType = discriminator.getPropertyType();
        processGenericAnnotations(prop, useBeanValidation, isGenerateHardNullable(), false, false, false, false, false, false);
    }

    private void processProperty(CodegenProperty property, boolean isServer, CodegenModel model, Map<String, ModelsMap> models) {

        var deprecatedMessage = property.vendorExtensions.get("x-deprecated-message");
        var xDeprecated = property.vendorExtensions.get("x-deprecated");
        if (deprecatedMessage == null && xDeprecated instanceof String xDeprecatedStr) {
            deprecatedMessage = xDeprecatedStr;
        }
        if (deprecatedMessage != null) {
            property.vendorExtensions.put("x-deprecated-message", deprecatedMessage.toString());
        }

        property.vendorExtensions.put("inRequiredArgsConstructor", !property.isReadOnly || isServer);
        property.vendorExtensions.put("isServer", isServer);
        property.vendorExtensions.put("lombok", lombok);
        property.vendorExtensions.put("defaultValueIsNotNull", property.defaultValue != null && !property.defaultValue.equals(NULL_STRING));
        property.vendorExtensions.put("x-implements", model.vendorExtensions.get("x-implements"));
        if (useBeanValidation && (
            (!property.isContainer && property.isModel)
                || (property.getIsArray() && property.getComplexType() != null && models.containsKey(property.getComplexType()))
        )) {
            property.vendorExtensions.put("withValid", true);
        }
        // check pattern property for date types: if set, need use this pattern as `@Format` annotation value
        if (isDateType(property.dataType)) {
            if (StringUtils.isNotEmpty(property.pattern)) {
                property.vendorExtensions.put("formatPattern", property.pattern);
                property.pattern = null;
            }
            property.minItems = null;
            property.maxItems = null;
            property.minLength = null;
            property.maxLength = null;
        }

        processGenericAnnotations(property, useBeanValidation, isGenerateHardNullable(), false, false, false, false, false, false);

        normalizeExtraAnnotations(EXT_ANNOTATIONS_FIELD, false, property.vendorExtensions);
        normalizeExtraAnnotations(EXT_ANNOTATIONS_SETTER, false, property.vendorExtensions);
    }

    public boolean isGenerateHardNullable() {
        return false;
    }

    private void processParentModel(CodegenModel model, List<CodegenProperty> requiredVarsWithoutDiscriminator,
                                    List<CodegenProperty> requiredParentVarsWithoutDiscriminator,
                                    List<CodegenProperty> allVars) {
        var parent = model.parentModel;
        var hasParent = parent != null;

        for (var variable : model.vars) {
            if (notContainsProp(variable, allVars)) {
                allVars.add(variable.clone());
            }
        }

        var parentIsOneOfInterface = hasParent && Boolean.TRUE.equals(parent.getVendorExtensions().get("x-is-one-of-interface"));
        for (var v : model.requiredVars) {
            if (notContainsProp(v, requiredVarsWithoutDiscriminator) && (!isDiscriminator(v, model) || parentIsOneOfInterface)) {
                requiredVarsWithoutDiscriminator.add(v);
            }
        }

        requiredParentVarsWithoutDiscriminator(model, requiredParentVarsWithoutDiscriminator);
        if (hasParent) {
            var parentVars = new ArrayList<CodegenProperty>();
            for (var v : parent.allVars) {
                if (notContainsProp(v, model.vars)) {
                    processGenericAnnotations(v, useBeanValidation, isGenerateHardNullable(), false, false, false, false, false, false);
                    parentVars.add(v);
                }
            }
            model.parentVars = parentVars;
            processParentModel(parent, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars);
        }

        if (parentIsOneOfInterface) {
            for (var variable : parent.vars) {
                if (notContainsProp(variable, model.vars)) {
                    if (parent.discriminator != null && parent.discriminator.getPropertyName().equals(variable.name)) {
                        variable.isDiscriminator = true;
                        variable.isOverridden = true;
                        removePropIfContains(variable, requiredVarsWithoutDiscriminator);
                        removePropIfContains(variable, requiredParentVarsWithoutDiscriminator);
                    }
                    model.vars.add(variable.clone());
                }
            }
            for (var variable : parent.requiredVars) {
                if (notContainsProp(variable, model.requiredVars)) {
                    model.requiredVars.add(variable);
                }
            }
            model.parentModel = null;
            model.parent = null;
            model.parentVars = null;
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
                if (notContainsProp(v, requiredParentVarsWithoutDiscriminator)) {
                    requiredParentVarsWithoutDiscriminator.add(v);
                }
            }
        }
    }

    private void removePropIfContains(CodegenProperty prop, List<CodegenProperty> props) {
        removePropIfContains(prop.name, props);
    }

    private void removePropIfContains(String propName, List<CodegenProperty> props) {
        if (props == null || props.isEmpty()) {
            return;
        }
        props.removeIf(p -> propName.equals(p.name));
    }

    private CodegenProperty findProp(CodegenProperty prop, List<CodegenProperty> props) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        for (var p : props) {
            if (prop.name.equals(p.name)) {
                return p;
            }
        }
        return null;
    }

    private boolean notContainsProp(CodegenProperty prop, List<CodegenProperty> props) {
        for (var p : props) {
            if (prop.name.equals(p.name)) {
                return false;
            }
        }
        return true;
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
        var model = allModels.get(p.getDataType());
        if (model == null && p.dataType != null) {
            model = allModels.get(p.dataType.toLowerCase(Locale.ENGLISH));
        }
        if (!p.vendorExtensions.containsKey("baseType")) {
            p.vendorExtensions.put("baseType", p.isContainer ? p.dataType : p.baseType);
        }

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            model != null ? model.requiredVars : null, groovy, false);
    }

    protected String getPropertyExampleValue(CodegenProperty p, boolean groovy) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");
        var model = allModels.get(p.getDataType());
        if (model == null && p.dataType != null) {
            model = allModels.get(p.dataType.toLowerCase(Locale.ENGLISH));
        }
        if (!p.vendorExtensions.containsKey("baseType")) {
            p.vendorExtensions.put("baseType", p.isContainer ? p.dataType : p.baseType);
        }

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
            example = NULL_STRING;
        } else if ("OffsetDateTime".equals(dataType)) {
            example = "OffsetDateTime.of(2001, 2, 3, 12, 0, 0, 0, java.time.ZoneOffset.of(\"+02:00\"))";
        } else if ("LocalDate".equals(dataType)) {
            example = "LocalDate.of(2001, 2, 3)";
        } else if ("LocalDateTime".equals(dataType)) {
            example = "LocalDateTime.of(2001, 2, 3, 4, 5)";
        } else if ("OffsetDateTime".equals(dataType)) {
            example = "OffsetDateTime.of(2001, 2, 3, 12, 0, 0, 0, java.time.ZoneOffset.of(\"+02:00\"))";
        } else if ("ZonedDateTime".equals(dataType)) {
            example = "ZonedDateTime.of(2001, 2, 3, 12, 0, 0, 0, java.time.ZoneOffset.of(\"+02:00\"))";
        } else if ("MultipartBody".equals(dataType)) {
            example = "MultipartBody.builder().build()";
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
                example = NULL_STRING;
            } else {
                if (requiredPropertiesInConstructor) {
                    var builder = new StringBuilder();
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
            example = NULL_STRING;
        }

        return example;
    }

    public String escapeTextGroovy(String text) {
        if (text == null) {
            return null;
        }
        return escapeText(text).replace("'", "\"");
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

    public void setJsonIncludeAlwaysForRequiredFields(boolean jsonIncludeAlwaysForRequiredFields) {
        this.jsonIncludeAlwaysForRequiredFields = jsonIncludeAlwaysForRequiredFields;
    }

    @Override
    public void setUseOneOfInterfaces(Boolean useOneOfInterfaces) {
        super.setUseOneOfInterfaces(useOneOfInterfaces);
        additionalProperties.put("useOneOfInterfaces", useOneOfInterfaces);
    }

    @Override
    public boolean getUseInlineModelResolver() {
        return false;
    }

    public void setGenerateSwaggerAnnotations(boolean generateSwaggerAnnotations) {
        this.generateSwaggerAnnotations = Boolean.toString(generateSwaggerAnnotations);
        if (generateSwaggerAnnotations) {
            additionalProperties.put("generateSwagger2Annotations", true);
        }
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        additionalProperties.put(OPT_DATE_FORMAT, dateFormat);
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        additionalProperties.put(OPT_DATE_TIME_FORMAT, dateTimeFormat);
    }

    public void setUseEnumCaseInsensitive(boolean useEnumCaseInsensitive) {
        this.useEnumCaseInsensitive = useEnumCaseInsensitive;
    }

    public void setRequiredPropertiesInConstructor(boolean requiredPropertiesInConstructor) {
        this.requiredPropertiesInConstructor = requiredPropertiesInConstructor;
    }

    @Override
    public void postProcess() {
        // disable output donation suggestion
    }
}
