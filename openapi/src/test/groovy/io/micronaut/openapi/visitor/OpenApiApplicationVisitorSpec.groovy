/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor

import io.micronaut.inject.BeanDefinition
import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme

class OpenApiApplicationVisitorSpec extends AbstractTypeElementSpec {

    def cleanup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "")
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "")
    }

    void "test build OpenAPI doc for simple endpoint"() {
        given:"An API definition"
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-endpoints.properties")
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean', '''
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

/**
 * @author graemerocher
 * @since 1.0
 */
@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                license = @License(name = "Apache 2.0", url = "http://foo.bar"),
                contact = @Contact(url = "http://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
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
                        url = "http://foo",
                        variables = {
                                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                                @ServerVariable(name = "var2", description = "var 2", defaultValue = "1", allowableValues = {"1", "2"})
                        })
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
class HelloWorldController implements HelloWorldApi {
    @Override
    public HttpResponse<String> helloWorld() {
        return null;
    }
}
@javax.inject.Singleton
class MyBean {}
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
        openAPI.servers[0].url == 'http://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'

        then:"User defined end point is processed"
        openAPI.paths['/message']
        openAPI.paths['/message'].delete
        openAPI.paths['/message/{message}']
        openAPI.paths['/message/{message}'].post
        openAPI.paths['/message/{message}'].post.parameters.size() == 1
        openAPI.paths['/message/{message}'].post.parameters[0].name == 'message'

        then:"Built-in end point are processed"
        openAPI.paths['/routes']
        openAPI.paths['/routes'].get
        openAPI.paths['/refresh']
        openAPI.paths['/refresh'].post
        openAPI.paths['/beans']
        openAPI.paths['/beans'].get
        openAPI.paths['/loggers']
        openAPI.paths['/loggers'].get

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
    }

    void "test build OpenAPI doc for simple type with generics"() {
        given:"An API definition"
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean', '''
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
                license = @License(name = "Apache 2.0", url = "http://foo.bar"),
                contact = @Contact(url = "http://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
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
                        url = "http://foo",
                        variables = {
                                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                                @ServerVariable(name = "var2", description = "var 2", defaultValue = "1", allowableValues = {"1", "2"})
                        })
        }
)
class Application {

}


@javax.inject.Singleton
class MyBean {}
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
        openAPI.servers[0].url == 'http://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'
    }

    void "test build OpenAPI doc tags, servers and security at class level"() {
        given:"An API definition"
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean', '''
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
                license = @License(name = "Apache 2.0", url = "http://foo.bar"),
                contact = @Contact(url = "http://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        ),

        externalDocs = @ExternalDocumentation(description = "definition docs desc")
)
@Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc"))
@Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2"))
@Tag(name = "Tag 3")
@Server(
        description = "server 1",
        url = "http://foo",
        variables = {
                @ServerVariable(name = "var1", description = "var 1", defaultValue = "1", allowableValues = {"1", "2"}),
                @ServerVariable(name = "var2", description = "var 2", defaultValue = "1", allowableValues = {"1", "2"})
        })
@SecurityRequirement(name = "req 1", scopes = {"a", "b"})
@SecurityRequirement(name = "req 2", scopes = {"b", "c"})
class Application {

}


@javax.inject.Singleton
class MyBean {}
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
        openAPI.servers[0].url == 'http://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'
    }

    void "test build OpenAPI security schemes"() {
        given:"An API definition"
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean', '''
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
import io.swagger.v3.oas.annotations.enums.*;
/**
 * @author graemerocher
 * @since 1.0
 */
@OpenAPIDefinition()
@SecurityScheme(name = "myOauth2Security",
           type = SecuritySchemeType.OAUTH2,
           in = SecuritySchemeIn.HEADER,
           flows = @OAuthFlows(
                   implicit = @OAuthFlow(authorizationUrl = "http://url.com/auth",
                           scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))))
@SecurityScheme(name = "myOauth3Security", type = SecuritySchemeType.OAUTH2)
@SecurityScheme(name = "myOauth4Security", type = SecuritySchemeType.APIKEY, paramName = "JWT")
class Application {

}


@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"the /pets path is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:"it is included in the OpenAPI doc"
        openAPI != null
        openAPI.components.securitySchemes['myOauth2Security']
        openAPI.components.securitySchemes['myOauth2Security'].type == SecurityScheme.Type.OAUTH2
        openAPI.components.securitySchemes['myOauth2Security'].name == null
        openAPI.components.securitySchemes['myOauth2Security'].flows
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.authorizationUrl == 'http://url.com/auth'
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes.size() == 1
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes.get("write:pets")
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes.get("write:pets") == 'modify pets in your account'
        openAPI.components.securitySchemes['myOauth2Security'].in == SecurityScheme.In.HEADER
        openAPI.components.securitySchemes['myOauth3Security'].name == null
        openAPI.components.securitySchemes['myOauth4Security'].name == "JWT"
    }
}
