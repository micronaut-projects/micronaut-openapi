package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme

class OpenApiApplicationVisitorSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI doc for simple endpoint"() {
        given:"An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-endpoints.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.management.endpoint.annotation.Delete;
import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Selector;
import io.micronaut.management.endpoint.annotation.Write;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author graemerocher
 * @since 1.0
 */
@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                termsOfService = "There are terms of service",
                license = @License(
                        name = "Apache 2.0",
                        url = "https://foo.bar",
                        extensions = {
                                @Extension(
                                        name = "license.custom1",
                                        properties = {
                                                @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                        }
                                ),
                                @Extension(
                                        name = "license.custom2",
                                        properties = {
                                                @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                        }
                                )
                        }
                ),
                contact = @Contact(
                        name = "Fred",
                        url = "https://gigantic-server.com",
                        email = "Fred@gigagantic-server.com",
                        extensions = {
                                @Extension(
                                        name = "contact.custom1",
                                        properties = {
                                                @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                        }
                                ),
                                @Extension(
                                        name = "contact.custom2",
                                        properties = {
                                                @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                        }
                                )
                        }
                ),
                extensions = {
                        @Extension(
                                name = "info.custom1",
                                properties = {
                                        @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                        @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                }
                        ),
                        @Extension(
                                name = "info.custom2",
                                properties = {
                                        @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                        @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                }
                        )
                }
        ),
        tags = {
                @Tag(
                        name = "Tag 0",
                        description = "desc 0",
                        externalDocs = @ExternalDocumentation(
                                description = "docs desc0",
                                url = "http://externaldoc.com",
                                extensions = {
                                        @Extension(
                                                name = "extdocs.custom1",
                                                properties = {
                                                        @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                        @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                                }
                                        ),
                                        @Extension(
                                                name = "extdocs.custom2",
                                                properties = {
                                                        @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                        @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                                }
                                        )
                                }
                        ),
                        extensions = {
                                @Extension(
                                        name = "tag.custom1",
                                        properties = {
                                                @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                        }
                                ),
                                @Extension(
                                        name = "tag.custom2",
                                        properties = {
                                                @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                        }
                                )
                        }
                ),
                @Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc")),
                @Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2")),
                @Tag(name = "Tag 3")
        },
        externalDocs = @ExternalDocumentation(
                description = "definition docs desc",
                url = "http://externaldoc.com",
                extensions = {
                        @Extension(
                                name = "extdocs.custom1",
                                properties = {
                                        @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                        @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                }
                        ),
                        @Extension(
                                name = "extdocs.custom2",
                                properties = {
                                        @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                        @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                }
                        )
                }
        ),
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
                        },
                        extensions = {
                                @Extension(
                                        name = "server.custom1",
                                        properties = {
                                                @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                        }
                                ),
                                @Extension(
                                        name = "server.custom2",
                                        properties = {
                                                @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                        }
                                )
                        }
                ),
                @Server(
                        description = "server 2",
                        url = "https://bar",
                        variables = {
                                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                                @ServerVariable(name = "var2", description = "var 2", defaultValue = "1", allowableValues = {"1", "2"})
                        }
                ),
        }
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

        then:"the state is correct"
        Utils.testReference != null

        when:"the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference

        then:"it is included in the OpenAPI doc"
        openAPI.info != null
        openAPI.info.title == 'the title'
        openAPI.info.version == '0.0'
        openAPI.info.description == 'My API'
        openAPI.info.termsOfService == 'There are terms of service'

        openAPI.info.license.name == 'Apache 2.0'
        openAPI.info.license.url == 'https://foo.bar'
        openAPI.info.license.extensions.size() == 2
        openAPI.info.license.extensions.'x-license.custom1'.prop11 == 'prop11Val'
        openAPI.info.license.extensions.'x-license.custom1'.prop12 == 'prop12Val'
        openAPI.info.license.extensions.'x-license.custom2'.prop21 == 'prop21Val'
        openAPI.info.license.extensions.'x-license.custom2'.prop22 == 'prop22Val'

        openAPI.info.contact.name == 'Fred'
        openAPI.info.contact.url == 'https://gigantic-server.com'
        openAPI.info.contact.email == 'Fred@gigagantic-server.com'
        openAPI.info.contact.extensions.size() == 2
        openAPI.info.contact.extensions.'x-contact.custom1'.prop11 == 'prop11Val'
        openAPI.info.contact.extensions.'x-contact.custom1'.prop12 == 'prop12Val'
        openAPI.info.contact.extensions.'x-contact.custom2'.prop21 == 'prop21Val'
        openAPI.info.contact.extensions.'x-contact.custom2'.prop22 == 'prop22Val'

        openAPI.info.extensions.size() == 2
        openAPI.info.extensions.'x-info.custom1'.prop11 == 'prop11Val'
        openAPI.info.extensions.'x-info.custom1'.prop12 == 'prop12Val'
        openAPI.info.extensions.'x-info.custom2'.prop21 == 'prop21Val'
        openAPI.info.extensions.'x-info.custom2'.prop22 == 'prop22Val'

        openAPI.tags.size() == 4

        openAPI.tags.get(0).name == 'Tag 0'
        openAPI.tags.get(0).description == 'desc 0'
        openAPI.tags.get(0).externalDocs.description == 'docs desc0'
        openAPI.tags.get(0).externalDocs.url == 'http://externaldoc.com'
        openAPI.tags.get(0).externalDocs.extensions.size() == 2
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.prop11 == 'prop11Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.prop12 == 'prop12Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.prop21 == 'prop21Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.prop22 == 'prop22Val'
        openAPI.tags.get(0).extensions.size() == 2
        openAPI.tags.get(0).extensions.'x-tag.custom1'.prop11 == 'prop11Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.prop12 == 'prop12Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.prop21 == 'prop21Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.prop22 == 'prop22Val'

        openAPI.tags.get(1).name == 'Tag 1'
        openAPI.tags.get(1).description == 'desc 1'
        openAPI.tags.get(1).externalDocs.description == 'docs desc'

        openAPI.tags.get(2).name == 'Tag 2'
        openAPI.tags.get(2).description == 'desc 2'
        openAPI.tags.get(2).externalDocs.description == 'docs desc 2'

        openAPI.tags.get(3).name == 'Tag 3'

        openAPI.externalDocs.description == 'definition docs desc'
        openAPI.externalDocs.url == 'http://externaldoc.com'
        openAPI.externalDocs.extensions.size() == 2
        openAPI.externalDocs.extensions.'x-extdocs.custom1'.prop11 == 'prop11Val'
        openAPI.externalDocs.extensions.'x-extdocs.custom1'.prop12 == 'prop12Val'
        openAPI.externalDocs.extensions.'x-extdocs.custom2'.prop21 == 'prop21Val'
        openAPI.externalDocs.extensions.'x-extdocs.custom2'.prop22 == 'prop22Val'

        openAPI.security.size() == 2
        openAPI.security[0] == ["req 1": ["a", "b"]]
        openAPI.security[1] == ["req 2":["b", "c"]]

        openAPI.servers.size() == 2
        openAPI.servers[0].description == 'server 1'
        openAPI.servers[0].url == 'https://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'
        openAPI.servers[0].variables.var1.enum == ['1', '2']
        openAPI.servers[0].variables.var2.description == 'var 2'
        openAPI.servers[0].variables.var2.default == '1'
        openAPI.servers[0].variables.var2.enum == ['1', '2']
        openAPI.servers[0].extensions.size() == 2
        openAPI.servers[0].extensions.'x-server.custom1'.prop11 == 'prop11Val'
        openAPI.servers[0].extensions.'x-server.custom1'.prop12 == 'prop12Val'
        openAPI.servers[0].extensions.'x-server.custom2'.prop21 == 'prop21Val'
        openAPI.servers[0].extensions.'x-server.custom2'.prop22 == 'prop22Val'

        openAPI.servers[1].description == 'server 2'
        openAPI.servers[1].url == 'https://bar'
        openAPI.servers[1].variables.size() == 2
        openAPI.servers[1].variables.var1.description == 'var 1'
        openAPI.servers[1].variables.var1.default == '1'
        openAPI.servers[1].variables.var1.enum == ['1', '2']
        openAPI.servers[1].variables.var2.description == 'var 2'
        openAPI.servers[1].variables.var2.default == '1'
        openAPI.servers[1].variables.var2.enum == ['1', '2']

        then:"User defined end point is processed"
        openAPI.paths['/message']
        openAPI.paths['/message'].delete
        openAPI.paths['/message'].delete.servers.size() == 1
        openAPI.paths['/message'].delete.servers[0].url == 'https://{username}.gigantic-server.com:{port}/{basePath}'
        openAPI.paths['/message'].delete.servers[0].description == 'The production API server'
        openAPI.paths['/message'].delete.servers[0].variables
        openAPI.paths['/message'].delete.servers[0].variables.size() == 3
        openAPI.paths['/message'].delete.security.size() == 1
        openAPI.paths['/message'].delete.security[0].size() == 1
        openAPI.paths['/message'].delete.security[0].get('api_key') == []
        openAPI.paths['/message/{message}']
        openAPI.paths['/message/{message}'].post
        openAPI.paths['/message/{message}'].post.parameters.size() == 1
        openAPI.paths['/message/{message}'].post.parameters[0].name == 'message'
        openAPI.paths['/message/{message}'].post.servers.size() == 1
        openAPI.paths['/message/{message}'].post.servers[0].url == 'https://{username}.gigantic-server.com:{port}/{basePath}'
        openAPI.paths['/message/{message}'].post.servers[0].description == 'The production API server'
        openAPI.paths['/message/{message}'].post.servers[0].variables
        openAPI.paths['/message/{message}'].post.servers[0].variables.size() == 3
        openAPI.paths['/message/{message}'].post.security.size() == 1
        openAPI.paths['/message/{message}'].post.security[0].size() == 1
        openAPI.paths['/message/{message}'].post.security[0].get('api_key') == []

        then:"Built-in end point are processed"
        openAPI.paths['/routes']
        openAPI.paths['/routes'].get
        openAPI.paths['/routes'].get.servers.size() == 1
        openAPI.paths['/routes'].get.servers[0].url == 'https://{username}.gigantic-server.com:{port}/{basePath}'
        openAPI.paths['/routes'].get.servers[0].description == 'The production API server'
        openAPI.paths['/routes'].get.servers[0].variables
        openAPI.paths['/routes'].get.servers[0].variables.size() == 3
        openAPI.paths['/routes'].get.security.size() == 1
        openAPI.paths['/routes'].get.security[0].size() == 1
        openAPI.paths['/routes'].get.security[0].get('api_key') == []
        openAPI.paths['/beans']
        openAPI.paths['/beans'].get
        openAPI.paths['/beans'].get.servers.size() == 1
        openAPI.paths['/beans'].get.servers[0].url == 'https://{username}.gigantic-server.com:{port}/{basePath}'
        openAPI.paths['/beans'].get.servers[0].description == 'The production API server'
        openAPI.paths['/beans'].get.servers[0].variables
        openAPI.paths['/beans'].get.servers[0].variables.size() == 3
        openAPI.paths['/beans'].get.security.size() == 1
        openAPI.paths['/beans'].get.security[0].size() == 1
        openAPI.paths['/beans'].get.security[0].get('api_key') == []
        openAPI.paths['/loggers']
        openAPI.paths['/loggers'].get
        openAPI.paths['/loggers'].get.servers.size() == 1
        openAPI.paths['/loggers'].get.servers[0].url == 'https://{username}.gigantic-server.com:{port}/{basePath}'
        openAPI.paths['/loggers'].get.servers[0].description == 'The production API server'
        openAPI.paths['/loggers'].get.servers[0].variables
        openAPI.paths['/loggers'].get.servers[0].variables.size() == 3
        openAPI.paths['/loggers'].get.security.size() == 1
        openAPI.paths['/loggers'].get.security[0].size() == 1
        openAPI.paths['/loggers'].get.security[0].get('api_key') == []
        openAPI.paths['/refresh']
        openAPI.paths['/refresh'].post
        openAPI.paths['/refresh'].post.servers.size() == 2
        openAPI.paths['/refresh'].post.security.size() == 2
        openAPI.paths['/refresh'].post.security[0].size() == 1
        openAPI.paths['/refresh'].post.security[0].get('api_key') == []
        openAPI.paths['/refresh'].post.security[1].size() == 1
        openAPI.paths['/refresh'].post.security[1].get('petstore_auth') == ['write:pets', 'read:pets']

        when:"Loggers end point param name"
        // with jdk8 the argument name is not preserved
        def uri = openAPI.paths['/loggers/{name}'] == null ? openAPI.paths['/loggers/{arg0}'] == null ? 'unknown' :  '/loggers/{arg0}' : '/loggers/{name}'

        then:
        openAPI.paths[uri]
        openAPI.paths[uri].get
        openAPI.paths[uri].get.parameters.size() == 1
        openAPI.paths[uri].get.parameters[0].name ==~ /arg0|name/
        openAPI.paths[uri].post
        openAPI.paths[uri].post.parameters.size() == 1
        openAPI.paths[uri].post.parameters[0].name ==~ /arg0|name/
        openAPI.paths[uri].post.requestBody

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE)
    }

    void "test build OpenAPI doc for simple type with generics"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.servers.*;
