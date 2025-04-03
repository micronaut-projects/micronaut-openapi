package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import spock.lang.Issue
import spock.util.environment.RestoreSystemProperties

class OpenApiMergeSchemaSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test merging of additional OpenAPI schema"() {
        given:
        String additionalSwaggerFilesDir = new File("src/test/resources/swagger/petstore.yml").absolutePath
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES, additionalSwaggerFilesDir)
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "append")

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
        then: "the state is correct"
        Utils.testReference != null

        when: "the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference

        then: "it is included in the OpenAPI doc"
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
        openAPI.security[0] == ["req 1": ["a", "b"]]
        openAPI.security[1] == ["req 2": ["b", "c"]]
        openAPI.servers.size() == 2
        openAPI.servers[0].description == 'server 1'
        openAPI.servers[0].url == 'https://foo'
        openAPI.servers[0].variables.size() == 2
        openAPI.servers[0].variables.var1.description == 'var 1'
        openAPI.servers[0].variables.var1.default == '1'
        openAPI.servers[0].variables.var1.enum == ['1', '2']
        openAPI.servers[1].url == 'https://petstore.swagger.io/v1'
        openAPI.paths.size() == 2

        when:
        Operation operation = openAPI.paths.get("/pets").get

        then:
        operation.tags.size() == 1
        operation.tags[0] == "pets"
        operation.summary == "List all pets"

        when:
        operation = openAPI.paths.get("/pets").post

        then:
        operation.tags.size() == 1
        operation.tags[0] == "pets"
        operation.summary == "Create a pet"

        when:
        operation = openAPI.paths.get("/pets/{petId}").get

        then:
        operation.tags.size() == 1
        operation.tags[0] == "pets"
        operation.summary == "Info for a specific pet"

        when:
        Components components = openAPI.components

        then:
        components.schemas.size() == 3
    }

    @RestoreSystemProperties
    void "test merge with additional openapi files: APPEND"() {
        given:
        String additionalSwaggerFilesDir = "file:" + new File("src/test/resources/swagger").absolutePath
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES, additionalSwaggerFilesDir)
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "append")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.management.endpoint.loggers.LoggersEndpoint;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import reactor.core.publisher.Mono;

@OpenAPIInclude(classes = LoggersEndpoint.class)
@OpenAPIDefinition
class Application {

}

@Controller
interface DefaultApi {

    /**
     * exampleRouteGet
     *
     * @return Mono&lt;String&gt;
     */
    @Get("/example-route")
    Mono<String> exampleRouteGet();
}

@Controller
class ApiImpl implements DefaultApi {

    @Override
    public Mono<String> exampleRouteGet() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/example-route".get
        var opLoggers = openApi.paths."/loggers".get
        var opLoggers2 = openApi.paths."/loggers/{name}".get
        var schemas = openApi.components.schemas

        then:
        opLoggers
        opLoggers2
        op
        op.responses."200".description == "exampleRouteGet 200 response"
        schemas
        schemas.BusinessObject
        schemas.BusinessObject.type == "string"
        openApi.servers
        openApi.servers[0].url == "https://petstore.swagger.io/v1"
        openApi.paths."/pets".get
        openApi.paths."/pets".post
    }

    @RestoreSystemProperties
    void "test merge with additional openapi files: REPLACE"() {
        given:
        String additionalSwaggerFilesDir = new File("src/test/resources/swagger").absolutePath
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES, additionalSwaggerFilesDir)
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "replace")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.management.endpoint.loggers.LoggersEndpoint;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import reactor.core.publisher.Mono;

@OpenAPIInclude(classes = LoggersEndpoint.class)
@OpenAPIDefinition
class Application {

}

@Controller
interface DefaultApi {

    /**
     * exampleRouteGet
     *
     * @return Mono&lt;String&gt;
     */
    @Get("/example-route")
    Mono<String> exampleRouteGet();
}

@Controller
class ApiImpl implements DefaultApi {

    @Override
    public Mono<String> exampleRouteGet() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/example-route".get
        var opLoggers = openApi.paths."/loggers".get
        var opLoggers2 = openApi.paths."/loggers/{name}".get
        var schemas = openApi.components.schemas

