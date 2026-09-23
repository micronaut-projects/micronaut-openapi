package io.micronaut.configuration.openapi.docs.versions

import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import spock.lang.Specification

class VersionedControllerSpec extends Specification {

    void "version 1"() {
        when:
        def openApi = OpenApiSpec.load("service-1.0.0-1.yml")
        def common = openApi.paths["/versioned/common"].post
        def hello = openApi.paths["/versioned/hello"].get

        then:
        common.operationId == "common"
        isVersionParameter(common.parameters[0])
        hello.operationId == "helloV1"
        isVersionParameter(hello.parameters[0])
        !openApi.paths["/versioned/hello"].readOperationsMap().containsKey(PathItem.HttpMethod.POST)
    }

    void "version 2"() {
        when:
        def openApi = OpenApiSpec.load("service-1.0.0-2.yml")
        def common = openApi.paths["/versioned/common"].post
        def hello = openApi.paths["/versioned/hello"].post
        def body = hello.requestBody.content["application/json"].schema
        String userDtoRef = body.properties["userDto"].$ref
        def userDto = openApi.components.schemas[userDtoRef - "#/components/schemas/"]

        then:
        common.operationId == "common"
        hello.operationId == "helloV2"
        isVersionParameter(hello.parameters[0])
        userDtoRef.endsWith("UserDto")
        userDto.required == ["address"]
        userDto.properties.keySet() as List == ["name", "age", "secondName", "address"]
        !openApi.paths["/versioned/hello"].readOperationsMap().containsKey(PathItem.HttpMethod.GET)
    }

    private static boolean isVersionParameter(Parameter parameter) {
        parameter.name == "version" &&
                parameter.in == "query" &&
                parameter.description == "API version" &&
                parameter.schema.type == "string"
    }
}
