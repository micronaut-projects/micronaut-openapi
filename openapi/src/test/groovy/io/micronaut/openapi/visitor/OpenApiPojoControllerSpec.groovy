package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiPojoControllerSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI for List"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean','''
package test;

import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;

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

    @ApiResponse(
            responseCode = "201", description = "Person created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class)))
    @ApiResponse(responseCode = "400", description = "Invalid Name Supplied")
    @ApiResponse(responseCode = "404", description = "Person not found")
    @Post("/xyz1")
    T xyzPost(String slug);
    
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Custom desc"),
        @ApiResponse(responseCode = "400", description = "Invalid Name Supplied"),
        @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @Put("/xyz1")
    T xyzPut(String slug);
    
    @Status(io.micronaut.http.HttpStatus.CREATED)
    @Operation(description = "Do the post")
    @ApiResponse(responseCode = "201", description = "Custom desc 2")
    @ApiResponse(responseCode = "400", description = "Invalid Name Supplied")
    @ApiResponse(responseCode = "404", description = "Person not found")
    @Post("/xyz2")
    T xyzPost2(String slug);
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

@jakarta.inject.Singleton
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

        when:
            PathItem xyz1 = openAPI.paths.get("/pets/xyz1")
            PathItem xyz2 = openAPI.paths.get("/pets/xyz2")

        then:
            xyz1.post.operationId == 'xyzPost'
            xyz1.post.responses.size() == 3
            xyz1.post.responses['201']
            xyz1.post.responses['201'].description == 'Person created'
            xyz1.post.responses['201'].content['application/json'].schema
            xyz1.post.responses['201'].content['application/json'].schema.$ref == '#/components/schemas/Pet'
            xyz1.post.responses['400']
            xyz1.post.responses['400'].description == 'Invalid Name Supplied'
            xyz1.post.responses['400'].content == null
            xyz1.post.responses['404']
            xyz1.post.responses['404'].description == 'Person not found'
            xyz1.post.responses['404'].content == null

            xyz1.put.operationId == 'xyzPut'
            xyz1.put.description == null
            xyz1.put.responses.size() == 3
            xyz1.put.responses['200']
            xyz1.put.responses['200'].description == 'Custom desc'
            xyz1.put.responses['400']
            xyz1.put.responses['400'].description == 'Invalid Name Supplied'
            xyz1.put.responses['400'].content == null
            xyz1.put.responses['404']
            xyz1.put.responses['404'].description == 'Person not found'
            xyz1.put.responses['404'].content == null

            xyz2.post.operationId == 'xyzPost2'
            xyz2.post.description == "Do the post"
            xyz2.post.responses.size() == 3
            xyz2.post.responses['201']
            xyz2.post.responses['201'].description == 'Custom desc 2'
            xyz2.post.responses['400']
            xyz2.post.responses['400'].description == 'Invalid Name Supplied'
            xyz2.post.responses['400'].content == null
            xyz2.post.responses['404']
            xyz2.post.responses['404'].description == 'Person not found'
            xyz2.post.responses['404'].content == null
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
    private Map<String, List<Tag>> tagArrays;

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

    /**
     * A string-to-array dictionary
     *
     * @return A string-to-array dictionary
     */
    public Map<String, List<Tag>> getTagArrays() {
        return tagArrays;
    }

    public void setTagArrays(Map<String, List<Tag>> tagArrays) {
        this.tagArrays = tagArrays;
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

@jakarta.inject.Singleton
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
        petSchema.properties.size() == 6

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

        ((MapSchema) petSchema.properties['tagArrays']).type == "object"
        ((MapSchema) petSchema.properties['tagArrays']).description == "A string-to-array dictionary"
        ((ArraySchema)((MapSchema) petSchema.properties['tagArrays']).getAdditionalProperties()).getType() == "array"
        ((ArraySchema)((MapSchema) petSchema.properties['tagArrays']).getAdditionalProperties()).getItems().$ref == "#/components/schemas/Tag"
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

@jakarta.inject.Singleton
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

@jakarta.inject.Singleton
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
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['200'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'
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
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Pet'

    }

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

@jakarta.inject.Singleton
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
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['200'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'
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
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Pet'

        when:"A flowable is returned"
        pathItem = openAPI.paths.get("/pets/flowable")

        then:
        pathItem.get.operationId == 'flowable'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['200'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'

        when:"A completable is returned"
        pathItem = openAPI.paths.get("/pets/completable")

        then:
        pathItem.post.operationId == 'completable'
        pathItem.post.responses['200']
        pathItem.post.responses['200'].description == 'completable 200 response'
        pathItem.post.responses['200'].content == null


        when:"An obsevable is returned"
        pathItem = openAPI.paths.get("/pets/observable")

        then:
        pathItem.get.operationId == 'observable'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['200'].content['application/json'].schema.items.$ref == '#/components/schemas/Pet'

        when:"A Single<HttpResponse<T>> is returned"
        pathItem = openAPI.paths.get("/pets/singleHttpResponse")

        then:
        pathItem.get.operationId == 'singleHttpResponse'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Pet'
    }

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

@jakarta.inject.Singleton
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
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['200'].content['application/json'].schema.items.$ref == '#/components/schemas/MyPet'
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
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/MyPet'

        when:"A flowable is returned"
        pathItem = openAPI.paths.get("/pets/flowable")

        then:
        pathItem.get.operationId == 'flowable'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.get.responses['200'].content['application/json'].schema.items.$ref == '#/components/schemas/MyPet'
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

@jakarta.inject.Singleton
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

@jakarta.inject.Singleton
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
import io.micronaut.http.annotation.Status;

import java.util.List;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Status(io.micronaut.http.HttpStatus.ALREADY_IMPORTED)
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

@jakarta.inject.Singleton
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
        operation.responses."208".content.size() == 2
        operation.responses."208".content['application/json'].schema
        operation.responses."208".content['application/x-www-form-urlencoded'].schema
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

@jakarta.inject.Singleton
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

@jakarta.inject.Singleton
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
     * @param age The Pet age
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

@jakarta.inject.Singleton
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
     * @param age The Pet age
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

@jakarta.inject.Singleton
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

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/490")
    void "test build OpenAPI for Controller with POJO with mandatory/optional fields"() {
        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.media.*;

import java.util.List;

@Controller("/example")
class ExampleController {

    @Get("/")
    ExampleData getExampleData() {
        return new ExampleData("name", true, new ExampleAdditionalData("hello"), 2, 0.456f, 1.2F);
    }
}

class ExampleData {
    private String name;
    @Schema(required = false)
    private Boolean active;
    private ExampleAdditionalData additionalData;
    private Integer age;
    private Float battingAverage;
    private float anotherFloat;

    ExampleData(String name, Boolean active, ExampleAdditionalData additionalData, Integer age, @Nullable Float battingAverage, float anotherFloat) {
        this.name = name;
        this.active = active;
        this.additionalData = additionalData;
        this.age = age;
        this.battingAverage = battingAverage;
        this.anotherFloat = anotherFloat;
    }

    public String getName() {
        return this.name;
    }

    public Boolean isActive() {
        return this.active;
    }

    public ExampleAdditionalData getAdditionalData() {
        return this.additionalData;
    }

    public Integer getAge() {
        return this.age;
    }

    public Float getBattingAverage() {
        return this.battingAverage;
    }

    public float getAnotherFloat() {
        return this.anotherFloat;
    }
}

class ExampleAdditionalData {
    private String something;

    ExampleAdditionalData(String something) {
        this.something = something;
    }

    String getSomething() {
        return this.something;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema schema = openAPI.components.schemas['ExampleData']

        then: "the components are valid"
        schema.properties.size() == 6
        schema.type == 'object'
        schema.required

        and: 'all params without annotations are required'
        schema.required.contains('name')
        schema.required.contains('additionalData')
        schema.required.contains('age')
        schema.required.contains('anotherFloat')

        and: 'active is not required because it is annotated with @Schema(required = false)'
        !schema.required.contains('active')

        and: 'battingAverage is not required because it is annotated with @Nullable in the constructor'
        !schema.required.contains('battingAverage')
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/587")
    void "test @Schema on fields take precedence on constructors for mandatory/optional params"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import io.swagger.v3.oas.annotations.media.*;

@Controller
class FooController {

    @Get("/person")
    public Person getPerson() {
        return new Person("John", 42, new Address("SomeCity", "SomeCountry", "12345", "my street"));
    }
}

class Address {
    @Schema(description = "city")
    private final String city;

    @Schema(description = "country", required = false)
    private final String country;

    @Schema(description = "zip", required = true)
    private final String zip;

    private final String street;

    public Address(String city, String country, String zip, String street) {
        this.city = city;
        this.country = country;
        this.zip = zip;
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getZip() {
        return zip;
    }

    public String getStreet() {
        return street;
    }
}

class Person {
    @Schema(description = "name of the person")
    private final String name;

    @Schema(description = "age of the person", required = false)
    private final int age;

    @Schema(description = "address of the person", implementation = Address.class)
    private final Address address;

    public Person(String name, int age, Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Address getAddress() {
        return address;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas.size() == 2

        when:
        Schema address = openAPI.components.schemas['Address']

        then: 'zip and street are required and city and country are not required because of @Schema(required = false)'
        address.required
        address.required.size() == 2
        address.required.contains('zip')
        address.required.contains('street')
        address.properties.size() == 4

        when:
        Schema person = openAPI.components.schemas['Person']

        then: 'no required properties because @Schema(required = false)'
        !person.required
        person.properties.size() == 3
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/548")
    void "test build OpenAPI for Controller with POJO with UUID fields"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import javax.validation.Valid;

@Controller("/")
class UuidController {

    @Post("/uuid")
    public void sendGreeting(@Valid @Body Greeting greeting) {
    }
}

@Schema(description = "Represent a greeting between a sender and a receiver")
class Greeting {

    @Schema(description = "Greeting message the receiver will get")
    private String message;

    @Schema(description = "ID of the sender")
    private UUID senderId;

    private UUID receiverId;

    public Greeting() {
    }

    public Greeting(String message, UUID senderId, UUID receiverId) {
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getSenderId() {
        return senderId;
    }
    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    @Schema(description = "ID of the receiver")
    public UUID getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['Greeting'].name == 'Greeting'
        openAPI.components.schemas['Greeting'].description == 'Represent a greeting between a sender and a receiver'
        openAPI.components.schemas['Greeting'].type == 'object'
        openAPI.components.schemas['Greeting'].properties.size() == 3
        openAPI.components.schemas['Greeting'].properties['message'].type == 'string'
        openAPI.components.schemas['Greeting'].properties['message'].description == 'Greeting message the receiver will get'
        openAPI.components.schemas['Greeting'].properties['senderId'].type == 'string'
        openAPI.components.schemas['Greeting'].properties['senderId'].description == 'ID of the sender'
        openAPI.components.schemas['Greeting'].properties['senderId'].format == 'uuid'
        openAPI.components.schemas['Greeting'].properties['receiverId'].type == 'string'
        openAPI.components.schemas['Greeting'].properties['receiverId'].description == 'ID of the receiver'
        openAPI.components.schemas['Greeting'].properties['receiverId'].format == 'uuid'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/555")
    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/564")
    void "test build OpenAPI for Controller with POJO with BigDecimal field"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Controller("/")
class UuidController {

    @Post("/big-decimal")
    public void sendGreeting(@Body MyDTO myDto) {
    }
}

class MyDTO {

    @Schema(description = "Should become a number in the spec")
    private final BigDecimal shouldBeNumber;

    @Schema(type = "string")
    private final BigDecimal shouldBeString;

    public MyDTO(BigDecimal shouldBeNumber, BigDecimal shouldBeString) {
        this.shouldBeNumber = shouldBeNumber;
        this.shouldBeString = shouldBeString;
    }

    public BigDecimal getShouldBeNumber() {
        return shouldBeNumber;
    }

    public BigDecimal getShouldBeString() {
        return shouldBeString;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['MyDTO'].name == 'MyDTO'
        openAPI.components.schemas['MyDTO'].type == 'object'
        openAPI.components.schemas['MyDTO'].properties.size() == 2
        openAPI.components.schemas['MyDTO'].properties['shouldBeNumber'].type == 'number'
        openAPI.components.schemas['MyDTO'].properties['shouldBeNumber'].description == 'Should become a number in the spec'
        openAPI.components.schemas['MyDTO'].properties['shouldBeString'].type == 'string'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/402")
    void "test build OpenAPI for Controller with Path parameter"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

@Controller
class UploadController {

    @Post("/upload/{id}")
    void upload(String id) {
    }

    @Post("/upload2/{id}")
    void upload2(@Parameter(in = ParameterIn.PATH) String id) {
    }

    @Post("/upload3/{id}")
    void upload3(String id, @Body User user) {
    }

}

class User {
    private final String name;
    private final String lastName;

    public User(String name, String lastName) {
        this.name = name;
        this.lastName = lastName;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.paths.size() == 3
        openAPI.paths.get("/upload/{id}")
        openAPI.paths.get("/upload/{id}").post
        !openAPI.paths.get("/upload/{id}").post.requestBody
        openAPI.paths.get("/upload/{id}").post.parameters.size() == 1
        openAPI.paths.get("/upload/{id}").post.parameters[0].in == "path"
        openAPI.paths.get("/upload/{id}").post.parameters[0].name == "id"
        openAPI.paths.get("/upload/{id}").post.parameters[0].required

        and:
        openAPI.paths.get("/upload2/{id}")
        openAPI.paths.get("/upload2/{id}").post
        !openAPI.paths.get("/upload2/{id}").post.requestBody
        openAPI.paths.get("/upload2/{id}").post.parameters.size() == 1
        openAPI.paths.get("/upload2/{id}").post.parameters[0].in == "path"
        openAPI.paths.get("/upload2/{id}").post.parameters[0].name == "id"
        openAPI.paths.get("/upload2/{id}").post.parameters[0].required

        and:
        openAPI.paths.get("/upload3/{id}")
        openAPI.paths.get("/upload3/{id}").post
        openAPI.paths.get("/upload3/{id}").post.requestBody
        openAPI.paths.get("/upload3/{id}").post.requestBody.required
        openAPI.paths.get("/upload3/{id}").post.requestBody.content
        openAPI.paths.get("/upload3/{id}").post.requestBody.content.size() == 1
        openAPI.paths.get("/upload3/{id}").post.requestBody.content['application/json'].schema
        openAPI.paths.get("/upload3/{id}").post.requestBody.content['application/json'].schema.$ref == '#/components/schemas/User'
        openAPI.paths.get("/upload3/{id}").post.parameters.size() == 1
        openAPI.paths.get("/upload3/{id}").post.parameters[0].in == "path"
        openAPI.paths.get("/upload3/{id}").post.parameters[0].name == "id"
        openAPI.paths.get("/upload3/{id}").post.parameters[0].required
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/611")
    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/632")
    void "test @Schema(nullable = false, required = true) takes priority for Kotlin data classes"() {
        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;
import io.micronaut.validation.Validated;
import io.micronaut.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.*;

import javax.validation.Valid;
import javax.validation.constraints.*;

import java.util.List;

@Controller(value = "/v1/customers")
@Validated
class CustomersController {

    @org.jetbrains.annotations.NotNull()
    @Status(value = HttpStatus.CREATED)
    @Post
    public String createCustomer(@org.jetbrains.annotations.NotNull() @Body @Valid() CreateCustomerRequest request) {
        return null;
    }
}

@io.micronaut.core.annotation.Introspected()
class CreateCustomerRequest {

    @org.jetbrains.annotations.Nullable()
    @javax.validation.constraints.NotNull()
    @io.swagger.v3.oas.annotations.media.Schema(nullable = false, required = true)
    private final java.lang.String customerName = null;

    @org.jetbrains.annotations.Nullable()
    @javax.validation.constraints.PastOrPresent()
    @javax.validation.constraints.NotNull()
    @io.swagger.v3.oas.annotations.media.Schema(nullable = false, required = true)
    private final java.time.LocalDate birthDate = null;

    public CreateCustomerRequest(
            @org.jetbrains.annotations.Nullable() java.lang.String customerName,
            @org.jetbrains.annotations.Nullable() java.time.LocalDate birthDate) {
        super();
    }

    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCustomerName() {
        return null;
    }

    @org.jetbrains.annotations.Nullable()
    public final java.time.LocalDate getBirthDate() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema schema = openAPI.components.schemas['CreateCustomerRequest']

        then: "the components are valid"
        schema.properties.size() == 2
        schema.type == 'object'
        schema.required

        and:
        schema.required.contains('customerName')
        schema.required.contains('birthDate')

        !schema.properties['customerName'].nullable
        !schema.properties['birthDate'].nullable
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/617")
    void "@ApiResponse takes priority over default Http status code"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import java.net.URI;

@Controller
class RootController {

    @Operation(operationId = "home")
    @ApiResponse(responseCode = "303", description = "redirects to /hello/world")
    @Get
    HttpResponse<?> index() {
        return HttpResponse.seeOther(URI.create("/hello/world"));
    }
}

@Controller("/hello")
class HelloWorldController {
    @Get(value = "/world", produces = MediaType.TEXT_PLAIN)
    String index() {
        return "Hello World";
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then: 'only 303 response because @ApiResponse is used.'
        openAPI.paths.size() == 2
        PathItem root = openAPI.paths.get('/')
        root.get.operationId == 'home'
        root.get.responses.size() == 1
        root.get.responses['303']
        root.get.responses['303'].description == 'redirects to /hello/world'

        and: 'no content because it is not defined in @ApiResponse'
        root.get.responses['303'].content == null

        and:
        PathItem helloWorld = openAPI.paths.get('/hello/world')
        helloWorld.get.operationId == 'index'
        helloWorld.get.responses.size() == 1
        helloWorld.get.responses['200']
        helloWorld.get.responses['200'].description == 'index 200 response'
        helloWorld.get.responses['200'].content['text/plain'].schema
        helloWorld.get.responses['200'].content['text/plain'].schema.type == 'string'
    }
}
