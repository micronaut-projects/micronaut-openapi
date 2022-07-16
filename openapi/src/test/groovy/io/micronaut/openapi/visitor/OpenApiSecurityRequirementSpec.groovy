package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.SecurityScheme
import spock.lang.Issue

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
        OpenAPI openAPI = Utils.testReference

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
        OpenAPI openAPI = Utils.testReference

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

        OpenAPI openAPI = Utils.testReference
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

import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.List;

@Controller
@SecurityScheme(
        name = "myOauth2Security",
        type = SecuritySchemeType.OAUTH2,
        scheme = "bearer",
        bearerFormat = "JWT",
        openIdConnectUrl = "https://openid.com/connect",
        description = "Security scheme description",
        in = SecuritySchemeIn.HEADER,

        flows = @OAuthFlows(
                implicit = @OAuthFlow(
                        authorizationUrl = "https://url.com/auth",
                        refreshUrl = "https://url.com/refresh",
                        tokenUrl = "https://url.com/token",
                        scopes = {
                                @OAuthScope(name = "write:pets", description = "modify pets in your account"),
                                @OAuthScope(name = "read:pets", description = "Read pets in your account")
                        },
                        extensions = {
                                @Extension(
                                        name = "myExt1",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                        }
                                ),
                                @Extension(
                                        name = "myExt2",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                        }
                                ),
                        }
                ),
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "https://authcode.com/auth",
                        refreshUrl = "https://authcode.com/refresh",
                        tokenUrl = "https://authcode.com/token",
                        scopes = {
                                @OAuthScope(name = "write:pets", description = "modify pets in your account"),
                                @OAuthScope(name = "read:pets", description = "Read pets in your account")
                        },
                        extensions = {
                                @Extension(
                                        name = "myExt1",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                        }
                                ),
                                @Extension(
                                        name = "myExt2",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                        }
                                ),
                        }
                ),
                password = @OAuthFlow(
                        authorizationUrl = "https://password.com/auth",
                        refreshUrl = "https://password.com/refresh",
                        tokenUrl = "https://password.com/token",
                        scopes = {
                                @OAuthScope(name = "write:pets", description = "modify pets in your account"),
                                @OAuthScope(name = "read:pets", description = "Read pets in your account")
                        },
                        extensions = {
                                @Extension(
                                        name = "myExt1",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                        }
                                ),
                                @Extension(
                                        name = "myExt2",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                        }
                                ),
                        }
                ),
                clientCredentials = @OAuthFlow(
                        authorizationUrl = "https://clientcred.com/auth",
                        refreshUrl = "https://clientcred.com/refresh",
                        tokenUrl = "https://clientcred.com/token",
                        scopes = {
                                @OAuthScope(name = "write:pets", description = "modify pets in your account"),
                                @OAuthScope(name = "read:pets", description = "Read pets in your account")
                        },
                        extensions = {
                                @Extension(
                                        name = "myExt1",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                        }
                                ),
                                @Extension(
                                        name = "myExt2",
                                        properties = {
                                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                        }
                                ),
                        }
                ),
                extensions = {
                        @Extension(
                                name = "custom1",
                                properties = {
                                        @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                        @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                }
                        ),
                        @Extension(
                                name = "custom2",
                                properties = {
                                        @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                        @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                }
                        ),
                }
        ),
        extensions = {
                @Extension(
                        name = "custom1",
                        properties = {
                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                        }
                ),
                @Extension(
                        name = "custom2",
                        properties = {
                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                        }
                ),
        }
)
class MyController {

    @Put
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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.security.size() == 1
        operation.security.get(0).containsKey('myOauth2Security')
        operation.security.get(0).get('myOauth2Security') == ['write: read']
        openAPI.components.securitySchemes.size() == 1
        def securityScheme = openAPI.components.securitySchemes['myOauth2Security']
        securityScheme.type == SecurityScheme.Type.OAUTH2
        securityScheme.scheme == 'bearer'
        securityScheme.openIdConnectUrl == "https://openid.com/connect"
        securityScheme.bearerFormat == 'JWT'
        securityScheme.description == 'Security scheme description'
        securityScheme.in == SecurityScheme.In.HEADER

        securityScheme.extensions
        securityScheme.extensions.size() == 2
        securityScheme.extensions.'x-custom1'.prop1 == "prop1Val"
        securityScheme.extensions.'x-custom1'.prop2 == "prop2Val"
        securityScheme.extensions.'x-custom2'.prop1 == "prop1Val1"
        securityScheme.extensions.'x-custom2'.prop2 == "prop2Val2"

