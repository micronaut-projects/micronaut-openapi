package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

class OpenApiInheritedPojoControllerSpec extends AbstractOpenApiTypeElementSpec {

    void "test controller inheritance with generics - Issue #193"() {
        given: "An API definition"

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;

class BaseObject {

    private int a;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}

class B extends BaseObject {

    private int b;

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }
}

@Controller("/fromTestOperations")
class TestOperations extends BaseTestOperations<B> {

    @Get("/fromBaseNoGet")
    @Override
    public B getFromBaseNoAnnot() {
        return null;
    }

    @Get("/getOnlyFromB")
    public B getOnlyFromB() {
        return null;
    }

    @Override
    @Get("/fromBaseWithAnnotOverrideB")
    public B getFromBaseWithAnnotOverride() {
        return null;
    }

    @Override
    public B getFromBaseWithNoAnnotOverride() {
        return null;
    }
}

abstract class BaseTestOperations<T extends BaseObject> {

    @Get("/fromBaseOnly")
    public T getFromBaseWithAnnot() {
        return null;
    }

    @Post("/postFromBaseOnly{?foo}")
    public HttpResponse<T> postFromBaseWithAnnot(@Body T value, @QueryValue @Nullable String foo) {
        return null;
    }

    public T getFromBaseNoAnnot() {
        return null;
    }

    public T getFromBaseNoAnnotNoOverride() {
        return null;
    }

    @Get("/fromBaseWithAnnotOverride")
    public T getFromBaseWithAnnotOverride() {
        return null;
    }

    @Get("/fromBaseWithNoAnnotOverride")
    public T getFromBaseWithNoAnnotOverride() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema baseSchema = openAPI.components.schemas['BaseObject']
        Schema bSchema = openAPI.components.schemas['B']

        then: "the components are valid"
        openAPI.paths.size() == 6
        baseSchema != null
        bSchema != null

        when:
        Operation fromBaseOnlyOperation = openAPI.paths.get("/fromTestOperations/fromBaseOnly").get

        then:
        fromBaseOnlyOperation
        fromBaseOnlyOperation.responses
        fromBaseOnlyOperation.responses.size() == 1
        fromBaseOnlyOperation.responses."200"
        fromBaseOnlyOperation.responses."200".content
        fromBaseOnlyOperation.responses."200".content."application/json"
        fromBaseOnlyOperation.responses."200".content."application/json".schema
        fromBaseOnlyOperation.responses."200".content."application/json".schema.$ref
        fromBaseOnlyOperation.responses."200".content."application/json".schema.$ref == '#/components/schemas/B'

        when:
        Operation fromBaseNoGetOperation = openAPI.paths.get("/fromTestOperations/fromBaseNoGet").get

        then:
        fromBaseNoGetOperation
        fromBaseNoGetOperation.responses
        fromBaseNoGetOperation.responses.size() == 1
        fromBaseNoGetOperation.responses."200"
        fromBaseNoGetOperation.responses."200".content
        fromBaseNoGetOperation.responses."200".content."application/json"
        fromBaseNoGetOperation.responses."200".content."application/json".schema
        fromBaseNoGetOperation.responses."200".content."application/json".schema.$ref
        fromBaseNoGetOperation.responses."200".content."application/json".schema.$ref == '#/components/schemas/B'
    }

