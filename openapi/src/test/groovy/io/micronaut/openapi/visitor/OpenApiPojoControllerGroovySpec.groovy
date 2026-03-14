package io.micronaut.openapi.visitor

import io.micronaut.ast.transform.test.AbstractBeanDefinitionSpec
import io.micronaut.http.MediaType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiPojoControllerGroovySpec extends AbstractBeanDefinitionSpec {

    def setup() {
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED, "false")
    }

    def cleanup() {
        System.clearProperty(Utils.ATTR_TEST_MODE)
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED)
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/561")
    void "test build OpenAPI for Controller with POJO with mandatory/optional fields works for Groovy"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.media.Schema

@Controller("/example")
class ExampleController {

    @Get("/")
    ExampleData getExampleData() {
        new ExampleData("name", true, new ExampleAdditionalData("hello"), 2, 0.456f, 1.2F)
    }
}

class ExampleData {
    String name
    @Schema(required = false)
    Boolean active
    ExampleAdditionalData additionalData
    Integer age
    Float battingAverage
    float anotherFloat

    ExampleData(String name, Boolean active, ExampleAdditionalData additionalData, Integer age, @Nullable Float battingAverage, float anotherFloat) {
        this.name = name
        this.active = active
        this.additionalData = additionalData
        this.age = age
        this.battingAverage = battingAverage
        this.anotherFloat = anotherFloat
    }
}

class ExampleAdditionalData {
    String something
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['ExampleData']

        then: "the components are valid"
        schema.properties.size() == 6
        schema.type == 'object'
        schema.required

        and: 'all params without annotations are required'
        schema.required.contains('name')
        schema.required.contains('additionalData')
        schema.required.contains('age')
        schema.required.contains('anotherFloat')

        and: 'active is not required because it is annotated with @Schema(required = false)'
        !schema.required.contains('active')

        and: 'battingAverage is not required because it is annotated with @Nullable in the constructor'
        !schema.required.contains('battingAverage')
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/1135")
    void "test build OpenAPI for set of enums"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Controller
class DogController {

    @Get('/dog')
    HttpResponse<Dog> get() {
        Dog dog = new Dog(
                name: 'Rex', breed: Breed.GERMAN_SHEPHERD,
                traits: EnumSet.of(Trait.QUICKNESS, Trait.COURAGE, Trait.LOYALTY)
        )

        return HttpResponse.ok(dog)
    }
}

@CompileStatic
class Dog {

    String name
    Breed breed
    Set<Trait> traits
}

@CompileStatic
enum Breed {

    GERMAN_SHEPHERD,
    BULLDOG,
    GOLDEN_RETRIEVER
}

@CompileStatic
enum Trait {

    QUICKNESS,
    COURAGE,
    LOYALTY
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema breedSchema = openAPI.components.schemas['Breed']
        Schema dogSchema = openAPI.components.schemas['Dog']
        Schema traitSchema = openAPI.components.schemas['Trait']

        then: "the components are valid"
        breedSchema
        dogSchema
        traitSchema

        dogSchema.properties.traits
        dogSchema.properties.traits.type == 'array'
        dogSchema.properties.traits.items.$ref == '#/components/schemas/Trait'

        traitSchema.type == 'string'
        traitSchema.enum.get(0) == 'QUICKNESS'
        traitSchema.enum.get(1) == 'COURAGE'
        traitSchema.enum.get(2) == 'LOYALTY'
    }

    void "test empty default value for Map body type groovy"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post

@Controller
class TestController {

    @Post("test1")
    String test1(@Body Map body) {
        return null
    }
}

@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.type == 'object'
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.additionalProperties
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.additionalProperties.type == "object"
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.default == null
    }

    void "test operation with content groovy"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status
import io.micronaut.http.multipart.CompletedFileUpload
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Controller('/file')
@CompileStatic
class FileController {

    @Post(value = "/", consumes = MediaType.MULTIPART_FORM_DATA)
    @Status(HttpStatus.NO_CONTENT)
    @Operation(
            operationId = 'UploadFile',
            summary = 'Upload a file',
            requestBody = @RequestBody(
                    description = 'File request',
                    content = @Content(
                        mediaType = MediaType.MULTIPART_FORM_DATA,
                        encoding = [
                            @Encoding(
                                name = "file",
                                contentType = MediaType.APPLICATION_OCTET_STREAM
                            )
                        ],
                        examples = [
                            @ExampleObject(
                                name = "example-1",
                                summary = "sum",
                                description = "this is description",
                                value = "{\\"name\\":\\"Charlie\\"}")
                        ],
                        schema = @Schema(type = 'object'))
            ),
            responses = [
                    @ApiResponse(responseCode = '204', description = 'OK'),
            ]
    )
    void uploadFile(CompletedFileUpload file) {
        assert file.bytes
    }

}

