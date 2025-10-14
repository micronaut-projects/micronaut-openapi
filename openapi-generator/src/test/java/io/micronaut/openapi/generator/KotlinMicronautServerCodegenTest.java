package io.micronaut.openapi.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.openapitools.codegen.CodegenConstants.APIS;
import static org.openapitools.codegen.CodegenConstants.API_TESTS;
import static org.openapitools.codegen.CodegenConstants.MODELS;
import static org.openapitools.codegen.CodegenConstants.MODEL_TESTS;
import static org.openapitools.codegen.CodegenConstants.SUPPORTING_FILES;

class KotlinMicronautServerCodegenTest extends AbstractMicronautCodegenTest {

    static String ROLES_EXTENSION_TEST_PATH = "src/test/resources/3_0/micronaut/roles-extension-test.yml";
    static String MULTI_TAGS_TEST_PATH = "src/test/resources/3_0/micronaut/multi-tags-test.yml";
    private static final String DUPLICATE_PROPERTY_NAMES_PATH = "src/test/resources/3_0/duplicatePropertyNames.yaml";

    @Test
    void clientOptsUniqueness() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        var codegen = new KotlinMicronautServerCodegen();
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
        assertEquals("org.openapitools.controller", codegen.additionalProperties().get(JavaMicronautServerCodegen.OPT_CONTROLLER_PACKAGE));
        assertEquals("org.openapitools", codegen.getPackageName());
        assertEquals("org.openapitools", codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE));
    }

    @Test
    void testApiAndModelFilesPresent() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String invokerFolder = outputPath + "src/main/kotlin/org/test/test/";
        assertFileExists(invokerFolder + "Application.kt");

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
    void doUseValidationParam() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiFolder + "PetApi.kt", "@Valid");
        assertFileContains(apiFolder + "PetApi.kt", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/kotlin/org/openapitools/api/";
        assertFileNotContains(apiFolder + "PetApi.kt", "@Valid");
        assertFileNotContains(apiFolder + "PetApi.kt", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_TEST, KotlinMicronautServerCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, true, SUPPORTING_FILES, APIS, MODELS, API_TESTS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/kotlin/");
        String apiTestFolder = outputPath + "src/test/kotlin/org/openapitools/api/";
        assertFileExists(apiTestFolder + "PetApiTest.kt");
        assertFileContains(apiTestFolder + "PetApiTest.kt", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.kt",
            """
                data class Pet(
                    @field:NotNull
                    @field:Schema(name = "name", example = "doggie", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    var name: String,
                    @field:NotNull
                    @field:Schema(name = "photoUrls", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_PHOTO_URLS)
                    var photoUrls: List<@NotNull String>,
                    @field:Nullable
                    @field:Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_ID)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var id: Long? = null,
                    @field:Nullable
                    @field:Valid
                    @field:Schema(name = "category", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_CATEGORY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var category: Category? = null,
                    @field:Nullable
                    @field:Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_TAGS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var tags: List<@Valid Tag>? = null,
                    @field:Nullable
                    @field:Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_STATUS)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var status: PetStatus? = null,
                ) {
                """);
    }

    @Test
    void doNotGenerateAuthRolesWithExtensionWhenNotUseAuth() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_USE_AUTH, false);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileNotContains(apiPath + "BooksApi.kt", "@Secured");
        assertFileNotContains(apiPath + "UsersApi.kt", "@Secured");
        assertFileNotContains(apiPath + "ReviewsApi.kt", "@Secured");
    }

    @Test
    void generateAuthRolesWithExtension() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_USE_AUTH, true);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContainsRegex(apiPath + "BooksApi.kt", "IS_ANONYMOUS[^;]{0,100}bookSearchGet");
        assertFileContainsRegex(apiPath + "BooksApi.kt", "@Secured\\(\"admin\"\\)[^;]{0,100}createBook");
        assertFileContainsRegex(apiPath + "BooksApi.kt", "IS_ANONYMOUS[^;]{0,100}getBook");
        assertFileContainsRegex(apiPath + "BooksApi.kt", "IS_AUTHENTICATED[^;]{0,100}reserveBook");

        assertFileContainsRegex(apiPath + "ReviewsApi.kt", "IS_AUTHENTICATED[^;]{0,100}bookSendReviewPost");
        assertFileContainsRegex(apiPath + "ReviewsApi.kt", "IS_ANONYMOUS[^;]{0,100}bookViewReviewsGet");

        assertFileContainsRegex(apiPath + "UsersApi.kt", "IS_ANONYMOUS[^;]{0,100}getUserProfile");
        assertFileContainsRegex(apiPath + "UsersApi.kt", "IS_AUTHENTICATED[^;]{0,100}updateProfile");
    }

    @Test
    void generateAuth() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_USE_AUTH, true);
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "SecurityRequirement(name = \"petstore_auth\", scopes = [\"write:pets\", \"read:pets\"])");
    }

    @Test
    void doGenerateMonoWrapHttpResponse() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, true, SUPPORTING_FILES, APIS, MODELS, API_TESTS, MODEL_TESTS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "Mono<HttpResponse<Pet>>");
    }

    @Test
    void doGenerateMono() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "false");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "Mono<Pet>");
        assertFileNotContains(apiPath + "PetApi.kt", "Flux<Pet>");
        assertFileNotContains(apiPath + "PetApi.kt", "HttpResponse");
    }

    @Test
    void doGenerateMonoAndFlux() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "true");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "Mono<Pet>");
        assertFileContains(apiPath + "PetApi.kt", "Flux<Pet>");
        assertFileNotContains(apiPath + "PetApi.kt", "HttpResponse");
    }

    @Test
    void doGenerateWrapHttpResponse() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "HttpResponse<Pet>");
        assertFileNotContains(apiPath + "PetApi.kt", "Mono");
    }

    @Test
    void doGenerateNoMonoNoWrapHttpResponse() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "Pet");
        assertFileNotContains(apiPath + "PetApi.kt", "Mono");
        assertFileNotContains(apiPath + "PetApi.kt", "HttpResponse");
    }

    @Test
    void doGenerateOperationOnlyForFirstTag() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, true, SUPPORTING_FILES, APIS, MODELS, MODEL_TESTS, API_TESTS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/kotlin/org/openapitools/api/";

        // Verify files are generated only for the required tags
        assertFileExists(apiPath + "AuthorsApi.kt");
        assertFileExists(apiPath + "BooksApi.kt");
        assertFileDoesntExist(apiPath + "SearchApi.kt");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.kt");
        assertFileExists(apiTestPath + "BooksApiTest.kt");
        assertFileDoesntExist(apiTestPath + "SearchApiTest.kt");

        // Verify all the methods are generated only ones
        assertFileContains(apiPath + "AuthorsApi.kt",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.kt",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable");
        assertFileNotContains(apiPath + "BooksApi.kt", "getAuthorBooks");
    }

    @Test
    void doRepeatOperationForAllTags() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "false");
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, true, SUPPORTING_FILES, APIS, MODELS, MODEL_TESTS, API_TESTS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/kotlin/org/openapitools/api/";

        // Verify all the tags created
        assertFileExists(apiPath + "AuthorsApi.kt");
        assertFileExists(apiPath + "BooksApi.kt");
        assertFileExists(apiPath + "SearchApi.kt");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.kt");
        assertFileExists(apiTestPath + "BooksApiTest.kt");
        assertFileExists(apiTestPath + "SearchApiTest.kt");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(apiPath + "AuthorsApi.kt",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.kt",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable", "getAuthorBooks");
        assertFileContains(apiPath + "SearchApi.kt",
            "authorSearchGet", "bookSearchGet");
    }

    @Test
    void testReadOnlyConstructorBug() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.kt",
            """
                open class BookInfo(
                
                    @field:NotNull
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    open var name: String,
                
                    @field:Nullable
                    @field:Schema(name = "requiredReadOnly", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_REQUIRED_READ_ONLY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var requiredReadOnly: String? = null,
                
                    @field:Nullable
                    @field:Size(min = 3)
                    @field:Schema(name = "author", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var author: String? = null,
                
                    @field:Nullable
                    @field:Schema(name = "optionalReadOnly", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_OPTIONAL_READ_ONLY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    open var optionalReadOnly: String? = null,
                
                    @field:Nullable
                    @field:Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
                    @field:Schema(name = "isbn", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                
                    @NotNull
                    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_NAME)
                    name: String,
                
                    @Nullable
                    @Schema(name = "requiredReadOnly", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_REQUIRED_READ_ONLY)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    requiredReadOnly: String? = null,
                
                    @Nullable
                    @Size(min = 3)
                    @Schema(name = "author", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @JsonProperty(JSON_PROPERTY_AUTHOR)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    author: String? = null,
                
                    @Nullable
                    @Schema(name = "optionalReadOnly", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @JsonProperty(JSON_PROPERTY_OPTIONAL_READ_ONLY)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    optionalReadOnly: String? = null,
                
                    @Nullable
                    @Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @JsonProperty(JSON_PROPERTY_TYPE)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    type: BookInfoType? = null,
                ) : BookInfo(name, requiredReadOnly, author, optionalReadOnly, type) {
                """);
    }

    @Test
    void testDiscriminatorConstructorBug() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.kt",
            """
                open class BookInfo(
                
                    @field:NotNull
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    open var name: String,
                
                    @field:NotNull
                    @field:Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_TYPE)
                    open var type: BookInfoType? = null,
                ) {
                """);


        assertFileContains(apiPath + "BasicBookInfo.kt",
            """
                open class BasicBookInfo(
                
                    @field:NotNull
                    @field:Size(min = 3)
                    @field:Schema(name = "author", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    open var author: String,
                
                    @NotNull
                    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_NAME)
                    name: String,
                
                    @Nullable
                    @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_TYPE)
                    type: BookInfoType? = null,
                ) : BookInfo(name, type) {
                """);
        assertFileContains(apiPath + "DetailedBookInfo.kt",
            """
                class DetailedBookInfo(
                
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:Schema(name = "isbn", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                
                    @NotNull
                    @Size(min = 3)
                    @Schema(name = "author", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_AUTHOR)
                    author: String,
                
                    @NotNull
                    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_NAME)
                    name: String,
                
                    @Nullable
                    @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_TYPE)
                    type: BookInfoType? = null,
                ) : BasicBookInfo(author, name, type) {
                """);
    }

    @Test
    void testGenericAnnotations() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BooksApi.kt", "@Body @NotNull requestBody: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>");
        assertFileContains(modelPath + "CountsContainer.kt", "var counts: List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull ZonedDateTime>>>");
        assertFileContains(modelPath + "BooksContainer.kt", "var books: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>");
    }

    @Test
    void testPluralBodyParamName() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/plural.yml");
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.kt", "@Body @NotNull books: List<@Valid Book>");
    }

    @Test
    void testControllerEnums() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum.yml");
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileExists(modelPath + "GetTokenRequestGrantType.kt");
        assertFileExists(modelPath + "GetTokenRequestClientSecret.kt");
        assertFileExists(modelPath + "GetTokenRequestClientId.kt");
        assertFileExists(modelPath + "ArtistsArtistIdDirectAlbumsGetSortByParameter.kt");
    }

    @Test
    void testReservedWords() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/kotlinReservedWords.yml", true,
            SUPPORTING_FILES,
            APIS,
            MODELS,
            MODEL_TESTS,
            API_TESTS,
            CodegenConstants.MODEL_DOCS,
            CodegenConstants.API_DOCS
        );
        String path = outputPath + "src/main/kotlin/org/openapitools/";
        String testPath = outputPath + "src/test/kotlin/org/openapitools/";

        assertFileContains(path + "controller/ParametersController.kt", """
                override fun callInterface(
                    name: Class,
                    `data`: String,
                ): Mono<Void> {
            """);
        assertFileContains(path + "api/ParametersApi.kt", "fun callInterface(",
            "@QueryValue(\"name\") @NotNull @Valid name: Class,",
            "@QueryValue(\"data\") @NotNull `data`: String",
            "): Mono<Void>");
        assertFileContains(path + "model/Class.kt",
            "Class.JSON_PROPERTY_DATA",
            "@field:Schema(name = \"data\", requiredMode = Schema.RequiredMode.REQUIRED)",
            "@field:JsonProperty(JSON_PROPERTY_DATA)",
            "var `data`: String,");
        assertFileContains(testPath + "api/ParametersApiTest.kt",
            """
                    // given
                    var name = Class(`data` = "example")
                    var `data` = "example"
                """,
            """
                    var request = HttpRequest.GET<Void>(uri)
                        .accept("application/json")
                """,
            """
                    request.parameters
                        .add("name", Class(`data` = "example").toString()) // The query parameter format should be\s
                        .add("data", "example") // The query parameter format should be\s
                """);
    }

    @Test
    void testCommonPathParametersWithRef() {
        var codegen = new KotlinMicronautServerCodegen();
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
    void testResponseRef() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/spec.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/ResponseBodyApi.kt", "ApiResponse(responseCode = \"default\", description = \"An unexpected error has occurred\")");
    }

    @Test
    void testExtraAnnotations() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/BooksApi.kt",
            """
                    @Post("/add-book")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    @NotBlank
                    fun addBook(
                """);

        assertFileContains(path + "model/Book.kt",
            """
                @io.micronaut.serde.annotation.Serdeable.Serializable
                data class Book(
                    @field:NotNull
                    @field:Size(max = 10)
                    @field:Schema(name = "title", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_TITLE)
                    @field:jakarta.validation.constraints.NotBlank
                    @set:NotEmpty
                    var title: String,
                """);
    }

    @Test
    void testOperationDescription() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/operation-with-desc.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DatasetsApi.kt", "description = \"Creates a brand new dataset.\"");
    }

    @Test
    void testSecurity() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            """
                    @Secured("read", "admin")
                    fun get(): Mono<Void>
                """,
            """
                    @Secured("write", "admin")
                    fun save(): Mono<Void>
                """);
    }

    @Test
    void testMultipartFormData() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipartdata.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/ResetPasswordApi.kt", """
                @Consumes("multipart/form-data")
                @Secured(SecurityRule.IS_ANONYMOUS)
                fun profilePasswordPost(
                    @Header("WCToken") @NotNull wcToken: String,
                    @Header("WCTrustedToken") @NotNull wcTrustedToken: String,
                    @Part("name") @NotNull name: String,
                    @Part("title") @Nullable title: String? = "bla-bla",
                    @Part("file") @Nullable file: CompletedFileUpload? = null,
                ): Mono<SuccessResetPassword>
            """);
    }

    @Test
    void testMultipleContentTypesEndpoints() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple-content-types.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt", """
                    @Post("/multiplecontentpath")
                    @Consumes("application/json", "application/xml")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun myOp(
                        @Body @Nullable @Valid coordinates: Coordinates? = null,
                    ): Mono<HttpResponse<Void>>
                """,
            """
                    @Post("/multiplecontentpath")
                    @Consumes("multipart/form-data")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun myOp_1(
                        @Nullable @Valid coordinates: Coordinates? = null,
                        @Nullable file: CompletedFileUpload? = null,
                    ): Mono<HttpResponse<Void>>
                """,
            """
                    @Post("/multiplecontentpath")
                    @Consumes("application/yaml", "text/json")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun myOp_2(
                        @Body @Nullable @Valid mySchema: MySchema? = null,
                    ): Mono<HttpResponse<Void>>
                """);
    }

    @Test
    void testPolymorphism() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setUseAuth(false);
        codegen.setReactive(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/1794/openapi.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/CurrencyInvoiceCreateDto.kt", """
                @NotNull
                @Size(max = 10)
                @Schema(name = "sellerVatId", requiredMode = Schema.RequiredMode.REQUIRED)
                @JsonProperty(JSON_PROPERTY_SELLER_VAT_ID)
                sellerVatId: String,
            """
        );
    }

    @Test
    void testDeprecated() {
        var codegen = new KotlinMicronautServerCodegen();
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
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun sendPrimitives(
                        @PathVariable("name") @NotNull @java.lang.Deprecated name: String,
                        @QueryValue("age") @NotNull age: BigDecimal,
                        @Header("height") @NotNull @java.lang.Deprecated height: Float,
                    ): Mono<SendPrimitivesResponse>
                """);
    }

    @Test
    void testCustomValidationMessages() {
        var codegen = new KotlinMicronautServerCodegen();
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
    }

    @Test
    void testSwaggerAnnotations() {
        var codegen = new KotlinMicronautServerCodegen();
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
                    @Produces("application/json", "application/xml")
                    @Secured("write:pets", "read:pets")
                    fun findPetsByStatus(
                        @QueryValue("status", defaultValue = "available") @Nullable @Format(FORMAT_MULTI) status: List<@NotNull String>? = arrayListOf("available"),
                    ): Mono<List<Pet>>
                """);
    }

    @Test
    void testOptionalQueryValues() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/optional-controller-values.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            """
                    @Post("/sendPrimitives/{name}")
                    @Secured(SecurityRule.IS_ANONYMOUS)
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
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(false);
        codegen.setUseAuth(false);
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
    void testGenerateControllerAsAbstract() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateControllerAsAbstract(true);
        codegen.setUseAuth(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/body-enum.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileNotContains(path + "api/MyCustomApi.java", "@Controller");
    }

    @Test
    void testDateWithoutSizeAnnotations() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DocumentResourcesApi.kt", """
            @QueryValue("CREATIONDATE") @Nullable CREATIONDATE: LocalDate? = null,
            """);
        assertFileContains(path + "model/Result.kt", """
                var id: String? = null,
            
                @field:Nullable
                @field:Schema(name = "date", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @field:JsonProperty(JSON_PROPERTY_DATE)
                @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                var date: ZonedDateTime? = null,
            """);
    }

    @Test
    void testImportZonedDateTime() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/library-definition.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "model/BookInfo.kt",
            "import java.time.ZonedDateTime",
            "var createdAt: ZonedDateTime? = null,");
    }

    @Test
    void testCoroutines() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setCoroutines(true);
        codegen.setReactive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.kt",
            """
                    @Get("/{apiVersion}/orders")
                    @Secured(SecurityRule.IS_ANONYMOUS)
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
    void testUseTags() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setUseTags(false);
        String outputPath = generateFiles(codegen, "src/test/resources/petstore.json");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "api/UserApi.kt");
        assertFileExists(path + "api/StoreApi.kt");
        assertFileExists(path + "api/PetApi.kt");
    }

    @Test
    void testGenerateOperationOnlyForFirstTagFalse() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setGenerateOperationOnlyForFirstTag(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/multi-tags-test.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        // Verify all the tags created
        assertFileExists(path + "api/AuthorsApi.kt");
        assertFileExists(path + "api/BooksApi.kt");
        assertFileExists(path + "api/SearchApi.kt");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(path + "api/AuthorsApi.kt",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(path + "api/BooksApi.kt",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable", "getAuthorBooks");
        assertFileContains(path + "api/SearchApi.kt",
            "authorSearchGet", "bookSearchGet");
    }

    @Test
    void testGenerateOperationOnlyForFirstTagTrue() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setGenerateOperationOnlyForFirstTag(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/multi-tags-test.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        // Verify all the tags created
        assertFileExists(path + "api/AuthorsApi.kt");
        assertFileExists(path + "api/BooksApi.kt");
        assertFileDoesntExist(path + "api/SearchApi.kt");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(path + "api/AuthorsApi.kt",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(path + "api/BooksApi.kt",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable");
        assertFileNotContains(path + "api/BooksApi.kt", "getAuthorBooks");
    }

    @Test
    void testOperationParameterWithDefaultValue() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/parameter-list-with-default.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "api/PetApi.kt");
        assertFileContains(path + "api/PetApi.kt",
            "@QueryValue(\"status\", defaultValue = \"available\") @Nullable @Format(FORMAT_MULTI) status: List<@NotNull String>? = arrayListOf(\"available\"),");
    }

    @Test
    void testOperationParameterWithDefaultValueSwagger2() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/parameter-list-with-default-swagger2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "api/PetApi.kt");
        assertFileContains(path + "api/PetApi.kt",
            "@QueryValue(\"status\", defaultValue = \"available\") @Nullable @Format(FORMAT_MULTI) status: List<@NotNull String>? = arrayListOf(\"available\"),");
    }

    @Test
    void testPropsWithDefaultValueAreOptionalKsp() {
        var codegen = new KotlinMicronautServerCodegen();
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
                    @field:Schema(name = "value", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                
                    @field:Schema(name = "currency", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_CURRENCY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var currency: MyCur = MyCur.USD,
                """
        );
    }

    @Test
    void testPropsWithDefaultValueAreOptionalKapt() {
        var codegen = new KotlinMicronautServerCodegen();
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
                    @field:Schema(name = "value", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_VALUE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var `value`: String? = null,
                
                    @field:Nullable
                    @field:Schema(name = "currency", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_CURRENCY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var currency: MyCur? = MyCur.USD,
                """
        );
    }

    @Test
    void testParamWithStyle() {
        var codegen = new KotlinMicronautServerCodegen();
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
    void testEnumConvertersConfig() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/config/";

        assertFileExists(path + "EnumConverterServerConfig.kt");

        assertFileContains(path + "EnumConverterServerConfig.kt",
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
            "class EnumConverterServerConfig {",
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
    void testEnumConvertersConfigWithoutEnumParams() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/config/";

        assertFileDoesntExist(path + "EnumConverterServerConfig.kt");
    }

    @Test
    void testEnumConvertersConfigDisabled() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setGenerateEnumConverters(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/config/";

        assertFileDoesntExist(path + "EnumConverterServerConfig.kt");
    }

    @Test
    void testResponseFileWithoutReactive() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setReactive(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/response-file.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(path + "DefaultApi.kt", """
                @Get("/example-route")
                @Produces("application/octet-stream")
                @Secured(SecurityRule.IS_ANONYMOUS)
                fun exampleRouteGet(): FileCustomizableResponseType
            """);
    }

    @Test
    void testResponseFileWithReactive() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/response-file.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(path + "DefaultApi.kt", """
                @Get("/example-route")
                @Produces("application/octet-stream")
                @Secured(SecurityRule.IS_ANONYMOUS)
                fun exampleRouteGet(): Mono<FileCustomizableResponseType>
            """);
    }

    @Test
    void testResponseFileWithCoroutines() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.setReactive(false);
        codegen.setCoroutines(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/response-file.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(path + "DefaultApi.kt", """
                @Get("/example-route")
                @Produces("application/octet-stream")
                @Secured(SecurityRule.IS_ANONYMOUS)
                suspend fun exampleRouteGet(): FileCustomizableResponseType
            """);
    }

    @Test
    void testAvoidDuplicatePropertyNames() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, DUPLICATE_PROPERTY_NAMES_PATH);
        String modelFolder = outputPath + "src/main/kotlin/org/openapitools/model/";
        String file = modelFolder + "ModelWithDuplicateProperties.kt";

        assertFileExists(file);
        assertFileContains(file, "var name: String? = null");
        assertFileContains(file, "var name2: String? = null");
        assertFileContains(file, "const val JSON_PROPERTY_NAME_2 = \"_name\"");
    }

    @Test
    void testMultipartOperationWithoutResponse() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipart-without-response.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "api/DefaultApi.kt");
        assertFileContains(path + "api/DefaultApi.kt",
            """
                    @Operation(
                        operationId = "testPut",
                        responses = [
                        ],
                    )
                    @Put("/test")
                    @Consumes("multipart/form-data")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun testPut(
                        @Part("file") @NotNull file: CompletedFileUpload,
                    ): Mono<Void>
                """
        );
    }

    @Test
    void testEnumInMultipart() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum-in-multipart.yml");
        String path = outputPath + "src/main/kotlin/org/openapitools/";

        assertFileExists(path + "config/EnumConverterServerConfig.kt");
        assertFileContains(path + "config/EnumConverterServerConfig.kt",
            """
                    @Bean
                    fun toEnumDataDirection() = TypeConverter<String, DataDirection> { v, _, _ -> Optional.of(DataDirection.fromValue(v)) }
                
                    @Bean
                    fun toStrDataDirection() = TypeConverter<DataDirection, String> { v, _, _ -> Optional.of(v.value) }
                
                    @Bean
                    fun toEnumDataChannel() = TypeConverter<String, DataChannel> { v, _, _ -> Optional.of(DataChannel.fromValue(v)) }
                
                    @Bean
                    fun toStrDataChannel() = TypeConverter<DataChannel, String> { v, _, _ -> Optional.of(v.value) }
                """
        );

        assertFileExists(path + "api/BasApi.kt");
        assertFileContains(path + "api/BasApi.kt",
            """
                    fun createMessage(
                        @Part("fileContent") @NotNull fileContent: CompletedFileUpload,
                        @Part("idempotencyKey") @NotNull idempotencyKey: String,
                        @Part("dataDirection") @NotNull dataDirection: DataDirection,
                        @Part("dataChannel") @NotNull dataChannel: DataChannel,
                    ): Mono<HttpResponse<InlineObject>>
                """
        );
    }

    @Test
    void testUserParameterModeNone() {

        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileNotContains(path + "DefaultApi.kt",
            "import java.security.Principal",
            "import io.micronaut.security.authentication.Authentication"
        );

        assertFileContains(path + "DefaultApi.kt", """
                    @Post("/deny-all-endpoint")
                    @Secured(SecurityRule.DENY_ALL)
                    fun denyAllOp(): Mono<Void>
                """,
            """
                    @Get("/pet")
                    @Secured("read", "admin")
                    fun get(): Mono<Void>
                """,
            """
                    @Post("/pet")
                    @Secured("write", "admin")
                    fun save(): Mono<Void>
                """,
            """
                    @Post("/pet-public")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun savePublic(): Mono<Void>
                """
        );
    }

    @Test
    void testUserParameterModePrincipal() {

        var codegen = new KotlinMicronautServerCodegen();
        codegen.setUserParameterMode(UserParameterMode.PRINCIPAL.name());
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(path + "DefaultApi.kt",
            "import java.security.Principal",
            """
                    @Post("/deny-all-endpoint")
                    @Secured(SecurityRule.DENY_ALL)
                    fun denyAllOp(): Mono<Void>
                """,
            """
                    @Get("/pet")
                    @Secured("read", "admin")
                    fun get(
                        principal: Principal,
                    ): Mono<Void>
                """,
            """
                    @Post("/pet")
                    @Secured("write", "admin")
                    fun save(
                        principal: Principal,
                    ): Mono<Void>
                """,
            """
                    @Post("/pet-public")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun savePublic(
                        @Nullable principal: Principal? = null,
                    ): Mono<Void>
                """
        );
    }

    @Test
    void testUserParameterModeAuthentication() {

        var codegen = new KotlinMicronautServerCodegen();
        codegen.setUserParameterMode(UserParameterMode.AUTHENTICATION.name());
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(path + "DefaultApi.kt",
            "import io.micronaut.security.authentication.Authentication",
            """
                    @Post("/deny-all-endpoint")
                    @Secured(SecurityRule.DENY_ALL)
                    fun denyAllOp(): Mono<Void>
                """,
            """
                    @Get("/pet")
                    @Secured("read", "admin")
                    fun get(
                        authentication: Authentication,
                    ): Mono<Void>
                """,
            """
                    @Post("/pet")
                    @Secured("write", "admin")
                    fun save(
                        authentication: Authentication,
                    ): Mono<Void>
                """,
            """
                    @Post("/pet-public")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    fun savePublic(
                        @Nullable authentication: Authentication? = null,
                    ): Mono<Void>
                """
        );
    }

    @Test
    void testDollarSign() {

        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/dollar-sign.yml");

        String path = outputPath + "src/main/kotlin/org/openapitools/";
        assertFileContains(path + "api/Tag1Api.kt",
            """
                @Controller
                @Tag(name = "\\$tag1", description = "Desc for tag with \\$dollarSign")
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
                    @Operation(
                        operationId = "getStrings",
                        summary = "summary with \\$dollarSign",
                        description = "contains \\$strings",
                        tags = [ "\\$tag1", "\\$tag2" ],
                        responses = [
                            ApiResponse(responseCode = "200", description = "desc resp 200 with \\$dollarSign", content = [
                                Content(mediaType = "\\$application/json", schema = Schema(implementation = String::class)),
                            ]),
                            ApiResponse(responseCode = "404", description = "desc resp 404 with \\$dollarSign", content = [
                                Content(mediaType = "\\$application/json", schema = Schema(implementation = String::class)),
                            ]),
                        ],
                        parameters = [
                            Parameter(name = "\\$pathVar", description = "desc pathVar with \\$dollarSign", required = true, `in` = ParameterIn.PATH),
                            Parameter(name = "\\$queryVar", deprecated = true, description = "desc queryVar with \\$dollarSign", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$headerVar", description = "desc header with \\$dollarSign", `in` = ParameterIn.HEADER),
                            Parameter(name = "\\$cookieVar", description = "desc cookie with \\$dollarSign", `in` = ParameterIn.COOKIE),
                            Parameter(name = "\\$propDouble", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propLong", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propInt", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propEmail", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propListMinMax", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propListMin", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propListMax", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propStrMinMax", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propStrMin", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propStrMax", `in` = ParameterIn.QUERY),
                            Parameter(name = "\\$propStrPattern", `in` = ParameterIn.QUERY),
                        ],
                        security = [
                            SecurityRequirement(name = "\\$notSimpleAuth"),
                            SecurityRequirement(name = "\\$OAuth2", scopes = ["\\$read", "\\$write"]),
                        ],
                    )
                    @Get("/\\$paths/{\\$pathVar}")
                    @Produces("\\$application/json")
                    @Consumes("\\$application/json")
                    @Secured("\\$notRead", "\\$notWrite")
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

        assertFileContains(path + "model/DollarGetDollarStringsDollarQueryVarParameter.kt",
            """
                enum class DollarGetDollarStringsDollarQueryVarParameter(
                    @get:JsonValue
                    val value: String,
                ) {
                
                    /**
                     * desc enumConst1 with $dollarSign
                     */
                    @JsonProperty("ref1/\\$ref")
                    REF1__REF("ref1/\\$ref"),
                    /**
                     * desc enumConst2 with $dollarSign
                     */
                    @Deprecated("")
                    @JsonProperty("ref2/\\$ref")
                    REF2__REF("ref2/\\$ref"),
                    ;
                
                    override fun toString(): String = value
                
                    companion object {
                
                        @JvmField
                        val VALUE_MAPPING = entries.associateBy { it.value }
                
                        /**
                         * Create this enum from a value.
                         *
                         * @param value The value
                         *
                         * @return The enum
                         */
                        @JvmStatic
                        @JsonCreator
                        fun fromValue(value: String): DollarGetDollarStringsDollarQueryVarParameter {
                            require(VALUE_MAPPING.containsKey(value)) { "Unexpected value '$value'" }
                            return VALUE_MAPPING[value]!!
                        }
                    }
                }
                """
        );

        assertFileContains(path + "model/SchemaTitleWithDollarDollarSign.kt",
            """
                /**
                 * Schema desc with $dollarSign
                 */
                @Schema(description = "Schema desc with \\$dollarSign")
                @JsonPropertyOrder(
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP1,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_DOUBLE,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_LONG,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_INT,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_EMAIL,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_LIST_MIN_MAX,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_LIST_MIN,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_LIST_MAX,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_STR_MIN_MAX,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_STR_MIN,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_DOLLAR_PROP_STR_MAX,
                    SchemaTitleWithDollarDollarSign.JSON_PROPERTY_PROP2,
                )
                @Serdeable
                @Generated("io.micronaut.openapi.generator.KotlinMicronautServerCodegen")
                data class SchemaTitleWithDollarDollarSign(
                
                    /**
                     * Schema prop1 desc with $dollarSign
                     *
                     * @deprecated Deprecated message with $dollarSign
                     */
                    @Deprecated("Deprecated message with \\$dollarSign")
                    @field:NotNull(message = "Message with \\$dollarSign")
                    @field:Pattern(regexp = "poi\\\\.feedback\\\\.Review$0(.)*", message = "Message with \\$dollarSign")
                    @field:Size(min = 10, message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$prop1", example = "\\$dollarSign", description = "Schema prop1 desc with \\$dollarSign", requiredMode = Schema.RequiredMode.REQUIRED, deprecated = true)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP1)
                    var dollarProp1: String,
                
                    @field:Nullable
                    @field:DecimalMin("10", message = "Message with \\$dollarSign")
                    @field:DecimalMax("100", message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propDouble", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_DOUBLE)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropDouble: BigDecimal? = null,
                
                    @field:Nullable
                    @field:Min(10L, message = "Message with \\$dollarSign")
                    @field:Max(100L, message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propLong", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_LONG)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropLong: Long? = null,
                
                    @field:Nullable
                    @field:Min(10, message = "Message with \\$dollarSign")
                    @field:Max(100, message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propInt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_INT)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropInt: Int? = null,
                
                    @field:Nullable
                    @field:Email(regexp = "poi\\\\.feedback\\\\.Review$0(.)*", message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propEmail", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_EMAIL)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropEmail: String? = null,
                
                    @field:Nullable
                    @field:Schema(name = "\\$propListMinMax", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_LIST_MIN_MAX)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropListMinMax: List<@NotNull String>? = null,
                
                    @field:Nullable
                    @field:Schema(name = "\\$propListMin", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_LIST_MIN)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropListMin: List<@NotNull String>? = null,
                
                    @field:Nullable
                    @field:Schema(name = "\\$propListMax", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_LIST_MAX)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropListMax: List<@NotNull String>? = null,
                
                    @field:Nullable
                    @field:Size(min = 10, max = 100, message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propStrMinMax", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_STR_MIN_MAX)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropStrMinMax: String? = null,
                
                    @field:Nullable
                    @field:Size(min = 10, message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propStrMin", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_STR_MIN)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropStrMin: String? = null,
                
                    @field:Nullable
                    @field:Size(max = 100, message = "Message with \\$dollarSign")
                    @field:Schema(name = "\\$propStrMax", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_DOLLAR_PROP_STR_MAX)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var dollarPropStrMax: String? = null,
                
                    /**
                     * @deprecated Deprecated message with $dollarSign
                     */
                    @Deprecated("Deprecated message with \\$dollarSign")
                    @field:Nullable
                    @field:Valid
                    @field:Schema(name = "prop2", requiredMode = Schema.RequiredMode.NOT_REQUIRED, deprecated = true)
                    @field:JsonProperty(JSON_PROPERTY_PROP2)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    var prop2: DollarSomeDto? = null,
                ) {
                
                    companion object {
                
                        const val JSON_PROPERTY_DOLLAR_PROP1 = "\\$prop1"
                        const val JSON_PROPERTY_DOLLAR_PROP_DOUBLE = "\\$propDouble"
                        const val JSON_PROPERTY_DOLLAR_PROP_LONG = "\\$propLong"
                        const val JSON_PROPERTY_DOLLAR_PROP_INT = "\\$propInt"
                        const val JSON_PROPERTY_DOLLAR_PROP_EMAIL = "\\$propEmail"
                        const val JSON_PROPERTY_DOLLAR_PROP_LIST_MIN_MAX = "\\$propListMinMax"
                        const val JSON_PROPERTY_DOLLAR_PROP_LIST_MIN = "\\$propListMin"
                        const val JSON_PROPERTY_DOLLAR_PROP_LIST_MAX = "\\$propListMax"
                        const val JSON_PROPERTY_DOLLAR_PROP_STR_MIN_MAX = "\\$propStrMinMax"
                        const val JSON_PROPERTY_DOLLAR_PROP_STR_MIN = "\\$propStrMin"
                        const val JSON_PROPERTY_DOLLAR_PROP_STR_MAX = "\\$propStrMax"
                        const val JSON_PROPERTY_PROP2 = "prop2"
                    }
                }
                """
        );
    }
}
