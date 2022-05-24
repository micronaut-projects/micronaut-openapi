package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiPathParamRegexSpec extends AbstractOpenApiTypeElementSpec {


    void "test build OpenAPI path parameters with regex"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;

@Controller("/path")
class OpenApiController {

    @Operation(summary = "Update tag", description = "Updates an existing tag", tags = "users_tag")
    @Post("/tags/{tagId: \\\\d+}/{path:.*}{.ext}/update{/id:[a-zA-Z]+}/{+path}{?max,offset}")
    public void postRaw() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.paths
        openAPI.paths."/path/tags/{tagId}/{path}/update/{id}/{path}"
    }

}
