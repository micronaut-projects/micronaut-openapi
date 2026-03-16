package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Encoding
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
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotNull;
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
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
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

    void "test build OpenAPI complex encoding block"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Controller
class MyController2 {

    @Put(consumes = {"multipart/mixed", "multipart/mixed2"})
    public Response updatePet(@RequestBody(
            description = "This is description",
            required = true,
            content = {
                    @Content(
                            mediaType = "multipart/mixed",
                            encoding = {
                                    @Encoding(
                                            name = "firstObject",
                                            contentType = "application/xml; charset=utf-8",
                                            style = "DEEP_OBJECT",
                                            explode = true,
                                            allowReserved = true,
                                            headers = {
                                                    @Header(
                                                            name = "MyHeader1",
                                                            description = "Header 1 description",
                                                            required = true,
                                                            deprecated = true
                                                    ),
                                                    @Header(
                                                            name = "MyHeader2",
                                                            description = "Header 2 description"
                                                    ),
                                                    @Header(
                                                            name = "MyHeader3",
                                                            ref = "#/components/headers/Head3"
                                                    ),
                                                    @Header(
                                                            name = "MyHeader4",
                                                            schema = @Schema(
                                                                name = "test",
                                                                description = "this is description",
                                                                defaultValue = "{\\"stampWidth\\": 100}",
                                                                format = "binary"
                                                            )
                                                    ),
                                                    @Header(
                                                            name = "MyHeader5",
                                                            schema = @Schema(
                                                                name = "test",
                                                                description = "this is description",
                                                                nullable = true,
                                                                deprecated = true,
                                                                accessMode = Schema.AccessMode.READ_ONLY,
                                                                defaultValue = "{\\"stampWidth\\": 100}",
                                                                required = true,
                                                                format = "binary",
                                                                title = "the title",
                                                                minimum = "10",
                                                                maximum = "100",
                                                                exclusiveMinimum = true,
                                                                exclusiveMaximum = true,
                                                                minLength = 10,
                                                                maxLength = 100,
                                                                minProperties = 10,
                                                                maxProperties = 100,
                                                                multipleOf = 1.5,
                                                                pattern = "ppp",
                                                                externalDocs = @ExternalDocumentation(description = "external docs"),
                                                                example = "{\\n" +
                                                                        "  \\"stampWidth\\": 220,\\n" +
                                                                        "  \\"stampHeight\\": 85,\\n" +
                                                                        "  \\"pageNumber\\": 1\\n" +
                                                                        "}"
                                                        )
                                                    ),
                                            },
                                            extensions = {
                                                    @Extension(
                                                            name = "myExt1",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                                            }
                                                    ),
                                                    @Extension(
                                                            name = "myExt2",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                                            }
                                                    ),
                                            }
                                    ),
                                    @Encoding(
                                            name = "secondObject",
                                            contentType = "application/json; charset=utf-8",
                                            style = "FORM",
                                            explode = true,
                                            headers = {
                                                    @Header(
                                                            name = "MyHeader21",
                                                            description = "Header 21 description",
                                                            required = true,
                                                            deprecated = true
                                                    ),
                                                    @Header(
                                                            name = "MyHeader22",
                                                            description = "Header 22 description"
                                                    ),
                                            },
                                            extensions = {
                                                    @Extension(
                                                            name = "myExt21",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                                            }
                                                    ),
                                                    @Extension(
                                                            name = "myExt22",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                                            }
                                                    ),
                                            }
                                    ),
                            },
                            extensions = {
                                    @Extension(
                                            name = "contentExt1",
                                            properties = {
                                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                            }
                                    ),
                                    @Extension(
                                            name = "contentExt2",
                                            properties = {
                                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                            }
                                    ),
                            }
                    ),
                    @Content(
                            mediaType = "multipart/mixed2",
                            encoding = {
                                    @Encoding(
                                            name = "firstObject",
                                            contentType = "application/xml; charset=utf-8",
                                            style = "DEEP_OBJECT",
                                            explode = true,
                                            allowReserved = true,
                                            headers = {
                                                    @Header(
                                                            name = "MyHeader1",
                                                            description = "Header 1 description",
                                                            required = true,
                                                            deprecated = true
                                                    ),
                                                    @Header(
                                                            name = "MyHeader2",
                                                            description = "Header 2 description"
                                                    ),
                                            },
                                            extensions = {
                                                    @Extension(
                                                            name = "myExt1",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                                            }
                                                    ),
                                                    @Extension(
                                                            name = "myExt2",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                                            }
                                                    ),
                                            }
                                    ),
                                    @Encoding(
                                            name = "secondObject",
                                            contentType = "application/json; charset=utf-8",
                                            style = "FORM",
                                            explode = true,
                                            headers = {
                                                    @Header(
                                                            name = "MyHeader21",
                                                            description = "Header 21 description",
                                                            required = true,
                                                            deprecated = true
                                                    ),
                                                    @Header(
                                                            name = "MyHeader22",
                                                            description = "Header 22 description"
                                                    ),
                                            },
                                            extensions = {
                                                    @Extension(
                                                            name = "myExt21",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                                            }
                                                    ),
                                                    @Extension(
                                                            name = "myExt22",
                                                            properties = {
                                                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                                            }
                                                    ),
                                            }
                                    ),
                            },
                            examples = {
                                    @ExampleObject(
                                        name = "Example1",
                                        summary = "Sum Example1",
                                        description = "Desc Example1",
                                        externalValue = "http://example1",
                                        value = "{\\"prop1\\":\\"val1\\"}",
                                        extensions = {
                                            @Extension(
                                                    name = "contentExt1",
                                                    properties = {
                                                            @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                            @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                                    }
                                            ),
                                            @Extension(
                                                    name = "contentExt2",
                                                    properties = {
                                                            @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                            @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                                    }
                                            ),
                                        }
                                    ),
                                    @ExampleObject(
                                        name = "Example2",
                                        summary = "Sum Example2",
                                        description = "Desc Example2",
                                        externalValue = "http://example2",
                                        value = "{\\"prop2\\":\\"val2\\"}",
                                        extensions = {
                                            @Extension(
                                                    name = "contExt1",
                                                    properties = {
                                                            @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                            @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                                    }
                                            ),
                                            @Extension(
                                                    name = "contExt2",
                                                    properties = {
                                                            @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                            @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                                    }
                                            ),
                                        }
                                    ),
                            },
                            extensions = {
                                    @Extension(
                                            name = "contentExt1",
                                            properties = {
                                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                                            }
                                    ),
                                    @Extension(
                                            name = "contentExt2",
                                            properties = {
                                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                                            }
                                    ),
                            }
                    )
            },
            extensions = {
                    @Extension(
                            name = "bodyExt1",
                            properties = {
                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                            }
                    ),
                    @Extension(
                            name = "bodyExt2",
                            properties = {
                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                            }
                    ),
            }
    ) @Body Pet pet) {
        return null;
    }

    static class Pet {
    }

    static class Response {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/").put

        then:
        operation

        operation.requestBody
        operation.requestBody.description == "This is description"
        operation.requestBody.content
        operation.requestBody.content.size() == 2

        operation.requestBody.content."multipart/mixed"
        operation.requestBody.content."multipart/mixed".schema
        operation.requestBody.content."multipart/mixed".schema.$ref == '#/components/schemas/MyController2.Pet'

        operation.requestBody.content."multipart/mixed".encoding.firstObject.contentType == "application/xml; charset=utf-8"
        operation.requestBody.content."multipart/mixed".encoding.firstObject.style == Encoding.StyleEnum.DEEP_OBJECT
        operation.requestBody.content."multipart/mixed".encoding.firstObject.explode
        operation.requestBody.content."multipart/mixed".encoding.firstObject.allowReserved
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.size() == 5
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader1.description == "Header 1 description"
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader1.required
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader1.deprecated
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader2.description == "Header 2 description"
        !operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader2.required
        !operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader2.deprecated
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader3.get$ref() == '#/components/headers/Head3'
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader4.schema.description == 'this is description'
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader4.schema.default
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader4.schema.format == 'binary'

        !operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.get$ref()
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.description == 'this is description'
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.default
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.default.stampWidth == 100
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.example
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.example.stampWidth == 220
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.example.stampHeight == 85
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.example.pageNumber == 1
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.deprecated
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.readOnly
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.format == 'binary'
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.externalDocs.description == 'external docs'
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.title == 'the title'
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.exclusiveMinimum
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.exclusiveMaximum
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.maximum == 100
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.minimum == 10
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.maximum == 100
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.minLength == 10
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.maxLength == 100
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.minProperties == 10
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.maxProperties == 100
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.multipleOf == 1.5
        operation.requestBody.content."multipart/mixed".encoding.firstObject.headers.MyHeader5.schema.pattern == "ppp"

        operation.requestBody.content."multipart/mixed".encoding.firstObject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.firstObject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.firstObject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.firstObject.extensions."x-myExt1".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed".encoding.firstObject.extensions."x-myExt2".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed".encoding.firstObject.extensions."x-myExt2".prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed".encoding.secondObject.contentType == "application/json; charset=utf-8"
        operation.requestBody.content."multipart/mixed".encoding.secondObject.style == Encoding.StyleEnum.FORM
        operation.requestBody.content."multipart/mixed".encoding.secondObject.explode
        !operation.requestBody.content."multipart/mixed".encoding.secondObject.allowReserved
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.secondObject.headers.MyHeader21.description == "Header 21 description"
        operation.requestBody.content."multipart/mixed".encoding.secondObject.headers.MyHeader21.required
        operation.requestBody.content."multipart/mixed".encoding.secondObject.headers.MyHeader21.deprecated
        operation.requestBody.content."multipart/mixed".encoding.secondObject.headers.MyHeader22.description == "Header 22 description"
        !operation.requestBody.content."multipart/mixed".encoding.secondObject.headers.MyHeader22.required
        !operation.requestBody.content."multipart/mixed".encoding.secondObject.headers.MyHeader22.deprecated
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions."x-myExt21".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions."x-myExt22".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed".encoding.secondObject.extensions."x-myExt22".prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed2"
        operation.requestBody.content."multipart/mixed2".schema
        operation.requestBody.content."multipart/mixed2".schema.$ref == '#/components/schemas/MyController2.Pet'

        operation.requestBody.content."multipart/mixed2".examples.Example1.summary == "Sum Example1"
        operation.requestBody.content."multipart/mixed2".examples.Example1.description == "Desc Example1"
        operation.requestBody.content."multipart/mixed2".examples.Example1.externalValue == "http://example1"
        operation.requestBody.content."multipart/mixed2".examples.Example1.value
        operation.requestBody.content."multipart/mixed2".examples.Example1.value.prop1 == "val1"
        operation.requestBody.content."multipart/mixed2".examples.Example1.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".examples.Example1.extensions.'x-contentExt1'.prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".examples.Example1.extensions.'x-contentExt1'.prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed2".examples.Example1.extensions.'x-contentExt2'.prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed2".examples.Example1.extensions.'x-contentExt2'.prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed2".examples.Example2.summary == "Sum Example2"
        operation.requestBody.content."multipart/mixed2".examples.Example2.description == "Desc Example2"
        operation.requestBody.content."multipart/mixed2".examples.Example2.externalValue == "http://example2"
        operation.requestBody.content."multipart/mixed2".examples.Example2.value
        operation.requestBody.content."multipart/mixed2".examples.Example2.value.prop2 == "val2"
        operation.requestBody.content."multipart/mixed2".examples.Example2.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".examples.Example2.extensions.'x-contExt1'.prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".examples.Example2.extensions.'x-contExt1'.prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed2".examples.Example2.extensions.'x-contExt2'.prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed2".examples.Example2.extensions.'x-contExt2'.prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed2".encoding.firstObject.contentType == "application/xml; charset=utf-8"
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.style == Encoding.StyleEnum.DEEP_OBJECT
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.explode
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.allowReserved
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.MyHeader1.description == "Header 1 description"
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.MyHeader1.required
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.MyHeader1.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.MyHeader2.description == "Header 2 description"
        !operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.MyHeader2.required
        !operation.requestBody.content."multipart/mixed2".encoding.firstObject.headers.MyHeader2.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.extensions."x-myExt1".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.extensions."x-myExt2".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed2".encoding.firstObject.extensions."x-myExt2".prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed2".encoding.secondObject.contentType == "application/json; charset=utf-8"
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.style == Encoding.StyleEnum.FORM
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.explode
        !operation.requestBody.content."multipart/mixed2".encoding.secondObject.allowReserved
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.headers.MyHeader21.description == "Header 21 description"
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.headers.MyHeader21.required
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.headers.MyHeader21.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.headers.MyHeader22.description == "Header 22 description"
        !operation.requestBody.content."multipart/mixed2".encoding.secondObject.headers.MyHeader22.required
        !operation.requestBody.content."multipart/mixed2".encoding.secondObject.headers.MyHeader22.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions."x-myExt21".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions."x-myExt22".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed2".encoding.secondObject.extensions."x-myExt22".prop2 == "prop2Val2"
    }

    void "test build OpenAPI multipart form data"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import jakarta.validation.constraints.NotNull;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import jakarta.inject.Singleton;

