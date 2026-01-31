package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import spock.util.environment.RestoreSystemProperties

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_OPERATION_DUPLICATE_RESOLUTION

class OpenApiOperationIdSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test duplicate operation ID resolution ERROR"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_OPERATION_DUPLICATE_RESOLUTION, ConfigUtils.DuplicateResolution.ERROR.name())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/resourceA")
class ControllerA {

    @Get
    String getResource() {
        return "test1";
    }
}

@Controller("/resourceB")
class ControllerB {

    @Get
    String getResource() {
        return "test2";
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Found 2 operations with same ID \"getResource\" for paths GET /resourceA and GET /resourceB")
    }
}

