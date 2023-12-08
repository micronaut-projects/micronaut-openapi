package io.micronaut.openapi.view

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.visitor.Utils
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.*

class OpenApiViewSpec extends AbstractOpenApiTypeElementSpec {

    def cleanup() {
        def outputDir = new File("output")
        outputDir.deleteDir()
    }

    @RestoreSystemProperties
    void "test disable generation spec and enabled views"() {

        given:
        Path outputDir = Paths.get("output")
        System.setProperty(MICRONAUT_OPENAPI_SWAGGER_FILE_GENERATION_ENABLED, "false")
        System.setProperty(MICRONAUT_OPENAPI_VIEWS_SPEC, "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true,rapipdf.enabled=true,swagger-ui.theme=flattop")
        System.setProperty(MICRONAUT_OPENAPI_VIEWS_DEST_DIR, outputDir.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;

@Controller
class PetController {

    @Get("/pet")
    Pet getPet() {
        return new Pet("John");
    }
}

/**
 *
 * @param name The name of the pet
 * @author gkrocher
 */
@Serdeable
record Pet(
    @NotBlank
    @Size(max = 200)
    String name
) {}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference == null

        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("redoc").resolve("res").resolve("redoc.standalone.js"))
        Files.exists(outputDir.resolve("redoc").resolve("res").resolve("rapipdf-min.js"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("res").resolve("rapidoc-min.js"))
        Files.exists(outputDir.resolve("rapidoc").resolve("res").resolve("rapipdf-min.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui.css"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("favicon-16x16.png"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("favicon-32x32.png"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui-bundle.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui-standalone-preset.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("rapipdf-min.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("flattop.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("bootstrap.min.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("default.min.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("font-awesome.min.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("highlight.min.js"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("openapi-explorer.min.js"))
    }
}