@Controller("/path/{input}")
class OpenApiController {

    /**
     * Operation description.
     *
     * @param template Template description
     * @param parameters Parameters description
     */
    @RequestBody(
            description = "Body description",
            content = @Content(encoding = {
            @Encoding(name = "template", contentType = MediaType.APPLICATION_OCTET_STREAM),
            @Encoding(name = "parameters", contentType = MediaType.APPLICATION_JSON),
    }))
    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public String print(
            String input,
            @NotNull CompletedFileUpload template,
            String parameters) {
        return null;
    }
}

@Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/{input}").post

        then:
        operation

        operation.parameters.size() == 1
        operation.parameters.get(0).name == 'input'
        operation.parameters.get(0).in == 'path'
        operation.parameters.get(0).required
        operation.parameters.get(0).schema
        operation.parameters.get(0).schema.type == 'string'

        operation.requestBody
        operation.requestBody.description == "Body description"
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."multipart/form-data"
        operation.requestBody.content."multipart/form-data".schema
        operation.requestBody.content."multipart/form-data".schema.required.size() == 1
        operation.requestBody.content."multipart/form-data".schema.required.get(0) == 'template'
        operation.requestBody.content."multipart/form-data".schema.properties.size() == 2
        operation.requestBody.content."multipart/form-data".schema.properties.'template'.type == 'string'
        operation.requestBody.content."multipart/form-data".schema.properties.'template'.format == 'binary'
        operation.requestBody.content."multipart/form-data".schema.properties.'parameters'.type == 'string'
        operation.requestBody.content."multipart/form-data".encoding."template".contentType == "application/octet-stream"
        operation.requestBody.content."multipart/form-data".encoding."parameters".contentType == "application/json"
    }

    void "test build OpenAPI multipart form data auto set application/octet-stream"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.HashMap;

