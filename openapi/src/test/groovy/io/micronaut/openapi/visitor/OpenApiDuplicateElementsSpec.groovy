package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.OpenApiUtils
import io.swagger.v3.oas.models.OpenAPI

class OpenApiDuplicateElementsSpec extends AbstractOpenApiTypeElementSpec {

    void "test duplicate elements removed"() {

        when:
        def visitor = new OpenApiApplicationVisitor()
        def openApi = OpenApiUtils.yamlMapper.readValue('''
openapi: 3.0.0
info:
  description: This is a sample server Petstore server.
  version: 1.0.0
  title: Swagger Petstore
tags:
  - name: pet
    description: Pet resource
  - name: pet
    description: Store resource
paths:
  /pets:
    post:
      tags:
        - pet
        - pet
        - pet
      parameters:
        - in: query
          name: status
          description: Status values that need to be considered for filter
          schema:
            type: string
        - in: query
          name: status
          description: Status values that need to be considered for filter
          schema:
            type: string
      requestBody:
        content:
          application/x-www-form-urlencoded:
            schema:
              type: object
              properties:
                name:
                  description: Updated name of the pet
                  type: string
                status:
                  description: Updated status of the pet
                  type: string
              required:
                - name
                - name
                - name
      responses:
        "405":
          description: Invalid input
      security:
        - petstore_auth:
            - write_pets
            - read_pets
        - petstore_auth:
            - write_pets
            - read_pets
        - petstore_auth:
            - write_pets
            - read_pets
servers:
  - url: https://petstore.swagger.io/v2
  - url: https://petstore.swagger.io/v2
  - url: https://petstore.swagger.io/v2
components:
  schemas:
    Pet:
      required:
        - id
        - id
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        tag:
          type: string

''', OpenAPI);
        OpenApiNormalizeUtils.findAndRemoveDuplicates(openApi)

        then:

        openApi.tags.size() == 1
        openApi.servers.size() == 1
        openApi.components.schemas.Pet.required.size() == 1
        openApi.paths.'/pets'.post.requestBody.content.'application/x-www-form-urlencoded'.schema.required.size() == 1
        openApi.paths.'/pets'.post.tags.size() == 1
        openApi.paths.'/pets'.post.parameters.size() == 1
        openApi.paths.'/pets'.post.security.size() == 1
    }

    void "test date enum default value"() {

        when:
        var defaultValue = "2023-12-12"
        var openApiSpec = """
openapi: 3.0.3
info:
  version: "1.0.0"
  title: Simple Inventory API
paths:
  /inventory:
    get:
      operationId: searchInventory
      parameters:
        - in: header
          name: test
          schema:
            type: string
            format: date
            enum:
              - 2023-12-12
            default: $defaultValue
      responses:
        '200':
          description: search results matching criteria
          content:
            application/json:
              schema:
                type: array
                items:
                  \$ref: '#/components/schemas/InventoryItem'
components:
  schemas:
    InventoryItem:
      type: object
      properties:
        releaseDate:
          type: string
          format: date-time
          example: '2016-08-29T09:12:33.001Z'
  """
        var openApi = OpenApiUtils.getYamlMapper().readValue(openApiSpec, OpenAPI.class)
        var serializedOpenApi = OpenApiUtils.getYamlMapper().writeValueAsString(openApi)

        then:
        serializedOpenApi.trim().contains("default: $defaultValue")
    }
}
