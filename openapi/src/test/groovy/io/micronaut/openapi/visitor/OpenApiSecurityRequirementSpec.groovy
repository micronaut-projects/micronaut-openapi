package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityScheme

class OpenApiSecurityRequirementSpec extends AbstractOpenApiTypeElementSpec {

    void "test global @SecurityRequirement override with empty array - Issue #212"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;
import io.micronaut.runtime.Micronaut;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
@Controller("/pets")
class PetController {
    @Operation(summary = "get Pet by name", security = {})
    @ApiResponse(
            responseCode = "200",
            description = "Returns a Pet information",
            content = @Content(schema = @Schema(implementation = Pet.class)))
    @Get("/{name}")
    public HttpResponse<Pet> get(String name) {
        return null;
    }
}
@OpenAPIDefinition(
        info = @Info(
                title = "openapi-demo",
                version = "0.1"
        )
)
@SecurityScheme(name = "X-API-Key",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER
)
@SecurityRequirement(name = "X-API-Key")
class Application {
}
class Pet {
    private String name;
    private int age;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.getSecurity().size() == 1
        openAPI.getSecurity().get(0).containsKey("X-API-Key")
        openAPI.getSecurity().get(0).get("X-API-Key") == []

        when:
        Operation operation = openAPI.getPaths().get("/pets/{name}").get

        then:
        operation
        operation.responses.size() == 1
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'200'.content['application/json'].schema.$ref == "#/components/schemas/Pet"
        operation.security != null
        operation.security == []

    }

    void "test parse the OpenAPI global @SecurityRequirement"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.runtime.Micronaut;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Controller("/pets")
class PetController {

    @ApiResponse(
            responseCode = "200",
            description = "Returns a Pet information",
            content = @Content(schema = @Schema(implementation = Pet.class)))
    @Get("/{name}")
    public HttpResponse<Pet> get(String name) {
        return null;
    }

}

@OpenAPIDefinition(
        info = @Info(
                title = "openapi-demo",
                version = "0.1"
        )
)
@SecurityScheme(name = "X-API-Key",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER
)
@SecurityRequirement(name = "X-API-Key")
class Application {
}

class Pet {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.getSecurity().size() == 1
        openAPI.getSecurity().get(0).containsKey("X-API-Key")
        openAPI.getSecurity().get(0).get("X-API-Key") == []

        when:
        Operation operation = openAPI.getPaths().get("/pets/{name}").get

        then:
        operation
        operation.responses.size() == 1
        operation.responses.'200'.content.size() == 1
        operation.responses.'200'.content['application/json'].schema
        operation.responses.'200'.content['application/json'].schema.$ref == "#/components/schemas/Pet"

    }

    void "test parse the OpenAPI @Operation annotation with @SecurityScheme for APIKEY"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
@SecurityScheme(name = "myOauth2Security",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER
)
class MyController {

    @Put("/")
    @Consumes("application/json")
    @SecurityRequirement(name = "myOauth2Security", scopes = "write: read")
    public Response updatePet(
      @RequestBody(description = "Pet object that needs to be added to the store", required = true,
                              content = @Content(
                                      schema = @Schema(implementation = Pet.class))) Pet pet) {
        return null;
    }
}

class Pet {}

class Response {}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.security.size() == 1
        operation.security.get(0).containsKey('myOauth2Security')
        operation.security.get(0).get('myOauth2Security') == ['write: read']
        openAPI.components.securitySchemes.size() == 1
        openAPI.components.securitySchemes['myOauth2Security'].type == SecurityScheme.Type.APIKEY
        openAPI.components.securitySchemes['myOauth2Security'].in == SecurityScheme.In.HEADER

    }

    void "test parse the OpenAPI @Operation annotation with @SecurityScheme for OAUTH2"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.extensions.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
@SecurityScheme(name = "myOauth2Security",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                implicit = @OAuthFlow(authorizationUrl = "https://url.com/auth",
                        scopes = @OAuthScope(name = "write:pets", description = "modify pets in your account"))),
        extensions = @Extension(
            name = "custom",
            properties = {
                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
             }
        )
)
class MyController {

    @Put("/")
    @Consumes("application/json")
    @SecurityRequirement(name = "myOauth2Security", scopes = "write: read")
    public Response updatePet(
      @RequestBody(description = "Pet object that needs to be added to the store", required = true,
                              content = @Content(
                                      schema = @Schema(implementation = Pet.class))) Pet pet) {
        return null;
    }
}

class Pet {}

class Response {}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.security.size() == 1
        operation.security.get(0).containsKey('myOauth2Security')
        operation.security.get(0).get('myOauth2Security') == ['write: read']
        openAPI.components.securitySchemes.size() == 1
        openAPI.components.securitySchemes['myOauth2Security'].type == SecurityScheme.Type.OAUTH2
        openAPI.components.securitySchemes['myOauth2Security'].flows
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.authorizationUrl == 'https://url.com/auth'
        openAPI.components.securitySchemes['myOauth2Security'].flows.implicit.scopes
        openAPI.components.securitySchemes['myOauth2Security'].extensions
        openAPI.components.securitySchemes['myOauth2Security'].extensions.'x-custom'.prop1 == "prop1Val"

    }
}
