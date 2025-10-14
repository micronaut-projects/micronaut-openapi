package io.micronaut.openapi.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import static io.micronaut.openapi.generator.assertions.TestUtils.assertExtraAnnotationFiles;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.openapitools.codegen.CodegenConstants.APIS;
import static org.openapitools.codegen.CodegenConstants.API_TESTS;
import static org.openapitools.codegen.CodegenConstants.MODELS;
import static org.openapitools.codegen.CodegenConstants.MODEL_TESTS;
import static org.openapitools.codegen.CodegenConstants.SUPPORTING_FILES;

class JavaMicronautServerCodegenTest extends AbstractMicronautCodegenTest {

    static String ROLES_EXTENSION_TEST_PATH = "src/test/resources/3_0/micronaut/roles-extension-test.yml";
    static String MULTI_TAGS_TEST_PATH = "src/test/resources/3_0/micronaut/multi-tags-test.yml";
    private static final String DUPLICATE_PROPERTY_NAMES_PATH = "src/test/resources/3_0/duplicatePropertyNames.yaml";

    @Test
    void clientOptsUniqueness() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        final var codegen = new JavaMicronautServerCodegen();
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
        assertEquals("org.openapitools", codegen.getInvokerPackage());
        assertEquals("org.openapitools", codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE));
    }

    @Test
    void testApiAndModelFilesPresent() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        String invokerFolder = outputPath + "src/main/java/org/test/test/";
        assertFileExists(invokerFolder + "Application.java");

        String apiFolder = outputPath + "src/main/java/org/test/test/api/";
        assertFileExists(apiFolder + "PetApi.java");
        assertFileExists(apiFolder + "StoreApi.java");
        assertFileExists(apiFolder + "UserApi.java");

        String modelFolder = outputPath + "src/main/java/org/test/test/model/";
        assertFileExists(modelFolder + "Pet.java");
        assertFileExists(modelFolder + "User.java");
        assertFileExists(modelFolder + "Order.java");

        String resources = outputPath + "src/main/resources/";
        assertFileExists(resources + "application.yml");
    }

    @Test
    void doUseValidationParam() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/java/org/openapitools/api/";

        assertFileContains(apiFolder + "PetApi.java", "@Valid");
        assertFileContains(apiFolder + "PetApi.java", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        // Files are not generated
        String apiFolder = outputPath + "/src/main/java/org/openapitools/api/";

        assertFileNotContains(apiFolder + "PetApi.java", "@Valid");
        assertFileNotContains(apiFolder + "PetApi.java", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_TEST, JavaMicronautServerCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, true, SUPPORTING_FILES, APIS, MODELS, API_TESTS, MODEL_TESTS);
        String apiTestFolder = outputPath + "src/test/java/org/openapitools/api/";

        // Files are not generated
        assertFileExists(outputPath + "src/test/java/");
        assertFileExists(apiTestFolder + "PetApiTest.java");
        assertFileContains(apiTestFolder + "PetApiTest.java", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateForTestSpock() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_TEST, JavaMicronautServerCodegen.OPT_TEST_SPOCK);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, true, SUPPORTING_FILES, APIS, MODELS, API_TESTS, MODEL_TESTS);
        String apiTestFolder = outputPath + "src/test/groovy/org/openapitools/api/";

        // Files are not generated
        assertFileExists(outputPath + "src/test/groovy");
        assertFileExists(apiTestFolder + "PetApiSpec.groovy");
        assertFileContains(apiTestFolder + "PetApiSpec.groovy", "PetApiSpec", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        codegen.additionalProperties().put(CodegenConstants.SERIALIZATION_LIBRARY, SerializationLibraryKind.JACKSON.name());
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(modelPath + "Pet.java", "public Pet(String name, List<@NotNull String> photoUrls)");
        assertFileContains(modelPath + "Pet.java", "private Pet()");
    }

    @Test
    void doNotGenerateRequiredPropertiesInConstructor() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileNotContainsRegex(modelPath + "Pet.java", "public Pet\\([^)]+\\)");
        assertFileNotContains(modelPath + "Pet.java", "private Pet()");
        assertFileNotContainsRegex(modelPath + "User.java", "public User\\([^)]+\\)");
        assertFileNotContains(modelPath + "User.java", "private User()");
        assertFileNotContainsRegex(modelPath + "Order.java", "public Order\\([^)]+\\)");
        assertFileNotContains(modelPath + "Order.java", "private Order()");
    }

    @Test
    void testExtraAnnotations1() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/issue_11772.yml");

        assertExtraAnnotationFiles(outputPath + "/src/main/java/org/openapitools/model");
    }

    @Test
    void doNotGenerateAuthRolesWithExtensionWhenNotUseAuth() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_USE_AUTH, false);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileNotContains(apiPath + "BooksApi.java", "@Secured");
        assertFileNotContains(apiPath + "UsersApi.java", "@Secured");
        assertFileNotContains(apiPath + "ReviewsApi.java", "@Secured");
    }

    @Test
    void generateAuthRolesWithExtension() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_USE_AUTH, true);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_ANONYMOUS[^;]{0,100}bookSearchGet");
        assertFileContainsRegex(apiPath + "BooksApi.java", "@Secured\\(\"admin\"\\)[^;]{0,100}createBook");
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_ANONYMOUS[^;]{0,100}getBook");
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_AUTHENTICATED[^;]{0,100}reserveBook");

        assertFileContainsRegex(apiPath + "ReviewsApi.java", "IS_AUTHENTICATED[^;]{0,100}bookSendReviewPost");
        assertFileContainsRegex(apiPath + "ReviewsApi.java", "IS_ANONYMOUS[^;]{0,100}bookViewReviewsGet");

        assertFileContainsRegex(apiPath + "UsersApi.java", "IS_ANONYMOUS[^;]{0,100}getUserProfile");
        assertFileContainsRegex(apiPath + "UsersApi.java", "IS_AUTHENTICATED[^;]{0,100}updateProfile");
    }

    @Test
    void doGenerateMonoWrapHttpResponse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "PetApi.java", "Mono<HttpResponse<@Valid Pet>>");
    }

    @Test
    void doGenerateMono() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "PetApi.java", "Mono<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "Flux<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    void doGenerateMonoAndFlux() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "PetApi.java", "Mono<@Valid Pet>");
        assertFileContains(apiPath + "PetApi.java", "Flux<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    void doGenerateWrapHttpResponse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "PetApi.java", "HttpResponse<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "Mono");
    }

    @Test
    void doGenerateNoMonoNoWrapHttpResponse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "PetApi.java", "Pet");
        assertFileNotContains(apiPath + "PetApi.java", "Mono");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    void doGenerateOperationOnlyForFirstTag() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, true, SUPPORTING_FILES, APIS, MODELS, MODEL_TESTS, API_TESTS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/java/org/openapitools/api/";

        // Verify files are generated only for the required tags
        assertFileExists(apiPath + "AuthorsApi.java");
        assertFileExists(apiPath + "BooksApi.java");
        assertFileDoesntExist(apiPath + "SearchApi.java");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.java");
        assertFileExists(apiTestPath + "BooksApiTest.java");
        assertFileDoesntExist(apiTestPath + "SearchApiTest.java");

        // Verify all the methods are generated only ones
        assertFileContains(apiPath + "AuthorsApi.java",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.java",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable");
        assertFileNotContains(apiPath + "BooksApi.java", "getAuthorBooks");
    }

    @Test
    void doRepeatOperationForAllTags() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "false");
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, true, SUPPORTING_FILES, APIS, MODELS, MODEL_TESTS, API_TESTS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/java/org/openapitools/api/";

        // Verify all the tags created
        assertFileExists(apiPath + "AuthorsApi.java");
        assertFileExists(apiPath + "BooksApi.java");
        assertFileExists(apiPath + "SearchApi.java");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.java");
        assertFileExists(apiTestPath + "BooksApiTest.java");
        assertFileExists(apiTestPath + "SearchApiTest.java");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(apiPath + "AuthorsApi.java",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.java",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable", "getAuthorBooks");
        assertFileContains(apiPath + "SearchApi.java",
            "authorSearchGet", "bookSearchGet");
    }

    @Test
    void testReadOnlyConstructorBug() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml");
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name, String requiredReadOnly)");
        assertFileContains(apiPath + "ExtendedBookInfo.java", "public ExtendedBookInfo(String isbn, String name, String requiredReadOnly)", "super(name, requiredReadOnly)");
    }

    @Test
    void testDiscriminatorConstructorBug() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml");
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name)");
        assertFileContains(apiPath + "BasicBookInfo.java",
            "public BasicBookInfo(String author, String name)",
            "super(name)",
            """
                    @Override
                    public BasicBookInfo name(String name) {
                        super.setName(name);
                        return this;
                    }
                
                    @Override
                    public BasicBookInfo type(BookInfoType type) {
                        super.setType(type);
                        return this;
                    }
                """
        );
        assertFileContains(apiPath + "DetailedBookInfo.java", "public DetailedBookInfo(String isbn, String name, String author)", "super(author, name)");
    }

    @Test
    void testGenericAnnotations() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml");
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BooksApi.java", "@Body @NotNull List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String> requestBody");
        assertFileContains(modelPath + "CountsContainer.java", "private List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull ZonedDateTime>>> counts;");
        assertFileContains(modelPath + "BooksContainer.java", "private List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String> books;");
    }

    @Test
    void testPluralBodyParamName() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/plural.yml");
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.java", "@Body @NotNull List<@Valid Book> books");
    }

    @Test
    void testControllerEnums() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum.yml");
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileExists(modelPath + "GetTokenRequestGrantType.java");
        assertFileExists(modelPath + "GetTokenRequestClientSecret.java");
        assertFileExists(modelPath + "GetTokenRequestClientId.java");
        assertFileExists(modelPath + "ArtistsArtistIdDirectAlbumsGetSortByParameter.java");
    }

    @Test
    void testFileEndpoint() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/file.yml");
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "RequestBodyApi.java", "@Nullable(inherited = true) CompletedFileUpload file");
    }

    @Test
    void testReservedWords() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/javaReservedWords.yml", true,
            SUPPORTING_FILES,
            APIS,
            MODELS,
            MODEL_TESTS,
            API_TESTS,
            CodegenConstants.MODEL_DOCS,
            CodegenConstants.API_DOCS
        );
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ParametersApi.java", "Mono<Void> callInterface(",
            "@QueryValue(\"class\") @NotNull @Valid Package propertyClass,",
            "@QueryValue(\"while\") @NotNull String _while");
        assertFileContains(path + "model/Package.java",
            "public static final String JSON_PROPERTY_FOR = \"for\";",
            "@JsonProperty(JSON_PROPERTY_FOR)",
            "private String _for;",
            "public String get_for() {",
            "public void set_for(String _for) {");
        assertFileContains(path + "controller/ParametersController.java",
            "public Mono<Void> callInterface(Package propertyClass, String _while) {");
    }

    @Test
    void testCommonPathParametersWithRef() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.java", "@Get(\"/v1/forecast/{id}\")",
            "@PathVariable(\"id\") @NotNull String id,",
            "@QueryValue(\"hourly\") @Nullable(inherited = true) List<V1ForecastIdGetHourlyParameterInner> hourly,",
            "@QueryValue(\"daily\") @Nullable(inherited = true) @Format(FORMAT_MULTI) List<V1ForecastIdGetDailyParameterInner> daily,"
        );

        assertFileContains(path + "model/V1ForecastIdGetHourlyParameterInner.java",
            "public enum V1ForecastIdGetHourlyParameterInner {",
            "@JsonProperty(\"temperature_2m\")",
            "TEMPERATURE_2M(\"temperature_2m\"),");
    }

    @Test
    void testResponseRef() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/spec.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ResponseBodyApi.java", "@ApiResponse(responseCode = \"default\", description = \"An unexpected error has occurred\")");
    }

    @Test
    void testExtraAnnotations() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/BooksApi.java",
            """
                    @Post("/add-book")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    @NotBlank
                    Mono<@Valid Book> addBook(
                """);

        assertFileContains(path + "model/Book.java",
            """
                @io.micronaut.serde.annotation.Serdeable.Serializable
                public class Book {
                """,
            """
                    @NotNull
                    @Size(max = 10)
                    @Schema(name = "title", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_TITLE)
                    @jakarta.validation.constraints.NotBlank
                    private String title;
                """,
            """
                    @NotEmpty
                    public void setTitle(String title) {
                        this.title = title;
                    }
                """
        );
    }

    @Test
    void testOperationDescription() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/operation-with-desc.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DatasetsApi.java", "description = \"Creates a brand new dataset.\"");
    }

    @Test
    void testSecurity() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java",
            """
                    @Secured({"read", "admin"})
                    Mono<Void> get();
                """,
            """
                    @Secured({"write", "admin"})
                    Mono<Void> save();
                """);
    }

    @Test
    void testMultipartFormData() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipartdata.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ResetPasswordApi.java", """
                @Consumes("multipart/form-data")
                @Secured(SecurityRule.IS_ANONYMOUS)
                Mono<@Valid SuccessResetPassword> profilePasswordPost(
                    @Header("WCToken") @NotNull String wcToken,
                    @Header("WCTrustedToken") @NotNull String wcTrustedToken,
                    @Part("name") @NotNull String name,
                    @Part("title") @Nullable(inherited = true) String title,
                    @Part("file") @Nullable(inherited = true) CompletedFileUpload file
                );
            """);
    }

    @Test
    void testMultipleContentTypesEndpoints() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple-content-types.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java", """
                    @Post("/multiplecontentpath")
                    @Consumes({"application/json", "application/xml"})
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<HttpResponse<Void>> myOp(
                        @Body @Nullable(inherited = true) @Valid Coordinates coordinates
                    );
                """,
            """
                    @Post("/multiplecontentpath")
                    @Consumes("multipart/form-data")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<HttpResponse<Void>> myOp_1(
                        @Nullable(inherited = true) @Valid Coordinates coordinates,
                        @Nullable(inherited = true) CompletedFileUpload file
                    );
                """,
            """
                    @Post("/multiplecontentpath")
                    @Consumes({"application/yaml", "text/json"})
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<HttpResponse<Void>> myOp_2(
                        @Body @Nullable(inherited = true) @Valid MySchema mySchema
                    );
                """);
    }

    @Test
    void testPolymorphism() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setUseAuth(false);
        codegen.setReactive(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/1794/openapi.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/CurrencyInvoiceCreateDto.java", """
                    @Override
                    public CurrencyInvoiceCreateDto docType(DocType docType) {
                        super.setDocType(docType);
                        return this;
                    }
                """,
            """
                    @Override
                    public CurrencyInvoiceCreateDto sellerVatId(String sellerVatId) {
                        super.setSellerVatId(sellerVatId);
                        return this;
                    }
                """
        );
        assertFileNotContains(path + "model/BaseInvoiceDto.java", """
                    this.docType = docType;
                    this.sellerVatId = sellerVatId;
            """
        );
    }

    @Test
    void testDeprecated() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/deprecated.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ParametersApi.java",
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
                    @Deprecated
                    @Operation(
                        operationId = "sendPrimitives",
                        description = "A method to send primitives as request parameters",
                        deprecated = true,
                        responses = {
                            @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SendPrimitivesResponse.class))),
                            @ApiResponse(responseCode = "default", description = "An unexpected error has occurred")
                        },
                        parameters = {
                            @Parameter(name = "name", deprecated = true, required = true, in = ParameterIn.PATH),
                            @Parameter(name = "age", required = true, in = ParameterIn.QUERY),
                            @Parameter(name = "height", deprecated = true, required = true, in = ParameterIn.HEADER)
                        }
                    )
                    @Get("/sendPrimitives/{name}")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<@Valid SendPrimitivesResponse> sendPrimitives(
                        @PathVariable("name") @NotNull @Deprecated String name,
                        @QueryValue("age") @NotNull BigDecimal age,
                        @Header("height") @NotNull @Deprecated Float height
                    );
                """);
    }

    @Test
    void testCustomValidationMessages() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setUseEnumCaseInsensitive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/validation-messages.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/BooksApi.java",
            """
                @QueryValue("emailParam") @NotNull @Format(FORMAT_MULTI) List<@Email(regexp = "email@dot.com", message = "This is email pattern message") @Size(min = 5, max = 10, message = "This is min max email length message") @NotNull(message = "This is required email message") String> emailParam,
                """,
            """
                @QueryValue("strParam") @NotNull @Format(FORMAT_MULTI) List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String> strParam,
                """,
            """
                @QueryValue("strParam2") @NotNull @Format(FORMAT_MULTI) List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, message = "This is min max string length message") @NotNull(message = "This is required string message") String> strParam2,
                """,
            """
                @QueryValue("strParam3") @NotNull @Format(FORMAT_MULTI) List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String> strParam3,
                """,
            """
                @QueryValue("intParam") @NotNull @Format(FORMAT_MULTI) List<@NotNull(message = "This is required int message") @Min(value = 5, message = "This is min message") @Max(value = 10, message = "This is max message") Integer> intParam,
                """,
            """
                @QueryValue("decimalParam") @NotNull @Format(FORMAT_MULTI) List<@NotNull(message = "This is required decimal message") @DecimalMin(value = "5.5", message = "This is decimal min message") @DecimalMax(value = "10.5", message = "This is decimal max message") BigDecimal> decimalParam,
                """,
            """
                    @QueryValue("decimalParam2") @NotNull(message = "This is required param message") @Format(FORMAT_MULTI) List<@NotNull(message = "This is required decimal message") @DecimalMin(value = "5.5", inclusive = false, message = "This is decimal min message") @DecimalMax(value = "10.5", inclusive = false, message = "This is decimal max message") BigDecimal> decimalParam2,
                """,
            """
                @QueryValue("positiveParam") @NotNull @Format(FORMAT_MULTI) List<@NotNull(message = "This is required int message") @Positive(message = "This is positive message") Integer> positiveParam,
                """,
            """
                @QueryValue("positiveOrZeroParam") @NotNull @Format(FORMAT_MULTI) List<@NotNull(message = "This is required int message") @PositiveOrZero(message = "This is positive or zero message") Integer> positiveOrZeroParam,
                """,
            """
                @QueryValue("negativeParam") @NotNull @Format(FORMAT_MULTI) List<@NotNull(message = "This is required int message") @Negative(message = "This is negative message") Integer> negativeParam,
                """,
            """
                @QueryValue("negativeOrZeroParam") @NotNull @Format(FORMAT_MULTI) List<@NotNull(message = "This is required int message") @NegativeOrZero(message = "This is negative or zero message") Integer> negativeOrZeroParam,
                """);
    }

    @Test
    void testSwaggerAnnotations() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/petstore.json");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/PetApi.java",
            """
                    @Operation(
                        operationId = "findPetsByStatus",
                        summary = "Finds Pets by status",
                        description = "Multiple status values can be provided with comma separated strings",
                        responses = {
                            @ApiResponse(responseCode = "200", description = "successful operation", content = {
                                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pet.class))),
                                @Content(mediaType = "application/xml", array = @ArraySchema(schema = @Schema(implementation = Pet.class)))
                            }),
                            @ApiResponse(responseCode = "400", description = "Invalid status value")
                        },
                        parameters = @Parameter(name = "status", description = "Status values that need to be considered for filter", in = ParameterIn.QUERY),
                        security = @SecurityRequirement(name = "petstore_auth", scopes = {"write:pets", "read:pets"})
                    )
                    @Get("/pet/findByStatus")
                    @Produces({"application/json", "application/xml"})
                    @Secured({"write:pets", "read:pets"})
                    Mono<@NotNull List<@Valid Pet>> findPetsByStatus(
                        @QueryValue(value = "status", defaultValue = "[\\"available\\"]") @Nullable(inherited = true) @Format(FORMAT_MULTI) List<@NotNull String> status
                    );
                """);
    }

    @Test
    void testBodyEnum() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(false);
        codegen.setUseAuth(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/body-enum.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/MyCustomApi.java", """
                @Post("/api/v1/colors/{name}")
                Mono<@NotNull String> selectColor(
                    @Body @NotNull Color body
                );
            """);
        assertFileContains(path + "model/Color.java", "public enum Color {");
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
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DocumentResourcesApi.java", """
            @QueryValue("CREATIONDATE") @Nullable(inherited = true) LocalDate CREATIONDATE
            """);
        assertFileContains(path + "model/Result.java", """
                private String id;

                @Nullable(inherited = true)
                @Schema(name = "date", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @JsonProperty(JSON_PROPERTY_DATE)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                private ZonedDateTime date;
            """);
    }

    @Test
    void testImportZonedDateTime() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/library-definition.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/BookInfo.java",
            "import java.time.ZonedDateTime;",
            "private ZonedDateTime createdAt;");
    }

    @Test
    void testReadOnlyRequiredPropertyInConstructor() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oas.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/CategoryObject.java", """
                public CategoryObject(Integer id, String locale, String name) {
                    this.id = id;
                    this.locale = locale;
                    this.name = name;
                }
            """);
    }

    @Test
    void testUseTags() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setUseTags(false);
        String outputPath = generateFiles(codegen, "src/test/resources/petstore.json");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileExists(path + "api/UserApi.java");
        assertFileExists(path + "api/StoreApi.java");
        assertFileExists(path + "api/PetApi.java");
    }

    @Test
    void testGenerateOperationOnlyForFirstTagFalse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateOperationOnlyForFirstTag(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/multi-tags-test.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        // Verify all the tags created
        assertFileExists(path + "api/AuthorsApi.java");
        assertFileExists(path + "api/BooksApi.java");
        assertFileExists(path + "api/SearchApi.java");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(path + "api/AuthorsApi.java",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(path + "api/BooksApi.java",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable", "getAuthorBooks");
        assertFileContains(path + "api/SearchApi.java",
            "authorSearchGet", "bookSearchGet");
    }

    @Test
    void testGenerateOperationOnlyForFirstTagTrue() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateOperationOnlyForFirstTag(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/multi-tags-test.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        // Verify all the tags created
        assertFileExists(path + "api/AuthorsApi.java");
        assertFileExists(path + "api/BooksApi.java");
        assertFileDoesntExist(path + "api/SearchApi.java");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(path + "api/AuthorsApi.java",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(path + "api/BooksApi.java",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable");
        assertFileNotContains(path + "api/BooksApi.java", "getAuthorBooks");
    }

    @Test
    void testParamWithStyle() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-style.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java",
            "import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.*;",
            "@QueryValue(\"fields\") @NotNull @Format(FORMAT_MULTI) Map<String, @NotNull String> fields",
            "@QueryValue(\"fieldsCsv\") @NotNull Map<String, @NotNull String> fieldsCsv",
            "@QueryValue(\"fieldsSpace\") @NotNull @Format(FORMAT_SSV) Map<String, @NotNull String> fieldsSpace",
            "@QueryValue(\"fieldsPipes\") @NotNull @Format(FORMAT_PIPES) Map<String, @NotNull String> fieldsPipes",
            "@QueryValue(\"fieldsDeepObject\") @NotNull @Format(FORMAT_DEEP_OBJECT) Map<String, @NotNull String> fieldsDeepObject"
        );
    }

    @Test
    void testEnumConvertersConfig() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");
        String path = outputPath + "src/main/java/org/openapitools/config/";

        assertFileExists(path + "EnumConverterServerConfig.java");

        assertFileContains(path + "EnumConverterServerConfig.java",
            "import org.openapitools.model.BytePrimitiveEnum;",
            "import org.openapitools.model.CharPrimitiveEnum;",
            "import org.openapitools.model.DecimalEnum;",
            "import org.openapitools.model.DoubleEnum;",
            "import org.openapitools.model.DoublePrimitiveEnum;",
            "import org.openapitools.model.FloatEnum;",
            "import org.openapitools.model.FloatPrimitiveEnum;",
            "import org.openapitools.model.IntEnum;",
            "import org.openapitools.model.IntPrimitiveEnum;",
            "import org.openapitools.model.LongEnum;",
            "import org.openapitools.model.LongPrimitiveEnum;",
            "import org.openapitools.model.ShortPrimitiveEnum;",
            "import org.openapitools.model.StringEnum;",
            "public class EnumConverterServerConfig {",
            """
                    @Bean
                    public TypeConverter<String, StringEnum> toEnumStringEnum() {
                        return (v, c, ctx) -> Optional.of(StringEnum.fromValue(v));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<StringEnum, String> toStrStringEnum() {
                        return (v, c, ctx) -> Optional.of(v.getValue());
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, IntEnum> toEnumIntEnum() {
                        return (v, c, ctx) -> Optional.of(IntEnum.fromValue(Integer.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<IntEnum, String> toStrIntEnum() {
                        return (v, c, ctx) -> Optional.of(v.getValue().toString());
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, LongEnum> toEnumLongEnum() {
                        return (v, c, ctx) -> Optional.of(LongEnum.fromValue(Long.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<LongEnum, String> toStrLongEnum() {
                        return (v, c, ctx) -> Optional.of(v.getValue().toString());
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, DecimalEnum> toEnumDecimalEnum() {
                        return (v, c, ctx) -> Optional.of(DecimalEnum.fromValue(new BigDecimal(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<DecimalEnum, String> toStrDecimalEnum() {
                        return (v, c, ctx) -> Optional.of(v.getValue().toString());
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, FloatEnum> toEnumFloatEnum() {
                        return (v, c, ctx) -> Optional.of(FloatEnum.fromValue(Float.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<FloatEnum, String> toStrFloatEnum() {
                        return (v, c, ctx) -> Optional.of(v.getValue().toString());
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, DoubleEnum> toEnumDoubleEnum() {
                        return (v, c, ctx) -> Optional.of(DoubleEnum.fromValue(Double.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<DoubleEnum, String> toStrDoubleEnum() {
                        return (v, c, ctx) -> Optional.of(v.getValue().toString());
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, BytePrimitiveEnum> toEnumBytePrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(BytePrimitiveEnum.fromValue(Byte.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<BytePrimitiveEnum, String> toStrBytePrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, ShortPrimitiveEnum> toEnumShortPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(ShortPrimitiveEnum.fromValue(Short.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<ShortPrimitiveEnum, String> toStrShortPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, IntPrimitiveEnum> toEnumIntPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(IntPrimitiveEnum.fromValue(Integer.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<IntPrimitiveEnum, String> toStrIntPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, LongPrimitiveEnum> toEnumLongPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(LongPrimitiveEnum.fromValue(Long.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<LongPrimitiveEnum, String> toStrLongPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, FloatPrimitiveEnum> toEnumFloatPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(FloatPrimitiveEnum.fromValue(Float.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<FloatPrimitiveEnum, String> toStrFloatPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, DoublePrimitiveEnum> toEnumDoublePrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(DoublePrimitiveEnum.fromValue(Double.valueOf(v)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<DoublePrimitiveEnum, String> toStrDoublePrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, CharPrimitiveEnum> toEnumCharPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(CharPrimitiveEnum.fromValue(v.charAt(0)));
                    }
                """,
            """
                    @Bean
                    public TypeConverter<CharPrimitiveEnum, String> toStrCharPrimitiveEnum() {
                        return (v, c, ctx) -> Optional.of(String.valueOf(v.getValue()));
                    }
                """
        );
    }

    @Test
    void testEnumConvertersConfigWithoutEnumParams() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml");
        String path = outputPath + "src/main/java/org/openapitools/config/";

        assertFileDoesntExist(path + "EnumConverterServerConfig.java");
    }

    @Test
    void testEnumConvertersConfigDisabled() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateEnumConverters(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml");
        String path = outputPath + "src/main/java/org/openapitools/config/";

        assertFileDoesntExist(path + "EnumConverterServerConfig.java");
    }

    @Test
    void testResponseFileWithoutReactive() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.setReactive(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/response-file.yml");
        String path = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(path + "DefaultApi.java", """
                @Get("/example-route")
                @Produces("application/octet-stream")
                @Secured(SecurityRule.IS_ANONYMOUS)
                FileCustomizableResponseType exampleRouteGet();
            """);
    }

    @Test
    void testResponseFileWithReactive() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/response-file.yml");
        String path = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(path + "DefaultApi.java", """
                @Get("/example-route")
                @Produces("application/octet-stream")
                @Secured(SecurityRule.IS_ANONYMOUS)
                Mono<@NotNull FileCustomizableResponseType> exampleRouteGet();
            """);
    }

    @Test
    void testAvoidDuplicatePropertyNames() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, DUPLICATE_PROPERTY_NAMES_PATH);
        String modelFolder = outputPath + "src/main/java/org/openapitools/model/";
        String file = modelFolder + "ModelWithDuplicateProperties.java";

        assertFileExists(file);
        assertFileContainsRegex(file, "String name");
        assertFileContainsRegex(file, "String name2");
        assertFileContains(file, "public static final String JSON_PROPERTY_NAME_2 = \"_name\";");
        assertFileContains(file, "public String get_name()");
        assertFileContains(file, "public void set_name(String name2)");
    }

    @Test
    void testMultipartOperationWithoutResponse() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipart-without-response.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileExists(path + "api/DefaultApi.java");
        assertFileContains(path + "api/DefaultApi.java",
            """
                    @Operation(
                        operationId = "testPut",
                        responses = {}
                    )
                    @Put("/test")
                    @Consumes("multipart/form-data")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<Void> testPut(
                        @Part("file") @NotNull CompletedFileUpload file
                    );
                """
        );
    }

    @Test
    void testEnumInMultipart() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum-in-multipart.yml");
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileExists(path + "config/EnumConverterServerConfig.java");
        assertFileContains(path + "config/EnumConverterServerConfig.java",
            """
                        @Bean
                        public TypeConverter<String, DataDirection> toEnumDataDirection() {
                            return (v, c, ctx) -> Optional.of(DataDirection.fromValue(v));
                        }
                    
                        @Bean
                        public TypeConverter<DataDirection, String> toStrDataDirection() {
                            return (v, c, ctx) -> Optional.of(v.getValue());
                        }
                    
                        @Bean
                        public TypeConverter<String, DataChannel> toEnumDataChannel() {
                            return (v, c, ctx) -> Optional.of(DataChannel.fromValue(v));
                        }
                    
                        @Bean
                        public TypeConverter<DataChannel, String> toStrDataChannel() {
                            return (v, c, ctx) -> Optional.of(v.getValue());
                        }
                    """
        );

        assertFileExists(path + "api/BasApi.java");
        assertFileContains(path + "api/BasApi.java",
            """
                    Mono<HttpResponse<@Valid InlineObject>> createMessage(
                        @Part("fileContent") @NotNull CompletedFileUpload fileContent,
                        @Part("idempotencyKey") @NotNull String idempotencyKey,
                        @Part("dataDirection") @NotNull DataDirection dataDirection,
                        @Part("dataChannel") @NotNull DataChannel dataChannel
                    );
                """
        );
    }

    @Test
    void testUserParameterModeNone() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");

        String path = outputPath + "src/main/java/org/openapitools/api/";

        assertFileNotContains(path + "DefaultApi.java",
            "import java.security.Principal;",
            "import io.micronaut.security.authentication.Authentication;"
        );

        assertFileContains(path + "DefaultApi.java", """
                    @Post("/deny-all-endpoint")
                    @Secured(SecurityRule.DENY_ALL)
                    Mono<Void> denyAllOp();
                """,
            """
                    @Get("/pet")
                    @Secured({"read", "admin"})
                    Mono<Void> get();
                """,
            """
                    @Post("/pet")
                    @Secured({"write", "admin"})
                    Mono<Void> save();
                """,
            """
                    @Post("/pet-public")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<Void> savePublic();
                """
        );
    }

    @Test
    void testUserParameterModePrincipal() {

        var codegen = new JavaMicronautServerCodegen();
        codegen.setUserParameterMode(UserParameterMode.PRINCIPAL.name());
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");

        String path = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(path + "DefaultApi.java",
            "import java.security.Principal;",
            """
                    @Post("/deny-all-endpoint")
                    @Secured(SecurityRule.DENY_ALL)
                    Mono<Void> denyAllOp();
                """,
            """
                    @Get("/pet")
                    @Secured({"read", "admin"})
                    Mono<Void> get(
                        Principal principal
                    );
                """,
            """
                    @Post("/pet")
                    @Secured({"write", "admin"})
                    Mono<Void> save(
                        Principal principal
                    );
                """,
            """
                    @Post("/pet-public")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<Void> savePublic(
                        @Nullable(inherited = true) Principal principal
                    );
                """
        );
    }

    @Test
    void testUserParameterModeAuthentication() {

        var codegen = new JavaMicronautServerCodegen();
        codegen.setUserParameterMode(UserParameterMode.AUTHENTICATION.name());
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml");

        String path = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(path + "DefaultApi.java",
            "import io.micronaut.security.authentication.Authentication;",
            """
                    @Post("/deny-all-endpoint")
                    @Secured(SecurityRule.DENY_ALL)
                    Mono<Void> denyAllOp();
                """,
            """
                    @Get("/pet")
                    @Secured({"read", "admin"})
                    Mono<Void> get(
                        Authentication authentication
                    );
                """,
            """
                    @Post("/pet")
                    @Secured({"write", "admin"})
                    Mono<Void> save(
                        Authentication authentication
                    );
                """,
            """
                    @Post("/pet-public")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<Void> savePublic(
                        @Nullable(inherited = true) Authentication authentication
                    );
                """
        );
    }
}
