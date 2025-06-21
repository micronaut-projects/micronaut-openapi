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

class JavaMicronautClientCodegenTest extends AbstractMicronautCodegenTest {

    @Test
    void clientOptsUniqueness() {
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

        assertEquals(Boolean.FALSE, codegen.additionalProperties().get(CodegenConstants.HIDE_GENERATION_TIMESTAMP));
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
        assertFileDoesntExist(outputPath + "/src/main/java/org/openapitools/auth/");
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

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/content-type.yml", CodegenConstants.APIS);

        // body and response content types should be properly annotated using @Consumes and @Produces micronaut annotations
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "DefaultApi.java", "@Consumes({\"application/vnd.oracle.resource+json; type=collection\", \"application/vnd.oracle.resource+json; type=error\"})");
        assertFileContains(apiPath + "DefaultApi.java", "@Produces(\"application/vnd.oracle.resource+json; type=singular\")");
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
        assertFileContains(outputPath + "/src/main/java/org/openapitools/auth/AuthorizationFilter.java", "@Filter(patterns = Filter.MATCH_ALL_PATTERN)");
    }

    @Test
    void testAuthorizationFilterPattern() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        codegen.additionalProperties().put(JavaMicronautClientCodegen.AUTHORIZATION_FILTER_PATTERN, "pet/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should match the provided pattern
        assertFileContains(outputPath + "/src/main/java/org/openapitools/auth/AuthorizationFilter.java", "@Filter(patterns = \"pet/**\")");
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
                    public static final Map<String, StringEnum> VALUE_MAPPING = Map.copyOf(Arrays.stream(values())
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

        assertFileContains(modelPath + "BusinessCardsApi.java", "@QueryValue(\"statusCodes\") @Nullable @Format(FORMAT_MULTI) List<@NotNull String> statusCodes");
    }

    @Test
    void testCommonPathParametersWithRef() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.java", "@Get(\"/v1/forecast/{id}\")",
            "@PathVariable(\"id\") @NotNull String id,",
            "@QueryValue(\"hourly\") @Nullable List<V1ForecastIdGetHourlyParameterInner> hourly,",
            "@QueryValue(\"daily\") @Nullable @Format(FORMAT_MULTI) List<V1ForecastIdGetDailyParameterInner> daily,"
        );

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

        assertFileNotContains(path + "model/ShoppingNotesDTO.java", "@JsonIgnoreProperties(",
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
            "@QueryValue(\"ids\") @Nullable @Format(FORMAT_MULTI) List<@NotNull Integer> ids",
            "@PathVariable(name = \"apiVersion\", defaultValue = \"v5\") @NotNull BrowseSearchOrdersApiVersionParameter apiVersion",
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminator2.yml", CodegenConstants.MODELS);
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
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminator2.yml", CodegenConstants.MODELS);
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
                        byte @Nullable [] file
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
                    public static final Map<String, StringEnum> VALUE_MAPPING = Map.copyOf(Arrays.stream(values())
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

        assertFileContains(path + "model/DecimalEnum.java",
            """
                    public static final Map<BigDecimal, DecimalEnum> VALUE_MAPPING = Map.copyOf(Arrays.stream(values())
                        .collect(Collectors.toMap(v -> v.value, Function.identity())));
                """,
            """
                    public static DecimalEnum fromValue(BigDecimal value) {
                        if (!VALUE_MAPPING.containsKey(value)) {
                            throw new IllegalArgumentException("Unexpected value '" + value + "'");
                        }
                        return VALUE_MAPPING.get(value);
                    }
                """);
    }

    @Test
    void testAdditionalAnnotations() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setAdditionalClientTypeAnnotations(List.of("@java.io.MyAnnotation1"));
        codegen.setAdditionalModelTypeAnnotations(List.of("@java.io.MyAnnotation2"));
        codegen.setAdditionalOneOfTypeAnnotations(List.of("@java.io.MyAnnotation3"));
        codegen.setAdditionalEnumTypeAnnotations(List.of("@java.io.MyAnnotation4"));
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/SubjectsApi.java", "@java.io.MyAnnotation1");
        assertFileContains(path + "model/Person.java", "@java.io.MyAnnotation2");
        assertFileContains(path + "model/Subject.java", "@java.io.MyAnnotation3");
        assertFileContains(path + "model/PersonSex.java", "@java.io.MyAnnotation4");
    }

    @Test
    void testAdditionalAnnotations2() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().putAll(Map.of(
            "additionalClientTypeAnnotations", List.of("@java.io.MyAnnotation1"),
            "additionalModelTypeAnnotations", List.of("@java.io.MyAnnotation2"),
            "additionalOneOfTypeAnnotations", List.of("@java.io.MyAnnotation3"),
            "additionalEnumTypeAnnotations", "@java.io.MyAnnotation41;@java.io.MyAnnotation42;\n@java.io.MyAnnotation43;"
        ));
        codegen.processOpts();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oneof-with-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/SubjectsApi.java", "@java.io.MyAnnotation1");
        assertFileContains(path + "model/Person.java", "@java.io.MyAnnotation2");
        assertFileContains(path + "model/Subject.java", "@java.io.MyAnnotation3");
        assertFileContains(path + "model/PersonSex.java", "@java.io.MyAnnotation41\n", "@java.io.MyAnnotation42\n", "@java.io.MyAnnotation43\n");
    }

    @Test
    void testEnumsExtensionsAndPrimitives() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(modelPath + "BytePrimitiveEnum.java",
            "NUMBER_1((byte) 1),",
            "private final byte value",
            "BytePrimitiveEnum(byte value)",
            "public byte getValue() {");

        assertFileContains(modelPath + "CharPrimitiveEnum.java",
            "A('a'),",
            "private final char value",
            "CharPrimitiveEnum(char value)",
            "public char getValue() {");

        assertFileContains(modelPath + "ShortPrimitiveEnum.java",
            "NUMBER_1((short) 1),",
            "private final short value",
            "ShortPrimitiveEnum(short value)",
            "public short getValue() {");

        assertFileContains(modelPath + "IntPrimitiveEnum.java",
            "NUMBER_1(1),",
            "private final int value",
            "IntPrimitiveEnum(int value)",
            "public int getValue() {");

        assertFileContains(modelPath + "LongPrimitiveEnum.java",
            "NUMBER_1(1L),",
            "private final long value",
            "LongPrimitiveEnum(long value)",
            "public long getValue() {");

        assertFileContains(modelPath + "FloatPrimitiveEnum.java",
            "NUMBER_1_DOT_23(1.23F),",
            "private final float value",
            "FloatPrimitiveEnum(float value)",
            "public float getValue() {");

        assertFileContains(modelPath + "DoublePrimitiveEnum.java",
            "NUMBER_1_DOT_23(1.23),",
            "private final double value",
            "DoublePrimitiveEnum(double value)",
            "public double getValue() {");

        assertFileContains(modelPath + "StringEnum.java",
            """
                    @Deprecated
                    @JsonProperty("starting")
                    STARTING("starting"),
                """,
            """
                    @Deprecated
                    @JsonProperty("running")
                    RUNNING("running"),
                """);

        assertFileContains(modelPath + "DecimalEnum.java",
            """
                    @Deprecated
                    @JsonProperty("34.1")
                    NUMBER_34_DOT_1(new BigDecimal("34.1")),
                    ;
                """);

        assertFileContains(modelPath + "ByteEnum.java",
            "NUMBER_1((byte) 1),",
            "private final Byte value",
            "ByteEnum(Byte value)",
            "public Byte getValue() {");

        assertFileContains(modelPath + "ShortEnum.java",
            "NUMBER_1((short) 1),",
            "private final Short value",
            "ShortEnum(Short value)",
            "public Short getValue() {");

        assertFileContains(modelPath + "IntEnum.java",
            """
                    /**
                     * This is one
                     */
                    @JsonProperty("1")
                    THE_ONE(1),
                """,
            """
                    @Deprecated
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

        assertFileContains(modelPath + "LongEnum.java",
            """
                    @Deprecated
                    @JsonProperty("2")
                    NUMBER_2(2L),
                """);
    }

    @Test
    void testPrimitives() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/model-with-primitives.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String basePath = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(basePath + "api/ParametersApi.java",
            "@QueryValue(\"name\") @NotNull String name",
            "@QueryValue(\"byteType\") @NotNull Byte byteType",
            "@QueryValue(\"byteType2\") @NotNull Byte byteType2",
            "@QueryValue(\"shortType\") @NotNull Short shortType",
            "@QueryValue(\"shortType2\") @NotNull Short shortType2",
            "@QueryValue(\"intType\") @NotNull Integer intType",
            "@QueryValue(\"longType\") @NotNull Long longType",
            "@QueryValue(\"boolType\") @NotNull Boolean boolType",
            "@QueryValue(\"decimalType\") @NotNull BigDecimal decimalType",
            "@QueryValue(\"floatType\") @NotNull Float floatType",
            "@QueryValue(\"doubleType\") @NotNull Double doubleType",
            "@QueryValue(\"bytePrimitiveType\") @NotNull Byte bytePrimitiveType",
            "@QueryValue(\"shortPrimitiveType\") @NotNull Short shortPrimitiveType",
            "@QueryValue(\"intPrimitiveType\") @NotNull Integer intPrimitiveType",
            "@QueryValue(\"longPrimitiveType\") @NotNull Long longPrimitiveType",
            "@QueryValue(\"floatPrimitiveType\") @NotNull Float floatPrimitiveType",
            "@QueryValue(\"doublePrimitiveType\") @NotNull Double doublePrimitiveType",
            "@QueryValue(\"charPrimitiveType\") @NotNull Character charPrimitiveType",
            "@QueryValue(\"bytePrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Byte> bytePrimitiveTypes",
            "@QueryValue(\"shortPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Short> shortPrimitiveTypes",
            "@QueryValue(\"intPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Integer> intPrimitiveTypes",
            "@QueryValue(\"longPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Long> longPrimitiveTypes",
            "@QueryValue(\"floatPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Float> floatPrimitiveTypes",
            "@QueryValue(\"doublePrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Double> doublePrimitiveTypes",
            "@QueryValue(\"charPrimitiveTypes\") @NotNull @Format(FORMAT_MULTI) List<Character> charPrimitiveTypes",
            "@QueryValue(\"byteTypes\") @NotNull @Format(FORMAT_MULTI) List<@NotNull Byte> byteTypes",
            "@QueryValue(\"byteTypes2\") @NotNull @Format(FORMAT_MULTI) List<@NotNull Byte> byteTypes2",
            "@QueryValue(\"shortTypes\") @NotNull @Format(FORMAT_MULTI) List<@NotNull Short> shortTypes",
            "@QueryValue(\"shortTypes2\") @NotNull @Format(FORMAT_MULTI) List<@NotNull Short> shortTypes2",
            "@QueryValue(\"intTypes\") @NotNull @Format(FORMAT_MULTI) List<@NotNull Integer> intTypes",
            "@QueryValue(\"longTypes\") @NotNull @Format(FORMAT_MULTI) List<@NotNull Long> longTypes"
        );

        assertFileContains(basePath + "model/Obj.java",
            "private String name",
            "private Byte byteType",
            "private Byte byteType2",
            "private Short shortType",
            "private Short shortType2",
            "private Integer intType",
            "private Long longType",
            "private Boolean boolType",
            "private BigDecimal decimalType",
            "private Float floatType",
            "private Double doubleType",
            "private Byte bytePrimitiveType",
            "private Short shortPrimitiveType",
            "private Integer intPrimitiveType",
            "private Long longPrimitiveType",
            "private Float floatPrimitiveType",
            "private Double doublePrimitiveType",
            "private Character charPrimitiveType",
            "private List<Byte> bytePrimitiveTypes",
            "private List<Short> shortPrimitiveTypes",
            "private List<Integer> intPrimitiveTypes",
            "private List<Long> longPrimitiveTypes",
            "private List<Float> floatPrimitiveTypes",
            "private List<Double> doublePrimitiveTypes",
            "private List<Character> charPrimitiveTypes",
            "private List<@NotNull Byte> byteTypes",
            "private List<@NotNull Byte> byteTypes2",
            "private List<@NotNull Short> shortTypes",
            "private List<@NotNull Short> shortTypes2",
            "private List<@NotNull Integer> intTypes",
            "private List<@NotNull Long> longTypes"
        );
    }

    @Test
    void testDeprecated() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/deprecated.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
                     * @return Success (status code 200)
                     *         or An unexpected error has occurred (status code default)
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
                    Mono<@Valid SendPrimitivesResponse> sendPrimitives(
                        @PathVariable("name") @NotNull @Deprecated String name,
                        @QueryValue("age") @NotNull BigDecimal age,
                        @Header("height") @NotNull @Deprecated Float height
                    );
                """);

        assertFileContains(path + "model/SendPrimitivesResponse.java",
            """
                /**
                 * SendPrimitivesResponse
                 *
                 * @deprecated Deprecated message5
                 */
                @Deprecated
                """,
            """
                    /**
                     * @deprecated Deprecated message6
                     */
                    @Deprecated
                    @Nullable
                    @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED, deprecated = true)
                    @JsonProperty(JSON_PROPERTY_NAME)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private String name;
                """,
            """
                    /**
                     * @return the name property value
                     *
                     * @deprecated Deprecated message6
                     */
                    @Deprecated
                    public String getName() {
                        return name;
                    }
                """,
            """
                    /**
                     * Set the name property value
                     *
                     * @param name property value to set
                     *
                     * @deprecated Deprecated message6
                     */
                    @Deprecated
                    public void setName(String name) {
                        this.name = name;
                    }
                """,
            """
                    /**
                     * Set name in a chainable fashion.
                     *
                     * @return The same instance of SendPrimitivesResponse for chaining.
                     *
                     * @deprecated Deprecated message6
                     */
                    @Deprecated
                    public SendPrimitivesResponse name(String name) {
                        this.name = name;
                        return this;
                    }
                """);

        assertFileContains(path + "model/StateEnum.java",
            """
                /**
                 * Gets or Sets StateEnum
                 *
                 * @deprecated Deprecated message9
                 */
                @Deprecated
                @Serdeable
                @Generated("io.micronaut.openapi.generator.JavaMicronautClientCodegen")
                public enum StateEnum {
                """
        );
    }

    @Test
    void testCustomValidationMessages() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setUseEnumCaseInsensitive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/validation-messages.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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

        assertFileContains(path + "model/Book.java",
            """
                    @NotNull(message = "This is required string message")
                    @Pattern(regexp = "[a-zA-Z ]+", message = "This is string pattern message")
                    @Size(min = 5, max = 10, message = "This is min max string length message")
                    @JsonProperty(JSON_PROPERTY_STR_PROP)
                    private String strProp;
                """,
            """
                    @Nullable
                    @Pattern(regexp = "[a-zA-Z ]+", message = "This is string pattern message")
                    @Size(min = 5, message = "This is min string length message")
                    @JsonProperty(JSON_PROPERTY_STR_PROP2)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private String strProp2;
                """,
            """
                    @Nullable
                    @Pattern(regexp = "[a-zA-Z ]+", message = "This is string pattern message")
                    @Size(max = 10, message = "This is min string length message")
                    @JsonProperty(JSON_PROPERTY_STR_PROP3)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private String strProp3;
                """,
            """
                    @NotNull(message = "This is required email message")
                    @Size(min = 5, max = 10, message = "This is min max email length message")
                    @Email(regexp = "email@dot.com", message = "This is email pattern message")
                    @JsonProperty(JSON_PROPERTY_EMAIL_PROP)
                    private String emailProp;
                """,
            """
                    @NotNull(message = "This is required int message")
                    @Min(value = 5, message = "This is min message")
                    @Max(value = 10, message = "This is max message")
                    @JsonProperty(JSON_PROPERTY_INT_PROP)
                    private Integer intProp;
                """,
            """
                    @Nullable
                    @Min(value = 0, message = "This is positive message")
                    @JsonProperty(JSON_PROPERTY_POSITIVE_PROP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private Integer positiveProp;
                """,
            """
                    @Nullable
                    @Min(value = 0, message = "This is positive or zero message")
                    @JsonProperty(JSON_PROPERTY_POSITIVE_OR_ZERO_PROP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private Integer positiveOrZeroProp;
                """,
            """
                    @Nullable
                    @Max(value = 0, message = "This is negative message")
                    @JsonProperty(JSON_PROPERTY_NEGATIVE_PROP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private Integer negativeProp;
                """,
            """
                    @Nullable
                    @Max(value = 0, message = "This is negative or zero message")
                    @JsonProperty(JSON_PROPERTY_NEGATIVE_OR_ZERO_PROP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private Integer negativeOrZeroProp;
                """,
            """
                    @Nullable
                    @DecimalMin(value = "5.5", message = "This is decimal min message")
                    @DecimalMax(value = "10.5", message = "This is decimal max message")
                    @JsonProperty(JSON_PROPERTY_DECIMAL_PROP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private BigDecimal decimalProp;
                """,
            """
                    @Nullable
                    @DecimalMin(value = "5.5", inclusive = false, message = "This is decimal min message")
                    @DecimalMax(value = "10.5", inclusive = false, message = "This is decimal max message")
                    @JsonProperty(JSON_PROPERTY_DECIMAL_PROP2)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private BigDecimal decimalProp2;
                """,
            """
                    @Nullable
                    @Size(min = 5, max = 10, message = "This is min max string length message")
                    @JsonProperty(JSON_PROPERTY_ARRAY_PROP1)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private List<@NotNull Integer> arrayProp1;
                """,
            """
                    @Nullable
                    @Size(min = 5, message = "This is min max string length message")
                    @JsonProperty(JSON_PROPERTY_ARRAY_PROP2)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private List<@NotNull Integer> arrayProp2;
                """,
            """
                    @Nullable
                    @Size(max = 10, message = "This is min max string length message")
                    @JsonProperty(JSON_PROPERTY_ARRAY_PROP3)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private List<@NotNull Integer> arrayProp3;
                """
        );
    }

    @Test
    void testNoVars() {

        System.setProperty("micronaut.test.no-vars", "true");

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/Book.java",
            """
                @Serdeable
                @Generated("io.micronaut.openapi.generator.JavaMicronautClientCodegen")
                @Serializable
                public class Book {
                
                    @Override
                """);

        System.clearProperty("micronaut.test.no-vars");
    }

    @Test
    void testSwaggerAnnotations() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/petstore.json", CodegenConstants.APIS, CodegenConstants.MODELS);
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
                        parameters = @Parameter(name = "status", description = "Status values that need to be considered for filter", in = ParameterIn.QUERY)
                            ,
                        security = @SecurityRequirement(name = "petstore_auth", scopes = {"write:pets", "read:pets"})
                    )
                    @Get("/pet/findByStatus")
                    @Consumes({"application/json", "application/xml"})
                    Mono<@NotNull List<@Valid Pet>> findPetsByStatus(
                        @QueryValue(value = "status", defaultValue = "[\\"available\\"]") @Nullable @Format(FORMAT_MULTI) List<@NotNull String> status
                    );
                """);
    }

    @Test
    void testDiscriminatorOverride() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/test-override-discriminator.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/AnimalRequest.java",
            """
                    /**
                     * @return the valueType property value
                     */
                    public String getValueType() {
                        return valueType;
                    }
                
                    /**
                     * Set the valueType property value
                     *
                     * @param valueType property value to set
                     */
                    public void setValueType(String valueType) {
                        this.valueType = valueType;
                    }
                
                    /**
                     * Set valueType in a chainable fashion.
                     *
                     * @return The same instance of AnimalRequest for chaining.
                     */
                    public AnimalRequest valueType(String valueType) {
                        this.valueType = valueType;
                        return this;
                    }
                """);

        assertFileContains(path + "model/AnimalResponse.java",
            """
                    /**
                     * @return the valueType property value
                     */
                    public String getValueType() {
                        return valueType;
                    }
                
                    /**
                     * Set the valueType property value
                     *
                     * @param valueType property value to set
                     */
                    public void setValueType(String valueType) {
                        this.valueType = valueType;
                    }
                
                    /**
                     * Set valueType in a chainable fashion.
                     *
                     * @return The same instance of AnimalResponse for chaining.
                     */
                    public AnimalResponse valueType(String valueType) {
                        this.valueType = valueType;
                        return this;
                    }
                """);
    }

    @Test
    void testEquals() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        codegen.setUseOneOfInterfaces(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/check-equals.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileNotContains(path + "model/SalesInvoiceCreateDto.java", "@JsonPropertyOrder");
        assertFileContains(path + "model/SalesInvoiceCreateDto.java", """
                @Override
                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }
                    if (o == null || getClass() != o.getClass()) {
                        return false;
                    }
                    return super.equals(o);
                }
            
                @Override
                public int hashCode() {
                    return Objects.hash(super.hashCode());
                }
            """);
    }

    @Test
    void testBodyEnum() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/body-enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
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
    void testDateWithoutSizeAnnotations() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DocumentResourcesApi.java", """
            @QueryValue("CREATIONDATE") @Nullable LocalDate CREATIONDATE
            """);
        assertFileContains(path + "model/Result.java", """
                private String id;
            
                @Nullable
                @JsonProperty(JSON_PROPERTY_DATE)
                @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                private ZonedDateTime date;
            """);
    }

    @Test
    void testJsonIncludeAlways() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setJsonIncludeAlwaysForRequiredFields(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/json-include-always.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/TextRequestDto.java",
            """
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_TEXT)
                    @JsonInclude(JsonInclude.Include.ALWAYS)
                    private String text;
                """,
            """
                    @NotNull
                    @Size(min = 1, max = 20)
                    @JsonProperty(JSON_PROPERTY_LOCALE)
                    @JsonInclude(JsonInclude.Include.ALWAYS)
                    private String locale;
                """);

        assertFileContains(path + "model/ResponseDto.java",
            """
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_MESSAGE)
                    @JsonInclude(JsonInclude.Include.ALWAYS)
                    private String message;
                """,
            """
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_TIMESTAMP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    private ZonedDateTime timestamp;
                """);
    }

    @Test
    void testBuiltInModelNamePrefixAndSuffix() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setModelNamePrefix("Api");
        codegen.setModelNameSuffix("Dto");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openapi-built-in-prefix.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java",
            """
                    @Get("/example-route2")
                    Mono<@NotNull Set<@NotNull UUID>> exampleRoute2Get();
                """,
            """
                    @Get("/example-route")
                    Mono<@NotNull Set<@NotNull String>> exampleRouteGet();
                """);
    }

    static Stream<Arguments> sealedScenarios() {
        return Stream.of(
            arguments("oneOf_polymorphismAndInheritance.yml", Map.of(
                "Foo.java", "public final class Foo extends Entity implements FooRefOrValue {",
                "FooRef.java", "public final class FooRef extends EntityRef implements FooRefOrValue {",
                "FooRefOrValue.java", "public sealed interface FooRefOrValue permits Foo, FooRef {",
                "Entity.java", "public sealed class Entity permits Bar, BarCreate, Foo, Pasta, Pizza {")),
            arguments("oneOf_additionalProperties.yml", Map.of(
                "SchemaA.java", "public final class SchemaA implements PostRequest {",
                "PostRequest.java", "public sealed interface PostRequest permits SchemaA {")),
            arguments("oneOf_array.yml", Map.of(
                "OneOf1.java", "public final class OneOf1 {")),
            arguments("oneOf_duplicateArray.yml", Map.of(
                "Example.java", "public interface Example {")),
            arguments("oneOf_nonPrimitive.yml", Map.of(
                "Example.java", "public interface Example {")),
            arguments("oneOf_primitive.yml", Map.of(
                "Child.java", "public final class Child implements Example {",
                "Example.java", "public sealed interface Example permits Child {")),
            arguments("oneOf_primitiveAndArray.yml", Map.of(
                "Example.java", "public interface Example {")),
            arguments("oneOf_reuseRef.yml", Map.of(
                "Fruit.java", "public sealed interface Fruit permits Apple, Banana {",
                "Banana.java", "public final class Banana implements Fruit {",
                "Apple.java", "public final class Apple implements Fruit {")),
            arguments("oneOf_twoPrimitives.yml", Map.of(
                "MyExamplePostRequest.java", "public interface MyExamplePostRequest {")),
            arguments("oneOf_arrayMapImport.yml", Map.of(
                "Fruit.java", "public interface Fruit {",
                "Grape.java", "public final class Grape {",
                "Apple.java", "public final class Apple {")),
            arguments("oneOf_discriminator.yml", Map.of(
                "FruitAllOfDisc.java", "public sealed interface FruitAllOfDisc permits AppleAllOfDisc, BananaAllOfDisc {",
                "FruitReqDisc.java", "public sealed interface FruitReqDisc permits AppleReqDisc, BananaReqDisc {"))
        );
    }

    @MethodSource("sealedScenarios")
    @ParameterizedTest
    public void sealedScenarios(String apiFile, Map<String, String> definitions) {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setUseSealed(true);
        codegen.setUseOneOfInterfaces(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/sealed/" + apiFile, CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/model/";

        definitions.forEach((file, check) ->
            assertFileContains(path + file, check));
    }

    @Test
    void testEnumXimplements() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum-implements.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/Type.java", "public enum Type implements java.io.Serializable {");
    }

    @Test
    void testPascalCaseInMethodName() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/issue_19393_map_of_inner_enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/EmployeeWithMapOfEnum.java",
            """
                    public EmployeeWithMapOfEnum putProjectRoleItem(String key, EmployeeWithMapOfEnumProjectRoleValue projectRoleItem) {
                        if (projectRole == null) {
                            projectRole = new HashMap<>();
                        }
                        projectRole.put(key, projectRoleItem);
                        return this;
                    }
                """
        );
    }

    @Test
    void testDateTimeFormat() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put("dateTimeFormat", "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-time-format.yml", CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/DateTimeResponse.java",
            """
                    @NotNull
                    @JsonProperty(JSON_PROPERTY_MESSAGE)
                    private String message;
                """,
            """
                    @Nullable
                    @JsonProperty(JSON_PROPERTY_TIMESTAMP)
                    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
                    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
                    private ZonedDateTime timestamp;
                """);
    }

    @Test
    void testNoArgsConstructorAlways() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setNoArgsConstructor(true);
        codegen.setRequiredPropertiesInConstructor(true);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/Pet.java", """
                public Pet() {
                }
            
                public Pet(String name, List<@NotNull String> photoUrls) {
                    this.name = name;
                    this.photoUrls = photoUrls;
                }
            """);
    }

    @Test
    void testReadOnlyRequiredPropertyInConstructor() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/oas.yml", CodegenConstants.MODELS, CodegenConstants.APIS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/CategoryObject.java", """
                public CategoryObject(String locale, String name) {
                    this.locale = locale;
                    this.name = name;
                }
            """);
    }

    @Test
    void testParamWithStyle() {
        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/params-with-style.yml", CodegenConstants.MODELS, CodegenConstants.APIS);
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
    void testUseOauth() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setUseOauth(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileNotContains(path + "auth/AuthorizationFilter.java",
            "import org.slf4j.Logger;",
            "import org.slf4j.LoggerFactory;",
            "import io.micronaut.context.BeanContext;",
            "import io.micronaut.core.util.StringUtils;",
            "import io.micronaut.core.util.Toggleable;",
            "import io.micronaut.inject.qualifiers.Qualifiers;",
            "import io.micronaut.security.oauth2.client.clientcredentials.ClientCredentialsClient;",
            "import io.micronaut.security.oauth2.client.clientcredentials.ClientCredentialsConfiguration;",
            "import io.micronaut.security.oauth2.client.clientcredentials.propagation.ClientCredentialsTokenPropagator;",
            "import io.micronaut.security.oauth2.configuration.OauthClientConfiguration;",
            "import java.util.HashMap;",
            "private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);",
            "protected ClientCredentialsTokenPropagator defaultTokenPropagator;",
            "private final BeanContext beanContext;",
            "private final Map<String, OauthClientConfiguration> clientConfigurationByName;",
            "private final Map<String, ClientCredentialsTokenPropagator> tokenPropagatorByName;",
            "private final Map<String, ClientCredentialsClient> clientCredentialsClientByName;",
            "ClientCredentialsClient",
            "Flux<HttpRequest<?>> authorizer = Flux.from(clientCredentialsClient"
        );
    }

    @Test
    void testUseBasicAuth() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setUseBasicAuth(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileExists(path + "auth/config/ConfigurableAuthorization.java");
        assertFileExists(path + "auth/config/ApiKeyAuthConfig.java");
        assertFileDoesntExist(path + "auth/config/HttpBasicAuthConfig.java");
        assertFileExists(path + "auth/AuthorizationFilter.java");
    }

    @Test
    void testUseApiKeyAuth() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setUseApiKeyAuth(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileExists(path + "auth/config/ConfigurableAuthorization.java");
        assertFileDoesntExist(path + "auth/config/ApiKeyAuthConfig.java");
        assertFileExists(path + "auth/config/HttpBasicAuthConfig.java");
        assertFileExists(path + "auth/AuthorizationFilter.java");
    }

    @Test
    void testAuthFilter() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setAuthFilter(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileExists(path + "auth/config/ConfigurableAuthorization.java");
        assertFileExists(path + "auth/config/ApiKeyAuthConfig.java");
        assertFileExists(path + "auth/config/HttpBasicAuthConfig.java");
        assertFileDoesntExist(path + "auth/AuthorizationFilter.java");
    }

    @Test
    void testGenerateAuthClasses() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setGenerateAuthClasses(false);
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileDoesntExist(path + "auth/config/ConfigurableAuthorization.java");
        assertFileDoesntExist(path + "auth/config/ApiKeyAuthConfig.java");
        assertFileDoesntExist(path + "auth/config/HttpBasicAuthConfig.java");
        assertFileDoesntExist(path + "auth/AuthorizationFilter.java");
        assertFileDoesntExist(path + "auth/Authorizations.java");
        assertFileDoesntExist(path + "auth/AuthorizationBinder.java");
        assertFileDoesntExist(path + "auth/Authorization.java");
    }

    @Test
    void testAuthWithClientIdAndMultiplePatterns() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.java",
            "@Filter(serviceId = \"myApiClient\", patterns = {\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"})");
        assertFileContains(path + "auth/AuthorizationBinder.java",
            "public static final CharSequence AUTHORIZATION_NAMES = \"micronaut.security.myApiClient.AUTHORIZATION_NAMES\";");
        assertFileContains(path + "auth/config/ApiKeyAuthConfig.java",
            "@EachProperty(\"security.myApiClient.api-key-auth\")");
        assertFileContains(path + "auth/config/HttpBasicAuthConfig.java",
            "@EachProperty(\"security.myApiClient.basic-auth\")");
    }

    @Test
    void testAuthWithAuthConfigName() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthConfigName("test");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.java",
            "@Filter(serviceId = \"myApiClient\", patterns = Filter.MATCH_ALL_PATTERN)");
        assertFileContains(path + "auth/AuthorizationBinder.java",
            "public static final CharSequence AUTHORIZATION_NAMES = \"micronaut.security.test.AUTHORIZATION_NAMES\";");
        assertFileContains(path + "auth/config/ApiKeyAuthConfig.java",
            "@EachProperty(\"security.test.api-key-auth\")");
        assertFileContains(path + "auth/config/HttpBasicAuthConfig.java",
            "@EachProperty(\"security.test.basic-auth\")");
    }

    @Test
    void testAuthWithAuthFilterClientIds() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.java",
            "@Filter(serviceId = {\"test1\", \"test2\", \"test3\"}, patterns = {\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"})");
    }

    @Test
    void testAuthWithAuthFilterExcludedClientIds() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterExcludedClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.java",
            "@Filter(serviceId = \"myApiClient\", excludeServiceId = {\"test1\", \"test2\", \"test3\"}, patterns = {\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"})");
    }

    @Test
    void testAuthWithAuthFilterExcludedClientIdsAndEmptyAuthFilterClientIds() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterClientIds(List.of());
        codegen.setAuthFilterExcludedClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.java",
            "@Filter(excludeServiceId = {\"test1\", \"test2\", \"test3\"}, patterns = {\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"})");
    }

    @Test
    void testAuthWithAuthorizationFilterPatternStyle() {
        var codegen = new JavaMicronautClientCodegen();
        codegen.setConfigureAuthorization(true);
        codegen.setClientId("myApiClient");
        codegen.setAuthFilterClientIds(List.of());
        codegen.setAuthFilterExcludedClientIds(List.of("test1", "test2", "test3"));
        codegen.setAuthorizationFilterPattern("/v1/user/**;/v1/company/**;/v1/payment/**");
        codegen.setAuthorizationFilterPatternStyle("regex");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS, CodegenConstants.SUPPORTING_FILES);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "auth/AuthorizationFilter.java",
            "import io.micronaut.http.filter.FilterPatternStyle;",
            "@Filter(excludeServiceId = {\"test1\", \"test2\", \"test3\"}, patternStyle = FilterPatternStyle.REGEX, patterns = {\"/v1/user/**\", \"/v1/company/**\", \"/v1/payment/**\"})");
    }

    @Test
    void testEnumConvertersConfig() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS, CodegenConstants.SUPPORTING_FILES);

        String path = outputPath + "src/main/java/org/openapitools/config/";
        assertFileExists(path + "EnumConverterClientConfig.java");

        assertFileContains(path + "EnumConverterClientConfig.java",
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
            "public class EnumConverterClientConfig {",
            "public EnumConverterClientConfig(ObjectMapper objectMapper) {",
            """
                    @Bean
                    public TypeConverter<String, StringEnum> toEnumStringEnum() {
                        return commonToEnumConverter(StringEnum.class, objectMapper);
                    }
                
                    @Bean
                    public TypeConverter<StringEnum, String> toStrStringEnum() {
                        return commonToStrConverter(StringEnum.class, objectMapper);
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, ShortPrimitiveEnum> toEnumShortPrimitiveEnum() {
                        return commonToEnumConverter(ShortPrimitiveEnum.class, objectMapper);
                    }
                
                    @Bean
                    public TypeConverter<ShortPrimitiveEnum, String> toStrShortPrimitiveEnum() {
                        return commonToStrConverter(ShortPrimitiveEnum.class, objectMapper);
                    }
                """
        );
    }

    @Test
    void testEnumConvertersConfigWithCustomClientId() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setClientId("myApiClient");
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS, CodegenConstants.SUPPORTING_FILES);

        String path = outputPath + "src/main/java/org/openapitools/config/";
        assertFileExists(path + "EnumConverterMyApiClientConfig.java");

        assertFileContains(path + "EnumConverterMyApiClientConfig.java",
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
            "public class EnumConverterMyApiClientConfig {",
            """
                    @Bean
                    public TypeConverter<String, StringEnum> toEnumStringEnum() {
                        return commonToEnumConverter(StringEnum.class, objectMapper);
                    }
                
                    @Bean
                    public TypeConverter<StringEnum, String> toStrStringEnum() {
                        return commonToStrConverter(StringEnum.class, objectMapper);
                    }
                """,
            """
                    @Bean
                    public TypeConverter<String, ShortPrimitiveEnum> toEnumShortPrimitiveEnum() {
                        return commonToEnumConverter(ShortPrimitiveEnum.class, objectMapper);
                    }
                
                    @Bean
                    public TypeConverter<ShortPrimitiveEnum, String> toStrShortPrimitiveEnum() {
                        return commonToStrConverter(ShortPrimitiveEnum.class, objectMapper);
                    }
                """
        );
    }

    @Test
    void testEnumConvertersConfigWithoutEnumParams() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/date-annotations.yml", CodegenConstants.APIS, CodegenConstants.MODELS, CodegenConstants.SUPPORTING_FILES);

        String path = outputPath + "src/main/java/org/openapitools/config/";
        assertFileDoesntExist(path + "EnumConverterClientConfig.java");
    }

    @Test
    void testEnumConvertersConfigDisabled() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setGenerateEnumConverters(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS, CodegenConstants.SUPPORTING_FILES);

        String path = outputPath + "src/main/java/org/openapitools/config/";
        assertFileDoesntExist(path + "EnumConverterClientConfig.java");
    }

    @Test
    void testEnumConvertersWithLombok() {

        var codegen = new JavaMicronautClientCodegen();
        codegen.setLombok(true);
        codegen.setGeneratedAnnotation(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/enum2.yml", CodegenConstants.APIS, CodegenConstants.MODELS, CodegenConstants.SUPPORTING_FILES);

        String path = outputPath + "src/main/java/org/openapitools/config/";
        assertFileExists(path + "EnumConverterClientConfig.java");
        assertFileContains(path + "EnumConverterClientConfig.java",
            "import lombok.RequiredArgsConstructor;",
            """
                @RequiredArgsConstructor
                @Factory
                public class EnumConverterClientConfig {
                """
        );
    }
}