    void "test build OpenAPI doc for POJO with Inheritance and discriminator field"() {
        given: "An API definition"

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

interface PetOperations {

    /**
     * @param name The Pet Name
     * @param age The Pet age
     * @return The Pet
     */
    @Post
    Single<Pet> save(@NotBlank String name, @Min(1L) int age);

    /**
     *
     * @param name The pet name
     * @return The Pet
     */
    @Get("/{name}")
    Single<Pet> get(@NotBlank String name);

}

class Dog extends Pet {

    private String breed;

    public void setBreed(String breed) {
        this.breed = breed;
    }

    /**
     * @return The Dog breed
     */
    public String getBreed() {
        return breed;
    }
}

class Cat extends Pet {

    private int clawSize;

    public void setClawSize(int clawSize) {
        this.clawSize = clawSize;
    }

    /**
     * @return The Cat claw size
     */
    public int getClawSize() {
        return clawSize;
    }
}

/**
 * Represents the Pet type.
 */
enum PetType {
    DOG,
    CAT
}

@Schema(discriminatorProperty = "type", oneOf = {Dog.class, Cat.class})
class Pet {

    private PetType type;
    private int age;
    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    @Schema(description="Pet age", maximum="20")
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    @Schema(description="Pet name", maxLength=20)
    public String getName() {
        return name;
    }

    public void setType(PetType t) {
        type = t;
    }

    public PetType getType() {
        return type;
    }
}

@Controller("/pet/cat")
class CatController implements PetOperations {

    static Map<String, Pet> pets = new HashMap<>(10);

    @Post
    @Override
    public Single<Pet> save(@NotBlank String name, @Min(1L) int age) {
        Pet pet = new Pet();
        pet.setType(PetType.CAT);
        pet.setName(name);
        pet.setAge(age);
        pets.put(name, pet);
        return Single.just(pet);
    }

    @Get("/{name}")
    @Override
    public Single<Pet> get(@NotBlank String name) {
        return Single.just(name)
                .map( petName -> pets.get(petName));
    }

    @Get("/claw/{size}")
    public Single<Pet> findByClawSize(Integer size) {
        return Single.just(pets.entrySet()
                .stream()
                .findFirst()
                .map(Map.Entry::getValue)
                .get());
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
        catSchema.allOf[0].$ref == '#/components/schemas/Pet'
        catSchema.allOf[1].properties.size() == 1
        catSchema.allOf[1].properties['clawSize'].type == 'integer'
        petSchema.type == 'object'
        petSchema.properties.size() == 3
        petSchema.discriminator.propertyName == "type"

        petSchema.oneOf.size() == 2
        petSchema.oneOf[0].$ref == '#/components/schemas/Dog'
        petSchema.oneOf[1].$ref == '#/components/schemas/Cat'

        when:
        Operation operation = openAPI.paths.get("/pet/cat/claw/{size}").get

        then:
        operation
        operation.responses
        operation.responses.size() == 1
        operation.responses."200"
        operation.responses."200".content
        operation.responses."200".content."application/json"
        operation.responses."200".content."application/json".schema
        operation.responses."200".content."application/json".schema.$ref
    }

    void "test build OpenAPI doc for POJO with Inheritance and discriminator mapping"() {
        given: "An API definition"

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

interface PetOperations {

    /**
     * @param name The Pet Name
     * @param age The Pet age
     * @return The Pet
     */
    @Post
    Single<Pet> save(@NotBlank String name, @Min(1L) int age);

    /**
     *
     * @param name The pet name
     * @return The Pet
     */
    @Get("/{name}")
    Single<Pet> get(@NotBlank String name);

}

class Dog extends Pet {

    private String breed;

    public void setBreed(String breed) {
        this.breed = breed;
    }

    /**
     * @return The Dog breed
     */
    public String getBreed() {
        return breed;
    }
}

class Cat extends Pet {

    private int clawSize;

    public void setClawSize(int clawSize) {
        this.clawSize = clawSize;
    }

    /**
     * @return The Cat claw size
     */
    public int getClawSize() {
        return clawSize;
    }
}

/**
 * Represents the Pet type.
 */
enum PetType {
    DOG,
    CAT
}

@Schema(discriminatorProperty = "type",
        discriminatorMapping = {
            @DiscriminatorMapping(value = "DOG", schema = Dog.class),
            @DiscriminatorMapping(value = "CAT", schema = Cat.class)
        },
        oneOf = {Dog.class, Cat.class})
class Pet {

    private PetType type;
    private int age;
    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    @Schema(description="Pet age", maximum="20")
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    @Schema(description="Pet name", maxLength=20)
    public String getName() {
        return name;
    }

    public void setType(PetType t) {
        type = t;
    }

    public PetType getType() {
        return type;
    }
}

@Controller("/pet/cat")
class CatController implements PetOperations {

    static Map<String, Pet> pets = new HashMap<>(10);

    @Post
    @Override
    public Single<Pet> save(@NotBlank String name, @Min(1L) int age) {
        Pet pet = new Pet();
        pet.setType(PetType.CAT);
        pet.setName(name);
        pet.setAge(age);
        pets.put(name, pet);
        return Single.just(pet);
    }

    @Get("/{name}")
    @Override
    public Single<Pet> get(@NotBlank String name) {
        return Single.just(name)
                .map( petName -> pets.get(petName));
    }

    @Get("/claw/{size}")
    public Single<Pet> findByClawSize(Integer size) {
        return Single.just(pets.entrySet()
                .stream()
                .findFirst()
                .map(Map.Entry::getValue)
                .get());
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
        catSchema.allOf[0].$ref == '#/components/schemas/Pet'
        catSchema.allOf[1].properties.size() == 1
        catSchema.allOf[1].properties['clawSize'].type == 'integer'
        petSchema.type == 'object'
        petSchema.properties.size() == 3
        petSchema.discriminator.propertyName == "type"
        petSchema.discriminator.mapping.size() == 2
        petSchema.discriminator.mapping["DOG"] == "#/components/schemas/Dog"
        petSchema.discriminator.mapping["CAT"] == "#/components/schemas/Cat"

        petSchema.oneOf.size() == 2
        petSchema.oneOf[0].$ref == '#/components/schemas/Dog'
        petSchema.oneOf[1].$ref == '#/components/schemas/Cat'

        when:
        Operation operation = openAPI.paths.get("/pet/cat/claw/{size}").get

        then:
        operation
        operation.responses
        operation.responses.size() == 1
        operation.responses."200"
        operation.responses."200".content
        operation.responses."200".content."application/json"
        operation.responses."200".content."application/json".schema
        operation.responses."200".content."application/json".schema.$ref
    }

    void "test build OpenAPI doc for POJO with inheritance"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Controller("/pets")
interface PetOperations {

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/cases/{name}", produces = MediaType.TEXT_PLAIN)
    Cat getCat(String name);

        /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/dogs/{name}", produces = MediaType.TEXT_PLAIN)
    Dog getDog(String name);
}

class Dog extends Pet {

    private String breed;

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getBreed() {
        return breed;
    }
}

class Cat extends Pet {

    private int clawSize;

    public void setClawSize(int clawSize) {
        this.clawSize = clawSize;
    }

    public int getClawSize() {
        return clawSize;
    }
}

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
        @JsonSubTypes.Type(value = Dog.class, name = "Dog") })
class Pet {
    @Min(18)
    @Positive
    @PositiveOrZero
    private int age;

    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    @NotEmpty
    @Size(max = 30)
    @Size(min = 20)
    public String getName() {
        return name;
    }
}

@Singleton
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
        catSchema.type == 'object'
        catSchema.properties == null
        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['name'].minLength == 20
        petSchema.properties['name'].maxLength == 30

        catSchema.allOf.size() == 2
        catSchema.allOf[0].$ref == '#/components/schemas/Pet'
        catSchema.allOf[1].properties['clawSize'].type == 'integer'
    }

    void "test build OpenAPI doc for POJO with custom annotated inheritance"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import com.fasterxml.jackson.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.micronaut.http.MediaType;

@Controller("/pets")
interface PetOperations {

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/cases/{name}", produces = MediaType.TEXT_PLAIN)
    Cat getCat(String name);

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/dogs/{name}", produces = MediaType.TEXT_PLAIN)
    Dog getDog(String name);
}

@Schema(description = "Dog", allOf = { Pet.class })
class Dog extends Pet {

    private String breed;

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getBreed() {
        return breed;
    }
}

@Schema(description = "Cat", allOf = { Pet.class })
class Cat extends Pet {

    private int clawSize;

    public void setClawSize(int clawSize) {
        this.clawSize = clawSize;
    }

    public int getClawSize() {
        return clawSize;
    }
}

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
        @JsonSubTypes.Type(value = Dog.class, name = "Dog") })
