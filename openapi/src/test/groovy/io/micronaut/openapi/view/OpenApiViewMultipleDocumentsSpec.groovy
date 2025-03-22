package io.micronaut.openapi.view

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import spock.util.environment.RestoreSystemProperties

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_GROUPS
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VIEWS_DEST_DIR
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VIEWS_SPEC

class OpenApiViewMultipleDocumentsSpec extends AbstractOpenApiTypeElementSpec {

    def cleanup() {
        def outputDir = new File("output")
        outputDir.deleteDir()
    }

    @RestoreSystemProperties
    void "test Swagger UI and Scalar with multiple documents"() {

        given:
        Path outputDir = Paths.get("output")
        System.setProperty(MICRONAUT_OPENAPI_GROUPS + ".api-v1.primary", "true")
        System.setProperty(MICRONAUT_OPENAPI_VIEWS_SPEC, "swagger-ui.enabled=true,scalar.enabled=true")
        System.setProperty(MICRONAUT_OPENAPI_VIEWS_DEST_DIR, outputDir.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.openapi.annotation.OpenAPIGroup;import io.micronaut.openapi.annotation.OpenAPIGroupInfo;import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;

@OpenAPIGroup("api-v1")
@Controller
class ApiV1Controller {

    @Get("/v1/create")
    Pet getPet() {
        return null;
    }
}

@OpenAPIGroup("api-v2")
@Controller
class ApiV2Controller {

    @Get("/v2/create")
    Pet getPet() {
        return null;
    }
}

@Serdeable
record Pet(
    String name
) {}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        var swaggerUiIndexPath = outputDir.resolve("swagger-ui").resolve("index.html")
        Files.exists(swaggerUiIndexPath)
        var swaggerUiContent = swaggerUiIndexPath.toFile().getText(StandardCharsets.UTF_8.displayName())

        swaggerUiContent.contains("\"urls.primaryName\":\"api-v1\",")
        swaggerUiContent.contains("urls: [{name: \"api-v2\",url: contextPath + \"/swagger/service-1.0.0-api-v2.yml\"},{name: \"api-v1\",url: contextPath + \"/swagger/service-1.0.0-api-v1.yml\"}],")

        var scalarIndexPath = outputDir.resolve("scalar").resolve("index.html")
        Files.exists(scalarIndexPath)
        var scalarContent = scalarIndexPath.toFile().getText(StandardCharsets.UTF_8.displayName())

        scalarContent.contains("sources: [{title: \"api-v2\",url: contextPath + \"/swagger/service-1.0.0-api-v2.yml\",},{title: \"api-v1\",url: contextPath + \"/swagger/service-1.0.0-api-v1.yml\",default: true}],")
    }
}
