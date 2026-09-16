package io.micronaut.configuration.openapi.docs.config

import spock.lang.Specification

class TagsGenerationSpec extends Specification {

    void "tags are generated from the class name"() {
        when:
        def openApi = OpenApiSpec.load("hello-world-v1.1.yml")
        def paths = openApi.paths

        then:
        // Groovy doc comments are not available to the Groovy AST transformation, so the global tag (with the
        // description of the controller) is not generated
        openApi.tags.empty
        paths["/read/{id}"].get.tags == ["user-operations"]
        paths["/read/{id}"].get.operationId == "read"
        paths["/save/{id}"].post.tags == ["user-operations"]
        paths["/save/{id}"].post.operationId == "save2"
        paths["/save"].post.tags == ["user-operations"]
        paths["/save"].post.operationId == "save"
    }
}
