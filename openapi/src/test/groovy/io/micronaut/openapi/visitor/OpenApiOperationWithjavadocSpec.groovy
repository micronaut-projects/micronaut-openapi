package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter

class OpenApiOperationWithjavadocSpec extends AbstractOpenApiTypeElementSpec {

    void "test javadoc description, summary and parameters"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;

@Controller
class MyController {

    /**
     * Description and summary here
     * @param id UUID of test
     */
    @Delete("/")
    public MyDto deleteObj(@QueryValue int id) {
        return null;
    }
}

/**
* My dto description
*
* @property test property desc
*/
class MyDto {

    private String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.delete
        Schema schema = openAPI.components.schemas.MyDto

        expect:
        operation
        operation.summary == 'Description and summary here'
        operation.description == 'Description and summary here'

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'id'
        operation.parameters[0].description == 'UUID of test'

        schema
        schema.properties.test
        schema.properties.test.description == 'property desc'

    }

    void "test Operation description, summary and parameters"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;

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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;

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
        operation.description == null

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'id'
        operation.parameters[0].description == 'UUID of test'

        operation2
        operation2.summary == 'Delete a thing'
        operation2.description == null

        operation2.parameters
        operation2.parameters.size() == 1
        operation2.parameters[0].name == 'id'
        operation2.parameters[0].description == 'UUID of test'
    }

    void "test read javadoc from class level and attribute level"() {
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
        personSchema.type == 'object'
        personSchema.description == 'The Person class description'
        personSchema.properties.name.type == 'string'
        personSchema.properties.name.description == 'This is name description'
        personSchema.properties.age.type == 'integer'
        personSchema.properties.age.description == 'This is age description'
    }

    void "test javadoc return tag"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@ApiResponse(responseCode = "200", description = "Desc10")
@ApiResponse(responseCode = "404", description = "Desc1")
@ApiResponse(responseCode = "400", description = "Desc2")
@ApiResponse(responseCode = "500", description = "Desc3")
@Controller
class PersonController {

    /**
     * This is description.
     *
     * @return Returns the Person
     */
    @ApiResponse(responseCode = "200", description = "Desc4")
    @Get("/person")
    Person getPerson() {
        return new Person();
    }

    /**
     * This is description.
     *
     * @return Returns the Person
     */
    @Status(HttpStatus.SEE_OTHER)
    @Post("/person")
    Person postPerson() {
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
        Operation get = openAPI.paths."/person".get
        get
        get.description == 'This is description.'
        get.responses.size() == 4
        get.responses."404".description == 'Desc1'
        get.responses."400".description == 'Desc2'
        get.responses."500".description == 'Desc3'
        get.responses."200".description == 'Desc4'

        Operation post = openAPI.paths."/person".post
        post.description == 'This is description.'
        post.responses.size() == 5
        post.responses."200".description == 'Desc10'
        post.responses."404".description == 'Desc1'
        post.responses."400".description == 'Desc2'
        post.responses."500".description == 'Desc3'
        post.responses."303".description == 'Returns the Person'
    }

}
