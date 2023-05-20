package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
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
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1
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
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['Person'].type == 'object'
        openAPI.components.schemas['Person'].properties['name'].type == 'string'
        openAPI.components.schemas['Person'].properties['age'].type == 'integer'
        openAPI.components.schemas['Person'].required
        openAPI.components.schemas['Person'].required.size() == 1
        openAPI.components.schemas['Person'].required.contains('age')
    }

    void "test read javadoc from class level for records"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class PersonController {

    @Get("/person")
    Person getPerson() {
        return new Person("John", 42);
    }
}

/**
 * The Person class description
 *
 * @param name this is persons name
 * @param age this is persons age
 *
 * @return new instance of Person class
*/
record Person(@Nullable String name, Integer age) {}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1

        Schema personSchema = openAPI.components.schemas['Person']
        personSchema.type == 'object'
        personSchema.description == 'The Person class description'
        personSchema.properties.name.type == 'string'
        personSchema.properties.name.description == 'this is persons name'
        personSchema.properties.age.type == 'integer'
        personSchema.properties.age.description == 'this is persons age'
        personSchema.required
        personSchema.required.size() == 1
        personSchema.required.contains('age')
    }

    void "test read javadoc from class level for records - 2"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;

@Controller
class PetController {

    @Get("/pet")
    Pet getPet() {
        return new Pet("John");
    }
}

/**
 *
 * @param name The name of the pet
 * @author gkrocher
 */
@Serdeable
record Pet(
        @NotBlank
    @Size(max = 200)
    String name) {}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1

        Schema petchema = openAPI.components.schemas['Pet']
        petchema.type == 'object'
        petchema.description == null
        petchema.properties.name.type == 'string'
        petchema.properties.name.description == 'The name of the pet'
        petchema.required
        petchema.required.size() == 1
        petchema.required.contains('name')
    }
}
