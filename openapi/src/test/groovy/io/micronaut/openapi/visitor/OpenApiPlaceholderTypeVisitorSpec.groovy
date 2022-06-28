package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiPlaceholderTypeVisitorSpec extends AbstractOpenApiTypeElementSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/532")
    void "test build OpenAPI schema example with placeholder"() {

        given: 'An API definition'
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-placeholder-type.properties")

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
        AbstractOpenApiVisitor.testReferenceAfterPlaceholders != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReferenceAfterPlaceholders
        Schema dtoSchema = openAPI.components.schemas['MyDto']

        then: "the components are valid"
        dtoSchema != null
        dtoSchema instanceof Schema

        dtoSchema.properties.size() == 2
        dtoSchema.properties.versionString.example == '42'
        dtoSchema.properties.versionLong.example == 42
    }
}
