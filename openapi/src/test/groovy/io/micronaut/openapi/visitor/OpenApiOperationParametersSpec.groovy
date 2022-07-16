package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiOperationParametersSpec extends AbstractOpenApiTypeElementSpec {

    void "test Parameters in Operation"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Get("/")
    @Operation(description = "Lists the Pets.",
               parameters = {
                   @Parameter(name = "petType", in = ParameterIn.HEADER, required = true, description = "A pet type", example = "['dog', 'cat']", schema = @Schema(description = "A  _Pet_'s type", type = "string",  allowableValues = {"dog", "cat", "snake"}, defaultValue = "dog"))
               }
    )
    @ApiResponse(responseCode = "200", description = "Returns a _Pet_.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Pet.class)))
    public Pet findPets(String petType) {
        return null;
    }
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet")
    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get

        expect:
        operation
        operation.responses.size() == 1
        operation.responses.'200'.description == 'Returns a _Pet_.'
        operation.responses.'200'.content.'application/json'.schema.$ref == '#/components/schemas/Pet'

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'petType'
        operation.parameters[0].description == 'A pet type'
        operation.parameters[0].schema.description == 'A  _Pet_\'s type'
        operation.parameters[0].schema.type == 'string'
        operation.parameters[0].schema.enum
        operation.parameters[0].schema.enum == ["dog", "cat", "snake"]
        operation.parameters[0].schema.default == 'dog'
    }

    void "test ApiResponses"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Get("/")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Returns a _Pet_.",
            useReturnTypeSchema = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Pet.class))),
        @ApiResponse(
            responseCode = "201",
            description = "my desc",
            ref = "#/components/responses/MyResponse",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Pet.class))),
        @ApiResponse(
            responseCode = "202",
            description = "my desc2",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(ref = "#/components/schemas/myCustomSchema"))),
    })
    public Pet findPets(String petType) {
        return null;
    }

    @Post("/")
    @ApiResponse(
        responseCode = "200",
        description = "Returns a _Pet_.",
        useReturnTypeSchema = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Pet.class)))
    @ApiResponse(
        responseCode = "201",
        description = "my desc",
        ref = "#/components/responses/MyResponse",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Pet.class)))
    @ApiResponse(
        responseCode = "202",
        description = "my desc2",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(ref = "#/components/schemas/myCustomSchema")))
    public Pet findPets2(String petType) {
        return null;
    }
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet")
    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get
        Operation operationPost = openAPI.paths?.get("/")?.post

        expect:
        operation
        operation.responses.size() == 3
        operation.responses.'200'.description == 'Returns a _Pet_.'
        operation.responses.'200'.content.'application/json'.schema.$ref == '#/components/schemas/Pet'

        operation.responses.'201'.description == 'my desc'
        operation.responses.'201'.get$ref() == "#/components/responses/MyResponse"

        operation.responses.'202'.description == 'my desc2'
        operation.responses.'202'.content."application/json".schema.$ref == "#/components/schemas/myCustomSchema"

        operationPost
        operationPost.responses.size() == 3
        operationPost.responses.'200'.description == 'Returns a _Pet_.'
        operationPost.responses.'200'.content.'application/json'.schema.$ref == '#/components/schemas/Pet'

        operationPost.responses.'201'.description == 'my desc'
        operationPost.responses.'201'.get$ref() == "#/components/responses/MyResponse"

        operationPost.responses.'202'.description == 'my desc2'
        operationPost.responses.'202'.content."application/json".schema.$ref == "#/components/schemas/myCustomSchema"
    }
}
