package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import spock.util.environment.RestoreSystemProperties

class OpenApiMergeSchemaSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test merging of additional OpenAPI schema"() {
        given:
        String additionalSwaggerFilesDir= new File("src/test/resources/swagger").absolutePath
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ADDITIONAL_FILES, additionalSwaggerFilesDir)

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

        cleanup:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ADDITIONAL_FILES, "")
    }
}
