package io.micronaut.configuration.openapi.docs.groups

import spock.lang.Specification

class OpenApiGroupSpec extends Specification {

    void "group v1"() {
        when:
        def openApi = OpenApiSpec.load("public-api-v1-v1-v1.yml")

        then:
        openApi.info.title == "Public api v1"
        openApi.info.version == "v1"
        openApi.info.description == "This is API version 1"
        openApi.paths.containsKey("/save")
        openApi.paths.containsKey("/read/{id}")
        !openApi.paths.containsKey("/save/{id}")
    }

    void "group v2"() {
        when:
        def openApi = OpenApiSpec.load("public-api-v2-v2-v2.yml")

        then:
        openApi.info.title == "Public api v2"
        openApi.info.version == "v2"
        openApi.paths.containsKey("/save")
        openApi.paths.containsKey("/save/{id}")
        !openApi.paths.containsKey("/read/{id}")
    }
}
