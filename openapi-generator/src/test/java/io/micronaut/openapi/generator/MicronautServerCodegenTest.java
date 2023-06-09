package io.micronaut.openapi.generator;

import io.micronaut.openapi.generator.assertions.TestUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MicronautServerCodegenTest extends AbstractMicronautCodegenTest {
    protected static String ROLES_EXTENSION_TEST_PATH = "src/test/resources/3_0/micronaut/roles-extension-test.yaml";
    protected static String MULTI_TAGS_TEST_PATH = "src/test/resources/3_0/micronaut/multi-tags-test.yaml";

    @Test
    public void clientOptsUnicity() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.cliOptions()
                .stream()
                .collect(groupingBy(CliOption::getOpt))
                .forEach((k, v) -> assertEquals(v.size(), 1, k + " is described multiple times"));
    }

    @Test
    public void testInitialConfigValues() {
        final JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
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
        assertEquals(codegen.additionalProperties().get(JavaMicronautServerCodegen.OPT_CONTROLLER_PACKAGE), "org.openapitools.controller");
        assertEquals(codegen.getInvokerPackage(), "org.openapitools");
        assertEquals(codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE), "org.openapitools");
    }

    @Test
    public void testApiAndModelFilesPresent() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
                CodegenConstants.SUPPORTING_FILES,
                CodegenConstants.APIS,
                CodegenConstants.MODELS);

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
    public void doUseValidationParam() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
                CodegenConstants.APIS);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/java/org/openapitools/api/";
        assertFileContains(apiFolder + "PetApi.java", "@Valid");
        assertFileContains(apiFolder + "PetApi.java", "@NotNull");
    }

    @Test
    public void doNotUseValidationParam() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
                CodegenConstants.APIS);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/java/org/openapitools/api/";
        assertFileNotContains(apiFolder + "PetApi.java", "@Valid");
        assertFileNotContains(apiFolder + "PetApi.java", "@NotNull");
    }

    @Test
    public void doGenerateForTestJUnit() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_TEST,
                JavaMicronautServerCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
                CodegenConstants.SUPPORTING_FILES,
                CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/java/");
        String apiTestFolder = outputPath + "src/test/java/org/openapitools/api/";
        assertFileExists(apiTestFolder + "PetApiTest.java");
        assertFileContains(apiTestFolder + "PetApiTest.java", "PetApiTest", "@MicronautTest");
    }

    @Test
    public void doGenerateForTestSpock() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_TEST,
                JavaMicronautServerCodegen.OPT_TEST_SPOCK);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
                CodegenConstants.SUPPORTING_FILES,
                CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/groovy");
        String apiTestFolder = outputPath + "src/test/groovy/org/openapitools/api/";
        assertFileExists(apiTestFolder + "PetApiSpec.groovy");
        assertFileContains(apiTestFolder + "PetApiSpec.groovy", "PetApiSpec", "@MicronautTest");
    }

    @Test
    public void doGenerateRequiredPropertiesInConstructor() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.java", "public Pet(String name, List<String> photoUrls)");
        assertFileNotContains(modelPath + "Pet.java", "public Pet()");
        assertFileContains(modelPath + "User.java", "public User()");
        assertFileContains(modelPath + "Order.java", "public Order()");
    }

    @Test
    public void doNotGenerateRequiredPropertiesInConstructor() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.java", "public Pet()");
        assertFileNotContainsRegex(modelPath + "Pet.java", "public Pet\\([^)]+\\)");
        assertFileContains(modelPath + "User.java", "public User()");
        assertFileNotContainsRegex(modelPath + "User.java", "public User\\([^)]+\\)");
        assertFileContains(modelPath + "Order.java", "public Order()");
        assertFileNotContainsRegex(modelPath + "Order.java", "public Order\\([^)]+\\)");
    }

    @Test
    @Disabled("Feature may not be fully implemented in OpenAPI generator")
    public void testExtraAnnotations() {

        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/issue_11772.yml", CodegenConstants.MODELS);

        TestUtils.assertExtraAnnotationFiles(outputPath + "/src/main/java/org/openapitools/model");

    }

    @Test
    public void doNotGenerateAuthRolesWithExtensionWhenNotUseAuth() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_USE_AUTH, false);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileNotContains(apiPath + "BooksApi.java", "@Secured");
        assertFileNotContains(apiPath + "UsersApi.java", "@Secured");
        assertFileNotContains(apiPath + "ReviewsApi.java", "@Secured");
    }

    @Test
    public void generateAuthRolesWithExtension() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_USE_AUTH, true);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_ANONYMOUS[^;]{0,100}bookSearchGet");
        assertFileContainsRegex(apiPath + "BooksApi.java", "@Secured\\(\\{\"admin\"\\}\\)[^;]{0,100}createBook");
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_ANONYMOUS[^;]{0,100}getBook");
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_AUTHENTICATED[^;]{0,100}reserveBook");

        assertFileContainsRegex(apiPath + "ReviewsApi.java", "IS_AUTHENTICATED[^;]{0,100}bookSendReviewPost");
        assertFileContainsRegex(apiPath + "ReviewsApi.java", "IS_ANONYMOUS[^;]{0,100}bookViewReviewsGet");

        assertFileContainsRegex(apiPath + "UsersApi.java", "IS_ANONYMOUS[^;]{0,100}getUserProfile");
        assertFileContainsRegex(apiPath + "UsersApi.java", "IS_AUTHENTICATED[^;]{0,100}updateProfile");
    }

    @Test
    public void doGenerateMonoWrapHttpResponse() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_WRAP_IN_HTTP_RESPONSE, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Mono<HttpResponse<Pet>>");
    }

    @Test
    public void doGenerateMono() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_WRAP_IN_HTTP_RESPONSE, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Mono<Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    public void doGenerateWrapHttpResponse() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_WRAP_IN_HTTP_RESPONSE, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "HttpResponse<Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "Mono");
    }

    @Test
    public void doGenerateNoMonoNoWrapHttpResponse() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_WRAP_IN_HTTP_RESPONSE, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Pet");
        assertFileNotContains(apiPath + "PetApi.java", "Mono");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    public void doGenerateOperationOnlyForFirstTag() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, CodegenConstants.MODELS,
                CodegenConstants.APIS, CodegenConstants.API_TESTS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/java/org/openapitools/api/";

        // Verify files are generated only for the required tags
        assertFileExists(apiPath + "AuthorsApi.java");
        assertFileExists(apiPath + "BooksApi.java");
        assertFileNotExists(apiPath + "SearchApi.java");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.java");
        assertFileExists(apiTestPath + "BooksApiTest.java");
        assertFileNotExists(apiTestPath + "SearchApiTest.java");

        // Verify all the methods are generated only ones
        assertFileContains(apiPath + "AuthorsApi.java",
                "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.java",
                "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable");
        assertFileNotContains(apiPath + "BooksApi.java", "getAuthorBooks");
    }

    @Test
    public void doRepeatOperationForAllTags() {
        JavaMicronautServerCodegen codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "false");
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, CodegenConstants.MODELS,
                CodegenConstants.APIS, CodegenConstants.API_TESTS);

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
}