import jakarta.validation.constraints.NotNull;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import jakarta.inject.Singleton;

@Controller("/path/{input}")
class OpenApiController {

    /**
     * Operation description.
     *
     * @param template Template description
     * @param parameters Parameters description
     */
    @RequestBody(
            description = "Body description",
            content = @Content(encoding = {
            @Encoding(name = "parameters", contentType = MediaType.APPLICATION_JSON),
    }))
    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public String print(
            String input,
            @NotNull CompletedFileUpload template,
            @Schema(implementation = Parameters.class) String parameters) {
        return null;
    }
}

class Parameters extends HashMap<String, Object> {

}

@Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/{input}").post

        then:
        operation

        operation.parameters.size() == 1
        operation.parameters.get(0).name == 'input'
        operation.parameters.get(0).in == 'path'
        operation.parameters.get(0).required
        operation.parameters.get(0).schema
        operation.parameters.get(0).schema.type == 'string'

        operation.requestBody
        operation.requestBody.description == "Body description"
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."multipart/form-data"
        operation.requestBody.content."multipart/form-data".schema
        operation.requestBody.content."multipart/form-data".schema.required.size() == 1
        operation.requestBody.content."multipart/form-data".schema.required.get(0) == 'template'
        operation.requestBody.content."multipart/form-data".schema.properties.size() == 2
        operation.requestBody.content."multipart/form-data".schema.properties.'template'.type == 'string'
        operation.requestBody.content."multipart/form-data".schema.properties.'template'.format == 'binary'
        operation.requestBody.content."multipart/form-data".schema.properties.'parameters'.type == 'object'
        operation.requestBody.content."multipart/form-data".encoding."template".contentType == "application/octet-stream"
        operation.requestBody.content."multipart/form-data".encoding."parameters".contentType == "application/json"
    }

    void "test build OpenAPI multipart form data with custom schema"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.HashMap;