import io.swagger.v3.oas.annotations.security.*;
/**
 * @author graemerocher
 * @since 1.0
 */
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


@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference

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
    }

    void "test build OpenAPI doc tags, servers and security at class level"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.servers.*;
import io.swagger.v3.oas.annotations.security.*;
/**
 * @author graemerocher
 * @since 1.0
 */
@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                license = @License(name = "Apache 2.0", url = "https://foo.bar"),
                contact = @Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        ),

        externalDocs = @ExternalDocumentation(description = "definition docs desc")
)
@Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc"))
@Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2"))
@Tag(name = "Tag 3")
@Server(
        description = "server 1",
        url = "https://foo",
        variables = {
                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                @ServerVariable(name = "var2", description = "var 2", defaultValue = "1", allowableValues = {"1", "2"})
        })
@SecurityRequirement(name = "req 1", scopes = {"a", "b"})
@SecurityRequirement(name = "req 2", scopes = {"b", "c"})
class Application {

}


@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference

        then:"it is included in the OpenAPI doc"
        openAPI.info != null
        openAPI.info.title == 'the title'
        openAPI.info.version == '0.0'
        openAPI.info.description == 'My API'
        openAPI.info.license.name == 'Apache 2.0'
        openAPI.info.contact.name == 'Fred'
        openAPI.tags.size() == 3
        openAPI.tags.first().name == 'Tag 1'
        openAPI.tags.first().name == 'Tag 1'
        openAPI.tags.first().description == 'desc 1'
        openAPI.externalDocs.description == 'definition docs desc'
        openAPI.security.size() == 2
        openAPI.security.get(0).containsKey('req 1')
        openAPI.security.get(0).get('req 1') == ["a", "b"]
        openAPI.security.get(1).containsKey('req 2')
        openAPI.security.get(1).get('req 2') == ["b", "c"]
        openAPI.servers.size() == 1
        openAPI.servers[0].description == 'server 1'
        openAPI.servers[0].url == 'https://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'
        openAPI.servers[0].variables.var1.enum == ['1', '2']
    }

    void "test build OpenAPI security schemes"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API"
        )
)
@SecurityScheme(
    name = "myOauth2Security",
    type = SecuritySchemeType.OAUTH2,
    in = SecuritySchemeIn.HEADER,
    paramName = "myHeader",
    flows = @OAuthFlows(
            implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                       scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account")))
)
@SecurityScheme(name = "myOauth3Security", type = SecuritySchemeType.OAUTH2)
@SecurityScheme(
    name = "myOauth4Security",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "JWT",
    openIdConnectUrl = "https://sdsd.sdsd.com",
    bearerFormat = "sdsdd",
    flows = @OAuthFlows(
            implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                       scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))),
    description = "ssssss"
)
@SecurityScheme(
    name = "testApiKey",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    scheme = "basic",
    paramName = "JWT",
    openIdConnectUrl = "https://sdsd.sdsd.com",
    bearerFormat = "sdsdd",
    flows = @OAuthFlows(
            implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                       scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))),
    description = "ssssss"
)
@SecurityScheme(
    name = "testHttp",
    type = SecuritySchemeType.HTTP,
    in = SecuritySchemeIn.COOKIE,
    scheme = "bearer",
    paramName = "JWT",
    openIdConnectUrl = "https://sdsd.sdsd.com",
    bearerFormat = "sdsdd",
    flows = @OAuthFlows(
            implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                       scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))),
    description = "ssssss"
)
@SecurityScheme(
    name = "testOpenIdConnect",
    type = SecuritySchemeType.OPENIDCONNECT,
    in = SecuritySchemeIn.COOKIE,
    scheme = "basic",
    paramName = "JWT",
    openIdConnectUrl = "https://sdsd.sdsd.com",
    bearerFormat = "sdsdd",
    flows = @OAuthFlows(
            implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                       scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))),
    description = "ssssss"
)
@SecurityScheme(
    name = "testOauth2",
    type = SecuritySchemeType.OAUTH2,
    in = SecuritySchemeIn.COOKIE,
    scheme = "basic",
    paramName = "JWT",
    openIdConnectUrl = "https://sdsd.sdsd.com",
    bearerFormat = "sdsdd",
    flows = @OAuthFlows(
            implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                       scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))),
    description = "ssssss"
)
class Application {
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference

