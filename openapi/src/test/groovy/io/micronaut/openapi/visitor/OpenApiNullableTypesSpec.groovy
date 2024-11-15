package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue
import spock.lang.PendingFeature
import spock.lang.Unroll

class OpenApiNullableTypesSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI for java.util.Optional"() {
        when:
        buildBeanDefinition('test.PetController', '''
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
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2

        petSchema.properties["age"].type == "integer"
        petSchema.properties["age"].nullable
        petSchema.properties["name"].type == "string"
        petSchema.properties["name"].nullable
    }

    void "test build OpenAPI for nullable fields"() {
        when:
        buildBeanDefinition('test.PetController', '''
package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;

import java.util.List;
import java.util.Optional;

class Pet {
    private String name;
    private Pet mother;

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    @Nullable
    public String getName() {
        return name;
    }

    public void setMother(Pet mother) {
        this.mother = mother;
    }

    /**
     * The Pet Mother
     *
     * @return The Pet Mother
     */
    @Nullable
    public Pet getMother() {
        return mother;
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
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2

        petSchema.properties["name"].nullable
        petSchema.properties["mother"].nullable
    }

    void "test build OpenAPI for nullable fields2"() {
        when:
        buildBeanDefinition('test.PetController', '''
package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

class Pet {
    private String name;
    private Pet mother;

    public void setName(String n) {
        name = n;
    }

    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    @Nullable
    public String getName() {
        return name;
    }

    public void setMother(Pet mother) {
        this.mother = mother;
    }

    /**
     * The Pet Mother
     *
     * @return The Pet Mother
     */
    @Nullable
    public Pet getMother() {
        return mother;
    }
}

@Controller
class PetController {

    @Get("/pet/{name}/{type}")
    HttpResponse<Pet> get(String name, @Nullable String type) {
        return HttpResponse.ok();
    }

    @Post("/pet/{type}")
    HttpResponse<Pet> post(@jakarta.annotation.Nullable String type, Pet p) {
       return HttpResponse.ok();
    }
}

''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation get = openAPI.paths."/pet/{name}/{type}".get
        Operation post = openAPI.paths."/pet/{type}".post
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema.type == 'object'
        petSchema.properties.size() == 2

        petSchema.properties["name"].nullable
        petSchema.properties["mother"].nullable

        get.parameters.get(0).in == 'path'
        get.parameters.get(0).name == 'name'
        get.parameters.get(0).required

        // Path variables always required
        get.parameters.get(1).in == 'path'
        get.parameters.get(1).name == 'type'
        get.parameters.get(1).required

        post.parameters.get(0).in == 'path'
        post.parameters.get(0).name == 'type'
        post.parameters.get(0).required
    }

    @Unroll
    void "test build OpenAPI with Nullable annotations"(String annotation) {
        when:
        buildBeanDefinition('test.PetController', sampleClass(annotation))
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['HelloWorldDto']

        then: "the components are valid"
        schema.type == 'object'
        schema.properties.size() == 1
        schema.properties.nullprop.nullable

        where:
        annotation << [
                'io.micronaut.core.annotation.Nullable',
                'jakarta.annotation.Nullable',
                'javax.annotation.Nullable',
                'androidx.annotation.Nullable',
                'edu.umd.cs.findbugs.annotations.Nullable',
                'io.reactivex.annotations.Nullable',
                'io.reactivex.rxjava3.annotations.Nullable',
                'reactor.util.annotation.Nullable',
        ]
    }

    @Issue("https://github.com/micronaut-projects/micronaut-core/issues/8706")
    @Unroll
    void "test build OpenAPI with eclipse and jspecify Nullable annotations"(String annotation) {
        when:
        buildBeanDefinition('test.PetController', sampleClass(annotation))
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['HelloWorldDto']

        then: "the components are valid"
        schema.type == 'object'
        schema.properties.size() == 1

        schema.properties.nullprop.nullable

        where:
        annotation << [
                'org.eclipse.jdt.annotation.Nullable',
                'org.jspecify.annotations.Nullable',
        ]
    }

    private static String sampleClass(String annotation) {
        """\
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Introspected
class HelloWorldDto {
    @${annotation}
    public String nullprop;
}

@Controller
class HelloWorldController {
    @Get
    public HelloWorldDto helloWorld(@Body HelloWorldDto dto) {
        return dto;
    }
}
"""
    }
}