        then:
        opLoggers
        opLoggers2
        op
        op.responses."200".description == "this is my unique description"
        schemas
        schemas.BusinessObject
        schemas.BusinessObject.type == "string"
        openApi.servers
        openApi.servers[0].url == "https://petstore.swagger.io/v1"
        openApi.paths."/pets".get
        openApi.paths."/pets".post
    }

    @RestoreSystemProperties
    void "test merge with additional openapi files: REPLACE with classpath"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES, "classpath:/swagger/openapi.yml,classpath:/swagger/petstore.yml")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "replace")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.management.endpoint.loggers.LoggersEndpoint;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import reactor.core.publisher.Mono;

@OpenAPIInclude(classes = LoggersEndpoint.class)
@OpenAPIDefinition
class Application {

}

@Controller
interface DefaultApi {

    /**
     * exampleRouteGet
     *
     * @return Mono&lt;String&gt;
     */
    @Get("/example-route")
    Mono<String> exampleRouteGet();
}

@Controller
class ApiImpl implements DefaultApi {

    @Override
    public Mono<String> exampleRouteGet() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/example-route".get
        var opLoggers = openApi.paths."/loggers".get
        var opLoggers2 = openApi.paths."/loggers/{name}".get
        var schemas = openApi.components.schemas

        then:
        opLoggers
        opLoggers2
        op
        op.responses."200".description == "this is my unique description"
        schemas
        schemas.BusinessObject
        schemas.BusinessObject.type == "string"
        openApi.servers
        openApi.servers[0].url == "https://petstore.swagger.io/v1"
        openApi.paths."/pets".get
        openApi.paths."/pets".post
    }

    @RestoreSystemProperties
    void "test merge with additional openapi files: REPLACE with project"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES, "project:src/test/resources/swagger/openapi.yml,classpath:/swagger/petstore.yml")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "replace")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.management.endpoint.loggers.LoggersEndpoint;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import reactor.core.publisher.Mono;

@OpenAPIInclude(classes = LoggersEndpoint.class)
@OpenAPIDefinition
class Application {

}

@Controller
interface DefaultApi {

    /**
     * exampleRouteGet
     *
     * @return Mono&lt;String&gt;
     */
    @Get("/example-route")
    Mono<String> exampleRouteGet();
}

@Controller
class ApiImpl implements DefaultApi {

    @Override
    public Mono<String> exampleRouteGet() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/example-route".get
        var opLoggers = openApi.paths."/loggers".get
        var opLoggers2 = openApi.paths."/loggers/{name}".get
        var schemas = openApi.components.schemas

        then:
        opLoggers
        opLoggers2
        op
        op.responses."200".description == "this is my unique description"
        schemas
        schemas.BusinessObject
        schemas.BusinessObject.type == "string"
        openApi.servers
        openApi.servers[0].url == "https://petstore.swagger.io/v1"
        openApi.paths."/pets".get
        openApi.paths."/pets".post
    }

    @RestoreSystemProperties
    void "test merge with additional openapi files: APPEND with project and yaml config"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "additional-files")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.management.endpoint.loggers.LoggersEndpoint;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import reactor.core.publisher.Mono;

@OpenAPIInclude(classes = LoggersEndpoint.class)
@OpenAPIDefinition
class Application {

}

@Controller
interface DefaultApi {

    /**
     * exampleRouteGet
     *
     * @return Mono&lt;String&gt;
     */
    @Get("/example-route")
    Mono<String> exampleRouteGet();
}

@Controller
class ApiImpl implements DefaultApi {

    @Override
    public Mono<String> exampleRouteGet() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/example-route".get
        var opLoggers = openApi.paths."/loggers".get
        var opLoggers2 = openApi.paths."/loggers/{name}".get
        var schemas = openApi.components.schemas

        then:
        opLoggers
        opLoggers2
        op
        op.responses."200".description == "exampleRouteGet 200 response"
        schemas
        schemas.BusinessObject
        schemas.BusinessObject.type == "string"
        openApi.servers
        openApi.servers[0].url == "https://petstore.swagger.io/v1"
        openApi.paths."/pets".get
        openApi.paths."/pets".post
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/2070")
    @RestoreSystemProperties
    void "test merge with additional openapi file path with different methods: APPEND mode"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "additional-files2")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;

@Controller
class DemoController {

