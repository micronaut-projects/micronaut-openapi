/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiOperationHeadersSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
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

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        operation.responses.default.headers.size() == 1
        operation.responses.default.headers['X-Rate-Limit-Limit'].description == 'The number of allowed requests in the current period'
        operation.responses.default.headers['X-Rate-Limit-Limit'].schema
        operation.responses.default.headers['X-Rate-Limit-Limit'].schema.type == 'integer'
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


@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
}
