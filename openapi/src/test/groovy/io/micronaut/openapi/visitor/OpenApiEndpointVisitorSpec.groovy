package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import spock.util.environment.RestoreSystemProperties

class OpenApiEndpointVisitorSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void 'test build OpenAPI with custom url for endpoints'() {
        given: 'An API definition'
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-custom-endpoints.properties")

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
    }

    @RestoreSystemProperties
    void 'test build OpenAPI with disabled endpoints and custom paths'() {
        given: 'An API definition'
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-custom-endpoints.properties")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "endpoints")

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
        // beans endpoint with custom path `/test`
        openAPI.paths['/internal/test']
        openAPI.paths['/internal/test'].get
        openAPI.paths['/internal/test'].get.summary == "This is test description"
        openAPI.paths['/internal/test'].get.description == "This is test description"
        openAPI.paths['/internal/test'].get.extensions.'x-ext1'
        ((Map<String, Object>) openAPI.paths['/internal/test'].get.extensions.'x-ext1').prop1 == true
        ((Map<String, Object>) openAPI.paths['/internal/test'].get.extensions.'x-ext1').prop2 == 123
        ((Map<String, Object>) openAPI.paths['/internal/test'].get.extensions.'x-ext1').prop3 == "value"
        openAPI.paths['/internal/loggers']
        openAPI.paths['/internal/loggers'].get
        openAPI.paths['/internal/refresh']
        openAPI.paths['/internal/refresh'].post
        !openAPI.paths['/internal/health']
        !openAPI.paths['/internal/routes']
    }

    @RestoreSystemProperties
    void 'test build OpenAPI endpoints with groups'() {
        given: 'An API definition'
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-endpoints-with-groups.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIGroup("v1")
@Controller
class MyControllerV1 {
    
    @Get("/v1/path1")
    String path1() {
        return null;
    }
}

@OpenAPIGroup("v2")
@Controller
class MyControllerV2 {
    
    @Get("/v2/path1")
    String path1() {
        return null;
    }
}

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
        Utils.testReferences != null

        when:
        var openApis = Utils.testReferences

        then:
        openApis
        openApis.size() == 3

        var v1OpenApi = openApis.get(Pair.of("v1", null)).openApi
        var v2OpenApi = openApis.get(Pair.of("v2", null)).openApi
        var managementOpenApi = openApis.get(Pair.of("management", null)).openApi

        v1OpenApi.paths."/beans"
        v1OpenApi.paths."/v1/path1"
        !v1OpenApi.paths."/refresh"

        !v2OpenApi.paths."/beans"
        v2OpenApi.paths."/v2/path1"

        managementOpenApi.paths."/beans"
        managementOpenApi.paths."/env"
        !managementOpenApi.paths."/refresh"
    }
}