@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths."/file".post.requestBody.content."multipart/form-data"
        openAPI.paths."/file".post.requestBody.content."multipart/form-data".schema
        openAPI.paths."/file".post.requestBody.content."multipart/form-data".encoding.file.contentType == MediaType.APPLICATION_OCTET_STREAM
    }

    void "test requestBody groovy"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status
import io.micronaut.http.multipart.CompletedFileUpload
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody

@Controller('/file')
@CompileStatic
class FileController {

    @Post(value = "/", consumes = MediaType.MULTIPART_FORM_DATA)
    @Status(HttpStatus.NO_CONTENT)
    void uploadFile(@RequestBody(
                    description = 'File request',
                    content = @Content(
                        mediaType = MediaType.MULTIPART_FORM_DATA,
                        encoding = [
                            @Encoding(
                                name = "file",
                                contentType = MediaType.APPLICATION_OCTET_STREAM
                            )
                        ],
                        examples = [
                            @ExampleObject(
                                name = "example-1",
                                summary = "sum",
                                description = "this is description",
                                value = "{\\"name\\":\\"Charlie\\"}")
                        ],
                        schema = @Schema(type = 'object'))
            ) CompletedFileUpload file) {
        assert file.bytes
    }

}

@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths."/file".post.requestBody.content."multipart/form-data"
        openAPI.paths."/file".post.requestBody.content."multipart/form-data".schema
        openAPI.paths."/file".post.requestBody.content."multipart/form-data".encoding.file.contentType == MediaType.APPLICATION_OCTET_STREAM
    }

    void "test java records with groovy"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.sample.FooRecord
import io.micronaut.serde.annotation.Serdeable

@Controller("/demo3")
class Demo3Controller {

    @Get(uri = "/", produces = "text/plain")
    String index() {
        "Example Response"
    }

    @Get("/foo")
    FooRecord foo() {
        return new FooRecord("foo", "bar")
    }

    @Get("/bar")
    BarRecord bar() {
        return new BarRecord("foo", "bar")
    }
}

@Serdeable
record BarRecord(String foo, String bar) {
}

@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas.FooRecord.type == 'object'
        openAPI.components.schemas.FooRecord.properties
        openAPI.components.schemas.FooRecord.properties.size() == 2
        openAPI.components.schemas.FooRecord.properties.foo
        openAPI.components.schemas.FooRecord.properties.foo.type == 'string'
        openAPI.components.schemas.FooRecord.properties.bar
        openAPI.components.schemas.FooRecord.properties.bar.type == 'string'
    }

    void "test java records with groovy22"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Controller
class ExecutionController {

    @Get(uri="/execution/{id}/output")
    @Operation(
        summary="Execution Output",
        tags = ["Job Executions"]
    )
    @ApiResponse(
        responseCode = '200',
        description = "Log Output Response",
        content = [
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object"),
                examples=[
                    @ExampleObject(
                        name = 'log-output-compacted',
                        description = 'Compacted JSON log output',
                        value="""{
  "id": 1,
  "compacted": "true",
  "entries": [
    {
      "time": "17:00:00",
      "log": "First log message"
    }
  ]
}"""
                    )
                ]
            ),
            @Content(
                mediaType = MediaType.TEXT_PLAIN,
                schema = @Schema(type = "string"),
                examples = [
                    @ExampleObject(
                        name = 'log-output-text',
                        description = 'Textual log output',
                        value = """Log output text..."""
                    )
                ]
            )
        ]
    )
    def apiExecutionOutput() {
    }
}

@jakarta.inject.Singleton
public class MyBean {}
''')

        var openApi = Utils.testReference

        then:
        var content = openApi.paths."/execution/{id}/output".get.responses."200".content

        content.size() == 2
        content."application/json".schema.type == "object"
        content."application/json".examples instanceof Map
        ((Map) content."application/json".examples).keySet().every { it instanceof String }
        ((Map) content."application/json".examples).values().every { it instanceof Example }

        def examplesMap = (Map<String, Example>) content."application/json".examples
        examplesMap."log-output-compacted"
        examplesMap."log-output-compacted".description == "Compacted JSON log output"
        examplesMap."log-output-compacted".value.id == 1

        content."text/plain".schema.type == "string"

        content."text/plain".examples instanceof Map
        ((Map) content."text/plain".examples).keySet().every { it instanceof String }
        ((Map) content."text/plain".examples).values().every { it instanceof Example }

        def examplesMap2 = (Map<String, Example>) content."text/plain".examples
        examplesMap2."log-output-text"
        examplesMap2."log-output-text".description == "Textual log output"
        examplesMap2."log-output-text".value == "Log output text..."
    }
}
