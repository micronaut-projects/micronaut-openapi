package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiSchemaJavaTimeSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI with response that contains Java 8 date-time types"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import java.time.*;
import java.util.List;

@Controller("/")
class MyController {

    @Put("/")
    public Response<Pet> updatePet(Pet pet) {
        return null;
    }
}

class Pet {
    private LocalDateTime local;

    public LocalDateTime getLocal() { return local; }
    public void setLocal(LocalDateTime newValue) { this.local = newValue; }

    private OffsetDateTime offset;

    public OffsetDateTime getOffset() { return offset; }
    public void setOffsetDateTime(OffsetDateTime newValue) { this.offset = newValue; }

    private ZonedDateTime zoned;

    public ZonedDateTime getZoned() { return zoned; }
    public void setZoned(ZonedDateTime newValue) { this.zoned = newValue; }

    private Instant instant;

    public Instant getInstant() { return instant; }
    public void setInstant(Instant newValue) { this.instant = newValue; }

}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['local'].type == 'string'
        openAPI.components.schemas['Pet'].properties['local'].format == 'date-time'
        openAPI.components.schemas['Pet'].properties['offset'].type == 'string'
        openAPI.components.schemas['Pet'].properties['offset'].format == 'date-time'
        openAPI.components.schemas['Pet'].properties['zoned'].type == 'string'
        openAPI.components.schemas['Pet'].properties['zoned'].format == 'date-time'
        openAPI.components.schemas['Pet'].properties['instant'].type == 'string'
        openAPI.components.schemas['Pet'].properties['instant'].format == 'date-time'
        openAPI.components.schemas['Response_Pet_'].properties['result'].$ref == '#/components/schemas/Pet'
    }

    void "test parse the OpenAPI with response that contains Java 8 date types"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import java.time.*;
import java.util.List;

@Controller("/")
class MyController {

    @Put("/")
    public Response<Pet> updatePet(Pet pet) {
        return null;
    }
}

class Pet {
    private LocalDate local;

    public LocalDate getLocal() { return local; }
    public void setLocal(LocalDate newValue) { this.local = newValue; }

}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['local'].type == 'string'
        openAPI.components.schemas['Pet'].properties['local'].format == 'date'
        openAPI.components.schemas['Response_Pet_'].properties['result'].$ref == '#/components/schemas/Pet'
    }

    void "test parse the OpenAPI with Java @Schema implementation/type override"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import com.fasterxml.jackson.annotation.JsonCreator;import com.fasterxml.jackson.annotation.JsonValue;import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import java.time.*;
import java.util.List;

@Controller("/")
class MyController {

    @Put("/")
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


@Schema(type = "Integer", minimum = "1", exclusiveMinimum = true, maximum = "200", exclusiveMaximum = true, multipleOf = 1)
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
    };
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
