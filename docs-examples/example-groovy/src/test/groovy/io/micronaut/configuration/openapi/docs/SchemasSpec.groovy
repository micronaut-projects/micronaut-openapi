package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class SchemasSpec extends Specification {

    void "schema annotation customizes the POGO"() {
        when:
        def schemas = OpenApiSpec.load().components.schemas
        def pet = schemas["MyPet"]
        def petType = schemas["PetType"]

        then:
        pet
        pet.description == "Pet description"
        pet.properties["age"].description == "Pet age"
        pet.properties["age"].maximum == 20
        pet.properties["name"].description == "Pet name"
        pet.properties["name"].maxLength == 20
        petType.type == "string"
        petType.enum == ["DOG", "CAT"]
    }

    void "meta annotation applies the schema"() {
        when:
        def operation = OpenApiSpec.load().paths["/pets"].post

        then:
        operation.requestBody.content["application/json"].schema.$ref == "#/components/schemas/MyPet"
    }

    void "generics are included in the schema name"() {
        when:
        def openApi = OpenApiSpec.load()
        def response = openApi.paths["/"].put.responses["200"]

        then:
        response.content["application/json"].schema.$ref == "#/components/schemas/Response_Pet_"
        openApi.components.schemas["Response_Pet_"].properties["result"].$ref == "#/components/schemas/MyPet"
    }

    void "schema name can be changed"() {
        when:
        def openApi = OpenApiSpec.load()
        def response = openApi.paths["/named"].put.responses["200"]

        then:
        response.content["application/json"].schema.$ref == "#/components/schemas/ResponseOfPet"
        openApi.components.schemas.containsKey("ResponseOfPet")
    }
}
