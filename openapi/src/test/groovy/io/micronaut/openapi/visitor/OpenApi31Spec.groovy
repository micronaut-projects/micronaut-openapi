package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.OpenApiUtils
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenApi31Spec extends AbstractOpenApiTypeElementSpec {

    void "test info OpenAPI 3.1.0"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_JSON_SCHEMA_DIALECT, "https://json-schema-3-1.org")
        Utils.clean()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.management.endpoint.annotation.Delete;
import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Selector;
import io.micronaut.management.endpoint.annotation.Write;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                summary = "the summary",
                license = @License(
                        name = "Apache 2.0",
                        url = "https://foo.bar",
                        identifier = "licenseId"
                )
        )
)
class Application {

}

@Controller("/hello")
class HelloWorldApi {

    @Get
    public HttpResponse<String> helloWorld() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.openapi == OpenApiUtils.OPENAPI_31_VERSION
        openApi.jsonSchemaDialect == "https://json-schema-3-1.org"
        openApi.info
        openApi.info.title == 'the title'
        openApi.info.summary == 'the summary'
        openApi.info.version == '0.0'
        openApi.info.description == 'My API'

        openApi.info.license
        openApi.info.license.name == 'Apache 2.0'
        openApi.info.license.url == 'https://foo.bar'
        openApi.info.license.identifier == 'licenseId'

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED)
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_JSON_SCHEMA_DIALECT)
        Utils.clean()
    }

    void "test Webhooks OpenAPI 3.1.0"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        Utils.clean()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Webhook;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/hello")
class HelloWorldApi {

    @Webhook(
        name = "controllerWebhook",
        operation = @Operation(summary = "Save a pet",
            description = "The saved pet information is returned",
            method = "post",
            requestBody = @RequestBody(description = "description"),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "The saved pet information")
            }
        )
    )
    @Post("/{var1}")
    public HttpResponse<String> webhookMethod(String var1) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.webhooks
        openApi.webhooks.size() == 1
        openApi.webhooks.'controllerWebhook'
        openApi.webhooks.'controllerWebhook'.post
        openApi.webhooks.'controllerWebhook'.post.summary == 'Save a pet'
        openApi.webhooks.'controllerWebhook'.post.description == "The saved pet information is returned"
        openApi.webhooks.'controllerWebhook'.post.requestBody.description == "description"

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED)
        Utils.clean()
    }

    void "test min/max contains OpenAPI 3.1.0"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        Utils.clean()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Controller("/hello")
class HelloWorldApi {

    @Post
    public HttpResponse<String> post(@Body Pet body) {
        return null;
    }
}

class Pet {

