package io.micronaut.openapi.test.api

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.openapi.test.model.SimpleModel
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Ignore
import spock.lang.Specification

@MicronautTest
class ResponseBodyControllerSpec extends Specification {

    @Inject
    EmbeddedServer server

    @Inject
    @Client("/api")
    HttpClient reactiveClient

    BlockingHttpClient client

    void setup() {
        this.client = reactiveClient.toBlocking()
    }

    void "test get simple model"() {
        when:
        HttpResponse<SimpleModel> response = client.exchange("/getSimpleModel", SimpleModel )

        then:
        HttpStatus.OK == response.status
        ResponseBodyController.SIMPLE_MODEL == response.body()
    }

    // TODO implement the behavior and test
    void "test get paginated simple model"() {
        when:
        HttpResponse<List<SimpleModel>> response =
                client.exchange(HttpRequest.GET("/getPaginatedSimpleModel"), Argument.listOf(SimpleModel))

        then:
        HttpStatus.OK == response.status
        ResponseBodyController.SIMPLE_MODELS == response.body()
    }

    // TODO implement the behavior and test
    void "test get dated simple model"() {
        HttpResponse<SimpleModel> response =
                client.exchange(HttpRequest.GET("/getDatedSimpleModel"), Argument.of(SimpleModel))

        HttpStatus.ACCEPTED == response.status()
        ResponseBodyController.SIMPLE_MODEL == response.body()
    }

    void "test get simple model with non standard status"() {
        HttpResponse<SimpleModel> response = client.exchange("/getSimpleModelWithNonStandardStatus", SimpleModel)

        HttpStatus.ACCEPTED == response.status()
        ResponseBodyController.SIMPLE_MODEL == response.body()
    }

    // TODO implement the behavior and test
    void "test get dated simple model with non standard headers"() {
        HttpResponse<SimpleModel> response = client.exchange(
                HttpRequest.GET("/getTaggedSimpleModelWithNonStandardHeaders"), Argument.of(SimpleModel))

        HttpStatus.OK == response.status
        ResponseBodyController.SIMPLE_MODEL == response.body()
    }

    void "test get simple model with non mapped header"() {
        HttpResponse<SimpleModel> response = client.exchange("/getSimpleModelWithNonStandardHeader", SimpleModel)

        HttpStatus.OK == response.status
        "simple model" == response.headers.get("custom-header")
        ResponseBodyController.SIMPLE_MODEL == response.body()
    }

    void "test get error response"() {
        when:
        client.retrieve(HttpRequest.GET("/getErrorResponse"), Argument.of(String), Argument.of(String))

        then:
        def e = thrown(HttpClientResponseException)
        HttpStatus.NOT_FOUND == e.status
    }

    @Ignore("Requires fixing")
    void "test get file"() {
        given:
        HttpRequest<?> request = HttpRequest.GET("/getFile").contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        Argument<byte[]> arg = Argument.of(byte[])
        byte[] response = client.retrieve(request, arg, Argument.of(String))

        then:
        def expectedContent = "My file content"
        expectedContent == new String(response)
    }

}