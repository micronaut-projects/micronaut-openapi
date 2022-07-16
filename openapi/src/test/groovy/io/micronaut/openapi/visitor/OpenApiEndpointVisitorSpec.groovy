package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme

class OpenApiEndpointVisitorSpec extends AbstractOpenApiTypeElementSpec {

    void 'test build OpenAPI with custom url for endpoints'() {
        given: 'An API definition'
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-custom-endpoints.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0"
        )
)
class Application {
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then: 'the state is correct'
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then: 'it is included in the OpenAPI doc'
        openAPI.info != null

        then: 'built-in end point are prefixed with /internal'
        openAPI.paths['/internal/beans']
        openAPI.paths['/internal/beans'].get
        openAPI.paths['/internal/health']
        openAPI.paths['/internal/health'].get
        openAPI.paths['/internal/loggers']
        openAPI.paths['/internal/loggers'].get
        openAPI.paths['/internal/refresh']
        openAPI.paths['/internal/refresh'].post
        openAPI.paths['/internal/routes']
        openAPI.paths['/internal/routes'].get

        cleanup:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "")
    }

}
