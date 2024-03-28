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

class KotlinMicronautServerCodegenTest extends AbstractMicronautCodegenTest {

    static String ROLES_EXTENSION_TEST_PATH = "src/test/resources/3_0/micronaut/roles-extension-test.yaml";
    static String MULTI_TAGS_TEST_PATH = "src/test/resources/3_0/micronaut/multi-tags-test.yaml";

    @Test
    void clientOptsUnicity() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(v.size(), 1, k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        final var codegen = new KotlinMicronautServerCodegen();
        codegen.processOpts();

        OpenAPI openAPI = new OpenAPI();
        openAPI.addServersItem(new Server().url("https://one.com/v2"));
        openAPI.setInfo(new Info());
        codegen.preprocessOpenAPI(openAPI);

        assertEquals(codegen.additionalProperties().get(CodegenConstants.HIDE_GENERATION_TIMESTAMP), Boolean.FALSE);
        assertFalse(codegen.isHideGenerationTimestamp());
        assertEquals(codegen.modelPackage(), "org.openapitools.model");
        assertEquals(codegen.additionalProperties().get(CodegenConstants.MODEL_PACKAGE), "org.openapitools.model");
        assertEquals(codegen.apiPackage(), "org.openapitools.api");
        assertEquals(codegen.additionalProperties().get(CodegenConstants.API_PACKAGE), "org.openapitools.api");
        assertEquals(codegen.additionalProperties().get(KotlinMicronautServerCodegen.OPT_CONTROLLER_PACKAGE), "org.openapitools.controller");
        assertEquals(codegen.getPackageName(), "org.openapitools");
        assertEquals(codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE), "org.openapitools");
    }

    @Test
    void testApiAndModelFilesPresent() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS,
            CodegenConstants.MODELS);

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
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiFolder + "PetApi.kt", "@Valid");
        assertFileContains(apiFolder + "PetApi.kt", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/kotlin/org/openapitools/api/";
        assertFileNotContains(apiFolder + "PetApi.kt", "@Valid");
        assertFileNotContains(apiFolder + "PetApi.kt", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_TEST,
            KotlinMicronautServerCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/kotlin/");
        String apiTestFolder = outputPath + "src/test/kotlin/org/openapitools/api/";
        assertFileExists(apiTestFolder + "PetApiTest.kt");
        assertFileContains(apiTestFolder + "PetApiTest.kt", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

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
    void doNotGenerateRequiredPropertiesInConstructor() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";
        assertFileNotContainsRegex(modelPath + "Pet.kt", "public Pet\\([^)]+\\)");
        assertFileNotContainsRegex(modelPath + "User.kt", "public User\\([^)]+\\)");
        assertFileNotContainsRegex(modelPath + "Order.kt", "public Order\\([^)]+\\)");
    }

    @Test
    void doNotGenerateAuthRolesWithExtensionWhenNotUseAuth() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_USE_AUTH, false);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileNotContains(apiPath + "BooksApi.kt", "@Secured");
        assertFileNotContains(apiPath + "UsersApi.kt", "@Secured");
        assertFileNotContains(apiPath + "ReviewsApi.kt", "@Secured");
    }

    @Test
    void generateAuthRolesWithExtension() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_USE_AUTH, true);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

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
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "SecurityRequirement(name = \"petstore_auth\", scopes = [\"write:pets\", \"read:pets\"])");
    }

    @Test
    void doGenerateMonoWrapHttpResponse() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.MODEL_TESTS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "Mono<HttpResponse<Pet>>");
    }

    @Test
    void doGenerateMono() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "false");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

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
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

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
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "HttpResponse<Pet>");
        assertFileNotContains(apiPath + "PetApi.kt", "Mono");
    }

    @Test
    void doGenerateNoMonoNoWrapHttpResponse() {
        var codegen = new KotlinMicronautServerCodegen();
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(KotlinMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.kt", "Pet");
        assertFileNotContains(apiPath + "PetApi.kt", "Mono");
        assertFileNotContains(apiPath + "PetApi.kt", "HttpResponse");
    }

    @Test
    void doGenerateOperationOnlyForFirstTag() {
        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, CodegenConstants.MODELS,
            CodegenConstants.APIS, CodegenConstants.API_TESTS);

        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/kotlin/org/openapitools/api/";

        // Verify files are generated only for the required tags
        assertFileExists(apiPath + "AuthorsApi.kt");
        assertFileExists(apiPath + "BooksApi.kt");
        assertFileNotExists(apiPath + "SearchApi.kt");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.kt");
        assertFileExists(apiTestPath + "BooksApiTest.kt");
        assertFileNotExists(apiTestPath + "SearchApiTest.kt");

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
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, CodegenConstants.MODELS,
            CodegenConstants.APIS, CodegenConstants.API_TESTS);

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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml", CodegenConstants.MODELS);
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
                data class ExtendedBookInfo(
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:Schema(name = "isbn", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                    @field:NotNull
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    override var name: String,
                    @field:Nullable
                    @field:Schema(name = "requiredReadOnly", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_REQUIRED_READ_ONLY)
                    @field:JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    override var requiredReadOnly: String? = null,
                ): BookInfo(name, requiredReadOnly)  {
                """);
    }

    @Test
    void testDiscriminatorConstructorBug() {

        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml", CodegenConstants.MODELS);
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
                    @field:NotNull
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    override var name: String,
                ): BookInfo(name) {
                """);
        assertFileContains(apiPath + "DetailedBookInfo.kt",
            """
                data class DetailedBookInfo(
                    @field:NotNull
                    @field:Pattern(regexp = "[0-9]{13}")
                    @field:Schema(name = "isbn", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_ISBN)
                    var isbn: String,
                    @field:NotNull
                    @field:Size(min = 3)
                    @field:Schema(name = "author", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_AUTHOR)
                    override var author: String,
                    @field:NotNull
                    @field:Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
                    @field:JsonProperty(JSON_PROPERTY_NAME)
                    override var name: String,
                ): BasicBookInfo(author, name) {
                """);
    }

    @Test
    void testGenericAnnotations() {

        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileContains(apiPath + "BooksApi.kt", "@Body @NotNull requestBody: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>");
        assertFileContains(modelPath + "CountsContainer.kt", "var counts: List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull String>>>");
        assertFileContains(modelPath + "BooksContainer.kt", "var books: List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String>");
    }

    @Test
    void testPluralBodyParamName() {

        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/plural.yml", CodegenConstants.APIS);
        String apiPath = outputPath + "src/main/kotlin/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.kt", "@Body @NotNull books: List<@Valid Book>");
    }

    @Test
    void testControllerEnums() {

        var codegen = new KotlinMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/kotlin/org/openapitools/model/";

        assertFileExists(modelPath + "GetTokenRequestGrantType.kt");
        assertFileExists(modelPath + "GetTokenRequestClientSecret.kt");
        assertFileExists(modelPath + "GetTokenRequestClientId.kt");
        assertFileExists(modelPath + "ArtistsArtistIdDirectAlbumsGetSortByParameter.kt");
    }

    @Test
    void testReservedWords() {

        var codegen = new KotlinMicronautServerCodegen();
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

        assertFileContains(path + "controller/ParametersController.kt", "override fun callInterface(name: Class, `data`: String): Mono<Void>");
        assertFileContains(path + "api/ParametersApi.kt", "fun callInterface(",
            "@QueryValue(\"name\") @NotNull @Valid name: Class,",
            "@QueryValue(\"data\") @NotNull `data`: String",
            "): Mono<Void>");
        assertFileContains(path + "model/Class.kt",
            "Class.JSON_PROPERTY_DATA",
            "@field:Schema(name = \"data\", requiredMode = Schema.RequiredMode.REQUIRED)",
            "@field:JsonProperty(JSON_PROPERTY_DATA)",
            "var `data`: String,");
    }
}
