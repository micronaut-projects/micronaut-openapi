package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths

class OpenApiPropsFromEnvSpec extends AbstractOpenApiTypeElementSpec {

    void "test requires env for controller"() {

        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "local")

        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;

@Controller
class Controller1 {

    @Post("${props-from-env-test.paths.my-controller.post}")
    public String post() {
        return null;
    }

    @Get("${props-from-env-test.paths.my-controller.get}")
    public String get() {
        return null;
    }

    @Put("${props-from-env-test.paths.my-controller.put}")
    public String put() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Paths paths = Utils.testReference?.paths

        then:
        paths
        paths.size() == 2
        paths."/this/is/path/from/default/env"
        paths."/this/is/path/from/default/env".post
        paths."/this/is/path/from/local/env"
        paths."/this/is/path/from/local/env".get
        paths."/this/is/path/from/local/env".put

        cleanup:
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS)
    }

    void "test disabled micronaut environments"() {

        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_ENVIRONMENT_ENABLED, "false")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "local")
        System.setProperty("props-from-env-test.paths.my-controller.get", "myGet")

        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;

@Controller
class Controller1 {

    @Post("${props-from-env-test.paths.my-controller.post}")
    public String post() {
        return null;
    }

    @Get("${props-from-env-test.paths.my-controller.get}")
    public String get() {
        return null;
    }

    @Put("${props-from-env-test.paths.my-controller.put}")
    public String put() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Paths paths = Utils.testReference?.paths

        then:
        paths
        paths.size() == 3
        paths.'/${props-from-env-test.paths.my-controller.post}'
        paths.'/${props-from-env-test.paths.my-controller.post}'.post
        !paths.'/${props-from-env-test.paths.my-controller.get}'
        paths.'/myGet'
        paths.'/myGet'.get
        paths.'/${props-from-env-test.paths.my-controller.put}'
        paths.'/${props-from-env-test.paths.my-controller.put}'.put

        cleanup:
        System.clearProperty("props-from-env-test.paths.my-controller.get")
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_ENVIRONMENT_ENABLED)
    }

    void "test expanded properties with environments"() {

        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-controller-cutom-uri.properties")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "local2")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.security.endpoints.LoginController;
import io.micronaut.security.endpoints.LogoutController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIInclude(value = LoginController.class,
    uri = "${login.placeholder}",
    tags = @Tag(name = "Tag 4"),
    security = @SecurityRequirement(name = "req 3", scopes = {"b", "c"})
)
@OpenAPIInclude(value = LogoutController.class,
    uri = "${env-logout.placeholder}",
    tags = @Tag(name = "Tag 5"),
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
        Utils.testReferenceAfterPlaceholders != null

        when:
        OpenAPI openAPI = Utils.testReferenceAfterPlaceholders

        PathItem helloPathItem = openAPI.paths."/hello"
        PathItem loginPathItem = openAPI.paths."/myLoginUrl"
        PathItem logoutPathItem = openAPI.paths."/fromEnvLogoutUrl"

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

        logoutPathItem.post.operationId == 'index'
        logoutPathItem.post.tags[0] == "Tag 5"
        logoutPathItem.post.security[0]["req 3"]
        logoutPathItem.post.responses['200'].content['application/json'].schema['$ref'] == '#/components/schemas/Object'

        logoutPathItem.get.operationId == 'indexGet'
        logoutPathItem.get.tags[0] == "Tag 5"
        logoutPathItem.get.security[0]["req 3"]
        logoutPathItem.get.responses['200'].content['application/json'].schema['$ref'] == '#/components/schemas/Object'

        openAPI.components.schemas['UsernamePasswordCredentials']
        openAPI.components.schemas['UsernamePasswordCredentials'].required.size() == 2
        openAPI.components.schemas['UsernamePasswordCredentials'].properties['username']
        openAPI.components.schemas['UsernamePasswordCredentials'].properties['password']

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE)
    }

}
