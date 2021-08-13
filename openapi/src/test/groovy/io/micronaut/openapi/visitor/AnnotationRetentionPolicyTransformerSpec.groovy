package io.micronaut.openapi.visitor

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tags
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.tags.Tag

class AnnotationRetentionPolicyTransformerSpec extends AbstractOpenApiTypeElementSpec {

    void "test transform annotation metadata"() {
            when:
            def definition = buildBeanDefinition('test.HelloWorldController', '''
package test;

import io.micronaut.http.annotation.*;
import io.micronaut.http.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.servers.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                license = @License(name = "Apache 2.0", url = "https://foo.bar"),
                contact = @Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        ),
        tags = {
                @Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc")),
                @Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2")),
                @Tag(name = "Tag 3")
        },
        externalDocs = @ExternalDocumentation(description = "definition docs desc"),
        security = {
                @SecurityRequirement(name = "req 1", scopes = {"a", "b"}),
                @SecurityRequirement(name = "req 2", scopes = {"b", "c"})
        },
        servers = {
                @Server(
                        description = "server 1",
                        url = "https://foo",
                        variables = {
                                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                                @ServerVariable(name = "var2", description = "var 2", defaultValue = "1", allowableValues = {"1", "2"})
                        })
        }
)
class Application {

}

@Tag(name = "HelloWorld")
interface HelloWorldApi {
    @Get("/")
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "Article Operations")
    @Operation(summary = "Get a message", description = "Returns a simple hello world.")
    @ApiResponse(responseCode = "200", description = "All good.")
    HttpResponse<String> helloWorld();
}

@Controller("/hello")
@Tag(name = "Controller")
class HelloWorldController implements HelloWorldApi {
    @Override
    public HttpResponse<String> helloWorld() {
        return null;
    }

    @Get("/dummy")
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "Dummy Operations")
    @Operation(summary = "Get a message", description = "Returns a simple hello world.")
    @ApiResponse(responseCode = "200", description = "All good.")
    public HttpResponse<String> dummy() {
        return null;
    }
}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"the /pets path is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"it is included in the OpenAPI doc"
        openAPI.info != null
        openAPI.info.title == 'the title'
        openAPI.info.version == '0.0'
        openAPI.info.description == 'My API'
        openAPI.info.license.name == 'Apache 2.0'
        openAPI.info.contact.name == 'Fred'
        openAPI.tags.size() == 3
        openAPI.tags.first().name == 'Tag 1'
        openAPI.tags.first().description == 'desc 1'
        openAPI.externalDocs.description == 'definition docs desc'
        openAPI.security.size() == 2
        openAPI.security[0] == ["req 1":["a", "b"]]
        openAPI.security[1] == ["req 2":["b", "c"]]
        openAPI.servers.size() == 1
        openAPI.servers[0].description == 'server 1'
        openAPI.servers[0].url == 'https://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'
        openAPI.servers[0].variables.var1.enum == ['1', '2']

        when:
        Operation operation = AbstractOpenApiVisitor.testReference?.paths?.get("/hello")?.get

        then:
        operation != null
        operation.operationId == 'helloWorld'
        operation.parameters.size() == 0
        operation.tags
        operation.tags.size() == 3
        operation.tags.contains("Controller")
        operation.tags.contains("HelloWorld")
        operation.tags.contains("Article Operations")

        when:"Controller"
        def annotations = definition.annotationNames

        then:
        annotations.contains(Controller.class.getName())
        !annotations.contains(Tag.class.getName())
        !annotations.contains(Tags.class.getName())

        when:"First method"
        annotations = definition.executableMethods.first().annotationNames

        then:
        annotations.contains(Get.class.getName())
        !annotations.contains(io.swagger.v3.oas.annotations.Operation.class.getName())
        !annotations.contains(ApiResponse.class.getName())

        when:"Second method"
        annotations = definition.executableMethods[1].annotationNames

        then:
        annotations.contains(Get.class.getName())
        !annotations.contains(io.swagger.v3.oas.annotations.Operation.class.getName())
        !annotations.contains(ApiResponse.class.getName())
        !annotations.contains(Tag.class.getName())
        !annotations.contains(Tags.class.getName())
    }

}