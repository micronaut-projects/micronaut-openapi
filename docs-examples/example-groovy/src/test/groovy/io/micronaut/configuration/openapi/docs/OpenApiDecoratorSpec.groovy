package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class OpenApiDecoratorSpec extends Specification {

    void "operation ids are prefixed and suffixed"() {
        when:
        def paths = OpenApiSpec.load().paths

        then:
        paths["/cats"].post.operationId == "cats-save-suffix"
        paths["/cats/{id}"].get.operationId == "cats-get-suffix"
        paths["/dogs"].post.operationId == "dogs-save"
        paths["/dogs/{id}"].get.operationId == "dogs-get"
    }
}
