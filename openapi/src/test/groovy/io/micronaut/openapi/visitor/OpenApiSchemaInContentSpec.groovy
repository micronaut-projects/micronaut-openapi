package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiSchemaInContentSpec extends AbstractOpenApiTypeElementSpec {

    void "test schema inside response content"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/path")
class OpenApiController {

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "This is summary",
            description = "This is description",
            tags = "Normalize",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Desc1", content = @Content(mediaType = MediaType.ALL, schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "300", description = "Desc1", content = @Content(mediaType = MediaType.ALL, schema = @Schema(ref = "#/components/schemas/myCustomSchema"))),
                    @ApiResponse(responseCode = "400", description = "Desc2", content = @Content(schema = @Schema(implementation = Response.class))),
            })
    public HttpResponse<byte[]> processSync(@RequestBody(required = true, description = "Template", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, encoding = @Encoding(name = "template", contentType = MediaType.APPLICATION_OCTET_STREAM)))
                                                  @NotNull CompletedFileUpload template) {

        return HttpResponse.ok(new byte[0]);
    }
}

class Response {

    private boolean success;
    private String message;
    private List<String> details;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path").post

        then:
        operation
        operation.responses
        operation.responses.size() == 3
        operation.responses."200".content.'*/*'.schema
        operation.responses."200".content.'*/*'.schema.type == 'string'
        operation.responses."200".content.'*/*'.schema.format == 'binary'
        operation.responses."300".content.'*/*'.schema.$ref == '#/components/schemas/myCustomSchema'
    }

    void "test schema inside response content2"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller("/path")
class OpenApiController {

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "This is summary",
            description = "This is description",
            tags = "Normalize")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Desc1", content = @Content(mediaType = MediaType.ALL, schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "300", description = "Desc1", content = @Content(mediaType = MediaType.ALL, schema = @Schema(ref = "#/components/schemas/myCustomSchema"))),
        @ApiResponse(responseCode = "400", description = "Desc2", content = @Content(schema = @Schema(implementation = Response.class))),
    })
    public HttpResponse<byte[]> processSync(@RequestBody(required = true, description = "Template", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, encoding = @Encoding(name = "template", contentType = MediaType.APPLICATION_OCTET_STREAM)))
                                                  @NotNull CompletedFileUpload template) {

        return HttpResponse.ok(new byte[0]);
    }
}

class Response {

    private boolean success;
    private String message;
    private List<String> details;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path").post

        then:
        operation
        operation.responses
        operation.responses.size() == 3
        operation.responses."200".content.'*/*'.schema
        operation.responses."200".content.'*/*'.schema.type == 'string'
        operation.responses."200".content.'*/*'.schema.format == 'binary'
        operation.responses."300".content.'*/*'.schema.$ref == '#/components/schemas/myCustomSchema'
    }
}
