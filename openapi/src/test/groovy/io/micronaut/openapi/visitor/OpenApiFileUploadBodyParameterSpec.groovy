package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import spock.lang.Issue

class OpenApiFileUploadBodyParameterSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI for file upload"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.PartData;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;

@Singleton
@Controller("/")
@Tag(name = "UploadOpenApi")
class UploadOpenApiController {

    @Post(value = "/receive-flow-control", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> partData(
            @Parameter(description = "File Parts.", schema = @Schema(type = "string", format = "binary"))
            Flowable<PartData> file) {
        return Single.just("");
    }

    @Post(value = "/receive-complete-file", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> complete(
            @Parameter(description = "Completed File.", schema = @Schema(type = "string", format = "binary"))
            CompletedFileUpload file) {
        return Single.just("");
    }

    @Post(value = "/receive-streaming-file", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> streaming(
            @Parameter(description = "Streaming File.", schema = @Schema(type = "string", format = "binary"))
            StreamingFileUpload file) {
        return Single.just("");
    }

    @Post(value = "/receive-streaming-multiple", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> multipleStreaming(
            @Parameter(description = "Streaming File 1.", schema = @Schema(type = "string", format = "binary"))
            StreamingFileUpload file1,
            @Parameter(description = "Streaming File 2.", schema = @Schema(type = "string", format = "binary"))
            StreamingFileUpload file2) {
        return Single.just("");
    }

    @Post(value = "/receive-streaming-iterable", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> streamingIterable(
            @Parameter(description = "List of Files.", array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            Flowable<StreamingFileUpload> files) {
        return Single.just("");
    }

}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI
        openAPI.paths.size() == 5

        when:
        Operation operation = openAPI.paths?.get("/receive-flow-control")?.post
        RequestBody requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file'].format == 'binary'
        requestBody.content['multipart/form-data'].schema.properties['file'].description == "File Parts."

        expect:
        operation
        operation.responses.size() == 1

        when:
        operation = openAPI.paths?.get("/receive-complete-file")?.post
        requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file'].format == 'binary'
        requestBody.content['multipart/form-data'].schema.properties['file'].description == "Completed File."

        expect:
        operation
        operation.responses.size() == 1

        when:
        operation = openAPI.paths?.get("/receive-streaming-file")?.post
        requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file'].format == 'binary'
        requestBody.content['multipart/form-data'].schema.properties['file'].description == "Streaming File."

        expect:
        operation
        operation.responses.size() == 1

        when:
        operation = openAPI.paths?.get("/receive-streaming-multiple")?.post
        requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file1']
        requestBody.content['multipart/form-data'].schema.properties['file1'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file1'].format == 'binary'
        requestBody.content['multipart/form-data'].schema.properties['file1'].description == "Streaming File 1."
        requestBody.content['multipart/form-data'].schema.properties['file2']
        requestBody.content['multipart/form-data'].schema.properties['file2'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file2'].format == 'binary'
        requestBody.content['multipart/form-data'].schema.properties['file2'].description == "Streaming File 2."

        expect:
        operation
        operation.responses.size() == 1

        when:
        operation = openAPI.paths?.get("/receive-streaming-iterable")?.post
        requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['files']
        requestBody.content['multipart/form-data'].schema.properties['files'] instanceof ArraySchema
        requestBody.content['multipart/form-data'].schema.properties['files'].description == 'List of Files.'
        requestBody.content['multipart/form-data'].schema.properties['files'].items.type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['files'].items.format == 'binary'

        expect:
        operation
        operation.responses.size() == 1
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/443")
    void "test multipart upload with @Part and CompletedFileUpload"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import jakarta.inject.Singleton;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.*;

@Controller("/docs")
class DocTestController {

    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<String> postMultipartForm(@Part("part") CompletedFileUpload completedFileUpload) {
        return HttpResponse.created("You did it!");
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI
        openAPI.paths.size() == 1

        when:
        Operation operation = openAPI.paths?.get("/docs")?.post
        RequestBody requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema.type == 'object'

        and: 'the  @Part value is used instead of the parameter name'
        requestBody.content['multipart/form-data'].schema.properties['part']
        requestBody.content['multipart/form-data'].schema.properties['part'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['part'].format == 'binary'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/907")
    void "test multipart upload with MultipartBody and @Body"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

@Singleton
@Controller("/")
class UploadOpenApiController {

    @Post(value = "/receive-multipart-body", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Publisher<String> completeMB(@Body MultipartBody file) {
        return reactor.core.publisher.Mono.just("");
    }

}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI
        openAPI.paths.size() == 1

        when:
        Operation operation = openAPI.paths?.get("/receive-multipart-body")?.post
        RequestBody requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['multipart/form-data'].schema
        requestBody.content['multipart/form-data'].schema instanceof ArraySchema
        requestBody.content['multipart/form-data'].schema.items.type == 'string'
        requestBody.content['multipart/form-data'].schema.items.format == 'binary'

        expect:
        operation
        operation.responses.size() == 1
    }

}