@Schema(description = "Pet Desc")
class Pet {
    @jakarta.validation.constraints.Min(18)
    private int age;

    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    @jakarta.validation.constraints.Size(max = 30)
    public String getName() {
        return name;
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
        catSchema.type == 'object'
        catSchema.properties == null
        petSchema.description == 'Pet Desc'
        petSchema.type == 'object'
        petSchema.properties.size() == 2

        catSchema.allOf.size() == 2
        catSchema.allOf[0].$ref == '#/components/schemas/Pet'
        catSchema.allOf[1].properties.size() == 1
        catSchema.allOf[1].properties['clawSize'].type == 'integer'
        dogSchema.allOf.size() == 2
        dogSchema.allOf[0].$ref == '#/components/schemas/Pet'
        dogSchema.allOf[1].properties.size() == 1
    }

    void "test build OpenAPI doc for POJO with Inheritance and response discriminator mapping"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

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
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
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

    Pet() {
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

    Dog(@NotBlank String name,
               @Nullable Integer age,
               @NotNull PetType type,
               @NotNull DogBreed breed) {

        super(name, age, type);
        this.breed = breed;
    }

    Dog() {
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

    Cat(@NotBlank String name,
               @Nullable Integer age,
               @NotNull PetType type,
               @NotNull CatBreed breed) {

        super(name, age, type);
        this.breed = breed;
    }

    Cat() {
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
                            schema = @Schema(oneOf = {Dog.class, Cat.class},
                                    discriminatorProperty = "type",
                                    discriminatorMapping = {
                                            @DiscriminatorMapping(value = "DOG", schema = Dog.class),
                                            @DiscriminatorMapping(value = "CAT", schema = Cat.class),
                                    }))),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "The saved pet information",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(oneOf = {Dog.class, Cat.class},
                                            discriminatorProperty= "type",
                                            discriminatorMapping = {
                                                    @DiscriminatorMapping(value = "DOG", schema = Dog.class),
                                                    @DiscriminatorMapping(value = "CAT", schema = Cat.class),
                                            })))
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
        operation.responses
        operation.responses.size() == 1
        operation.responses."200"
        operation.responses."200".content
        operation.responses."200".content."application/json"
        operation.responses."200".content."application/json".schema

        operation.responses."200".content."application/json".schema.discriminator
        operation.responses."200".content."application/json".schema.discriminator.mapping.size() == 2
        operation.responses."200".content."application/json".schema.discriminator.mapping["CAT"] == "#/components/schemas/Cat"
        operation.responses."200".content."application/json".schema.discriminator.mapping["DOG"] == "#/components/schemas/Dog"

        operation.responses."200".content."application/json".schema.oneOf
        operation.responses."200".content."application/json".schema.oneOf.size() == 2
        operation.responses."200".content."application/json".schema.oneOf[0].get$ref() == "#/components/schemas/Dog"
        operation.responses."200".content."application/json".schema.oneOf[1].get$ref() == "#/components/schemas/Cat"
    }

    void "test build OpenAPI doc for interface POJO with inheritance"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import com.fasterxml.jackson.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.micronaut.http.MediaType;

