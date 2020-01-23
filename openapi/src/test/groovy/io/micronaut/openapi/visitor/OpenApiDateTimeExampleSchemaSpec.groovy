package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.http.MediaType
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.models.OpenAPI

class OpenApiDateTimeExampleSchemaSpec extends AbstractTypeElementSpec {

    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test jdk8 date time example"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import java.time.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller
class TimesController {

    @Get("/times")
    @ApiResponse(responseCode = "200", description = "Times.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Times.class)))
    HttpResponse<Times> get() {
        return HttpResponse.ok();
    }
}

@Schema(description = "times")
class Times {

    private OffsetDateTime offsetDateTime;
    private ZonedDateTime zonedDateTime;

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Z")
    @Schema(description = "offsetDateTime", example = "2020-07-21T17:32:28Z", type = "string", format = "date-time")
    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public void setOffsetDateTime(OffsetDateTime time) {
        offsetDateTime = time;
    }
 
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Z")
    @Schema(description = "zonedDateTime", example = "2020-07-21T17:32:28Z", type = "string", format = "date-time")
    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime time) {
        zonedDateTime = time;
    }
}

@javax.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        openAPI.components.schemas["Times"]
        openAPI.components.schemas["Times"].type == "object"

        openAPI.components.schemas["Times"].properties
        openAPI.components.schemas["Times"].properties.size() == 2

        openAPI.components.schemas["Times"].properties["zonedDateTime"].example.toString() == "2020-07-21T17:32:28Z"
        openAPI.components.schemas["Times"].properties["offsetDateTime"].example.toString() == "2020-07-21T17:32:28Z"
    }
}
