package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class HelloControllerSpec extends Specification {

    void "OpenAPI definition"() {
        when:
        def info = OpenApiSpec.load().info

        then:
        info.title == "Hello World"
        info.description == "My API"
        info.version == "0.0"
        info.contact.name == "Fred"
        info.contact.url == "https://gigantic-server.com"
        info.contact.email == "Fred@gigagantic-server.com"
        info.license.name == "Apache 2.0"
        info.license.url == "https://foo.bar"
    }

    // Groovy doc comments are not available to the Groovy AST transformation, so the parameter
    // and response descriptions of the Java example are not generated for Groovy
    void "the controller is documented"() {
        when:
        def operation = OpenApiSpec.load().paths["/hello/{name}"].get
        def parameter = operation.parameters[0]
        def response = operation.responses["200"]

        then:
        operation.operationId.startsWith("index")
        parameter.name == "name"
        parameter.in == "path"
        parameter.required
        parameter.schema.type == "string"
        response.content["text/plain"].schema.type == "string"
    }
}
