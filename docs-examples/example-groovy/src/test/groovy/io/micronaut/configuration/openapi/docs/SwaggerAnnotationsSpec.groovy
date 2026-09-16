package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class SwaggerAnnotationsSpec extends Specification {

    void "swagger annotations take precedence"() {
        when:
        def operation = OpenApiSpec.load().paths["/greetings/{name}"].get
        def parameter = operation.parameters[0]
        def responses = operation.responses

        then:
        operation.tags == ["greeting"]
        operation.summary == "Greets a person"
        operation.description == "A friendly greeting is returned"
        operation.operationId == "greetings"
        parameter.name == "name"
        parameter.in == "path"
        parameter.description == "The name of the person"
        parameter.required
        parameter.schema.minLength == 1
        parameter.schema.type == "string"
        responses["200"].content["text/plain"].schema.type == "string"
        responses["400"].description == "Invalid Name Supplied"
        responses["404"].description == "Person not found"
    }
}
