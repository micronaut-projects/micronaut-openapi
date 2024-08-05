package io.micronaut.openapi.generator;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

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

        assertFileContains(apiPath + "BooksApi.kt", "requestBody: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>");
        assertFileContains(modelPath + "CountsContainer.kt", "var counts: List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull String>>>");
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

        assertFileContains(modelPath + "StringEnum.kt", "@JsonProperty(\"starting\")", "STARTING(\"starting\"),");
        assertFileContains(modelPath + "IntEnum.kt", "@JsonProperty(\"1\")", "_1(1),");
        assertFileContains(modelPath + "LongEnum.kt", "@JsonProperty(\"1\")", "_3(3L),");
        assertFileContains(modelPath + "DecimalEnum.kt", "@JsonProperty(\"1.23\")", "_34_1(BigDecimal(\"34.1\"))");
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
    void testCommonPathParametersWithRef() {

        var codegen = new KotlinMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.kt", "@Get(\"/v1/forecast/{id}\")",
                "@PathVariable(\"id\") @NotNull id: String,",
                "@QueryValue(\"hourly\") @Nullable hourly: List<V1ForecastIdGetHourlyParameterInner>?,");

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
}
