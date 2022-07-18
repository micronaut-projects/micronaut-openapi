package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiParameterSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test parameter with schema"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Period;
import java.util.Optional;

@Controller("/path")
class OpenApiController {

    @Get
    public HttpResponse<String> processSync(
            @Parameter(schema = @Schema(implementation = String.class)) Optional<Period> period) {
        return HttpResponse.ok();
    }

    @Post
    public HttpResponse<String> processSync2(
            @Parameter(ref = "#/components/parameters/MyParam") Optional<Period> period) {
        return HttpResponse.ok();
    }

    @Put
    public HttpResponse<String> processSync3(
            @Parameter(schema = @Schema(ref = "#/components/schemas/MyParamSchema")) Optional<Period> period) {
        return HttpResponse.ok();
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
        Operation operationPost = openAPI.paths."/path".post
        Operation operationPut = openAPI.paths."/path".put
        Operation operationPatch = openAPI.paths."/path".patch

        then:
        operation
        operation.operationId == "processSync"
        operation.parameters
        operation.parameters[0].name == "period"
        operation.parameters[0].in == "query"
        operation.parameters[0].schema
        operation.parameters[0].schema.type == "string"

        operationPost.parameters[0].get$ref() == "#/components/parameters/MyParam"

        operationPut.parameters[0].schema.oneOf.get(0).get$ref() == "#/components/schemas/MyParamSchema"
    }

    void "test parameter with schema with attributes"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Period;
import java.util.Optional;

@Controller("/path")
class OpenApiController {

    @Patch
    public HttpResponse<String> processSync4(@Parameter(schema = @Schema(type = "string", format = "uuid")) String param) {
        return HttpResponse.ok();
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operationPatch = openAPI.paths."/path".patch

        then:
        operationPatch.parameters[0].name == 'param'
        operationPatch.parameters[0].schema.type == 'string'
        operationPatch.parameters[0].schema.format == 'uuid'
    }
}
