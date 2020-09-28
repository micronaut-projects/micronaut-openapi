
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

class OpenApiPojoControllerSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test build OpenAPI for List"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    T find(String slug);
}

class Pet {
    private int age;
    private String name;
    private List<String> tags;
    public InnerBean inner;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }


    /**
     * The Pet Tags
     *
     * @return The Tag
     */
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public static class InnerBean {
        public String xyz;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']
        Schema petInnerBeanSchema = openAPI.components.schemas['Pet.InnerBean']

        then:"the components are valid"
        petInnerBeanSchema
        petSchema.type == 'object'
        petSchema.properties.size() == 4

        petSchema.properties["tags"].type == "array"
        petSchema.properties["tags"].description == "The Pet Tags"
        ((ArraySchema) petSchema.properties["tags"]).items.type == "string"
    }

    void "test build OpenAPI for Dictionaries, HashMaps and Associative Arrays" () {
        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/")
    List<T> list();

    @Get("/random")
    T random();

    @Get("/vendor/{name}")
    List<T> byVendor(String name);

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    T find(String slug);

    @Post("/")
    T save(@Body T pet);
}

class Pet {
    private int age;
    private String name;
    private Map freeForm;
    private Map<String, String> dictionariesPlain;
    private Map<String, Tag> tags;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }

    /**
     * A free-form object
     *
     * @return A free-form object
     */
    public Map getFreeForm() {
        return freeForm;
    }

    public void setFreeForm(Map freeForm) {
        this.freeForm = freeForm;
    }

    /**
     * A string-to-string dictionary
     *
     * @return A string-to-string dictionary
     */
    public Map<String, String> getDictionariesPlain() {
        return dictionariesPlain;
    }

    public void setDictionariesPlain(Map<String, String> dictionariesPlain) {
        this.dictionariesPlain = dictionariesPlain;
    }

    /**
     * A string-to-object dictionary
     *
     * @return A string-to-object dictionary
     */
    public Map<String, Tag> getTags() {
        return tags;
    }

    public void setTags(Map<String, Tag> tags) {
        this.tags = tags;
    }
}

class Tag {
    private String name;
    private String description;

    public Tag(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * The Tag Name
     *
     * @return The Tag Name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The Tag Description
     *
     * @return The Tag Description
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']
        Schema tagSchema = openAPI.components.schemas['Tag']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 5

        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'The Pet Age'

        petSchema.properties['name'].type == 'string'
        petSchema.properties['name'].description == 'The Pet Name'

        ((MapSchema)petSchema.properties['freeForm']).type == "object"
        ((MapSchema)petSchema.properties['freeForm']).description == "A free-form object"
        ((MapSchema) petSchema.properties['freeForm']).getAdditionalProperties() == true

        ((MapSchema) petSchema.properties['dictionariesPlain']).type == "object"
        ((MapSchema) petSchema.properties['dictionariesPlain']).description == "A string-to-string dictionary"
        ((Schema)((MapSchema) petSchema.properties['dictionariesPlain']).getAdditionalProperties()).getType() == "string"

        ((MapSchema) petSchema.properties['tags']).type == "object"
        ((MapSchema) petSchema.properties['tags']).description == "A string-to-object dictionary"
        ((Schema)((MapSchema) petSchema.properties['tags']).getAdditionalProperties()).$ref == "#/components/schemas/Tag"

        tagSchema.properties['name'].type == "string"
        tagSchema.properties['name'].description == "The Tag Name"
        tagSchema.properties['description'].type == "string"
    }

    void "test build OpenAPI doc for POJO type with javax.constraints"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/")
    List<T> list();

    @Get("/random")
    T random();

    @Get("/vendor/{name}")
    List<T> byVendor(String name);

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    T find(String slug);

    @Post("/")
    T save(@Body T pet);
}

//@Schema
class Pet {
    @javax.validation.constraints.Min(18)
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

    @javax.validation.constraints.Size(max=30)
    public String getName() {
        return name;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'The age'
        petSchema.properties['age'].minimum == 18
        petSchema.properties['name'].type == 'string'
        petSchema.properties['name'].maxLength == 30
    }

    void "test build OpenAPI doc for POJO type with generics non-reactive"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/")
    List<T> list();

    @Get("/random")
    T random();

    @Get("/vendor/{name}")
    List<T> byVendor(String name);

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    T find(String slug);

    @Post("/")
    T save(@Body T pet);
}

//@Schema
class Pet {
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

    public String getName() {
        return name;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'The age'
        petSchema.properties['name'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'list'
        pathItem.get.description == 'List the pets'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].description == 'a list of pet names'
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['default'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1


        when:"the /{slug} path is retrieved"
        pathItem = openAPI.paths.get("/pets/{slug}")

        then:"it is included in the OpenAPI doc"
        pathItem.get.description == 'Find a pet by a slug'
        pathItem.get.operationId == 'find'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'slug'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].description == 'The slug name'
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['default'] != null
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.$ref == '#/components/schemas/Pet'

    }