@Controller("/pets")
interface PetOperations {

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/cases/{name}", produces = MediaType.TEXT_PLAIN)
    Cat getCat(String name);

        /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/dogs/{name}", produces = MediaType.TEXT_PLAIN)
    Dog getDog(String name);
}

interface Dog extends Pet {

    String getBreed();
}

interface Cat extends Pet {

    int getClawSize();
}

@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
        @JsonSubTypes.Type(value = Dog.class, name = "Dog") })
interface Pet {

    int getAge();

    String getName();
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
        catSchema.type == 'object'
        catSchema.properties == null
        petSchema.type == 'object'
        petSchema.properties.size() == 2

        catSchema.allOf.size() == 2
        catSchema.allOf[0].$ref == '#/components/schemas/Pet'
        catSchema.allOf[1].properties['clawSize'].type == 'integer'
    }

    void "test build OpenAPI doc for interface POJO with multiple and multi leveled inheritance"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.io.Serializable;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Controller("/pets")
interface PetOperations {

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/cases/{name}", produces = MediaType.TEXT_PLAIN)
    Cat getCat(String name);

}

interface Cat extends Pet,Sleeper {

    int getClawSize();
}

@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes(@JsonSubTypes.Type(value = Cat.class, name = "Cat"))
interface Pet extends Animal {

    int getAge();

    String getName();
}

@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes(@JsonSubTypes.Type(value = Pet.class, name = "Pet"))
interface Animal {

    double getWeight();
}

interface Sleeper extends Serializable, Readable {

    double getSleepDuration();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']
        Schema animalSchema = openAPI.components.schemas['Animal']
        Schema sleeperSchema = openAPI.components.schemas['Sleeper']
        Schema catSchema = openAPI.components.schemas['Cat']

        then: "the components are valid"
        petSchema != null
        animalSchema != null
        sleeperSchema != null
        catSchema != null

