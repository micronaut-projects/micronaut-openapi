package io.micronaut.openapi.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JavaMicronautClientCodegenTest extends AbstractMicronautCodegenTest {

    @Test
    void clientOptsUnicity() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.processOpts();

        var openAPI = new OpenAPI();
        openAPI.addServersItem(new Server().url("https://one.com/v2"));
        openAPI.setInfo(new Info());
        codegen.preprocessOpenAPI(openAPI);

        assertEquals(codegen.additionalProperties().get(CodegenConstants.HIDE_GENERATION_TIMESTAMP), Boolean.FALSE);
        assertFalse(codegen.isHideGenerationTimestamp());

        assertEquals(Boolean.FALSE, codegen.additionalProperties().get(CodegenConstants.HIDE_GENERATION_TIMESTAMP));
        assertFalse(codegen.isHideGenerationTimestamp());
        assertEquals("org.openapitools.model", codegen.modelPackage());
        assertEquals("org.openapitools.model", codegen.additionalProperties().get(CodegenConstants.MODEL_PACKAGE));
        assertEquals("org.openapitools.api", codegen.apiPackage());
        assertEquals("org.openapitools.api", codegen.additionalProperties().get(CodegenConstants.API_PACKAGE));
        assertEquals("org.openapitools", codegen.getInvokerPackage());
        assertEquals("org.openapitools", codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE));
    }

    @Test
    void testApiAndModelFilesPresent() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS,
            CodegenConstants.MODELS);

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
    void doConfigureAuthParam() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS);

        // Files generated
        assertFileExists(outputPath + "/src/main/java/org/openapitools/auth/Authorization.java");
        // Endpoints are annotated with @Authorization Bindable
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Authorization");
    }

    @Test
    void doNotConfigureAuthParam() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileNotExists(outputPath + "/src/main/java/org/openapitools/auth/");
        assertFileNotContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Authorization");
    }

    @Test
    void doUseValidationParam() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Valid");
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileNotContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Valid");
        assertFileNotContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_TEST,
            JavaMicronautClientCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/java/");
        assertFileExists(outputPath + "src/test/java/org/openapitools/api/PetApiTest.java");
        assertFileContains(outputPath + "src/test/java/org/openapitools/api/PetApiTest.java", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateForTestSpock() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_TEST,
            JavaMicronautClientCodegen.OPT_TEST_SPOCK);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/groovy");
        assertFileExists(outputPath + "src/test/groovy/org/openapitools/api/PetApiSpec.groovy");
        assertFileContains(outputPath + "src/test/groovy/org/openapitools/api/PetApiSpec.groovy", "PetApiSpec", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.java", "public Pet(String name, List<@NotNull String> photoUrls)");
        assertFileNotContains(modelPath + "Pet.java", "public Pet()");
    }

    @Test
    void doNotGenerateRequiredPropertiesInConstructor() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileNotContainsRegex(modelPath + "Pet.java", "public Pet\\([^)]+\\)");
        assertFileNotContainsRegex(modelPath + "User.java", "public User\\([^)]+\\)");
        assertFileNotContainsRegex(modelPath + "Order.java", "public Order\\([^)]+\\)");
    }

    @Test
    void doGenerateMultipleContentTypes() {
        var codegen = new JavaMicronautClientCodegen();

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/content-type.yaml", CodegenConstants.APIS);

        // body and response content types should be properly annotated using @Consumes and @Produces micronaut annotations
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "DefaultApi.java", "@Consumes({\"application/vnd.oracle.resource+json; type=collection\", \"application/vnd.oracle.resource+json; type=error\"})");
        assertFileContains(apiPath + "DefaultApi.java", "@Produces(\"application/vnd.oracle.resource+json; type=singular\")");
    }

    @Test
    void doGenerateOauth2InApplicationConfig() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/oauth2.yaml", CodegenConstants.SUPPORTING_FILES);

        // micronaut yaml property names shouldn't contain any dots
        String resourcesPath = outputPath + "src/main/resources/";
        assertFileContains(resourcesPath + "application.yml", "OAuth_2_0_Client_Credentials:");
    }

    @Test
    void testAdditionalClientTypeAnnotations() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, "@MyAdditionalAnnotation1(1,${param1});@MyAdditionalAnnotation2(2,${param2});");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java",
            "@MyAdditionalAnnotation1(1,${param1})", "@MyAdditionalAnnotation2(2,${param2})");
    }

    @Test
    void testAdditionalClientTypeAnnotationsFromSetter() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setAdditionalClientTypeAnnotations(List.of("@MyAdditionalAnnotation1(1,${param1})", "@MyAdditionalAnnotation2(2,${param2})"));
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java",
            "@MyAdditionalAnnotation1(1,${param1})", "@MyAdditionalAnnotation2(2,${param2})");
    }

    @Test
    void testDefaultAuthorizationFilterPattern() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should default to match all patterns
        assertFileContains(outputPath + "/src/main/java/org/openapitools/auth/AuthorizationFilter.java", "@Filter(Filter.MATCH_ALL_PATTERN)");
    }

    @Test
    void testAuthorizationFilterPattern() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        codegen.additionalProperties().put(JavaMicronautClientCodegen.AUTHORIZATION_FILTER_PATTERN, "pet/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should match the provided pattern
        assertFileContains(outputPath + "/src/main/java/org/openapitools/auth/AuthorizationFilter.java", "@Filter(\"pet/**\")");
    }

    @Test
    void testNoConfigureClientId() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should not specify a Client id
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"${openapi-micronaut-client.base-path}\")");
    }

    @Test
    void testConfigureClientId() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.CLIENT_ID, "unit-test");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"unit-test\")");
    }

    @Test
    void testConfigureClientIdWithPath() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.CLIENT_ID, "unit-test");
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CLIENT_PATH, true);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(id = \"unit-test\", path = \"${unit-test.base-path}\")");
    }

    @Test
    void testDefaultPathSeparator() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the default path separator
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"${openapi-micronaut-client.base-path}\")");
    }

    @Test
    void testConfigurePathSeparator() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.BASE_PATH_SEPARATOR, "-");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided path separator
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"${openapi-micronaut-client-base-path}\")");
    }

    @Test
    void testReadOnlyConstructorBug() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml", CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name)");
        assertFileContains(apiPath + "ExtendedBookInfo.java", "public ExtendedBookInfo(String isbn, String name)", "super(name)");
    }

    @Test
    void testAddValidAnnotations() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithlist.yml", CodegenConstants.APIS, CodegenConstants.API_TESTS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BooksContainer.java",
            """
                    @JsonProperty(JSON_PROPERTY_BOOKS)
                    private List<@Valid Book> books;
                """);
    }

    @Test
    void testGenericAnnotations() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BooksApi.java",
            "@QueryValue(\"before\") @NotNull @Format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\") ZonedDateTime before,",
            "List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String> requestBody",
            ""
        );
        assertFileContains(modelPath + "CountsContainer.java", "private List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull ZonedDateTime>>> counts;");
        assertFileContains(modelPath + "BooksContainer.java", "private List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String> books;");
    }

    @Test
    void testDiscriminatorConstructorBug() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml",
            CodegenConstants.MODELS
        );
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name)");
        assertFileContains(apiPath + "BasicBookInfo.java", "public BasicBookInfo(String author, String name)", "super(name)");
        assertFileContains(apiPath + "DetailedBookInfo.java", "public DetailedBookInfo(String isbn, String name, String author)", "super(author, name)");
    }

    @Test
    void testDifferentPropertyCase() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/propWithSecondUpperCaseChar.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(
            modelPath + "Book.java",
            "public static final String JSON_PROPERTY_TITLE = \"tItle\";",
            "public static final String JSON_PROPERTY_I_S_B_N = \"ISBN\";",
            "private String title;",
            "public String getTitle()",
            "public void setTitle(String title)",
            "private String ISBN;",
            "public String getISBN()",
            "public void setISBN(String ISBN)"
        );
    }

    @Test
    void testEnumsWithNonStringTypeValue() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(modelPath + "StringEnum.java", "@JsonProperty(\"starting\")", "STARTING(\"starting\"),",
            """
                    public final static Map<String, StringEnum> VALUE_MAPPING = Map.copyOf(Arrays.stream(values())
                            .collect(Collectors.toMap(v -> v.value, Function.identity())));
                """,
            """
                    public static StringEnum fromValue(String value) {
                        if (!VALUE_MAPPING.containsKey(value)) {
                            throw new IllegalArgumentException("Unexpected value '" + value + "'");
                        }
                        return VALUE_MAPPING.get(value);
                    }
                """);

        assertFileContains(modelPath + "IntEnum.java", "@JsonProperty(\"1\")", "NUMBER_1(1),");
        assertFileContains(modelPath + "LongEnum.java", "@JsonProperty(\"1\")", "NUMBER_3(3L),");
        assertFileContains(modelPath + "DecimalEnum.java", "@JsonProperty(\"1.23\")", "NUMBER_34_DOT_1(new BigDecimal(\"34.1\"))");
    }

    @Test
    void testUnderscore() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/underscore.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(modelPath + "MyModel.java", "private BigDecimal _default;",
            "public static final String JSON_PROPERTY_DEFAULT = \"_default\";",
            "public BigDecimal get_default() {",
            "public void set_default(BigDecimal _default) {");
    }

    @Test
    void testReservedWords() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/javaReservedWords.yml",
            CodegenConstants.APIS,
            CodegenConstants.MODELS,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.MODEL_TESTS,
            CodegenConstants.MODEL_DOCS,
            CodegenConstants.API_TESTS,
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
    }

    @Test
    void testControllerEnums2() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(modelPath + "BusinessCardsApi.java", "@QueryValue(\"statusCodes\") @Nullable List<@NotNull String> statusCodes");
    }

    @Test
    void testCommonPathParametersWithRef() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.java", "@Get(\"/v1/forecast/{id}\")",
            "@PathVariable(\"id\") @NotNull String id,",
            "@QueryValue(\"hourly\") @Nullable List<V1ForecastIdGetHourlyParameterInner> hourly,");

        assertFileContains(path + "model/V1ForecastIdGetHourlyParameterInner.java",
            "public enum V1ForecastIdGetHourlyParameterInner {",
            "@JsonProperty(\"temperature_2m\")",
            "TEMPERATURE_2M(\"temperature_2m\"),");
    }

    @Test
    void testExtraAnnotations() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/BooksApi.java",
            """
                    @Post("/add-book")
                    @NotBlank
                    Mono<@Valid Book> addBook(
                """);

        assertFileContains(path + "model/Book.java",
            """
                @Serializable
                public class Book {
                """,
            """
                    @NotNull
                    @Size(max = 10)
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
    void testOneOf() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/Subject.java", "String getTypeCode();");
        assertFileContains(path + "model/Person.java", "public String getTypeCode() {");
    }

    @Test
    void testOneOfWithoutDiscriminator() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-without-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileNotContains(path + "model/OrderDTOShoppingNotes.java", "@JsonIgnoreProperties(",
            "@JsonTypeInfo"
        );
    }

    @Test
    void testDiscriminatorCustomType() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator2.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/CancellationReasonTypesV2.java", """
                @NotNull
                @JsonProperty(JSON_PROPERTY_VERSION)
                protected Integer version;
            """);
        assertFileContains(path + "model/CancellationReasonTypesDTO.java", "Integer getVersion();");
    }

    @Test
    void testUuidWithModelNameSuffix() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setModelNameSuffix("Dto");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/schema-with-uuid.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/OrderDTODto.java", "private UUID id;");
    }

    @Test
    void testParamsWithDefaultValue() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java",
            "@QueryValue(\"ids\") @Nullable List<@NotNull Integer> ids",
            "@PathVariable(name = \"apiVersion\", defaultValue = \"v5\") @Nullable BrowseSearchOrdersApiVersionParameter apiVersio",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable String contentType"
        );
    }

    @Test
    void testFileDownloadEndpoint() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/file-download.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.java", "Mono<HttpResponse<@NotNull ByteBuffer<?>>> fetchData(");
    }

    @Test
    void testSingleProduceContentType() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/client-produces-content-type.yml", CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/FilesApi.java", "@Produces(\"application/octet-stream\")");
    }

    @Test
    void testLombok() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setLombok(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/V1ForecastIdGet400Response.java",
            "import lombok.AllArgsConstructor;",
            "import lombok.NoArgsConstructor;",
            "import lombok.Data;",
            "import lombok.RequiredArgsConstructor;",
            "import lombok.EqualsAndHashCode;",
            "import lombok.Getter;",
            "import lombok.Setter;",
            "import lombok.ToString;",
            "import lombok.experimental.Accessors;",
            "@Accessors(chain = true)",
            "@NoArgsConstructor",
            "@AllArgsConstructor",
            "@Data");
    }

    @Test
    void testImplicitHeaders() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setImplicitHeaders(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml", CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileNotContains(path + "api/DefaultApi.java", "@Header(\"X-Favor-Token\") @Nullable String xFavorToken",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable String contentType"
        );
    }

    @Test
    void testImplicitHeadersRegex() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setImplicitHeadersRegex(".*");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-default-value.yml", CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileNotContains(path + "api/DefaultApi.java", "@Header(\"X-Favor-Token\") @Nullable String xFavorToken",
            "@Header(name = \"Content-Type\", defaultValue = \"application/json\") @Nullable String contentType"
        );
    }

    @Test
    void testInnerEnum() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/inner-enum.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/CustomerCreateDTO.java", "import java.util.function.Function;");
    }

    @Test
    void testDiscriminatorWithoutUseOneOfInterfaces() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discirminator2.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/JsonOp.java",
            "private String path;",
            "private String op;",
            "public JsonOp(String path, String op) {"
        );

        assertFileNotContains(path + "model/JsonOp.java",
            "private String value;",
            "private String from;"
        );

        assertFileContains(path + "model/OpAdd.java",
            "public class OpAdd extends JsonOp {",
            "private String value;",
            """
                    public OpAdd(String path, String op) {
                        super(path, op);
                    }
                """
        );
    }

    @Test
    void testDiscriminatorWithUseOneOfInterfaces() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setUseOneOfInterfaces(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discirminator2.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/JsonOp.java",
            "public interface JsonOp {",
            "String getOp();"
        );

        assertFileContains(path + "model/OpAdd.java",
            "public class OpAdd implements JsonOp {",
            "private String value;",
            "private String path;",
            "protected String op;",
            """
                    public OpAdd(String path, String op) {
                        this.path = path;
                        this.op = op;
                    }
                """,
            """
                    @Override
                    public String getOp() {
                        return op;
                    }
                """
        );
    }

    @Test
    void testMultipartFormData() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipartdata.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ResetPasswordApi.java", """
                @Produces("multipart/form-data")
                Mono<@Valid SuccessResetPassword> profilePasswordPost(
                    @Header("WCToken") @NotNull String wcToken,
                    @Header("WCTrustedToken") @NotNull String wcTrustedToken,
                    @Body @Nullable MultipartBody multipartBody
                );
            """);
    }

    @Test
    void testGenerateByMultipleFiles() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple/swagger.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/CustomerApi.java",
            """
                    @Post("/api/customer/{id}/files")
                    Mono<HttpResponse<@NotNull String>> uploadFile(
                        @PathVariable("id") @NotNull UUID id,
                        @Body @NotNull @Valid FileCreateDto fileCreateDto
                    );
                """);
        assertFileContains(path + "model/FileCreateDto.java",
            """
                public class FileCreateDto {
                
                    public static final String JSON_PROPERTY_TYPE_CODE = "typeCode";
                    public static final String JSON_PROPERTY_ORG_NAME = "orgName";
                
                    /**
                     * Customer type ORG
                     */
                    @NotNull
                    @Pattern(regexp = "^ORG$")
                    @JsonProperty(JSON_PROPERTY_TYPE_CODE)
                    private String typeCode = "ORG";
                
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_ORG_NAME)
                    private String orgName;
                
                    public FileCreateDto(String typeCode, String orgName) {
                        this.typeCode = typeCode;
                        this.orgName = orgName;
                    }
                """);
    }

    @Test
    void testMultipleContentTypesEndpoints() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple-content-types.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java", """
                    @Post("/multiplecontentpath")
                    @Produces({"application/json", "application/xml"})
                    Mono<HttpResponse<Void>> myOp(
                        @Body @Nullable @Valid Coordinates coordinates
                    );
                """,
            """
                    @Post("/multiplecontentpath")
                    @Produces("multipart/form-data")
                    Mono<HttpResponse<Void>> myOp_1(
                        @Nullable @Valid Coordinates coordinates,
                        @Nullable byte[] file
                    );
                """,
            """
                    @Post("/multiplecontentpath")
                    @Produces({"application/yaml", "text/json"})
                    Mono<HttpResponse<Void>> myOp_2(
                        @Body @Nullable @Valid MySchema mySchema
                    );
                """);
    }

    @Test
    void testUseEnumCaseInsensitive() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setUseEnumCaseInsensitive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/StringEnum.java",
            """
                    public final static Map<String, StringEnum> VALUE_MAPPING = Map.copyOf(Arrays.stream(values())
                            .collect(Collectors.toMap(v -> v.value.toLowerCase(), Function.identity())));
                """,
            """
                    public static StringEnum fromValue(String value) {
                        var key = value.toLowerCase();
                        if (!VALUE_MAPPING.containsKey(key)) {
                            throw new IllegalArgumentException("Unexpected value '" + key + "'");
                        }
                        return VALUE_MAPPING.get(key);
                    }
                """);
    }
}
