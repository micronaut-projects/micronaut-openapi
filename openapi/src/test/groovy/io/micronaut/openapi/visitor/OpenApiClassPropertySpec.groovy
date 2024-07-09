package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiClassPropertySpec extends AbstractOpenApiTypeElementSpec {

    void "test class property is not exposed"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

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

    public Class<?> getTestProp() {
        return personClass;
    }

    public String getNotHiddenProp() {
        return "";
    }

    @Hidden
    public String getHiddenProp2() {
        return "";
    }

    @JsonIgnore
    public String getHiddenProp3() {
        return "";
    }

    @Schema(hidden = true)
    public String getHiddenProp4() {
        return "";
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
        openAPI.components.schemas["Person"].properties.size() == 2

        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["notHiddenProp"]
    }
}
