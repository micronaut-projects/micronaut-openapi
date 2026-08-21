package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import spock.util.environment.RestoreSystemProperties

/**
 * Compatibility checks against the canonical $dynamicRef / $dynamicAnchor fixture scenarios from
 * the openapi-dynamicref-adoption-tracker (fixtures/*.yaml). The tracker fixtures are hand-authored
 * reference OpenAPI specs; Micronaut does not byte-match them (it uses the Java class name as the
 * template name, omits $id, and registers concrete args as component $refs), but each test here
 * asserts that Micronaut emits the SAME dynamic-refs mechanism for the fixture's Java equivalent.
 *
 * Corresponds to: paginated-response, request-body-binding, generic-schema-binding, api-envelope,
 * recursive-category-tree, and the multi-parameter core of nested-workspace-resources.
 */
class OpenApi31DynamicRefFixtureCompatSpec extends AbstractOpenApiTypeElementSpec {

    private static void enableDynamicRefs() {
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")
    }

    // fixture: paginated-response.yaml — Page<T> with inline route-level binding.
    @RestoreSystemProperties
    void "paginated-response: single-variable template with inline response binding"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/users")
    public Page<User> listUsers() { return null; }

    @Get("/groups")
    public Page<Group> listGroups() { return null; }
}

class Page<T> {
    private List<T> items;
    private int total;
    private int page;
    private int pageSize;
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}

class User {
    private String id;
    private String email;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class Group {
    private String id;
    private String name;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
        page.get$dynamicAnchor() == 'itemType'
        page.properties.items.type == 'array'
        page.properties.items.items.get$dynamicRef() == '#itemType'
        page.properties.total.type == 'integer'

        // Inline route-level bindings (sibling $defs of $ref), one per concrete type.
        Schema users = openApi.paths['/users'].get.responses['200'].content['application/json'].schema
        users.get$ref() == '#/components/schemas/Page'
        users.getExtensions().get('$defs')['itemType']['$dynamicAnchor'] == 'itemType'
        users.getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/User'

        Schema groups = openApi.paths['/groups'].get.responses['200'].content['application/json'].schema
        groups.get$ref() == '#/components/schemas/Page'
        groups.getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/Group'
    }

    // fixture: request-body-binding.yaml — Batch<T> with inline request-body binding.
    @RestoreSystemProperties
    void "request-body-binding: single-variable template with inline request-body binding"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import java.util.List;

@Controller
class Api {
    @Post("/users/batch")
    public void createUsers(@Body Batch<UserCreate> body) { }

    @Post("/groups/batch")
    public void createGroups(@Body Batch<GroupCreate> body) { }
}

class Batch<T> {
    private List<T> items;
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
}

class UserCreate {
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class GroupCreate {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema batch = openApi.components.schemas['Batch']

        then:
        batch != null
        batch.get$dynamicAnchor() == 'itemType'
        batch.properties.items.items.get$dynamicRef() == '#itemType'

        Schema usersBody = openApi.paths['/users/batch'].post.requestBody.content['application/json'].schema
        usersBody.get$ref() == '#/components/schemas/Batch'
        usersBody.getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/UserCreate'

        Schema groupsBody = openApi.paths['/groups/batch'].post.requestBody.content['application/json'].schema
        groupsBody.get$ref() == '#/components/schemas/Batch'
        groupsBody.getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/GroupCreate'
    }

    // fixture: generic-schema-binding.yaml — named pure-specialization subtypes as binding components.
    @RestoreSystemProperties
    void "generic-schema-binding: named subtypes emitted as shared binding components"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/users")
    public PaginatedUserResponse listUsers() { return null; }

    @Get("/groups")
    public PaginatedGroupResponse listGroups() { return null; }
}

class Page<T> {
    private List<T> items;
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
}

class PaginatedUserResponse extends Page<User> {}
class PaginatedGroupResponse extends Page<Group> {}

class User {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

class Group {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference

        then:
        openApi.components.schemas['Page'].get$dynamicAnchor() == 'itemType'

        // Each named subtype is a binding component: $ref to the template + inline $defs rebinding.
        Schema userResp = openApi.components.schemas['PaginatedUserResponse']
        userResp != null
        userResp.get$ref() == '#/components/schemas/Page'
        userResp.getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/User'

        Schema groupResp = openApi.components.schemas['PaginatedGroupResponse']
        groupResp.get$ref() == '#/components/schemas/Page'
        groupResp.getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/Group'

        // Routes reference the shared named components by $ref (no duplicated inline binding).
        openApi.paths['/users'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/PaginatedUserResponse'
        openApi.paths['/groups'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/PaginatedGroupResponse'
    }

    // fixture: api-envelope.yaml — nested parameterized binding Envelope<Page<User>>.
    @RestoreSystemProperties
    void "api-envelope: nested parameterized binding folded into the enclosing slot"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/users/{userId}")
    public Envelope<User> getUser() { return null; }

    @Get("/users")
    public Envelope<Page<User>> listUsers() { return null; }
}

class Envelope<T> {
    private T data;
    private String requestId;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}

class Page<T> {
    private List<T> items;
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
}

class User {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference

        then:
        openApi.components.schemas['Envelope'].get$dynamicAnchor() == 'dataType'
        openApi.components.schemas['Envelope'].properties.data.get$dynamicRef() == '#dataType'
        openApi.components.schemas['Page'].get$dynamicAnchor() == 'itemType'

