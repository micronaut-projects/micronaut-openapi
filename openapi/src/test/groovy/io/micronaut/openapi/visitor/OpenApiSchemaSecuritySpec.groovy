package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme

class OpenApiSchemaSecuritySpec extends AbstractOpenApiTypeElementSpec {

    void "test map micronaut security settings to OpenAPI with SecurityScheme"() {
        given:
        System.setProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "security")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API"
        )
)
@SecurityScheme(
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        name = "my-schema",
        description = "Token without bearer"
)
class Application {
}

@Controller
class OpenApiController {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/private")
    public MyDto privateEndpoint() {
        return null;
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/public")
    public MyDto publicEndpoint() {
        return null;
    }

    @Secured(SecurityRule.DENY_ALL)
    @Get("/denyAll")
    public MyDto denyAll() {
        return null;
    }

    @Get("/fromfile/endpoint")
    public MyDto fromFile() {
        return null;
    }

    @Put("/from-file2/endpoint")
    public MyDto putFromFile() {
        return null;
    }

    @Get("/from-file2/endpointGet")
    public MyDto getFromFile() {
        return null;
    }

    @Secured({"myRole1", "myRole2"})
    @Get("/privateRoles")
    public MyDto privateEndpointWithRoles() {
        return null;
    }
}

class MyDto {

    public String field;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        SecurityScheme secSchema = openAPI.components.securitySchemes."my-schema"

        then:

        openAPI.paths."/private".get.security != null
        openAPI.paths."/private".get.security.size() == 1
        openAPI.paths."/private".get.security.get(0)."my-schema" != null
        openAPI.paths."/private".get.security.get(0)."my-schema".isEmpty()

        openAPI.paths."/public".get.security == null
        openAPI.paths."/denyAll".get.security == null

        openAPI.paths."/fromfile/endpoint".get.security != null
        openAPI.paths."/fromfile/endpoint".get.security.size() == 1
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."my-schema" != null
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."my-schema".isEmpty()

        openAPI.paths."/from-file2/endpointGet".get.security == null

        openAPI.paths."/from-file2/endpoint".put.security != null
        openAPI.paths."/from-file2/endpoint".put.security.size() == 1
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."my-schema" != null
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."my-schema".size() == 2
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."my-schema".contains("role1")
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."my-schema".contains("role2")

        openAPI.paths."/privateRoles".get.security != null
        openAPI.paths."/privateRoles".get.security.size() == 1
        openAPI.paths."/privateRoles".get.security.get(0)."my-schema" != null
        openAPI.paths."/privateRoles".get.security.get(0)."my-schema".size() == 2
        openAPI.paths."/privateRoles".get.security.get(0)."my-schema".contains("myRole1")
        openAPI.paths."/privateRoles".get.security.get(0)."my-schema".contains("myRole2")

        cleanup:
        System.clearProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }

    void "test map micronaut security settings to OpenAPI without SecurityScheme"() {
        given:
        System.setProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "security")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API"
        )
)
class Application {
}

@Controller
class OpenApiController {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/private")
    public MyDto privateEndpoint() {
        return null;
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/public")
    public MyDto publicEndpoint() {
        return null;
    }

    @Secured(SecurityRule.DENY_ALL)
    @Get("/denyAll")
    public MyDto denyAll() {
        return null;
    }

    @Get("/fromfile/endpoint")
    public MyDto fromFile() {
        return null;
    }

    @Put("/from-file2/endpoint")
    public MyDto putFromFile() {
        return null;
    }

    @Get("/from-file2/endpointGet")
    public MyDto getFromFile() {
        return null;
    }

    @Secured({"myRole1", "myRole2"})
    @Get("/privateRoles")
    public MyDto privateEndpointWithRoles() {
        return null;
    }
}

class MyDto {

    public String field;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        SecurityScheme secSchema = openAPI.components.securitySchemes."Authorization"

        then:

        openAPI.paths."/private".get.security != null
        openAPI.paths."/private".get.security.size() == 1
        openAPI.paths."/private".get.security.get(0)."Authorization" != null
        openAPI.paths."/private".get.security.get(0)."Authorization".isEmpty()

        openAPI.paths."/public".get.security == null
        openAPI.paths."/denyAll".get.security == null

        openAPI.paths."/fromfile/endpoint".get.security != null
        openAPI.paths."/fromfile/endpoint".get.security.size() == 1
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."Authorization" != null
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."Authorization".isEmpty()

        openAPI.paths."/from-file2/endpointGet".get.security == null

        openAPI.paths."/from-file2/endpoint".put.security != null
        openAPI.paths."/from-file2/endpoint".put.security.size() == 1
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization" != null
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".size() == 2
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".contains("role1")
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".contains("role2")

        openAPI.paths."/privateRoles".get.security != null
        openAPI.paths."/privateRoles".get.security.size() == 1
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization" != null
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization".size() == 2
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization".contains("myRole1")
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization".contains("myRole2")

        cleanup:
        System.clearProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }

    void "test map micronaut security settings to OpenAPI with custm schema name"() {
        given:
        System.setProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(ConfigProperty.MICRONAUT_OPENAPI_SECURITY_DEFAULT_SCHEMA_NAME, "customSchema")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "security")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API"
        )
)
class Application {
}

