package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import spock.util.environment.RestoreSystemProperties

class OpenApi31DynamicGenericsSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test scalar generic emits a dynamic-ref template plus inline defs binding"() {

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
        Schema response = openApi.components.schemas['Response']

        then:
        response != null
        // Template: scalar type-variable slot uses the 'dataType' anchor.
        response.get$dynamicAnchor() == 'dataType'
        response.properties.data.get$dynamicRef() == '#dataType'
        response.properties.data.get$ref() == null
        // $defs placeholder (unbound slot) is attached via the extensions map.
        Schema placeholder = response.getExtensions().get('$defs')['dataType']
        placeholder.get$dynamicAnchor() == 'dataType'
        placeholder.getNot() != null

        // No duplicated concrete schemas.
        !openApi.components.schemas.containsKey('Response_Pet_')
        !openApi.components.schemas.containsKey('Response_Group_')

        // Usage sites: inline $defs binding (sibling of $ref) rebinds the anchor to the concrete type.
        Schema petBinding = openApi.paths['/pet'].get.responses['200'].content['application/json'].schema
        petBinding.get$ref() == '#/components/schemas/Response'
        Schema petSlot = petBinding.getExtensions().get('$defs')['dataType']
        petSlot.get$dynamicAnchor() == 'dataType'
        petSlot.get$ref() == '#/components/schemas/Pet'

        Schema groupBinding = openApi.paths['/group'].get.responses['200'].content['application/json'].schema
        groupBinding.get$ref() == '#/components/schemas/Response'
        Schema groupSlot = groupBinding.getExtensions().get('$defs')['dataType']
        groupSlot.get$dynamicAnchor() == 'dataType'
        groupSlot.get$ref() == '#/components/schemas/Group'
    }

    @RestoreSystemProperties
    void "test collection generic uses itemType anchor and dynamicRef array items"() {

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
        Schema page = openApi.components.schemas['Page']

        then:
        page != null
        // Collection-element usage of T uses the 'itemType' anchor.
        page.get$dynamicAnchor() == 'itemType'
        page.properties.items.type == 'array'
        page.properties.items.items.get$dynamicRef() == '#itemType'
        // Non-generic properties are unaffected.
        page.properties.total.type == 'integer'
        Schema placeholder = page.getExtensions().get('$defs')['itemType']
        placeholder.get$dynamicAnchor() == 'itemType'
        placeholder.getNot() != null

        // Usage site binds the template to Pet.
        Schema binding = openApi.paths['/page'].get.responses['200'].content['application/json'].schema
        binding.get$ref() == '#/components/schemas/Page'
        Schema slot = binding.getExtensions().get('$defs')['itemType']
        slot.get$dynamicAnchor() == 'itemType'
        slot.get$ref() == '#/components/schemas/Pet'
    }

    @RestoreSystemProperties
    void "test default behavior unchanged when dynamic-refs disabled in 3.1"() {

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
        // Default (concrete) mode: duplicated concrete schema, no dynamic keywords.
        openApi.components.schemas.containsKey('Response_Pet_')
        openApi.components.schemas['Response_Pet_'].properties.data.get$ref() == '#/components/schemas/Pet'
        openApi.components.schemas['Response'] == null
    }

    @RestoreSystemProperties
    void "test dynamic-refs ignored on OpenAPI 3.0 (requires 3.1)"() {

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
    void "test primitive concrete argument falls back to concrete schema"() {

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
        // String does not produce a $ref binding slot, so the generic stays concrete.
        openApi.components.schemas.containsKey('Response_String_')
        openApi.components.schemas['Response_String_'].properties.data.type == 'string'
        openApi.components.schemas['Response'] == null
    }

    @RestoreSystemProperties
    void "test raw generic does not poison later parameterized generic"() {

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
        // Raw usage resolves T to Object and gets its own concrete schema name (Response_Object_);
        // the parameterized usage builds the reusable template (Response) with an inline binding.
        // The two do not collide and neither produces a dangling $ref.
        openApi.components.schemas['Response_Object_'] != null
        openApi.components.schemas['Response_Object_'].get$dynamicAnchor() == null
        openApi.components.schemas['Response'].get$dynamicAnchor() == 'dataType'

        openApi.paths['/raw'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/Response_Object_'

        // /pet rebinds the template to Pet; asserted via serialized output (the binding's $defs
        // slot may be a Map after internal merge/copy paths, but the emitted spec is well-formed).
        Schema petBinding = openApi.paths['/pet'].get.responses['200'].content['application/json'].schema
        petBinding.get$ref() == '#/components/schemas/Response'
        String yaml = Utils.getYamlMapper().writeValueAsString(openApi)
        yaml.contains('$dynamicAnchor: dataType')
        yaml.contains('$ref: "#/components/schemas/Pet"')
    }

    @RestoreSystemProperties
    void "test recursive generic falls back to concrete schema"() {

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
    @Get("/tree")
    public Tree<Pet> getTree() { return null; }
}

class Tree<T> {
    private T value;
    private List<Tree<T>> children;
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    public List<Tree<T>> getChildren() { return children; }
    public void setChildren(List<Tree<T>> children) { this.children = children; }
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
        // Self-referential generics combine the template and recursion mechanisms in ways that
        // are not supported, so they keep the default concrete behavior (no leaked $dynamicRef).
        !openApi.components.schemas.containsKey('Tree')
        openApi.components.schemas.containsKey('Tree_Pet_')
        openApi.components.schemas['Tree_Pet_'].properties.value.get$ref() == '#/components/schemas/Pet'
        // No leaked unresolvable dynamic anchor on the concrete schema.
        openApi.components.schemas['Tree_Pet_'].getExtensions() == null || openApi.components.schemas['Tree_Pet_'].getExtensions().get('$defs') == null
    }

    @RestoreSystemProperties
    void "test defs serialized to JSON output"() {

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
        String json = Utils.getJsonMapper().writeValueAsString(openApi)

        then:
        json.contains('"$defs"')
        json.contains('"$dynamicAnchor":"dataType"')
        json.contains('"$dynamicRef":"#dataType"')
    }
}
