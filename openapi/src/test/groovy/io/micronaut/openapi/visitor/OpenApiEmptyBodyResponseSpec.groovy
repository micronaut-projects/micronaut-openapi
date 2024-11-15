package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiEmptyBodyResponseSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI empty body response"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Status;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Singleton;

@Controller("/path")
class OpenApiController {

    @Operation(summary = "HTTP response does not contain a body.")
    @Status(HttpStatus.NO_CONTENT)
    @Get(uri = "/200")
    public HttpResponse<Void> processSync() {
        return HttpResponse.ok(null);
    }
}

@Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation

        operation.summary == "HTTP response does not contain a body."
        operation.responses
        operation.responses.size() == 1
        operation.responses."204".description == "processSync 204 response"
        !operation.responses."204".content
    }

    void "test build OpenAPI empty body response with void method"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Status;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Singleton;

@Controller("/path")
class OpenApiController {

    @Operation(summary = "HTTP response does not contain a body.")
    @Status(HttpStatus.NO_CONTENT)
    @Get(uri = "/200")
    public void processSync() {
    }
}

@Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation

        operation.summary == "HTTP response does not contain a body."
        operation.responses
        operation.responses.size() == 1
        operation.responses."204".description == "processSync 204 response"
        !operation.responses."204".content
    }
}
