package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED

class OpenApiAdocModuleSpec extends AbstractTypeElementSpec {

    def setup() {
        Utils.clean()
        System.clearProperty(MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
        System.setProperty(MICRONAUT_OPENAPI_ADOC_ENABLED, "true")
    }

    def cleanup() {
        Utils.clean()
        System.clearProperty(Utils.ATTR_TEST_MODE)
        System.clearProperty(MICRONAUT_OPENAPI_ADOC_ENABLED)
    }

    void "test ADoc module"() {

        given:
        Path outputDir = Paths.get("output")
        def filename = "my-openapi.adoc";
        System.setProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, outputDir.toString())
        System.setProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, filename)

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                summary = "the summary",
                license = @License(
                        name = "Apache 2.0",
                        url = "https://foo.bar",
                        identifier = "licenseId"
                )
        )
)
class Application {

}

@Controller("/hello")
class HelloWorldApi {

    @Get
    public HttpResponse<String> helloWorld() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Files.exists(outputDir.resolve(filename))

        cleanup:
        System.clearProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH)
        System.clearProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME)
    }

    void "test ADoc module with default filename"() {

        given:
        Path outputDir = Paths.get("output")
        System.setProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, outputDir.toString())

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API",
                summary = "the summary",
                license = @License(
                        name = "Apache 2.0",
                        url = "https://foo.bar",
                        identifier = "licenseId"
                )
        )
)
class Application {

}

@Controller("/hello")
class HelloWorldApi {

    @Get
    public HttpResponse<String> helloWorld() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Files.exists(outputDir.resolve("the-title-0.0.adoc"))

        cleanup:
        System.clearProperty(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH)
    }
}