        securityScheme.flows

        OAuthFlow flowImplicit = securityScheme.flows.implicit

        flowImplicit
        flowImplicit.authorizationUrl == 'https://url.com/auth'
        flowImplicit.refreshUrl == 'https://url.com/refresh'
        flowImplicit.tokenUrl == 'https://url.com/token'
        flowImplicit.scopes
        flowImplicit.scopes.size() == 2
        flowImplicit.scopes."write:pets" == "modify pets in your account"
        flowImplicit.scopes."read:pets" == "Read pets in your account"

        flowImplicit.extensions
        flowImplicit.extensions.size() == 2
        flowImplicit.extensions.'x-myExt1'.prop1 == "prop1Val"
        flowImplicit.extensions.'x-myExt1'.prop2 == "prop2Val"
        flowImplicit.extensions.'x-myExt2'.prop1 == "prop1Val1"
        flowImplicit.extensions.'x-myExt2'.prop2 == "prop2Val2"

        OAuthFlow flowAuthCode = securityScheme.flows.authorizationCode

        flowAuthCode
        flowAuthCode.authorizationUrl == 'https://authcode.com/auth'
        flowAuthCode.refreshUrl == 'https://authcode.com/refresh'
        flowAuthCode.tokenUrl == 'https://authcode.com/token'
        flowAuthCode.scopes
        flowAuthCode.scopes.size() == 2
        flowAuthCode.scopes."write:pets" == "modify pets in your account"
        flowAuthCode.scopes."read:pets" == "Read pets in your account"

        flowAuthCode.extensions
        flowAuthCode.extensions.size() == 2
        flowAuthCode.extensions.'x-myExt1'.prop1 == "prop1Val"
        flowAuthCode.extensions.'x-myExt1'.prop2 == "prop2Val"
        flowAuthCode.extensions.'x-myExt2'.prop1 == "prop1Val1"
        flowAuthCode.extensions.'x-myExt2'.prop2 == "prop2Val2"

        OAuthFlow flowPassword = securityScheme.flows.password

        flowPassword
        flowPassword.authorizationUrl == 'https://password.com/auth'
        flowPassword.refreshUrl == 'https://password.com/refresh'
        flowPassword.tokenUrl == 'https://password.com/token'
        flowPassword.scopes
        flowPassword.scopes.size() == 2
        flowPassword.scopes."write:pets" == "modify pets in your account"
        flowPassword.scopes."read:pets" == "Read pets in your account"

        flowPassword.extensions
        flowPassword.extensions.size() == 2
        flowPassword.extensions.'x-myExt1'.prop1 == "prop1Val"
        flowPassword.extensions.'x-myExt1'.prop2 == "prop2Val"
        flowPassword.extensions.'x-myExt2'.prop1 == "prop1Val1"
        flowPassword.extensions.'x-myExt2'.prop2 == "prop2Val2"

        OAuthFlow flowClientCred = securityScheme.flows.clientCredentials

        flowClientCred
        flowClientCred.authorizationUrl == 'https://clientcred.com/auth'
        flowClientCred.refreshUrl == 'https://clientcred.com/refresh'
        flowClientCred.tokenUrl == 'https://clientcred.com/token'
        flowClientCred.scopes
        flowClientCred.scopes.size() == 2
        flowClientCred.scopes."write:pets" == "modify pets in your account"
        flowClientCred.scopes."read:pets" == "Read pets in your account"

        flowClientCred.extensions
        flowClientCred.extensions.size() == 2
        flowClientCred.extensions.'x-myExt1'.prop1 == "prop1Val"
        flowClientCred.extensions.'x-myExt1'.prop2 == "prop2Val"
        flowClientCred.extensions.'x-myExt2'.prop1 == "prop1Val1"
        flowClientCred.extensions.'x-myExt2'.prop2 == "prop2Val2"
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/633")
    void "test @SecurityScheme always sets 'name' property"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.*;

@OpenAPIDefinition(
        info = @Info(
                title = "openapi-demo",
                version = "0.1"
        )
)
@SecurityScheme(
        type = SecuritySchemeType.APIKEY,
        name = "MyScheme",
        in = SecuritySchemeIn.HEADER
)
class Application {
}

@jakarta.inject.Singleton
class MyBean {}
''')

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.securitySchemes.size() == 1
        openAPI.components.securitySchemes['MyScheme']
        openAPI.components.securitySchemes['MyScheme'].name == 'MyScheme'
    }
}
