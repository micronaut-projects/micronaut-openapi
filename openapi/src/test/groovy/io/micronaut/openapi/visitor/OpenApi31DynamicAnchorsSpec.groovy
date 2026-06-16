package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import spock.util.environment.RestoreSystemProperties

class OpenApi31DynamicAnchorsSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test recursive type emits dynamicAnchor and dynamicRef in 3.1 with dynamic-refs enabled"() {

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
class CategoryApi {

    @Get("/localized")
    public LocalizedCategory getLocalized() { return null; }

    @Get("/base")
    public BaseCategory getBase() { return null; }
}

class BaseCategory {
    private List<BaseCategory> children;
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
        Schema baseCategory = openApi.components.schemas['BaseCategory']
        Schema localizedCategory = openApi.components.schemas['LocalizedCategory']

        then:
        baseCategory != null
        localizedCategory != null

        // The recursive base schema gets a dynamic anchor named after itself.
        baseCategory.get$dynamicAnchor() == 'BaseCategory'

        // The self-referential property emits a $dynamicRef consumer instead of a $ref.
        Schema children = baseCategory.properties.children
        children.type == 'array'
        children.items.get$dynamicRef() == '#BaseCategory'
        children.items.get$ref() == null

        // The subtype re-declares the same anchor (so dynamic scope switches to it) and
        // still references the base through a normal $ref inside allOf.
        localizedCategory.get$dynamicAnchor() == 'BaseCategory'
        localizedCategory.allOf[0].get$ref() == '#/components/schemas/BaseCategory'
        localizedCategory.allOf[1].properties.containsKey('displayName')
        localizedCategory.allOf[1].properties.containsKey('locale')
    }

    @RestoreSystemProperties
    void "test subtype self-recursion falls back to ref when inherited anchor is different"() {

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
class CategoryApi {
    @Get("/localized")
    public LocalizedCategory getLocalized() { return null; }
}

class BaseCategory {
    private List<BaseCategory> children;
    public List<BaseCategory> getChildren() { return children; }
    public void setChildren(List<BaseCategory> children) { this.children = children; }
}

class LocalizedCategory extends BaseCategory {
    private List<LocalizedCategory> localizedChildren;
    public List<LocalizedCategory> getLocalizedChildren() { return localizedChildren; }
    public void setLocalizedChildren(List<LocalizedCategory> localizedChildren) { this.localizedChildren = localizedChildren; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema localizedCategory = openApi.components.schemas['LocalizedCategory']
        Schema localizedChildren = localizedCategory.allOf[1].properties.localizedChildren

        then:
        localizedCategory.get$dynamicAnchor() == 'BaseCategory'
        localizedChildren.type == 'array'
        localizedChildren.items.get$dynamicRef() == null
        localizedChildren.items.get$ref() == '#/components/schemas/LocalizedCategory'
    }

    @RestoreSystemProperties
    void "test recursive schema with non-anchor schema name emits sanitized dynamicAnchor"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_31_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Controller
class CategoryApi {
    @Get("/category")
    public Category getCategory() { return null; }
}

@Schema(name = "2024 v1:Category")
class Category {
    private List<Category> children;
    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema category = openApi.components.schemas['2024 v1:Category']

        then:
        category != null
        category.get$dynamicAnchor() == '_2024_v1_Category'
        category.properties.children.type == 'array'
        category.properties.children.items.get$dynamicRef() == '#_2024_v1_Category'
    }

    @RestoreSystemProperties
    void "test lone self-referential type emits dynamicAnchor and dynamicRef"() {

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
class NodeApi {
    @Get("/node")
    public Node getNode() { return null; }
}

class Node {
    private List<Node> children;
    public List<Node> getChildren() { return children; }
    public void setChildren(List<Node> children) { this.children = children; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema node = openApi.components.schemas['Node']

        then:
        node != null
        node.get$dynamicAnchor() == 'Node'
        node.properties.children.type == 'array'
        node.properties.children.items.get$dynamicRef() == '#Node'
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
import java.util.List;

@Controller
class NodeApi {
    @Get("/node")
    public Node getNode() { return null; }
}

class Node {
    private List<Node> children;
    public List<Node> getChildren() { return children; }
    public void setChildren(List<Node> children) { this.children = children; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema node = openApi.components.schemas['Node']

        then:
        node != null
        // Default (concrete) mode: no dynamic keywords, normal self $ref.
        node.get$dynamicAnchor() == null
        node.properties.children.type == 'array'
        node.properties.children.items.get$dynamicRef() == null
        node.properties.children.items.get$ref() == '#/components/schemas/Node'
    }

    @RestoreSystemProperties
    void "test dynamic-refs ignored on OpenAPI 3.0 (requires 3.1)"() {

        setup:
        // 3.1 NOT enabled (default 3.0), but dynamic-refs flag set: must fall back to concrete.
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_DYNAMIC_REFS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller
class NodeApi {
    @Get("/node")
    public Node getNode() { return null; }
}

class Node {
    private List<Node> children;
    public List<Node> getChildren() { return children; }
    public void setChildren(List<Node> children) { this.children = children; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        Schema node = openApi.components.schemas['Node']

        then:
        node != null
        node.get$dynamicAnchor() == null
        node.properties.children.type == 'array'
        node.properties.children.items.get$dynamicRef() == null
        node.properties.children.items.get$ref() == '#/components/schemas/Node'
    }

    @RestoreSystemProperties
    void "test dynamicAnchor and dynamicRef are serialized to YAML in 3.1"() {

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
class CategoryApi {
    @Get("/localized")
    public LocalizedCategory getLocalized() { return null; }
}

class BaseCategory {
    private List<BaseCategory> children;
    public List<BaseCategory> getChildren() { return children; }
    public void setChildren(List<BaseCategory> children) { this.children = children; }
}

class LocalizedCategory extends BaseCategory {
    private String displayName;
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openApi = Utils.testReference
        String yaml = Utils.getYamlMapper().writeValueAsString(openApi)

        then:
        yaml.contains('$dynamicAnchor: BaseCategory')
        yaml.contains('$dynamicRef')
        yaml.contains('#BaseCategory')
    }
}