        then:"it is included in the OpenAPI doc"
        openAPI != null
        openAPI.components.securitySchemes['myOauth2Security']
        openAPI.components.securitySchemes['myOauth2Security'].type == SecurityScheme.Type.OAUTH2
        openAPI.components.securitySchemes['myOauth2Security'].name == null
        openAPI.components.securitySchemes['myOauth2Security'].in == null
        openAPI.components.securitySchemes['myOauth2Security'].flows
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.authorizationUrl == 'https://url.com/auth'
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes.size() == 1
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes.get("write:pets")
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes.get("write:pets") == 'modify pets in your account'

        openAPI.components.securitySchemes['myOauth3Security'].name == null

        openAPI.components.securitySchemes['myOauth4Security'].name == "JWT"
        openAPI.components.securitySchemes['myOauth4Security'].in == SecurityScheme.In.COOKIE
        openAPI.components.securitySchemes['myOauth4Security'].type == SecurityScheme.Type.APIKEY

        def apiKey = openAPI.components.securitySchemes['testApiKey']
        apiKey.type == SecurityScheme.Type.APIKEY
        apiKey.in == SecurityScheme.In.COOKIE
        apiKey.name == 'JWT'
        apiKey.description == 'ssssss'
        apiKey.openIdConnectUrl == null
        apiKey.bearerFormat == null
        apiKey.flows == null
        apiKey.scheme == null

