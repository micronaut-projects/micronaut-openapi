package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiRequestBodyArraySchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI with ArraySchema in RequestBody"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/")
class MyController {

    @Put("/")
    @Tag(name = "Tag name", description = "tag description.")
    @Operation(description = "Operation description.", summary = "Operation summary.")
    @RequestBody(description = "Body description.",
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = Long.class)
                    )))
    public void update(List<Long> l) {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI
        openAPI.paths.size() == 1
        openAPI.paths.get("/")
        openAPI.paths.get("/").put

        when:
        Operation operation = openAPI.paths?.get("/")?.put
        RequestBody requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.description == "Body description."
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['application/json'].schema
        requestBody.content['application/json'].schema instanceof ArraySchema

        expect:
        operation
        operation.responses.size() == 1
    }

}