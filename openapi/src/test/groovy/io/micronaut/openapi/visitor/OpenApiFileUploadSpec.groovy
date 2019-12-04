/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiFileUploadSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test parse the OpenAPI for file upload"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import javax.inject.Singleton;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.PartData;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.tags.Tag;

@Singleton
@Controller("/")
@Tag(name = "Upload")
class UploadController {

    /**
     * Single streaming file upload. Receiving PartData.
     * @param file The file parts.
     * @return nothing.
     */
    @Post(value = "/receive-flow-control", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> partData(Flowable<PartData> file) {
        return Single.just("");
    }

    /**
     * Single file upload. Receiving CompletedFileUpload.
     * @param file The complete file.
     * @return nothing.
     */
    @Post(value = "/receive-complete-file", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> complete(CompletedFileUpload file) {
        return Single.just(Long.toString(file.getSize()));
    }

    /**
     * Single streaming file upload. Receiving StreamingFileUpload.
     * @param file The streaming file.
     * @return nothing.
     */
    @Post(value = "/receive-streaming-file", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> streaming(StreamingFileUpload file) {
        return Single.just("");
    }

    /**
     * Two streaming files upload. Receiving StreamingFileUpload.
     * @param file1 The streaming file 1.
     * @param file2 The streaming file 2.
     * @return nothing.
     */
    @Post(value = "/receive-streaming-multiple", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> multipleStreaming(StreamingFileUpload file1, StreamingFileUpload file2) {
        return Single.just("");
    }

    /**
     * Multiple streaming files upload. Receiving StreamingFileUpload.
     * @param files The streaming files.
     * @return nothing.
     */
    @Post(value = "/receive-streaming-iterable", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> streamingIterable(Flowable<StreamingFileUpload> files) {
        return Single.just("");
    }
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

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
        requestBody.content['multipart/form-data'].schema instanceof ObjectSchema
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'] instanceof BinarySchema
        requestBody.content['multipart/form-data'].schema.properties['file'].description == "The file parts."

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
        requestBody.content['multipart/form-data'].schema instanceof ObjectSchema
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'] instanceof BinarySchema
        requestBody.content['multipart/form-data'].schema.properties['file'].description == "The complete file."

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
        requestBody.content['multipart/form-data'].schema instanceof ObjectSchema
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'] instanceof BinarySchema
        requestBody.content['multipart/form-data'].schema.properties['file'].description == "The streaming file."

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
        requestBody.content['multipart/form-data'].schema instanceof ObjectSchema
        requestBody.content['multipart/form-data'].schema.properties['file1']
        requestBody.content['multipart/form-data'].schema.properties['file1'] instanceof BinarySchema
        requestBody.content['multipart/form-data'].schema.properties['file1'].description == "The streaming file 1."
        requestBody.content['multipart/form-data'].schema.properties['file2']
        requestBody.content['multipart/form-data'].schema.properties['file2'] instanceof BinarySchema
        requestBody.content['multipart/form-data'].schema.properties['file2'].description == "The streaming file 2."

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
        requestBody.content['multipart/form-data'].schema instanceof ObjectSchema
        requestBody.content['multipart/form-data'].schema.properties['files']
        requestBody.content['multipart/form-data'].schema.properties['files'] instanceof ArraySchema
        requestBody.content['multipart/form-data'].schema.properties['files'].description == 'The streaming files.'
        requestBody.content['multipart/form-data'].schema.properties['files'].items  instanceof BinarySchema

        expect:
        operation
        operation.responses.size() == 1
    }

}