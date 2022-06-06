package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import spock.lang.Issue

class OpenApiIncludeVisitorSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI doc for security Login controller"() {
        when:
            buildBeanDefinition('test.MyBean', '''
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
        externalDocs = @ExternalDocumentation(description = "definition docs desc"),
        security = {
                @SecurityRequirement(name = "req 1", scopes = {"a", "b"}),
                @SecurityRequirement(name = "req 2", scopes = {"b", "c"})
        }
)
@io.micronaut.openapi.annotation.OpenAPIInclude(value = io.micronaut.security.endpoints.LoginController.class, 
    tags = @Tag(name = "Tag 4"),
    security = @SecurityRequirement(name = "req 3", scopes = {"b", "c"})
)
class Application {

}

@Tag(name = "HelloWorld")
interface HelloWorldApi {
 @Get("/")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get a message", description = "Returns a simple hello world.")
    @ApiResponse(responseCode = "200", description = "All good.")
    HttpResponse<String> helloWorld();
}

@Controller("/hello")
@Tag(name = "HelloWorldController")
class HelloWorldController implements HelloWorldApi {
    @Override
    public HttpResponse<String> helloWorld() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
            AbstractOpenApiVisitor.testReference != null

        when:
            OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
            openAPI.info != null
        when:

            PathItem helloPathItem = openAPI.paths.get("/hello")
            PathItem loginPathItem = openAPI.paths.get("/login")

        then:
            helloPathItem
            loginPathItem.post.operationId == 'login'
            loginPathItem.post.tags[0] == "Tag 4"
            loginPathItem.post.security[0]["req 3"]
            loginPathItem.post.requestBody
            loginPathItem.post.requestBody.required
            loginPathItem.post.requestBody.content
            loginPathItem.post.requestBody.content.size() == 2
            loginPathItem.post.requestBody.content['application/x-www-form-urlencoded'].schema
            loginPathItem.post.requestBody.content['application/x-www-form-urlencoded'].schema['$ref'] == '#/components/schemas/UsernamePasswordCredentials'
            loginPathItem.post.requestBody.content['application/json'].schema
            loginPathItem.post.requestBody.content['application/json'].schema['$ref'] == '#/components/schemas/UsernamePasswordCredentials'
            loginPathItem.post.responses['200'].content['application/json'].schema['$ref'] == '#/components/schemas/Object'
            openAPI.components.schemas['UsernamePasswordCredentials']
            openAPI.components.schemas['UsernamePasswordCredentials'].required.size() == 2
            openAPI.components.schemas['UsernamePasswordCredentials'].properties['username']
            openAPI.components.schemas['UsernamePasswordCredentials'].properties['password']
    }

    void "test build OpenAPI doc for simple endpoint"() {
        when:
            buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Delete;
import io.micronaut.management.endpoint.annotation.Write;
import io.micronaut.management.endpoint.annotation.Selector;

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
@io.micronaut.openapi.annotation.OpenAPIInclude(
    classes = io.micronaut.management.endpoint.routes.RoutesEndpoint.class,
    tags = @Tag(name = "Tag 4"),
    security = @SecurityRequirement(name = "req 3", scopes = {"b", "c"})
)
@io.micronaut.openapi.annotation.OpenAPIInclude(
    value = test.MessageEndpoint.class,
    tags = @Tag(name = "Tag 4"),
    security = @SecurityRequirement(name = "req 3", scopes = {"b", "c"})
)
class Application {

}

@Endpoint(id = "message", defaultSensitive = false, defaultEnabled = true)
class MessageEndpoint {

    String message;

    @Delete(description = "Delete message", produces = "application/text")
    public String deleteMessage() {
        this.message = null;

        return "Message deleted";
    }

    @Write(description = "Set message", produces = "application/text")
    public String setMessage(@Selector String message) {
        String old = this.message;
        this.message = message;

        return old;
    }

}
@Tag(name = "HelloWorld")
interface HelloWorldApi {
 @Get("/")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get a message", description = "Returns a simple hello world.")
    @ApiResponse(responseCode = "200", description = "All good.")
    HttpResponse<String> helloWorld();
}

@Controller("/hello")
@Tag(name = "HelloWorldController")
class HelloWorldController implements HelloWorldApi {
    @Override
    public HttpResponse<String> helloWorld() {
        return null;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:
            AbstractOpenApiVisitor.testReference != null

        when:
            OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
            openAPI.info != null
            openAPI.info.title == 'the title'
            openAPI.info.version == '0.0'
            openAPI.info.description == 'My API'
            openAPI.info.license.name == 'Apache 2.0'
            openAPI.info.contact.name == 'Fred'
            openAPI.tags.size() == 3
            Tag tag = openAPI.tags.find { it -> (it.name == 'Tag 1') }
            tag
            tag.description == 'desc 1'
            openAPI.externalDocs.description == 'definition docs desc'
            openAPI.security.size() == 2
            openAPI.security[0] == ["req 1": ["a", "b"]]
            openAPI.security[1] == ["req 2": ["b", "c"]]
            openAPI.servers.size() == 1
            openAPI.servers[0].description == 'server 1'
            openAPI.servers[0].url == 'https://foo'
            openAPI.servers[0].variables.size() == 2
            openAPI.servers[0].variables.var1.description == 'var 1'
            openAPI.servers[0].variables.var1.default == '1'
            openAPI.servers[0].variables.var1.enum == ['1', '2']

        then:
            openAPI.paths['/message']
            openAPI.paths['/message'].delete
            openAPI.paths['/message'].delete.tags[0] == "Tag 4"
            openAPI.paths['/message'].delete.security[0]["req 3"]
            openAPI.paths['/message/{message}']
            openAPI.paths['/message/{message}'].post
            openAPI.paths['/message/{message}'].post.parameters.size() == 1
            openAPI.paths['/message/{message}'].post.parameters[0].name == 'message'
            openAPI.paths['/message/{message}'].post.tags[0] == "Tag 4"
            openAPI.paths['/message/{message}'].post.security[0]["req 3"]

        then:
            openAPI.paths['/routes']
            openAPI.paths['/routes'].get.tags[0] == "Tag 4"
            openAPI.paths['/routes'].get.security[0]["req 3"]
    }

    void "test build OpenAPI for management endpoints"() {
        when:
            buildBeanDefinition('test.MyBean', '''
package test;

@io.swagger.v3.oas.annotations.OpenAPIDefinition
@io.micronaut.openapi.annotation.OpenAPIManagement(tags = @io.swagger.v3.oas.annotations.tags.Tag(name = "Micronaut Management"))
class Application {
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:
            AbstractOpenApiVisitor.testReference != null

        when:
            OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
            openAPI.paths['/health']
            openAPI.paths['/health'].get.tags[0] == "Micronaut Management"
            openAPI.paths['/beans']
            openAPI.paths['/env']
            openAPI.paths['/info']
            openAPI.paths['/loggers']
            openAPI.paths['/refresh']
            openAPI.paths['/routes']
            openAPI.paths['/stop']
            openAPI.paths['/threaddump']
    }

    void "test build OpenAPI for security endpoints"() {
        when:
            buildBeanDefinition('test.MyBean', '''
package test;

@io.swagger.v3.oas.annotations.OpenAPIDefinition
@io.micronaut.openapi.annotation.OpenAPISecurity(tags = @io.swagger.v3.oas.annotations.tags.Tag(name = "Micronaut Security"))
class Application {
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:
            AbstractOpenApiVisitor.testReference != null

        when:
            OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then: "User defined end point is processed"
            openAPI.paths['/login']
            openAPI.paths['/login'].post.tags[0] == "Micronaut Security"
            openAPI.paths['/logout']
            openAPI.paths['/logout'].post.tags[0] == "Micronaut Security"
            openAPI.paths['/logout'].get.tags[0] == "Micronaut Security"
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/522")
    void "test @Server annotation works and it includes allowableValues"() {
        when:
        buildBeanDefinition('test.MyBean', '''
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
        version = "0.0"
    ),
    servers = {
        @Server(
            description = "server 1",
            url = "https://foo",
            variables = {
                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                @ServerVariable(name = "var2", description = "var 2", defaultValue = "2", allowableValues = {"1", "2"})
            }
        ),
        @Server(
            description = "server 2",
            url = "https://bar",
            variables = {
                @ServerVariable(name = "varA", description = "var A", defaultValue = "A", allowableValues = {"A", "B"}),
                @ServerVariable(name = "varB", description = "var B", defaultValue = "B", allowableValues = {"A", "B"})
            }
        )
    }
)
class Application {
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.info != null
        openAPI.servers.size() == 2

        when:
        Server server = openAPI.servers[0]

        then:
        server.url == 'https://foo'
        server.description == 'server 1'
        server.variables
        server.variables.size() == 2
        server.variables.get('var1').description == 'var 1'
        server.variables.get('var1').default == '1'
        server.variables.get('var1').enum == ['1', '2']
        server.variables.get('var2').description == 'var 2'
        server.variables.get('var2').default == '2'
        server.variables.get('var2').enum == ['1', '2']

        when:
        Server server2 = openAPI.servers[1]

        then:
        server2.url == 'https://bar'
        server2.description == 'server 2'
        server2.variables
        server2.variables.size() == 2
        server2.variables.get('varA').description == 'var A'
        server2.variables.get('varA').default == 'A'
        server2.variables.get('varA').enum == ['A', 'B']
        server2.variables.get('varB').description == 'var B'
        server2.variables.get('varB').default == 'B'
        server2.variables.get('varB').enum == ['A', 'B']
    }
}
