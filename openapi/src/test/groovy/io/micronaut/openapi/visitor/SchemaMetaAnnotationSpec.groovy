
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.UUIDSchema
import spock.lang.Specification

class SchemaMetaAnnotationSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "")
    }

    void "test default UUID handling"() {


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
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface MyOps {

    @Post("/")
    String save(java.util.UUID uuid);
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'savePost'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.type == 'object'
        pathItem.post.requestBody.content['application/json'].schema.properties.size() == 1
        pathItem.post.requestBody.content['application/json'].schema.properties['uuid']
        pathItem.post.requestBody.content['application/json'].schema.properties['uuid'] instanceof UUIDSchema
        pathItem.post.requestBody.content['application/json'].schema.properties['uuid'].type == 'string'
        pathItem.post.requestBody.content['application/json'].schema.properties['uuid'].format == 'uuid'
    }

    void "test that it possible to define custom schema annotations at the type level"() {


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
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface MyOps {

    @Post("/")
    String save(@MyAnn java.util.UUID uuid);
}

@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Schema(type="string", format="uuid")
@interface MyAnn {

}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'savePost'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.type == 'object'
        pathItem.post.requestBody.content['application/json'].schema.properties.size() == 1
        pathItem.post.requestBody.content['application/json'].schema.properties['uuid']
        pathItem.post.requestBody.content['application/json'].schema.properties['uuid'].$ref  == '#/components/schemas/MyAnn'

        openAPI.components.schemas['MyAnn']
        openAPI.components.schemas['MyAnn'].type == 'string'
        openAPI.components.schemas['MyAnn'].format == 'uuid'
    }


    void "test that it possible to define custom RequestBody annotations at the type level"() {


        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface MyOps {

    @Post("/")
    String save(@MyAnn java.util.UUID uuid);
}

@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@RequestBody(description = "A pet", required = false, content = {
        @Content(mediaType = "application/json",
                 schema = @Schema(type = "string"))})
@interface MyAnn {

}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        PathItem pathItem = openAPI.paths.get("/pets")

        then:"it is included in the OpenAPI doc"
        pathItem.post.requestBody
        pathItem.post.requestBody.description == 'A pet'
        !pathItem.post.requestBody.required
        pathItem.post.requestBody.content.size() == 1
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.type == 'string'
    }
}
