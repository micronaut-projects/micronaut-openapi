package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiHttpHeadersSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI doc for controller with HttpHeaders param (GET)"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/path")
class OpenApiController {

    @Get
    public HttpResponse<String> processSync(HttpHeaders headers) {
        return HttpResponse.ok("");
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths."/path".get

        then:
        operation
        operation.operationId == "processSync"
        !operation.parameters
        !operation.requestBody
    }

    void "test build OpenAPI doc for controller with HttpHeaders param (POST)"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller("/path")
class OpenApiController {

    @Post
    public HttpResponse<String> processSync(HttpHeaders headers) {
        return HttpResponse.ok("");
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths."/path".post

        then:
        operation
        operation.operationId == "processSync"
        !operation.parameters
        !operation.requestBody
    }
}
