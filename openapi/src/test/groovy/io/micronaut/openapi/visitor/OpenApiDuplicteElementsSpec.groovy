package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiDuplicteElementsSpec extends AbstractOpenApiTypeElementSpec {

    void "test duplicate elements removed"() {

        when:
        def visitor = new OpenApiApplicationVisitor()
        def openApi = ConvertUtils.yamlMapper.readValue('''
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
  - url: http://petstore.swagger.io/v2
  - url: http://petstore.swagger.io/v2
  - url: http://petstore.swagger.io/v2
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
        visitor.findAndRemoveDuplicates(openApi)

        then:

        openApi.tags.size() == 1
        openApi.servers.size() == 1
        openApi.components.schemas.Pet.required.size() == 1
        openApi.paths.'/pets'.post.requestBody.content.'application/x-www-form-urlencoded'.schema.required.size() == 1
        openApi.paths.'/pets'.post.tags.size() == 1
        openApi.paths.'/pets'.post.parameters.size() == 1
        openApi.paths.'/pets'.post.security.size() == 1
    }

}
