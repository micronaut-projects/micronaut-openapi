package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import spock.util.environment.RestoreSystemProperties

class OpenApi31DynamicGenericsSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test generic type keeps concrete schemas in 3.1 with dynamic-refs enabled"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class Api {

    @Get("/pet")
    public Response<Pet> getPet() { return null; }

    @Get("/group")
    public Response<Group> getGroup() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class Pet {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

class Group {
    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        then:
        // Generic type variables cannot be rebound with $dynamicRef without applying the
        // concrete type schema to the wrapper instance, so dynamic-refs mode keeps the
        // existing concrete generic schemas.
        openApi.components.schemas['Response_Pet_'].properties.data.get$ref() == '#/components/schemas/Pet'
        openApi.components.schemas['Response_Group_'].properties.data.get$ref() == '#/components/schemas/Group'
        openApi.components.schemas['Response'] == null

        Schema petBinding = openApi.paths['/pet'].get.responses['200'].content['application/json'].schema
        petBinding.get$ref() == '#/components/schemas/Response_Pet_'

        Schema groupBinding = openApi.paths['/group'].get.responses['200'].content['application/json'].schema
        groupBinding.get$ref() == '#/components/schemas/Response_Group_'
    }

    @RestoreSystemProperties
    void "test generic type with collection of type variable keeps concrete items"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/page")
    public Page<Pet> getPage() { return null; }
}

class Page<T> {
    private List<T> items;
    private int total;
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

class Pet {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        then:
        Schema page = openApi.components.schemas['Page_Pet_']
        page.properties.items.type == 'array'
        page.properties.items.items.get$ref() == '#/components/schemas/Pet'
        page.properties.total.type == 'integer'
        openApi.components.schemas['Page'] == null

        Schema binding = openApi.paths['/page'].get.responses['200'].content['application/json'].schema
        binding.get$ref() == '#/components/schemas/Page_Pet_'
    }

    @RestoreSystemProperties
    void "test raw generic schema does not affect later parameterized generic schema"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class Api {

    @Get("/raw")
    public Response getRaw() { return null; }

    @Get("/pet")
    public Response<Pet> getPet() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class Pet {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference

        then:
        openApi.components.schemas['Response_Object_'] != null
        openApi.components.schemas['Response_Object_'].get$dynamicAnchor() == null
        openApi.components.schemas['Response_Object_'].properties.data.get$dynamicRef() == null

        openApi.components.schemas['Response_Pet_'].properties.data.get$ref() == '#/components/schemas/Pet'
        openApi.paths['/pet'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/Response_Pet_'
    }

    @RestoreSystemProperties
    void "test generic type default behavior unchanged when dynamic-refs disabled in 3.1"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class Api {
    @Get("/pet")
    public Response<Pet> getPet() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class Pet {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference

        then:
        // Default (concrete) mode: the duplicated concrete schema is produced, no dynamic keywords.
        openApi.components.schemas.containsKey('Response_Pet_')
        openApi.components.schemas['Response_Pet_'].properties.data.get$ref() == '#/components/schemas/Pet'
        openApi.components.schemas['Response_Pet_'].get$dynamicAnchor() == null
        openApi.components.schemas['Response'] == null
    }

    @RestoreSystemProperties
    void "test generic dynamic-refs ignored on OpenAPI 3.0 (requires 3.1)"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class Api {
    @Get("/pet")
    public Response<Pet> getPet() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class Pet {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference

        then:
        openApi.components.schemas.containsKey('Response_Pet_')
        openApi.components.schemas['Response_Pet_'].properties.data.get$ref() == '#/components/schemas/Pet'
        openApi.components.schemas['Response'] == null
    }

    @RestoreSystemProperties
    void "test generic binding falls back to concrete for primitive concrete argument"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class Api {
    @Get("/name")
    public Response<String> getName() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference

        then:
        // String does not produce a $ref binding slot, so the generic stays concrete
        // instead of emitting a broken empty dynamic anchor.
        openApi.components.schemas.containsKey('Response_String_')
        openApi.components.schemas['Response_String_'].properties.data.type == 'string'
        openApi.components.schemas['Response'] == null
    }
}
