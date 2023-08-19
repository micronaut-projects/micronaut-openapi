package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractKotlinCompilerSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenApiPojoControllerKotlinSpec extends AbstractKotlinCompilerSpec {

    void "test kotlin"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test

import io.micronaut.core.annotation.*
import io.micronaut.http.annotation.*
import io.micronaut.http.*

@Controller("/hello")
class HelloController {

    @Get
    @Produces(MediaType.TEXT_PLAIN)
    fun index(@Nullable @QueryValue("channels") channels: Collection<Channel?>) = ""

    @Introspected
    enum class Channel {
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
        Schema schema = openAPI.components.schemas['ExampleData']

        then: "the components are valid"
        schema.properties.size() == 6
        schema.type == 'object'
        schema.required
    }
}
