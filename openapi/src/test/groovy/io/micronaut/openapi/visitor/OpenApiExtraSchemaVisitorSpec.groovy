package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import spock.lang.PendingFeature
import spock.util.environment.RestoreSystemProperties

class OpenApiExtraSchemaVisitorSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI with extra schemas"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.openapi.annotation.OpenAPIExtraSchema;
import jakarta.inject.Singleton;

@OpenAPIExtraSchema(
    classes = UnusedModel1.class,
    excludeClasses = ExcludedModel.class
)
class Application {
}

class MyModel {
    
    public String prop1;
}

class UnusedModel1 {
    
    public String field1;
}

@OpenAPIExtraSchema
class UnusedModelWithAnn {
    
    public String field1;
}

@OpenAPIExtraSchema
class UnusedComplexModel {
    
    public String field1;
    public InternalClass field2;
}

class InternalClass {
    
    public String prop1;
}

@OpenAPIExtraSchema
class ExcludedModel {
    
    public String field1;
    public InternalClass field2;
}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.components
        openApi.components.schemas
        openApi.components.schemas.size() == 4

        !openApi.components.schemas.MyModel

        openApi.components.schemas.UnusedModel1
        openApi.components.schemas.UnusedModel1.properties.field1

        openApi.components.schemas.UnusedModelWithAnn
        openApi.components.schemas.UnusedModelWithAnn.properties.field1

        openApi.components.schemas.UnusedComplexModel
        openApi.components.schemas.UnusedComplexModel.properties.field1
        openApi.components.schemas.UnusedComplexModel.properties.field2

        openApi.components.schemas.InternalClass
        openApi.components.schemas.InternalClass.properties.prop1

        !openApi.components.schemas.ExcludedModel
    }

    void "test build OpenAPI with excluded extra schemas"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.openapi.annotation.OpenAPIExtraSchema;
import jakarta.inject.Singleton;

@OpenAPIExtraSchema(
    classes = UnusedModel1.class,
    excludeClasses = ExcludedModel.class,
    excludePackages = "test",
    packages = "io.micronaut.openapi.extra"
)
class Application {
}

class MyModel {
    
    public String prop1;
}

class UnusedModel1 {
    
    public String field1;
}

@OpenAPIExtraSchema
class UnusedModelWithAnn {
    
    public String field1;
}

@OpenAPIExtraSchema
class UnusedComplexModel {
    
    public String field1;
    public InternalClass field2;
}

class InternalClass {
    
    public String prop1;
}

@OpenAPIExtraSchema
class ExcludedModel {
    
    public String field1;
    public InternalClass field2;
}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.components
        openApi.components.schemas
        openApi.components.schemas.size() == 3

        openApi.components.schemas.ExtraModel1
        openApi.components.schemas.ExtraModel1.properties.prop1
        openApi.components.schemas.ExtraModel1.properties.ddd

        openApi.components.schemas.ExtraModel2
        openApi.components.schemas.ExtraModel2.properties.field1

        openApi.components.schemas."ExtraModel1.InternalClass"
        openApi.components.schemas."ExtraModel1.InternalClass".properties.prop2
    }

    @PendingFeature(reason = "Currently, we can't use wildcard to get all subpackages")
    void "test build OpenAPI with extra schemas in subpackages"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.openapi.annotation.OpenAPIExtraSchema;
import jakarta.inject.Singleton;

@OpenAPIExtraSchema(packages = "io.micronaut.openapi.extra.*")
class Application {
}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.components
        openApi.components.schemas
        openApi.components.schemas.size() == 4

        openApi.components.schemas.ExtraModel1
        openApi.components.schemas.ExtraModel1.properties.prop1
        openApi.components.schemas.ExtraModel1.properties.ddd

        openApi.components.schemas.ExtraModel2
        openApi.components.schemas.ExtraModel2.properties.field1

        openApi.components.schemas."ExtraModel1.InternalClass"
        openApi.components.schemas."ExtraModel1.InternalClass".properties.prop2

        openApi.components.schemas.ExtraModelInternalPackage
        openApi.components.schemas.ExtraModelInternalPackage.properties.prop1
    }

    @RestoreSystemProperties
    void "test disable OpenAPI extra schemas"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_EXTRA_ENABLED, "false")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIExtraSchema;
import jakarta.inject.Singleton;

@OpenAPIExtraSchema(
    classes = UnusedModel1.class
)
class Application {
}

@Controller
class MyController {
    
    @Get
    public void getProp() {
    }
}

class MyModel {
    
    public String prop1;
}

class UnusedModel1 {
    
    public String field1;
}

@OpenAPIExtraSchema
class UnusedModelWithAnn {
    
    public String field1;
}

@OpenAPIExtraSchema
class UnusedComplexModel {
    
    public String field1;
    public InternalClass field2;
}

class InternalClass {
    
    public String prop1;
}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        !openApi.components
    }
}
