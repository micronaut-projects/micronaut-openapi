package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiInnerClassSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI inner classes"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.Collections;
import java.util.List;

class ParentClass {

    @Controller("/path")
    public static class OpenApiController {
        @Get("/tags/{tagId}/update")
        public void postRaw() {
        }
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
        openAPI.paths.size() == 1
        openAPI.paths."/path/tags/{tagId}/update"
    }
}
