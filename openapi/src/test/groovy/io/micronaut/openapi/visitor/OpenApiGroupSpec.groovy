package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiGroupSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI with groups"() {

        setup:
//        System.setProperty("micronaut.router.versioning.enabled", "true")
//        System.setProperty("micronaut.router.versioning.parameter.enabled", "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import jakarta.inject.Singleton;

@Controller
class GroupController {

    // common endpoint
    @Get("/common")
    String common() {
        return null;
    }

    // only group-2 endpoint
    @OpenAPIGroup(exclude = "group-1")
    @Get("/common2")
    String common2() {
        return null;
    }

    @OpenAPIGroup("group-1")
    @Get("/v1/hello")
    String helloV1() {
        return "helloV1";
    }

    @OpenAPIGroup("group-2")
    @Get("/v2/hello")
    String helloV2() {
        return "helloV2";
    }
}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths."/versioned/hello".get

        then:

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters.get(0).name == "api-version"
        operation.parameters.get(0).in == "query"
        operation.parameters.get(0).schema.type == "string"

//        cleanup:
//        System.clearProperty("micronaut.router.versioning.enabled")
//        System.clearProperty("micronaut.router.versioning.parameter.enabled")
    }

}
