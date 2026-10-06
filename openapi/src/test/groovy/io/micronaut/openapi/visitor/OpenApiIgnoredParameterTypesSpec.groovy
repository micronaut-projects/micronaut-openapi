package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import spock.util.environment.RestoreSystemProperties

class OpenApiIgnoredParameterTypesSpec extends AbstractOpenApiTypeElementSpec {

    private static final String SOURCE = '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;

@Controller("/items")
class ItemController {

    @Get
    String list(SignedInUser user, @QueryValue String filter) {
        return null;
    }

    @Get("/tenant")
    String tenant(Tenant tenant, @QueryValue String filter) {
        return null;
    }

    @Post
    String create(SignedInUser user, @Body Item item) {
        return null;
    }
}

interface BoundFromRequest {
}

class SignedInUser implements BoundFromRequest {
    public String email;
    public String hashedPassword;
}

class Tenant {
    public String id;
}

class Item {
    public String title;
}

@jakarta.inject.Singleton
class MyBean {}
'''

    void "a parameter of a type which is not configured is part of the specification"() {

        when:
        buildBeanDefinition('test.MyBean', SOURCE)

        then:
        Utils.testReference != null

        when:
        OpenAPI openApi = Utils.testReference

        then:
        openApi.paths."/items".get.parameters*.name == ['user', 'filter']
        openApi.paths."/items/tenant".get.parameters*.name == ['tenant', 'filter']
        openApi.components.schemas.SignedInUser
    }

    @RestoreSystemProperties
    void "a parameter of a configured type is ignored"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_IGNORED_PARAMETER_TYPES, "test.SignedInUser")

        when:
        buildBeanDefinition('test.MyBean', SOURCE)

        then:
        Utils.testReference != null

        when:
        OpenAPI openApi = Utils.testReference

        then: "the parameter is gone, and so is the schema nothing else refers to"
        openApi.paths."/items".get.parameters*.name == ['filter']
        !openApi.components.schemas.SignedInUser

        and: "it is not mistaken for a part of the request body either"
        !openApi.paths."/items".post.parameters
        openApi.paths."/items".post.requestBody.content."application/json".schema.$ref == '#/components/schemas/Item'

        and: "a type which is not configured is untouched"
        openApi.paths."/items/tenant".get.parameters*.name == ['tenant', 'filter']
    }

    @RestoreSystemProperties
    void "a parameter assignable to a configured type is ignored"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_IGNORED_PARAMETER_TYPES, "test.BoundFromRequest")

        when:
        buildBeanDefinition('test.MyBean', SOURCE)

        then:
        Utils.testReference != null

        when:
        OpenAPI openApi = Utils.testReference

        then:
        openApi.paths."/items".get.parameters*.name == ['filter']
        !openApi.components.schemas.SignedInUser
    }

    @RestoreSystemProperties
    void "several types can be configured"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_IGNORED_PARAMETER_TYPES, "test.SignedInUser, test.Tenant")

        when:
        buildBeanDefinition('test.MyBean', SOURCE)

        then:
        Utils.testReference != null

        when:
        OpenAPI openApi = Utils.testReference

        then:
        openApi.paths."/items".get.parameters*.name == ['filter']
        openApi.paths."/items/tenant".get.parameters*.name == ['filter']
        !openApi.components.schemas.SignedInUser
        !openApi.components.schemas.Tenant
    }
}