    @PendingFeature(reason = "Requires upstream fix in Micronaut core")
    void "test build OpenAPI doc for POJO type with generics"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/")
    Single<List<T>> list();

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/flowable")
    Flowable<T> flowable();

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/observable")
    Observable<T> observable();

    @Get("/random")
    Maybe<T> random();

    @Get("/vendor/{name}")
    Single<List<T>> byVendor(String name);

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    Maybe<T> find(String slug);

    @Post("/")
    Single<T> save(@Body T pet);

    @Post("/completable")
    Completable completable(@Body T pet);

    @Get("/singleHttpResponse")
    Single<HttpResponse<T>> singleHttpResponse();
}

//@Schema
class Pet {
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

    public String getName() {
        return name;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'The age'
        petSchema.properties['name'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'list'
        pathItem.get.description == 'List the pets'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].description == 'a list of pet names'
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['default'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1


        when:"the /{slug} path is retrieved"
        pathItem = openAPI.paths.get("/pets/{slug}")

        then:"it is included in the OpenAPI doc"
        pathItem.get.description == 'Find a pet by a slug'
        pathItem.get.operationId == 'find'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'slug'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].description == 'The slug name'
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['default'] != null
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.$ref == '#/components/schemas/Pet'

        when:"A flowable is returned"
        pathItem = openAPI.paths.get("/pets/flowable")

        then:
        pathItem.get.operationId == 'flowable'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].description == 'a list of pet names'
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['default'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'

        when:"A completable is returned"
        pathItem = openAPI.paths.get("/pets/completable")

        then:
        pathItem.post.operationId == 'completable'
        pathItem.post.responses['default']
        pathItem.post.responses['default'].description == 'completable default response'
        pathItem.post.responses['default'].content == null


        when:"An obsevable is returned"
        pathItem = openAPI.paths.get("/pets/observable")

        then:
        pathItem.get.operationId == 'observable'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].description == 'a list of pet names'
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['default'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'

        when:"A Single<HttpResponse<T>> is returned"
        pathItem = openAPI.paths.get("/pets/singleHttpResponse")

        then:
        pathItem.get.operationId == 'singleHttpResponse'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.$ref == '#/components/schemas/Pet'
    }

    @PendingFeature(reason = "Requires upstream fix in Micronaut core")
    void "test build OpenAPI doc for POJO with custom Schema"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/")
    Single<List<T>> list();

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/flowable")
    Flowable<T> flowable();

    @Get("/random")
    Maybe<T> random();

    @Get("/vendor/{name}")
    Single<List<T>> byVendor(String name);

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    Maybe<T> find(String slug);

    @Post("/")
    Single<T> save(@Body T pet);
}

@Schema(name="MyPet", description="Pet description", requiredProperties={"type", "age", "name"})
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

enum PetType {
    DOG, CAT;
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['MyPet']
        Schema petType = openAPI.components.schemas['PetType']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.description == "Pet description"
        petSchema.required == ['age', 'name', 'type']
        petSchema.properties.size() == 3
        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'Pet age'
        petSchema.properties['age'].maximum == 20
        petSchema.properties['name'].type == 'string'
        petSchema.properties['name'].description == 'Pet name'
        petSchema.properties['name'].maxLength == 20
        petSchema.properties['type'].$ref == '#/components/schemas/PetType'
        petType.type == 'string'
        petType.enum.contains('DOG')
        petType.enum.contains('CAT')

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'list'
        pathItem.get.description == 'List the pets'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].description == 'a list of pet names'
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['default'].content['application/json'].schema.items.$ref == '#/components/schemas/MyPet'
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1


        when:"the /{slug} path is retrieved"
        pathItem = openAPI.paths.get("/pets/{slug}")

        then:"it is included in the OpenAPI doc"
        pathItem.get.description == 'Find a pet by a slug'
        pathItem.get.operationId == 'find'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'slug'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].description == 'The slug name'
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['default'] != null
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.$ref == '#/components/schemas/MyPet'

        when:"A flowable is returned"
        pathItem = openAPI.paths.get("/pets/flowable")

        then:
        pathItem.get.operationId == 'flowable'
        pathItem.get.responses['default']
        pathItem.get.responses['default'].description == 'a list of pet names'
        pathItem.get.responses['default'].content['application/json'].schema
        pathItem.get.responses['default'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['default'].content['application/json'].schema.items.$ref == '#/components/schemas/MyPet'
    }


    void "test build OpenAPI doc for POJO with properties not required as default"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post("/")
    Single<T> save(@Body T pet);
}

@Schema(name="MyPet", description="Pet description")
class Pet {
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
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['MyPet']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.description == "Pet description"
        !petSchema.required
        petSchema.properties.size() == 2
        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'Pet age'
        petSchema.properties['age'].maximum == 20
        petSchema.properties['name'].type == 'string'
        petSchema.properties['name'].description == 'Pet name'
        petSchema.properties['name'].maxLength == 20

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1

    }


    void "test build OpenAPI doc when no Body tag specified in POST"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post("/")
    Single<T> save(String name, int age);
}