        // Single-resource route: Envelope<User> binds dataType -> User.
        Schema single = openApi.paths['/users/{userId}'].get.responses['200'].content['application/json'].schema
        single.get$ref() == '#/components/schemas/Envelope'
        single.getExtensions().get('$defs')['dataType']['$ref'] == '#/components/schemas/User'

        // Nested route: Envelope<Page<User>> binds dataType -> Page AND folds the inner Page<User>
        // binding (itemType -> User) into the same slot.
        Schema nested = openApi.paths['/users'].get.responses['200'].content['application/json'].schema
        nested.get$ref() == '#/components/schemas/Envelope'
        Map nestedSlot = nested.getExtensions().get('$defs')['dataType']
        nestedSlot['$ref'] == '#/components/schemas/Page'
        nestedSlot['$defs']['itemType']['$dynamicAnchor'] == 'itemType'
        nestedSlot['$defs']['itemType']['$ref'] == '#/components/schemas/User'
    }

    // fixture: recursive-category-tree.yaml — recursive base + subtype sharing the anchor.
    @RestoreSystemProperties
    void "recursive-category-tree: recursive base and subtype share the dynamic anchor"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/categories/tree")
    public LocalizedCategory getCategoryTree() { return null; }
}

class BaseCategory {
    private String id;
    private List<BaseCategory> children;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<BaseCategory> getChildren() { return children; }
    public void setChildren(List<BaseCategory> children) { this.children = children; }
}

class LocalizedCategory extends BaseCategory {
    private String displayName;
    private String locale;
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema base = openApi.components.schemas['BaseCategory']
        Schema localized = openApi.components.schemas['LocalizedCategory']

        then:
        base != null
        // Anchor name is derived from the base schema name (fixture uses a hand-picked 'category';
        // Micronaut derives 'BaseCategory'). Same dynamic-scope mechanism.
        base.get$dynamicAnchor() == 'BaseCategory'
        base.properties.children.items.get$dynamicRef() == '#BaseCategory'

        localized != null
        // The subtype re-declares the same anchor and composes the base via allOf.
        localized.get$dynamicAnchor() == 'BaseCategory'
        localized.allOf[0].get$ref() == '#/components/schemas/BaseCategory'
        localized.allOf[1].properties.containsKey('displayName')
    }

    // fixture: nested-workspace-resources.yaml — multi-parameter generic template (core mechanism).
    // Models the two-slot template Folder<F, R>; the fixture additionally uses oneOf children and an
    // allOf+extra-field named subtype, which Micronaut does not emit from plain Java types (those
    // are documented as divergences in the coverage report, not asserted here).
    @RestoreSystemProperties
    void "nested-workspace-resources: multi-parameter template with per-variable anchors"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/workspaces/current")
    public Folder<Document, Resource> getWorkspace() { return null; }
}

class Folder<F, R> {
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
        // Multi-variable template: root carries the primary (first) variable's anchor; each variable
        // has its own $defs placeholder. Anchors are derived from the field that uses each variable
        // (children -> childrenType, shortcuts -> shortcutsType).
        folder.get$dynamicAnchor() == 'childrenType'
        folder.getExtensions().get('$defs')['childrenType']['not'] != null
        folder.getExtensions().get('$defs')['shortcutsType']['not'] != null
        folder.properties.children.items.get$dynamicRef() == '#childrenType'
        folder.properties.shortcuts.items.get$dynamicRef() == '#shortcutsType'

        // Usage rebinds both variables inline.
        Schema binding = openApi.paths['/workspaces/current'].get.responses['200'].content['application/json'].schema
        binding.get$ref() == '#/components/schemas/Folder'
        binding.getExtensions().get('$defs')['childrenType']['$ref'] == '#/components/schemas/Document'
        binding.getExtensions().get('$defs')['shortcutsType']['$ref'] == '#/components/schemas/Resource'
    }

    // fixture: inherited-component-binding.yaml — named subtype that adds a field, used at routes.
    // Micronaut composes it as allOf[ binding, own-properties ]; the fixture places the binding at
    // the route instead, but the dynamic-refs mechanism (template + $defs rebinding) is equivalent.
    @RestoreSystemProperties
    void "inherited-component-binding: named subtype with an extra field composes binding + own fields"() {

        setup:
        enableDynamicRefs()

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class Api {
    @Get("/pages/users")
    public DerivedPage listUserPages() { return null; }

    @Get("/pages/groups")
    public DerivedPage listGroupPages() { return null; }
}

class Page<T> {
    private List<T> items;
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
}

class DerivedPage extends Page<User> {
    private int page;
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}

class User {
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema derived = openApi.components.schemas['DerivedPage']

        then:
        derived != null
        // The shared named component composes the generic binding (template $ref + $defs rebinding)
        // with the subtype's own field, so both routes $ref it and the User binding is reused.
        derived.allOf != null
        derived.allOf.size() == 2
        derived.allOf[0].get$ref() == '#/components/schemas/Page'
        derived.allOf[0].getExtensions().get('$defs')['itemType']['$ref'] == '#/components/schemas/User'
        derived.allOf[1].getProperties().containsKey('page')
        !derived.allOf[1].getProperties().containsKey('items')

        openApi.components.schemas['Page'].get$dynamicAnchor() == 'itemType'

        // Both routes reference the shared component; no per-route duplicated binding.
        openApi.paths['/pages/users'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/DerivedPage'
        openApi.paths['/pages/groups'].get.responses['200'].content['application/json'].schema.get$ref() == '#/components/schemas/DerivedPage'
    }
}
