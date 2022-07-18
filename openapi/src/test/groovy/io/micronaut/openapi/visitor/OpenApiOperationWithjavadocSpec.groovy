package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import spock.lang.Ignore

class OpenApiOperationWithjavadocSpec extends AbstractOpenApiTypeElementSpec {

    void "test javadoc description, summary and parameters"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;import io.swagger.v3.oas.annotations.enums.ParameterStyle;

@Controller
class MyController {

    /**
     * Description and summary here
     * @param id UUID of test
     */
    @Delete("/")
    public void deleteObj(@QueryValue int id) {

    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.delete

        expect:
        operation
        operation.summary == 'Description and summary here'
        operation.description == 'Description and summary here'

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'id'
        operation.parameters[0].description == 'UUID of test'
    }

    void "test Operation description, summary and parameters"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;import io.swagger.v3.oas.annotations.enums.ParameterStyle;

@Controller
class MyController {

    @Delete("/")
    @Operation(
        summary = "Delete a thing",
        description = "description test",
        parameters = @Parameter(name = "id", description = "id description", style = ParameterStyle.LABEL)
    )
    @Parameter(name = "param1", description = "my desc", style = ParameterStyle.DEEPOBJECT)
    public void deleteObj(@QueryValue int id, @QueryValue int param1) {

    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.delete

        expect:
        operation
        operation.summary == 'Delete a thing'
        operation.description == 'description test'

        operation.parameters
        operation.parameters.size() == 2
        operation.parameters[0].name == 'id'
        operation.parameters[0].description == 'id description'
        operation.parameters[0].style == Parameter.StyleEnum.LABEL
        operation.parameters[1].name == 'param1'
        operation.parameters[1].description == 'my desc'
        operation.parameters[1].style == Parameter.StyleEnum.DEEPOBJECT
    }

    void "test Operation empty description and not empty summary"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;import io.swagger.v3.oas.annotations.enums.ParameterStyle;

@Controller
class MyController {

    /**
     * @param id UUID of test
     */
    @Delete("/")
    @Operation(
        summary = "Delete a thing",
        description = ""
    )
    public void deleteObj(@QueryValue int id) {
    }

    /**
     * @param id UUID of test
     */
    @Delete("/del2")
    @Operation(
        summary = "Delete a thing"
    )
    public void deleteObj2(@QueryValue int id) {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.delete
        Operation operation2 = openAPI.paths?.get("/del2")?.delete

        expect:
        operation
        operation.summary == 'Delete a thing'
        operation.description == ''

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'id'
        operation.parameters[0].description == 'UUID of test'

        operation2
        operation2.summary == 'Delete a thing'
        operation2.description == ''

        operation2.parameters
        operation2.parameters.size() == 1
        operation2.parameters[0].name == 'id'
        operation2.parameters[0].description == 'UUID of test'
    }

    @Ignore("Need to fix problem with reading javadoc from class level (see this: https://github.com/micronaut-projects/micronaut-core/pull/7662)")
    void "test read javadoc from class level for records"() {
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
        return new Person();
    }
}

/**
 * The Person class description
*/
class Person {
    /**
     * This is name description
     */
    @Nullable
    private String name;
    /**
     * This is age description
     */
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}

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
        personSchema.name == 'Person'
        personSchema.type == 'object'
        personSchema.description == 'The Person class description'
        personSchema.properties.name.type == 'string'
        personSchema.properties.name.description == 'This is name description'
        personSchema.properties.age.type == 'integer'
        personSchema.properties.age.description == 'This is age description'
    }

}
