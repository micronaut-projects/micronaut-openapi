package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractKotlinCompilerSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse

class OpenApiPojoControllerKotlinSpec extends AbstractKotlinCompilerSpec {

    def setup() {
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.clearProperty(Utils.ATTR_TEST_MODE)
    }

    void "test kotlin"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue

@Controller("/hello")
class HelloController {

    @Get
    @Produces(MediaType.TEXT_PLAIN)
    fun index(@Nullable @QueryValue("channels") channels: Collection<Channel?>) = ""

    @Introspected
    enum class Channel {
        @JsonProperty("mysys")
        SYSTEM1,
        SYSTEM2
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.'/hello'.get
        Schema schema = openAPI.components.schemas.'HelloController.Channel'
        ApiResponse response = operation.responses.'200'

        then: "the components are valid"
        operation.parameters.size() == 1
        operation.parameters[0].name == 'channels'
        operation.parameters[0].in == 'query'
        operation.parameters[0].schema
        operation.parameters[0].schema.type == 'array'
        operation.parameters[0].schema.nullable == true
        operation.parameters[0].schema.items.allOf[0].$ref == '#/components/schemas/HelloController.Channel'
        operation.parameters[0].schema.items.allOf[1].nullable == true

        response.content.'text/plain'.schema.type == 'string'

        schema
        schema.type == 'string'
        schema.enum.size() == 2
        schema.enum[0] == 'mysys'
        schema.enum[1] == 'SYSTEM2'
    }
}
