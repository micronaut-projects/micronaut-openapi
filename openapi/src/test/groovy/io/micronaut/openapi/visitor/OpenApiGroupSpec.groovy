package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.security.SecurityScheme
import spock.util.environment.RestoreSystemProperties

class OpenApiGroupSpec extends AbstractOpenApiTypeElementSpec {

    void "test group security schemes"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import io.micronaut.openapi.annotation.OpenAPIGroupInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIGroup("private")
@Controller
class MyController {

    @OpenAPIGroup("private")
    @Get("/id/{id}")
    String get(String id) {
        return null;
    }

    @OpenAPIGroup("public")
    @Get("/name/{name}")
    String getByName(String name) {
        return null;
    }

    // common
    @Get("/all")
    String getAll() {
        return null;
    }
}

@OpenAPIGroupInfo(
  names = "private",
  info = @OpenAPIDefinition(
    info = @Info(
      title = "Private api"
    )
  )
)
@OpenAPIGroupInfo(
  names = "public",
  info = @OpenAPIDefinition(
    info = @Info(
      title = "Public api"
    ),
    security = {
      @SecurityRequirement(name = "authorizer")
    }
  ),
  securitySchemes = {
    @SecurityScheme(
      name = "authorizer",
      type = SecuritySchemeType.APIKEY,
      paramName = "Authorization"
    )
  }
)
@SecurityScheme(
  name = "common",
  type = SecuritySchemeType.HTTP,
  scheme = "basic",
  in = SecuritySchemeIn.HEADER
)
class Application {
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        def openApis = Utils.testReferences
        openApis
        openApis.size() == 2

        def apiPrivate = openApis.get(Pair.of("private", null)).getOpenApi()
        def apiPublic = openApis.get(Pair.of("public", null)).getOpenApi()

        apiPrivate.components.securitySchemes
        apiPrivate.components.securitySchemes.size() == 1
        apiPrivate.components.securitySchemes.common
        apiPrivate.components.securitySchemes.common.type == SecurityScheme.Type.HTTP

        apiPublic.components.securitySchemes
        apiPublic.components.securitySchemes.size() == 2
        apiPublic.components.securitySchemes.common
        apiPublic.components.securitySchemes.common.type == SecurityScheme.Type.HTTP
        apiPublic.components.securitySchemes.authorizer
        apiPublic.components.securitySchemes.authorizer.type == SecurityScheme.Type.APIKEY
    }

    void "test group specific operation extensions"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import io.micronaut.openapi.annotation.OpenAPIGroupInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Controller
class MyController {

    @OpenAPIGroup(
            names = "private",
            extensions = {
                @Extension(name = "amazon-apigateway-integration", properties = {
                    @ExtensionProperty(name = "uri", value = "${amz.private.lambda-integration.uri}"),
                    @ExtensionProperty(name = "httpMethod", value = "${amz.private.lambda-integration.http-method}")
                }),
                @Extension(name = "google-apigateway-integration", properties = {
                    @ExtensionProperty(name = "uri", value = "${google.private.lambda-integration.uri}"),
                    @ExtensionProperty(name = "httpMethod", value = "${google.private.lambda-integration.http-method}")
                })
            }
    )
    @OpenAPIGroup(
            names = "public",
            extensions = {
                @Extension(name = "amazon-apigateway-integration", properties = {
                    @ExtensionProperty(name = "uri", value = "${amz.public.lambda-integration.uri}"),
                    @ExtensionProperty(name = "httpMethod", value = "${amz.public.lambda-integration.http-method}")
                }),
                @Extension(name = "google-apigateway-integration", properties = {
                    @ExtensionProperty(name = "uri", value = "${google.public.lambda-integration.uri}"),
                    @ExtensionProperty(name = "httpMethod", value = "${google.public.lambda-integration.http-method}")
                })
            }
    )
    @Get("/id/{id}")
    String get(String id) {
        return null;
    }

    @OpenAPIGroup("public")
    @Get("/name/{name}")
    String getByName(String name) {
        return null;
    }

    // common
    @Get("/all")
    String getAll() {
        return null;
    }
}

@OpenAPIGroupInfo(
  names = "private",
  info = @OpenAPIDefinition(
    info = @Info(
      title = "Private api"
    )
  )
)
@OpenAPIGroupInfo(
  names = "public",
  info = @OpenAPIDefinition(
    info = @Info(
      title = "Public api"
    )
  )
)
@SecurityScheme(
  name = "common",
  type = SecuritySchemeType.HTTP,
  scheme = "basic",
  in = SecuritySchemeIn.HEADER
)
class Application {
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        def openApis = Utils.testReferences
        openApis
        openApis.size() == 2

        def apiPrivate = openApis.get(Pair.of("private", null)).getOpenApi()
        def apiPublic = openApis.get(Pair.of("public", null)).getOpenApi()

        def opPrivateExt = apiPrivate.paths.'/id/{id}'.get.extensions
        def opPublicExt = apiPublic.paths.'/id/{id}'.get.extensions

        opPrivateExt
        opPrivateExt.size() == 2
        opPrivateExt.'x-amazon-apigateway-integration'
        ((Map<String, String>) opPrivateExt.'x-amazon-apigateway-integration').uri == '${amz.private.lambda-integration.uri}'
        ((Map<String, String>) opPrivateExt.'x-amazon-apigateway-integration').httpMethod == '${amz.private.lambda-integration.http-method}'
        opPrivateExt.'x-google-apigateway-integration'
        ((Map<String, String>) opPrivateExt.'x-google-apigateway-integration').uri == '${google.private.lambda-integration.uri}'
        ((Map<String, String>) opPrivateExt.'x-google-apigateway-integration').httpMethod == '${google.private.lambda-integration.http-method}'

        opPublicExt
        opPublicExt.size() == 2
        opPublicExt.'x-amazon-apigateway-integration'
        ((Map<String, String>) opPublicExt.'x-amazon-apigateway-integration').uri == '${amz.public.lambda-integration.uri}'
        ((Map<String, String>) opPublicExt.'x-amazon-apigateway-integration').httpMethod == '${amz.public.lambda-integration.http-method}'
        opPublicExt.'x-google-apigateway-integration'
        ((Map<String, String>) opPublicExt.'x-google-apigateway-integration').uri == '${google.public.lambda-integration.uri}'
        ((Map<String, String>) opPublicExt.'x-google-apigateway-integration').httpMethod == '${google.public.lambda-integration.http-method}'
    }

    @RestoreSystemProperties
    void "test group properties"() {

        given:
        def group1Title = "Private API"
        def group2Title = "Public API"
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS + ".private.display-name", group1Title)
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS + ".private.common-exclude", "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS + ".private.adoc-enabled", "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS + ".private.adoc-filename", "adocFile.adoc")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS + ".public.display-name", group2Title)

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIGroup;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Controller
class MyController {

    @OpenAPIGroup("private")
    @Get("/id/{id}")
    String get(String id) {
        return null;
    }

    @OpenAPIGroup("public")
    @Get("/name/{name}")
    String getByName(String name) {
        return null;
    }

    // common
    @Get("/all")
    String getAll() {
        return null;
    }
}

@OpenAPIDefinition(
    info = @Info(
        title = "Title My API",
        version = "0.0",
        description = "My API"
    )
)
class Application {
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        def openApis = Utils.testReferences
        openApis
        openApis.size() == 2

        def apiPrivate = openApis.get(Pair.of("private", null)).getOpenApi()
        def apiPublic = openApis.get(Pair.of("public", null)).getOpenApi()

        apiPrivate.paths.size() == 1
        apiPublic.paths.size() == 2
    }
}