    @ArraySchema(
            contains = @Schema(
                    types = {"string", "null"},
                    format = "int64"
            ),
            minContains = 10,
            maxContains = 20,
            schema = @Schema(
                    type = "string"
            )
    )
    public List<Object> attrs;

}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.components.schemas
        openApi.components.schemas.Pet
        openApi.components.schemas.Pet.properties.attrs.contains
        openApi.components.schemas.Pet.properties.attrs.contains.extensions.types.contains("string")
        openApi.components.schemas.Pet.properties.attrs.contains.extensions.types.contains("null")
        openApi.components.schemas.Pet.properties.attrs.contains.format == "int64"
        openApi.components.schemas.Pet.properties.attrs.items.types.size() == 1
        openApi.components.schemas.Pet.properties.attrs.items.types.contains('string')
        openApi.components.schemas.Pet.properties.attrs.minContains == 10
        openApi.components.schemas.Pet.properties.attrs.maxContains == 20

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED)
        Utils.clean()
    }

    void "test discriminator extensions OpenAPI 3.1.0"() {
        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        Utils.clean()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.HashMap;
import java.util.Map;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

interface PetOperations {

    /**
     * @param name The Pet Name
     * @param age The Pet age
     * @return The Pet
     */
    @Post
    Single<Pet> save(@NotBlank String name, @Min(1L) int age);

    /**
     *
     * @param name The pet name
     * @return The Pet
     */
    @Get("/{name}")
    Single<Pet> get(@NotBlank String name);

}

class Dog extends Pet {

    private String breed;

    public void setBreed(String breed) {
        this.breed = breed;
    }

    /**
     * @return The Dog breed
     */
    public String getBreed() {
        return breed;
    }
}

class Cat extends Pet {

    private int clawSize;

    public void setClawSize(int clawSize) {
        this.clawSize = clawSize;
    }

    /**
     * @return The Cat claw size
     */
    public int getClawSize() {
        return clawSize;
    }
}

/**
 * Represents the Pet type.
 */
enum PetType {
    DOG,
    CAT
}

@Schema(discriminatorProperty = "type",
        discriminatorMapping = {
            @DiscriminatorMapping(
                    value = "DOG",
                    schema = Dog.class,
                    extensions = {
                        @Extension(
                                name = "myExt1",
                                properties = {
                                        @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                        @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                }
                        ),
                        @Extension(
                                name = "myExt2",
                                properties = {
                                        @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                        @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                }
                        ),
                    }
            ),
            @DiscriminatorMapping(
                    value = "CAT",
                    schema = Cat.class,
                    extensions = {
                        @Extension(
                                name = "myExt21",
                                properties = {
                                        @ExtensionProperty(name = "prop21", value = "prop1Val"),
                                        @ExtensionProperty(name = "prop22", value = "prop2Val"),
                                }
                        ),
                        @Extension(
                                name = "myExt22",
                                properties = {
                                        @ExtensionProperty(name = "prop221", value = "prop1Val1"),
                                        @ExtensionProperty(name = "prop222", value = "prop2Val2"),
                                }
                        ),
                    }
            )
        },
        oneOf = {Dog.class, Cat.class})
class Pet {

    private PetType type;
    private int age;
    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    @Schema(description="Pet age", maximum="20")
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    @Schema(description="Pet name", maxLength=20)
    public String getName() {
        return name;
    }

    public void setType(PetType t) {
        type = t;
    }

    public PetType getType() {
        return type;
    }
}

@Controller("/pet/cat")
class CatController implements PetOperations {

    static Map<String, Pet> pets = new HashMap<>(10);

    @Post
    @Override
    public Single<Pet> save(@NotBlank String name, @Min(1L) int age) {
        Pet pet = new Pet();
        pet.setType(PetType.CAT);
        pet.setName(name);
        pet.setAge(age);
        pets.put(name, pet);
        return Single.just(pet);
    }

    @Get("/{name}")
    @Override
    public Single<Pet> get(@NotBlank String name) {
        return Single.just(name)
                .map( petName -> pets.get(petName));
    }

    @Get("/claw/{size}")
    public Single<Pet> findByClawSize(Integer size) {
        return Single.just(pets.entrySet()
                .stream()
                .findFirst()
                .map(Map.Entry::getValue)
                .get());
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']
        Schema catSchema = openAPI.components.schemas['Cat']
        Schema dogSchema = openAPI.components.schemas['Dog']

        then: "the components are valid"
        petSchema != null
        dogSchema != null
        catSchema != null
        petSchema.discriminator.propertyName == "type"
        petSchema.discriminator.extensions.size() == 4
        petSchema.discriminator.extensions.'x-myExt1'
        petSchema.discriminator.extensions.'x-myExt1'.prop1 == 'prop1Val'
        petSchema.discriminator.extensions.'x-myExt1'.prop2 == 'prop2Val'
        petSchema.discriminator.extensions.'x-myExt2'
        petSchema.discriminator.extensions.'x-myExt2'.prop1 == 'prop1Val1'
        petSchema.discriminator.extensions.'x-myExt2'.prop2 == 'prop2Val2'
        petSchema.discriminator.extensions.'x-myExt21'
        petSchema.discriminator.extensions.'x-myExt21'.prop21 == 'prop1Val'
        petSchema.discriminator.extensions.'x-myExt21'.prop22 == 'prop2Val'
        petSchema.discriminator.extensions.'x-myExt22'
        petSchema.discriminator.extensions.'x-myExt22'.prop221 == 'prop1Val1'
        petSchema.discriminator.extensions.'x-myExt22'.prop222 == 'prop2Val2'

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED)
        Utils.clean()
    }
}
