package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiEncodingSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI encoding block"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Singleton;import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Controller("/path/{input}")
class OpenApiController {

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Endpoint summary", description = "Endpoint description", tags = "Print")
    public Mono<HttpResponse<byte[]>> processSync(@RequestBody(required = true,
                                                          description = "Template as file",
                                                          content = @Content(
                                                                  mediaType = MediaType.MULTIPART_FORM_DATA,
                                                                  encoding = @Encoding(
                                                                          name = "template",
                                                                          contentType = MediaType.APPLICATION_OCTET_STREAM
                                                                  ),
                                                                  schema = @Schema(implementation = UploadPrint.class)
                                                          )
                                                  ) @NotNull Flux<CompletedFileUpload> template) {
        return Mono.empty();
    }
}

@Schema(requiredProperties = {"parameters", "template"})
@Introspected
class UploadPrint {

    @Schema(name = "template", description = "Template as file", type = "object", format = "binary", required = true)
    private Object template;
    @Schema(implementation = PrintParameters.class)
    private Map<String, Object> parameters;
    
    UploadPrint(Object template, Map<String, Object> parameters) {
        this.template = template;
        this.parameters = parameters;
    }

    public Object getTemplate() {
        return template;
    }

    public void setTemplate(Object template) {
        this.template = template;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}

@Introspected
class PrintParameters {

    @Schema
    private Map<String, Object> parameters;

    PrintParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}

@Introspected
class ErrorResponse {

    private String code;
    private String message;
    
    ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

@Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema uploadPrintSchema = openAPI.components.schemas['UploadPrint']
        Schema printParametersSchema = openAPI.components.schemas['PrintParameters']

        then: "the components are valid"
        uploadPrintSchema != null
        printParametersSchema != null

        uploadPrintSchema instanceof Schema
        printParametersSchema instanceof Schema

        when:
        Operation operation = openAPI.paths.get("/path/{input}").post

        then:
        operation

        operation.requestBody
        operation.requestBody.description == "Template as file"
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."multipart/form-data"
        operation.requestBody.content."multipart/form-data".schema
        operation.requestBody.content."multipart/form-data".schema instanceof Schema
        ((Schema) operation.requestBody.content."multipart/form-data".schema).get$ref() == "#/components/schemas/UploadPrint"
        operation.requestBody.content."multipart/form-data".encoding."template".contentType == "application/octet-stream"
    }
}
