package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiPlaceholdersSpec extends AbstractOpenApiTypeElementSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/532")
    void "test build OpenAPI schema example with placeholder"() {

        given: 'An API definition'
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-placeholder-type.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(example = "${example.version}")
    public String getVersionString() {
      return "foo";
    }
    @Schema(example = "${example.version}")
    public long getVersionLong() {
      return 0;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas['MyDto']

        then: "the components are valid"
        dtoSchema != null
        dtoSchema instanceof Schema

        dtoSchema.properties.size() == 2
        dtoSchema.properties.versionString.example == '42'
        dtoSchema.properties.versionLong.example == 42

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONFIG_FILE)
    }

    void "test build OpenAPIDefinition with placeholder"() {

        given: 'An API definition'
        System.setProperty("app.title", "The title")
        System.setProperty("app.version", "1.0.0")
        System.setProperty("app.description", "The description")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;

@OpenAPIDefinition(
        info = @Info(
                title = "${app.title}",
                version = "${app.version}",
                description = "${app.description}"
        )
)
class Application {

}

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(example = "${example.version}")
    public String getVersionString() {
      return "foo";
    }
    @Schema(example = "${example.version}")
    public long getVersionLong() {
      return 0;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Info info = openAPI.info

        then: "the components are valid"

        info
        info.title == 'The title'
        info.version == '1.0.0'
        info.description == 'The description'

        cleanup:
        System.clearProperty("app.title")
        System.clearProperty("app.version")
        System.clearProperty("app.description")
    }
}
