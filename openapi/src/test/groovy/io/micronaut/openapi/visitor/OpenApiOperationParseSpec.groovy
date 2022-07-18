package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema

class OpenApiOperationParseSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI @ApiResponse Content with @Schema annotation with custom fieldname"() {
        given:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Schema(description = "Represents a pet")
class Pet {
    @Schema(name="pet-name", description = "The pet name")
    private String name;
    private Integer age;

    @Schema(name="pet-name", description = "The pet name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(name="pet-age", description = "The pet age")
    public Integer getAge() {
        return age;
    }
}

@Controller("/pet")
interface PetOperations {
    @Operation(summary = "Save Pet",
            description = "Save Pet",
            tags = "save-pet",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Save Pet",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Pet.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Page Not Found"
                    )})
    @Post
    HttpResponse<Pet> save(@Body Pet pet);
}

@jakarta.inject.Singleton
class MyBean {}
''')

        Schema petSchema = (Schema) Utils.testReference.getComponents().getSchemas().get("Pet")

        expect:
        petSchema
        petSchema.type == 'object'
        petSchema.properties
        petSchema.properties.size() == 2
        petSchema.properties.containsKey("pet-name")
        petSchema.properties.containsKey("pet-age")

    }

    void "test parse the OpenAPI @ApiResponse Content with @ArraySchema annotation"() {
        given:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;

@Controller("/pet")
interface PetOperations {

    @Operation(summary = "List Pets",
            description = "List Pets",
            tags = "pets",
            responses = {
                    @ApiResponse(description = "List Pets", responseCode = "200", content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Pet.class)))),
                    @ApiResponse(description = "Pets not found", responseCode = "404")
            })
    @Get
    HttpResponse<List<Pet>> list();

    @Operation(summary = "List Pet Names",
            description = "List Pet Names",
            tags = "pet-name",
            responses = {
                    @ApiResponse(description = "List Pet Names", responseCode = "200", content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(type="string")))),
                    @ApiResponse(description = "Not found", responseCode = "404")
            })
    @Get("/names")
    HttpResponse<List<String>> listNames();
}

@Schema(description = "Represents a pet")
class Pet {
    @Schema(description = "The pet name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        Operation operation = Utils.testReference?.paths?.get("/pet")?.get

        expect:
        operation
        operation.summary == 'List Pets'
        operation.tags.size() == 1
        operation.tags == ['pets']
        operation.responses.size() == 2
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json']
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'200'.content['application/json'].schema.type == "array"
        operation.responses.'200'.content['application/json'].schema.items.$ref == "#/components/schemas/Pet"

        when:
        Operation operationNames = Utils.testReference?.paths?.get("/pet/names")?.get

        then:
        operationNames
        operationNames.summary == "List Pet Names"
        operationNames.tags == ['pet-name']
        operationNames.responses.size() == 2
        operationNames.responses.'200'.content.size() == 1
        operationNames.responses.'200'.content['application/json']
        operationNames.responses.'200'.content['application/json'].schema
        operationNames.responses.'200'.content['application/json'].schema.type == "array"
        operationNames.responses.'200'.content['application/json'].schema.items.type == "string"
    }

    void "test parse the OpenAPI @ApiResponse Content with @Schema annotation"() {
        given:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Schema(description = "Represents a pet")
class Pet {
    @Schema(description = "The pet name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

@Controller("/pet")
interface PetOperations {
    @Operation(summary = "Save Pet",
            description = "Save Pet",
            tags = "save-pet",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Save Pet",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Pet.class),
                                    examples = {
                    @ExampleObject(name = "example-1", value = "{\\"name\\":\\"Charlie\\"}")
                })),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Page Not Found"
                    )})
    @Post
    HttpResponse<Pet> save(@Body Pet pet);
}

@jakarta.inject.Singleton
class MyBean {}
''')

        Operation operation = Utils.testReference?.paths?.get("/pet")?.post

        expect:
        operation
        operation.summary == 'Save Pet'
        operation.tags.size() == 1
        operation.tags == ['save-pet']
        operation.responses.size() == 2
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json']
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'200'.content['application/json'].schema.$ref == "#/components/schemas/Pet"
        operation.responses.'200'.content['application/json'].examples
        operation.responses.'200'.content['application/json'].examples.'example-1'
        operation.responses.'200'.content['application/json'].examples.'example-1'.value
        operation.responses.'200'.content['application/json'].examples.'example-1'.value.name == 'Charlie'
    }

    void "test parse the OpenAPI @Operation annotation"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

  @Put
  @Consumes("application/json")
  @Operation(summary = "Update an existing pet",
          tags = {"pets"},
          security = @SecurityRequirement(
                                  name = "petstore-auth",
                                  scopes = "write:pets"),
          responses = {
                  @ApiResponse(
                     content = @Content(mediaType = "application/json",
                             schema = @Schema(implementation = Pet.class))),
                  @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
                  @ApiResponse(responseCode = "404", description = "Pet not found"),
                  @ApiResponse(responseCode = "405", description = "Validation exception") }
    )
    public Response updatePet(
      @RequestBody(description = "Pet object that needs to be added to the store", required = true,
                              content = @Content(
                                      schema = @Schema(implementation = Pet.class))) Pet pet) {
        return null;
    }
}

