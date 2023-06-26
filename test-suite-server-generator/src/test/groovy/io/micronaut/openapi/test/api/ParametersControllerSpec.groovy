package io.micronaut.openapi.test.api

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import io.micronaut.openapi.test.model.SendPrimitivesResponse
import spock.lang.Unroll

@MicronautTest
class ParametersControllerSpec extends Specification {

    @Inject
    EmbeddedServer server

    @Inject
    @Client("/api")
    HttpClient reactiveClient

    BlockingHttpClient client

    void setup() {
        this.client = reactiveClient.toBlocking()
    }

    void "test send primitives"() {
        given:
        HttpRequest<?> request =
                HttpRequest.GET("/sendPrimitives/Andrew?age=1&isPositive=true")
                        .header("height", "17.3")

        when:
        Argument<SendPrimitivesResponse> arg = Argument.of(SendPrimitivesResponse)
        SendPrimitivesResponse response = client.retrieve(request, arg, Argument.of(String))

        then:
        "Andrew" == response.name
        BigDecimal.valueOf(1) == response.age
        Float.valueOf(17.3f) == response.height
        Boolean.TRUE == response.isPositive
    }

    void "test send not null primitives"() {
        given:
        HttpRequest<?> request = HttpRequest.GET("/sendPrimitives/Andrew")

        when:
        Argument<SendPrimitivesResponse> arg = Argument.of(SendPrimitivesResponse)
        client.retrieve(request, arg, Argument.of(String))

        then:
        var e = thrown(HttpClientResponseException)

        HttpStatus.BAD_REQUEST == e.getStatus()
        e.getMessage().contains("not specified")
    }

    void "test send nullable primitives"() {
        when:
        HttpRequest<?> request =
                HttpRequest.GET("/sendValidatedPrimitives").accept(MediaType.TEXT_PLAIN_TYPE)
        client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        notThrown(Exception)
    }

    void "test send validated primitives"() {
        when:
        HttpRequest<?> request =
                HttpRequest.GET("/sendValidatedPrimitives?name=Andrew&age=20&favoriteNumber=3.14&height=1.5")
        client.retrieve(request, Argument.of(String))

        then:
        notThrown(Exception)
    }

    @Unroll
    void "test send invalid primitives: #message"() {
        given:
        HttpRequest<?> request = HttpRequest.GET("/sendValidatedPrimitives?" + query)

        when:
        client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        def e = thrown(HttpClientResponseException)
        HttpStatus.BAD_REQUEST == e.status
        e.message != null
        e.message.contains(message)

        where:
        query                  | message
        "name=aa"              | "name: size must be between 3 and 2147483647"
        "name=123456"          | 'name: must match \\"[a-zA-Z]+\\"'
        "age=-2"               | "age: must be greater than or equal to 10"
        "age=500"              | "age: must be less than or equal to 200"
        "favoriteNumber=-500"  | "favoriteNumber: must be greater than or equal to -100.5"
        "favoriteNumber=100.6" | "favoriteNumber: must be less than or equal to 100.5"
        "height=0"             | "height: must be greater than or equal to 0.1"
        "height=100"           | "height: must be less than or equal to 3"
    }

    void "test send dates"() {
        when:
        HttpRequest<?> request =
                HttpRequest.GET("/sendDates?commitDate=2022-03-04&commitDateTime=2022-03-04T12:00:01.321Z")
        String response = client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        '{"commitDate":"2022-03-04","commitDateTime":"2022-03-04T12:00:01.321Z"}' == response
    }

    void "test send page query"() {
        when:
        HttpRequest<?> request =
                HttpRequest.GET("/sendPageQuery?page=2&size=20&sort=my-property%20desc,my-property-2")
        String response = client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        "(page: 2, size: 20, sort: my-property desc,my-property-2)" == response
    }

}
