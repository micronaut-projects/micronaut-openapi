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

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
        operation.parameters[0].schema.type ==  'string'
        operation.parameters[0].schema.enum
        operation.parameters[0].schema.enum == ["dog", "cat", "snake"]
        operation.parameters[0].schema.default == 'dog'
    }
}