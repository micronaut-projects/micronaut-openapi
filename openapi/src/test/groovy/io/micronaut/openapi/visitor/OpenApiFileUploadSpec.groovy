package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiFileUploadSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI for file upload"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import jakarta.inject.Singleton;

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
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file'].format == 'binary'
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
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file']
        requestBody.content['multipart/form-data'].schema.properties['file'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file'].format == 'binary'
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
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['file1']
        requestBody.content['multipart/form-data'].schema.properties['file1'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file1'].format == 'binary'
        requestBody.content['multipart/form-data'].schema.properties['file1'].description == "The streaming file 1."
        requestBody.content['multipart/form-data'].schema.properties['file2']
        requestBody.content['multipart/form-data'].schema.properties['file2'].type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['file2'].format == 'binary'
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
        requestBody.content['multipart/form-data'].schema.type == 'object'
        requestBody.content['multipart/form-data'].schema.properties['files']
        requestBody.content['multipart/form-data'].schema.properties['files'] instanceof ArraySchema
        requestBody.content['multipart/form-data'].schema.properties['files'].description == 'The streaming files.'
        requestBody.content['multipart/form-data'].schema.properties['files'].items.type == 'string'
        requestBody.content['multipart/form-data'].schema.properties['files'].items.format == 'binary'

        expect:
        operation
        operation.responses.size() == 1
    }

    void "Issue#303 - multiple content types in separate methods"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;
@Controller("/pets")
interface PetOperations<T extends Pet> {
    /**
     * Saves a Pet
     *
     * @param pet pet
     * @return A pet or 404
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/")
    T save(@Body T pet);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Post("/")
    T saveByUpload(@Body T pet);
}
class Pet {
    private int age;
    private String name;
    private List<String> tags;
    public void setAge(int a) {
        age = a;
    }
    /**
     * The Pet Age
     *
     * @return The Pet Age
     */
    public int getAge() {
        return age;
    }
    public void setName(String n) {
        name = n;
    }
    /**
     * The Pet Name
     *
     * @return The Pet Name
     */
    public String getName() {
        return name;
    }
    /**
     * The Pet Tags
     *
     * @return The Tag
     */
    public List<String> getTags() {
        return tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then: "The state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference

        then: "The operation has only one path"
        openAPI.paths.size() == 1

        when: "The POST /pets operation is retrieved"
        Operation operation = openAPI.paths?.get("/pets")?.post

        then: "The body has multiple content types"
        operation.requestBody
        operation.requestBody.content.size() == 2
        operation.requestBody.content['application/json'].schema
        operation.requestBody.content['application/x-www-form-urlencoded'].schema
    }

}