class Pet {
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

    public String getName() {
        return name;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then:"the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['age'].type == 'integer'
        petSchema.properties['age'].description == 'The age'
        petSchema.properties['name'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.type == 'object'
        pathItem.post.requestBody.content['application/json'].schema.properties.size() == 2
        pathItem.post.requestBody.content['application/json'].schema.properties['name'].type == 'string'
        !pathItem.post.requestBody.content['application/json'].schema.properties['name'].nullable
    }

    void "test build OpenAPI for response with multiple content types"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Produces({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Get("/{slug}")
    T find(String slug);
}

class Pet {
    private int age;
    private String name;
    private List<String> tags;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }


    /**
     * The Pet Tags
     *
     * @return The Tag
     */
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"The state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"The operation has only one path"
        openAPI.paths.size() == 1

        when: "The GET /pets/{slug} operation is retrieved"
        Operation operation = openAPI.paths?.get("/pets/{slug}")?.get

        then: "The response has multiple content types"
        operation.responses.size() == 1
        operation.responses.default.content.size() == 2
        operation.responses.default.content['application/json'].schema
        operation.responses.default.content['application/x-www-form-urlencoded'].schema
    }

    void "test build OpenAPI for body with multiple content types"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Saves a Pet
     *
     * @param pet The Pet details
     * @return A pet or 404
     */
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Post("/")
    T save(@Body T pet);

}

class Pet {
    private int age;
    private String name;
    private List<String> tags;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }

    /**
     * The Pet Tags
     *
     * @return The Tag
     */
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"The state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"The operation has only one path"
        openAPI.paths.size() == 1

        when: "The POST /pets operation is retrieved"
        Operation operation = openAPI.paths?.get("/pets")?.post

        then: "The body has multiple content types"
        operation.requestBody
        operation.requestBody.content.size() == 2
        operation.requestBody.content['application/json'].schema
        operation.requestBody.content['application/x-www-form-urlencoded'].schema
    }

    void "test build OpenAPI for body tagged with Swagger @RequestBody"() {

        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Saves a Pet
     *
     * @param pet The Pet details
     * @return A pet or 404
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/")
    T save(
    @RequestBody(description = "A pet", required = false, content = {
        @Content(mediaType = "application/json",
                 schema = @Schema(implementation = Pet.class),
                 examples = {
                    @ExampleObject(name = "example-1", value = "{\\"name\\":\\"Charlie\\"}")})}) T pet);

}

class Pet {
    private int age;
    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"The state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"The operation has only one path"
        openAPI.paths.size() == 1

        when: "The POST /pets operation is retrieved"
        Operation operation = openAPI.paths?.get("/pets")?.post

        then: "The body has specified attributes"
        operation.requestBody
        operation.requestBody.description == 'A pet'
        !operation.requestBody.required
        operation.requestBody.content.size() == 1
        operation.requestBody.content['application/json'].schema
        operation.requestBody.content['application/json'].schema.$ref == '#/components/schemas/Pet'
        operation.requestBody.content['application/json'].examples
        operation.requestBody.content['application/json'].examples.'example-1'
        operation.requestBody.content['application/json'].examples.'example-1'.value
        operation.requestBody.content['application/json'].examples.'example-1'.value.name == 'Charlie'
    }

    void "test build OpenAPI for multiple content types and parameters"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Saves a Pet
     *
     * @param name The Pet name
     * @param aget The Pet age
     * @return A pet or 404
     */
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Post("/")
    T save(String name, int age);
}

class Pet {
    private int age;
    private String name;
    private List<String> tags;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }


    /**
     * The Pet Tags
     *
     * @return The Tag
     */
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"The state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"The operation has only one path"
        openAPI.paths.size() == 1

        when: "The POST /pets operation is retrieved"
        Operation operation = openAPI.paths?.get("/pets")?.post

        then: "The body has multiple content types"
        operation.requestBody
        operation.requestBody.content.size() == 2
        operation.requestBody.content['application/json'].schema
        operation.requestBody.content['application/x-www-form-urlencoded'].schema
    }

    void "test build OpenAPI for multiple content types and @Parameter"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Saves a Pet
     *
     * @param name The Pet name
     * @param aget The Pet age
     * @return A pet or 404
     */
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Post("/")
    T save(@Parameter(
            name = "name",
            in = ParameterIn.PATH,
            description = "The Pet Name",
            required = true,
            schema = @Schema(type = "string")
        ) String name, int age);
}

class Pet {
    private int age;
    private String name;
    private List<String> tags;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }


    /**
     * The Pet Tags
     *
     * @return The Tag
     */
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"The state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"The operation has only one path"
        openAPI.paths.size() == 1

        when: "The POST /pets operation is retrieved"
        Operation operation = openAPI.paths?.get("/pets")?.post

        then: "The body has multiple content types"
        operation.requestBody
        operation.requestBody.content.size() == 2
        operation.requestBody.content['application/json'].schema
        operation.requestBody.content['application/x-www-form-urlencoded'].schema
    }
}
