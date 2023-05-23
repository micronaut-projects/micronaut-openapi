package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema

class OpenApiPublicFieldsSpec extends AbstractOpenApiTypeElementSpec {

    void "test that public fields can also be used to define schema"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {
    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 1
        petSchema.properties['age'].type == 'integer'
        !petSchema.required

        when: "the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets")

        then: "it is included in the OpenAPI doc"
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

    void "test private visibility level"() {

        given: "An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, VisibilityLevel.PRIVATE.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 6
        petSchema.properties.privateAge.type == 'integer'
        petSchema.properties.protectedAge.type == 'integer'
        petSchema.properties.packageAge.type == 'integer'
        petSchema.properties.age.type == 'integer'
        petSchema.properties.name.type == 'string'
        petSchema.properties.protectme.type == 'string'
        !petSchema.required

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL)
    }

    void "test package-private visibility level"() {

        given: "An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, VisibilityLevel.PACKAGE.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 4
        petSchema.properties.protectedAge.type == 'integer'
        petSchema.properties.packageAge.type == 'integer'
        petSchema.properties.age.type == 'integer'
        petSchema.properties.protectme.type == 'string'
        !petSchema.required

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL)
    }

    void "test protected visibility level"() {

        given: "An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, VisibilityLevel.PROTECTED.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 3
        petSchema.properties.protectedAge.type == 'integer'
        petSchema.properties.age.type == 'integer'
        petSchema.properties.protectme.type == 'string'
        !petSchema.required

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL)
    }

    void "test public visibility level"() {

        given: "An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, VisibilityLevel.PUBLIC.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 1
        petSchema.properties.age.type == 'integer'
        !petSchema.required

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL)
    }

    void "test private visibility level with getters"() {

        given: "An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL, VisibilityLevel.PRIVATE.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;

    int getPrivateAge() {
        return privateAge;
    }

    void setPrivateAge(int privateAge) {
        this.privateAge = privateAge;
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

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 6
        petSchema.properties.privateAge.type == 'integer'
        petSchema.properties.protectedAge.type == 'integer'
        petSchema.properties.packageAge.type == 'integer'
        petSchema.properties.age.type == 'integer'
        petSchema.properties.name.type == 'string'
        petSchema.properties.protectme.type == 'string'
        !petSchema.required

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FIELD_VISIBILITY_LEVEL)
    }
}
