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

    void "test build OpenAPI complex encoding block"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
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
                                            name = "firstOject",
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
                                            name = "secondOject",
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
                                            name = "firstOject",
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
                                            name = "secondOject",
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
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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

        operation.requestBody.content."multipart/mixed".encoding.firstOject.contentType == "application/xml; charset=utf-8"
        operation.requestBody.content."multipart/mixed".encoding.firstOject.style == Encoding.StyleEnum.DEEP_OBJECT
        operation.requestBody.content."multipart/mixed".encoding.firstOject.explode
        operation.requestBody.content."multipart/mixed".encoding.firstOject.allowReserved
        operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.MyHeader1.description == "Header 1 description"
        operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.MyHeader1.required
        operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.MyHeader1.deprecated
        operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.MyHeader2.description == "Header 2 description"
        !operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.MyHeader2.required
        !operation.requestBody.content."multipart/mixed".encoding.firstOject.headers.MyHeader2.deprecated
        operation.requestBody.content."multipart/mixed".encoding.firstOject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.firstOject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.firstOject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.firstOject.extensions."x-myExt1".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed".encoding.firstOject.extensions."x-myExt2".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed".encoding.firstOject.extensions."x-myExt2".prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed".encoding.secondOject.contentType == "application/json; charset=utf-8"
        operation.requestBody.content."multipart/mixed".encoding.secondOject.style == Encoding.StyleEnum.FORM
        operation.requestBody.content."multipart/mixed".encoding.secondOject.explode
        !operation.requestBody.content."multipart/mixed".encoding.secondOject.allowReserved
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.secondOject.headers.MyHeader21.description == "Header 21 description"
        operation.requestBody.content."multipart/mixed".encoding.secondOject.headers.MyHeader21.required
        operation.requestBody.content."multipart/mixed".encoding.secondOject.headers.MyHeader21.deprecated
        operation.requestBody.content."multipart/mixed".encoding.secondOject.headers.MyHeader22.description == "Header 22 description"
        !operation.requestBody.content."multipart/mixed".encoding.secondOject.headers.MyHeader22.required
        !operation.requestBody.content."multipart/mixed".encoding.secondOject.headers.MyHeader22.deprecated
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions."x-myExt21".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions."x-myExt22".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed".encoding.secondOject.extensions."x-myExt22".prop2 == "prop2Val2"

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

        operation.requestBody.content."multipart/mixed2".encoding.firstOject.contentType == "application/xml; charset=utf-8"
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.style == Encoding.StyleEnum.DEEP_OBJECT
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.explode
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.allowReserved
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.MyHeader1.description == "Header 1 description"
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.MyHeader1.required
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.MyHeader1.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.MyHeader2.description == "Header 2 description"
        !operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.MyHeader2.required
        !operation.requestBody.content."multipart/mixed2".encoding.firstOject.headers.MyHeader2.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.extensions."x-myExt1".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.extensions."x-myExt1".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.extensions."x-myExt2".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed2".encoding.firstOject.extensions."x-myExt2".prop2 == "prop2Val2"

        operation.requestBody.content."multipart/mixed2".encoding.secondOject.contentType == "application/json; charset=utf-8"
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.style == Encoding.StyleEnum.FORM
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.explode
        !operation.requestBody.content."multipart/mixed2".encoding.secondOject.allowReserved
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.headers.MyHeader21.description == "Header 21 description"
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.headers.MyHeader21.required
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.headers.MyHeader21.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.headers.MyHeader22.description == "Header 22 description"
        !operation.requestBody.content."multipart/mixed2".encoding.secondOject.headers.MyHeader22.required
        !operation.requestBody.content."multipart/mixed2".encoding.secondOject.headers.MyHeader22.deprecated
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions.size() == 2
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions."x-myExt21".prop1 == "prop1Val"
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions."x-myExt21".prop2 == "prop2Val"
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions."x-myExt22".prop1 == "prop1Val1"
        operation.requestBody.content."multipart/mixed2".encoding.secondOject.extensions."x-myExt22".prop2 == "prop2Val2"
    }
}