@Controller
class OpenApiController {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/private")
    public MyDto privateEndpoint() {
        return null;
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/public")
    public MyDto publicEndpoint() {
        return null;
    }

    @Secured(SecurityRule.DENY_ALL)
    @Get("/denyAll")
    public MyDto denyAll() {
        return null;
    }

    @Get("/fromfile/endpoint")
    public MyDto fromFile() {
        return null;
    }

    @Put("/from-file2/endpoint")
    public MyDto putFromFile() {
        return null;
    }

    @Get("/from-file2/endpointGet")
    public MyDto getFromFile() {
        return null;
    }

    @Secured({"myRole1", "myRole2"})
    @Get("/privateRoles")
    public MyDto privateEndpointWithRoles() {
        return null;
    }
}

class MyDto {

    public String field;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        SecurityScheme secSchema = openAPI.components.securitySchemes."customSchema"

        then:

        openAPI.paths."/private".get.security != null
        openAPI.paths."/private".get.security.size() == 1
        openAPI.paths."/private".get.security.get(0)."customSchema" != null
        openAPI.paths."/private".get.security.get(0)."customSchema".isEmpty()

        openAPI.paths."/public".get.security == null
        openAPI.paths."/denyAll".get.security == null

        openAPI.paths."/fromfile/endpoint".get.security != null
        openAPI.paths."/fromfile/endpoint".get.security.size() == 1
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."customSchema" != null
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."customSchema".isEmpty()

        openAPI.paths."/from-file2/endpointGet".get.security == null

        openAPI.paths."/from-file2/endpoint".put.security != null
        openAPI.paths."/from-file2/endpoint".put.security.size() == 1
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."customSchema" != null
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."customSchema".size() == 2
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."customSchema".contains("role1")
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."customSchema".contains("role2")

        openAPI.paths."/privateRoles".get.security != null
        openAPI.paths."/privateRoles".get.security.size() == 1
        openAPI.paths."/privateRoles".get.security.get(0)."customSchema" != null
        openAPI.paths."/privateRoles".get.security.get(0)."customSchema".size() == 2
        openAPI.paths."/privateRoles".get.security.get(0)."customSchema".contains("myRole1")
        openAPI.paths."/privateRoles".get.security.get(0)."customSchema".contains("myRole2")

        cleanup:
        System.clearProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(ConfigProperty.MICRONAUT_OPENAPI_SECURITY_DEFAULT_SCHEMA_NAME)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }

    void "test map micronaut security settings to OpenAPI with class level annotation"() {
        given:
        System.setProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "security")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API"
        )
)
class Application {
}

@Secured({"global-role1", "global-role2"})
@Controller
class OpenApiController {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/private")
    public MyDto privateEndpoint() {
        return null;
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/public")
    public MyDto publicEndpoint() {
        return null;
    }

    @Secured(SecurityRule.DENY_ALL)
    @Get("/denyAll")
    public MyDto denyAll() {
        return null;
    }

    @Get("/fromfile/endpoint")
    public MyDto fromFile() {
        return null;
    }

    @Put("/from-file2/endpoint")
    public MyDto putFromFile() {
        return null;
    }

    @Get("/from-file2/endpointGet")
    public MyDto getFromFile() {
        return null;
    }

    @Secured({"myRole1", "myRole2"})
    @Get("/privateRoles")
    public MyDto privateEndpointWithRoles() {
        return null;
    }
}

class MyDto {

    public String field;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        SecurityScheme secSchema = openAPI.components.securitySchemes."Authorization"

        then:

        openAPI.paths."/private".get.security != null
        openAPI.paths."/private".get.security.size() == 1
        openAPI.paths."/private".get.security.get(0)."Authorization" != null
        openAPI.paths."/private".get.security.get(0)."Authorization".isEmpty()

        openAPI.paths."/public".get.security == null
        openAPI.paths."/denyAll".get.security == null

        openAPI.paths."/fromfile/endpoint".get.security != null
        openAPI.paths."/fromfile/endpoint".get.security.size() == 1
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."Authorization" != null
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."Authorization".size() == 2
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."Authorization".contains("global-role1")
        openAPI.paths."/fromfile/endpoint".get.security.get(0)."Authorization".contains("global-role2")

        openAPI.paths."/from-file2/endpointGet".get.security != null
        openAPI.paths."/from-file2/endpointGet".get.security.size() == 1
        openAPI.paths."/from-file2/endpointGet".get.security.get(0)."Authorization" != null
        openAPI.paths."/from-file2/endpointGet".get.security.get(0)."Authorization".size() == 2
        openAPI.paths."/from-file2/endpointGet".get.security.get(0)."Authorization".contains("global-role1")
        openAPI.paths."/from-file2/endpointGet".get.security.get(0)."Authorization".contains("global-role2")

        openAPI.paths."/from-file2/endpoint".put.security != null
        openAPI.paths."/from-file2/endpoint".put.security.size() == 1
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization" != null
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".size() == 4
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".contains("role1")
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".contains("role2")
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".contains("global-role1")
        openAPI.paths."/from-file2/endpoint".put.security.get(0)."Authorization".contains("global-role2")

        openAPI.paths."/privateRoles".get.security != null
        openAPI.paths."/privateRoles".get.security.size() == 1
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization" != null
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization".size() == 2
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization".contains("myRole1")
        openAPI.paths."/privateRoles".get.security.get(0)."Authorization".contains("myRole2")

        cleanup:
        System.clearProperty(ConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }
}