import jakarta.validation.constraints.NotNull;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import jakarta.inject.Singleton;

@Controller("/path/{input}")
class OpenApiController {

    /**
     * Operation description.
     *
     * @param template Template description
     * @param parameters Parameters description
     */
    @RequestBody(
            description = "Body description",
            content = @Content(
                    schema = @Schema(implementation = UploadItem.class),
                    encoding = @Encoding(name = "parameters", contentType = MediaType.APPLICATION_JSON)))
    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public String print(String input,
                        @NotNull CompletedFileUpload template,
                        String parameters) {
        return null;
    }
}

class UploadItem {

    /**
     * Upload item template description
     */
    @Schema(type = "string", format = "binary")
    @NotNull
    public String template;

    @NotNull
    public Parameters parameters;
}

class Parameters extends HashMap<String, Object> {

}

@Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/{input}").post

        then:
        operation

        operation.parameters.size() == 1
        operation.parameters.get(0).name == 'input'
        operation.parameters.get(0).in == 'path'
        operation.parameters.get(0).required
        operation.parameters.get(0).schema
        operation.parameters.get(0).schema.type == 'string'

        operation.requestBody
        operation.requestBody.description == "Body description"
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."multipart/form-data"
        operation.requestBody.content."multipart/form-data".encoding."template".contentType == "application/octet-stream"
        operation.requestBody.content."multipart/form-data".encoding."parameters".contentType == "application/json"
        operation.requestBody.content."multipart/form-data".schema.$ref == '#/components/schemas/UploadItem'

        openAPI.components.schemas.UploadItem.required.size() == 2
        openAPI.components.schemas.UploadItem.required.contains('template')
        openAPI.components.schemas.UploadItem.required.contains('parameters')
        openAPI.components.schemas.UploadItem.properties.size() == 2
        openAPI.components.schemas.UploadItem.properties.'template'.type == 'string'
        openAPI.components.schemas.UploadItem.properties.'template'.format == 'binary'
        openAPI.components.schemas.UploadItem.properties.'template'.description == 'Upload item template description'
        openAPI.components.schemas.UploadItem.properties.'parameters'.type == 'object'
    }
}