  @Get("/resources/{id}")
  public String getResource(@PathVariable String id) {
    return "The resource id is: " + id;
  }

}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var pathItem = openApi.paths."/resources/{id}"

        then:
        openApi
        pathItem.get
        pathItem.get.parameters
        pathItem.get.parameters[0].name == 'id'

        pathItem.options
        pathItem.options.operationId == 'resourcesOptions'
        pathItem.options.responses
        pathItem.options.responses."200"
        pathItem.options.responses."200".headers.size() == 4
    }

    @RestoreSystemProperties
    void "test merge with additional openapi file path with different methods: REPLACE mode"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "additional-files2")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "replace")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;

@Controller
class DemoController {

  @Get("/resources/{id}")
  public String getResource(@PathVariable String id) {
    return "The resource id is: " + id;
  }

}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var pathItem = openApi.paths."/resources/{id}"

        then:
        openApi
        pathItem.get
        pathItem.get.parameters
        pathItem.get.parameters[0].name == 'id'

        pathItem.options
        pathItem.options.operationId == 'resourcesOptions'
        pathItem.options.responses
        pathItem.options.responses."200"
        pathItem.options.responses."200".headers.size() == 4
    }

    @RestoreSystemProperties
    void "test merge with additional openapi file path with different content types: APPEND mode"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "additional-files3")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;

@Controller
class DemoController {

  @Post("/resources/{id}")
  public String getResource(@PathVariable String id, @Body String body) {
    return "The resource id is: " + id;
  }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/resources/{id}".post

        then:
        op
        op.requestBody.content."application/json".schema.type == "string"
        !op.requestBody.content."application/json".schema.properties
        op.requestBody.content."text/xml".schema.properties
        op.requestBody.content."text/xml".schema.properties.prop1
        op.requestBody.content."text/xml".schema.properties.prop2

        op.responses."200".content."application/json".schema.type == "string"
        !op.responses."200".content."application/json".schema.properties
        op.responses."200".content."text/xml".schema.properties
        op.responses."200".content."text/xml".schema.properties.prop1
        op.responses."200".content."text/xml".schema.properties.prop2
    }

    @RestoreSystemProperties
    void "test merge with additional openapi file path with different content types: REPLACE mode"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "additional-files3")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADDITIONAL_FILES_MERGE_MODE, "replace")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;

@Controller
class DemoController {

  @Post("/resources/{id}")
  public String getResource(@PathVariable String id, @Body String body) {
    return "The resource id is: " + id;
  }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/resources/{id}".post

        then:
        op
        op.requestBody.content."application/json".schema.properties
        op.requestBody.content."application/json".schema.properties.prop1
        op.requestBody.content."application/json".schema.properties.prop2
        op.requestBody.content."text/xml".schema.properties
        op.requestBody.content."text/xml".schema.properties.prop1
        op.requestBody.content."text/xml".schema.properties.prop2

        op.responses."200".content."application/json".schema.properties
        op.responses."200".content."application/json".schema.properties.prop1
        op.responses."200".content."application/json".schema.properties.prop2
        op.responses."200".content."text/xml".schema.properties
        op.responses."200".content."text/xml".schema.properties.prop1
        op.responses."200".content."text/xml".schema.properties.prop2
    }

    @RestoreSystemProperties
    void "test merge with additional openapi file path with different content types: APPEND mode2"() {

        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "additional-files3")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;

@Controller
class DemoController {

  @Post(value = "/resources/{id}", consumes = MediaType.TEXT_XML, produces = MediaType.TEXT_XML)
  public String getResource(@PathVariable String id, @Body String body) {
    return "The resource id is: " + id;
  }

  @Post("/resources/{id}")
  public String getResource2(@PathVariable String id, @Body String body) {
    return "The resource id is: " + id;
  }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference

        when:
        var openApi = Utils.testReference
        var op = openApi.paths."/resources/{id}".post

        then:
        op
        op.requestBody.content."application/json".schema.type == "string"
        !op.requestBody.content."application/json".schema.properties
        op.requestBody.content."text/xml".schema.type == "string"
        !op.requestBody.content."text/xml".schema.properties

        op.responses."200".content."application/json".schema.type == "string"
        !op.responses."200".content."application/json".schema.properties
        op.responses."200".content."text/xml".schema.type == "string"
        !op.responses."200".content."text/xml".schema.properties
    }
}
