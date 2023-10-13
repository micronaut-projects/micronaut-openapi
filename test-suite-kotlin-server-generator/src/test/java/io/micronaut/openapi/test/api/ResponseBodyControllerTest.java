package io.micronaut.openapi.test.api;

import java.time.ZonedDateTime;
import java.util.List;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.openapi.test.model.DateModel;
import io.micronaut.openapi.test.model.SimpleModel;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
public class ResponseBodyControllerTest {

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
    void testGetSimpleModel() {
        HttpResponse<SimpleModel> response = client.exchange("/getSimpleModel", SimpleModel.class);

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body());
    }

    @Test
    void testGetDateTime() {
        HttpResponse<ZonedDateTime> response = client.exchange("/getDateTime", ZonedDateTime.class);

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(ResponseBodyController.DATE_TIME_INSTANCE, response.body());

        String stringResponse = client.retrieve("/getDateTime", String.class);

        assertEquals("\"2022-12-04T06:35:00.784-05:00[America/Toronto]\"", stringResponse);
    }

    @Test
    void testGetDateModel() {
        HttpResponse<DateModel> response = client.exchange("/getDateModel", DateModel.class);

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(ResponseBodyController.DATE_MODEL_INSTANCE, response.body());

        String strResponse = client.retrieve("/getDateModel", String.class);

        assertEquals("""
            {"commitDate":"2023-06-27","commitDateTime":"2022-12-04T06:35:00.784-05:00[America/Toronto]"}""", strResponse);
    }

    @Test
    void testGetPaginatedSimpleModel() {
        var page = "12";
        var pageSize = "10";

        HttpRequest<?> request = HttpRequest.GET("/getPaginatedSimpleModel?page=" + page + "&size=" + pageSize);
        HttpResponse<List<SimpleModel>> response = client.exchange(request, Argument.listOf(SimpleModel.class));

        var totalCount = "3";
        var pageCount = "1";

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(ResponseBodyController.SIMPLE_MODELS, response.body());
        assertEquals(page, response.header("X-Page-Number"));
        assertEquals(totalCount, response.header("X-Total-Count"));
        assertEquals(3, response.body().size());
        assertEquals(pageSize, response.header("X-Page-Size"));
        assertEquals(pageCount, response.header("X-Page-Count"));
    }

    @Test
    void testGetDatedSimpleModel() {
        HttpResponse<SimpleModel> response =
            client.exchange(HttpRequest.GET("/getDatedSimpleModel"), Argument.of(SimpleModel.class));

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body());
        assertEquals(ResponseBodyController.LAST_MODIFIED_STRING, response.header("Last-Modified"));
    }

    @Test
    void testGetSimpleModelWithNonStandardStatus() {
        HttpResponse<SimpleModel> response = client.exchange("/getSimpleModelWithNonStandardStatus", SimpleModel.class);

        assertEquals(HttpStatus.CREATED, response.status());
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body());
    }

    @Test
    void testGetDatedSimpleModelWithNonMappedHeader() {
        HttpResponse<SimpleModel> response = client.exchange(
            HttpRequest.GET("/getDatedSimpleModelWithNonMappedHeader"), Argument.of(SimpleModel.class));

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body());
        assertEquals("custom-value", response.header("custom-header"));
        assertEquals(ResponseBodyController.LAST_MODIFIED_STRING, response.header("Last-Modified"));
    }

    @Test
    void testGetSimpleModelWithNonMappedHeader() {
        HttpResponse<SimpleModel> response = client.exchange("/getSimpleModelWithNonMappedHeader", SimpleModel.class);

        assertEquals(HttpStatus.OK, response.status());
        assertEquals("custom-value-2", response.getHeaders().get("custom-header"));
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body());
    }

    @Test
    void testGetErrorResponse() {

        var e = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(HttpRequest.GET("/getErrorResponse"), Argument.of(String.class), Argument.of(String.class)));

        assertEquals(e.getStatus(), HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetFile() {
        HttpRequest<?> request = HttpRequest.GET("/getFile");

        var arg = Argument.of(byte[].class);
        byte[] response = client.retrieve(request, arg, Argument.of(String.class));

        var expectedContent = "My file content";
        assertEquals(expectedContent, new String(response));
    }
}
