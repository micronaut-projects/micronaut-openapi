package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.security.SecurityScheme

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
}
