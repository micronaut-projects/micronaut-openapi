
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import spock.lang.PendingFeature

class OpenApiNullableTypesSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test build OpenAPI for java.util.Optional"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.PetController','''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;

import java.util.List;
import java.util.Optional;

class Pet {
    private Optional<Integer> age;
    private Optional<String> name;

    public void setAge(Optional<Integer> a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public Optional<Integer> getAge() {
        return age;
    }

    public void setName(Optional<String> n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public Optional<String> getName() {
        return name;
    }
}

@Controller
class PetController {

    @Get("/pet/{name}")
    HttpResponse<Pet> get(String name) {
        return HttpResponse.ok();
    }
    
    @Post("/pet/")
    HttpResponse<Pet> post(Pet p) {
       return HttpResponse.ok();
    }
}

''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2

        petSchema.properties["age"].type == "integer"
        petSchema.properties["name"].type == "string"


    }

}
