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
    void "test request body generic emits the same template plus inline defs binding"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller
class Api {

    @Post("/pet")
    public void savePet(@Body Response<Pet> body) { }

    @Post("/group")
    public void saveGroup(@Body Response<Group> body) { }
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
        // A @Body generic routes through the same schema-definition path as a return type /
        // field, so the emitted output matches the scalar GET case: one template plus an inline
        // $defs binding on each request body.
        response != null
        response.get$dynamicAnchor() == 'dataType'
        response.properties.data.get$dynamicRef() == '#dataType'
        !openApi.components.schemas.containsKey('Response_Pet_')
        !openApi.components.schemas.containsKey('Response_Group_')

        Schema petBody = openApi.paths['/pet'].post.requestBody.content['application/json'].schema
        petBody.get$ref() == '#/components/schemas/Response'
        Schema petSlot = petBody.getExtensions().get('$defs')['dataType']
        petSlot.get$dynamicAnchor() == 'dataType'
        petSlot.get$ref() == '#/components/schemas/Pet'

        Schema groupBody = openApi.paths['/group'].post.requestBody.content['application/json'].schema
        groupBody.get$ref() == '#/components/schemas/Response'
        Schema groupSlot = groupBody.getExtensions().get('$defs')['dataType']
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
    void "test self-referential generic emits a template with a plain recursive ref"() {

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
        Schema tree = openApi.components.schemas['Tree']

        then:
        // A self-referential generic is emitted as a template like any other generic. The single
        // $dynamicAnchor slot belongs to the type variable (dataType); the self-reference is a
        // plain $ref back to the template, which re-enters it in the same dynamic scope so the
        // type-variable binding stays in effect at every recursion depth.
        tree != null
        tree.get$dynamicAnchor() == 'dataType'
        tree.properties.value.get$dynamicRef() == '#dataType'
        tree.properties.children.type == 'array'
        tree.properties.children.items.get$ref() == '#/components/schemas/Tree'
        tree.properties.children.items.get$dynamicRef() == null
        // The unbound $defs placeholder is present like any generic template. (For a recursive
        // template the $defs entry comes back as a Map through the recursion copy path.)
        tree.getExtensions().get('$defs')['dataType']['not'] != null
        // No duplicated concrete schema.
        !openApi.components.schemas.containsKey('Tree_Pet_')

        // Usage site binds the type variable to Pet inline.
        Schema treeBinding = openApi.paths['/tree'].get.responses['200'].content['application/json'].schema
        treeBinding.get$ref() == '#/components/schemas/Tree'
        treeBinding.getExtensions().get('$defs')['dataType']['$dynamicAnchor'] == 'dataType'
        treeBinding.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/Pet'
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

    @RestoreSystemProperties
    void "test multi-variable generic emits per-variable anchors and inline defs binding"() {

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

    @Get("/pair1")
    public Pair<Pet, Group> getPair1() { return null; }

    @Get("/pair2")
    public Pair<Pet, Owner> getPair2() { return null; }
}

class Pair<K, V> {
    private K key;
    private V value;
    public K getKey() { return key; }
    public void setKey(K key) { this.key = key; }
    public V getValue() { return value; }
    public void setValue(V value) { this.value = value; }
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

class Owner {
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema pair = openApi.components.schemas['Pair']

        then:
        pair != null
        // Multi-variable template: root carries the primary (first) variable's anchor only,
        // because a schema object can hold a single $dynamicAnchor.
        pair.get$dynamicAnchor() == 'K'
        // Each variable has its own $defs placeholder (anchor + not: {}), keyed by the sanitized
        // variable name.
        Schema kPlaceholder = pair.getExtensions().get('$defs')['K']
        kPlaceholder.get$dynamicAnchor() == 'K'
        kPlaceholder.getNot() != null
        Schema vPlaceholder = pair.getExtensions().get('$defs')['V']
        vPlaceholder.get$dynamicAnchor() == 'V'
        vPlaceholder.getNot() != null
        // Field usages resolve to the correct per-variable anchor.
        pair.properties.key.get$dynamicRef() == '#K'
        pair.properties.key.get$ref() == null
        pair.properties.value.get$dynamicRef() == '#V'

        // No duplicated concrete schema per binding.
        !openApi.components.schemas.containsKey('Pair_Pet_Group_')
        !openApi.components.schemas.containsKey('Pair_Pet_Owner_')

        // Each usage site rebinds both anchors inline. (After the response-content merge path the
        // binding's $defs slots are Maps, not Schema objects — same duality the raw-generic test
        // asserts against — so read them with map access.)
        Schema pair1 = openApi.paths['/pair1'].get.responses['200'].content['application/json'].schema
        pair1.get$ref() == '#/components/schemas/Pair'
        pair1.getExtensions().get('$defs')['K']['$dynamicAnchor'] == 'K'
        pair1.getExtensions().get('$defs')['K']['$ref'] == '#/components/schemas/Pet'
        pair1.getExtensions().get('$defs')['V']['$dynamicAnchor'] == 'V'
        pair1.getExtensions().get('$defs')['V']['$ref'] == '#/components/schemas/Group'

        Schema pair2 = openApi.paths['/pair2'].get.responses['200'].content['application/json'].schema
        pair2.get$ref() == '#/components/schemas/Pair'
        pair2.getExtensions().get('$defs')['K']['$ref'] == '#/components/schemas/Pet'
        pair2.getExtensions().get('$defs')['V']['$ref'] == '#/components/schemas/Owner'
    }

    @RestoreSystemProperties
    void "test multi-variable generic with a primitive argument falls back to concrete schema"() {

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
    @Get("/entry")
    public Pair<String, Pet> getEntry() { return null; }
}

class Pair<K, V> {
    private K key;
    private V value;
    public K getKey() { return key; }
    public void setKey(K key) { this.key = key; }
    public V getValue() { return value; }
    public void setValue(V value) { this.value = value; }
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
        // A primitive argument cannot form a $ref binding slot, so the all-or-nothing rule keeps
        // the whole generic on its default concrete behavior.
        openApi.components.schemas.containsKey('Pair_String.Pet_')
        openApi.components.schemas['Pair'] == null
    }

    @RestoreSystemProperties
    void "test named subtype of a parameterized generic emits a shared binding component"() {

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
    public PetResponse getPet() { return null; }

    @Get("/group")
    public GroupResponse getGroup() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class PetResponse extends Response<Pet> {}
class GroupResponse extends Response<Group> {}

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
        // The generic template is built once.
        Schema response = openApi.components.schemas['Response']
        response != null
        response.get$dynamicAnchor() == 'dataType'

        // Each named subtype becomes a binding component that references the template and rebinds
        // the anchor inline, instead of a concrete schema with materialized fields.
        Schema petResponse = openApi.components.schemas['PetResponse']
        petResponse != null
        petResponse.get$ref() == '#/components/schemas/Response'
        petResponse.getExtensions().get('$defs')['dataType']['$dynamicAnchor'] == 'dataType'
        petResponse.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/Pet'

        Schema groupResponse = openApi.components.schemas['GroupResponse']
        groupResponse.get$ref() == '#/components/schemas/Response'
        groupResponse.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/Group'

        // Usages reference the shared named component by $ref (no inline binding duplicated).
        openApi.paths['/pet'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/PetResponse'
        openApi.paths['/group'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/GroupResponse'
    }

    @RestoreSystemProperties
    void "test named subtype with an extra field emits allOf of binding and own properties"() {

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
    public PetResponse getPet() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class PetResponse extends Response<Pet> {
    private String tag;
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
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
        Schema petResponse = openApi.components.schemas['PetResponse']

        then:
        // A subtype that adds its own field is composed as allOf[ binding, own-properties ] so the
        // extra field survives without falling back to a fully concrete schema. The binding branch
        // holds the generic template $ref plus the inline $defs rebinding; the own-properties
        // branch holds only the subtype's declared field(s).
        petResponse != null
        petResponse.allOf != null
        petResponse.allOf.size() == 2

        Schema bindingBranch = petResponse.allOf[0]
        bindingBranch.get$ref() == '#/components/schemas/Response'
        bindingBranch.getExtensions().get('$defs')['dataType']['$dynamicAnchor'] == 'dataType'
        bindingBranch.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/Pet'

        Schema ownBranch = petResponse.allOf[1]
        // type:object on the member is implied by its properties (swagger-core drops the explicit
        // type during allOf post-processing); the contract is that the subtype's own field survives
        // and the inherited generic field is not duplicated here.
        ownBranch.getProperties().containsKey('tag')
        ownBranch.getProperties().get('tag').getType() == 'string'
        !ownBranch.getProperties().containsKey('data')

        // The generic template itself is still emitted once.
        openApi.components.schemas['Response'].get$dynamicAnchor() == 'dataType'

        // The route references the shared named component by $ref.
        openApi.paths['/pet'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/PetResponse'
    }

    @RestoreSystemProperties
    void "test nested parameterized generic folds the inner binding into the outer slot"() {

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
    @Get("/envelope")
    public Envelope<Page<Pet>> getEnvelope() { return null; }
}

class Envelope<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
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
        // Both templates are built once.
        openApi.components.schemas['Envelope'].get$dynamicAnchor() == 'dataType'
        openApi.components.schemas['Page'].get$dynamicAnchor() == 'itemType'

        // The outer binding's data slot points at the Page template AND carries the inner binding's
        // $defs rebinding (itemType -> Pet), so the nested Page<Pet> is fully resolved rather than
        // collapsing to an unbound Page template.
        Schema envelopeBinding = openApi.paths['/envelope'].get.responses['200'].content['application/json'].schema
        envelopeBinding.get$ref() == '#/components/schemas/Envelope'
        Map envelopeDefs = envelopeBinding.getExtensions().get('$defs')
        envelopeDefs['dataType']['$dynamicAnchor'] == 'dataType'
        envelopeDefs['dataType']['$ref'] == '#/components/schemas/Page'
        Map nestedPageDefs = envelopeDefs['dataType']['$defs']
        nestedPageDefs != null
        nestedPageDefs['itemType']['$dynamicAnchor'] == 'itemType'
        nestedPageDefs['itemType']['$ref'] == '#/components/schemas/Pet'
    }

    @RestoreSystemProperties
    void "test concrete-parameterization self-reference keeps its nested binding"() {

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
    @Get("/wrap")
    public Wrapper<Group> getWrap() { return null; }
}

class Wrapper<T> {
    private T data;
    private Wrapper<Pet> petWrapper;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Wrapper<Pet> getPetWrapper() { return petWrapper; }
    public void setPetWrapper(Wrapper<Pet> petWrapper) { this.petWrapper = petWrapper; }
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
        Schema wrapper = openApi.components.schemas['Wrapper']

        then:
        wrapper != null
        wrapper.get$dynamicAnchor() == 'dataType'
        // The unresolved-variable field stays a dynamic-ref consumer.
        wrapper.properties.data.get$dynamicRef() == '#dataType'
        // The concrete-parameterization self-reference (Wrapper<Pet>) keeps its nested binding
        // instead of collapsing to a plain $ref to the unbound template — the petWrapper slot
        // rebinds dataType to Pet.
        Schema petWrapper = wrapper.properties.petWrapper
        petWrapper.get$ref() == '#/components/schemas/Wrapper'
        petWrapper.getExtensions().get('$defs')['dataType']['$dynamicAnchor'] == 'dataType'
        petWrapper.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/Pet'

        // The outer usage binds the template variable to Group.
        Schema wrapBinding = openApi.paths['/wrap'].get.responses['200'].content['application/json'].schema
        wrapBinding.get$ref() == '#/components/schemas/Wrapper'
        wrapBinding.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/Group'
    }

    @RestoreSystemProperties
    void "test named subtype with a getter-only property falls back to concrete"() {

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
    public PetResponse getPet() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

class PetResponse extends Response<Pet> {
    public String getComputed() { return "x"; }
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
        // A subtype that adds a getter-only property (no backing field) is not a pure binding
        // alias — the property would be dropped — so it keeps the default concrete behavior.
        Schema petResponse = openApi.components.schemas['PetResponse']
        petResponse != null
        petResponse.get$ref() == null
        petResponse.getExtensions() == null || petResponse.getExtensions().get('$defs') == null
        // The concrete subtype composes via allOf (top-level properties is null), so verify the
        // computed property actually survives the fallback by checking the serialized output —
        // this is the contract that motivated falling back instead of emitting a binding alias.
        Utils.getYamlMapper().writeValueAsString(petResponse).contains('computed')
    }

    @RestoreSystemProperties
    void "test self-referential named subtype falls back to concrete instead of looping"() {

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
    @Get("/ws")
    public WorkspaceFolder getWorkspace() { return null; }
}

class Folder<F, R> {
    private List<F> children;
    private List<R> shortcuts;
    public List<F> getChildren() { return children; }
    public void setChildren(List<F> children) { this.children = children; }
    public List<R> getShortcuts() { return shortcuts; }
    public void setShortcuts(List<R> shortcuts) { this.shortcuts = shortcuts; }
}

// Self-referential named subtype: appears as its own type argument (Folder<WorkspaceFolder, ...>).
class WorkspaceFolder extends Folder<WorkspaceFolder, Resource> {
    private List<String> permissions;
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}

class Resource {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema ws = openApi.components.schemas['WorkspaceFolder']

        then:
        // A self-referential named subtype cannot be a binding alias (resolving its binding would
        // recurse into resolving itself), so it falls back to the default concrete behavior. The
        // important contract: it terminates (no infinite loop) and emits no leaked dynamic anchor.
        ws != null
        ws.get$ref() == null
        ws.getExtensions() == null || ws.getExtensions().get('$defs') == null
    }

    @RestoreSystemProperties
    void "test inherited @JsonProperty-renamed method stripped from own-branch by resolved key"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import com.fasterxml.jackson.annotation.JsonProperty;

@Controller
class Api {
    @Get("/pet")
    public PetResponse getPet() { return null; }
}

class Response<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    @JsonProperty("checksum")
    public String computeChecksum() { return ""; }
}

class PetResponse extends Response<Pet> {
    private String tag;
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
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
        Schema petResponse = openApi.components.schemas['PetResponse']

        then:
        petResponse != null
        petResponse.allOf != null
        petResponse.allOf.size() == 2
        Schema ownBranch = petResponse.allOf[1]
        // The subtype's own field survives...
        ownBranch.getProperties().containsKey('tag')
        // ...the inherited bean property is filtered upstream...
        !ownBranch.getProperties().containsKey('data')
        // ...and the inherited @JsonProperty-renamed method (emitted as 'checksum', not the method
        // name 'computeChecksum') is stripped so it isn't duplicated with the binding branch.
        // This requires the strip to match the @JsonProperty value, not the raw method name.
        !ownBranch.getProperties().containsKey('checksum')
    }

    @RestoreSystemProperties
    void "test polymorphic array items fold the type variable into oneOf"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Controller
class Api {
    @Get("/ws")
    public Folder<Document, Resource> get() { return null; }
}

class Folder<F, R> {
    @ArraySchema(schema = @Schema(oneOf = { Document.class }))
    private List<F> children;
    private List<R> shortcuts;
    public List<F> getChildren() { return children; }
    public void setChildren(List<F> children) { this.children = children; }
    public List<R> getShortcuts() { return shortcuts; }
    public void setShortcuts(List<R> shortcuts) { this.shortcuts = shortcuts; }
}

class Document {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

class Resource {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema folder = openApi.components.schemas['Folder']

        then:
        folder != null
        Schema children = folder.properties.children
        children.type == 'array'
        Schema items = children.items
        items.oneOf != null
        items.oneOf.size() == 2
        items.oneOf*.get$ref() == ['#/components/schemas/Document', null]
        items.oneOf*.get$dynamicRef() == [null, '#F']
        items.get$dynamicRef() == null

        folder.properties.shortcuts.items.get$dynamicRef() == '#R'

        Schema binding = openApi.paths['/ws'].get.responses['200'].content['application/json'].schema
        binding.get$ref() == '#/components/schemas/Folder'
        binding.getExtensions().get('$defs')['F']['$ref'] == '#/components/schemas/Document'
        binding.getExtensions().get('$defs')['R']['$ref'] == '#/components/schemas/Resource'
    }
}
