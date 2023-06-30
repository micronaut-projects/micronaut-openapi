package io.micronaut.openapi.test.api

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.openapi.test.model.DateModel
import io.micronaut.openapi.test.model.SimpleModel
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import java.time.ZonedDateTime

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

    void "test get date time"() {
        when:
        HttpResponse<ZonedDateTime> response = client.exchange("/getDateTime", ZonedDateTime)

        then:
        HttpStatus.OK == response.status()
        ResponseBodyController.DATE_TIME_INSTANCE == response.body()

        when:
        String stringResponse = client.retrieve("/getDateTime", String)

        then:
        '"2022-12-04T06:35:00.784-05:00[America/Toronto]"' == stringResponse
    }

    void "test get date model"() {
        when:
        HttpResponse<DateModel> response = client.exchange("/getDateModel", DateModel)

        then:
        HttpStatus.OK == response.status()
        ResponseBodyController.DATE_MODEL_INSTANCE == response.body()

        when:
        String strResponse = client.retrieve("/getDateModel", String)

        then:
        '{"commitDate":"2023-06-27","commitDateTime":"2022-12-04T06:35:00.784-05:00[America/Toronto]"}' == strResponse
    }

    void "test get paginated simple model"() {
        given:
        var page = "12"
        var pageSize = "10"

        when:
        HttpRequest<?> request = HttpRequest.GET("/getPaginatedSimpleModel?page=${page}&size=${pageSize}")
        HttpResponse<List<SimpleModel>> response =
                client.exchange(request, Argument.listOf(SimpleModel))

        then:
        var totalCount = "3"
        var pageCount = "1"

        HttpStatus.OK == response.status
        ResponseBodyController.SIMPLE_MODELS == response.body()
        page == response.header("X-Page-Number")
        totalCount == response.header("X-Total-Count")
        3 == response.body().size()
        pageSize == response.header("X-Page-Size")
        pageCount == response.header("X-Page-Count")
    }

    void "test get dated simple model"() {
        when:
        HttpResponse<SimpleModel> response =
                client.exchange(HttpRequest.GET("/getDatedSimpleModel"), Argument.of(SimpleModel))

        then:
        HttpStatus.OK == response.status()
        ResponseBodyController.SIMPLE_MODEL == response.body()
        ResponseBodyController.LAST_MODIFIED_STRING == response.header("Last-Modified")
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

    void "test get file"() {
        given:
        HttpRequest<?> request = HttpRequest.GET("/getFile")

        when:
        Argument<byte[]> arg = Argument.of(byte[])
        byte[] response = client.retrieve(request, arg, Argument.of(String))

        then:
        def expectedContent = "My file content"
        expectedContent == new String(response)
    }

}
