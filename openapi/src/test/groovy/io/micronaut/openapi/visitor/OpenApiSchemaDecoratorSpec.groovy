package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenApiSchemaDecoratorSpec extends AbstractOpenApiTypeElementSpec {

    void "test custom OpenAPI schema decorators"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-schema-decorators.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class OpenApiController {

    @Get("/v0")
    public MyDto getV0() {
        return null;
    }

    @Get("/v1")
    public io.micronaut.openapi.api.v1_0_0.MyDto getV1() {
        return null;
    }

    @Get("/v2")
    public io.micronaut.openapi.api.v2_0_1.MyDto getV2() {
        return null;
    }
}

class MyDto {

    public String field;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        Schema myDtoSchema1 = openAPI.components.schemas.MyDto1_0_0
        Schema myDtoSchema2 = openAPI.components.schemas.MyDto2_0_1

        then:

        myDtoSchema
        openAPI.paths."/v0".get.responses['200'].content."application/json".schema.$ref == '#/components/schemas/MyDto'

        myDtoSchema1
        openAPI.paths."/v1".get.responses['200'].content."application/json".schema.$ref == '#/components/schemas/MyDto1_0_0'

        myDtoSchema2
        openAPI.paths."/v2".get.responses['200'].content."application/json".schema.$ref == '#/components/schemas/MyDto2_0_1'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE)
    }

    void "test custom OpenAPI schema decorators with environments"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "schemadecorator")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class OpenApiController {

    @Get("/v0")
    public MyDto getV0() {
        return null;
    }

    @Get("/v1")
    public io.micronaut.openapi.api.v1_0_0.MyDto getV1() {
        return null;
    }

    @Get("/v2")
    public io.micronaut.openapi.api.v2_0_1.MyDto getV2() {
        return null;
    }
}

class MyDto {

    public String field;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        Schema myDtoSchema1 = openAPI.components.schemas.MyDto1_0_0
        Schema myDtoSchema2 = openAPI.components.schemas.MyDto2_0_1

        then:

        myDtoSchema
        openAPI.paths."/v0".get.responses['200'].content."application/json".schema.$ref == '#/components/schemas/MyDto'

        myDtoSchema1
        openAPI.paths."/v1".get.responses['200'].content."application/json".schema.$ref == '#/components/schemas/MyDto1_0_0'

        myDtoSchema2
        openAPI.paths."/v2".get.responses['200'].content."application/json".schema.$ref == '#/components/schemas/MyDto2_0_1'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }
}
