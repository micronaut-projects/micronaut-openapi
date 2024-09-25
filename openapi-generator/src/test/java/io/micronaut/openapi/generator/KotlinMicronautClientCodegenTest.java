package io.micronaut.openapi.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KotlinMicronautClientCodegenTest extends AbstractMicronautCodegenTest {

    @Test
    void clientOptsUnicity() {
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

        OpenAPI openAPI = new OpenAPI();
        openAPI.addServersItem(new Server().url("https://one.com/v2"));
        openAPI.setInfo(new Info());
        codegen.preprocessOpenAPI(openAPI);

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
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS,
            CodegenConstants.MODELS);

        String apiFolder = outputPath + "src/main/kotlin/org/test/test/api/";
        assertFileExists(apiFolder + "PetApi.kt");
        assertFileExists(apiFolder + "StoreApi.kt");
        assertFileExists(apiFolder + "UserApi.kt");

        String modelFolder = outputPath + "src/main/kotlin/org/test/test/model/";
        assertFileExists(modelFolder + "Pet.kt");
        assertFileExists(modelFolder + "User.kt");
        assertFileExists(modelFolder + "Order.kt");

        String resources = outputPath + "src/main/resources/";
        assertFileExists(resources + "application.yml");
    }

    @Test
    void doConfigureAuthParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS);

        // Files generated
        assertFileExists(outputPath + "/src/main/kotlin/org/openapitools/auth/Authorization.kt");
        // Endpoints are annotated with @Authorization Bindable
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Authorization");
    }

    @Test
    void doNotConfigureAuthParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileNotExists(outputPath + "/src/main/kotlin/org/openapitools/auth/");
        assertFileNotContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Authorization");
    }

    @Test
    void doUseValidationParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Valid");
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileNotContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Valid");
        assertFileNotContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_TEST,
            KotlinMicronautClientCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/kotlin/");
        assertFileExists(outputPath + "src/test/kotlin/org/openapitools/api/PetApiTest.kt");
        assertFileContains(outputPath + "src/test/kotlin/org/openapitools/api/PetApiTest.kt", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

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
    void doNotGenerateRequiredPropertiesInConstructor() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";
        assertFileNotContainsRegex(modelPath + "Pet.kt", "public Pet\\([^)]+\\)");
        assertFileNotContainsRegex(modelPath + "User.kt", "public User\\([^)]+\\)");
        assertFileNotContainsRegex(modelPath + "Order.kt", "public Order\\([^)]+\\)");
    }

    @Test
    void doGenerateMultipleContentTypes() {
        var codegen = new KotlinMicronautClientCodegen();

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/content-type.yaml", CodegenConstants.APIS);

        // body and response content types should be properly annotated using @Consumes and @Produces micronaut annotations
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "DefaultApi.kt", "@Consumes(\"application/vnd.oracle.resource+json; type=collection\", \"application/vnd.oracle.resource+json; type=error\")");
        assertFileContains(apiPath + "DefaultApi.kt", "@Produces(\"application/vnd.oracle.resource+json; type=singular\")");
    }

    @Test
    void doGenerateOauth2InApplicationConfig() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/oauth2.yaml", CodegenConstants.SUPPORTING_FILES);

        // micronaut yaml property names shouldn't contain any dots
        String resourcesPath = outputPath + "src/main/resources/";
        assertFileContains(resourcesPath + "application.yml", "OAuth_2_0_Client_Credentials:");
    }

    @Test
    void testAdditionalClientTypeAnnotations() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, "@MyAdditionalAnnotation1(1,${param1});@MyAdditionalAnnotation2(2,${param2});");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt",
            "@MyAdditionalAnnotation1(1,${param1})", "@MyAdditionalAnnotation2(2,${param2})");
    }

    @Test
    void testAdditionalClientTypeAnnotationsFromSetter() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.setAdditionalClientTypeAnnotations(List.of("@MyAdditionalAnnotation1(1,${param1})", "@MyAdditionalAnnotation2(2,${param2})"));
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt",
            "@MyAdditionalAnnotation1(1,${param1})", "@MyAdditionalAnnotation2(2,${param2})");
    }

    @Test
    void testDefaultAuthorizationFilterPattern() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should default to match all patterns
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/auth/AuthorizationFilter.kt", "@Filter(Filter.MATCH_ALL_PATTERN)");
    }

    @Test
    void testAuthorizationFilterPattern() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.AUTHORIZATION_FILTER_PATTERN, "pet/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should match the provided pattern
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/auth/AuthorizationFilter.kt", "@Filter(\"pet/**\")");
    }

    @Test
    void testNoConfigureClientId() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should not specify a Client id
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"\\${openapi-micronaut-client.base-path}\")");
    }

    @Test
    void testConfigureClientId() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.CLIENT_ID, "unit-test");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"unit-test\")");
    }

    @Test
    void testConfigureClientIdWithPath() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.CLIENT_ID, "unit-test");
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.OPT_CLIENT_PATH, true);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(id = \"unit-test\", path = \"\\${unit-test.base-path}\")");
    }

    @Test
    void testDefaultPathSeparator() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the default path separator
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"\\${openapi-micronaut-client.base-path}\")");
    }

    @Test
    void testConfigurePathSeparator() {
        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.BASE_PATH_SEPARATOR, "-");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided path separator
        assertFileContains(outputPath + "/src/main/kotlin/org/openapitools/api/PetApi.kt", "@Client(\"\\${openapi-micronaut-client-base-path}\")");
    }

    @Test
    void testReadOnlyConstructorBug() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml", CodegenConstants.MODELS);
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
                    open var requiredReadOnly: String?,
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
                data class ExtendedBookInfo(
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    override var name: String,
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_REQUIRED_READ_ONLY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    override var requiredReadOnly: String?,
                ): BookInfo(name, requiredReadOnly)  {
                """);
    }

    @Test
    void testAddValidAnnotations() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().put(KotlinMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithlist.yml", CodegenConstants.APIS, CodegenConstants.API_TESTS, CodegenConstants.MODELS);
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml",
            CodegenConstants.MODELS
        );
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
                ) {""");
        assertFileContains(apiPath + "BasicBookInfo.kt",
            """
                open class BasicBookInfo(
                    @field:NotNull
                    @field:Size(min = 3)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    open var author: String,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    override var name: String,
                ): BookInfo(name)  {
                """);
        assertFileContains(apiPath + "DetailedBookInfo.kt",
            """
                data class DetailedBookInfo(
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                    @field:NotNull
                    @field:Size(min = 3)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    override var author: String,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    override var name: String,
                ): BasicBookInfo(author, name)
                """);
    }

    @Test
    void testDifferentPropertyCase() {
        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/propWithSecondUpperCaseChar.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/kotlinReservedWords.yml",
            CodegenConstants.APIS,
            CodegenConstants.MODELS,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.MODEL_TESTS,
            CodegenConstants.MODEL_DOCS,
            CodegenConstants.API_TESTS,
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(modelPath + "BusinessCardsApi.kt", "@QueryValue(\"statusCodes\") @Nullable statusCodes: List<@NotNull String>?");
    }

    @Test
    void testCommonPathParametersWithRef() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.kt", "@Get(\"/v1/forecast/{id}\")",
            "@PathVariable(\"id\") @NotNull id: String,",
            "@QueryValue(\"hourly\") @Nullable hourly: List<V1ForecastIdGetHourlyParameterInner>? = null,");

        assertFileContains(path + "model/V1ForecastIdGetHourlyParameterInner.kt",
            "enum class V1ForecastIdGetHourlyParameterInner(",
            "@JsonProperty(\"temperature_2m\")",
            "TEMPERATURE_2M(\"temperature_2m\"),");
    }

    @Test
    void testExtraAnnotations() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/BooksApi.kt",
            """
                    @Post("/add-book")
                    @NotBlank
                    fun addBook(
                """);

        assertFileContains(path + "model/Book.kt",
            """
                @Serializable
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/Subject.kt", "val typeCode: String");
        assertFileContains(path + "model/Person.kt", "override var typeCode: String = \"PERS\",");
    }

    @Test
    void testOneOfWithoutDiscriminator() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-without-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "model/OrderDTOShoppingNotes.kt", "@JsonIgnoreProperties(",
            "@JsonTypeInfo"
        );
    }

    @Test
    void testDiscriminatorCustomType() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator2.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/CancellationReasonTypesV2.kt", """
                @field:NotNull
                @field:JsonProperty(JSON_PROPERTY_VERSION)
                override var version: Int,
            """);
        assertFileContains(path + "model/CancellationReasonTypesDTO.kt", "val version: Int");
    }

    @Test
    void testUuidWithModelNameSuffix() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setModelNameSuffix("Dto");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/schema-with-uuid.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/OrderDTODto.kt", "var id: UUID,");
    }

    @Test
    void testParamsWithDefaultValue() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            "@QueryValue(\"ids\") @Nullable ids: List<@NotNull Int>? = null,",
            "@Header(\"X-Favor-Token\") @Nullable xFavorToken: String? = null,",
            "@PathVariable(name = \"apiVersion\", defaultValue = \"v5\") @Nullable apiVersion: BrowseSearchOrdersApiVersionParameter? = BrowseSearchOrdersApiVersionParameter.V5,",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable contentType: String? = \"application/json\"",
            "@QueryValue(\"algorithm\") @Nullable algorithm: BrowseSearchOrdersAlgorithmParameter? = null"
        );
    }

    @Test
    void testFileDownloadEndpoint() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/file-download.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.kt", """
                fun fetchData(
                    @PathVariable("id") @NotNull @Min(0L) id: Long
                ): Mono<HttpResponse<ByteBuffer<?>>>
            """);
    }

    @Test
    void testSingleProduceContentType() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/client-produces-content-type.yml", CodegenConstants.APIS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/FilesApi.kt", "@Produces(\"application/octet-stream\")");
    }

    @Test
    void testImplicitHeaders() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setImplicitHeaders(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml", CodegenConstants.APIS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "api/DefaultApi.kt", "@Header(\"X-Favor-Token\") @Nullable xFavorToken: String? = null,",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable contentType: String? = \"application/json\""
        );
    }

    @Test
    void testImplicitHeadersRegex() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setImplicitHeadersRegex(".*");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml", CodegenConstants.APIS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileNotContains(path + "api/DefaultApi.kt", "@Header(\"X-Favor-Token\") @Nullable xFavorToken: String? = null,",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable contentType: String? = \"application/json\""
        );
    }

    @Test
    void testDiscriminatorWithoutUseOneOfInterfaces() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discirminator2.yml", CodegenConstants.MODELS);
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
                data class OpAdd(
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PATH)
                    override var path: String,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_OP)
                    override var op: String,
                ): JsonOp(path, op)  {
                """
        );
    }

    @Test
    void testDiscriminatorWithUseOneOfInterfaces() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discirminator2.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/JsonOp.kt",
            """
                interface JsonOp {
                
                    val op: String
                }
                """
        );

        assertFileContains(path + "model/OpAdd.kt",
            """
                data class OpAdd(
                    @field:Nullable
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_PATH)
                    var path: String,
                    @field:NotNull
                    @field:JsonProperty(JSON_PROPERTY_OP)
                    override var op: String,
                ): JsonOp {
                """
        );
    }

    @Test
    void testMultipartFormData() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipartdata.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/ResetPasswordApi.kt", """
                @Produces("multipart/form-data")
                fun profilePasswordPost(
                    @Header("WCToken") @NotNull wcToken: String,
                    @Header("WCTrustedToken") @NotNull wcTrustedToken: String,
                    @Body @Nullable multipartBody: MultipartBody?
                ): Mono<SuccessResetPassword>
            """);
    }

    @Test
    void testGenerateByMultipleFiles() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple/swagger.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/CustomerApi.kt",
            """
                    @Post("/api/customer/{id}/files")
                    fun uploadFile(
                        @PathVariable("id") @NotNull id: UUID,
                        @Body @NotNull @Valid fileCreateDto: FileCreateDto
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple-content-types.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt", """
                    @Post("/multiplecontentpath")
                    @Produces("application/json", "application/xml")
                    fun myOp(
                        @Body @Nullable @Valid coordinates: Coordinates?
                    ): Mono<HttpResponse<Void>>
                """,
            """
                    @Post("/multiplecontentpath")
                    @Produces("multipart/form-data")
                    fun myOp_1(
                        @Nullable @Valid coordinates: Coordinates?,
                        @Nullable file: ByteArray?
                    ): Mono<HttpResponse<Void>>
                """,
            """
                    @Post("/multiplecontentpath")
                    @Produces("application/yaml", "text/json")
                    fun myOp_2(
                        @Body @Nullable @Valid mySchema: MySchema?
                    ): Mono<HttpResponse<Void>>
                """);
    }

    @Test
    void testUseEnumCaseInsensitive() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setUseEnumCaseInsensitive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
    }

    @Test
    void testAdditionalAnnotations() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.setAdditionalClientTypeAnnotations(List.of("@java.io.MyAnnotation1"));
        codegen.setAdditionalModelTypeAnnotations(List.of("@java.io.MyAnnotation2"));
        codegen.setAdditionalOneOfTypeAnnotations(List.of("@java.io.MyAnnotation3"));
        codegen.setAdditionalEnumTypeAnnotations(List.of("@java.io.MyAnnotation4"));
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/SubjectsApi.kt", "@java.io.MyAnnotation1");
        assertFileContains(path + "model/Person.kt", "@java.io.MyAnnotation2");
        assertFileContains(path + "model/Subject.kt", "@java.io.MyAnnotation3");
        assertFileContains(path + "model/PersonSex.kt", "@java.io.MyAnnotation4");
    }

    @Test
    void testAdditionalAnnotations2() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.additionalProperties().putAll(Map.of(
            "additionalClientTypeAnnotations", List.of("@java.io.MyAnnotation1"),
            "additionalModelTypeAnnotations", List.of("@java.io.MyAnnotation2"),
            "additionalOneOfTypeAnnotations", List.of("@java.io.MyAnnotation3"),
            "additionalEnumTypeAnnotations", "@java.io.MyAnnotation41;@java.io.MyAnnotation42;\n@java.io.MyAnnotation43;"
        ));
        codegen.processOpts();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/SubjectsApi.kt", "@java.io.MyAnnotation1");
        assertFileContains(path + "model/Person.kt", "@java.io.MyAnnotation2");
        assertFileContains(path + "model/Subject.kt", "@java.io.MyAnnotation3");
        assertFileContains(path + "model/PersonSex.kt", "@java.io.MyAnnotation41\n", "@java.io.MyAnnotation42\n", "@java.io.MyAnnotation43\n");
    }

    @Test
    void testEnumsExtensions() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
                    NUMBER_34_DOT_1(BigDecimal("34.1"));
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/model-with-primitives.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
            "@QueryValue(\"bytePrimitiveTypes\") @NotNull bytePrimitiveTypes: List<@NotNull Byte>",
            "@QueryValue(\"shortPrimitiveTypes\") @NotNull shortPrimitiveTypes: List<@NotNull Short>",
            "@QueryValue(\"intPrimitiveTypes\") @NotNull intPrimitiveTypes: List<@NotNull Int>",
            "@QueryValue(\"longPrimitiveTypes\") @NotNull longPrimitiveTypes: List<@NotNull Long>",
            "@QueryValue(\"floatPrimitiveTypes\") @NotNull floatPrimitiveTypes: List<@NotNull Float>",
            "@QueryValue(\"doublePrimitiveTypes\") @NotNull doublePrimitiveTypes: List<@NotNull Double>",
            "@QueryValue(\"charPrimitiveTypes\") @NotNull charPrimitiveTypes: List<@NotNull Char>",
            "@QueryValue(\"byteTypes\") @NotNull byteTypes: List<@NotNull Byte>",
            "@QueryValue(\"byteTypes2\") @NotNull byteTypes2: List<@NotNull Byte>",
            "@QueryValue(\"shortTypes\") @NotNull shortTypes: List<@NotNull Short>",
            "@QueryValue(\"shortTypes2\") @NotNull shortTypes2: List<@NotNull Short>",
            "@QueryValue(\"intTypes\") @NotNull intTypes: List<@NotNull Int>",
            "@QueryValue(\"longTypes\") @NotNull longTypes: List<@NotNull Long>"
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/deprecated.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
                     * @return Mono&lt;SendPrimitivesResponse&gt;
                     *
                     * @deprecated Deprecated message1
                     */
                    @Suppress("DEPRECATED_JAVA_ANNOTATION")
                    @Deprecated("Deprecated message1")
                    @Operation(
                        operationId = "sendPrimitives",
                        description = "A method to send primitives as request parameters",
                        responses = [
                            ApiResponse(responseCode = "200", description = "Success", content = [
                                Content(mediaType = "application/json", schema = Schema(implementation = SendPrimitivesResponse::class))
                            ]),
                            ApiResponse(responseCode = "default", description = "An unexpected error has occurred")
                        ],
                        parameters = [
                            Parameter(name = "name", deprecated = true, required = true, `in` = ParameterIn.PATH),
                            Parameter(name = "age", required = true, `in` = ParameterIn.QUERY),
                            Parameter(name = "height", deprecated = true, required = true, `in` = ParameterIn.HEADER)
                        ]
                    )
                    @Get("/sendPrimitives/{name}")
                    fun sendPrimitives(
                        @PathVariable("name") @NotNull @java.lang.Deprecated name: String,
                        @QueryValue("age") @NotNull age: BigDecimal,
                        @Header("height") @NotNull @java.lang.Deprecated height: Float
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/validation-messages.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/BooksApi.kt",
            """
                @QueryValue("emailParam") @NotNull emailParam: List<@Email(regexp = "email@dot.com", message = "This is email pattern message") @Size(min = 5, max = 10, message = "This is min max email length message") @NotNull(message = "This is required email message") String>,
                """,
            """
                @QueryValue("strParam") @NotNull strParam: List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String>,
                """,
            """
                @QueryValue("strParam2") @NotNull strParam2: List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, message = "This is min max string length message") @NotNull(message = "This is required string message") String>,
                """,
            """
                @QueryValue("strParam3") @NotNull strParam3: List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String>,
                """,
            """
                @QueryValue("intParam") @NotNull intParam: List<@NotNull(message = "This is required int message") @Min(value = 5, message = "This is min message") @Max(value = 10, message = "This is max message") Int>,
                """,
            """
                @QueryValue("decimalParam") @NotNull decimalParam: List<@NotNull(message = "This is required decimal message") @DecimalMin(value = "5.5", message = "This is decimal min message") @DecimalMax(value = "10.5", message = "This is decimal max message") BigDecimal>,
                """,
            """
                @QueryValue("decimalParam2") @NotNull(message = "This is required param message") decimalParam2: List<@NotNull(message = "This is required decimal message") @DecimalMin(value = "5.5", inclusive = false, message = "This is decimal min message") @DecimalMax(value = "10.5", inclusive = false, message = "This is decimal max message") BigDecimal>,
                """,
            """
                @QueryValue("positiveParam") @NotNull positiveParam: List<@NotNull(message = "This is required int message") @Positive(message = "This is positive message") Int>,
                """,
            """
                @QueryValue("positiveOrZeroParam") @NotNull positiveOrZeroParam: List<@NotNull(message = "This is required int message") @PositiveOrZero(message = "This is positive or zero message") Int>,
                """,
            """
                @QueryValue("negativeParam") @NotNull negativeParam: List<@NotNull(message = "This is required int message") @Negative(message = "This is negative message") Int>,
                """,
            """
                @QueryValue("negativeOrZeroParam") @NotNull negativeOrZeroParam: List<@NotNull(message = "This is required int message") @NegativeOrZero(message = "This is negative or zero message") Int>,
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
                    @field:Min(value = 5, message = "This is min message")
                    @field:Max(value = 10, message = "This is max message")
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
                    @field:Min(value = 0, message = "This is positive message")
                    @field:JsonProperty(JSON_PROPERTY_POSITIVE_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var positiveProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:Min(value = 0, message = "This is positive or zero message")
                    @field:JsonProperty(JSON_PROPERTY_POSITIVE_OR_ZERO_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var positiveOrZeroProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:Max(value = 0, message = "This is negative message")
                    @field:JsonProperty(JSON_PROPERTY_NEGATIVE_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var negativeProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:Max(value = 0, message = "This is negative or zero message")
                    @field:JsonProperty(JSON_PROPERTY_NEGATIVE_OR_ZERO_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var negativeOrZeroProp: Int? = null,
                """,
            """
                    @field:Nullable
                    @field:DecimalMin(value = "5.5", message = "This is decimal min message")
                    @field:DecimalMax(value = "10.5", message = "This is decimal max message")
                    @field:JsonProperty(JSON_PROPERTY_DECIMAL_PROP)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var decimalProp: BigDecimal? = null,
                """,
            """
                    @field:Nullable
                    @field:DecimalMin(value = "5.5", inclusive = false, message = "This is decimal min message")
                    @field:DecimalMax(value = "10.5", inclusive = false, message = "This is decimal max message")
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
}
