package io.micronaut.openapi.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Consumer;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.AbstractKotlinCodegen;
import org.openapitools.codegen.languages.KotlinClientCodegen;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class MicronautClientCodegenTest extends AbstractMicronautCodegenTest {

    @Test
    void clientOptsUnicity() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(v.size(), 1, k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        final JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        assertEquals(codegen.getInvokerPackage(), "org.openapitools");
        assertEquals(codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE), "org.openapitools");
    }

    @Test
    void testApiAndModelFilesPresent() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Valid");
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        assertFileNotContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Valid");
        assertFileNotContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.java", "public Pet(String name, List<String> photoUrls)");
        assertFileNotContains(modelPath + "Pet.java", "public Pet()");
    }

    @Test
    void doNotGenerateRequiredPropertiesInConstructor() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
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
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/content-type.yaml", CodegenConstants.APIS);

        // body and response content types should be properly annotated using @Consumes and @Produces micronaut annotations
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "DefaultApi.java", "@Consumes({\"application/vnd.oracle.resource+json; type=collection\", \"application/vnd.oracle.resource+json; type=error\"})");
        assertFileContains(apiPath + "DefaultApi.java", "@Produces(\"application/vnd.oracle.resource+json; type=singular\")");
    }

    @Test
    void doGenerateOauth2InApplicationConfig() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");

        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/oauth2.yaml", CodegenConstants.SUPPORTING_FILES);

        // micronaut yaml property names shouldn't contain any dots
        String resourcesPath = outputPath + "src/main/resources/";
        assertFileContains(resourcesPath + "application.yml", "OAuth_2_0_Client_Credentials:");
    }

    @Test
    void testAdditionalClientTypeAnnotations() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.ADDITIONAL_CLIENT_TYPE_ANNOTATIONS, "MyAdditionalAnnotation1(1,${param1});MyAdditionalAnnotation2(2,${param2});");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Micronaut declarative http client should contain custom added annotations
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "MyAdditionalAnnotation1(1,${param1})");
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "MyAdditionalAnnotation2(2,${param2})");
    }

    @Test
    void testDefaultAuthorizationFilterPattern() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should default to match all patterns
        assertFileContains(outputPath + "/src/main/java/org/openapitools/auth/AuthorizationFilter.java", "@Filter(Filter.MATCH_ALL_PATTERN)");
    }

    @Test
    void testAuthorizationFilterPattern() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.OPT_CONFIGURE_AUTH, "true");
        codegen.additionalProperties().put(JavaMicronautClientCodegen.AUTHORIZATION_FILTER_PATTERN, "pet/**");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.SUPPORTING_FILES, CodegenConstants.APIS);

        // Micronaut AuthorizationFilter should match the provided pattern
        assertFileContains(outputPath + "/src/main/java/org/openapitools/auth/AuthorizationFilter.java", "@Filter(\"pet/**\")");
    }

    @Test
    void testNoConfigureClientId() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should not specify a Client id
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"${openapi-micronaut-client-base-path}\")");
    }

    @Test
    void testConfigureClientId() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.CLIENT_ID, "unit-test");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided Client id
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(id = \"unit-test\", path = \"${openapi-micronaut-client-base-path}\")");
    }

    @Test
    void testDefaultPathSeparator() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the default path separator
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"${openapi-micronaut-client-base-path}\")");
    }

    @Test
    void testConfigurePathSeparator() {
        JavaMicronautClientCodegen codegen = new JavaMicronautClientCodegen();
        codegen.additionalProperties().put(JavaMicronautClientCodegen.BASE_PATH_SEPARATOR, ".");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.APIS);

        // Micronaut declarative http client should use the provided path separator
        assertFileContains(outputPath + "/src/main/java/org/openapitools/api/PetApi.java", "@Client(\"${openapi-micronaut-client.base-path}\")");
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
                        @Valid
                        @JsonProperty(JSON_PROPERTY_BOOKS)
                        private List<Book> books;
                    """);
    }

    @Test
    void testDiscriminatorConstructorBug() {

        var codegen = new JavaMicronautClientCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/micronaut/roles-extension-test.yaml",
            CodegenConstants.APIS,
            CodegenConstants.API_TESTS,
//            CodegenConstants.API_DOCS,
            CodegenConstants.MODELS,
//            CodegenConstants.MODEL_DOCS,
            CodegenConstants.SUPPORTING_FILES
        );
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

//        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name)");
//        assertFileContains(apiPath + "BasicBookInfo.java", "public BasicBookInfo(String author, String name)", "super(name)");
//        assertFileContains(apiPath + "DetailedBookInfo.java", "public DetailedBookInfo(String isbn, String name, String author)", "super(author, name)");
    }

    @Test
    void testNewKotlinGenerator() {

        var codegen = new KotlinMicronautClientCodegen();
        codegen.useBeanValidation = true;
        codegen.reactive = false;
        String outputPath = generate(codegen, "src/test/resources/3_0/micronaut/roles-extension-test.yaml",
            CodegenConstants.APIS,
            CodegenConstants.API_TESTS,
//            CodegenConstants.API_DOCS,
            CodegenConstants.MODELS,
//            CodegenConstants.MODEL_DOCS,
            CodegenConstants.SUPPORTING_FILES
            );
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";
    }

    @Test
    void testKotlinGenerator() {

        var codegen = new KotlinClientCodegen();
        codegen.setSerializationLibrary("jackson");

        String outputPath = generate(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml",
            CodegenConstants.APIS,
//            CodegenConstants.API_TESTS,
//            CodegenConstants.API_DOCS,
            CodegenConstants.MODELS
//            CodegenConstants.MODEL_DOCS,
//            CodegenConstants.SUPPORTING_FILES
        );
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

//        assertFileContains(apiPath + "DefaultApi.java", "import java.io.InputStream;");
    }

    public String generate(AbstractKotlinCodegen codegen, String configPath, String... filesToGenerate) {

        File output = null;
        try {
            output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        } catch (IOException e) {
            fail("Unable to create temporary directory for output");
        }
//        output.deleteOnExit();

        var openAPI = new OpenAPIParser()
            .readLocation(new File(configPath).toURI().toString(), null, new ParseOptions()).getOpenAPI();

        var codeGenerator = codegen;

        // Configure codegen
        withPath(output, codeGenerator::setOutputDir);

        // Disable timestamps are it makes builds non preproducible
        codeGenerator.setHideGenerationTimestamp(true);

//        configureOptions();

        // Create input
        ClientOptInput input = new ClientOptInput();
        input.openAPI(openAPI);
        input.config(codeGenerator);

        var outputs = Arrays.stream(filesToGenerate)
            .map(MicronautCodeGeneratorEntryPoint.OutputKind::of)
            .toList()
            .toArray(new MicronautCodeGeneratorEntryPoint.OutputKind[0]);

        // Generate
        DefaultGenerator generator = new DefaultGenerator();
        for (MicronautCodeGeneratorEntryPoint.OutputKind outputKind : MicronautCodeGeneratorEntryPoint.OutputKind.values()) {
            generator.setGeneratorPropertyDefault(outputKind.getGeneratorProperty(), "false");
        }
        for (MicronautCodeGeneratorEntryPoint.OutputKind outputKind : outputs) {
            generator.setGeneratorPropertyDefault(outputKind.getGeneratorProperty(), "true");
        }

        generator.opts(input).generate();

        // Create parser
        String outputPath = output.getAbsolutePath().replace('\\', '/');

        return outputPath + "/";
    }

    private static void withPath(File file, Consumer<? super String> action) {
        if (file == null) {
            return;
        }
        try {
            String path = file.getCanonicalPath();
            action.accept(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