        def http = openAPI.components.securitySchemes['testHttp']
        http.type == SecurityScheme.Type.HTTP
        http.in == null
        http.name == null
        http.description == 'ssssss'
        http.openIdConnectUrl == null
        http.bearerFormat == 'sdsdd'
        http.flows == null
        http.scheme == 'bearer'

        def openIdConnect = openAPI.components.securitySchemes['testOpenIdConnect']
        openIdConnect.type == SecurityScheme.Type.OPENIDCONNECT
        openIdConnect.in == null
        openIdConnect.name == null
        openIdConnect.description == 'ssssss'
        openIdConnect.openIdConnectUrl == 'https://sdsd.sdsd.com'
        openIdConnect.bearerFormat == null
        openIdConnect.flows == null
        openIdConnect.scheme == null

        def oauth2 = openAPI.components.securitySchemes['testOauth2']
        oauth2.type == SecurityScheme.Type.OAUTH2
        oauth2.in == null
        oauth2.name == null
        oauth2.description == 'ssssss'
        oauth2.openIdConnectUrl == null
        oauth2.bearerFormat == null
        oauth2.flows
        oauth2.flows.implicit
        oauth2.scheme == null
    }

    void "test disable openapi"() {

        given: "An API definition"
        Utils.testReference = null
        Utils.testReferences = null
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED, "false")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        !Utils.testReference

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED)
    }

    void "test disable openapi from file"() {

        given: "An API definition"
        Utils.testReference = null
        Utils.testReferences = null
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "disabled-openapi")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        !Utils.testReference

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }

    void "test disable openapi from openapi.properties file"() {

        given: "An API definition"
        Utils.testReference = null
        Utils.testReferences = null
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-disabled-openapi.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Controller("/pets")
interface PetOperations<T extends Pet> {

    @Post
    Single<T> save(String name, int age);
}

class Pet {

    private int privateAge;
    protected int protectedAge;
    int packageAge;

    public int age;

    // ignored by json
    @JsonIgnore
    public int ignored;
    // hidden by swagger
    @Hidden
    public int hidden;
    // hidden by swagger
    @Schema(hidden = true)
    public int hidden2;

    // private should not be included
    private String name;

    // protected should not be included
    protected String protectme;

    // static should not be included
    public static String CONST;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        !Utils.testReference

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE)
    }

    void "test build OpenAPIDefinition with placeholders"() {

        given: "An API definition"
        Utils.testReference = null
        Utils.testReferences = null
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-placeholders.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "broken-micronaut-openapi-expand",
                version = "${api.version}",
                description = "${another.placeholder.value}"
        )
)
class Application {
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.info
        openAPI.info.title == "broken-micronaut-openapi-expand"
        openAPI.info.description == 'monkey'
        openAPI.info.version == '2.2.2'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE)
    }
}
