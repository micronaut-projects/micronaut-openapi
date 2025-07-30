package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenApiJsonUnwrappedSpec extends AbstractOpenApiTypeElementSpec {

    void "test JsonUnwrapped annotation and schema allOf annotation"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.Single;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Controller("/test")
interface TestOperations {

    @Post
    Single<Test> save(String name, int age);
}

@Schema(description = "Represents a pet")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class Pet {
    @Schema(name="pet-name", description = "The pet name")
    private String name;

    @Schema(name="pet-name", description = "The pet name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

@Schema(description = "Represents a cat", name="TheCat")
class Cat extends Pet {
    private boolean grinning; // duplicated in Dog with different type
    private String description; // duplicated in Dog with the same type

    public boolean isGrinning() {
	    return grinning;
    }
    public void setGrinning(boolean grinning) {
        this.grinning = grinning;
    }
    public String getDescription() {
	    return description;
    }
    public void setDescription(String description) {
	    this.description = description;
    }
}

@Schema(description = "Represents a dog")
class Dog extends Pet {
    @Schema(name = "dog-herding")
    private boolean herding;
    private String grinning;
    private String description;

    public boolean isHerding() {
	    return herding;
    }
    public void setHerding(boolean herding) {
        this.herding = herding;
    }
    public String getGrinning() {
	    return grinning;
    }
    public void setGrinning(String grinning) {
        this.grinning = grinning;
    }
    public String getDescription() {
	    return description;
    }
    public void setDescription(String description) {
	    this.description = description;
    }
}

@Schema
class Test {
    @Schema(allOf = {Cat.class, Dog.class})
    public Pet petAsSuperTypeWithoutUnwrapped;

    @JsonUnwrapped
    @Schema(allOf = {Cat.class, Dog.class})
    public Pet petAsSuperType;
}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['Test']
        Schema petSchema = openAPI.components.schemas['Pet']
        Schema catSchema = openAPI.components.schemas['TheCat']
        Schema dogSchema = openAPI.components.schemas['Dog']

        then: "the components are valid"
        openAPI.components.schemas.size() == 4
        schema.type == 'object'
        schema.properties.size() == 5
        schema.properties['petAsSuperTypeWithoutUnwrapped'].type == null
        schema.properties['petAsSuperTypeWithoutUnwrapped'].allOf.size() == 2
        schema.properties['pet-name'].type == 'string'
        schema.properties['grinning'].type == 'boolean'
        schema.properties['dog-herding'].type == 'boolean'
        schema.required == null
        petSchema.properties.size() == 1
        petSchema.properties['pet-name'].type == 'string'
        petSchema.required == null
        catSchema.properties.size() == 2
        catSchema.properties['grinning'].type == 'boolean'
        catSchema.properties['description'].type == 'string'
        catSchema.required == null
        dogSchema.properties.size() == 3
        dogSchema.properties['dog-herding'].type == 'boolean'
        dogSchema.properties['grinning'].type == 'string'
        dogSchema.properties['description'].type == 'string'
        dogSchema.required == null
    }

    void "test JsonUnwrapped annotation"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Controller("/test")
interface TestOperations {

    @Post("/")
    Single<Test> save(String name, int age);
}

class Dummy {
    public int aa;
    @JsonProperty(value = "cc", required = true)
    public String bb;
}

@Schema(description = "Represents a pet")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class Pet {
    @Schema(name="pet-name", description = "The pet name")
    private String name;
    private String lastName;
    private Integer age;

    @Schema(name="pet-name", description = "The pet name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(name="pet-age", description = "The pet age")
    public Integer getAge() {
        return age;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}

@Schema
class Test {
    public Dummy plain;

    @JsonUnwrapped
    public Dummy unwrapped;

    @JsonUnwrapped(prefix = "aaa", suffix = "zzz")
    public Dummy unwrappedRenamed;

    @JsonUnwrapped(enabled = false)
    public Dummy unwrappedDisabled;

    @JsonUnwrapped
    public Pet pet;

    @JsonUnwrapped(prefix = "aaa", suffix = "zzz")
    public Pet petRenamed;
    public Pet plainPet;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['Test']
        Schema dummySchema = openAPI.components.schemas['Dummy']
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        schema.type == 'object'
        schema.properties.size() == 13
        schema.properties['plain'].$ref == '#/components/schemas/Dummy'
        schema.properties['plainPet'].$ref == '#/components/schemas/Pet'
        schema.properties['unwrappedDisabled'].$ref == '#/components/schemas/Dummy'
        schema.properties['aa'].type == 'integer'
        schema.properties['bb'] == null
        schema.properties['cc'].type == 'string'
        schema.properties['aaaaazzz'].type == 'integer'
        schema.properties['aaabbzzz'] == null
        schema.properties['aaacczzz'].type == 'string'
        schema.properties['pet-name'].type == 'string'
        schema.properties['pet-age'].type == 'integer'
        schema.properties['last_name'].type == 'string'
        schema.properties['aaapet-namezzz'].type == 'string'
        schema.properties['aaapet-agezzz'].type == 'integer'
        schema.properties['aaalast_namezzz'].type == 'string'
        schema.required == ['aaacczzz', 'cc']
        dummySchema.properties['bb'] == null
        dummySchema.properties['cc'].type == 'string'
        dummySchema.required == ['cc']
        petSchema.properties['last_name'].type == 'string'
        petSchema.required == null
    }

    void "test build OpenAPI with JsonProperty and Schema"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

@Controller("/api/example")
class ExampleController {
    @Get
    public ExampleModel getExample() {
        return new ExampleModel();
    }
}

@Introspected
class ExampleModel {
    @JsonProperty("nameInJson")
    @Schema(description = "example field")
    private String nameInPojo;

    public String getNameInPojo() {
        return nameInPojo;
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
        Schema exampleSchema = openAPI.components.schemas.ExampleModel
        exampleSchema.type == 'object'
        !exampleSchema.properties.nameInPojo
        exampleSchema.properties.nameInJson.type == 'string'
        exampleSchema.properties.nameInJson.description == 'example field'
    }

    void "test issue with JsonUnwrapped and wildcard response type"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;

@Controller("/test")
interface TestOperations {

    @Post
    Single<Test<?>> save(String name, int age);
}

class Base {

    public String name;
}

class Test<T extends Base> {
    @JsonUnwrapped
    public T wrapped;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        Schema schema = openApi.components.schemas.Test_Base_

        then: "the components are valid"
        schema.type == 'object'
        schema.properties
        schema.properties.size() == 1
        schema.properties.name
    }

    void "test JsonUnwrapped with schema with custom name"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class DemoController {

    @Get("/")
    MyDto index() {
        return null;
    }
}

@Schema(name = "My")
class MyDto {
    @JsonUnwrapped
    public FieldsDto fields;
}

@Schema(name = "Fields") // does not work
//@Schema // works
class FieldsDto {
    public String field1;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        Schema schema = openApi.components.schemas.My

        then: "the components are valid"
        schema
        schema.type == 'object'
        schema.properties
        schema.properties.size() == 1
        schema.properties.field1
        schema.properties.field1.type == "string"
    }
}
