package io.micronaut.openapi.visitor

import io.micronaut.ast.transform.test.AbstractBeanDefinitionSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiIncludeVisitorGroovySpec extends AbstractBeanDefinitionSpec {

    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "")
    }

    void 'test build OpenAPI for management endpoints'() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test

@io.swagger.v3.oas.annotations.OpenAPIDefinition
@io.micronaut.openapi.annotation.OpenAPIManagement(tags = @io.swagger.v3.oas.annotations.tags.Tag(name = "Micronaut Management"))
class Application {
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.paths['/health']
        openAPI.paths['/health'].get.tags[0] == "Micronaut Management"
        openAPI.paths['/beans']
        openAPI.paths['/env']
        openAPI.paths['/info']
        openAPI.paths['/loggers']
        openAPI.paths['/refresh']
        openAPI.paths['/routes']
        openAPI.paths['/stop']
        openAPI.paths['/threaddump']
    }

}
