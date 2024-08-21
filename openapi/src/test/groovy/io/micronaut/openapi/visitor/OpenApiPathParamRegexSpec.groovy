package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

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
    @Post("/tags/{tagId: \\\\d+}/{path:.*}{.ext}/update{?max,offset}{/id:[a-zA-Z]+}")
    public void postRaw() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths
        openAPI.paths."/path/tags/{tagId}/{path}/update/{id}"
    }

}