        petSchema.type == 'object'
        petSchema.properties.size() == 3
        animalSchema.type == 'object'
        animalSchema.properties.size() == 1
        sleeperSchema.type == 'object'
        sleeperSchema.properties.size() == 1
        catSchema.type == 'object'
        catSchema.properties == null

        catSchema.allOf.size() == 4
        catSchema.allOf[0].$ref == '#/components/schemas/Pet'
        catSchema.allOf[1].$ref == '#/components/schemas/Animal'
        catSchema.allOf[2].$ref == '#/components/schemas/Sleeper'
        catSchema.allOf[3].properties['clawSize'].type == 'integer'

        petSchema.allOf.size() == 1
        petSchema.allOf[0].$ref == '#/components/schemas/Animal'
    }

    void "test build OpenAPI for interface inheritance with generics"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

@Controller("/pets")
interface MyOperations extends Api<MyRequest, MyResponse>, SecApi {
}

interface SecApi {

    @Post("/api/sec/request")
    String getRequestSec();
}

interface Api<T, R> extends ParentApi<ParentRequest> {

    @Get("/api/request")
    T getRequest();

    @Get("/api/response")
    R getResponse();

    String otherMethod();
}

interface ParentApi<P> {

    @Get("/api/parentRequest")
    P getParentRequest();
}

class MyRequest {

    private String prop1;

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }
}

class ParentRequest {

    private String prop3;

    public String getProp3() {
        return prop3;
    }

    public void setProp3(String prop3) {
        this.prop3 = prop3;
    }
}

class MyResponse {

    private String prop2;

    public String getProp2() {
        return prop2;
    }

    public void setProp2(String prop2) {
        this.prop2 = prop2;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths.size() == 4
        Operation opRequest = openAPI.paths."/pets/api/request".get
        Operation opResponse = openAPI.paths."/pets/api/response".get
        Operation opParentResponse = openAPI.paths."/pets/api/parentRequest".get
        Operation opSecResponse = openAPI.paths."/pets/api/sec/request".post

        then:
        opRequest
        opResponse
        opParentResponse
        opSecResponse
    }

    void "test build OpenAPI for class inheritance with generics"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/pets")
class MyOperations extends Api<MyRequest, MyResponse> {
}

abstract class Api<T, R> extends ParentApi<ParentRequest> {

    @Get("/api/request")
    T getRequest() {
        return null;
    }

    @Get("/api/response")
    R getResponse() {
        return null;
    }

    String otherMethod() {
        return null;
    }
}

abstract class ParentApi<P> {

    @Get("/api/parentRequest")
    P getParentRequest() {
        return null;
    }
}

class MyRequest {

    private String prop1;

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }
}

class ParentRequest {

    private String prop3;

    public String getProp3() {
        return prop3;
    }

    public void setProp3(String prop3) {
        this.prop3 = prop3;
    }
}

class MyResponse {

    private String prop2;

    public String getProp2() {
        return prop2;
    }

    public void setProp2(String prop2) {
        this.prop2 = prop2;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths.size() == 3

        Operation opRequest = openAPI.paths."/pets/api/request".get
        Operation opResponse = openAPI.paths."/pets/api/response".get
        Operation opParentRequest = openAPI.paths."/pets/api/parentRequest".get

        then:
        opRequest
        opResponse
        opParentRequest
    }

    void "test class inheritance Schema"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class MyOperations {

    @Post("/get")
    void getRequest(@Body MyDto dto) {
    }
}

@Schema(name = "MyDto", description = "This is filter")
@Introspected
class MyDto extends FilterDto {

    @Schema(hidden = true)
    private final String entityType;
    private final String entitySubType;
    @Schema(description = "My description")
    private final List<RelationFilter> relationFilters;

    MyDto(String entityType, String entitySubType, List<RelationFilter> relationFilters, Integer page) {
        super(page);
        this.entityType = entityType;
        this.entitySubType = entitySubType;
        this.relationFilters = relationFilters;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntitySubType() {
        return entitySubType;
    }

    public List<RelationFilter> getRelationFilters() {
        return relationFilters;
    }
}

@Schema(name = "FilterDto", description = "This is parent filter")
@Introspected
class FilterDto {

    @Schema(name="page", description = "Page description", type = "integer", defaultValue = "0")
    private final Integer page;

