package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import spock.lang.IgnoreIf
import spock.lang.Issue

@IgnoreIf({ !jvm.isJava16Compatible() })
class OpenApiRecordsSpec extends AbstractOpenApiTypeElementSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/594")
    void "test build OpenAPI returning a record from a controller"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.*;

@Controller
class PersonController {

    @Get("/person")
    Person getPerson() {
        return new Person("John", 42);
    }
}

record Person(String name, Integer age) {}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['Person'].name == 'Person'
        openAPI.components.schemas['Person'].type == 'object'
        openAPI.components.schemas['Person'].properties['name'].type == 'string'
        openAPI.components.schemas['Person'].properties['age'].type == 'integer'
        openAPI.components.schemas['Person'].required
        openAPI.components.schemas['Person'].required.size() == 2
        openAPI.components.schemas['Person'].required.contains('age')
        openAPI.components.schemas['Person'].required.contains('name')
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/594")
    void "test build OpenAPI returning a record with nullable field from a controller"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;

@Controller
class PersonController {

    @Get("/person")
    Person getPerson() {
        return new Person("John", 42);
    }
}

record Person(@Nullable String name, Integer age) {}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['Person'].name == 'Person'
        openAPI.components.schemas['Person'].type == 'object'
        openAPI.components.schemas['Person'].properties['name'].type == 'string'
        openAPI.components.schemas['Person'].properties['age'].type == 'integer'
        openAPI.components.schemas['Person'].required
        openAPI.components.schemas['Person'].required.size() == 1
        openAPI.components.schemas['Person'].required.contains('age')
    }
}
