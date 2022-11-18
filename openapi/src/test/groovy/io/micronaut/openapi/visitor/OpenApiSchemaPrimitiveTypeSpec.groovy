package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiSchemaPrimitiveTypeSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI with Java @Schema implementation/type override"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Controller
class MyController {
    @Put
    public Response<Pet> updatePet(Pet pet) {
        return null;
    }
}

class Pet {

    private PetType petType;
    private PetAge age;

    public PetType getPetType() { return petType; }

    public void setPetType(PetType newValue) { this.petType = newValue; }

    public PetAge getPetAge() { return age; }

    public void setPetAge(PetAge newValue) { this.age = newValue; }
}

@Schema(implementation = String.class, pattern = "pet_pattern", minLength = 3, maxLength = 10, example = "Dog", defaultValue = "Cat")
class PetType {

    private String value = "Cat";

    @JsonCreator
    public PetType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

@Schema(type = "integer", minimum = "1", exclusiveMinimum = true, maximum = "200", exclusiveMaximum = true, multipleOf = 1)
class PetAge {

    private Integer value = 2;

    @JsonCreator
    public PetAge(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }
}

class Response<T> {

    T r;

    public T getResult() {
        return r;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['petType'].type == 'string'
        openAPI.components.schemas['Pet'].properties['petType'].pattern == 'pet_pattern'
        openAPI.components.schemas['Pet'].properties['petType'].maxLength == 10
        openAPI.components.schemas['Pet'].properties['petType'].minLength == 3
        openAPI.components.schemas['Pet'].properties['petType'].example == "Dog"
        openAPI.components.schemas['Pet'].properties['petType'].default == "Cat"
        openAPI.components.schemas['Pet'].properties['petType'].exclusiveMaximum == null
        openAPI.components.schemas['Pet'].properties['petType'].exclusiveMinimum == null
        openAPI.components.schemas['Pet'].properties['petType'].multipleOf == null

        openAPI.components.schemas['Pet'].properties['petAge'].type == 'integer'
        openAPI.components.schemas['Pet'].properties['petAge'].maximum == 200
        openAPI.components.schemas['Pet'].properties['petAge'].exclusiveMaximum
        openAPI.components.schemas['Pet'].properties['petAge'].minimum == 1
        openAPI.components.schemas['Pet'].properties['petAge'].exclusiveMinimum
        openAPI.components.schemas['Pet'].properties['petAge'].multipleOf == 1
        openAPI.components.schemas['Pet'].properties['petAge'].default == null
        openAPI.components.schemas['Response_Pet_'].properties['result'].$ref == '#/components/schemas/Pet'
    }
}
