package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiClassPropertySpec extends AbstractOpenApiTypeElementSpec {
    void "test class property is not exposed"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
class Person {

    private String name;
    private Class<?> personClass;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setPersonClass(Class<?> personClass) {
        this.personClass = personClass;
    }

    public Class<?> getPersonClass() {
        return personClass;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 1

        openAPI.components.schemas["Person"].properties["name"]
    }
}