    FilterDto(Integer page) {
        this.page = page;
    }

    public Integer getPage() {
        return page;
    }
}

@Introspected
class RelationFilter {
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        Schema filterDtoSchema = openAPI.components.schemas.FilterDto

        then:
        myDtoSchema
        !myDtoSchema.properties.entityType
        myDtoSchema.properties.entitySubType
        myDtoSchema.properties.relationFilters
        myDtoSchema.allOf
        myDtoSchema.allOf.size() == 1
        myDtoSchema.allOf[0].$ref == '#/components/schemas/FilterDto'

        filterDtoSchema
        filterDtoSchema.properties.page
        filterDtoSchema.properties.page.type == 'integer'
        filterDtoSchema.properties.page.default == 0
    }

    void "test class two level inheritance Schema"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class MyOperations {

    @Get("/get")
    ExportPaymentsRequest getRequest() {
        return null;
    }
}

@Schema(name = "RequestType2_4_0")
class RequestType {
}

@Schema(name = "ExportRequestType2_4_0")
class ExportRequestType extends RequestType {
}

/**
 * My request
 */
@Schema(name = "ExportPaymentsRequest2_4_0")
class ExportPaymentsRequest extends ExportRequestType {

    protected Object field;

    public Object getField() {
        return field;
    }

    public void setField(Object field) {
        this.field = field;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema requestTypeSchema = openAPI.components.schemas.RequestType2_4_0
        Schema exportRequestTypeSchema = openAPI.components.schemas.ExportRequestType2_4_0
        Schema exportPaymentsRequestSchema = openAPI.components.schemas.ExportPaymentsRequest2_4_0

        then:
        requestTypeSchema
        requestTypeSchema.type == 'object'
        !requestTypeSchema.allOf

        exportRequestTypeSchema
        !exportRequestTypeSchema.allOf
        exportRequestTypeSchema.$ref == '#/components/schemas/RequestType2_4_0'

        exportPaymentsRequestSchema
        exportPaymentsRequestSchema.allOf
        exportPaymentsRequestSchema.allOf.size() == 2
        exportPaymentsRequestSchema.allOf[0].$ref == '#/components/schemas/ExportRequestType2_4_0'
        exportPaymentsRequestSchema.allOf[1].$ref == '#/components/schemas/RequestType2_4_0'
    }

    void "test type is null for allOf"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/bug12")
@Tag(name = "Bug12")
class BackupController {

    @Get("/backup")
    BackupDto12 getBackup() {
        return new BackupDto12(new BackupSubObject12("somewhere"), BackupSubEnum12.WORKING);
    }
}

@Introspected
@Schema(description = "Schema to represent a backup")
record BackupDto12(

        @Schema(description = "Referenced class. $ref shouldn't specify the type of BackupSubObject12")
        BackupSubObject12 location,

        @Schema(description = "Referenced enum. $ref shouldn't specify the type of BackupSubEnum12")
        BackupSubEnum12 state
) {
}

@Introspected
@Schema(description = "Schema to describe the backup location")
record BackupSubObject12(

        @Schema(description = "Location of the backup")
        String backupLocation
) {
}

@Introspected
@Schema(description = "State of the backup")
enum BackupSubEnum12 {
    WORKING,
    FAILED
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema backupDto12Schema = openAPI.components.schemas.BackupDto12

        then:
        backupDto12Schema
        backupDto12Schema.properties
        backupDto12Schema.properties.location
        !backupDto12Schema.properties.location.type
        backupDto12Schema.properties.location.description == 'Referenced class. $ref shouldn\'t specify the type of BackupSubObject12'
        backupDto12Schema.properties.location.allOf
        backupDto12Schema.properties.location.allOf.size() == 1
        backupDto12Schema.properties.location.allOf[0].$ref == '#/components/schemas/BackupSubObject12'

        backupDto12Schema.properties.state
        !backupDto12Schema.properties.state.type
        backupDto12Schema.properties.state.description == 'Referenced enum. $ref shouldn\'t specify the type of BackupSubEnum12'
        backupDto12Schema.properties.state.allOf
        backupDto12Schema.properties.state.allOf.size() == 1
        backupDto12Schema.properties.state.allOf[0].$ref == '#/components/schemas/BackupSubEnum12'
    }
}
