package io.micronaut.openapi.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.openapitools.codegen.CodegenConstants.APIS;
import static org.openapitools.codegen.CodegenConstants.API_TESTS;
import static org.openapitools.codegen.CodegenConstants.MODELS;
import static org.openapitools.codegen.CodegenConstants.SUPPORTING_FILES;

class KotlinMicronautClientCodegenTest extends AbstractMicronautCodegenTest {

    @Test
    void clientOptsUniqueness() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.processOpts();

        var openApi = new OpenAPI();
        openApi.addServersItem(new Server().url("https://one.com/v2"));
        openApi.setInfo(new Info());
        codegen.preprocessOpenAPI(openApi);

        assertEquals(Boolean.FALSE, codegen.additionalProperties().get(CodegenConstants.HIDE_GENERATION_TIMESTAMP));
        assertFalse(codegen.isHideGenerationTimestamp());
        assertEquals("org.openapitools.model", codegen.modelPackage());
        assertEquals("org.openapitools.model", codegen.additionalProperties().get(CodegenConstants.MODEL_PACKAGE));
        assertEquals("org.openapitools.api", codegen.apiPackage());
        assertEquals("org.openapitools.api", codegen.additionalProperties().get(CodegenConstants.API_PACKAGE));
        assertEquals("org.openapitools", codegen.getPackageName());
        assertEquals("org.openapitools", codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE));
    }

    @Test
    void testApiAndModelFilesPresent() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String apiFolder = outputPath + "src/main/kotlin/org/test/test/api/";
        assertFileExists(apiFolder + "PetApi.kt");
        assertFileExists(apiFolder + "StoreApi.kt");
        assertFileExists(apiFolder + "UserApi.kt");

        String modelFolder = outputPath + "src/main/kotlin/org/test/test/model/";
        assertFileExists(modelFolder + "Pet.kt");
        assertFileExists(modelFolder + "User.kt");
        assertFileExists(modelFolder + "Order.kt");
    }

    @Test
    void doConfigureAuthParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files generated
        assertFileExists(outputPath + "/src/main/kotlin/org/openapitools/auth/Authorization.kt");
        // Endpoints are annotated with @Authorization Bindable
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Authorization");
    }

    @Test
    void doNotConfigureAuthParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files are not generated
        assertFileDoesntExist(outputPath + "/src/main/kotlin/org/openapitools/auth/");
        assertFileNotContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Authorization");
    }

    @Test
    void doUseValidationParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files are not generated
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Valid");
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files are not generated
        assertFileNotContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Valid");
        assertFileNotContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_TEST, KotlinMicronautClientCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, true, SUPPORTING_FILES, APIS, MODELS, API_TESTS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/kotlin/");
        assertFileExists(outputPath + "src/test/kotlin/org/openapitools/api/PetApiTest.kt");
        assertFileContains(outputPath + "src/test/kotlin/org/openapitools/api/PetApiTest.kt", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.kt",
            """
                data class Pet(
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    var name: String,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PHOTO_URLS)
                    var photoUrls: List<@NotNull String>,
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_ID)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var id: Long? = null,
                    @field:Nullable
                    @field:Valid
                    @field:JsonProperty(JSON_PROPERTY_CATEGORY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var category: Category? = null,
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TAGS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var tags: List<@Valid Tag>? = null,
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_STATUS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var status: PetStatus? = null,
                ) {
                """);
    }

    @Test
    void doGenerateMultipleContentTypes() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/content-type.yml");

        // body and response content types should be properly annotated using @Consumes and @Produces micronaut annotations
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "DefaultApi.kt", "@Consumes(\"application/vnd.oracle.resource+json; type=collection\", \"application/vnd.oracle.resource+json; type=error\")");
        assertFileContains(apiPath + "DefaultApi.kt", "@Produces(\"application/vnd.oracle.resource+json; type=singular\")");
    }

    @Test
    void testAdditionalClientTypeAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, "@SuppressWarnings(\"someWarning\");@java.lang.Deprecated(since=\"\\${badVersion}\",forRemoval=true);");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt",
            "@SuppressWarnings(\"someWarning\")", "@java.lang.Deprecated(since=\"\\${badVersion}\",forRemoval=true)");
    }

    @Deprecated
    @Test
    void testAdditionalClientTypeAnnotationsFromSetter() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setAdditionalClientTypeAnnotations(List.of("@SuppressWarnings(\"someWarning\")", "@java.lang.Deprecated(since=\"\\${badVersion}\",forRemoval=true)"));
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt",
            "@SuppressWarnings(\"someWarning\")", "@java.lang.Deprecated(since=\"\\${badVersion}\",forRemoval=true)");
    }

    @Test
    void testDefaultAuthorizationFilterPattern() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut AuthorizationFilter should default to match all patterns
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/auth/AuthorizationFilter.kt", "@Filter(patterns = [Filter.MATCH_ALL_PATTERN])");
    }

    @Test
    void testAuthorizationFilterPattern() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.AUTHORIZATION_FILTER_PATTERN, "pet/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut AuthorizationFilter should match the provided pattern
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/auth/AuthorizationFilter.kt", "@Filter(patterns = [\"pet/**\"])");
    }

    @Test
    void testNoConfigureClientId() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should not specify a Client id
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"\\${openapi-micronaut-client.base-path}\")");
    }

    @Test
    void testConfigureClientId() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.CLIENT_ID, "unit-test");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"unit-test\")");
    }

    @Test
    void testConfigureClientIdWithPath() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.CLIENT_ID, "unit-test");
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CLIENT_PATH, true);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(id = \"unit-test\", path = \"\\${unit-test.base-path}\")");
    }

    @Test
    void testDefaultPathSeparator() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should use the default path separator
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"\\${openapi-micronaut-client.base-path}\")");
    }

    @Test
    void testConfigurePathSeparator() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.BASE_PATH_SEPARATOR, "-");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Micronaut declarative http client should use the provided path separator
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"\\${openapi-micronaut-client-base-path}\")");
    }

    @Test
    void testReadOnlyConstructorBug() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.kt",
            """
                open class BookInfo(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    open var name: String,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_REQUIRED_READ_ONLY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var requiredReadOnly: String? = null,
                
                    @field:Nullable
                    @field:Size(min = 3)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var author: String? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_OPTIONAL_READ_ONLY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var optionalReadOnly: String? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TYPE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var type: BookInfoType? = null,
                ) {
                """);
        assertFileContains(apiPath + "ExtendedBookInfo.kt",
            """
                class ExtendedBookInfo(
                
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_NAME)
                    name: String,
                
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_REQUIRED_READ_ONLY)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    requiredReadOnly: String? = null,
                
                    @Nullable
                    @Size(min = 3)
                    @JsonProperty(JSON_PROPERTY_AUTHOR)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    author: String? = null,
                
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_OPTIONAL_READ_ONLY)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    optionalReadOnly: String? = null,
                
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_TYPE)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    type: BookInfoType? = null,
                ) : BookInfo(name, requiredReadOnly, author, optionalReadOnly, type) {
                """);
    }

    @Test
    void testAddValidAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithlist.yml", true, SUPPORTING_FILES, APIS, MODELS, API_TESTS);
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BooksContainer.kt",
            """
                    @field:JsonProperty(JSON_PROPERTY_BOOKS)
                    var books: List<@Valid Book>
                """);
    }

    @Test
    void testGenericAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BooksApi.kt",
            "requestBody: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>",
            "@QueryValue(\"before\") @NotNull @Format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\") before: ZonedDateTime,"
        );
        assertFileContains(modelPath + "CountsContainer.kt", "var counts: List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull ZonedDateTime>>>");
        assertFileContains(modelPath + "BooksContainer.kt", "var books: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>");
    }

    @Test
    void testDiscriminatorConstructorBug() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.kt",
            """
                open class BookInfo(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    open var name: String,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_TYPE)
                    open var type: BookInfoType? = null,
                ) {
                """);
        assertFileContains(apiPath + "BasicBookInfo.kt",
            """
                open class BasicBookInfo(
                
                    @field:NotNull
                    @field:Size(min = 3)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    open var author: String,
                
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_NAME)
                    name: String,
                
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_TYPE)
                    type: BookInfoType? = null,
                ) : BookInfo(name, type) {
                """);
        assertFileContains(apiPath + "DetailedBookInfo.kt",
            """
                class DetailedBookInfo(
                
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                
                    @NotNull
                    @Size(min = 3)
                    @JsonProperty(JSON_PROPERTY_AUTHOR)
                    author: String,
                
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_NAME)
                    name: String,
                
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_TYPE)
                    type: BookInfoType? = null,
                ) : BasicBookInfo(author, name, type) {
                """);
    }

    @Test
    void testDifferentPropertyCase() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/propWithSecondUpperCaseChar.yml");
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(
            modelPath + "Book.kt",
            "const val JSON_PROPERTY_TITLE = \"tItle\"",
            "var title: String,",
            "const val JSON_PROPERTY_I_S_B_N = \"ISBN\"",
            "var ISBN: String? = null,"
        );
    }

    @Test
    void testEnumsWithNonStringTypeValue() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum.yml");
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(modelPath + "StringEnum.kt", "@JsonProperty(\"starting\")", "STARTING(\"starting\"),",
            "val VALUE_MAPPING = entries.associateBy { it.value }",
            """
                    fun fromValue(value: String): StringEnum {
                        require(VALUE_MAPPING.containsKey(value)) { "Unexpected value '$value'" }
                        return VALUE_MAPPING[value]!!
                    }
                """);
        assertFileContains(modelPath + "IntEnum.kt", "@JsonProperty(\"1\")", "NUMBER_1(1),");
        assertFileContains(modelPath + "LongEnum.kt", "@JsonProperty(\"1\")", "NUMBER_3(3L),");
        assertFileContains(modelPath + "DecimalEnum.kt", "@JsonProperty(\"1.23\")", "NUMBER_34_DOT_1(BigDecimal(\"34.1\"))");
    }

    @Test
    void testReservedWords() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/kotlinReservedWords.yml", true,
            APIS,
            MODELS,
            SUPPORTING_FILES,
            CodegenConstants.MODEL_TESTS,
            CodegenConstants.MODEL_DOCS,
            API_TESTS,
            CodegenConstants.API_DOCS
        );
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/ParametersApi.kt", "fun callInterface(",
            "@QueryValue(\"name\") @NotNull @Valid name: Class,",
            "@QueryValue(\"data\") @NotNull `data`: String",
            "): Mono<Void>");
        assertFileContains(path + "model/Class.kt",
            "Class.JSON_PROPERTY_DATA",
            "@field:JsonProperty(JSON_PROPERTY_DATA)",
            "var `data`: String,");
    }

    @Test
    void testControllerEnums2() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum2.yml");
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(modelPath + "BusinessCardsApi.kt", "@QueryValue(\"statusCodes\") @Nullable @Format(FORMAT_MULTI) statusCodes: List<@NotNull String>?");
    }

    @Test
    void testCommonPathParametersWithRef() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.kt", "@Get(\"/v1/forecast/{id}\")",
            "@PathVariable(\"id\") @NotNull id: String,",
            "@QueryValue(\"hourly\") @Nullable hourly: List<V1ForecastIdGetHourlyParameterInner>? = null,",
            "@QueryValue(\"daily\") @Nullable @Format(FORMAT_MULTI) daily: List<V1ForecastIdGetDailyParameterInner>? = null,"
        );

        assertFileContains(path + "model/V1ForecastIdGetHourlyParameterInner.kt",
            "enum class V1ForecastIdGetHourlyParameterInner(",
            "@JsonProperty(\"temperature_2m\")",
            "TEMPERATURE_2M(\"temperature_2m\"),");
    }

    @Test
    void testExtraAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/BooksApi.kt",
            """
                    @Post("/add-book")
                    @NotBlank
                    fun addBook(
                """);

        assertFileContains(path + "model/Book.kt",
            """
                @io.micronaut.serde.annotation.Serdeable.Serializable
                data class Book(
                    @field:NotNull
                    @field:Size(max = 10)
                    @field:JsonProperty(JSON_PROPERTY_TITLE)
                    @field:jakarta.validation.constraints.NotBlank
                    @set:NotEmpty
                    var title: String,
                """);
    }

    @Test
    void testOneOf() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/Subject.kt", "val typeCode: String?");
        assertFileContains(path + "model/Person.kt", "override var typeCode: String? = \"PERS\",");
    }

    @Test
    void testOneOfWithoutDiscriminator() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-without-discriminator.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "model/ShoppingNotesDTO.kt", "@JsonIgnoreProperties(",
            "@JsonTypeInfo"
        );
    }

    @Test
    void testDiscriminatorCustomType() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/CancellationReasonTypesV2.kt", """
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_VERSION)
                override var version: Int? = null,
            """);
        assertFileContains(path + "model/CancellationReasonTypesDTO.kt", "val version: Int?");
    }

    @Test
    void testUuidWithModelNameSuffix() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setModelNameSuffix("Dto");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/schema-with-uuid.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/OrderDTODto.kt", "var id: UUID,");
    }

    @Test
    void testParamsWithDefaultValue() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            "@QueryValue(\"ids\") @Nullable @Format(FORMAT_MULTI) ids: List<@NotNull Int>? = null,",
            "@Header(\"X-Favor-Token\") @Nullable xFavorToken: String? = null,",
            "@PathVariable(\"apiVersion\", defaultValue = \"v5\") @NotNull apiVersion: BrowseSearchOrdersApiVersionParameter = BrowseSearchOrdersApiVersionParameter.V5,",
            "@Header(\"Content-Type\", defaultValue = \"application/json\") @Nullable contentType: String? = \"application/json\"",
            "@QueryValue(\"algorithm\") @Nullable algorithm: BrowseSearchOrdersAlgorithmParameter? = null"
        );
    }

    @Test
    void testFileDownloadEndpoint() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/file-download.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.kt", """
                fun fetchData(
                    @PathVariable("id") @NotNull @Min(0L) id: Long,
                ): Mono<HttpResponse<ByteBuffer<*>>>
            """);
    }

    @Test
    void testSingleProduceContentType() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/client-produces-content-type.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/FilesApi.kt", "@Produces(\"application/octet-stream\")");
    }

    @Test
    void testImplicitHeaders() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setImplicitHeaders(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "api/DefaultApi.kt", "@Header(\"X-Favor-Token\") @Nullable xFavorToken: String? = null,",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable contentType: String? = \"application/json\""
        );
    }

    @Test
    void testImplicitHeadersRegex() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setImplicitHeadersRegex(".*");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "api/DefaultApi.kt", "@Header(\"X-Favor-Token\") @Nullable xFavorToken: String? = null,",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable contentType: String? = \"application/json\""
        );
    }

    @Test
    void testDiscriminatorWithoutUseOneOfInterfaces() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminator2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/JsonOp.kt",
            """
                open class JsonOp(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PATH)
                    open var path: String,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_OP)
                    open var op: String,
                ) {
                """
        );

        assertFileNotContains(path + "model/JsonOp.kt",
            "var `value`: String? = null",
            "var from: String"
        );

        assertFileContains(path + "model/OpAdd.kt",
            """
                class OpAdd(
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_PATH)
                    path: String,
                
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_OP)
                    op: String,
                ) : JsonOp(path, op) {
                """
        );
    }

    @Test
    void testDiscriminatorWithUseOneOfInterfaces() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminator2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/JsonOp.kt",
            """
                interface JsonOp {
                
                    val op: String?
                }
                """
        );

        assertFileContains(path + "model/OpAdd.kt",
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PATH)
                    var path: String,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_OP)
                    override var op: String? = null,
                """
        );
    }

    @Test
    void testMultipartFormData() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipartdata.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/ResetPasswordApi.kt", """
                @Produces("multipart/form-data")
                fun profilePasswordPost(
                    @Header("WCToken") @NotNull wcToken: String,
                    @Header("WCTrustedToken") @NotNull wcTrustedToken: String,
                    @Body @Nullable multipartBody: MultipartBody? = null,
                ): Mono<SuccessResetPassword>
            """);
    }

    @Test
    void testGenerateByMultipleFiles() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple/swagger.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/CustomerApi.kt",
            """
                    @Post("/api/customer/{id}/files")
                    fun uploadFile(
                        @PathVariable("id") @NotNull id: UUID,
                        @Body @NotNull @Valid fileCreateDto: FileCreateDto,
                    ): Mono<HttpResponse<String>>
                """);
        assertFileContains(path + "model/FileCreateDto.kt",
            """
                data class FileCreateDto(
                
                    /**
                     * Customer type ORG
                     */
                    @field:NotNull
                    @field:Pattern(regexp = "^ORG$")
                    @field:JsonProperty(JSON_PROPERTY_TYPE_CODE)
                    var typeCode: String = "ORG",
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_ORG_NAME)
                    var orgName: String,
                ) {
                """);
    }

    @Test
    void testMultipleContentTypesEndpoints() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple-content-types.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt", """
                    @Post("/multiplecontentpath")
                    @Produces("application/json", "application/xml")
                    fun myOp(
                        @Body @Nullable @Valid coordinates: Coordinates? = null,
                    ): Mono<HttpResponse<Void>>
                """,
            """
                    @Post("/multiplecontentpath")
                    @Produces("multipart/form-data")
                    fun myOp_1(
                        @Nullable @Valid coordinates: Coordinates? = null,
                        file: ByteArray? = null,
                    ): Mono<HttpResponse<Void>>
                """,
            """
                    @Post("/multiplecontentpath")
                    @Produces("application/yaml", "text/json")
                    fun myOp_2(
                        @Body @Nullable @Valid mySchema: MySchema? = null,
                    ): Mono<HttpResponse<Void>>
                """);
    }

    @Test
    void testUseEnumCaseInsensitive() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseEnumCaseInsensitive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/StringEnum.kt",
            "val VALUE_MAPPING = entries.associateBy { it.value.lowercase() }",
            """
                    fun fromValue(value: String): StringEnum {
                        val key = value.lowercase()
                        require(VALUE_MAPPING.containsKey(key)) { "Unexpected value '$key'" }
                        return VALUE_MAPPING[key]!!
                    }
                """);

        assertFileContains(path + "model/DecimalEnum.kt",
            "val VALUE_MAPPING = entries.associateBy { it.value }",
            """
                    fun fromValue(value: BigDecimal): DecimalEnum {
                        require(VALUE_MAPPING.containsKey(value)) { "Unexpected value '$value'" }
                        return VALUE_MAPPING[value]!!
                    }
                """);
    }

    @Test
    void testAdditionalAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        String clientTypeAnnotation = "@io.micronaut.validation.Validated";
        String modelTypeAnnotation = "@com.fasterxml.jackson.annotation.JsonClassDescription";
        String oneOfTypeAnnotation = "@io.micronaut.core.annotation.Introspected";
        String enumTypeAnnotation = "@io.swagger.v3.oas.annotations.media.Schema";

        codegen.setAdditionalClientTypeAnnotations(List.of(clientTypeAnnotation));
        codegen.setAdditionalModelTypeAnnotations(List.of(modelTypeAnnotation));
        codegen.setAdditionalOneOfTypeAnnotations(List.of(oneOfTypeAnnotation));
        codegen.setAdditionalEnumTypeAnnotations(List.of(enumTypeAnnotation));
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/SubjectsApi.kt", clientTypeAnnotation);
        assertFileContains(path + "model/Person.kt", modelTypeAnnotation);
        assertFileContains(path + "model/Subject.kt", oneOfTypeAnnotation);
        assertFileContains(path + "model/PersonSex.kt", enumTypeAnnotation);
    }

    @Test
    void testAdditionalAnnotations2() {
        var codegen = new KotlinMicronautClientCodegen();
        String clientTypeAnnotation = "@io.micronaut.validation.Validated";
        String modelTypeAnnotation = "@com.fasterxml.jackson.annotation.JsonClassDescription";
        String oneOfTypeAnnotation = "@io.micronaut.core.annotation.Introspected";
        String enumTypeAnnotation1 = "@io.micronaut.core.annotation.Introspected";
        String enumTypeAnnotation2 = "@io.swagger.v3.oas.annotations.media.Schema";
        String enumTypeAnnotation3 = "@com.fasterxml.jackson.annotation.JsonClassDescription";
        codegen.additionalProperties().putAll(Map.of(
            "additionalClientTypeAnnotations", List.of(clientTypeAnnotation),
            "additionalModelTypeAnnotations", List.of(modelTypeAnnotation),
            "additionalOneOfTypeAnnotations", List.of(oneOfTypeAnnotation),
            "additionalEnumTypeAnnotations", "%s;%s;\n%s;".formatted(enumTypeAnnotation1, enumTypeAnnotation2, enumTypeAnnotation3)
        ));
        codegen.processOpts();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/SubjectsApi.kt", clientTypeAnnotation);
        assertFileContains(path + "model/Person.kt", modelTypeAnnotation);
        assertFileContains(path + "model/Subject.kt", oneOfTypeAnnotation);
        assertFileContains(path + "model/PersonSex.kt", enumTypeAnnotation1 + "\n", enumTypeAnnotation2 + "\n", enumTypeAnnotation3 + "\n");
    }

    @Test
    void testEnumsExtensions() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(modelPath + "BytePrimitiveEnum.kt",
            "NUMBER_1(1),",
            "@get:JsonValue val value: Byte");

        assertFileContains(modelPath + "CharPrimitiveEnum.kt",
            "A('a'),",
            "@get:JsonValue val value: Char");

        assertFileContains(modelPath + "ShortPrimitiveEnum.kt",
            "NUMBER_1(1),",
            "@get:JsonValue val value: Short");

        assertFileContains(modelPath + "IntPrimitiveEnum.kt",
            "NUMBER_1(1),",
            "@get:JsonValue val value: Int");

        assertFileContains(modelPath + "LongPrimitiveEnum.kt",
            "NUMBER_1(1L),",
            "@get:JsonValue val value: Long");

        assertFileContains(modelPath + "FloatPrimitiveEnum.kt",
            "NUMBER_1_DOT_23(1.23F),",
            "@get:JsonValue val value: Float");

        assertFileContains(modelPath + "DoublePrimitiveEnum.kt",
            "NUMBER_1_DOT_23(1.23),",
            "@get:JsonValue val value: Double");

        assertFileContains(modelPath + "StringEnum.kt",
            """
                    @Deprecated("")
                    @JsonProperty("starting")
                    STARTING("starting"),
                """,
            """
                    @Deprecated("")
                    @JsonProperty("running")
                    RUNNING("running"),
                """);

        assertFileContains(modelPath + "DecimalEnum.kt",
            """
                    @Deprecated("")
                    @JsonProperty("34.1")
                    NUMBER_34_DOT_1(BigDecimal("34.1")),
                    ;
                """);

        assertFileContains(modelPath + "ByteEnum.kt",
            "NUMBER_1(1),",
            "@get:JsonValue val value: Byte");

        assertFileContains(modelPath + "ShortEnum.kt",
            "NUMBER_1(1),",
            "@get:JsonValue val value: Short");

        assertFileContains(modelPath + "IntEnum.kt",
            """
                    /**
                     * This is one
                     */
                    @JsonProperty("1")
                    THE_ONE(1),
                """,
            """
                    @Deprecated("")
                    @JsonProperty("2")
                    THE_TWO(2),
                """,
            """
                    /**
                     * This is three
                     */
                    @JsonProperty("3")
                    THE_THREE(3),
                """
        );

        assertFileContains(modelPath + "LongEnum.kt",
            """
                    @Deprecated("")
                    @JsonProperty("2")
                    NUMBER_2(2L),
                """);
    }

    @Test
    void testPrimitives() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/model-with-primitives.yml");
        String basePath = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(basePath + "api/ParametersApi.kt",
            "@QueryValue(\"name\") @NotNull name: String",
            "@QueryValue(\"byteType\") @NotNull byteType: Byte",
            "@QueryValue(\"byteType2\") @NotNull byteType2: Byte",
            "@QueryValue(\"shortType\") @NotNull shortType: Short",
            "@QueryValue(\"shortType2\") @NotNull shortType2: Short",
            "@QueryValue(\"intType\") @NotNull intType: Int",
            "@QueryValue(\"longType\") @NotNull longType: Long",
            "@QueryValue(\"boolType\") @NotNull boolType: Boolean",
            "@QueryValue(\"decimalType\") @NotNull decimalType: BigDecimal",
            "@QueryValue(\"floatType\") @NotNull floatType: Float",
            "@QueryValue(\"doubleType\") @NotNull doubleType: Double",
            "@QueryValue(\"bytePrimitiveType\") @NotNull bytePrimitiveType: Byte",
            "@QueryValue(\"shortPrimitiveType\") @NotNull shortPrimitiveType: Short",
            "@QueryValue(\"intPrimitiveType\") @NotNull intPrimitiveType: Int",
            "@QueryValue(\"longPrimitiveType\") @NotNull longPrimitiveType: Long",
            "@QueryValue(\"floatPrimitiveType\") @NotNull floatPrimitiveType: Float",
            "@QueryValue(\"doublePrimitiveType\") @NotNull doublePrimitiveType: Double",
            "@QueryValue(\"charPrimitiveType\") @NotNull charPrimitiveType: Char",
            "@QueryValue(\"bytePrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) bytePrimitiveTypes: List<@NotNull Byte>",
            "@QueryValue(\"shortPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) shortPrimitiveTypes: List<@NotNull Short>",
            "@QueryValue(\"intPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) intPrimitiveTypes: List<@NotNull Int>",
            "@QueryValue(\"longPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) longPrimitiveTypes: List<@NotNull Long>",
            "@QueryValue(\"floatPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) floatPrimitiveTypes: List<@NotNull Float>",
            "@QueryValue(\"doublePrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) doublePrimitiveTypes: List<@NotNull Double>",
            "@QueryValue(\"charPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) charPrimitiveTypes: List<@NotNull Char>",
            "@QueryValue(\"byteTypes\") @NotNull @Format(FORMAT_MULTI) byteTypes: List<@NotNull Byte>",
            "@QueryValue(\"byteTypes2\") @NotNull @Format(FORMAT_MULTI) byteTypes2: List<@NotNull Byte>",
            "@QueryValue(\"shortTypes\") @NotNull @Format(FORMAT_MULTI) shortTypes: List<@NotNull Short>",
            "@QueryValue(\"shortTypes2\") @NotNull @Format(FORMAT_MULTI) shortTypes2: List<@NotNull Short>",
            "@QueryValue(\"intTypes\") @NotNull @Format(FORMAT_MULTI) intTypes: List<@NotNull Int>",
            "@QueryValue(\"longTypes\") @NotNull @Format(FORMAT_MULTI) longTypes: List<@NotNull Long>"
        );

        assertFileContains(basePath + "model/Obj.kt",
            "name: String?",
            "byteType: Byte?",
            "byteType2: Byte?",
            "shortType: Short?",
            "shortType2: Short?",
            "intType: Int?",
            "longType: Long?",
            "boolType: Boolean?",
            "decimalType: BigDecimal?",
            "floatType: Float?",
            "doubleType: Double?",
            "bytePrimitiveType: Byte?",
            "shortPrimitiveType: Short?",
            "intPrimitiveType: Int?",
            "longPrimitiveType: Long?",
            "floatPrimitiveType: Float?",
            "doublePrimitiveType: Double?",
            "charPrimitiveType: Char?",
            "bytePrimitiveTypes: List<@NotNull Byte>?",
            "shortPrimitiveTypes: List<@NotNull Short>?",
            "intPrimitiveTypes: List<@NotNull Int>?",
            "longPrimitiveTypes: List<@NotNull Long>?",
            "floatPrimitiveTypes: List<@NotNull Float>?",
            "doublePrimitiveTypes: List<@NotNull Double>?",
            "charPrimitiveTypes: List<@NotNull Char>?",
            "byteTypes: List<@NotNull Byte>?",
            "byteTypes2: List<@NotNull Byte>?",
            "shortTypes: List<@NotNull Short>?",
            "shortTypes2: List<@NotNull Short>",
            "intTypes: List<@NotNull Int>",
            "longTypes: List<@NotNull Long>"
        );
    }

    @Test
    void testDeprecated() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/deprecated.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/ParametersApi.kt",
            """
                    /**
                     * A method to send primitives as request parameters
                     *
                     * @param name (required)
                     *        Deprecated: Deprecated message2
                     * @param age (required)
                     * @param height (required)
                     *        Deprecated: Deprecated message4
                     *
                     * @return Success (status code 200)
                     *         or An unexpected error has occurred (status code default)
                     *
                     * @deprecated Deprecated message1
                     */
                    @Deprecated("Deprecated message1")
                    @Operation(
                        operationId = "sendPrimitives",
                        description = "A method to send primitives as request parameters",
                        responses = [
                            ApiResponse(responseCode = "200", description = "Success", content = [
                                Content(mediaType = "application/json", schema = Schema(implementation = SendPrimitivesResponse::class)),
                            ]),
                            ApiResponse(responseCode = "default", description = "An unexpected error has occurred"),
                        ],
                        parameters = [
                            Parameter(name = "name", deprecated = true, required = true, `in` = ParameterIn.PATH),
                            Parameter(name = "age", required = true, `in` = ParameterIn.QUERY),
                            Parameter(name = "height", deprecated = true, required = true, `in` = ParameterIn.HEADER),
                        ],
                    )
                    @Get("/sendPrimitives/{name}")
                    fun sendPrimitives(
                        @PathVariable("name") @NotNull @java.lang.Deprecated name: String,
                        @QueryValue("age") @NotNull age: BigDecimal,
                        @Header("height") @NotNull @java.lang.Deprecated height: Float,
                    ): Mono<SendPrimitivesResponse>
                """);

        assertFileContains(path + "model/SendPrimitivesResponse.kt",
            """
                /**
                 * SendPrimitivesResponse
                 *
                 * @deprecated Deprecated message5
                 */
                @Deprecated("Deprecated message5")
                """,
            """
                    /**
                     * @deprecated Deprecated message6
                     */
                    @Deprecated("Deprecated message6")
                    @field:Nullable
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED, deprecated = true)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var name: String? = null,
                """);

        assertFileContains(path + "model/StateEnum.kt",
            """
                /**
                 * Gets or Sets StateEnum
                 *
                 * @param value The value represented by this enum
                 *
                 * @deprecated  Deprecated message9
                 */
                @Deprecated("Deprecated message9")
                @Serdeable
                @Generated("io.micronaut.openapi.generator.KotlinMicronautClientCodegen")
                enum class StateEnum(
                """
        );
    }

    @Test
    void testCustomValidationMessages() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/validation-messages.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/BooksApi.kt",
            """
                @QueryValue("emailParam") @NotNull @Format(FORMAT_MULTI) emailParam: List<@Email(regexp = "email@dot.com", message = "This is email pattern message") @Size(min = 5, max = 10, message = "This is min max email length message") @NotNull(message = "This is required email message") String>,
                """,
            """
                @QueryValue("strParam") @NotNull @Format(FORMAT_MULTI) strParam: List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String>,
                """,
            """
                @QueryValue("strParam2") @NotNull @Format(FORMAT_MULTI) strParam2: List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, message = "This is min max string length message") @NotNull(message = "This is required string message") String>,
                """,
            """
                @QueryValue("strParam3") @NotNull @Format(FORMAT_MULTI) strParam3: List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String>,
                """,
            """
                @QueryValue("intParam") @NotNull @Format(FORMAT_MULTI) intParam: List<@NotNull(message = "This is required int message") @Min(5, message = "This is min message") @Max(10, message = "This is max message") Int>,
                """,
            """
                @QueryValue("decimalParam") @NotNull @Format(FORMAT_MULTI) decimalParam: List<@NotNull(message = "This is required decimal message") @DecimalMin("5.5", message = "This is decimal min message") @DecimalMax("10.5", message = "This is decimal max message") BigDecimal>,
                """,
            """
                @QueryValue("decimalParam2") @NotNull(message = "This is required param message") @Format(FORMAT_MULTI) decimalParam2: List<@NotNull(message = "This is required decimal message") @DecimalMin("5.5", inclusive = false, message = "This is decimal min message") @DecimalMax("10.5", inclusive = false, message = "This is decimal max message") BigDecimal>,
                """,
            """
                @QueryValue("positiveParam") @NotNull @Format(FORMAT_MULTI) positiveParam: List<@NotNull(message = "This is required int message") @Positive(message = "This is positive message") Int>,
                """,
            """
                @QueryValue("positiveOrZeroParam") @NotNull @Format(FORMAT_MULTI) positiveOrZeroParam: List<@NotNull(message = "This is required int message") @PositiveOrZero(message = "This is positive or zero message") Int>,
                """,
            """
                @QueryValue("negativeParam") @NotNull @Format(FORMAT_MULTI) negativeParam: List<@NotNull(message = "This is required int message") @Negative(message = "This is negative message") Int>,
                """,
            """
                @QueryValue("negativeOrZeroParam") @NotNull @Format(FORMAT_MULTI) negativeOrZeroParam: List<@NotNull(message = "This is required int message") @NegativeOrZero(message = "This is negative or zero message") Int>,
                """);

        assertFileContains(path + "model/Book.kt",
            """
                    @field:NotNull(message = "This is required string message")
                    @field:Pattern(regexp = "[a-zA-Z ]+", message = "This is string pattern message")
                    @field:Size(min = 5, max = 10, message = "This is min max string length message")
                    @field:JsonProperty(JSON_PROPERTY_STR_PROP)
                    var strProp: String,
                """,
            """
                    @field:NotNull(message = "This is required email message")
                    @field:Size(min = 5, max = 10, message = "This is min max email length message")
                    @field:Email(regexp = "email@dot.com", message = "This is email pattern message")
                    @field:JsonProperty(JSON_PROPERTY_EMAIL_PROP)
                    var emailProp: String,
                """,
            """
                    @field:NotNull(message = "This is required int message")
                    @field:Min(5, message = "This is min message")
                    @field:Max(10, message = "This is max message")
                    @field:JsonProperty(JSON_PROPERTY_INT_PROP)
                    var intProp: Int,
                """,
            """
                    @field:Nullable
                    @field:Pattern(regexp = "[a-zA-Z ]+", message = "This is string pattern message")
                    @field:Size(min = 5, message = "This is min string length message")
                    @field:JsonProperty(JSON_PROPERTY_STR_PROP2)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var strProp2: String? = null,
                """,
            """
                    @field:Nullable
                    @field:Pattern(regexp = "[a-zA-Z ]+", message = "This is string pattern message")
                    @field:Size(max = 10, message = "This is min string length message")
                    @field:JsonProperty(JSON_PROPERTY_STR_PROP3)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var strProp3: String? = null,
                """,
            """
                    @field:Nullable
                    @field:Min(0, message = "This is positive message")
                    @field:JsonProperty(JSON_PROPERTY_POSITIVE_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var positiveProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:Min(0, message = "This is positive or zero message")
                    @field:JsonProperty(JSON_PROPERTY_POSITIVE_OR_ZERO_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var positiveOrZeroProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:Max(0, message = "This is negative message")
                    @field:JsonProperty(JSON_PROPERTY_NEGATIVE_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var negativeProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:Max(0, message = "This is negative or zero message")
                    @field:JsonProperty(JSON_PROPERTY_NEGATIVE_OR_ZERO_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var negativeOrZeroProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:DecimalMin("5.5", message = "This is decimal min message")
                    @field:DecimalMax("10.5", message = "This is decimal max message")
                    @field:JsonProperty(JSON_PROPERTY_DECIMAL_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var decimalProp: BigDecimal? = null,
                """,
            """
                    @field:Nullable
                    @field:DecimalMin("5.5", inclusive = false, message = "This is decimal min message")
                    @field:DecimalMax("10.5", inclusive = false, message = "This is decimal max message")
                    @field:JsonProperty(JSON_PROPERTY_DECIMAL_PROP2)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var decimalProp2: BigDecimal? = null,
                """,
            """
                    @field:Nullable
                    @field:Size(min = 5, max = 10, message = "This is min max string length message")
                    @field:JsonProperty(JSON_PROPERTY_ARRAY_PROP1)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var arrayProp1: List<@NotNull Int>? = null,
                """,
            """
                    @field:Nullable
                    @field:Size(min = 5, message = "This is min max string length message")
                    @field:JsonProperty(JSON_PROPERTY_ARRAY_PROP2)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var arrayProp2: List<@NotNull Int>? = null,
                """,
            """
                    @field:Nullable
                    @field:Size(max = 10, message = "This is min max string length message")
                    @field:JsonProperty(JSON_PROPERTY_ARRAY_PROP3)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var arrayProp3: List<@NotNull Int>? = null,
                """
        );
    }

    @Test
    void testNoVars() {
        System.setProperty("micronaut.test.no-vars", "true");

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/Book.kt",
            """
                @Serdeable
                @Generated("io.micronaut.openapi.generator.KotlinMicronautClientCodegen")
                @io.micronaut.serde.annotation.Serdeable.Serializable
                class Book {
                }
                """);

        System.clearProperty("micronaut.test.no-vars");
    }

    @Test
    void testSwaggerAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/petstore.json");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/PetApi.kt",
            """
                    @Operation(
                        operationId = "findPetsByStatus",
                        summary = "Finds Pets by status",
                        description = "Multiple status values can be provided with comma separated strings",
                        responses = [
                            ApiResponse(responseCode = "200", description = "successful operation", content = [
                                Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = Pet::class))),
                                Content(mediaType = "application/xml", array = ArraySchema(schema = Schema(implementation = Pet::class))),
                            ]),
                            ApiResponse(responseCode = "400", description = "Invalid status value"),
                        ],
                        parameters = [
                            Parameter(name = "status", description = "Status values that need to be considered for filter", `in` = ParameterIn.QUERY),
                        ],
                        security = [
                            SecurityRequirement(name = "petstore_auth", scopes = ["write:pets", "read:pets"]),
                        ],
                    )
                    @Get("/pet/findByStatus")
                    @Consumes("application/json", "application/xml")
                    fun findPetsByStatus(
                        @QueryValue("status", defaultValue = "available") @Nullable @Format(FORMAT_MULTI) status: List<@NotNull String>? = arrayListOf("available"),
                    ): Mono<List<Pet>>
                """);
    }

    @Test
    void testDiscriminatorOverride() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/test-override-discriminator.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/AnimalRequest.kt",
            """
                    @field:Nullable
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var name: String? = null,
                
                    @field:Nullable
                    @field:Schema(name = "valueType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_VALUE_TYPE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var valueType: String? = null,
                """);

        assertFileContains(path + "model/AnimalResponse.kt",
            """
                    @field:Nullable
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var name: String? = null,
                
                    @field:Nullable
                    @field:Schema(name = "valueType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_VALUE_TYPE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var valueType: String? = null,
                """);
    }

    @Test
    void testEquals() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/check-equals.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "model/SalesInvoiceCreateDto.kt", "@JsonPropertyOrder");
        assertFileContains(path + "model/SalesInvoiceCreateDto.kt", """
                override fun equals(other: Any?): Boolean {
                    if (this === other) {
                        return true
                    }
                    if (javaClass != other?.javaClass) {
                        return false
                    }
                    other as SalesInvoiceCreateDto
                    return super.equals(other)
                }
            
                override fun hashCode(): Int =
                    Objects.hash(super.hashCode())
            """);
    }

    @Test
    void testOptionalQueryValues() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/optional-controller-values.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            """
                   @Post("/sendPrimitives/{name}")
                   fun sendPrimitives(
                       @PathVariable("name") @NotNull name: String,
                       @QueryValue("brand") @Nullable brand: String? = null,
                       @CookieValue("coc") @Nullable coc: String? = null,
                       @Header("head") @Nullable head: String? = null,
                       @Body @Nullable body: String? = null,
                   ): Mono<String>
                """);
    }

    @Test
    void testBodyEnum() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/body-enum.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/MyCustomApi.kt", """
                @Post("/api/v1/colors/{name}")
                fun selectColor(
                    @Body @NotNull body: Color,
                ): Mono<String>
            """);
        assertFileContains(path + "model/Color.kt", "enum class Color(");
    }

    @Test
    void testDateWithoutSizeAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DocumentResourcesApi.kt", """
            @QueryValue("CREATIONDATE") @Nullable CREATIONDATE: LocalDate? = null,
            """);
        assertFileContains(path + "model/Result.kt", """
                var id: String? = null,
            
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_DATE)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                var date: ZonedDateTime? = null,
            """);
    }

    @Test
    void testJsonIncludeAlways() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setJsonIncludeAlwaysForRequiredFields(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/json-include-always.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/TextRequestDto.kt",
            """
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_TEXT)
                    @field:JsonInclude(JsonInclude.Include.ALWAYS)
                    var text: String,
                """,
            """
                    @field:NotNull
                    @field:Size(min = 1, max = 20)
                    @field:JsonProperty(JSON_PROPERTY_LOCALE)
                    @field:JsonInclude(JsonInclude.Include.ALWAYS)
                    var locale: String,
                """);

        assertFileContains(path + "model/ResponseDto.kt",
            """
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_MESSAGE)
                    @field:JsonInclude(JsonInclude.Include.ALWAYS)
                    var message: String,
                """,
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TIMESTAMP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var timestamp: ZonedDateTime? = null,
                """);
    }

    @Test
    void testBuiltInModelNamePrefixAndSuffix() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setModelNamePrefix("Api");
        codegen.setModelNameSuffix("Dto");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openapi-built-in-prefix.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            """
                    @Get("/example-route2")
                    fun exampleRoute2Get(): Mono<Set<UUID>>
                """,
            """
                    @Get("/example-route")
                    fun exampleRouteGet(): Mono<Set<String>>
                """);
    }

    static Stream<Arguments> sealedScenarios() {
        return Stream.of(
            arguments("oneOf_polymorphismAndInheritance.yml", Map.of(
                "Bar.kt", """
                    class Bar(
                    
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_ID)
                        id: String? = null,
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_BAR_PROP_A)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var barPropA: String? = null,
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_FOO_PROP_B)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var fooPropB: String? = null,
                    
                        @field:Nullable
                        @field:Valid
                        @field:JsonProperty(JSON_PROPERTY_FOO)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var foo: FooRefOrValue? = null,
                    
                        /**
                         * When sub-classing, this defines the sub-class Extensible name
                         */
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_AT_TYPE)
                        atType: String? = null,
                    
                        /**
                         * Hyperlink reference
                         */
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_HREF)
                        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        href: String? = null,
                    
                        /**
                         * A URI to a JSON-Schema file that defines additional attributes and relationships
                         */
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
                        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        atSchemaLocation: String? = null,
                    
                        /**
                         * When sub-classing, this defines the super-class
                         */
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
                        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        atBaseType: String? = null,
                    ) : Entity(atType, href, id, atSchemaLocation, atBaseType), BarRefOrValue {
                    """,
                "Banana.kt", """
                    data class Banana(
                    
                        @field:NotNull
                        @field:JsonProperty(JSON_PROPERTY_LENGTH)
                        var length: Int,
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_FRUIT_TYPE)
                        override var fruitType: FruitType? = null,
                    ) : Fruit {
                    """,
                "Entity.kt", """
                    open class Entity(
                    
                        /**
                         * When sub-classing, this defines the sub-class Extensible name
                         */
                        @field:NotNull
                        @field:JsonProperty(JSON_PROPERTY_AT_TYPE)
                        open var atType: String? = null,
                    
                        /**
                         * Hyperlink reference
                         */
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_HREF)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        open var href: String? = null,
                    
                        /**
                         * unique identifier
                         */
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_ID)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        open var id: String? = null,
                    
                        /**
                         * A URI to a JSON-Schema file that defines additional attributes and relationships
                         */
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        open var atSchemaLocation: String? = null,
                    
                        /**
                         * When sub-classing, this defines the super-class
                         */
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        open var atBaseType: String? = null,
                    ) {
                    """)),
            arguments("oneOf_additionalProperties.yml", Map.of(
                "SchemaA.kt", """
                    data class SchemaA(
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_PROP_A)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var propA: String? = null,
                    ) : PostRequest {
                    """,
                "PostRequest.kt", "interface PostRequest")),
            arguments("oneOf_array.yml", Map.of(
                "OneOf1.kt", "data class OneOf1(")),
            arguments("oneOf_duplicateArray.yml", Map.of(
                "Example.kt", "interface Example")),
            arguments("oneOf_nonPrimitive.yml", Map.of(
                "Example.kt", "interface Example")),
            arguments("oneOf_primitive.yml", Map.of(
                "Child.kt", """
                    data class Child(
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_NAME)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var name: String? = null,
                    ) : Example {
                    """,
                "Example.kt", "interface Example")),
            arguments("oneOf_primitiveAndArray.yml", Map.of(
                "Example.kt", "interface Example")),
            arguments("oneOf_reuseRef.yml", Map.of(
                "Fruit.kt", "interface Fruit {",
                "Banana.kt", """
                    data class Banana(
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_LENGTH_CM)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var lengthCm: BigDecimal? = null,
                    
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_FRUIT_TYPE)
                        override var fruitType: String? = null,
                    ) : Fruit {
                    """,
                "Apple.kt", """
                    data class Apple(
                    
                        @field:Nullable
                        @field:Pattern(regexp = "^[a-zA-Z\\\\s]*$")
                        @field:JsonProperty(JSON_PROPERTY_CULTIVAR)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var cultivar: String? = null,
                    
                        @field:Nullable
                        @field:Pattern(regexp = "/^[A-Z\\\\s]*$/i")
                        @field:JsonProperty(JSON_PROPERTY_ORIGIN)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var origin: String? = null,
                    
                        @Nullable
                        @JsonProperty(JSON_PROPERTY_FRUIT_TYPE)
                        override var fruitType: String? = null,
                    ) : Fruit {
                    """)),
            arguments("oneOf_twoPrimitives.yml", Map.of(
                "MyExamplePostRequest.kt", "interface MyExamplePostRequest")),
            arguments("oneOf_arrayMapImport.yml", Map.of(
                "Fruit.kt", "interface Fruit",
                "Grape.kt", """
                    data class Grape(
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_COLOR)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var color: String? = null,
                    ) {
                    """,
                "Apple.kt", """
                    data class Apple(
                    
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_KIND)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var kind: String? = null,
                    ) {
                    """)),
            arguments("oneOf_discriminator.yml", Map.of(
                "FruitAllOfDisc.kt", "interface FruitAllOfDisc {",
                "FruitReqDisc.kt", "interface FruitReqDisc {"))
        );
    }

    @MethodSource("sealedScenarios")
    @ParameterizedTest
    public void sealedScenarios(String apiFile, Map<String, String> definitions) {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/sealed/" + apiFile);
        String path = outputPath + "src/main/kotlin/org/openapitools/model/";

        definitions.forEach((file, check) -> assertFileContains(path + file, check));
    }

    @Test
    void testCoroutines() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setCoroutines(true);
        codegen.setReactive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            """
                    @Get("/{apiVersion}/orders")
                    suspend fun browseSearchOrders(
                        @PathVariable("apiVersion", defaultValue = "v5") @NotNull apiVersion: BrowseSearchOrdersApiVersionParameter = BrowseSearchOrdersApiVersionParameter.V5,
                        @QueryValue("ids") @Nullable @Format(FORMAT_MULTI) ids: List<@NotNull Int>? = null,
                        @Header("X-Favor-Token") @Nullable xFavorToken: String? = null,
                        @Header("Content-Type", defaultValue = "application/json") @Nullable contentType: String? = "application/json",
                        @QueryValue("algorithm") @Nullable algorithm: BrowseSearchOrdersAlgorithmParameter? = null,
                    ): String
                """
        );
    }

    @Test
    void testEnumXimplements() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum-implements.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/Type.kt",
            """
                enum class Type(
                    @get:JsonValue val value: String,
                ) : java.io.Serializable {
                """
        );
    }

    @Test
    void testPropsWithDefaultValueAreOptionalKsp() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setKsp(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/default-value-optional.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            "@QueryValue(\"reqParamWithDefault\", defaultValue = \"test-req\") @NotNull reqParamWithDefault: String = \"test-req\",",
            "@QueryValue(\"reqParam\") @NotNull reqParam: String,",
            "@Body @NotNull @Valid teSTRequest: TESTRequest,",
            "@QueryValue(\"optParamWithDefault\", defaultValue = \"test\") optParamWithDefault: String = \"test\",",
            "@QueryValue(\"optParam\") @Nullable optParam: String? = null,"
        );

        assertFileContains(path + "model/TESTRequest.kt",
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                
                    @field:JsonProperty(JSON_PROPERTY_CURRENCY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var currency: MyCur = MyCur.USD,
                """
        );
    }

    @Test
    void testPropsWithDefaultValueAreOptionalKapt() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/default-value-optional.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            "@QueryValue(\"reqParamWithDefault\", defaultValue = \"test-req\") @NotNull reqParamWithDefault: String = \"test-req\",",
            "@QueryValue(\"reqParam\") @NotNull reqParam: String,",
            "@Body @NotNull @Valid teSTRequest: TESTRequest,",
            "@QueryValue(\"optParamWithDefault\", defaultValue = \"test\") @Nullable optParamWithDefault: String? = \"test\",",
            "@QueryValue(\"optParam\") @Nullable optParam: String? = null,"
        );

        assertFileContains(path + "model/TESTRequest.kt",
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_CURRENCY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var currency: MyCur? = MyCur.USD,
                """
        );
    }

    @Test
    void testParamWithStyle() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-style.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            "import io.micronaut.core.convert.converters.MultiValuesConverterFactory.*",
            "@QueryValue(\"fields\") @NotNull @Format(FORMAT_MULTI) fields: Map<String, @NotNull String>,",
            "@QueryValue(\"fieldsCsv\") @NotNull fieldsCsv: Map<String, @NotNull String>,",
            "@QueryValue(\"fieldsSpace\") @NotNull @Format(FORMAT_SSV) fieldsSpace: Map<String, @NotNull String>,",
            "@QueryValue(\"fieldsPipes\") @NotNull @Format(FORMAT_PIPES) fieldsPipes: Map<String, @NotNull String>,",
            "@QueryValue(\"fieldsDeepObject\") @NotNull @Format(FORMAT_DEEP_OBJECT) fieldsDeepObject: Map<String, @NotNull String>,"
        );
    }


    @Test
    void testUseBasicAuth() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setUseBasicAuth(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "auth/config/ConfigurableAuthorization.kt");
        assertFileExists(path + "auth/config/ApiKeyAuthConfig.kt");
        assertFileDoesntExist(path + "auth/config/HttpBasicAuthConfig.kt");
        assertFileExists(path + "auth/AuthorizationFilter.kt");
    }

    @Test
    void testUseApiKeyAuth() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setUseApiKeyAuth(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "auth/config/ConfigurableAuthorization.kt");
        assertFileDoesntExist(path + "auth/config/ApiKeyAuthConfig.kt");
        assertFileExists(path + "auth/config/HttpBasicAuthConfig.kt");
        assertFileExists(path + "auth/AuthorizationFilter.kt");
    }

    @Test
    void testAuthFilter() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setAuthFilter(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "auth/config/ConfigurableAuthorization.kt");
        assertFileExists(path + "auth/config/ApiKeyAuthConfig.kt");
        assertFileExists(path + "auth/config/HttpBasicAuthConfig.kt");
        assertFileDoesntExist(path + "auth/AuthorizationFilter.kt");
    }

    @Test
    void testGenerateAuthClasses() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setGenerateAuthClasses(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, false);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileDoesntExist(path + "auth/config/ConfigurableAuthorization.kt");
        assertFileDoesntExist(path + "auth/config/ApiKeyAuthConfig.kt");
        assertFileDoesntExist(path + "auth/config/HttpBasicAuthConfig.kt");
        assertFileDoesntExist(path + "auth/AuthorizationFilter.kt");
        assertFileDoesntExist(path + "auth/Authorizations.kt");
        assertFileDoesntExist(path + "auth/AuthorizationBinder.kt");
        assertFileDoesntExist(path + "auth/Authorization.kt");
    }

    @Test
    void testAuthWithClientIdAndMultiplePatterns() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.kt",
            "@Filter(serviceId = [\"myApiClient\"], patterns = [\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"])");
        assertFileContains(path + "auth/AuthorizationBinder.kt",
            "const val AUTHORIZATION_NAMES = \"micronaut.security.myApiClient.AUTHORIZATION_NAMES\"");
        assertFileContains(path + "auth/config/ApiKeyAuthConfig.kt",
            "@EachProperty(\"security.myApiClient.api-key-auth\")");
        assertFileContains(path + "auth/config/HttpBasicAuthConfig.kt",
            "@EachProperty(\"security.myApiClient.basic-auth\")");
    }

    @Test
    void testAuthWithAuthConfigName() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthConfigName("test");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.kt",
            "@Filter(serviceId = [\"myApiClient\"], patterns = [Filter.MATCH_ALL_PATTERN])");
        assertFileContains(path + "auth/AuthorizationBinder.kt",
            "const val AUTHORIZATION_NAMES = \"micronaut.security.test.AUTHORIZATION_NAMES\"");
        assertFileContains(path + "auth/config/ApiKeyAuthConfig.kt",
            "@EachProperty(\"security.test.api-key-auth\")");
        assertFileContains(path + "auth/config/HttpBasicAuthConfig.kt",
            "@EachProperty(\"security.test.basic-auth\")");
    }

    @Test
    void testAuthWithAuthFilterClientIds() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.kt",
            "@Filter(serviceId = [\"test1\", \"test2\", \"test3\"], patterns = [\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"])");
    }

    @Test
    void testAuthWithAuthFilterExcludedClientIds() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterExcludedClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.kt",
            "@Filter(serviceId = [\"myApiClient\"], excludeServiceId = [\"test1\", \"test2\", \"test3\"], patterns = [\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"])");
    }

    @Test
    void testAuthWithAuthFilterExcludedClientIdsAndEmptyAuthFilterClientIds() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterClientIds(List.of());
        codegen.setAuthFilterExcludedClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.kt",
            "@Filter(excludeServiceId = [\"test1\", \"test2\", \"test3\"], patterns = [\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"])");
    }

    @Test
    void testAuthWithAuthorizationFilterPatternStyle() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterClientIds(List.of());
        codegen.setAuthFilterExcludedClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        codegen.setAuthorizationFilterPatternStyle("regex");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.kt",
            "import io.micronaut.http.filter.FilterPatternStyle",
            "@Filter(excludeServiceId = [\"test1\", \"test2\", \"test3\"], patternStyle = FilterPatternStyle.REGEX, patterns = [\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"])");
    }

    @Test
    void testJvmOverloadsAndJvmRecordAndJavaCompatibility() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setJvmOverloads(true);
        codegen.setJvmRecord(true);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(modelPath + "Pet.kt",
            """
                @JvmRecord
                data class Pet @JvmOverloads constructor(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    val name: String,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PHOTO_URLS)
                    val photoUrls: List<@NotNull String>,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_ID)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    val id: Long? = null,
                
                    @field:Nullable
                    @field:Valid
                    @field:JsonProperty(JSON_PROPERTY_CATEGORY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    val category: Category? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TAGS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    val tags: List<@Valid Tag>? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_STATUS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    val status: PetStatus? = null,
                ) {
                """);
        assertFileContains(modelPath + "OrderStatus.kt",
            """
                        @JvmField
                        val VALUE_MAPPING = entries.associateBy { it.value }
                """,
            """
                        @JvmStatic
                        @JsonCreator
                        fun fromValue(value: String): OrderStatus {
                """
        );
    }

    @Test
    void testJvmOverloadsAndNotJavaCompatibility() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setJvmOverloads(true);
        codegen.setJvmRecord(false);
        codegen.setJavaCompatibility(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileNotContains(modelPath + "Pet.kt", "@JvmRecord");
        assertFileContains(modelPath + "Pet.kt",
            """
                data class Pet @JvmOverloads constructor(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    var name: String,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PHOTO_URLS)
                    var photoUrls: List<@NotNull String>,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_ID)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var id: Long? = null,
                
                    @field:Nullable
                    @field:Valid
                    @field:JsonProperty(JSON_PROPERTY_CATEGORY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var category: Category? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TAGS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var tags: List<@Valid Tag>? = null,
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_STATUS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var status: PetStatus? = null,
                ) {
                """);
        assertFileNotContains(modelPath + "OrderStatus.kt", "@JvmField", "@JvmStatic");
    }

    @Test
    void testJvmOverloadsWithoutDefaultValues() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setJvmOverloads(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml");
        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(modelPath + "CurrentWeather.kt",
            """
                data class CurrentWeather(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_TIME)
                    var time: String,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_TEMPERATURE)
                    var temperature: Float,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_WINDSPEED)
                    var windspeed: Float,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_WINDDIRECTION)
                    var winddirection: Float,
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_WEATHERCODE)
                    var weathercode: Int,
                ) {
                """);
        assertFileContains(modelPath + "DailyResponse.kt", "data class DailyResponse @JvmOverloads constructor(");
    }

    @Test
    void testJvmRecordWithInheritance() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setJvmRecord(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml");
        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(modelPath + "BookInfo.kt",
            """
                @JsonSubTypes(
                    JsonSubTypes.Type(ExtendedBookInfo::class, name = "EXTENDED"),
                )
                open class BookInfo(
                
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    open val name: String,
                """);
        assertFileContains(modelPath + "ExtendedBookInfo.kt",
            """
                @Generated("io.micronaut.openapi.generator.KotlinMicronautClientCodegen")
                class ExtendedBookInfo(
                
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    val isbn: String,
                """);
    }

    @Test
    void testEnumConvertersConfig() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/config/";

        assertFileExists(path + "EnumConverterClientConfig.kt");

        assertFileContains(path + "EnumConverterClientConfig.kt",
            "import org.openapitools.model.BytePrimitiveEnum",
            "import org.openapitools.model.CharPrimitiveEnum",
            "import org.openapitools.model.DecimalEnum",
            "import org.openapitools.model.DoubleEnum",
            "import org.openapitools.model.DoublePrimitiveEnum",
            "import org.openapitools.model.FloatEnum",
            "import org.openapitools.model.FloatPrimitiveEnum",
            "import org.openapitools.model.IntEnum",
            "import org.openapitools.model.IntPrimitiveEnum",
            "import org.openapitools.model.LongEnum",
            "import org.openapitools.model.LongPrimitiveEnum",
            "import org.openapitools.model.ShortPrimitiveEnum",
            "import org.openapitools.model.StringEnum",
            "class EnumConverterClientConfig {",
            """
                    @Bean
                    fun toEnumStringEnum() = TypeConverter<String, StringEnum> { v, _, _ -> Optional.of(StringEnum.fromValue(v)) }
                """,
            """
                    @Bean
                    fun toStrStringEnum() = TypeConverter<StringEnum, String> { v, _, _ -> Optional.of(v.value) }
                """,
            """
                    @Bean
                    fun toEnumIntEnum() = TypeConverter<String, IntEnum> { v, _, _ -> Optional.of(IntEnum.fromValue(v.toInt())) }
                """,
            """
                    @Bean
                    fun toStrIntEnum() = TypeConverter<IntEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumLongEnum() = TypeConverter<String, LongEnum> { v, _, _ -> Optional.of(LongEnum.fromValue(v.toLong())) }
                """,
            """
                    @Bean
                    fun toStrLongEnum() = TypeConverter<LongEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumDecimalEnum() = TypeConverter<String, DecimalEnum> { v, _, _ -> Optional.of(DecimalEnum.fromValue(v.toBigDecimal())) }
                """,
            """
                    @Bean
                    fun toStrDecimalEnum() = TypeConverter<DecimalEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumFloatEnum() = TypeConverter<String, FloatEnum> { v, _, _ -> Optional.of(FloatEnum.fromValue(v.toFloat())) }
                """,
            """
                    @Bean
                    fun toStrFloatEnum() = TypeConverter<FloatEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumDoubleEnum() = TypeConverter<String, DoubleEnum> { v, _, _ -> Optional.of(DoubleEnum.fromValue(v.toDouble())) }
                """,
            """
                    @Bean
                    fun toStrDoubleEnum() = TypeConverter<DoubleEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumBytePrimitiveEnum() = TypeConverter<String, BytePrimitiveEnum> { v, _, _ -> Optional.of(BytePrimitiveEnum.fromValue(v.toByte())) }
                """,
            """
                    @Bean
                    fun toStrBytePrimitiveEnum() = TypeConverter<BytePrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumShortPrimitiveEnum() = TypeConverter<String, ShortPrimitiveEnum> { v, _, _ -> Optional.of(ShortPrimitiveEnum.fromValue(v.toShort())) }
                """,
            """
                    @Bean
                    fun toStrShortPrimitiveEnum() = TypeConverter<ShortPrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumIntPrimitiveEnum() = TypeConverter<String, IntPrimitiveEnum> { v, _, _ -> Optional.of(IntPrimitiveEnum.fromValue(v.toInt())) }
                """,
            """
                    @Bean
                    fun toStrIntPrimitiveEnum() = TypeConverter<IntPrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumLongPrimitiveEnum() = TypeConverter<String, LongPrimitiveEnum> { v, _, _ -> Optional.of(LongPrimitiveEnum.fromValue(v.toLong())) }
                """,
            """
                    @Bean
                    fun toStrLongPrimitiveEnum() = TypeConverter<LongPrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumFloatPrimitiveEnum() = TypeConverter<String, FloatPrimitiveEnum> { v, _, _ -> Optional.of(FloatPrimitiveEnum.fromValue(v.toFloat())) }
                """,
            """
                    @Bean
                    fun toStrFloatPrimitiveEnum() = TypeConverter<FloatPrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumDoublePrimitiveEnum() = TypeConverter<String, DoublePrimitiveEnum> { v, _, _ -> Optional.of(DoublePrimitiveEnum.fromValue(v.toDouble())) }
                """,
            """
                    @Bean
                    fun toStrDoublePrimitiveEnum() = TypeConverter<DoublePrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """,
            """
                    @Bean
                    fun toEnumCharPrimitiveEnum() = TypeConverter<String, CharPrimitiveEnum> { v, _, _ -> Optional.of(CharPrimitiveEnum.fromValue(v[0])) }
                """,
            """
                    @Bean
                    fun toStrCharPrimitiveEnum() = TypeConverter<CharPrimitiveEnum, String> { v, _, _ -> Optional.of(v.value.toString()) }
                """
        );
    }

    @Test
    void testEnumConvertersConfigWithCustomClientId() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setClientId("myApiClient");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/config/";
        assertFileExists(path + "EnumConverterMyApiClientConfig.kt");

        assertFileContains(path + "EnumConverterMyApiClientConfig.kt",
            "import org.openapitools.model.BytePrimitiveEnum",
            "import org.openapitools.model.CharPrimitiveEnum",
            "import org.openapitools.model.DecimalEnum",
            "import org.openapitools.model.DoubleEnum",
            "import org.openapitools.model.DoublePrimitiveEnum",
            "import org.openapitools.model.FloatEnum",
            "import org.openapitools.model.FloatPrimitiveEnum",
            "import org.openapitools.model.IntEnum",
            "import org.openapitools.model.IntPrimitiveEnum",
            "import org.openapitools.model.LongEnum",
            "import org.openapitools.model.LongPrimitiveEnum",
            "import org.openapitools.model.ShortPrimitiveEnum",
            "import org.openapitools.model.StringEnum",
            "class EnumConverterMyApiClientConfig {"
        );
    }

    @Test
    void testEnumConvertersConfigWithoutEnumParams() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/config/";
        assertFileDoesntExist(path + "EnumConverterClientConfig.kt");
    }

    @Test
    void testEnumConvertersConfigDisabled() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setGenerateEnumConverters(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/config/";
        assertFileDoesntExist(path + "EnumConverterClientConfig.kt");
    }

    @Test
    void testHideGenerationTimestamp() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(CodegenConstants.HIDE_GENERATION_TIMESTAMP, false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/file.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(apiPath + "RequestBodyApi.kt", """
            @Generated("io.micronaut.openapi.generator.KotlinMicronautClientCodegen", date =
            """);
    }

    @Test
    void testInheritanceWithInterfaces() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/sealed/oneOf_discriminator.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "model/FruitType.kt");
        assertFileContains(path + "model/FruitType.kt",
            """
                data class FruitType(
                
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_FRUIT_TYPE)
                    override var fruitType: String? = null,
                ) : ComposedDiscRequiredInconsistent, ComposedDiscTypeInconsistent, FruitAnyOfDisc, FruitOneOfDisc {
                """);
    }

    @Test
    void testLocalDateTimeDefaultValue() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setDateTimeLibrary("LOCAL_DATETIME");
        codegen.setModelNameSuffix("Dto");
        codegen.setReactive(false);
        codegen.setConfigureAuthorization(false);
        codegen.setGenerateHttpResponseAlways(false);
        codegen.setGenerateHttpResponseWhereRequired(true);
        codegen.setUseBeanValidation(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/default-local-date-time.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "model/ObjectWithLocalDateTimeDto.kt");
        assertFileContains(path + "model/ObjectWithLocalDateTimeDto.kt",
            "import java.time.LocalDateTime",
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TIMESTAMP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var timestamp: LocalDateTime? = LocalDateTime.parse("2025-05-26T14:00"),
                """
        );
    }

    @Test
    void testZonedDateTimeDefaultValue() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setDateTimeLibrary("ZONED_DATETIME");
        codegen.setModelNameSuffix("Dto");
        codegen.setReactive(false);
        codegen.setConfigureAuthorization(false);
        codegen.setGenerateHttpResponseAlways(false);
        codegen.setGenerateHttpResponseWhereRequired(true);
        codegen.setUseBeanValidation(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/default-zoned-date-time.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "model/ObjectWithZonedDateTimeDto.kt");
        assertFileContains(path + "model/ObjectWithZonedDateTimeDto.kt",
            "import java.time.ZonedDateTime",
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TIMESTAMP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var timestamp: ZonedDateTime? = ZonedDateTime.parse("2007-12-03T10:15:30+01:00"),
                """
        );
    }

    @Test
    void testOffsetDateTimeDefaultValue() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setDateTimeLibrary("OFFSET_DATETIME");
        codegen.setModelNameSuffix("Dto");
        codegen.setReactive(false);
        codegen.setConfigureAuthorization(false);
        codegen.setGenerateHttpResponseAlways(false);
        codegen.setGenerateHttpResponseWhereRequired(true);
        codegen.setUseBeanValidation(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/default-offset-date-time.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "model/ObjectWithOffsetDateTimeDto.kt");
        assertFileContains(path + "model/ObjectWithOffsetDateTimeDto.kt",
            "import java.time.OffsetDateTime",
            """
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_TIMESTAMP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var timestamp: OffsetDateTime? = OffsetDateTime.parse("2007-12-03T10:15:30+01:00"),
                """
        );
    }

    @Test
    void testHtmlDescription() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/html-description.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/MyCustomApi.kt",
            """
                    /**
                     * {@summary Summary {@see <a href='https://support.zendesk.com/hc/en-us/articles/235721587'>Viewing and restoring archived articles</a>}}
                     * Archives the article. You can restore the article using the Help Center user interface. {@see <a href='https://support.zendesk.com/hc/en-us/articles/235721587'>Viewing and restoring archived articles</a>}. Allowed for Agents
                     *
                     * @param locale Param locale description {@see <a href='https://support.zendesk.com/hc/en-us/articles/235721587'>Viewing and restoring archived articles</a>} (required)
                     * @param articleId Param article_id description {@see <a href='https://support.zendesk.com/hc/en-us/articles/235721587'>Viewing and restoring archived articles</a>} (required)
                     * @param body Request body {@see <a href='https://support.zendesk.com/hc/en-us/articles/235721587'>Viewing and restoring archived articles</a>} (required)
                     *
                     * @return Response description {@see <a href='https://support.zendesk.com/hc/en-us/articles/235721587'>Viewing and restoring archived articles</a>} (status code 200)
                     */
                """
        );
    }

    @Test
    void testDollarSign() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setClientId("$myClientId");
        codegen.setClientPath(true);
        codegen.setConfigureAuthorization(true);
        codegen.setGenerateOperationOnlyForFirstTag(true);
        codegen.setAuthFilterClientIds("$myClientId");
        codegen.setAuthFilterExcludedClientIds("$notMyClientId");
        codegen.setBasePathSeparator("$sep");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/dollar-sign.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/Tag1Api.kt",
            """
                @Client(id = "\\$myClientId", path = "\\${\\$myClientId\\$sepbase-path}")
                interface Tag1Api {
                
                    /**
                     * {@summary summary with $dollarSign}
                     * contains $strings
                     *
                     * @param dollarPathVar desc pathVar with $dollarSign (required)
                     * @param schemaTitleWithDollarDollarSign Request body desc with $dollarSign (required)
                     * @param dollarQueryVar desc queryVar with $dollarSign (optional, default to ref2/$ref)
                     *        Deprecated: Deprecated message with $dollarSign
                     * @param dollarHeaderVar desc header with $dollarSign (optional, default to $dollar)
                     * @param dollarCookieVar desc cookie with $dollarSign (optional, default to $dollar)
                     * @param dollarPropDouble (optional)
                     * @param dollarPropLong (optional)
                     * @param dollarPropInt (optional)
                     * @param dollarPropEmail (optional)
                     * @param dollarPropListMinMax (optional)
                     * @param dollarPropListMin (optional)
                     * @param dollarPropListMax (optional)
                     * @param dollarPropStrMinMax (optional)
                     * @param dollarPropStrMin (optional)
                     * @param dollarPropStrMax (optional)
                     * @param dollarPropStrPattern (optional)
                     *
                     * @return desc resp 200 with $dollarSign (status code 200)
                     *         or desc resp 404 with $dollarSign (status code 404)
                     *
                     * @deprecated Deprecated message with $dollarSign
                     *
                     * @see <a href="https://external2.server.com/$dollar-sign">summary with $dollarSign Documentation</a>
                     */
                    @Deprecated("Deprecated message with \\$dollarSign")
                    @Get("/\\$paths/{\\$pathVar}")
                    @Consumes("\\$application/json")
                    @Produces("\\$application/json")
                    @Authorization("\\$notSimpleAuth")
                    @Authorization("\\$OAuth2", scopes = ["\\$read", "\\$write"])
                    fun getStrings(
                        @PathVariable("\\$pathVar", defaultValue = "\\$dollar") @NotNull dollarPathVar: String = "\\$dollar",
                        @Body @NotNull @Valid schemaTitleWithDollarDollarSign: SchemaTitleWithDollarDollarSign,
                        @QueryValue("\\$queryVar", defaultValue = "ref2/\\$ref") @Nullable @java.lang.Deprecated dollarQueryVar: DollarGetDollarStringsDollarQueryVarParameter? = DollarGetDollarStringsDollarQueryVarParameter.REF2__REF,
                        @Header("\\$headerVar", defaultValue = "\\$dollar") @Nullable dollarHeaderVar: String? = "\\$dollar",
                        @CookieValue("\\$cookieVar", defaultValue = "\\$dollar") @Nullable dollarCookieVar: String? = "\\$dollar",
                        @QueryValue("\\$propDouble") @Nullable @DecimalMin("10", message = "Message with \\$dollarSign") @DecimalMax("100", message = "Message with \\$dollarSign") dollarPropDouble: BigDecimal? = null,
                        @QueryValue("\\$propLong") @Nullable @Min(10L, message = "Message with \\$dollarSign") @Max(100L, message = "Message with \\$dollarSign") dollarPropLong: Long? = null,
                        @QueryValue("\\$propInt") @Nullable @Min(10, message = "Message with \\$dollarSign") @Max(100, message = "Message with \\$dollarSign") dollarPropInt: Int? = null,
                        @QueryValue("\\$propEmail") @Nullable @Email(regexp = "poi\\\\.feedback\\\\.Review$0(.)*", message = "Message with \\$dollarSign") dollarPropEmail: String? = null,
                        @QueryValue("\\$propListMinMax") @Nullable @Format(FORMAT_MULTI) dollarPropListMinMax: List<@NotNull String>? = null,
                        @QueryValue("\\$propListMin") @Nullable @Format(FORMAT_MULTI) dollarPropListMin: List<@NotNull String>? = null,
                        @QueryValue("\\$propListMax") @Nullable @Format(FORMAT_MULTI) dollarPropListMax: List<@NotNull String>? = null,
                        @QueryValue("\\$propStrMinMax") @Nullable @Size(min = 10, max = 100, message = "Message with \\$dollarSign") dollarPropStrMinMax: String? = null,
                        @QueryValue("\\$propStrMin") @Nullable @Size(min = 10, message = "Message with \\$dollarSign") dollarPropStrMin: String? = null,
                        @QueryValue("\\$propStrMax") @Nullable @Size(max = 100, message = "Message with \\$dollarSign") dollarPropStrMax: String? = null,
                        @QueryValue("\\$propStrPattern") @Nullable @Pattern(regexp = "poi\\\\.feedback\\\\.Review$0(.)*", message = "Message with \\$dollarSign") dollarPropStrPattern: String? = null,
                    ): Mono<String>
                }
                """
        );
        assertFileContains(path + "auth/config/ApiKeyAuthConfig.kt",
            """
                @EachProperty("security.\\$myClientId.api-key-auth")
                open class ApiKeyAuthConfig @ConfigurationInject constructor(
                """
        );
        assertFileContains(path + "auth/config/HttpBasicAuthConfig.kt",
            """
                @EachProperty("security.\\$myClientId.basic-auth")
                open class HttpBasicAuthConfig @ConfigurationInject constructor(
                """
        );
        assertFileContains(path + "auth/AuthorizationFilter.kt",
            """
                @Filter(serviceId = ["\\$myClientId"], excludeServiceId = ["\\$notMyClientId"], patterns = [Filter.MATCH_ALL_PATTERN])
                open class AuthorizationFilter(
                """
        );
    }

    @Test
    void testEnumNullable() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPathApi = generateFiles(codegen, "src/test/resources/3_0/enum-nullable.yml", false, SUPPORTING_FILES, APIS);
        generateFiles(codegen, "src/test/resources/3_0/enum-nullable.yml", true, MODELS);

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "config/EnumConverterClientConfig.kt", """
                @Bean
                fun toEnumParamEnum() = TypeConverter<String, ParamEnum> { v, _, _ -> Optional.of(ParamEnum.fromValue(v)) }
            
                @Bean
                fun toStrParamEnum() = TypeConverter<ParamEnum, String> { v, _, _ -> Optional.of(v.value) }
            """);
    }

    @Test
    void testEnumExtensions() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPathApi = generateFiles(codegen, "src/test/resources/3_0/enum-extensions.yml");

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "model/EnumWithList.kt", """
                 *
                 * @deprecated This is enum deprecated message
                 */
                @Deprecated("This is enum deprecated message")
                """,
            """
                    /**
                     * doc1
                     */
                    @JsonProperty("CGI")
                    A("CGI"),
                
                    /**
                     *
                     * @deprecated This is deprecated message1
                     */
                    @Deprecated("This is deprecated message1")
                    @JsonProperty("CGU")
                    B("CGU"),
                
                    /**
                     * doc3
                     *
                     * @deprecated This is deprecated message2
                     */
                    @Deprecated("This is deprecated message2")
                    @JsonProperty("BRB")
                    C("BRB"),
                
                    @JsonProperty("NUM")
                    NUM("NUM"),
                """);
        assertFileContains(path + "model/EnumWithMap.kt", """
                 *
                 * @deprecated This is enum deprecated message
                 */
                @Deprecated("This is enum deprecated message")
                """,
            """
                    /**
                     * doc1
                     */
                    @JsonProperty("CGI")
                    A("CGI"),
                
                    /**
                     *
                     * @deprecated This is deprecated message1
                     */
                    @Deprecated("This is deprecated message1")
                    @JsonProperty("CGU")
                    B("CGU"),
                
                    /**
                     * doc3
                     *
                     * @deprecated This is deprecated message2
                     */
                    @Deprecated("This is deprecated message2")
                    @JsonProperty("BRB")
                    C("BRB"),
                
                    @JsonProperty("NUM")
                    NUM("NUM"),
                """);
        assertFileContains(path + "model/EnumWithMap2.kt", """
                 *
                 * @deprecated This is enum deprecated message
                 */
                @Deprecated("This is enum deprecated message")
                """,
            """
                    /**
                     * doc1
                     */
                    @JsonProperty("CGI")
                    A("CGI"),
                
                    /**
                     *
                     * @deprecated This is deprecated message1
                     */
                    @Deprecated("This is deprecated message1")
                    @JsonProperty("CGU")
                    B("CGU"),
                
                    /**
                     * doc3
                     *
                     * @deprecated This is deprecated message2
                     */
                    @Deprecated("This is deprecated message2")
                    @JsonProperty("BRB")
                    C("BRB"),
                
                    @JsonProperty("NUM")
                    NUM("NUM"),
                """);
        assertFileContains(path + "model/EnumWithDifferentExts.kt", """
                 *
                 * @deprecated This is enum deprecated message
                 */
                @Deprecated("This is enum deprecated message")
                """,
            """
                    /**
                     * doc1
                     */
                    @JsonProperty("CGI")
                    A("CGI"),
                
                    /**
                     *
                     * @deprecated This is deprecated message1
                     */
                    @Deprecated("This is deprecated message1")
                    @JsonProperty("CGU")
                    B("CGU"),
                
                    /**
                     * doc3
                     *
                     * @deprecated This is deprecated message2
                     */
                    @Deprecated("This is deprecated message2")
                    @JsonProperty("BRB")
                    C("BRB"),
                
                    @JsonProperty("NUM")
                    NUM("NUM"),
                """);
    }

    @Test
    void testTrueFalse() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPathApi = generateFiles(codegen, "src/test/resources/3_0/true-false.yml");

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/OrderApi.kt", """
                /**
                 * getOrderById
                 *
                 * @param true (optional)
                 * @param false (optional)
                 * @param null (optional)
                 * @param boolean (optional)
                 * @param propertyClass (optional)
                 *
                 * @return OK (status code 200)
                 */
                @Get("/orders/{id}")
                fun getOrderById(
                    @QueryValue("true") @Nullable `true`: String? = null,
                    @QueryValue("false") @Nullable `false`: String? = null,
                    @QueryValue("null") @Nullable `null`: String? = null,
                    @QueryValue("boolean") @Nullable boolean: String? = null,
                    @QueryValue("class") @Nullable propertyClass: String? = null,
                ): Mono<String>
            """);

        assertFileContains(path + "model/CDBAttributeUsageUiBoolean.kt", """
                /**
                 * x
                 */
                @field:NotNull
                @field:JsonProperty(JSON_PROPERTY_TRUE)
                @field:JsonInclude(content = JsonInclude.Include.ALWAYS)
                var `true`: Map<String, Any?>,
            
                /**
                 * x
                 */
                @field:NotNull
                @field:JsonProperty(JSON_PROPERTY_FALSE)
                @field:JsonInclude(content = JsonInclude.Include.ALWAYS)
                var `false`: Map<String, Any?>,
            
                /**
                 * x
                 */
                @field:NotNull
                @field:JsonProperty(JSON_PROPERTY_NULL)
                @field:JsonInclude(content = JsonInclude.Include.ALWAYS)
                var `null`: Map<String, Any?>,
            """);
    }

    @Test
    void testModelImmutable() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setModelMutable(false);
        String outputPathApi = generateFiles(codegen, "src/test/resources/3_0/sealed/oneOf_polymorphismAndInheritance.yml");

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "model/Foo.kt", """
            class Foo(
            
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_FOO_PROP_A)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                val fooPropA: String? = null,
            
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_FOO_PROP_B)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                val fooPropB: String? = null,
            
                /**
                 * When sub-classing, this defines the sub-class Extensible name
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_AT_TYPE)
                atType: String? = null,
            
                /**
                 * Hyperlink reference
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_HREF)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                href: String? = null,
            
                /**
                 * unique identifier
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_ID)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                id: String? = null,
            
                /**
                 * A URI to a JSON-Schema file that defines additional attributes and relationships
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                atSchemaLocation: String? = null,
            
                /**
                 * When sub-classing, this defines the super-class
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                atBaseType: String? = null,
            ) : Entity(atType, href, id, atSchemaLocation, atBaseType), FooRefOrValue {
            """);

        assertFileContains(path + "model/Entity.kt", """
            open class Entity(
            
                /**
                 * When sub-classing, this defines the sub-class Extensible name
                 */
                @field:NotNull
                @field:JsonProperty(JSON_PROPERTY_AT_TYPE)
                open val atType: String? = null,
            
                /**
                 * Hyperlink reference
                 */
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_HREF)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                open val href: String? = null,
            
                /**
                 * unique identifier
                 */
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_ID)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                open val id: String? = null,
            
                /**
                 * A URI to a JSON-Schema file that defines additional attributes and relationships
                 */
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                open val atSchemaLocation: String? = null,
            
                /**
                 * When sub-classing, this defines the super-class
                 */
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                open val atBaseType: String? = null,
            ) {
            """);
    }

    @Test
    void testNonPublicApi() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setNonPublicApi(true);
        String outputPathApi = generateFiles(codegen, PETSTORE_PATH);

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/PetApi.kt", "internal interface PetApi {");
        assertFileContains(path + "model/Category.kt", "internal data class Category(");
        assertFileContains(path + "model/OrderStatus.kt", "internal enum class OrderStatus(");
    }

    @Test
    void testExplicitApi() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setExplicitApi(true);
        String outputPathApi = generateFiles(codegen, PETSTORE_PATH);

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/PetApi.kt", "public interface PetApi {", "public fun addPet(");
        assertFileContains(path + "model/Category.kt", "public data class Category(");
        assertFileContains(path + "model/OrderStatus.kt", "public enum class OrderStatus(", "public val value: String,");
    }

    @Test
    void testIntermediateReference() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPathApi = generateFiles(codegen, "src/test/resources/3_0/intermediate-reference.yml");

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "model/BuiltInClientObjectSort.kt", """
            class BuiltInClientObjectSort(
            
                @field:Nullable
                @field:JsonProperty(JSON_PROPERTY_FIELD)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                var `field`: BuiltInClientObjectSortAllOfField? = null,
            
                /**
                 * Used as a discriminator value between implementations of this type
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_O)
                o: String? = null,
            
                /**
                 * Specifies whether the sort order is ascending or descending. If not specified, ascending sort is assumed.
                 */
                @Nullable
                @JsonProperty(JSON_PROPERTY_ASCENDING)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                ascending: Boolean? = null,
            ) : ObjectSort(o, ascending) {
            """);
    }

    @Test
    void testRetryable() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.retryable = true;
        String outputPathApi = generateFiles(codegen, PETSTORE_PATH);

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/PetApi.kt", """
            @Retryable
            @Client("\\${openapi-micronaut-client.base-path}")
            """,
            "import io.micronaut.retry.annotation.Retryable");
    }

    @Test
    void testRetryableAll() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.retryable = true;
        codegen.retryableIncludes = List.of("java.lang.IllegalAccessException", "java.lang.RuntimeException::class");
        codegen.retryableExcludes = List.of("java.lang.IllegalAccessException", "java.lang.RuntimeException::class");
        codegen.retryableAttempts = 10;
        codegen.retryableDelay = "10s";
        codegen.retryableMaxDelay = "100s";
        codegen.retryableMultiplier = "2.32";
        codegen.retryableJitter = "5.43";
        codegen.retryablePredicate = "io.micronaut.retry.annotation.DefaultRetryPredicate";
        codegen.retryableCapturedException = "Exception";

        String outputPathApi = generateFiles(codegen, PETSTORE_PATH);

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/PetApi.kt", """
            @Retryable(
                java.lang.IllegalAccessException::class, java.lang.RuntimeException::class,
                excludes = [java.lang.IllegalAccessException::class, java.lang.RuntimeException::class],
                attempts = "10",
                delay = "10s",
                maxDelay = "100s",
                multiplier = "2.32",
                jitter = "5.43",
                predicate = io.micronaut.retry.annotation.DefaultRetryPredicate::class,
                capturedException = Exception::class,
            )
            @Client("\\${openapi-micronaut-client.base-path}")
            """);
    }

    @Test
    void testRetryableNotAll() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.retryable = true;
        codegen.retryableAttempts = 10;
        codegen.retryableDelay = "10s";
        codegen.retryableMaxDelay = "100s";
        codegen.retryableMultiplier = "2.32";
        codegen.retryablePredicate = "io.micronaut.retry.annotation.DefaultRetryPredicate";

        String outputPathApi = generateFiles(codegen, PETSTORE_PATH);

        String path = outputPathApi + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/PetApi.kt", """
            @Retryable(
                attempts = "10",
                delay = "10s",
                maxDelay = "100s",
                multiplier = "2.32",
                predicate = io.micronaut.retry.annotation.DefaultRetryPredicate::class,
            )
            @Client("\\${openapi-micronaut-client.base-path}")
            """);
    }

    @Test
    void testAdditionalPropertiesHashMapImport() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/additional-properties.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileNotContains(apiPath + "PatchDto.kt",
                """
                import org.openapitools.model.HashMap
                """
        );

        assertFileContains(apiPath + "PatchDto.kt",
                """
                    data class PatchDto(
    
                        /**
                         * Patch operation
                         */
                        @field:NotNull
                        @field:JsonProperty(JSON_PROPERTY_OP)
                        var op: String,
    
                        /**
                         * Path to update
                         */
                        @field:NotNull
                        @field:JsonProperty(JSON_PROPERTY_PATH)
                        var path: String,
    
                        /**
                         * Value to assign
                         */
                        @field:Nullable
                        @field:JsonProperty(JSON_PROPERTY_VALUE)
                        @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                        var `value`: Any? = null,
                    ) : HashMap<String, Any>() {
    
                    """);
    }
}
