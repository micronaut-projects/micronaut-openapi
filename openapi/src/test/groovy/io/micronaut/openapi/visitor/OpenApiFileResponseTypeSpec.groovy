package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiFileResponseTypeSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    void "test build the OpenAPI for returning files"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.*;

import java.io.*;
import java.nio.*;
import java.util.UUID;

@Controller("/upload")
class FooController {

    @Produces("application/pdf")
    @Get("/response-streamed-file/{documentId}")
    public HttpResponse<StreamedFile> action1(UUID documentId) {
        return null;
    }

    @Produces("application/pdf")
    @Get("/response-system-file/{documentId}")
    public HttpResponse<SystemFile> action2(UUID documentId) {
        return null;
    }

    @Produces("application/pdf")
    @Get("/streamed-file/{documentId}")
    public StreamedFile action3(UUID documentId) {
        return null;
    }

    @Produces("application/pdf")
    @Get("/system-file/{documentId}")
    public SystemFile action4(UUID documentId) {
        return null;
    }

    @Produces("application/pdf")
    @Get("/file/{documentId}")
    public File action5(UUID documentId) {
        return null;
    }

    @Produces("application/pdf")
    @Get("/input-stream/{documentId}")
    public InputStream action6(UUID documentId) {
        return null;
    }

    @Produces("application/pdf")
    @Get("/byte-buffer/{documentId}")
    public ByteBuffer action7(UUID documentId) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI
        openAPI.paths.size() == 7
        openAPI.paths.each {
            assert it.value.get.responses.size() == 1
            assert it.value.get.responses['200'].content['application/pdf'].schema.type == 'string'
            assert it.value.get.responses['200'].content['application/pdf'].schema.format == 'binary'
            assert it.value.get.responses['200'].content['application/pdf'].schema.$ref == null
        }
    }

    void "test build the OpenAPI for returning files and check their annotations takes precedence"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;

import java.util.UUID;

@Controller("/upload")
class FooController {

    @Produces("application/octet-stream")
    @Get("/{documentId}")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary")))
    public HttpResponse<StreamedFile> action(UUID documentId) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then: 'The content is the one defined in the @Content annotation'
        openAPI
        openAPI.paths.size() == 1
        Operation operation = openAPI.paths.get('/upload/{documentId}').get
        operation.responses['200'].content['application/octet-stream'].schema.type == 'string'
        operation.responses['200'].content['application/octet-stream'].schema.format == 'binary'
        operation.responses['200'].content['application/octet-stream'].schema.$ref == null
    }
}
