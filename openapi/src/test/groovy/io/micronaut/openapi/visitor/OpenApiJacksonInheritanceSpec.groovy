package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiJacksonInheritanceSpec extends AbstractOpenApiTypeElementSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/508")
    void "test render OpenAPI with @JsonSubTypes and @JsonTypeInfo"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.*;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.media.*;

@Controller("/pets")
class PetController {
    @Get
    Pet[] getPets() {
        return null;
    }

    @Post
    String createPet(Pet pet) {
        return null;
    }
}

@Introspected
@JsonSubTypes({
        @JsonSubTypes.Type(name = "CAT", value = Cat.class),
        @JsonSubTypes.Type(name = "DOG", value = Dog.class)
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
interface Pet {
    Integer getId();
}

@Introspected
class Cat implements Pet {
    private final Integer id;
    private final String name;

    public Cat(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Integer getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }
}

@Introspected
class Dog implements Pet {
    private final Integer id;
    private final Boolean barking;

    public Dog(Integer id, Boolean barking) {
        this.id = id;
        this.barking = barking;
    }

    @Override
    public Integer getId() {
        return this.id;
    }
    public Boolean isBarking() {
        return barking;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReferenceAfterPlaceholders

        expect:
        openAPI
        openAPI.paths.get("/pets")

        and:
        Schema catSchema = openAPI.components.schemas['Cat']
        catSchema instanceof Schema
        catSchema.type == 'object'
        catSchema.properties.size() == 3
        catSchema.properties['id'].type == 'integer'
        catSchema.properties['name'].type == 'string'
        catSchema.properties['type'].type == 'string'

        and:
        Schema dogSchema = openAPI.components.schemas['Dog']
        dogSchema instanceof Schema
        dogSchema.type == 'object'
        dogSchema.properties.size() == 3
        dogSchema.properties['id'].type == 'integer'
        dogSchema.properties['barking'].type == 'boolean'
        dogSchema.properties['type'].type == 'string'

        and:
        Schema petSchema = openAPI.components.schemas['Pet']
        petSchema instanceof ComposedSchema
        ComposedSchema composedPetSchema = (ComposedSchema) openAPI.components.schemas['Pet']

        composedPetSchema.type == 'object'
        composedPetSchema.properties.size() == 1
        composedPetSchema.properties['id'].type == 'integer'
        composedPetSchema.oneOf.size() == 2
        composedPetSchema.oneOf[0].$ref == '#/components/schemas/Cat'
        composedPetSchema.oneOf[1].$ref == '#/components/schemas/Dog'
        composedPetSchema.discriminator.propertyName == 'type'
        composedPetSchema.discriminator.mapping['CAT'] == '#/components/schemas/Cat'
        composedPetSchema.discriminator.mapping['DOG'] == '#/components/schemas/Dog'
    }
}
