package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiUrlParametersAsPojoSpec extends AbstractOpenApiTypeElementSpec {

    void "test urlMatchVar wrapped parameters"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.time.Instant;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

import jakarta.validation.constraints.Positive;

@Controller
class MyController {

    @Post("/myPost")
    void checkAvailabilityPost(@Body AvailabilityRequest availabilityRequest) {
    }

    @Get("/{?availabilityRequest*}")
    void checkAvailability(AvailabilityRequest availabilityRequest) {
    }

    @Get("/test/{?availabilityRequest2*}")
    void checkAvailabilityWithoutSchema(AvailabilityRequest2 availabilityRequest2) {
    }
}

class AvailabilityRequest {
    public String restaurantId;
    @Positive
    public Integer partySize;
    public Instant visitTime;
}

class AvailabilityRequest2 {
    public String restaurantId;
    @Positive
    public Integer partySize;
    public Instant visitTime;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation getOp = openAPI.paths?.get("/")?.get
        Operation getOp2 = openAPI.paths?.get("/test")?.get

        Schema schemaAvailabilityRequest = openAPI.components.schemas.AvailabilityRequest
        Schema schemaAvailabilityRequest2 = openAPI.components.schemas.AvailabilityRequest2

        expect:
        getOp
        getOp.parameters
        getOp.parameters.size() == 3
        getOp.parameters[0].name == 'restaurantId'
        getOp.parameters[1].name == 'partySize'
        getOp.parameters[2].name == 'visitTime'

        schemaAvailabilityRequest
        schemaAvailabilityRequest.properties
        schemaAvailabilityRequest.properties.size() == 3
        schemaAvailabilityRequest.properties.restaurantId
        schemaAvailabilityRequest.properties.partySize
        schemaAvailabilityRequest.properties.visitTime

        schemaAvailabilityRequest2 == null

        getOp2
        getOp2.parameters
        getOp2.parameters.size() == 3
        getOp2.parameters[0].name == 'restaurantId'
        getOp2.parameters[1].name == 'partySize'
        getOp2.parameters[2].name == 'visitTime'
    }
}
