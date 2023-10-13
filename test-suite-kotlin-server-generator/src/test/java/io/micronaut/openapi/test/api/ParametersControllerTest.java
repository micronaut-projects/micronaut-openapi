package io.micronaut.openapi.test.api;

import java.math.BigDecimal;
import java.util.stream.Stream;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.openapi.test.model.ColorEnum;
import io.micronaut.openapi.test.model.SendPrimitivesResponse;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@MicronautTest
class ParametersControllerTest {

    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/api")
    HttpClient reactiveClient;

    BlockingHttpClient client;

    @BeforeEach
    void setup() {
        client = reactiveClient.toBlocking();
    }

    @Test
    void testSendPrimitives() {
        HttpRequest<?> request = HttpRequest.GET("/sendPrimitives/Andrew?age=1&isPositive=true")
                .header("height", "17.3");

        var arg = Argument.of(SendPrimitivesResponse.class);
        var response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(response.getName(), "Andrew");
        assertEquals(response.getAge(), BigDecimal.valueOf(1));
        assertEquals(response.getHeight(), Float.valueOf(17.3f));
        assertEquals(response.isPositive(), Boolean.TRUE);
    }

    @Test
    void testSendNotNullPrimitives() {
        HttpRequest<?> request = HttpRequest.GET("/sendPrimitives/Andrew");

        var arg = Argument.of(SendPrimitivesResponse.class);

        var e = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(request, arg, Argument.of(String.class)));

        assertEquals(e.getStatus(), HttpStatus.BAD_REQUEST);
        assertTrue(e.getMessage().contains("not specified"));
    }

    @Disabled
    @Test
    void testSendNullablePrimitives() {
        HttpRequest<?> request = HttpRequest.GET("/sendValidatedPrimitives")
            .accept(MediaType.TEXT_PLAIN_TYPE);
        client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertDoesNotThrow(() -> client.retrieve(request, Argument.of(String.class), Argument.of(String.class)));
    }

    @Test
    void testSendValidatedPrimitives() {
        HttpRequest<?> request = HttpRequest.GET("/sendValidatedPrimitives?name=Andrew&age=20&favoriteNumber=3.14&height=1.5");

        assertDoesNotThrow(() -> client.retrieve(request, Argument.of(String.class)));
    }

    static Stream<Arguments> queries() {
        return Stream.of(
            arguments("name=aa", "name: size must be between 3 and 2147483647"),
            arguments("name=123456", "name: must match \"[a-zA-Z]+\""),
            arguments("age=-2", "age: must be greater than or equal to 10"),
            arguments("age=500", "age: must be less than or equal to 200"),
            arguments("favoriteNumber=-500", "favoriteNumber: must be greater than or equal to -100.5"),
            arguments("favoriteNumber=100.6", "favoriteNumber: must be less than or equal to 100.5"),
            arguments("height=0", "height: must be greater than or equal to 0.1"),
            arguments("height=100", "height: must be less than or equal to 3")
        );
    }

    @Disabled
    @MethodSource("queries")
    @ParameterizedTest
    void testSendInvalidPrimitivesMessage(String query, String message) {

        HttpRequest<?> request = HttpRequest.GET("/sendValidatedPrimitives?" + query);

        var e = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(request, Argument.of(String.class), Argument.of(String.class)));

        assertEquals(e.getStatus(), HttpStatus.BAD_REQUEST);
        assertTrue(e.getMessage().contains(message));
    }

    @EnumSource(ColorEnum.class)
    @ParameterizedTest
    void testSendParameterEnumColor(ColorEnum color) {
        HttpRequest<?> request = HttpRequest.GET("/sendParameterEnum?colorParam=" + color.getValue());

        ColorEnum response = client.retrieve(request, Argument.of(ColorEnum.class));

        assertEquals(color, response);

        String stringResponse = client.retrieve(request, Argument.of(String.class));

        assertEquals('"' + color.getValue() + '"', stringResponse);
    }

    @Test
    void testSendDates() {
        HttpRequest<?> request =
            HttpRequest.GET("/sendDates?commitDate=2022-03-04&commitDateTime=2022-03-04T12:00:01.321Z");
        String response = client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertEquals("""
            {"commitDate":"2022-03-04","commitDateTime":"2022-03-04T12:00:01.321Z"}""", response);
    }

    @Test
    void testSendPageQuery() {
        HttpRequest<?> request = HttpRequest.GET("/sendPageQuery?page=2&size=20&sort=my-property,desc&sort=my-property-2");
        String response = client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertEquals("(page: 2, size: 20, sort: my-property(dir=DESC) my-property-2(dir=ASC))", response);
    }

    @Test
    void testSendMappedParameter() {
        var filter = "name=Andrew,age>20";

        HttpRequest<?> request = HttpRequest.GET("/sendMappedParameter")
                .header("Filter", filter);
        String response = client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertEquals(response, filter);
    }

    @Test
    void testSendInvalidMappedParameter() {
        HttpRequest<?> request = HttpRequest.GET("/sendMappedParameter")
            .header("Filter", "name=Andrew,age>20,something-else");
        var e = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(request, Argument.of(String.class), Argument.of(String.class)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

}
