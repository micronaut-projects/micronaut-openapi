package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiOperationHeadersSpec extends AbstractOpenApiTypeElementSpec {

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
import io.swagger.v3.oas.annotations.links.*;
import io.swagger.v3.oas.annotations.headers.Header;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Put("/")
    @Consumes("application/json")
    @Operation(operationId = "getUser",
        responses = {
                @ApiResponse(description = "test description",
                        content = @Content(mediaType = "*/*", schema = @Schema(ref = "#/components/schemas/User")),
                        headers = {
                                @Header(
                                        name = "X-Rate-Limit-Limit",
                                        description = "The number of allowed requests in the current period",
                                        schema = @Schema(implementation = Integer.class))
                        })}
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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        operation.responses['200'].headers.size() == 1
        operation.responses['200'].headers['X-Rate-Limit-Limit'].description == 'The number of allowed requests in the current period'
        operation.responses['200'].headers['X-Rate-Limit-Limit'].schema
        operation.responses['200'].headers['X-Rate-Limit-Limit'].schema.type == 'integer'
    }

    void "test parse the micronaut @Header annotation and body"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/create2")
    public String create2(@Header("X-Session-Id") String sessionId, String phone, String name) {
        return name;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/create2")?.post
        RequestBody requestBody = operation.requestBody

        expect:
        operation
        operation.parameters.size() == 1
        operation.parameters[0].name == 'X-Session-Id'
        operation.parameters[0].in == ParameterIn.HEADER.toString()

        requestBody
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content["application/json"].schema
        requestBody.content["application/json"].schema.properties
        requestBody.content["application/json"].schema.properties.size() == 2
        requestBody.content["application/json"].schema.properties["phone"]
        requestBody.content["application/json"].schema.properties["name"]
    }

    void "test hidden response header"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/")
class MyController {

    @ApiResponse(headers = {
            @Header(name = "myHeader", example = """
                {"name": "this is example", "value": 10}
            """),
            @Header(name = "headerHidden", hidden = true)
    })
    @Post("/create2")
    public String create2(String phone, String name) {
        return name;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/create2")?.post

        expect:
        operation
        def myHeader = operation.responses.'200'.headers.myHeader
        myHeader
        myHeader.example
        myHeader.example.name == 'this is example'
        myHeader.example.value == 10
        !operation.responses.'200'.headers.headerHidden
    }
}