class Pet {}

class Response {}

@jakarta.inject.Singleton
class MyBean {}
''')

        Operation operation = Utils.testReference?.paths?.get("/")?.put

        expect:
        operation
        operation.summary == 'Update an existing pet'
        operation.tags.size() == 1
        operation.tags == ['pets']
        operation.security.size() == 1
        operation.security.get(0).containsKey('petstore-auth')
        operation.security.get(0).get('petstore-auth') == ['write:pets']
        operation.responses.size() == 4
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json']
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'400'.description == 'Invalid ID supplied'

    }

    void "test parse the OpenAPI @Operation annotation with tags and security defined as annotations"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

  @Put
  @Consumes("application/json")
  @Operation(summary = "Update an existing pet",
          responses = {
                  @ApiResponse(
                     content = @Content(mediaType = "application/json",
                             schema = @Schema(implementation = Pet.class))),
                  @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
                  @ApiResponse(responseCode = "404", description = "Pet not found"),
                  @ApiResponse(responseCode = "405", description = "Validation exception") }
    )
    @Tag(name = "pets")
    @SecurityRequirement(
          name = "petstore-auth",
          scopes = "write:pets")
    public Response updatePet(
      @RequestBody(description = "Pet object that needs to be added to the store", required = true,
                              content = @Content(
                                      schema = @Schema(implementation = Pet.class))) Pet pet) {
        return null;
    }
}

class Pet {}

class Response {}

@jakarta.inject.Singleton
class MyBean {}
''')

        Operation operation = Utils.testReference?.paths?.get("/")?.put

        expect:
        operation
        operation.summary == 'Update an existing pet'
        operation.tags.size() == 1
        operation.tags == ['pets']
        operation.security.size() == 1
        operation.security.get(0).containsKey('petstore-auth')
        operation.security.get(0).get('petstore-auth') == ['write:pets']
        operation.responses.size() == 4
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json']
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'400'.description == 'Invalid ID supplied'

    }

    void "test parse the OpenAPI @Operation annotation with @ApiResponse on method"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.extensions.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;

@Controller("/")
class MyController {

    @Put
    @Consumes("application/json")
    @Operation(summary = "Update an existing pet",
            tags = {"pets"},
            security = @SecurityRequirement(name = "petstore-auth", scopes = "write:pets")
    )
    @ApiResponse(
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class)),
            extensions = @Extension(
                    name = "custom",
                    properties = {
                            @ExtensionProperty(name = "prop1", value = "prop1Val"),
                            @ExtensionProperty(name = "prop1", value = "prop1Val")
                    }
            ))
    @ApiResponse(responseCode = "400", description = "Invalid ID supplied")
    @ApiResponse(responseCode = "404", description = "Pet not found")
    @ApiResponse(responseCode = "405", description = "Validation exception")
    public Response updatePet(
            @RequestBody(description = "Pet object that needs to be added to the store", required = true,
                    content = @Content( schema = @Schema(implementation = Pet.class))) Pet pet) {
        return null;
    }
}

class Pet {}

class Response {}

@jakarta.inject.Singleton
class MyBean {}
''')

        Operation operation = Utils.testReference?.paths?.get("/")?.put

        expect:
        operation
        operation.summary == 'Update an existing pet'
        operation.tags.size() == 1
        operation.tags == ['pets']
        operation.security.size() == 1
        operation.security.get(0).containsKey('petstore-auth')
        operation.security.get(0).get('petstore-auth') == ['write:pets']
        operation.responses.size() == 4
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json']
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'200'.extensions.'x-custom'.prop1 == "prop1Val"
        operation.responses.'400'.description == 'Invalid ID supplied'

    }
}
