package io.micronaut.openapi.visitor

import io.micronaut.ast.transform.test.AbstractBeanDefinitionSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiPojoControllerGroovySpec extends AbstractBeanDefinitionSpec {

    def setup() {
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.clearProperty(Utils.ATTR_TEST_MODE)
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/561")
    void "test build OpenAPI for Controller with POJO with mandatory/optional fields works for Groovy"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test

import io.reactivex.*
import io.micronaut.core.annotation.*
import io.micronaut.http.annotation.*
import io.swagger.v3.oas.annotations.media.*

@Controller("/example")
class ExampleController {

    @Get("/")
    ExampleData getExampleData() {
        new ExampleData("name", true, new ExampleAdditionalData("hello"), 2, 0.456f, 1.2F)
    }
}

class ExampleData {
    String name
    @Schema(required = false)
    Boolean active
    ExampleAdditionalData additionalData
    Integer age
    Float battingAverage
    float anotherFloat

    ExampleData(String name, Boolean active, ExampleAdditionalData additionalData, Integer age, @Nullable Float battingAverage, float anotherFloat) {
        this.name = name
        this.active = active
        this.additionalData = additionalData
        this.age = age
        this.battingAverage = battingAverage
        this.anotherFloat = anotherFloat
    }
}

class ExampleAdditionalData {
    String something
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['ExampleData']

        then: "the components are valid"
        schema.properties.size() == 6
        schema.type == 'object'
        schema.required

        and: 'all params without annotations are required'
        schema.required.contains('name')
        schema.required.contains('additionalData')
        schema.required.contains('age')
        schema.required.contains('anotherFloat')

        and: 'active is not required because it is annotated with @Schema(required = false)'
        !schema.required.contains('active')

        and: 'battingAverage is not required because it is annotated with @Nullable in the constructor'
        !schema.required.contains('battingAverage')
    }

}
