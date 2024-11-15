package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

class OpenApiComplexSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI doc for oneOf, allOf and anyOf keyword"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.micronaut.core.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Pet type.
 */
enum PetType {
    DOG,
    CAT
}

/**
 * Represents a pet.
 */
@JsonTypeInfo(include = JsonTypeInfo.As.EXISTING_PROPERTY, use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = PetType.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Cat.class, name = "CAT"),
        @JsonSubTypes.Type(value = Dog.class, name = "DOG")})
@Introspected
abstract class Pet {

    /**
     * The name
     */
    @NotBlank
    private String name;

    /**
     * The age
     */
    @Nullable
    Integer age;

    /**
     * The {@link PetType} type
     */
    @NotNull
    private PetType type;

    public Pet(@NotBlank String name,
               @Nullable Integer age,
               @NotNull PetType type) {

        this.name = name;
        this.age = age;
        this.type = type;
    }

    public Pet() {
    }

    @NotBlank
    public String getName() {
        return name;
    }

    @Nullable
    public Integer getAge() {
        return age;
    }

    @NotNull
    public PetType getType() {
        return type;
    }

    public void setName(@NotBlank String name) {
        this.name = name;
    }

    public void setAge(@Nullable Integer age) {
        this.age = age;
    }

    public void setType(@NotNull PetType type) {
        this.type = type;
    }
}

/**
 * Represents a Dog.
 */
@Introspected
class Dog extends Pet {

    @NotNull
    private DogBreed breed;

    public Dog(@NotBlank String name,
               @Nullable Integer age,
               @NotNull PetType type,
               @NotNull DogBreed breed) {

        super(name, age, type);
        this.breed = breed;
    }

    public Dog() {
        super();
    }

    @NotNull
    public DogBreed getBreed() {
        return breed;
    }

    public void setBreed(@NotNull DogBreed breed) {
        this.breed = breed;
    }
}

enum DogBreed {
    LABRADOR,
    POMERANIAN
}

/**
 * Represents a Cat.
 */
@Introspected
class Cat extends Pet {

    @NotNull
    private CatBreed breed;

    public Cat(@NotBlank String name,
               @Nullable Integer age,
               @NotNull PetType type,
               @NotNull CatBreed breed) {

        super(name, age, type);
        this.breed = breed;
    }

    public Cat() {
        super();
    }

    @NotNull
    public CatBreed getBreed() {
        return breed;
    }

    public void setBreed(@NotNull CatBreed breed) {
        this.breed = breed;
    }
}

enum CatBreed {
    PERSIAN,
    MAINE
}

@Controller("/pets")
class PetController {

    @Operation(summary = "Save a pet",
            description = "The saved pet information is returned",
            requestBody = @RequestBody(description = "",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = {Dog.class, Cat.class}))),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "The saved pet information",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(anyOf = {Dog.class, Cat.class})))
            }
    )
    @Tag(name = "save-pet")
    @Post
    HttpResponse<Pet> save(@Valid @NotNull @Body Pet pet) {
        return HttpResponse.ok(pet);
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']
        Schema catSchema = openAPI.components.schemas['Cat']
        Schema dogSchema = openAPI.components.schemas['Dog']

        then: "the components are valid"
        petSchema != null
        dogSchema != null
        catSchema != null

        petSchema instanceof Schema
        catSchema instanceof ComposedSchema
        dogSchema instanceof ComposedSchema

        petSchema.type == 'object'
        petSchema.properties.size() == 3

        ((ComposedSchema) catSchema).allOf.size() == 2
        ((ComposedSchema) catSchema).allOf[0].get$ref() == "#/components/schemas/Pet"
        ((ComposedSchema) catSchema).allOf[1].properties.size() == 1
        ((ComposedSchema) catSchema).allOf[1].properties.get("breed") instanceof Schema
        ((ComposedSchema) catSchema).allOf[1].properties.get("breed").get$ref() == "#/components/schemas/CatBreed"

        ((ComposedSchema) dogSchema).allOf.size() == 2
        ((ComposedSchema) dogSchema).allOf[0].$ref == '#/components/schemas/Pet'
        ((ComposedSchema) dogSchema).allOf[1].properties.size() == 1
        ((ComposedSchema) dogSchema).allOf[1].properties.get("breed") instanceof Schema
        ((ComposedSchema) dogSchema).allOf[1].properties.get("breed").get$ref() == "#/components/schemas/DogBreed"

        when:
        Operation operation = openAPI.paths.get("/pets").post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema.oneOf
        operation.requestBody.content."application/json".schema.oneOf.size() == 2
        operation.requestBody.content."application/json".schema.oneOf[0].get$ref() == "#/components/schemas/Dog"
        operation.requestBody.content."application/json".schema.oneOf[1].get$ref() == "#/components/schemas/Cat"

        and:
        operation.responses
        operation.responses.size() == 1
        operation.responses."200"
        operation.responses."200".content
        operation.responses."200".content."application/json"
        operation.responses."200".content."application/json".schema

        operation.responses."200".content."application/json".schema.anyOf
        operation.responses."200".content."application/json".schema.anyOf.size() == 2
        operation.responses."200".content."application/json".schema.anyOf[0].get$ref() == "#/components/schemas/Dog"
        operation.responses."200".content."application/json".schema.anyOf[1].get$ref() == "#/components/schemas/Cat"
    }
}
