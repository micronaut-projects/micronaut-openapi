
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema

class OpenApiInterfaceBeanSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "")
    }

    void "test that interface bean methods are picked up"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import com.fasterxml.jackson.annotation.*;

@Controller("/pets")
interface PetOperations {

    @Post("/")
    Single<Boolean> save(@Body IPet pet);
}

// @JsonDeserialize(as = Pet.class)
interface IPet {
    String getName();
    
    boolean isAlive();
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema petSchema = openAPI.components.schemas['IPet']
        then:
        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['name'].type == 'string'
        petSchema.properties['alive'].type == 'boolean'
        when:

        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.$ref == "#/components/schemas/IPet"
    }
}
